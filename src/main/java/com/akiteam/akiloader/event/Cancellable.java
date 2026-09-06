package com.akiteam.akiloader.event;

/**
 * 可取消事件接口。
 * <p>
 * 实现该接口的事件，其生命周期可以被监听器提前终止：
 * 一旦 {@link #setCancelled(boolean)} 设为 {@code true}，{@link EventBus}
 * 将停止向后续监听器传播，且设置了 {@code ignoreCancelled = true} 的监听器不会被调用。
 * 语义与 Paper 的 {@code Cancellable} 一致。
 */
public interface Cancellable {

    /**
     * 当前事件是否已被取消。
     *
     * @return {@code true} 表示已取消
     */
    boolean isCancelled();

    /**
     * 设置该事件的取消状态。
     *
     * @param cancelled {@code true} 表示取消该事件
     */
    void setCancelled(boolean cancelled);
}