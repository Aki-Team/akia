package com.akiteam.akia.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.ArrayList;
import java.util.List;

/**
 * 多级子命令树的流畅构建器（配合 {@link CommandRegistry#registerNode(String)}）。
 * <p>
 * 用 Brigadier 的 {@code Commands.literal(...).then(...)} 建成真正的多级字面量命令树，
 * 让 vanilla 客户端天然理解层级：每个子命令节点独立承载自己的执行器与权限，Tab 补全
 * 由 Brigadier 按"当前节点的子节点 + 已输入 token"自动过滤与隐藏（无需手动 suggestion）。
 * <p>
 * 用法（{@code /evtest} 一级子命令 + {@code all} 后二级候选 on/off）：
 * <pre>{@code
 * NodeBuilder ev = registry.registerNode("evtest");
 * ev.sub("on", execOn).sub("off", execOff)        // 一级字面量
 *   .sub("all", execAllToggle)
 *     .child("on", execAllOn).child("off", execAllOff); // all 后二级 on/off（各自执行器）
 * }</pre>
 * <ul>
 *     <li>{@link #sub(String, CommandExecutor)} —— 在当前命令的<b>顶级</b>加字面量子命令；</li>
 *     <li>{@link #child(String, CommandExecutor)} —— 在<b>最近一次 {@code sub} 的节点</b>下加可执行的子命令；</li>
 *     <li>{@link #suggest(String...)} —— 在最近一次 {@code sub} 的节点下加<b>仅用于补全</b>的字面量叶子；</li>
 *     <li>{@link #executes(CommandExecutor)} —— 设置命令无子命令匹配时的默认执行器。</li>
 * </ul>
 * 权限：{@link #sub}/{@link #child} 可覆盖该节点的权限等级与字符串权限节点，缺省继承根命令的。
 */
public final class NodeBuilder {

    private final CommandRegistry registry;
    private final String label;
    private final int rootPermLevel;
    private final String rootPermNode;
    private final LiteralArgumentBuilder<CommandSourceStack> root;

    /** 最近一次 {@link #sub} 添加的顶级字面量节点；{@link #child}/{@link #suggest} 附加到这些节点之下。 */
    private final List<LiteralArgumentBuilder<CommandSourceStack>> lastBranch = new ArrayList<>();

    /**
     * {@link #sub} 登记的全部顶级字面量节点。注意：不能像最初的实现那样在 {@link #sub} 里就
     * {@code root.then(child)}——Brigadier 构建树时会把每个父级节点快照一次，之后对已挂载子公司
     * 的 {@code then()} 变更不会被拾取，导致 {@link #child}/{@link #suggest} 追加的孙级全部丢失。
     * 所以改为先收集顶级子命令，留到 {@link #build()} 最后一并挂到根，确保子级在挂载前已完整组好。
     */
    private final List<LiteralArgumentBuilder<CommandSourceStack>> rootChildren = new ArrayList<>();

    /** 是否已组装进 {@code root}；防止 repeat build 时重复挂载同一批子节点。 */
    private boolean assembled;

    /** 登记失败（重名/非法名）时为 {@code false}，此时所有构建方法成为 no-op，不影响已存在的命令。 */
    private boolean registerable = true;

    NodeBuilder(CommandRegistry registry, String label, int permissionLevel, String permission) {
        this.registry = registry;
        this.label = label;
        this.rootPermLevel = permissionLevel;
        this.rootPermNode = permission;
        this.root = Commands.literal(label)
                .requires(src -> CommandRegistry.checkPermissions(src, permissionLevel, permission));
    }

    /** 标记为不可登记（重复登记时由 {@link CommandRegistry} 调用）。 */
    void markUnregisterable() {
        this.registerable = false;
        this.lastBranch.clear();
        this.rootChildren.clear();
    }

    boolean isRegisterable() {
        return registerable;
    }

