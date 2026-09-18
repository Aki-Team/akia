package com.akiteam.akia.persistence.bridge;

import com.akiteam.akia.persistence.AkiPersistentContainer;
import com.akiteam.akia.persistence.impl.AkiAdapterContextImpl;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 在物品（{@link ItemStack}）上挂接 PDC 的桥接层。
 * <p>
 * 底层使用 Minecraft 1.21 的 DataComponent 系统：所有数据被放进内置的
 * {@link DataComponents#CUSTOM_DATA}（{@link CustomData}）组件的一个
 * {@code akia:pdc} 内层标签下，写入后随物品本身的组件一起参与存档。
 * 无需注册自定义 DataComponentType，即可在任意物品上持久化数据。
 */
public final class ItemPersistentDataBridge {

    /** 物品 CUSTOM_DATA 组件中的内层键名。 */
    private static final String PDC_TAG_KEY = "akia:pdc";

    private static final DataComponentType<CustomData> TYPE = DataComponents.CUSTOM_DATA;

    private ItemPersistentDataBridge() {
    }

    /**
     * 读取物品上挂接的 PDC 容器（副本）。
     *
     * @param stack 目标物品
     * @return 物品当前 PDC 的副本（不会为 {@code null}）
     */
    public static AkiPersistentContainer get(ItemStack stack) {
        CustomData customData = stack.getComponents().get(TYPE);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        CompoundTag pdc = tag.getCompound(PDC_TAG_KEY);

        AkiPersistentContainer container = AkiAdapterContextImpl.INSTANCE.newPersistentDataContainer();
        container.fromNbt(pdc);
        return container;
    }

    /**
     * 把 PDC 容器写回物品（以 CUSTOM_DATA 组件的形式，随物品存档）。
     *
     * @param stack     目标物品
     * @param container 要保存的容器
     */
    public static void set(ItemStack stack, AkiPersistentContainer container) {
        CustomData customData = stack.getComponents().get(TYPE);
        CompoundTag root = customData != null ? customData.copyTag() : new CompoundTag();
        root.put(PDC_TAG_KEY, container.toNbt());
        stack.set(TYPE, CustomData.of(root));
    }
}