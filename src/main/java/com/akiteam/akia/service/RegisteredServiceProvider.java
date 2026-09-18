package com.akiteam.akia.service;

import com.akiteam.akia.api.AkiPlugin;

/**
 * 一条已注册的服务提供者记录（参考 Paper 的 {@code RegisteredServiceProvider}）。
 * <p>
 * 记录了服务接口、提供者实例、注册它的插件、优先级以及服务版本号。由
 * {@link ServiceRegistry#register} 生成，供插件查询提供者来源、优先级与版本。
 *
 * @param <T> 服务接口类型
 */
public final class RegisteredServiceProvider<T> {

    /** 未显式声明版本号时的默认版本。 */
    public static final String DEFAULT_VERSION = "1.0.0";

    private final Class<T> serviceClass;
    private final T provider;
    private final AkiPlugin plugin;
    private final ServicePriority priority;
    private final String version;

    public RegisteredServiceProvider(Class<T> serviceClass, T provider, AkiPlugin plugin, ServicePriority priority) {
        this(serviceClass, provider, plugin, priority, DEFAULT_VERSION);
    }

    public RegisteredServiceProvider(Class<T> serviceClass, T provider, AkiPlugin plugin,
                                     ServicePriority priority, String version) {
        this.serviceClass = serviceClass;
        this.provider = provider;
        this.plugin = plugin;
        this.priority = priority;
        this.version = (version == null || version.trim().isEmpty()) ? DEFAULT_VERSION : version.trim();
    }

    /** 返回服务接口类型。 */
    public Class<T> getServiceClass() {
        return serviceClass;
    }

    /** 返回服务实现实例。 */
    public T getProvider() {
        return provider;
    }

    /** 返回注册该服务的插件。 */
    public AkiPlugin getPlugin() {
        return plugin;
    }

    /** 返回注册时声明的优先级。 */
    public ServicePriority getPriority() {
        return priority;
    }

    /** 返回注册时声明的服务版本号（缺省为 {@value #DEFAULT_VERSION}）。 */
    public String getVersion() {
        return version;
    }
}