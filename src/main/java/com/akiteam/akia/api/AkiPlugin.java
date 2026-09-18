package com.akiteam.akia.api;

import com.akiteam.akia.command.CommandRegistry;
import com.akiteam.akia.config.PluginConfig;
import com.akiteam.akia.event.EventBus;
import com.akiteam.akia.permission.Permissible;
import com.akiteam.akia.persistence.AkiAdapterContext;
import com.akiteam.akia.persistence.AkiPersistentDataHolder;
import com.akiteam.akia.scheduler.Scheduler;
import com.akiteam.akia.service.ServiceRegistry;
import com.akiteam.akia.text.AkiText;

/**
 * 所有 Akia 插件的主类都必须实现此接口。
 * <p>
 * 插件的生命周期由 {@link PluginManager} 驱动，分为三个阶段：
 * <ul>
 *     <li>加载（{@link #onLoad()}）——插件进入扫描阶段时调用；</li>
 *     <li>启用（{@link #onEnable()}）——服务器启动完成后调用；</li>
 *     <li>禁用（{@link #onDisable()}）——服务器关闭前或重新加载时调用。</li>
 * </ul>
 * 插件主类还应在构造（或字段）中持有自己的 {@link PluginInfo}，供 {@link #getPluginInfo()} 返回。
 * <p>
 * 事件监听：插件主类上直接标注 {@code @EventHandler} 的方法会被自动注册到事件总线；
 * 若插件需要注册额外监听器对象，可覆盖 {@link #registerEvents(EventBus)} 手动加入。
 */
public interface AkiPlugin {

    /**
     * 插件被加载时调用（扫描阶段）。
     * <p>
     * 这里通常只做轻量初始化（创建配置、注册数据结构），
     * 不要在这里启动需要运行环境的服务——那应该放到 {@link #onEnable()}。
     */
    void onLoad();

    /**
     * 插件被启用时调用（服务器启动完成后运行）。
     * <p>
     * 真正开始干活（注册命令、监听事件、启动线程）都在这里。
     */
    void onEnable();

    /**
     * 插件被禁用时调用（服务器关闭前或插件被重新加载时运行)。
     * <p>
     * 在这里释放资源、停止线程、保存状态，确保插件能被干净地卸载。
     */
    void onDisable();

    /**
     * 返回此插件对应的元数据信息。
     * <p>
     * 实现方应在插件对象中保存从 {@code plugin.json} 解析出的 {@link PluginInfo} 并在此返回，
     * 它是 {@link #getName()} 的数据来源。
     *
     * @return 插件的元数据，不应为 {@code null}
     */
    PluginInfo getPluginInfo();

    /**
     * 返回插件名称，内容取自 {@link #getPluginInfo()} 中的 {@code name} 字段。
     *
     * @return 插件名称
     */
    default String getName() {
        return getPluginInfo().name();
    }

    /**
     * 插件注册额外事件监听器的钩子（默认不做任何事）。
     * <p>
     * 插件被加载时，{@code PluginManager} 会自动把插件主类自身注册为监听器 ——
     * 因此主类上直接写 {@code @EventHandler} 方法即可生效，无需覆盖本方法。
     * 只有当插件还需要把 <b>其他对象</b> 作为监听器交给 {@link EventBus} 时，
     * 才需要覆盖它并调用 {@code bus.registerEvents(this, myListener)}。
     *
     * @param bus 插件的共享事件总线（已注入，可为任意独立监听器对象调用注册）
     */
    default void registerEvents(EventBus bus) {
    }

    /**
     * 插件注册命令的钩子（默认不做任何事）。
     * <p>
     * 插件被加载时 {@code PluginManager} 会调用本方法，把 {@link CommandRegistry}
     * 交给插件登记命令。例如：
     * <pre>{@code
     * registry.register("hello", ctx -> {
     *     ctx.getSource().sendSuccess(() -> Component.literal("Hello from Akia!"), false);
     *     return 1;
     * });
     * }</pre>
     * 命令会按照 Brigadier 框架注册，无需在此手动处理 {@code /} 前缀。
     *
     * @param registry 共享命令注册表，用于登记以 {@code /} 开头的命令
     */
    default void registerCommands(CommandRegistry registry) {
    }

    /**
     * 插件注册调度器的钩子（默认不做任何事）。
     * <p>
     * 插件被加载时 {@code PluginManager} 会调用本方法，把全局 {@link Scheduler}
     * 交给插件。插件可在此把调度器保存到自己的字段中，之后用
     * {@code scheduler.runTaskLater(plugin, runnable, delayTicks)} 等方式安排任务。
     * 例如：
     * <pre>{@code
     * private Scheduler scheduler;
     * @Override
     * public void registerScheduler(Scheduler s) {
     *     this.scheduler = s;
     * }
     * }</pre>
     * 插件卸载时，其名下所有任务会被自动取消。
     *
     * @param scheduler 全局调度器实例（不会为 {@code null}）
     */
    default void registerScheduler(Scheduler scheduler) {
    }

