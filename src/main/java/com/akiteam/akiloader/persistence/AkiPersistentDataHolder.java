package com.akiteam.akiloader.persistence;

/**
 * 可持有 PDC 的对象标记接口（参考 Paper 的 {@code PersistentDataHolder}）。
 * <p>
 * 需要对外暴露持久化容器的对象（插件主类、自定义业务对象等）可以实现此接口，
 * 并通过 {@link #getPersistentDataContainer()} 提供其容器。
 */
public interface AkiPersistentDataHolder {

    /**
     * 返回该对象持有的持久化数据容器。
     *
     * @return 持久化容器，不应为 {@code null}
     */
    AkiPersistentContainer getPersistentDataContainer();
}