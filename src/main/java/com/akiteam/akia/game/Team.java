package com.akiteam.akia.game;

import com.akiteam.akia.text.NamedTextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 游戏对象：一个记分板队伍（team）的包装（对齐 Bukkit {@code Team}）。
 * <p>
 * 包装 {@code net.minecraft.world.scores.PlayerTeam}，供插件设置队伍显示名 / 前后缀 / 颜色、
 * 增删成员 entry、查询成员并注销队伍。队伍由 {@link Scoreboard#registerNewTeam} 创建。
 * <p>
 * 颜色以 {@link NamedTextColor} 表达；与原生 {@link ChatFormatting} 互相映射，透明的槽位颜色
 * （如 {@code RESET}）在 {@link #getColor} 时安全降级返回 {@code null}。
 */
public final class Team {

    private final Scoreboard scoreboard;
    private final net.minecraft.world.scores.PlayerTeam handle;

    Team(Scoreboard scoreboard, net.minecraft.world.scores.PlayerTeam handle) {
        this.scoreboard = scoreboard;
        this.handle = handle;
    }

    /** 返回被包装的原生队伍（逃生口）。 */
    public net.minecraft.world.scores.PlayerTeam getHandle() {
        return handle;
    }

    /** 队伍内部名（唯一标识）。 */
    public String getName() {
        return handle.getName();
    }

    /** 队伍显示名（纯文本）。 */
    public String getDisplayName() {
        return handle.getDisplayName().getString();
    }

    /** 设置队伍显示名。 */
    public void setDisplayName(String displayName) {
        handle.setDisplayName(Component.literal(displayName == null ? "" : displayName));
    }

    /** 队伍成员名前缀（纯文本）。 */
    public String getPrefix() {
        return handle.getPlayerPrefix().getString();
    }

    /** 设置队伍成员名前缀。 */
    public void setPrefix(String prefix) {
        handle.setPlayerPrefix(Component.literal(prefix == null ? "" : prefix));
    }

    /** 队伍成员名后缀（纯文本）。 */
    public String getSuffix() {
        return handle.getPlayerSuffix().getString();
    }

    /** 设置队伍成员名后缀。 */
    public void setSuffix(String suffix) {
        handle.setPlayerSuffix(Component.literal(suffix == null ? "" : suffix));
    }

    /** 队伍颜色；队伍未设置颜色（RESET）返回 {@code null}。 */
    public NamedTextColor getColor() {
        Integer rgb = handle.getColor().getColor();
        return rgb == null ? null : NamedTextColor.fromRgb(rgb);
    }

    /** 设置队伍颜色；传入 {@code null} 表示重置为透明。 */
    public void setColor(NamedTextColor color) {
        if (color == null) {
            handle.setColor(ChatFormatting.RESET);
            return;
        }
        int rgb = color.toTextColor().getValue();
        for (ChatFormatting cf : ChatFormatting.values()) {
            Integer c = cf.getColor();
            if (c != null && c == rgb) {
                handle.setColor(cf);
                return;
            }
        }
        handle.setColor(ChatFormatting.RESET);
    }

    /** 把某 entry 加入队伍。 */
    public void addEntry(String entry) {
        if (entry != null) {
            scoreboard.getHandle().addPlayerToTeam(entry, handle);
        }
    }

    /** 把某 entry 移出队伍。 */
    public void removeEntry(String entry) {
        if (entry != null) {
            try {
                scoreboard.getHandle().removePlayerFromTeam(entry, handle);
            } catch (IllegalStateException ignored) {
                // entry 不在本队伍时原生会抛异常，安全降级忽略
            }
        }
    }

    /** 某 entry 是否在该队伍中。 */
    public boolean hasEntry(String entry) {
        return entry != null && handle.getPlayers().contains(entry);
    }

    /** 队伍全部成员 entry 名。 */
    public List<String> getEntries() {
        return List.copyOf(handle.getPlayers());
    }

    /** 队伍成员数。 */
    public int getSize() {
        return handle.getPlayers().size();
    }

    /** 注销该队伍（从记分板移除并广播）。 */
    public void unregister() {
        scoreboard.getHandle().removePlayerTeam(handle);
    }

    @Override
    public String toString() {
        return "Team{" + getName() + ", size=" + getSize() + "}";
    }
}