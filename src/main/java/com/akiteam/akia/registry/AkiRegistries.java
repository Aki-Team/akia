package com.akiteam.akia.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * 注册表访问的静态工具，快速按注册表键或字符串获取一个 {@link AkiRegistry} 及条目值，无需持有注册表实例。
 * <pre>{@code
 * AkiRegistry<Item> items = AkiRegistries.of("minecraft", "item");
 * Optional<Item> d = AkiRegistries.get(items, AkiRegistryKey.of("minecraft:diamond"));
 * Item rare = AkiRegistries.getOrThrow(items, AkiRegistryKey.of("minecraft:diamond"));
 * }</pre>
 * 所有操作均为对已冻结注册表的只读查询，天然线程安全。
 */
public final class AkiRegistries {

    private AkiRegistries() {
    }

    /**
     * 从注册表读取条目值。
     *
     * @param registry 目标注册表
     * @param key      条目键
     * @param <T>      值类型
     * @return 对应值；不存在时为空
     */
    public static <T> Optional<T> get(AkiRegistry<T> registry, AkiRegistryKey<? extends T> key) {
        return registry.get(key);
    }

    /**
     * 从注册表读取条目值，不存在时抛出 {@link IllegalArgumentException}。
     *
     * @param registry 目标注册表
     * @param key      条目键
     * @param <T>      值类型
     * @return 对应值（恒存在）
     */
    public static <T> T getOrThrow(AkiRegistry<T> registry, AkiRegistryKey<? extends T> key) {
        return registry.getOrThrow(key);
    }

    /**
     * 通过原生注册表键获取一个 {@link AkiRegistry}。
     *
     * @param key 注册表自身的键（如 {@code Registries.ITEM}）
     * @param <T> 值类型
     * @return 对应的 AkiRegistry
     */
    public static <T> AkiRegistry<T> of(ResourceKey<? extends Registry<T>> key) {
        return AkiRegistry.of(key);
    }

    /**
     * 通过命名空间与路径获取一个 {@link AkiRegistry}。
     * <p>
     * 例如 {@code of("minecraft", "item")} 等价于 {@link Registries#ITEM}。值类型由调用方指定，
     * 查询到条目时按该类型读取（由 Minecraft 记录类保证一致）。
     *
     * @param namespace 命名空间（如 {@code minecraft}）
     * @param path      路径（如 {@code item}）
     * @param <T>       值类型（由调用方指定）
     * @return 对应的 AkiRegistry
     */
    public static <T> AkiRegistry<T> of(String namespace, String path) {
        ResourceKey<Registry<T>> key = ResourceKey.createRegistryKey(
                ResourceLocation.fromNamespaceAndPath(namespace, path));
        return AkiRegistry.of(key);
    }
}