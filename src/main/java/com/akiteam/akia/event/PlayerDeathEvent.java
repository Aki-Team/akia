package com.akiteam.akia.event;

import java.util.Collection;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * 插件事件：玩家死亡事件（含掉落物，可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link LivingDropsEvent} 时包装并转发，
 * 桥内仅对死亡主体为玩家的情形触发。取消后玩家的掉落物不会被放入世界。
 */
public final class PlayerDeathEvent implements Event, Cancellable {

    private final LivingDropsEvent neoEvent;

    public PlayerDeathEvent(LivingDropsEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public LivingDropsEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回死亡的玩家。 */
    public Player getPlayer() {
        return (Player) neoEvent.getEntity();
    }

    /** 返回本次死亡的掉落物集合。 */
    public Collection<ItemEntity> getDrops() {
        return neoEvent.getDrops();
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