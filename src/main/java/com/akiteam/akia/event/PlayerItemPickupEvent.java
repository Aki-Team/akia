package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * 插件事件：玩家拾取掉落物事件（{@code Post} 阶段，物品已实际进入背包）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code ItemEntityPickupEvent.Post} 时包装并分发。
 * 对齐 Paper 的 {@code PlayerPickupItemEvent}/{@code EntityPickupItemEvent}：
 * 只有物品真正入包才触发。若只需关注"拾取尝试/尚未入包"，用
 * {@link PlayerAttemptPickupItemEvent}。
 */
public final class PlayerItemPickupEvent implements Event {

    private final ItemEntityPickupEvent.Post neoEvent;

    public PlayerItemPickupEvent(ItemEntityPickupEvent.Post neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public ItemEntityPickupEvent.Post getNeoEvent() {
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

    /** 返回本次实际拾取到的物品堆（拾取前掉落物上的堆，Post 阶段实体已被清空，故回退读 originalStack）。 */
    public ItemStack getItem() {
        ItemStack original = neoEvent.getOriginalStack();
        return original.isEmpty() ? neoEvent.getItemEntity().getItem() : original;
    }
}