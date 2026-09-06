package com.akiteam.akiloader.persistence.impl;

import com.akiteam.akiloader.persistence.AkiAdapterContext;
import com.akiteam.akiloader.persistence.AkiPersistentContainer;

/**
 * {@link AkiAdapterContext} 的默认实现：每次 {@code newPersistentDataContainer()}
 * 返回一个以自身为上下文的新 {@link AkiPersistentContainerImpl}，保证所有容器
 * 都能互相嵌套、正确序列化。
 */
public final class AkiAdapterContextImpl implements AkiAdapterContext {

    /** 全局共享实例（本类无状态、线程安全）。 */
    public static final AkiAdapterContextImpl INSTANCE = new AkiAdapterContextImpl();

    private AkiAdapterContextImpl() {
    }

    @Override
    public AkiPersistentContainer newPersistentDataContainer() {
        return new AkiPersistentContainerImpl(this);
    }
}