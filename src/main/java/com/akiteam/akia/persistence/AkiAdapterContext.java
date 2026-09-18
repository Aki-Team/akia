package com.akiteam.akia.persistence;

/**
 * PDC 的适配上下文：提供给 {@link AkiDataType} 在转换时创建嵌套
 * {@link AkiPersistentContainer} 等对象的能力。
 * <p>
 * 由 Akia 在加载插件时注入（见 {@code AkiAdapterContext} 的实现），
 * 保证插件调用 {@code context.newPersistentDataContainer()} 得到的是
 * 与当前实现一致、可正确序列化的容器实例。
 */
public interface AkiAdapterContext {

    /**
     * 创建一个新的、空的 {@link AkiPersistentContainer}。
     * <p>
     * 通常在 {@code TAG_CONTAINER} 反序列化、{@code copyTo} 等场景使用。
     *
     * @return 新的持久化容器（不会为 {@code null}）
     */
    AkiPersistentContainer newPersistentDataContainer();
}