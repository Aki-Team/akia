package com.akiteam.akia.game;

/**
 * 记分板目标渲染类型（对齐 Bukkit {@code RenderType}）。
 * <p>
 * 映射到原生 {@code net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType}：
 * {@code INTEGER}（显示数字）与 {@code HEARTS}（显示心形）。各常量持有原生类型 id
 * （{@code "integer" / "hearts"}）。
 */
public enum RenderType {

    INTEGER("integer"),
    HEARTS("hearts");

    private final String id;

    RenderType(String id) {
        this.id = id;
    }

    /** 原生类型名（如 {@code "integer"}）。 */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    /** 映射到原生渲染类型。 */
    public net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType toNative() {
        return switch (this) {
            case INTEGER -> net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER;
            case HEARTS -> net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.HEARTS;
        };
    }

    /** 从原生渲染类型反向映射。 */
    public static RenderType fromNative(net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType type) {
        return switch (type) {
            case INTEGER -> INTEGER;
            case HEARTS -> HEARTS;
        };
    }
}