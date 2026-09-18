package com.akiteam.akia.event;

import net.minecraft.world.entity.Entity;

/**
 * 插件事件：实体跨维度传送事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code EntityTravelToDimensionEvent} 时包装并分发。
 */
public final class EntityTravelToDimensionEvent implements Event, Cancellable {

    private final net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent neoEvent;

    public EntityTravelToDimensionEvent(net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回进行跨维度传送的实体。 */
    public Entity getEntity() {
        return neoEvent.getEntity();
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