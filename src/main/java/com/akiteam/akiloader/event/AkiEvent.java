package com.akiteam.akiloader.event;

/**
 * 所有 AkiLoader 插件事件的标记接口。
 * <p>
 * 自定义插件事件直接实现此接口；需要支持“取消”语义的事件再额外实现
 * {@link Cancellable}。游戏内建事件（由 {@code NeoForgeEventBridge} 转发的
 * NeoForge 事件）也会被包装成实现了本接口的事件对象再分发。
 */
public interface AkiEvent {
}