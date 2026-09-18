package com.akiteam.akia.event;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

/**
 * 插件事件：玩家丢出物品事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link ItemTossEvent} 时包装并分发。
 * 取消后物品不会被丢出。
 */
public final class PlayerDropItemEvent implements Event, Cancellable {

    private final ItemTossEvent neoEvent;

    public PlayerDropItemEvent(ItemTossEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public ItemTossEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回丢出物品的玩家。 */
    public Player getPlayer() {
        return neoEvent.getPlayer();
    }

    /** 返回被丢出的掉落物实体。 */
    public ItemEntity getItem() {
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