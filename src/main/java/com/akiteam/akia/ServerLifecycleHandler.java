package com.akiteam.akia;

import com.akiteam.akia.api.PluginManager;
import com.akiteam.akia.config.ConfigManager;
import com.akiteam.akia.diag.DumpReport;
import com.akiteam.akia.diag.PluginDiagnostic;
import com.akiteam.akia.loader.PluginManagerImpl;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 服务器生命周期事件处理器：把插件生命周期与服务器的启停状态绑定
 * （参考 Paper {@code SimplePluginManager}，启动完成后启用、关闭前禁用）。
 * <p>
 * 通过类上的 {@link EventBusSubscriber} 注解，FML 会自动把这些
 * {@code static} 订阅方法注册到 NeoForge 事件总线（GAME 总线），无需手动注册。
 * <p>
 * 说明：官方 NeoForge 并没有名为 {@code ServerLifecycleEvent} 的类，
 * 启动/停止这一类事件实际位于 {@code net.neoforged.neoforge.event.server} 包：
 * <ul>
 *     <li>{@link ServerStartedEvent} —— 服务器完全启动完成后触发（"启动完成"）</li>
 *     <li>{@link ServerStoppingEvent} —— 服务器开始关闭前触发（"关闭前"）</li>
 * </ul>
 * 因此这里监听的是这两个事件，语义与需求中的 {@code SERVER_STARTED}/{@code SERVER_STOPPING} 对应。
 */
@EventBusSubscriber(modid = Akia.MODID)
public final class ServerLifecycleHandler {

    private static final org.slf4j.Logger LOGGER = Akia.LOGGER;

    private ServerLifecycleHandler() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 服务器完全启动完成后调用：启用所有已加载的插件。
     * <p>
     * 逐个调用 {@link PluginManagerImpl#enablePlugin(String)}。单个插件启用失败
     * （已启用或抛异常，均由 {@code PluginManagerImpl} 内部捕获返回 {@code false}）
     * 不会中断其余插件的启用，最后统一记录成功/失败数量。
     *
     * @param event 服务器启动完成事件
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        PluginManagerImpl manager = Akia.getPluginManager();
        if (manager == null) {
            LOGGER.error("Akia plugin manager is null; cannot enable plugins.");
            return;
        }

        int enabled = 0;
        int failed = 0;
        int[] result = manager.enableAllPlugins();
        enabled = result[0];
        failed = result[1];
        LOGGER.info("Akia: enabled {} plugin(s), {} failed.", enabled, failed);
    }

