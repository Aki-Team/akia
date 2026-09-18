package com.akiteam.akia.persistence.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * PDC 原始的 Java 值 ⟷ Minecraft NBT Tag 的类型注册表（参考 Paper 的
 * {@code PersistentDataTypeRegistry}）。
 * <p>
 * Akia 的容器内部保存的是"原始 Java 值"（String / Integer / Long / Double /
 * Boolean / UUID / byte[] / int[] / CompoundTag / List）。当需要把容器整体写入
 * 实体或物品的 NBT 时，由本类把这些原始值分别转成对应的 NBT Tag；读取时反向还原。
 * <p>
 * 支持的类型一览：
 * <ul>
 *     <li>{@code String → StringTag}、{@code int → IntTag}、{@code long → LongTag}</li>
 *     <li>{@code double → DoubleTag}、{@code boolean → ByteTag(1|0)}</li>
 *     <li>{@code UUID → CompoundTag{int array 的两个 long}}、{@code byte[] / int[] → 对应数组 Tag}</li>
 *     <li>{@code CompoundTag → 自身}（嵌套容器）、{@code List → ListTag}（逐元素递归）</li>
 * </ul>
 */
public final class AkiPersistentDataTypeRegistry {

    /** 全局共享实例（本类无状态、线程安全）。 */
    public static final AkiPersistentDataTypeRegistry INSTANCE = new AkiPersistentDataTypeRegistry();

    private AkiPersistentDataTypeRegistry() {
    }

    /**
     * 把一个 PDC 原始 Java 值转成 NBT Tag。
     *
     * @param value PDC 内部存的原始值
     * @return 对应的 NBT {@link Tag}
     * @throws IllegalArgumentException 遇到不支持的原始类型时
     */
    public Tag primitiveToTag(Object value) {
        if (value instanceof String s) {
            return StringTag.valueOf(s);
        }
        if (value instanceof Integer i) {
            return IntTag.valueOf(i);
        }
        if (value instanceof Long l) {
            return LongTag.valueOf(l);
        }
        if (value instanceof Double d) {
            return DoubleTag.valueOf(d);
        }
        if (value instanceof Boolean b) {
            return ByteTag.valueOf(b ? (byte) 1 : (byte) 0);
        }
        if (value instanceof UUID id) {
            return uuidToTag(id);
        }
        if (value instanceof byte[] bytes) {
            return new ByteArrayTag(bytes);
        }
        if (value instanceof int[] ints) {
            return new IntArrayTag(ints);
        }
        if (value instanceof CompoundTag tag) {
            return tag;
        }
        if (value instanceof List<?> list) {
            ListTag result = new ListTag();
            for (Object element : list) {
                result.add(primitiveToTag(element));
            }
            return result;
        }
        throw new IllegalArgumentException("Unsupported PDC value type: " + (value == null ? "null" : value.getClass().getName()));
    }

    /**
     * 把一个 NBT Tag 还原为 PDC 原始 Java 值。
     *
     * @param tag 从存储中读出的内容
     * @return 还原后的原始值
     * @throws IllegalArgumentException 遇到不支持的 Tag 类型时
     */
    public Object tagToPrimitive(Tag tag) {
        if (tag instanceof StringTag t) {
            return t.getAsString();
        }
        if (tag instanceof IntTag t) {
            return t.getAsInt();
        }
        if (tag instanceof LongTag t) {
            return t.getAsLong();
        }
        if (tag instanceof DoubleTag t) {
            return t.getAsDouble();
        }
        if (tag instanceof ByteTag t) {
            return t.getAsByte() != 0;
        }
        if (tag instanceof CompoundTag t && isUuidTag(t)) {
            return uuidFromTag(t);
        }
        if (tag instanceof ByteArrayTag t) {
            return t.getAsByteArray();
        }
        if (tag instanceof IntArrayTag t) {
            return t.getAsIntArray();
        }
        if (tag instanceof CompoundTag t) {
            return t;
        }
        if (tag instanceof ListTag t) {
            List<Object> result = new ArrayList<>(t.size());
            for (int i = 0; i < t.size(); i++) {
                result.add(tagToPrimitive(t.get(i)));
            }
            return result;
        }
        throw new IllegalArgumentException("Unsupported NBT tag type: " + tag.getClass().getName());
    }

    /** UUID → {@code CompoundTag{m: M, l: L}} 两个长整型，避免与普通复合标签冲突。 */
    private static Tag uuidToTag(UUID uuid) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("m", uuid.getMostSignificantBits());
        tag.putLong("l", uuid.getLeastSignificantBits());
        return tag;
    }

    /** 判定是否为 UUID 专用结构：恰有两个长整型字段 {@code m}/{@code l}。 */
    private static boolean isUuidTag(CompoundTag tag) {
        return tag.size() == 2
                && tag.contains("m", Tag.TAG_LONG)
                && tag.contains("l", Tag.TAG_LONG);
    }

    /** 从 {@code CompoundTag{m: M, l: L}} 还原 UUID。 */
    private static UUID uuidFromTag(CompoundTag tag) {
        return new UUID(tag.getLong("m"), tag.getLong("l"));
    }
}