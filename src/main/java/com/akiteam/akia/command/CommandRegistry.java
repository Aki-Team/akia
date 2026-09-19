package com.akiteam.akia.command;

import com.akiteam.akia.Akia;
import com.akiteam.akia.permission.CommandPermissible;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 插件命令注册表（参考 Paper {@code CommandMap} 的“按命令名登记 + 同名冲突拒绝”设计）。
 * <p>
 * 插件在 {@code registerCommands(CommandRegistry)} 里用它登记命令；命令不会立刻生效，
 * 而是先记录在 {@link #commands} 表中，等到服务器加载命令时由 {@link #onRegisterCommands}
 * 统一把每个插件命令转换成 Brigadier 的 {@code Command<CommandSourceStack>} 注册进
 * {@link CommandDispatcher}。
 * <p>
 * 命令名约定：登记时不带开头的 {@code /}（传入时若有也会被剥离）。同名登记会被拒绝并告警，
 * 与官方/其他插件命令重名的会在真正注册时跳过并告警，避免覆盖。
 */
public final class CommandRegistry {

    private static final org.slf4j.Logger LOGGER = Akia.LOGGER;

    /** 注册命令的默认权限等级（2 = 需要管理员/OP）。 */
    public static final int DEFAULT_PERMISSION_LEVEL = 2;

    /**
     * 一条已登记的命令。
     *
     * @param executor        命令执行逻辑
     * @param permissionLevel 执行所需的权限等级（{@link CommandSourceStack#hasPermission(int)}）
     * @param permissionNode  执行所需的字符串权限节点；为 {@code null} 表示不校验节点
     * @param argumentName    命令的贪心字符串参数名；为 {@code null} 表示无参数（字面量命令）
     * @param suggestion      该参数的 Tab 补全提供者；为 {@code null} 表示无补全
     */
    private record CommandEntry(CommandExecutor executor, int permissionLevel,
                                String permissionNode, String argumentName, AkiCommandSuggestion suggestion) {
    }

    /** 命令名（不含 '/'）→ 命令实现。 */
    private final ConcurrentMap<String, CommandEntry> commands = new ConcurrentHashMap<>();

    /** 命令名（不含 '/'）→ 多级子命令树构建器（{@code registerNode} 登记的命令为真正多级 literal 树）。 */
    private final ConcurrentMap<String, NodeBuilder> nodeCommands = new ConcurrentHashMap<>();

    /** 最近一次收到 {@link RegisterCommandsEvent} 时的命令调度器；用于热重载时移除并重挂命令。 */
    private volatile CommandDispatcher<CommandSourceStack> dispatcher;

    /** 本注册表实际注册进 dispatcher 的命令名，用于热重载时精确移除旧节点。 */
    private final Set<String> registeredNames = ConcurrentHashMap.newKeySet();

    /**
     * 构造注册表，并把自己注册到 NeoForge 会话事件总线以接收 {@link RegisterCommandsEvent}。
     */
    public CommandRegistry() {
        NeoForge.EVENT_BUS.register(this);
    }

    /**
     * 登记一条命令，权限等级使用默认值 {@link #DEFAULT_PERMISSION_LEVEL}。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String)} 构建多级命令树，
     *             无参数字面量命令用 {@code registerNode(name).executes(...)} 实现。
     * @param name     命令名（可带或不带开头的 {@code /}）
     * @param executor 命令执行器
     * @return {@code true} 登记成功；命令名为空或已登记同名命令时返回 {@code false}
     */
    @Deprecated
    public boolean register(String name, CommandExecutor executor) {
        return register(name, executor, DEFAULT_PERMISSION_LEVEL);
    }

    /**
     * 登记一条命令，并指定执行所需的权限等级。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String, int)} 构建多级命令树，
     *             无参数字面量命令用 {@code registerNode(name, level).executes(...)} 实现。
     * @param name            命令名（可带或不带开头的 {@code /}）
     * @param executor        命令执行器
     * @param permissionLevel 所需权限等级（0 任何玩家，2 及以上需要 OP）
     * @return {@code true} 登记成功；命令名、执行器为空或已登记同名命令时返回 {@code false}
     */
    @Deprecated
    public boolean register(String name, CommandExecutor executor, int permissionLevel) {
        String label = stripSlash(name);
        if (label.isEmpty() || executor == null) {
            LOGGER.warn("Rejected invalid command registration (name='{}').", label);
            return false;
        }
        CommandEntry previous = commands.putIfAbsent(label, new CommandEntry(executor, permissionLevel, null, null, null));
        if (previous != null) {
            LOGGER.warn("Command '/{}' is already registered; rejected the duplicate.", label);
            return false;
        }
        LOGGER.info("Queued registration of command '/{}'.", label);
        return true;
    }

    /**
     * 登记一条命令，并指定<b>字符串权限节点</b>（权限等级使用默认值 {@link #DEFAULT_PERMISSION_LEVEL}）。
     * <p>
     * 执行前框架会用执行命令的 {@link CommandSourceStack} 校验该节点；
     * 当前无外挂权限插件，节点默认退化为 OP 身份校验（非 OP 玩家将被拦截）。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String, String)} 构建多级命令树。
     * @param name       命令名（可带或不带开头的 {@code /}）
     * @param executor   命令执行器
     * @param permission 权限节点（如 {@code "test.node"}）；为 {@code null}/{@code ""} 时不校验节点
     * @return {@code true} 登记成功；命令名、执行器为空或已登记同名命令时返回 {@code false}
     */
    @Deprecated
    public boolean register(String name, CommandExecutor executor, String permission) {
        return register(name, executor, DEFAULT_PERMISSION_LEVEL, permission);
    }

    /**
     * 登记一条命令，并同时指定权限等级与<b>字符串权限节点</b>。
     * <p>
     * 执行前框架会先校验 {@code permissionLevel}，再校验 {@code permission} 节点（二者都要满足）。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String, int, String)} 构建多级命令树。
     * @param name            命令名（可带或不带开头的 {@code /}）
     * @param executor        命令执行器
     * @param permissionLevel 所需权限等级（0 任何玩家，2 及以上需要 OP）
     * @param permission      权限节点（如 {@code "test.node"}）；为 {@code null}/{@code ""} 时不校验节点
     * @return {@code true} 登记成功；命令名、执行器为空或已登记同名命令时返回 {@code false}
     */
    @Deprecated
    public boolean register(String name, CommandExecutor executor, int permissionLevel, String permission) {
        String label = stripSlash(name);
        if (label.isEmpty() || executor == null) {
            LOGGER.warn("Rejected invalid command registration (name='{}').", label);
            return false;
        }
        CommandEntry previous = commands.putIfAbsent(label,
                new CommandEntry(executor, permissionLevel, permission, null, null));
        if (previous != null) {
            LOGGER.warn("Command '/{}' is already registered; rejected the duplicate.", label);
            return false;
        }
        LOGGER.info("Queued registration of command '/{}'.", label);
        return true;
    }

    /**
     * 登记一条带<b>贪心字符串参数</b>的命令（如 {@code /color <message>}），权限使用默认值。
     * <p>
     * 参数名用于在执行器中通过 {@code ctx.getArgument(name, String.class)} 取回输入，
     * 例如 {@code /color hello world} 会取回 {@code "hello world"}。参数必须是命令名的最后一个
     * 子节点，因此命令执行器里不能再用该参数名登记子命令。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}：带贪心字符串参数
     * 的命令用 {@code registerNode(name).sub(...).arg(name, StringArgumentType.greedyString(), ...)}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String)} 构建多级命令树，
     *             用 {@code arg} 挂类型化参数（如 {@code StringArgumentType.greedyString()}）。
     * @param name         命令名（可带或不带开头的 {@code /}）
     * @param argumentName 参数名（将在执行器中作为 key 使用）
     * @param executor     命令执行器
     * @return {@code true} 登记成功
     */
    @Deprecated
    public boolean registerArgument(String name, String argumentName, CommandExecutor executor) {
        return registerArgument(name, argumentName, executor, DEFAULT_PERMISSION_LEVEL);
    }

    /**
     * 登记一条带贪心字符串参数的命令，并指定权限等级。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String, int)} 构建多级命令树，
     *             用 {@code arg} 挂类型化参数。
     * @param name            命令名
     * @param argumentName    参数名
     * @param executor        命令执行器
     * @param permissionLevel 所需权限等级
     * @return {@code true} 登记成功；名称/参数/执行器非法或重名时返回 {@code false}
     */
    @Deprecated
    public boolean registerArgument(String name, String argumentName, CommandExecutor executor, int permissionLevel) {
        String label = stripSlash(name);
        if (label.isEmpty() || argumentName == null || argumentName.isEmpty() || executor == null) {
            LOGGER.warn("Rejected invalid argument command registration (name='{}').", label);
            return false;
        }
        CommandEntry previous = commands.putIfAbsent(label,
                new CommandEntry(executor, permissionLevel, null, argumentName, null));
        if (previous != null) {
            LOGGER.warn("Command '/{}' is already registered; rejected the duplicate.", label);
            return false;
        }
        LOGGER.info("Queued registration of argument command '/{} <{}>'.", label, argumentName);
        return true;
    }

    /**
     * 登记一条带贪心字符串参数的命令，并指定<b>字符串权限节点</b>。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String, String)} 构建多级命令树，
     *             用 {@code arg} 挂类型化参数。
     * @param name         命令名
     * @param argumentName 参数名
     * @param executor     命令执行器
     * @param permission   权限节点；为 {@code null}/{@code ""} 时不校验节点
     * @return {@code true} 登记成功
     */
    @Deprecated
    public boolean registerArgument(String name, String argumentName, CommandExecutor executor, String permission) {
        return registerArgument(name, argumentName, executor, DEFAULT_PERMISSION_LEVEL, permission);
    }

    /**
     * 登记一条带贪心字符串参数的命令，并同时指定权限等级与<b>字符串权限节点</b>。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String, int, String)} 构建多级命令树，
     *             用 {@code arg} 挂类型化参数。
     * @param name            命令名
     * @param argumentName    参数名
     * @param executor        命令执行器
     * @param permissionLevel 所需权限等级
     * @param permission      权限节点；为 {@code null}/{@code ""} 时不校验节点
     * @return {@code true} 登记成功；名称/参数/执行器非法或重名时返回 {@code false}
     */
    @Deprecated
    public boolean registerArgument(String name, String argumentName, CommandExecutor executor,
                                    int permissionLevel, String permission) {
        String label = stripSlash(name);
        if (label.isEmpty() || argumentName == null || argumentName.isEmpty() || executor == null) {
            LOGGER.warn("Rejected invalid argument command registration (name='{}').", label);
            return false;
        }
        CommandEntry previous = commands.putIfAbsent(label,
                new CommandEntry(executor, permissionLevel, permission, argumentName, null));
        if (previous != null) {
            LOGGER.warn("Command '/{}' is already registered; rejected the duplicate.", label);
            return false;
        }
        LOGGER.info("Queued registration of argument command '/{} <{}>'.", label, argumentName);
        return true;
    }

    /**
     * 登记一条带同一个<b>贪心字符串参数</b>、且带 <b>Tab 补全</b>的命令，权限使用默认值。
     * <p>
     * 与 {@link #registerArgument(String, String, CommandExecutor)} 等价，只是额外提供
     * {@link AkiCommandSuggestion}：玩家输入命令名后按 Tab 即可补全 {@code suggestion}
     * 返回的候选值。执行器读取参数的方式不变（{@code ctx.getArgument(name, String.class)}）。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}：该接口本身容易导致
     * "子命令菜单补全失灵"，请改用 {@link #registerNode(String)} 的层级树或 {@code arg} 类型化参数补全。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String)} 构建层级命令树，
     *             或配合类型化参数（{@code arg}）做原生补全。
     * @param name         命令名（可带或不带开头的 {@code /}）
     * @param argumentName 参数名（将在执行器中作为 key 使用）
     * @param executor     命令执行器
     * @param suggestion   Tab 补全提供者
     * @return {@code true} 登记成功
     */
    @Deprecated
    public boolean registerArgumentSuggestions(String name, String argumentName,
                                               CommandExecutor executor, AkiCommandSuggestion suggestion) {
        return registerArgumentSuggestions(name, argumentName, executor, suggestion, DEFAULT_PERMISSION_LEVEL);
    }

    /**
     * 登记一条带贪心字符串参数和 Tab 补全的命令，并指定权限等级。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String, int)} 构建层级命令树，
     *             或配合类型化参数（{@code arg}）做原生补全。
     * @param name            命令名
     * @param argumentName    参数名
     * @param executor        命令执行器
     * @param suggestion      Tab 补全提供者
     * @param permissionLevel 所需权限等级
     * @return {@code true} 登记成功；名称/参数/执行器非法或重名时返回 {@code false}
     */
    @Deprecated
    public boolean registerArgumentSuggestions(String name, String argumentName, CommandExecutor executor,
                                               AkiCommandSuggestion suggestion, int permissionLevel) {
        String label = stripSlash(name);
        if (label.isEmpty() || argumentName == null || argumentName.isEmpty()
                || executor == null || suggestion == null) {
            LOGGER.warn("Rejected invalid suggestion argument command registration (name='{}').", label);
            return false;
        }
        CommandEntry previous = commands.putIfAbsent(label,
                new CommandEntry(executor, permissionLevel, null, argumentName, suggestion));
        if (previous != null) {
            LOGGER.warn("Command '/{}' is already registered; rejected the duplicate.", label);
            return false;
        }
        LOGGER.info("Queued registration of suggestion argument command '/{} <{}>'.", label, argumentName);
        return true;
    }

    /**
     * 登记一条带贪心字符串参数和 Tab 补全的命令，并指定<b>字符串权限节点</b>。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String, String)} 构建层级命令树，
     *             或配合类型化参数（{@code arg}）做原生补全。
     * @param name         命令名
     * @param argumentName 参数名
     * @param executor     命令执行器
     * @param suggestion   Tab 补全提供者
     * @param permission   权限节点；为 {@code null}/{@code ""} 时不校验节点
     * @return {@code true} 登记成功
     */
    @Deprecated
    public boolean registerArgumentSuggestions(String name, String argumentName, CommandExecutor executor,
                                               AkiCommandSuggestion suggestion, String permission) {
        return registerArgumentSuggestions(name, argumentName, executor, suggestion, DEFAULT_PERMISSION_LEVEL, permission);
    }

    /**
     * 登记一条带贪心字符串参数和 Tab 补全的命令，并同时指定权限等级与<b>字符串权限节点</b>。
     * <p>
     * 本方法<b>仍可用</b>（旧插件无需改动），但新代码请改用 {@code registerNode}。
     *
     * @deprecated 已过时，请使用 {@link #registerNode(String, int, String)} 构建层级命令树，
     *             或配合类型化参数（{@code arg}）做原生补全。
     * @param name            命令名
     * @param argumentName    参数名
     * @param executor        命令执行器
     * @param suggestion      Tab 补全提供者
     * @param permissionLevel 所需权限等级
     * @param permission      权限节点；为 {@code null}/{@code ""} 时不校验节点
     * @return {@code true} 登记成功；名称/参数/执行器非法或重名时返回 {@code false}
     */
    @Deprecated
    public boolean registerArgumentSuggestions(String name, String argumentName, CommandExecutor executor,
                                               AkiCommandSuggestion suggestion, int permissionLevel, String permission) {
        String label = stripSlash(name);
        if (label.isEmpty() || argumentName == null || argumentName.isEmpty()
                || executor == null || suggestion == null) {
            LOGGER.warn("Rejected invalid suggestion argument command registration (name='{}').", label);
            return false;
        }
        CommandEntry previous = commands.putIfAbsent(label,
                new CommandEntry(executor, permissionLevel, permission, argumentName, suggestion));
        if (previous != null) {
            LOGGER.warn("Command '/{}' is already registered; rejected the duplicate.", label);
            return false;
        }
        LOGGER.info("Queued registration of suggestion argument command '/{} <{}>'.", label, argumentName);
        return true;
    }

    /**
     * 登记一条<b>多级子命令树</b>命令（真正用 Brigadier literal 构建的层级命令），权限用默认值。
     * <p>
     * 返回 {@link NodeBuilder}，可链式用 {@code sub}/{@code child}/{@code suggest}/{@code executes}
     * 构建多层子命令与每层的 Tab 补全，让 vanilla 客户端按输入 token 自动过滤与隐藏候选、
     * 选完子命令后空格只剩下一层的候选。适合 {@code /cmd <子命令> [选项] } 这类层级命令。
     * <p>
     * 与 {@link #register}/{@link #registerArgument} 系列互斥：同一命令名已在另一边登记时拒绝。
     *
     * @param name 命令名（可带或不带开头的 {@code /}）
     * @return 命令树构建器；命令名非法或重名（含与字面量命令重名）时返回一个不可用的 no-op 构建器
     */
    public NodeBuilder registerNode(String name) {
        return registerNode(name, DEFAULT_PERMISSION_LEVEL, null);
    }

    /**
     * 登记一条多级子命令树命令，并指定根命令的权限等级。
     */
    public NodeBuilder registerNode(String name, int permissionLevel) {
        return registerNode(name, permissionLevel, null);
    }

    /**
     * 登记一条多级子命令树命令，并指定根命令的<b>字符串权限节点</b>（等级用默认值）。
     */
    public NodeBuilder registerNode(String name, String permission) {
        return registerNode(name, DEFAULT_PERMISSION_LEVEL, permission);
    }

    /**
     * 登记一条多级子命令树命令，并同时指定根命令的权限等级与<b>字符串权限节点</b>。
     */
    public NodeBuilder registerNode(String name, int permissionLevel, String permission) {
        String label = stripSlash(name);
        NodeBuilder dead = new NodeBuilder(this, label, permissionLevel, permission);
        if (label.isEmpty()) {
            LOGGER.warn("Rejected invalid node command registration (name='{}').", label);
            dead.markUnregisterable();
            return dead;
        }
        if (commands.containsKey(label)) {
            LOGGER.warn("Command '/{}' already registered as an argument command; rejected the node duplicate.", label);
            dead.markUnregisterable();
            return dead;
        }
        NodeBuilder previous = nodeCommands.putIfAbsent(label, dead);
        if (previous != null) {
            LOGGER.warn("Command '/{}' is already registered; rejected the duplicate.", label);
            dead.markUnregisterable();
            return dead;
        }
        LOGGER.info("Queued node command tree '/{}'.", label);
        return dead;
    }

    /**
     * 当服务器加载命令时，保存 dispatcher 引用并注册所有已登记的插件命令。
     *
     * @param event NeoForge 命令注册事件
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        this.dispatcher = event.getDispatcher();
        registerAllCommands();
    }

    /**
     * 把所有已登记的插件命令（重新）注册到当前 dispatcher。
     * <p>
     * 该方法是幂等的：注册前会先移除由本注册表管辖区、且已存在的旧节点，
     * 因此可在热重载（{@code RegisterCommandsEvent} 不会再次触发）时手动调用，
     * 让新插件的命令生效。与 vanilla/其他 mod 重名的命令仍会跳过，不覆盖他人。
     */
    public void registerAllCommands() {
        CommandDispatcher<CommandSourceStack> d = this.dispatcher;
        if (d == null) {
            LOGGER.warn("No dispatcher available; cannot register commands.");
            return;
        }
        Set<String> existing = d.getRoot().getChildren().stream()
                .map(CommandNode::getName)
                .collect(Collectors.toSet());

        for (Map.Entry<String, CommandEntry> e : commands.entrySet()) {
            String label = e.getKey();
            // 若该名称已存在且不是我们之前注册的，视为重名冲突，跳过以保护他人命令
            if (existing.contains(label) && !registeredNames.contains(label)) {
                LOGGER.warn("Command '/{}' already exists (vanilla or other plugin); skipped.", label);
                continue;
            }
            // 移除我们已注册的同名旧节点，保证重新注册是幂等的
            d.getRoot().getChildren().removeIf(n -> n.getName().equals(label));

            CommandEntry entry = e.getValue();
            LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(label)
                    .requires(src -> checkPermissions(src, entry.permissionLevel(), entry.permissionNode()));
            if (entry.argumentName() != null) {
                // 贪心字符串参数：允许接收 /cmd message（含空格），参数从命令名后一直取到行尾
                var arg = Commands.argument(entry.argumentName(), StringArgumentType.greedyString())
                        .executes(ctx -> entry.executor().execute(ctx));
                AkiCommandSuggestion suggestion = entry.suggestion();
                if (suggestion != null) {
                    arg = arg.suggests((ctx, builder) -> {
                        for (String candidate : suggestion.suggest(ctx, builder.getRemaining())) {
                            builder.suggest(candidate);
                        }
                        return builder.buildFuture();
                    });
                }
                literal.then(arg);
            } else {
                literal.executes(ctx -> entry.executor().execute(ctx));
            }
            d.register(literal);
            registeredNames.add(label);
            existing.add(label);
            LOGGER.info("Registered plugin command '/{}'.", label);
        }

        // 多级子命令树命令：以 build() 得到的完整 literal 树注册（含各层子命令与权限）
        for (Map.Entry<String, NodeBuilder> e : nodeCommands.entrySet()) {
            String label = e.getKey();
            // 与 legacy 字面量命令（commands）占用同一名字时不重复注册
            if (commands.containsKey(label)) {
                continue;
            }
            if (existing.contains(label) && !registeredNames.contains(label)) {
                LOGGER.warn("Command '/{}' already exists (vanilla or other plugin); skipped.", label);
                continue;
            }
            d.getRoot().getChildren().removeIf(n -> n.getName().equals(label));
            NodeBuilder nb = e.getValue();
            if (!nb.isRegisterable()) {
                continue;
            }
            d.register(nb.build());
            registeredNames.add(label);
            existing.add(label);
            LOGGER.info("Registered plugin node command '/{}'.", label);
        }
    }

    /**
     * 移除所有由本注册表注册的命令节点，并清空登记表。
     * <p>
     * 供热重载使用：先清命令，再重新加载插件（插件会重新填充 {@link #commands}），
     * 最后调用 {@link #registerAllCommands()} 让新命令生效。
     */
    public void clearPluginCommands() {
        CommandDispatcher<CommandSourceStack> d = this.dispatcher;
        if (d != null) {
            for (String name : registeredNames) {
                d.getRoot().getChildren().removeIf(n -> n.getName().equals(name));
            }
        }
        registeredNames.clear();
        commands.clear();
        nodeCommands.clear();
        LOGGER.info("Cleared all plugin commands.");
    }

    private String stripSlash(String name) {
        return name == null ? "" : (name.startsWith("/") ? name.substring(1) : name);
    }

    /**
     * 命令执行前的权限校验：先校验 OP 等级，再校验字符串权限节点（二者都需满足）。
     * <p>
     * 权限等级 <= 0 表示对权限不作限制；节点为 {@code null}/{@code ""} 表示不校验节点。
     * 注意：未通过 {@code requires} 校验的使用者根本看不到也不触发该命令（Brigadier 行为），
     * 因此“非 OP 玩家被拦截”即由此保证。
     */
    static boolean checkPermissions(CommandSourceStack source, int permissionLevel, String permissionNode) {
        if (permissionLevel > 0 && !source.hasPermission(permissionLevel)) {
            return false;
        }
        if (permissionNode == null || permissionNode.isEmpty()) {
            return true;
        }
        return new CommandPermissible(source).hasPermission(permissionNode);
    }
}