    /**
     * 在命令<b>顶级</b>登记一个或多个字面量子命令（{@code names} 可用 {@code |} 分隔多个名字，
     * 如 {@code "on|off|list"}，它们共享同一个执行器）。权限继承根命令的。
     *
     * @return 本构建器（链式）
     */
    public NodeBuilder sub(String names, CommandExecutor executor) {
        return sub(names, executor, rootPermLevel, rootPermNode);
    }

    /**
     * 在命令顶级登记字面量子命令，并为该节点单独指定权限等级与字符串权限节点。
     */
    public NodeBuilder sub(String names, CommandExecutor executor, int permissionLevel, String permission) {
        if (!registerable || names == null || executor == null) {
            return this;
        }
        lastBranch.clear();
        for (String name : split(names)) {
            LiteralArgumentBuilder<CommandSourceStack> child = Commands.literal(name)
                    .requires(s -> CommandRegistry.checkPermissions(s, permissionLevel, permission))
                    .executes(ctx -> executor.execute(ctx));
            // 不立即挂到 root，等 build() 最后一并挂载，避免后续 child()/suggest() 的孙级丢失
            rootChildren.add(child);
            lastBranch.add(child);
        }
        return this;
    }

    /**
     * 在<b>最近一次 {@link #sub} 的节点</b>下登记一个可执行的子命令（多级树的下一层）。
     * 权限继承根命令的。
     *
     * @return 本构建器（链式）
     */
    public NodeBuilder child(String name, CommandExecutor executor) {
        return child(name, executor, rootPermLevel, rootPermNode);
    }

    /**
     * 在最近一次 {@link #sub} 的节点下登记可执行的子命令，并指定该子节点的权限。
     */
    public NodeBuilder child(String name, CommandExecutor executor, int permissionLevel, String permission) {
        if (!registerable || name == null || executor == null) {
            return this;
        }
        for (LiteralArgumentBuilder<CommandSourceStack> branch : lastBranch) {
            branch.then(Commands.literal(name)
                    .requires(s -> CommandRegistry.checkPermissions(s, permissionLevel, permission))
                    .executes(ctx -> executor.execute(ctx)));
        }
        return this;
    }

    /**
     * 在最近一次 {@link #sub} 的节点下加若干<b>仅用于 Tab 补全</b>的字面量叶子
     * （无执行器；用于展示"选择某子命令后下一位置的候选"）。
     *
     * @return 本构建器（链式）
     */
    public NodeBuilder suggest(String... candidates) {
        if (!registerable || candidates == null) {
            return this;
        }
        for (LiteralArgumentBuilder<CommandSourceStack> branch : lastBranch) {
            for (String candidate : candidates) {
                branch.then(Commands.literal(candidate)
                        .requires(s -> CommandRegistry.checkPermissions(s, rootPermLevel, rootPermNode)));
            }
        }
        return this;
    }

    /**
     * 设置命令根节点的默认执行器：当输入命令名而未匹配任何子命令时执行。
     *
     * @return 本构建器（链式）
     */
    public NodeBuilder executes(CommandExecutor executor) {
        if (!registerable || executor == null) {
            return this;
        }
        root.executes(ctx -> executor.execute(ctx));
        return this;
    }

    /** 命令名（用于 CommandRegistry 登记与重名保护）。 */
    String label() {
        return label;
    }

    /** 完成并返回根字面量，供 {@link CommandRegistry} 注册。 */
    LiteralArgumentBuilder<CommandSourceStack> build() {
        if (!assembled) {
            // 此刻所有 sub()/child()/suggest() 链式调用已结束，子级都已组好，最后统一挂载到根
            for (LiteralArgumentBuilder<CommandSourceStack> child : rootChildren) {
                root.then(child);
            }
            assembled = true;
        }
        return root;
    }

    private static List<String> split(String names) {
        List<String> out = new ArrayList<>();
        for (String n : names.split("\\|")) {
            String t = n.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }
}