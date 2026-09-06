package com.akiteam.akiloader.service;

import com.akiteam.akiloader.api.AkiPlugin;
import com.akiteam.akiloader.event.AkiEvent;

/**
 * 服务生命周期事件：有插件注册一个新的服务提供者时，由 {@link ServiceRegistry}
 * 触发并分发到插件事件总线。
 * <p>
 * 插件可用 {@code @AkiEventHandler} 监听它，在服务上线时做出反应（如初始化缓存、
 * 对接提供者）。事件携带完整的 {@link RegisteredServiceProvider} 信息（服务类、提供者、插件、优先级）。
 */
public final class ServiceRegisteredEvent implements AkiEvent {

    private final Class<?> serviceClass;
    private final RegisteredServiceProvider<?> provider;

    /**
     * 构造事件。
     *
     * @param serviceClass 被注册的服务接口类型
     * @param provider     对应的服务提供者记录
     */
    public ServiceRegisteredEvent(Class<?> serviceClass, RegisteredServiceProvider<?> provider) {
        this.serviceClass = serviceClass;
        this.provider = provider;
    }

    /** 返回被注册的服务接口类型。 */
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /** 返回完整服务提供者记录（含提供者、插件、优先级）。 */
    public RegisteredServiceProvider<?> getProvider() {
        return provider;
    }

    /** 便捷方法：返回注册该服务的插件。 */
    public AkiPlugin getPlugin() {
        return provider.getPlugin();
    }

    /** 便捷方法：返回注册时声明的优先级。 */
    public ServicePriority getPriority() {
        return provider.getPriority();
    }
}