package com.akiteam.akiloader.service;

/**
 * 服务监听器（热插拔通知回调接口）。
 * <p>
 * 通过 {@link ServiceRegistry#watch} 订阅某个服务类型，当该类型下有提供者
 * 注册 / 注销，或处于“顶端”的活动提供者被替换时，对应回调被触发。
 * 相比 {@link ServiceRegisteredEvent} / {@link ServiceUnregisteredEvent} 这两类
 * 全局事件，{@code ServiceWatcher} 只关注自己订阅的单个服务类型，粒度更细。
 * <p>
 * <b>回调线程</b>：回调在服务注册 / 注销发生的线程上同步执行（通常为主线程）。
 * 回调应尽快返回，避免阻塞插件加载与卸载流程。
 *
 * @param <T> 被订阅的服务接口类型
 */
public interface ServiceWatcher<T> {

    /**
     * 订阅的服务类型下有新的提供者被注册。
     *
     * @param provider 新注册的服务提供者记录
     */
    void onServiceRegistered(RegisteredServiceProvider<T> provider);

    /**
     * 订阅的服务类型下有提供者被注销（插件卸载 / 重载自动清理）。
     *
     * @param provider 被注销的服务提供者记录
     */
    void onServiceUnregistered(RegisteredServiceProvider<T> provider);

    /**
     * 订阅的服务类型的主提供者（当前 {@code get(serviceClass)} 返回者）被替换。
     * <p>
     * 默认空实现，仅在需要感知“当前活动实现变了”时覆写。
     *
     * @param oldProvider 原来的主提供者（可能为 {@code null}，若此前无提供者）
     * @param newProvider 新的主提供者
     */
    default void onServiceReplaced(RegisteredServiceProvider<T> oldProvider, RegisteredServiceProvider<T> newProvider) {
    }
}