    /**
     * 服务器开始停止时调用：禁用所有已加载的插件。
     * <p>
     * 逐个调用 {@link PluginManagerImpl#disablePlugin(String)}，使插件能在关闭前
     * 通过 {@code onDisable()} 释放资源。失败不会中断其余插件。
     *
     * @param event 服务器停止事件
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PluginManagerImpl manager = Akia.getPluginManager();
        if (manager == null) {
            LOGGER.error("Akia plugin manager is null; cannot disable plugins.");
            return;
        }

        // 关服前保存所有插件配置，避免未保存的改动丢失
        ConfigManager.INSTANCE.saveAllConfigs();

        int disabled = 0;
        int failed = 0;
        int[] result = manager.disableAllPlugins();
        disabled = result[0];
        failed = result[1];
        LOGGER.info("Akia: disabled {} plugin(s), {} failed.", disabled, failed);
    }

    /**
     * 注册 Akia 自带的内置命令 {@code /akia reload}。
     * <p>
     * 该命令不经过插件命令注册表（避免热重载时被清空），由 Akia 自身在命令
     * 加载阶段注册。权限等级 2（需 OP），用于在运行时重新加载全部插件。
     *
     * @param event 命令注册事件
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("akia")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(ServerLifecycleHandler::executeReload))
                        .then(Commands.literal("list")
                                .executes(ServerLifecycleHandler::executeList))
                        .then(Commands.literal("info")
                                .then(Commands.argument("plugin", StringArgumentType.word())
                                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                Akia.getPluginManager().getDiagnostics().stream()
                                                        .map(PluginDiagnostic::name),
                                                builder))
                                        .executes(ServerLifecycleHandler::executeInfo)))
                        .then(Commands.literal("dump")
                                .executes(ServerLifecycleHandler::executeDump)));
    }

    /**
     * 执行 {@code /akia reload}：调用 {@link PluginManagerImpl#reloadPlugins()}，
     * 并把启用成功/失败统计反馈给命令源。全程在主线程（服务器线程）执行，内部异常被
     * 捕获并记录，不会导致服务器崩溃。
     *
     * @param context 命令上下文
     * @return 命令执行结果（成功返回 1）
     */
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("Reloading Akia plugins..."), true);
        try {
            int[] result = Akia.getPluginManager().reloadPlugins();
            // 热重载只在服务器端重注册插件命令，不会自动把更新后的命令树推送给在线玩家，
            // 导致客户端 Tab 补全 / 参数提示仍沿用旧定义（命令虽能执行却无法补全、报参数错误）。
            // 这里手动把磁盘上最新的命令树重新下发给所有在线玩家，使 Tab 补全即时生效。
            source.getServer().getPlayerList().getPlayers()
                    .forEach(p -> source.getServer().getCommands().sendCommands(p));
            source.sendSuccess(() -> Component.literal(
                    "Akia plugins reloaded. " + result[0] + " enabled, " + result[1] + " failed."), true);
        } catch (Throwable t) {
            LOGGER.error("Failed to reload plugins: {}", t.toString(), t);
            source.sendFailure(Component.literal("Failed to reload plugins: " + t));
        }
        return 1;
    }

    /**
     * 执行 {@code /akia list}：列出所有已加载插件及其详细状态
     * （版本、启停状态、依赖、导出包、注册服务数）。空数据时给友好提示。
     *
     * @param context 命令上下文
     * @return 命令执行结果（成功返回 1）
     */
    private static int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<PluginDiagnostic> plugins = Akia.getPluginManager().getDiagnostics();
        if (plugins.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No plugins loaded."), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("Plugins (").append(plugins.size()).append("):");
        for (PluginDiagnostic p : plugins) {
            sb.append("\n  ").append(p.name()).append(" v").append(p.version())
              .append("  [").append(p.enabled() ? "enabled" : "disabled").append("]")
              .append("  services=").append(p.services().size())
              .append("  listeners=").append(p.eventListenerCount())
              .append("  tasks=").append(p.taskCount());
            if (!p.depend().isEmpty()) {
                sb.append("  depend=").append(String.join(",", p.depend()));
            }
            if (!p.exports().isEmpty()) {
                sb.append("  exports=").append(String.join(",", p.exports()));
            }
        }
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    /**
     * 执行 {@code /akia info <plugin>}：显示单个插件的完整诊断信息
     * （元数据、依赖、导出包、已注册服务、监听器数、任务数）。未知插件给出提示。
     *
     * @param context 命令上下文
     * @return 命令执行结果（成功返回 1，未知插件返回 0）
     */
    private static int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "plugin");
        PluginDiagnostic d = Akia.getPluginManager().getDiagnostic(name);
        if (d == null) {
            source.sendFailure(Component.literal("Plugin '" + name + "' is not loaded."));
            return 0;
        }
        StringBuilder sb = new StringBuilder("=== ").append(d.name()).append(" v").append(d.version()).append(" ===");
        sb.append("\n  status: ").append(d.enabled() ? "enabled" : "disabled");
        sb.append("\n  main: ").append(d.mainClass());
        sb.append("\n  author: ").append(d.author().isEmpty() ? "-" : d.author());
        sb.append("\n  description: ").append(d.description().isEmpty() ? "-" : d.description());
        sb.append("\n  depend: ").append(d.depend().isEmpty() ? "-" : String.join(", ", d.depend()));
        sb.append("\n  softDepend: ").append(d.softDepend().isEmpty() ? "-" : String.join(", ", d.softDepend()));
        sb.append("\n  loadBefore: ").append(d.loadBefore().isEmpty() ? "-" : String.join(", ", d.loadBefore()));
        sb.append("\n  exports: ").append(d.exports().isEmpty() ? "-" : String.join(", ", d.exports()));
        sb.append("\n  services (").append(d.services().size()).append("): ")
          .append(d.services().isEmpty() ? "-" : String.join(", ", d.services()));
        sb.append("\n  event listeners: ").append(d.eventListenerCount());
        sb.append("\n  tasks: ").append(d.taskCount());
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    /**
     * 执行 {@code /akia dump}：把完整诊断报告用 Gson 序列化为 JSON 文件，
     * 写到 {@code <插件目录>/dump/dump-<时间戳>.json}。空数据时报告各字段为空数组/空对象。
     *
     * @param context 命令上下文
     * @return 命令执行结果（成功返回 1）
     */
    private static int executeDump(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            PluginManager manager = Akia.getPluginManager();
            DumpReport report = manager.getDumpReport();
            Path dir = manager.getDumpDirectory();
            if (dir == null) {
                source.sendFailure(Component.literal("Dump directory unavailable."));
                return 0;
            }
            Path file = dir.resolve("dump-" + report.timestamp() + ".json");
            String json = new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(report);
            Files.writeString(file, json, StandardCharsets.UTF_8);
            source.sendSuccess(() -> Component.literal(
                    "Diagnostic dump written to " + file.toAbsolutePath()), true);
        } catch (Throwable t) {
            LOGGER.error("Failed to write diagnostic dump: {}", t.toString(), t);
            source.sendFailure(Component.literal("Failed to write dump: " + t));
        }
        return 1;
    }
}