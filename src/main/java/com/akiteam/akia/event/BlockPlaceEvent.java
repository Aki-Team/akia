package com.akiteam.akia.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/**
 * 插件事件：方块被放置事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code BlockEvent.EntityPlaceEvent} 时包装并分发。
 */
public final class BlockPlaceEvent implements Event, Cancellable {

    private final net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent neoEvent;

    public BlockPlaceEvent(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回放置方块的实体（通常为玩家）。 */
    public Entity getEntity() {
        return neoEvent.getEntity();
    }

    /** 返回方块被放置的位置。 */
    public BlockPos getPos() {
        return neoEvent.getPos();
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