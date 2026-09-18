package com.akiteam.akia.persistence;

import com.akiteam.akia.api.AkiPlugin;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * PDC 的命名空间键（参考 Paper 的 {@code NamespacedKey}），唯一标识一个持久化数据项。
 * <p>
 * 由 {@code namespace} 与 {@code key} 两部分组成，通常写作 {@code namespace:key}。
 * namespace 用于把不同插件/模块的数据隔离开：两个插件即使使用相同的 key，
 * 只要 namespace 不同就互不干扰。
 * <p>
 * 两者只允许小写字母、数字、{@code '_'}、{@code '-'}、{@code '.'}、{@code '/'}，
 * 构造时自动归一化为小写；整个键长度不超过 {@value #MAX_LENGTH}。
 */
public record AkiKey(String namespace, String key) {

    /** 允许的字符：小写字母、数字、下划线、中划线、点、斜杠。 */
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9_\\-./]+$");

    /** 序列化后（含冒号）的最大长度。 */
    public static final int MAX_LENGTH = 32768;

    /** 键过长时抛出的异常消息前缀。 */
    private static final String TOO_LONG = "AkiKey too long: ";

    /**
     * 正向构造函数：校验并归一化两个部分。
     *
     * @throws IllegalArgumentException 存在非法字符或总长度超限时
     */
    public AkiKey {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(key, "key");
        if (!ALLOWED.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }
        if (!ALLOWED.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        if (namespace.length() + key.length() + 1 > MAX_LENGTH) {
            throw new IllegalArgumentException(TOO_LONG + namespace + ":" + key);
        }
        namespace = namespace.toLowerCase();
        key = key.toLowerCase();
    }

    /**
     * 用显式的 namespace 与 key 构造一个键。
     *
     * @param namespace 命名空间（仅允许小写字母、数字、{@code _-.}、{@code /}）
     * @param key       键名（规则同上）
     * @return 校验通过后的 {@link AkiKey}
     */
    public static AkiKey of(String namespace, String key) {
        return new AkiKey(namespace, key);
    }

    /**
     * 用插件名作为 namespace 构造一个键，自动把插件名归一化为小写。
     *
     * @param plugin 插件实例（其名称会被用作 namespace）
     * @param key    键名
     * @return 该校验通过后的 {@link AkiKey}
     */
    public static AkiKey of(AkiPlugin plugin, String key) {
        return new AkiKey(plugin.getName(), key);
    }

    /**
     * 序列化为 {@code namespace:key} 字符串，用于在 NBT 等底层存储中作为键名。
     *
     * @return 形如 {@code "ns:key"} 的字符串
     */
    @Override
    public String toString() {
        return namespace + ":" + key;
    }
}