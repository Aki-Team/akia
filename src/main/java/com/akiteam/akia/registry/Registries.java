package com.akiteam.akia.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Paper 风格的内建注册表常量，映射到 Minecraft 原生 {@link Registries} 的注册表键。
 * <p>
 * 每个常量都是声明式的 {@link AkiRegistry}，可在类加载期安全创建，运行时按需解析底层注册表：
 * <pre>{@code
 * Optional<Item> o = Registries.ITEM.get(AkiRegistryKey.of("minecraft:diamond"));
 * boolean inSound = Registries.SOUND_EVENT.contains(AkiRegistryKey.of("minecraft:block.anvil.land"));
 * Registries.ENTITY_TYPE.streamKeys().limit(10).forEach(k -> ...);
 * }</pre>
 * <p>
 * <b>版本说明：</b>任务清单中的 {@code GAMEMODE (GameType)} 在本版 Minecraft 1.21.1 中不存在对应的原生注册表
 * （{@code GameType} 是普通枚举，不是注册表），因此从常量集中移除；其余 13 个常量均有原生注册表。
 * {@link #DIMENSION_TYPE} / {@link #BIOME} / {@link #STRUCTURE} 是数据包注册表，仅在服务器启动后可查询。
 */
public final class Registries {

    private Registries() {
    }

    /** {@code minecraft:item} —— 物品注册表。 */
    public static final AkiRegistry<Item> ITEM = AkiRegistry.of(net.minecraft.core.registries.Registries.ITEM);

    /** {@code minecraft:block} —— 方块注册表。 */
    public static final AkiRegistry<Block> BLOCK = AkiRegistry.of(net.minecraft.core.registries.Registries.BLOCK);

    /** {@code minecraft:entity_type} —— 实体类型注册表。 */
    public static final AkiRegistry<EntityType<?>> ENTITY_TYPE = AkiRegistry.of(net.minecraft.core.registries.Registries.ENTITY_TYPE);

    /** {@code minecraft:enchantment} —— 附魔注册表。 */
    public static final AkiRegistry<Enchantment> ENCHANTMENT = AkiRegistry.of(net.minecraft.core.registries.Registries.ENCHANTMENT);

    /** {@code minecraft:attribute} —— 实体属性注册表。 */
    public static final AkiRegistry<Attribute> ATTRIBUTE = AkiRegistry.of(net.minecraft.core.registries.Registries.ATTRIBUTE);

    /** {@code minecraft:potion} —— 药水注册表。 */
    public static final AkiRegistry<Potion> POTION = AkiRegistry.of(net.minecraft.core.registries.Registries.POTION);

    /** {@code minecraft:mob_effect} —— 状态效果注册表。 */
    public static final AkiRegistry<MobEffect> MOB_EFFECT = AkiRegistry.of(net.minecraft.core.registries.Registries.MOB_EFFECT);

    /** {@code minecraft:sound_event} —— 音效注册表。 */
    public static final AkiRegistry<SoundEvent> SOUND_EVENT = AkiRegistry.of(net.minecraft.core.registries.Registries.SOUND_EVENT);

    /** {@code minecraft:particle_type} —— 粒子类型注册表。 */
    public static final AkiRegistry<ParticleType<?>> PARTICLE_TYPE = AkiRegistry.of(net.minecraft.core.registries.Registries.PARTICLE_TYPE);

    /** {@code minecraft:dimension_type} —— 维度类型注册表（数据包，服务器启动后可查）。 */
    public static final AkiRegistry<DimensionType> DIMENSION_TYPE = AkiRegistry.of(net.minecraft.core.registries.Registries.DIMENSION_TYPE);

    /** {@code minecraft:biome} —— 生物群系注册表（数据包，服务器启动后可查）。 */
    public static final AkiRegistry<Biome> BIOME = AkiRegistry.of(net.minecraft.core.registries.Registries.BIOME);

    /** {@code minecraft:structure} —— 结构注册表（数据包，服务器启动后可查）。 */
    public static final AkiRegistry<Structure> STRUCTURE = AkiRegistry.of(net.minecraft.core.registries.Registries.STRUCTURE);

    /** {@code minecraft:data_component_type} —— 数据组件类型注册表。 */
    public static final AkiRegistry<DataComponentType<?>> DATA_COMPONENT_TYPE =
            AkiRegistry.of(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE);
}