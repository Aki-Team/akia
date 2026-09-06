package com.akiteam.akiloader.persistence.bridge;

import com.akiteam.akiloader.persistence.AkiPersistentContainer;
import com.akiteam.akiloader.persistence.impl.AkiAdapterContextImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * 在 Minecraft 实体上挂接 PDC 的桥接层。
 * <p>
 * 底层使用 NeoForge 为 {@link Entity} 提供的 {@code getPersistentData()}
 * （一个返回 {@link CompoundTag} 的扩展字段）。该字段会随 {@code Entity.saveWithoutId}
 * / {@code Entity.load} 的存档流程自动写入存档，因此存进去的数据具备真正的
 * 持久化能力（关闭世界 / 服务器后仍在）。
 * <p>
 * 所有数据被挂在一个名为 {@code akiloader:pdc} 的内层标签下，避免与其它
 * 模组 / NeoForge 自身的数据冲突。
 */
public final class EntityPersistentDataBridge {

    /** 实体持久化标签中的内层键名。 */
    private static final String PDC_TAG_KEY = "akiloader:pdc";

    private EntityPersistentDataBridge() {
    }

    /**
     * 读取实体上挂接的 PDC 容器。
     * <p>
     * 返回的是根据实体当前数据重建的<b>副本</b>；修改后需通过
     * {@link #set(Entity, AkiPersistentContainer)} 写回才能生效。
     *
     * @param entity 目标实体
     * @return 实体当前 PDC 的副本（不会为 {@code null}）
     */
    public static AkiPersistentContainer get(Entity entity) {
        CompoundTag tag = entity.getPersistentData().getCompound(PDC_TAG_KEY);
        AkiPersistentContainer container = AkiAdapterContextImpl.INSTANCE.newPersistentDataContainer();
        container.fromNbt(tag);
        return container;
    }

    /**
     * 把 PDC 容器写回实体。
     * <p>
     * 会整体覆盖该实体此前挂接的 PDC。
     *
     * @param entity    目标实体
     * @param container 要保存的容器
     */
    public static void set(Entity entity, AkiPersistentContainer container) {
        entity.getPersistentData().put(PDC_TAG_KEY, container.toNbt());
    }
}