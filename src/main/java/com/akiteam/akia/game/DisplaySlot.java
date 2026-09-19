package com.akiteam.akia.game;

/**
 * 记分板显示槽位（对齐 Bukkit {@code DisplaySlot}）。
 * <p>
 * 映射到原生 {@code net.minecraft.world.scores.DisplaySlot}，为插件提供
 * {@code PLAYER_LIST / SIDEBAR / BELOW_NAME} 三个常用槽位。每个枚举常量持有
 * 原生槽位与字符串 id（{@code "list" / "sidebar" / "below_name"}）。
 * <p>
 * 因与原生同名，本类不 import 原生 {@code DisplaySlot}，一律使用全限定名
 * {@code net.minecraft.world.scores.DisplaySlot}。
 */
public enum DisplaySlot {

    PLAYER_LIST("list"),
    SIDEBAR("sidebar"),
    BELOW_NAME("below_name");

    private final String id;

    DisplaySlot(String id) {
        this.id = id;
    }

    /** 原生槽位名（如 {@code "sidebar"}）。 */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    /** 映射到原生 {@code net.minecraft.world.scores.DisplaySlot}。 */
    public net.minecraft.world.scores.DisplaySlot toNative() {
        return switch (this) {
            case PLAYER_LIST -> net.minecraft.world.scores.DisplaySlot.LIST;
            case SIDEBAR -> net.minecraft.world.scores.DisplaySlot.SIDEBAR;
            case BELOW_NAME -> net.minecraft.world.scores.DisplaySlot.BELOW_NAME;
        };
    }

    /** 从原生槽位反向映射；未知槽位（如团队颜色槽）返回 {@code null}。 */
    public static DisplaySlot fromNative(net.minecraft.world.scores.DisplaySlot slot) {
        if (slot == net.minecraft.world.scores.DisplaySlot.LIST) {
            return PLAYER_LIST;
        }
        if (slot == net.minecraft.world.scores.DisplaySlot.SIDEBAR) {
            return SIDEBAR;
        }
        if (slot == net.minecraft.world.scores.DisplaySlot.BELOW_NAME) {
            return BELOW_NAME;
        }
        return null;
    }
}