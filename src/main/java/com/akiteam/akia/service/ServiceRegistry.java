package com.akiteam.akia.service;

import com.akiteam.akia.api.AkiPlugin;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 服务注册表接口（插件可见，参考 Paper 的 {@code ServicesManager}）。
 * <p>
 * 插件把自己的 API 实现注册为"服务"，供其他插件按接口类型获取，从而解耦插件之间的调用。
 * 推荐约定：服务注册与获取都应在 {@code onEnable} 中进行（此时所有插件已完成加载）。
 * <p>
 * 所有实现都必须是线程安全的，可在多线程下安全地注册与获取。
 */
public interface ServiceRegistry {

    /**
     * 以默认优先级 {@link ServicePriority#Normal} 注册一个服务。
     *
     * @param serviceClass 服务接口类型
     * @param provider     服务实现实例，应实现 {@code serviceClass}
     * @param plugin       注册该服务的插件（卸载时据此自动清理）
     * @param <T>          服务接口类型参数
     */
    <T> void register(Class<T> serviceClass, T provider, AkiPlugin plugin);

    /**
     * 以指定优先级注册一个服务。
     *
     * @param serviceClass 服务接口类型
     * @param provider     服务实现实例，应实现 {@code serviceClass}
     * @param plugin       注册该服务的插件
     * @param priority     声明的优先级
     * @param <T>          服务接口类型参数
     */
    <T> void register(Class<T> serviceClass, T provider, AkiPlugin plugin, ServicePriority priority);

    /**
     * 以指定优先级和版本号注册一个服务。
     *
     * @param serviceClass 服务接口类型
     * @param provider     服务实现实例，应实现 {@code serviceClass}
     * @param plugin       注册该服务的插件
     * @param priority     声明的优先级
     * @param version      服务版本号（如 {@code "2.1.0"}，需符合语义化版本格式）
     * @param <T>          服务接口类型参数
     */
    <T> void register(Class<T> serviceClass, T provider, AkiPlugin plugin, ServicePriority priority, String version);

    /**
     * 获取当前优先级最高的服务提供者实例。
     * <p>
     * 同优先级返回先注册者。不存在时返回 {@code null}，不抛异常（与 Paper 一致）。
     *
     * @param serviceClass 服务接口类型
     * @param <T>          服务接口类型参数
     * @return 优先级最高的提供者，或 {@code null}
     */
    <T> T get(Class<T> serviceClass);

    /**
     * 获取某接口的所有服务提供者记录，按优先级（高→低）排序，同优先级按注册顺序。
     *
     * @param serviceClass 服务接口类型
     * @param <T>          服务接口类型参数
     * @return 提供者记录列表（可能为空，不为 {@code null}）
     */
    <T> List<RegisteredServiceProvider<T>> getRegistrations(Class<T> serviceClass);

    /**
     * 获取某个插件注册的所有服务提供者记录。
     *
     * @param plugin 目标插件
     * @return 该插件注册的服务记录列表（可能为空，不为 {@code null}）
     */
    List<RegisteredServiceProvider<?>> getRegistrations(AkiPlugin plugin);

    /**
     * 注销某个插件注册的所有服务。
     * <p>
     * 由 {@code PluginManager} 在插件卸载 / 重载时自动调用，插件无需手动清理。
     * 每次注销一个服务提供者都会触发 {@link ServiceUnregisteredEvent}。
     *
     * @param plugin 目标插件
     */
    void unregisterAll(AkiPlugin plugin);

    /**
     * 获取所有已注册的服务接口类型。
     * <p>
     * 用于"服务发现"：遍历当前生态里有哪些服务可用。
     *
     * @return 服务接口类型集合（可能为空，不为 {@code null}）
     */
    Set<Class<?>> getRegisteredServiceTypes();

    /**
     * 按服务类型分组，获取某个插件注册的所有服务。
     * <p>
     * 返回 {@code 服务接口 → 该插件注册的提供者记录列表}，便于查看单个插件对外暴露了哪些服务。
     *
     * @param plugin 目标插件
     * @return 按服务类型分组的结果（可能为空，不为 {@code null}）
     */
    Map<Class<?>, List<RegisteredServiceProvider<?>>> getServicesByPlugin(AkiPlugin plugin);

    /**
     * 判断某个服务接口类型当前是否有任何已注册的提供者。
     *
     * @param serviceClass 服务接口类型
     * @return {@code true} 表示存在至少一个提供者；{@code serviceClass} 为 {@code null} 时返回 {@code false}
     */
    boolean isRegistered(Class<?> serviceClass);

    /**
     * 按条件查询服务提供者记录。
     * <p>
     * {@code query} 需至少指定服务接口类型（见 {@link ServiceQuery#forClass}），并可按
     * 优先级、插件名、版本约束、最新优先进行组合筛选。结果按优先级（高→低）排序。
     *
     * @param query 查询条件
     * @param <T>   服务接口类型参数
     * @return 满足条件的提供者记录列表（可能为空，不为 {@code null}）
     */
    <T> List<RegisteredServiceProvider<T>> query(ServiceQuery query);

