package com.akiteam.akiloader.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * AkiLoader 的注册表条目键（Paper 风格 {@code RegistryKey / TypedKey} 的薄封装）。
 * <p>
 * 包装原生 {@link ResourceKey} 与 {@link ResourceLocation}，用不可变 {@code record} 表达，提供类型安全的字符串键。
 * <p>
 * 工厂方法：
 * <pre>{@code
 * AkiRegistryKey<EntityType<?>> zombie = AkiRegistryKey.of("minecraft:zombie");
 * AkiRegistryKey<Item> diamond    = AkiRegistryKey.of("minecraft", "diamond");
 * AkiRegistryKey<Item> fromNative = AkiRegistryKey.of(Registries.ITEM, ...); // 或 ResourceKey.of(...)
 * }</pre>
 * 注意：{@link #of(String)} 等基于字符串的工厂并不知道值的具体类型，由调用方在声明处指定泛型。
 *
 * @param <T> 该键指向的注册值类型（如 {@code Item}、{@code EntityType<?>}）
 * @param resourceKey  绑定的原生 {@link ResourceKey}；通过字符串构造时可能为 {@code null}
 * @param location    键的字符串形式对应的 {@link ResourceLocation}（如 {@code minecraft:zombie}）
 */
public record AkiRegistryKey<T>(ResourceKey<T> resourceKey, ResourceLocation location) {

    /** 紧凑构造器：只要求 location 非空；resourceKey 允许为空（字符串构造场景）。 */
    public AkiRegistryKey {
        Objects.requireNonNull(location, "location");
    }

    /**
     * 从原生 {@link ResourceKey} 包装。
     *
     * @param key 原生资源键，不要为 {@code null}
     * @param <T> 值类型
     * @return 对应 AkiRegistryKey
     */
    public static <T> AkiRegistryKey<T> of(ResourceKey<T> key) {
        Objects.requireNonNull(key, "key");
        return new AkiRegistryKey<>(key, key.location());
    }

    /**
     * 从 {@link ResourceLocation} 构造（无 registry 绑定，仅用于按位置查询）。
     *
     * @param location 位置（如 {@code minecraft:zombie}），不要为 {@code null}
     * @param <T>      值类型（由调用方指定）
     * @return 对应 AkiRegistryKey
     */
    public static <T> AkiRegistryKey<T> of(ResourceLocation location) {
        return new AkiRegistryKey<>(null, Objects.requireNonNull(location, "location"));
    }

    /**
     * 从单段字符串解析（形如 {@code "minecraft:zombie"}），无 registry 绑定。
     *
     * @param id 形如 {@code namespace:path} 的字符串；不符合格式时抛出 {@link IllegalArgumentException}
     * @param <T> 值类型（由调用方指定）
     * @return 对应 AkiRegistryKey
     */
    public static <T> AkiRegistryKey<T> of(String id) {
        return of(ResourceLocation.parse(id));
    }

    /**
     * 从命名空间和路径构造（{@code "minecraft", "diamond"}），无 registry 绑定。
     *
     * @param namespace 命名空间（如 {@code minecraft}）
     * @param path      路径（如 {@code diamond}）
     * @param <T>       值类型（由调用方指定）
     * @return 对应 AkiRegistryKey
     */
    public static <T> AkiRegistryKey<T> of(String namespace, String path) {
        return of(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /** 命名空间部分（{@code minecraft:zombie} 中的 {@code minecraft}）。 */
    public String namespace() {
        return location().getNamespace();
    }

    /** 路径部分（{@code minecraft:zombie} 中的 {@code zombie}）。 */
    public String path() {
        return location().getPath();
    }

    /**
     * 若持有绑定的原生 {@link ResourceKey} 则返回之；字符串构造的键此值为空。
     *
     * @return 原生资源键（可能为空）
     */
    public Optional<ResourceKey<T>> asResourceKey() {
        return Optional.ofNullable(resourceKey());
    }

    @Override
    public String toString() {
        return location().toString();
    }
}