    /**
     * 插件配置文件注入的钩子（默认不做任何事）。
     * <p>
     * 插件被加载时，{@code PluginManager} 会为本插件创建专属 {@link PluginConfig}
     * （对应 {@code run/akia/<插件名>/config.yml}）并调用本方法交给插件。
     * 插件应把 {@code config} 保存到自己的字段中，之后即可读写配置：
     * <pre>{@code
     * private PluginConfig config;
     * @Override
     * public void registerConfig(PluginConfig c) {
     *     this.config = c;
     * }
     * // 在 onEnable 中使用：
     * String host = config.getString("database.host", "localhost");
     * config.set("settings.enabled", true);
     * config.save();
     * }</pre>
     * 插件卸载时，其配置对象会被 {@code ConfigManager} 移除并释放引用；重新加载后
     * 会收到新的 {@link PluginConfig} 实例。
     *
     * @param config 本插件专属的配置文件对象（不会为 {@code null}）
     */
    default void registerConfig(PluginConfig config) {
    }

    /**
     * 插件 PDC 适配上下文注入的钩子（默认不做任何事）。
     * <p>
     * 插件被加载时，{@code PluginManager} 会调用本方法把全局 {@link AkiAdapterContext}
     * 交给插件。插件可用它创建新的 {@code AkiPersistentContainer}：
     * <pre>{@code
     * private AkiAdapterContext pdc;
     * @Override
     * public void registerPersistence(AkiAdapterContext context) {
     *     this.pdc = context;
     * }
     * // 在事件处理中使用：
     * var container = pdc.newPersistentDataContainer();
     * container.set(AkiKey.of(this, "kills"), BuiltInDataTypes.INTEGER, 42);
     * EntityPersistentDataBridge.set(player, container); // 写到实体并随存档保存
     * }</pre>
     *
     * @param context 全局 PDC 适配上下文（不会为 {@code null}）
     */
    default void registerPersistence(AkiAdapterContext context) {
    }

    /**
     * 返回插件自身作为“PDC 持有者”时的容器。
     * <p>
     * 插件若想把自己的某些数据作为插件级 PDC 暴露，可覆盖此方法返回
     * 一个已持有的 {@code AkiPersistentContainer}（通常来自
     * {@code registerPersistence} 注入的上下文）。默认返回 {@code null} 表示不适用。
     *
     * @return 插件的持久化容器，或 {@code null}
     */
    default AkiPersistentDataHolder getPersistentDataHolder() {
        return null;
    }

    /**
     * 插件文本组件工具注入的钩子（默认不做任何事）。
     * <p>
     * 插件被加载时，{@code PluginManager} 会调用本方法把全局 {@link AkiText} 工具交给插件。
     * 插件可保存它来发送彩色 / 可点击 / 带悬停提示的富文本消息：
     * <pre>{@code
     * private AkiText text;
     * @Override
     * public void registerText(AkiText t) {
     *     this.text = t;
     * }
     * // 在命令 / 事件中使用：
     * text.sendMessage(player,
     *     TextComponent.text("Hello, ").color(NamedTextColor.GOLD)
     *         .append(TextComponent.text("World").color(NamedTextColor.AQUA).bold(true)));
     * }</pre>
     *
     * @param text 全局 {@link AkiText} 工具实例（不会为 {@code null}）
     */
    default void registerText(AkiText text) {
    }

    /**
     * 插件服务注册表注入的钩子（默认不做任何事）。
     * <p>
     * 插件被加载时，{@code PluginManager} 会调用本方法把全局 {@link ServiceRegistry} 交给插件。
     * <b>推荐</b>在 {@link #onEnable()} 中通过它注册本插件提供的服务（供其他插件按接口类型获取）：
     * <pre>{@code
     * private ServiceRegistry services;
     * @Override
     * public void registerServices(ServiceRegistry r) {
     *     this.services = r;
     * }
     * // 在 onEnable 中注册服务：
     * services.register(Economy.class, new EconomyImpl(), this, ServicePriority.Normal);
     * // 在 onEnable 中获取其他插件提供的服务：
     * Economy economy = services.get(Economy.class);
     * }</pre>
     * 服务注册与获取都应在 {@code onEnable}（而非 {@code onLoad}）中进行，此时所有插件
     * 已完成加载，依赖拓扑序也已满足。插件卸载时，其注册的服务会被自动注销。
     *
     * @param registry 全局 {@link ServiceRegistry} 实例（不会为 {@code null}）
     */
    default void registerServices(ServiceRegistry registry) {
    }

    /**
     * 返回插件自身的权限判定对象。
     * <p>
     * 默认实现把插件本体视作可信方（等同 OP）：{@link Permissible#hasPermission(String)}
     * 直接返回 {@code true}。这是刻意为之——真正面向玩家的字符串权限节点校验在命令层完成
     * （见 {@code CommandRegistry.register(..., String permission)}），那里会基于执行命令的
     * {@link net.minecraft.commands.CommandSourceStack} 包装出按玩家判定的实现。
     * <p>
     * 返回值恒不为 {@code null}；若插件需要自定义本对象，可覆盖此方法返回自己的实现。
     *
     * @return 本插件的权限判定对象（不会为 {@code null}）
     */
    default Permissible getPermissible() {
        return new Permissible() {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }

            @Override
            public boolean isOp() {
                return true;
            }

            @Override
            public void setOp(boolean op) {
                // 插件本体不参与 OP 身份的增删，视为空操作
            }
        };
    }
}