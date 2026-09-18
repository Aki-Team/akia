package com.akiteam.akia.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Akia 的只读注册表（Paper 风格 {@code Registry<T>} 的薄封装）。
 * <p>
 * 不直接持有原生 {@link Registry} 实例，而是保存其 {@link ResourceKey}（声明式、不可变、可安全静态持有），
 * 在每次访问时惰性解析出底层注册表：
 * <ul>
 *     <li>内建注册表（Item/Block/EntityType/Enchantment…）—— 从 {@link BuiltInRegistries#REGISTRY} 解析；</li>
 *     <li>数据包注册表（Biome/DimensionType/Structure…）—— 从当前已启动服务器的 {@code RegistryAccess} 解析。</li>
 * </ul>
 * 这使 10 多个内置常量可以在类加载期安全创建，无需静态绑定一个服务器。查询本身是只读操作，
 * Minecraft 注册表在服务器启动后被冻结为不可变，因此这些读取天然线程安全（推荐在主线程调用）。
 * <p>
 * 数据包类型注册表仅在服务器启动完成（{@code onEnable} 及之后）才可查询，之前访问会抛出
 * {@link IllegalStateException}。
 *
 * @param <T> 该注册表内条目的值类型
 */
public final class AkiRegistry<T> {

    /** 本注册表自身的注册表键（{@link ResourceKey}{@code <Registry<T>>}）。 */
    private final ResourceKey<? extends Registry<T>> key;

    private AkiRegistry(ResourceKey<? extends Registry<T>> key) {
        this.key = key;
    }

    /**
     * 用注册表键构造一个 {@link AkiRegistry}。
     *
     * @param key 注册表自身的键（如 {@code Registries.ITEM}），不要为 {@code null}
     * @param <T> 值类型
     * @return 对应 AkiRegistry
     */
    public static <T> AkiRegistry<T> of(ResourceKey<? extends Registry<T>> key) {
        return new AkiRegistry<>(key);
    }

    /**
     * 返回本注册表自身的键（字符串形式如 {@code minecraft:item}）。
     *
     * @return 注册表键
     */
    @SuppressWarnings("unchecked")
    public AkiRegistryKey<T> key() {
        // 注册表自身键的类型实为 ResourceKey<Registry<T>>，此处按规范以值类型 T 呈现（与 Paper 一致，仅需字符串/相等语义）
        return AkiRegistryKey.of((ResourceKey<T>) (ResourceKey<?>) key);
    }

    /**
     * 依据键获取注册表条目。
     *
     * @param key 条目键（如 {@code minecraft:zombie}）
     * @return 对应值；不存在时为空
     */
    public Optional<T> get(AkiRegistryKey<? extends T> key) {
        return Optional.ofNullable(resolve().get(key.location()));
    }

    /**
     * 依据键获取注册表条目，不存在时抛出 {@link IllegalArgumentException}。
     *
     * @param key 条目键
     * @return 对应值（恒存在）
     */
    public T getOrThrow(AkiRegistryKey<? extends T> key) {
        return get(key).orElseThrow(() ->
                new IllegalArgumentException("No entry in " + this.key.location() + " for " + key));
    }

    /**
     * 依据键获取该条目的 {@link Holder}（可用于引用标记、标签判断等）。
     *
     * @param key 条目键
     * @return 对应 Holder；不存在时为空
     */
    @SuppressWarnings("unchecked")
    public Optional<Holder<T>> getHolder(AkiRegistryKey<? extends T> key) {
        // 原生返回 Optional<Holder.Reference<T>>，其是 Holder<T> 子类型，向上转型安全
        return (Optional<Holder<T>>) (Optional<?>) resolve().getHolder(key.location());
    }

    /**
     * 返回注册表内全部条目键。
     *
     * @return 全部键的只读集合
     */
    public Set<AkiRegistryKey<T>> keys() {
        return resolve().registryKeySet().stream()
                .map(AkiRegistryKey::of)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 返回注册表内全部值。
     *
     * @return 全部值的只读集合（副本）
     */
    public Collection<T> values() {
        return resolve().stream().toList();
    }

    /** 条目键的流。 */
    public Stream<AkiRegistryKey<T>> streamKeys() {
        return resolve().registryKeySet().stream().map(AkiRegistryKey::of);
    }

    /** 条目值的流。 */
    public Stream<T> streamValues() {
        return resolve().stream();
    }

    /**
     * 判断键是否存在于注册表。
     *
     * @param key 条目键
     * @return {@code true} 表示存在
     */
    public boolean contains(AkiRegistryKey<? extends T> key) {
        return resolve().containsKey(key.location());
    }

    /** 注册表条目数。 */
    public int size() {
        return resolve().keySet().size();
    }

    /**
     * 惰性解析底层原生 {@link Registry}。
     *
     * @return 解析出的注册表
     * @throws IllegalStateException 当既不在内建注册表、又无可用服务器时
     */
    @SuppressWarnings("unchecked")
    private Registry<T> resolve() {
        Registry<?> builtIn = BuiltInRegistries.REGISTRY.get(this.key.location());
        if (builtIn != null) {
            return (Registry<T>) builtIn;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            Optional<Registry<T>> datapack = server.registryAccess().registry(this.key);
            if (datapack.isPresent()) {
                return datapack.get();
            }
        }
        throw new IllegalStateException(
                "Registry is not available (not built-in and server not ready): " + this.key.location());
    }
}