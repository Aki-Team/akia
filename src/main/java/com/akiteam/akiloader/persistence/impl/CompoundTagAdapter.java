package com.akiteam.akiloader.persistence.impl;

import com.akiteam.akiloader.persistence.AkiKey;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

/**
 * {@link Map AkiKey→原始值} 与 Minecraft {@link CompoundTag} 之间的双向转换器。
 * <p>
 * 每个 {@link AkiKey} 先经 {@link AkiKeySerializer} 编码为字符串作为 NBT 键名，
 * 值再经 {@link AkiPersistentDataTypeRegistry} 转为对应的 NBT Tag；反向还原同理。
 * 遇到无法解析的键名时跳过该项，保证外部数据不会让容器崩溃。
 */
public final class CompoundTagAdapter {

    private static final AkiPersistentDataTypeRegistry REGISTRY = AkiPersistentDataTypeRegistry.INSTANCE;

    private CompoundTagAdapter() {
    }

    /**
     * 把 PDC 数据映射序列化为 {@link CompoundTag}。
     *
     * @param data AkiKey → 原始值 的数据映射
     * @return 序列化结果，不会为 {@code null}
     */
    public static CompoundTag toCompoundTag(Map<AkiKey, Object> data) {
        CompoundTag root = new CompoundTag();
        for (Map.Entry<AkiKey, Object> entry : data.entrySet()) {
            root.put(AkiKeySerializer.encode(entry.getKey()), REGISTRY.primitiveToTag(entry.getValue()));
        }
        return root;
    }

    /**
     * 从 {@link CompoundTag} 还原 PDC 数据映射。
     *
     * @param tag 读取到的 {@link CompoundTag}
     * @return AkiKey → 原始值 的数据映射，不会为 {@code null}
     */
    public static Map<AkiKey, Object> fromCompoundTag(CompoundTag tag) {
        Map<AkiKey, Object> data = new LinkedHashMap<>();
        for (String name : tag.getAllKeys()) {
            try {
                AkiKey key = AkiKeySerializer.decode(name);
                data.put(key, REGISTRY.tagToPrimitive(tag.get(name)));
            } catch (IllegalArgumentException e) {
                // 非法的键名 / 不支持的值类型：跳过该条目，不影响其余数据
            }
        }
        return data;
    }
}