package com.akiteam.akiloader.component;

import net.minecraft.core.component.DataComponentPatch;

/**
 * 数据组件补丁（{@link DataComponentPatch}）的可读流式构造器。
 * <p>
 * 补丁描述的是"对一组组件的增/删/改"，可在不改动原物品的前提下生成一件新物品，供
 * {@link AkiComponents#create(ItemStack, DataComponentPatch)} 使用：
 * <pre>{@code
 * DataComponentPatch patch = DataComponentPatchBuilder.builder()
 *         .set(Components.RARITY, Rarity.EPIC)
 *         .set(Components.DISPLAY_NAME, Component.literal("Epic Blade"))
 *         .remove(Components.DAMAGE)
 *         .build();
 * ItemStack newItem = AkiComponents.create(origin, patch);
 * }</pre>
 * 本类无共享状态，仅对本线程内构造的 {@code builder} 进行链式追加。
 */
public final class DataComponentPatchBuilder {

    private final DataComponentPatch.Builder builder;

    private DataComponentPatchBuilder() {
        this.builder = DataComponentPatch.builder();
    }

    /**
     * 创建一个新的补丁构造器。
     *
     * @return 空补丁构造器
     */
    public static DataComponentPatchBuilder builder() {
        return new DataComponentPatchBuilder();
    }

    /**
     * 追加"设置一个组件"操作。
     *
     * @param type  要设置的组件类型
     * @param value 组件值，不要为 {@code null}
     * @param <T>   组件值的类型
     * @return 本构造器（支持链式调用）
     */
    public <T> DataComponentPatchBuilder set(AkiDataComponentType<T> type, T value) {
        builder.set(type.getDelegate(), value);
        return this;
    }

    /**
     * 追加"移除一个组件"操作。
     *
     * @param type 要移除的组件类型
     * @return 本构造器（支持链式调用）
     */
    public DataComponentPatchBuilder remove(AkiDataComponentType<?> type) {
        builder.remove(type.getDelegate());
        return this;
    }

    /**
     * 结束构造并生成不可变的 {@link DataComponentPatch}。
     *
     * @return 组装好的补丁
     */
    public DataComponentPatch build() {
        return builder.build();
    }
}