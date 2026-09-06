package com.akiteam.akiloader.persistence.impl;

import com.akiteam.akiloader.persistence.AkiAdapterContext;
import com.akiteam.akiloader.persistence.AkiDataType;
import com.akiteam.akiloader.persistence.AkiKey;
import com.akiteam.akiloader.persistence.AkiPersistentContainer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;

/**
 * {@link AkiPersistentContainer} 的默认实现。
 * <p>
 * 内部用 {@link LinkedHashMap} 以 {@link AkiKey} 为键存储<b>某一类型的原始值</b>：
 * <ul>
 *     <li>{@link #set} 时调用 {@link AkiDataType#toPrimitive} 把复杂值降为原始值再存；</li>
 *     <li>{@link #get} 时用 {@link AkiDataType#fromPrimitive} 还原为复杂值（并用
 *         {@code getPrimitiveType()} 做类型校验，类型不符返回 {@code null}）。</li>
 * </ul>
 * 序列化（{@link #toNbt} / {@link #fromNbt}）复用 {@link CompoundTagAdapter}，
 * 从而可把整个容器塞进实体或物品的存档标签实现持久化。
 */
public class AkiPersistentContainerImpl implements AkiPersistentContainer {

    private final Map<AkiKey, Object> data = new LinkedHashMap<>();
    private final AkiAdapterContext context;

    /**
     * 构造一个容器。
     *
     * @param context 适配上下文（用于创建嵌套容器与解析类型）
     */
    public AkiPersistentContainerImpl(AkiAdapterContext context) {
        this.context = context;
    }

    @Override
    public <P, C> void set(AkiKey key, AkiDataType<P, C> type, C value) {
        if (value == null) {
            throw new NullPointerException("PDC value cannot be null");
        }
        data.put(key, type.toPrimitive(value, context));
    }

    @Override
    public void remove(AkiKey key) {
        data.remove(key);
    }

    @Override
    public boolean has(AkiKey key) {
        return data.containsKey(key);
    }

    @Override
    public <P, C> C get(AkiKey key, AkiDataType<P, C> type) {
        Object primitive = data.get(key);
        if (primitive == null || !type.getPrimitiveType().isInstance(primitive)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        P typed = (P) primitive;
        return type.fromPrimitive(typed, context);
    }

    @Override
    public <P, C> C getOrDefault(AkiKey key, AkiDataType<P, C> type, C defaultValue) {
        C value = get(key, type);
        return value != null ? value : defaultValue;
    }

    @Override
    public Set<AkiKey> getKeys() {
        return Set.copyOf(data.keySet());
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public int getSize() {
        return data.size();
    }

    @Override
    public void copyTo(AkiPersistentContainer other, boolean replace) {
        if (other instanceof AkiPersistentContainerImpl target) {
            for (Map.Entry<AkiKey, Object> entry : data.entrySet()) {
                if (replace || !target.data.containsKey(entry.getKey())) {
                    target.data.put(entry.getKey(), entry.getValue());
                }
            }
            return;
        }
        // 只有一种实现，遇到未知实现时退化为逐键复制（需重新给定类型，无法保真，故不支持）
        throw new UnsupportedOperationException("copyTo only supports AkiPersistentContainerImpl");
    }

    @Override
    public CompoundTag toNbt() {
        return CompoundTagAdapter.toCompoundTag(data);
    }

    @Override
    public void fromNbt(CompoundTag tag) {
        data.clear();
        data.putAll(CompoundTagAdapter.fromCompoundTag(tag));
    }
}