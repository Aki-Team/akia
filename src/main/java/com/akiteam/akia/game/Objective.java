package com.akiteam.akia.game;

import net.minecraft.network.chat.Component;

/**
 * 游戏对象：一个记分板目标（objective）的包装（对齐 Bukkit {@code Objective}）。
 * <p>
 * 包装 {@code net.minecraft.world.scores.Objective}，供插件注册/查询目标、设置显示槽与渲染类型、
 * 读写某 entry 的分数、注销目标。目标由 {@link Scoreboard#registerNewObjective} 创建。
 */
public final class Objective {

    private final Scoreboard scoreboard;
    private final net.minecraft.world.scores.Objective handle;

    Objective(Scoreboard scoreboard, net.minecraft.world.scores.Objective handle) {
        this.scoreboard = scoreboard;
        this.handle = handle;
    }

    /** 返回被包装的原生目标（逃生口）。 */
    public net.minecraft.world.scores.Objective getHandle() {
        return handle;
    }

    /** 该目标所属的封装记分板（供同包 {@link Score} 做复位/存在性查询）。 */
    Scoreboard getScoreboard() {
        return scoreboard;
    }

    /** 目标内部名（唯一标识，如 {@code "rank"}）。 */
    public String getName() {
        return handle.getName();
    }

    /** 客户端显示标题（纯文本）。 */
    public String getDisplayName() {
        return handle.getDisplayName().getString();
    }

    /** 设置客户端显示标题。 */
    public void setDisplayName(String displayName) {
        handle.setDisplayName(Component.literal(displayName == null ? "" : displayName));
    }

    /**
     * 用原生组件设置客户端显示标题（支持完整 {@code Style}：颜色/加粗/下划线等）。
     *
     * @param displayName 原生组件；为 {@code null} 时按空字符串处理
     */
    public void setDisplayName(net.minecraft.network.chat.Component displayName) {
        handle.setDisplayName(displayName != null ? displayName : net.minecraft.network.chat.Component.literal(""));
    }

    /**
     * 用框架 {@code TextComponent} 链式设置彩色/带格式的客户端显示标题。
     * <p>
     * 例如：{@code rank.setDisplayName(TextComponent.text("排行榜").color(NamedTextColor.AQUA).bold(true));}
     *
     * @param displayName 框架文本组件；为 {@code null} 时按空字符串处理
     */
    public void setDisplayName(com.akiteam.akia.text.TextComponent displayName) {
        net.minecraft.network.chat.Component comp =
                displayName == null ? net.minecraft.network.chat.Component.literal("") : displayName.build();
        handle.setDisplayName(comp);
    }

    /** 目标判据名（如 {@code "dummy"}、{@code "health"}）。 */
    public String getCriteria() {
        return handle.getCriteria().getName();
    }

    /** 目标是否可写（判据是否为只读，如 {@code health} 是只读不可手动设分）。 */
    public boolean isModifiable() {
        return !handle.getCriteria().isReadOnly();
    }

    /** 当前挂载该目标的显示槽；未挂载到任何常用槽位返回 {@code null}。 */
    public DisplaySlot getDisplaySlot() {
        net.minecraft.world.scores.Objective nativeObj = handle;
        for (net.minecraft.world.scores.DisplaySlot slot : net.minecraft.world.scores.DisplaySlot.values()) {
            DisplaySlot mapped = DisplaySlot.fromNative(slot);
            if (mapped != null && scoreboard.getHandle().getDisplayObjective(slot) == nativeObj) {
                return mapped;
            }
        }
        return null;
    }

    /** 把该目标挂载到指定显示槽（如 {@code SIDEBAR} 侧边栏）。 */
    public void setDisplaySlot(DisplaySlot slot) {
        if (slot != null) {
            scoreboard.getHandle().setDisplayObjective(slot.toNative(), handle);
        }
    }

    /** 当前渲染类型（数字 / 心形）。 */
    public RenderType getRenderType() {
        return RenderType.fromNative(handle.getRenderType());
    }

    /** 设置渲染类型。 */
    public void setRenderType(RenderType renderType) {
        if (renderType != null) {
            handle.setRenderType(renderType.toNative());
        }
    }

    /** 取得某 entry 在该目标下的分数（读写访问器）。 */
    public Score getScore(String entry) {
        if (entry == null) {
            return null;
        }
        net.minecraft.world.scores.ScoreAccess access = scoreboard.getOrCreatePlayerScore(entry, handle);
        return new Score(this, entry, access);
    }

    /** 注销该目标（从记分板移除并停止追踪）。 */
    public void unregister() {
        scoreboard.getHandle().removeObjective(handle);
    }

    @Override
    public String toString() {
        return "Objective{" + getName() + ", criteria=" + getCriteria() + "}";
    }
}