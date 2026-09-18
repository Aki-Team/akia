package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * 插件事件：玩家拾取掉落物事件（{@code Pre} 阶段，捡起但尚未入包）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code ItemEntityPickupEvent.Pre} 时包装并分发。
 */
public final class PlayerItemPickupEvent implements Event {

    private final ItemEntityPickupEvent.Pre neoEvent;

    public PlayerItemPickupEvent(ItemEntityPickupEvent.Pre neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public ItemEntityPickupEvent.Pre getNeoEvent() {
        return neoEvent;
    }

    /** 返回拾取掉落物的玩家。 */
    public Player getPlayer() {
        return neoEvent.getPlayer();
    }

    /** 返回被拾取的掉落物实体。 */
    public ItemEntity getItemEntity() {
        return neoEvent.getItemEntity();
    }

    /** 返回本次拾取的物品堆。 */
    public ItemStack getItem() {
        return neoEvent.getItemEntity().getItem();
    }
}