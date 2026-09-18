package com.akiteam.akia.persistence;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;

/**
 * 持久化数据容器：插件在实体 / 物品 / 对象上存取自定义数据的统一接口
 * （参考 Paper 的 {@code PersistentDataContainer}）。
 * <p>
 * 内部以 {@link AkiKey} 为键、以某类型的"原始值"为值存储数据：
 * <ul>
 *     <li>{@link #set} 存入一个复杂值（用该类型的 {@link AkiDataType#toPrimitive} 转换后存储）；</li>
 *     <li>{@link #get} 读回复杂值（用 {@link AkiDataType#fromPrimitive} 还原）。</li>
 * </ul>
 * 容器可序列化为 Minecraft 的 {@link CompoundTag}（{@link #toNbt}），并在需要时从
 * {@link CompoundTag} 恢复（{@link #fromNbt}），从而借助实体 / 物品的存档机制实现持久化。
 */
public interface AkiPersistentContainer {

    /**
     * 存入一个键值对。已存在的同键数据会被覆盖。
     *
     * @param key   数据键
     * @param type  数据类型
     * @param value 要存入的复杂值，不要为 {@code null}
     * @param <P>   原始类型
     * @param <C>   复杂类型
     */
    <P, C> void set(AkiKey key, AkiDataType<P, C> type, C value);

    /**
     * 移除一个键。不存在时静默忽略。
     *
     * @param key 要移除的数据键
     */
    void remove(AkiKey key);

    /**
     * 判断某个键是否存在。
     *
     * @param key 数据键
     * @return {@code true} 表示存在
     */
    boolean has(AkiKey key);

    /**
     * 读回某个键对应的复杂值。
     *
     * @param key  数据键
     * @param type 数据类型（应与 {@link #set} 时一致）
     * @return 复杂值；键不存在或类型不匹配时返回 {@code null}
     * @param <P> 原始类型
     * @param <C> 复杂类型
     */
    <P, C> C get(AkiKey key, AkiDataType<P, C> type);

    /**
     * 读回某个键对应的复杂值，不存在或类型不匹配时返回给定的默认值。
     *
     * @param key          数据键
     * @param type         数据类型
     * @param defaultValue 默认值
     * @return 复杂值或默认值
     * @param <P> 原始类型
     * @param <C> 复杂类型
     */
    <P, C> C getOrDefault(AkiKey key, AkiDataType<P, C> type, C defaultValue);

    /**
     * 返回当前所有数据键的不可变快照。
     *
     * @return 键集合
     */
    Set<AkiKey> getKeys();

    /**
     * 容器是否为空。
     *
     * @return {@code true} 表示没有任何数据
     */
    boolean isEmpty();

    /**
     * 返回容器中数据项的数量。
     *
     * @return 数据项数量
     */
    int getSize();

    /**
     * 把本容器的全部数据复制到另一个容器。
     *
     * @param other   目标容器
     * @param replace 为 {@code true} 时覆盖目标容器中已有的同名键；为 {@code false} 时保留目标已有值
     */
    void copyTo(AkiPersistentContainer other, boolean replace);

    /**
     * 序列化为 Minecraft 的 {@link CompoundTag}。
     * <p>
     * 生成的结构可保存到实体 / 物品的存档标签中，实现真正的持久化。
     *
     * @return 当前容器的 NBT 表示
     */
    CompoundTag toNbt();

    /**
     * 用 {@link CompoundTag} 的内容覆盖本容器（通常是 {@link #toNbt} 的逆操作）。
     * <p>
     * 会先清空本容器，再装入给定标签里的全部数据。
     *
     * @param tag 从存储中读回的标签
     */
    void fromNbt(CompoundTag tag);
}