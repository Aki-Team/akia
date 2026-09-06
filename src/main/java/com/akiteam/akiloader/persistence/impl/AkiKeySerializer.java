package com.akiteam.akiloader.persistence.impl;

import com.akiteam.akiloader.persistence.AkiKey;

/**
 * AkiKey 与字符串之间的序列化（参考 Paper 的 {@code NamespacedKey} 格式化）。
 * <p>
 * 序列化格式为 {@code namespace:key}（与 {@link AkiKey#toString()} 一致），
 * 直接作为 NBT / DataComponent 存储中的键名使用。由于 {@link AkiKey} 的 key
 * 部分不允许包含冒号，反向拆分时只需按第一个冒号切分即可，不会歧义。
 */
public final class AkiKeySerializer {

    private static final char SEPARATOR = ':';

    private AkiKeySerializer() {
    }

    /**
     * 把 {@link AkiKey} 编码成唯一字符串。
     *
     * @param key 键
     * @return {@code "namespace:key"} 形式，不会为 {@code null}
     */
    public static String encode(AkiKey key) {
        return key.namespace() + SEPARATOR + key.key();
    }

    /**
     * 把形如 {@code "namespace:key"} 的字符串解码为 {@link AkiKey}。
     *
     * @param encoded 编码后的字符串
     * @return 解析出的 {@link AkiKey}
     * @throws IllegalArgumentException 格式非法（缺少冒号或两部分为空）时
     */
    public static AkiKey decode(String encoded) {
        int index = encoded.indexOf(SEPARATOR);
        if (index <= 0 || index == encoded.length() - 1) {
            throw new IllegalArgumentException("Invalid AkiKey format: " + encoded);
        }
        return AkiKey.of(encoded.substring(0, index), encoded.substring(index + 1));
    }
}