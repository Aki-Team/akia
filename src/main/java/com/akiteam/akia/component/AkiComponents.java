package com.akiteam.akia.component;

import com.akiteam.akia.persistence.AkiPersistentContainer;
import com.akiteam.akia.persistence.bridge.ItemPersistentDataBridge;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;

/**
 * 操作物品（{@link ItemStack}）数据组件的静态工具类。
 * <p>
 * 所有方法均为静态方法，插件开发者可直接调用，无需继承任何类，也不需要持有实例：
 * <pre>{@code
 * AkiComponents.set(stack, Components.DISPLAY_NAME,
 *         Component.literal("My Sword").withStyle(s -> s.withBold(true)));
 * int damage = AkiComponents.getOrDefault(stack, Components.DAMAGE, 0);
 * AkiComponents.remove(stack, Components.RARITY);
 * boolean enchanted = AkiComponents.has(stack, Components.ENCHANTMENTS);
 * }</pre>
 * 底层直接操作原生 {@link ItemStack} 的组件映射（可变操作即时生效；读取返回快照值），
 * 与 Minecraft 1.21 的数据组件体系保持一致，内部不会复制整个物品（除显式 {@code copy} / {@code create} 外）。
 */
public final class AkiComponents {

    private AkiComponents() {
    }

    /**
     * 读取物品上某个组件值，不存在时返回 {@code null}（若该类型声明了默认值则返回默认值）。
     *
     * @param stack 目标物品，不要为 {@code null}
     * @param type  要读取的组件类型
     * @param <T>   组件值的类型
     * @return 组件值；不存在且无默认值时返回 {@code null}
     */
    public static <T> T get(ItemStack stack, AkiDataComponentType<T> type) {
        return stack.get(type.getDelegate());
    }

    /**
     * 读取物品上某个组件值，不存在或为 {@code null} 时返回给定的默认值。
     *
     * @param stack        目标物品，不要为 {@code null}
     * @param type         要读取的组件类型
     * @param defaultValue 不存在时的默认值
     * @param <T>          组件值的类型
     * @return 组件值或默认值
     */
    public static <T> T getOrDefault(ItemStack stack, AkiDataComponentType<T> type, T defaultValue) {
        T value = stack.get(type.getDelegate());
        return value != null ? value : defaultValue;
    }

    /**
     * 在物品上设置（覆盖）一个组件值。
     *
     * @param stack 目标物品，不要为 {@code null}
     * @param type  要设置的组件类型
     * @param value 组件值，不要为 {@code null}（原生 API 不允许置空）
     * @param <T>   组件值的类型
     */
    public static <T> void set(ItemStack stack, AkiDataComponentType<T> type, T value) {
        stack.set(type.getDelegate(), value);
    }

    /**
     * 从物品上移除一个组件（隐式回到该类型的默认值）。
     *
     * @param stack 目标物品，不要为 {@code null}
     * @param type  要移除的组件类型；不存在时静默忽略
     */
    public static void remove(ItemStack stack, AkiDataComponentType<?> type) {
        stack.remove(type.getDelegate());
    }

    /**
     * 判断物品上是否存在某个组件（即该组件不是默认值）。
     *
     * @param stack 目标物品，不要为 {@code null}
     * @param type  要判断的组件类型
     * @return {@code true} 表示该组件存在且非默认值
     */
    public static boolean has(ItemStack stack, AkiDataComponentType<?> type) {
        return stack.has(type.getDelegate());
    }

    /**
     * 深复制一件物品，保留其全部组件与数量。
     *
     * @param stack 源物品
     * @return 与原物品内容完全一致的全新 {@link ItemStack} 实例
     */
    public static ItemStack copy(ItemStack stack) {
        return stack.copy();
    }

    /**
     * 在副本上应用一个组件补丁，生成一件新物品（不修改原物品）。
     * <p>
     * 与修改原物品（{@link #set} / {@link #remove}）不同，此处用于"基于既有物品、批量调整组件后得到新物品"，
     * 适合需要保留原物品不变、产出变体的场景。补丁可用 {@link DataComponentPatchBuilder} 构建：
     * <pre>{@code
     * DataComponentPatch patch = DataComponentPatchBuilder.builder()
     *         .set(Components.RARITY, Rarity.EPIC)
     *         .remove(Components.DAMAGE)
     *         .build();
     * ItemStack epic = AkiComponents.create(stack, patch);
     * }</pre>
     *
     * @param stack 源物品，不要为 {@code null}
     * @param patch 要应用的组件补丁，不要为 {@code null}
     * @return 应用补丁后的新 {@link ItemStack}（原物品不受影响）
     */
    public static ItemStack create(ItemStack stack, DataComponentPatch patch) {
        ItemStack copy = stack.copy();
        copy.applyComponents(patch);
        return copy;
    }

    /**
     * 读取物品上与 PDC 桥接的持久化容器（副本）。
     * <p>
     * 底层委托给 {@link ItemPersistentDataBridge#get(ItemStack)}：读取
     * {@link Components#CUSTOM_DATA} 组件中 {@code akia:pdc} 子树的内容。
     * 也就是说，通过本方法 / 组件 API 写入的数据，与通过 PDC 系统写入的数据完全互通。
     *
     * @param stack 目标物品，不要为 {@code null}
     * @return 物品当前 PDC 的副本（不会为 {@code null}）
     */
    public static AkiPersistentContainer getPersistence(ItemStack stack) {
        return ItemPersistentDataBridge.get(stack);
    }

    /**
     * 把 PDC 容器写回物品（以 {@link Components#CUSTOM_DATA} 组件形式，随物品存档）。
     * <p>
     * 底层委托给 {@link ItemPersistentDataBridge#set(ItemStack, AkiPersistentContainer)}。
     *
     * @param stack     目标物品，不要为 {@code null}
     * @param container 要保存的容器
     */
    public static void setPersistence(ItemStack stack, AkiPersistentContainer container) {
        ItemPersistentDataBridge.set(stack, container);
    }
}