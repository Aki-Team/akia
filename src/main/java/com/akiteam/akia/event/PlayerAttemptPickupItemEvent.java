package com.akiteam.akia.event;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * 插件事件：玩家拾取掉落物的<b>尝试</b>事件（{@code Pre} 阶段，物品可能尚未真正进入背包）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code ItemEntityPickupEvent.Pre} 时包装并分发。
 * 对齐 Paper 的 {@code PlayerAttemptPickupItemEvent}：只要物品进入"可被拾取"判定就触发，
 * 不保证物品真的入包（如背包满、被取消）。若只想在物品真正入包时收到通知，用
 * {@link PlayerItemPickupEvent}。
 */
public final class PlayerAttemptPickupItemEvent implements Event {

    private final ItemEntityPickupEvent.Pre neoEvent;

    public PlayerAttemptPickupItemEvent(ItemEntityPickupEvent.Pre neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public ItemEntityPickupEvent.Pre getNeoEvent() {
        return neoEvent;
    }

    /** 返回尝试拾取的玩家。 */
    public Player getPlayer() {
        return neoEvent.getPlayer();
    }

    /** 返回被尝试拾取的掉落物实体。 */
    public ItemEntity getItemEntity() {
        return neoEvent.getItemEntity();
    }

    /** 返回被尝试拾取的物品堆。 */
    public ItemStack getItem() {
        return neoEvent.getItemEntity().getItem();
    }
}