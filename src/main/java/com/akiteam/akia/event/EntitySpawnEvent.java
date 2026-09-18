package com.akiteam.akia.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 插件事件：实体生成进入世界事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code EntityJoinLevelEvent} 时包装并分发。
 * 取消后该实体不会真正进入世界。
 */
public final class EntitySpawnEvent implements Event, Cancellable {

    private final net.neoforged.neoforge.event.entity.EntityJoinLevelEvent neoEvent;

    public EntitySpawnEvent(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.entity.EntityJoinLevelEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回生成进世界的实体。 */
    public Entity getEntity() {
        return neoEvent.getEntity();
    }

    /** 返回实体进入的世界。 */
    public Level getLevel() {
        return neoEvent.getLevel();
    }

    @Override
    public boolean isCancelled() {
        return neoEvent.isCanceled();
    }

    @Override
    public void setCancelled(boolean cancelled) {
        neoEvent.setCanceled(cancelled);
    }
}