    /**
     * 获取满足版本约束的最高优先级提供者。
     * <p>
     * 仅允许 {@code >}、{@code >=}、{@code <}、{@code <=}、{@code =} 及 {@code ^}、{@code ~}
     * 等版本比较/范围操作符。数字形式（如 {@code "2.0"}）视为精确匹配。
     *
     * @param serviceClass 服务接口类型
     * @param constraint   版本约束表达式（如 {@code ">=2.0"}）
     * @param <T>          服务接口类型参数
     * @return 满足条件的优先级最高提供者实例，无则返回 {@code null}
     */
    <T> T get(Class<T> serviceClass, String constraint);

    /**
     * 获取该接口当前所有已注册提供者中版本号最高的实例。
     * <p>
     * 若想结合版本约束 / 优先级 / 插件名等条件，请使用 {@link #query(ServiceQuery)}。
     *
     * @param serviceClass 服务接口类型
     * @param <T>          服务接口类型参数
     * @return 版本号最高的提供者实例，无提供者则返回 {@code null}
     */
    <T> T getLatest(Class<T> serviceClass);

    /**
     * 获取指定优先级下、版本号最高的提供者实例。
     *
     * @param serviceClass 服务接口类型
     * @param priority     要求的优先级（为空表示不限制）
     * @param <T>          服务接口类型参数
     * @return 满足条件的提供者实例，无则返回 {@code null}
     */
    <T> T get(Class<T> serviceClass, ServicePriority priority);

    /**
     * 订阅某服务类型，实时感知其提供者的注册 / 注销 / 主导者替换。
     * <p>
     * 返回一个 {@link ServiceWatch} 句柄用于取消；当注册该监听的插件被卸载 / 重载时，
     * 该监听会自动被清理，无需插件手动取消。
     * <p>
     * <b>回调时机</b>：仅在订阅之后发生的变化才会触发回调；对回调触发前已存在的提供者
     * 不会立即回调（如需当前值请用 {@link #get}）。
     *
     * @param serviceClass 要订阅的服务接口类型
     * @param plugin       拥有该监听的插件（用于卸载时自动清理，不可为 {@code null}）
     * @param watcher      监听回调
     * @param <T>          服务接口类型参数
     * @return 监听句柄
     */
    <T> ServiceWatch watch(Class<T> serviceClass, AkiPlugin plugin, ServiceWatcher<T> watcher);

    /**
     * 生成当前所有已注册服务的完整快照（服务图），用于诊断、导出与展示。
     * <p>
     * 结果为不可变快照，不会随注册表后续变化而改变。
     *
     * @return 服务图快照
     */
    ServiceGraph getServiceGraph();

    // ------------------------------------------------------------------ 命名服务

    /**
     * 以默认优先级 {@link ServicePriority#Normal} 按名字注册一个服务。
     * <p>
     * 与 {@link #register(Class, Object, AkiPlugin)} 不同，命名服务不依赖接口类型，而是
     * 用一个字符串标识一个 {@link Object} 实例——适用于"模组 ↔ 插件"或"跨插件"之间按
     * 名字通信（模组不知道插件定义的接口类型）。同名重复注册会<b>覆盖</b>旧值，且该名字的
     * 归属权前移到本次注册的插件。
     *
     * @param name     服务名字（不为 {@code null} 或空字符串）
     * @param provider 服务实例（可为任意 {@link Object}；不为 {@code null}）
     * @param plugin   注册该服务的插件（卸载时据此自动清理，不为 {@code null}）
     */
    void registerNamed(String name, Object provider, AkiPlugin plugin);

    /**
     * 按名字获取服务实例。
     * <p>
     * 不存在时返回 {@code null}，不抛异常（与 {@link #get(Class)} 一致）。
     *
     * @param name 服务名字
     * @return 该名字对应的服务实例，或 {@code null}
     */
    Object getNamed(String name);

    /**
     * 订阅某名字服务，实时感知其注册 / 注销 / 替换（重点解决插件与模组的加载顺序问题）。
     * <p>
     * 返回一个 {@link ServiceWatch} 句柄用于取消；当注册该监听的插件被卸载 / 重载时，
     * 该监听会自动被清理，无需插件手动取消。
     * <p>
     * <b>回调时机</b>：仅在订阅之后发生的变化才会触发回调；对订阅前已存在的命名服务
     * 不会立即回调（如需当前值请用 {@link #getNamed}）。
     * <p>
     * <b>回调负载</b>：命名服务存的是 {@link Object}（模组不知道其类型），故回调参数
     * 统一包装成 {@link RegisteredServiceProvider}，其 {@code serviceClass} 取服务实例的
     * 实际类型（best-effort）。通常只需关注 {@link RegisteredServiceProvider#getProvider()}。
     *
     * @param name    要订阅的服务名字
     * @param plugin  拥有该监听的插件（用于卸载时自动清理，不可为 {@code null}）
     * @param watcher 监听回调
     * @param <T>     泛型参数（回调负载类型按 {@code Object} 处理）
     * @return 监听句柄
     */
    <T> ServiceWatch watchNamed(String name, AkiPlugin plugin, ServiceWatcher<T> watcher);

    /**
     * 判断某个名字是否已有注册的服务。
     *
     * @param name 服务名字
     * @return {@code true} 表示该名字当前已注册；{@code name} 为 {@code null} 时返回 {@code false}
     */
    boolean isNamedRegistered(String name);

    /**
     * 获取全部已注册的命名服务名字（服务发现）。
     *
     * @return 名字集合（可能为空，不为 {@code null}）
     */
    Set<String> getNamedServiceNames();
}