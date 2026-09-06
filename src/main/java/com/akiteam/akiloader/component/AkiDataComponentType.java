package com.akiteam.akiloader.component;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * AkiLoader 对 Minecraft 1.21 {@link DataComponentType} 的类型安全包装。
 * <p>
 * 数据组件（Data Component）是 Minecraft 1.20.5+ 取代传统 NBT "attribute / display.Tag" 的物品属性系统：
 * 每个物品持有一组 {@link DataComponentType 类型}→{@code 值} 的组件映射，读写通过强类型进行。
 * 本类把一个原生 {@link DataComponentType} 封装为不可变对象，供 {@link AkiComponents} 与
 * {@link Components} 使用，避免插件开发者直接操作过于底层的原生类型。
 * <p>
 * 构造后的实例不可变、无共享可变状态，可安全地在多个线程间共享（类型系统只暴露读取，不做修改）。
 *
 * @param <T> 该组件值的类型（如 {@code Component}、{@code Integer}、{@code ItemEnchantments} 等）
 */
public final class AkiDataComponentType<T> {

    /** 被包装的原生组件类型；恒非空。 */
    private final DataComponentType<T> delegate;

    /** 已知时记录其在注册表中的键；通过 {@link #of(ResourceLocation)} 构造时才会有值。 */
    private final ResourceLocation id;

    private AkiDataComponentType(DataComponentType<T> delegate, ResourceLocation id) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.id = id;
    }

    /**
     * 用原生 {@link DataComponentType} 包装为 {@link AkiDataComponentType}。
     *
     * @param type  原生组件类型，不要为 {@code null}
     * @param <T>   组件值的类型
     * @return 包装后的类型；当 {@code type} 为 {@code null} 时抛出 {@link NullPointerException}
     */
    public static <T> AkiDataComponentType<T> of(DataComponentType<T> type) {
        return new AkiDataComponentType<>(type, null);
    }

    /**
     * 按 {@link ResourceLocation} 从数据组件注册表解析并包装一个组件类型。
     * <p>
     * 例如 {@code of(ResourceLocation.parse("minecraft:custom_data"))}。
     *
     * @param location 组件的注册键，不要为 {@code null}
     * @return 包装后的类型（含其注册键）；当该键不存在于注册表时抛出 {@link IllegalArgumentException}
     */
    public static AkiDataComponentType<?> of(ResourceLocation location) {
        Objects.requireNonNull(location, "location");
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(location);
        if (type == null) {
            throw new IllegalArgumentException("Unknown data component type: " + location);
        }
        return new AkiDataComponentType<>(type, location);
    }

    /**
     * 取回被包装的原生 {@link DataComponentType}，用于直接操作 {@code ItemStack} 底层 API。
     *
     * @return 原生组件类型（恒非空）
     */
    public DataComponentType<T> getDelegate() {
        return delegate;
    }

    /**
     * 返回该组件在注册表中的键（若已知）。
     *
     * @return 注册键；通过 {@link #of(DataComponentType)} 构造时通常为空
     */
    public Optional<ResourceLocation> key() {
        return Optional.ofNullable(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AkiDataComponentType<?> that)) {
            return false;
        }
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return "AkiDataComponentType{" + (id != null ? id : delegate) + "}";
    }
}