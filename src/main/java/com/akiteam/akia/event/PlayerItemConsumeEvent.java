package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish;

/**
 * 插件事件：玩家进食/饮用物品（用完物品）事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link Finish} 时包装并转发，
 * 桥内仅对使用主体为玩家的情形触发。
 */
public final class PlayerItemConsumeEvent implements Event {

    private final Finish neoEvent;

    public PlayerItemConsumeEvent(Finish neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public Finish getNeoEvent() {
        return neoEvent;
    }

    /** 返回使用者（玩家）。 */
    public Player getPlayer() {
        return (Player) neoEvent.getEntity();
    }

    /** 返回被使用/消耗的物品堆。 */
    public ItemStack getItem() {
        return neoEvent.getItem();
    }
}