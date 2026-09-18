package com.akiteam.akia.component;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Paper 风格的内置数据组件注册表。
 * <p>
 * 每个常量包装一个 Minecraft 原生 {@link net.minecraft.core.component.DataComponents 组件}，
 * 并携带准确的泛型值类型，供插件以类型安全的方式直接引用：
 * <pre>{@code
 * AkiComponents.set(stack, Components.DISPLAY_NAME, Component.literal("My Sword"));
 * AkiComponents.set(stack, Components.LORE, new ItemLore(List.of(Component.literal("line 1"))));
 * AkiComponents.remove(stack, Components.RARITY);
 * }</pre>
 * <p>
 * <b>版本说明：</b>本构建基于 Minecraft 1.21.1（NeoForge 21.1.248），原生 {@link DataComponents}
 * 尚未包含 {@code TOOLTIP_STYLE}、{@code EQUIPPABLE}、{@code GLIDER}、{@code CONSUMABLE}、
 * {@code USE_REMAINDER}（这些在更高版本的 Minecraft 中才被加入）。
 * 因此这些组件在本次未提供；如需访问版本内可用的其他组件，可用
 * {@code AkiDataComponentType.of(DataComponents.X)} 或
 * {@code AkiDataComponentType.of(ResourceLocation.parse("minecraft:xx"))} 自行包装。
 */
public final class Components {

    private Components() {
    }

    /** {@code minecraft:item_name} —— 物品的显示名称（原 {@code DataComponents.ITEM_NAME}）。 */
    public static final AkiDataComponentType<Component> DISPLAY_NAME =
            AkiDataComponentType.of(DataComponents.ITEM_NAME);

    /** {@code minecraft:lore} —— 物品的 Loot Lore 文本行列表（{@link ItemLore}）。 */
    public static final AkiDataComponentType<ItemLore> LORE =
            AkiDataComponentType.of(DataComponents.LORE);

    /** {@code minecraft:enchantments} —— 物品携带的附魔（{@link ItemEnchantments}）。 */
    public static final AkiDataComponentType<ItemEnchantments> ENCHANTMENTS =
            AkiDataComponentType.of(DataComponents.ENCHANTMENTS);

    /** {@code minecraft:stored_enchantments} —— 物品（如附魔书）储存的附魔。 */
    public static final AkiDataComponentType<ItemEnchantments> STORED_ENCHANTMENTS =
            AkiDataComponentType.of(DataComponents.STORED_ENCHANTMENTS);

    /** {@code minecraft:damage} —— 物品已损失的使用次数（整数）。 */
    public static final AkiDataComponentType<Integer> DAMAGE =
            AkiDataComponentType.of(DataComponents.DAMAGE);

    /** {@code minecraft:max_stack_size} —— 物品的最大堆叠数量（整数）。 */
    public static final AkiDataComponentType<Integer> MAX_STACK_SIZE =
            AkiDataComponentType.of(DataComponents.MAX_STACK_SIZE);

    /** {@code minecraft:fire_resistant} —— 是否免疫火焰（存在该组件即为真，值为 {@link Unit}）。 */
    public static final AkiDataComponentType<Unit> FIRE_RESISTANT =
            AkiDataComponentType.of(DataComponents.FIRE_RESISTANT);

    /** {@code minecraft:unbreakable} —— 是否不可破坏（{@link Unbreakable}，可控制是否在提示中显示）。 */
    public static final AkiDataComponentType<Unbreakable> UNBREAKABLE =
            AkiDataComponentType.of(DataComponents.UNBREAKABLE);

    /**
     * {@code minecraft:custom_data} —— 通用自定义数据（{@link CustomData}，即原始 NBT）。
     * <p>
     * 与 PDC 系统的桥接点：{@link net.minecraft.world.item.component.CustomData} 内嵌套
     * {@code akia:pdc} 子树即承载通过 {@link AkiComponents#getPersistence}/{@link AkiComponents#setPersistence}
     * 或 {@code ItemPersistentDataBridge} 写入的数据。因此通过组件 API 对该子树读写，与通过 PDC 读写完全互通。
     */
    public static final AkiDataComponentType<CustomData> CUSTOM_DATA =
            AkiDataComponentType.of(DataComponents.CUSTOM_DATA);

    /** {@code minecraft:attribute_modifiers} —— 物品提供的属性修饰符（{@link ItemAttributeModifiers}）。 */
    public static final AkiDataComponentType<ItemAttributeModifiers> ATTRIBUTE_MODIFIERS =
            AkiDataComponentType.of(DataComponents.ATTRIBUTE_MODIFIERS);

    /** {@code minecraft:rarity} —— 物品稀有度（{@link Rarity}）。 */
    public static final AkiDataComponentType<Rarity> RARITY =
            AkiDataComponentType.of(DataComponents.RARITY);

    /** {@code minecraft:repair_cost} —— 物品的附魔/修复成本（整数）。 */
    public static final AkiDataComponentType<Integer> REPAIR_COST =
            AkiDataComponentType.of(DataComponents.REPAIR_COST);
}