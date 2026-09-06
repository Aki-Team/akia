package com.akiteam.akiloader.service;

/**
 * 服务监听句柄：{@link ServiceRegistry#watch} 返回的取消令牌。
 * <p>
 * 持有者可在任意时刻调用 {@link #cancel()} 停止接收该监听器的回调。
 * 另外，当注册该监听的插件被卸载 / 重载时，其全部监听会被 {@link ServiceRegistry}
 * 自动取消，无需手动清理。
 */
public interface ServiceWatch {

    /**
     * 取消该监听。取消后 {@link #isActive()} 返回 {@code false}，此后不会再有回调。
     * 重复取消是安全的（幂等）。
     */
    void cancel();

    /**
     * 返回该监听当前是否仍处于激活状态。
     *
     * @return {@code true} 表示仍会接收回调
     */
    boolean isActive();
}