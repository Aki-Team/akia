package com.akiteam.akia.service;

import com.akiteam.akia.api.AkiPlugin;
import com.akiteam.akia.event.Event;

/**
 * 服务生命周期事件：某服务的提供者被注销（插件卸载 / 重载时自动清理）时，由
 * {@link ServiceRegistry} 触发并分发到插件事件总线。
 * <p>
 * 插件可用 {@code @EventHandler} 监听它，在服务下线时做出反应（如回退到备用提供者）。
 * 事件携带完整的 {@link RegisteredServiceProvider} 信息。
 */
public final class ServiceUnregisteredEvent implements Event {

    private final Class<?> serviceClass;
    private final RegisteredServiceProvider<?> provider;

    /**
     * 构造事件。
     *
     * @param serviceClass 被注销的服务接口类型
     * @param provider     对应的服务提供者记录
     */
    public ServiceUnregisteredEvent(Class<?> serviceClass, RegisteredServiceProvider<?> provider) {
        this.serviceClass = serviceClass;
        this.provider = provider;
    }

    /** 返回被注销的服务接口类型。 */
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /** 返回完整服务提供者记录（含提供者、插件、优先级）。 */
    public RegisteredServiceProvider<?> getProvider() {
        return provider;
    }

    /** 便捷方法：返回原本注册该服务的插件。 */
    public AkiPlugin getPlugin() {
        return provider.getPlugin();
    }

    /** 便捷方法：返回注册时声明的优先级。 */
    public ServicePriority getPriority() {
        return provider.getPriority();
    }
}