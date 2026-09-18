package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;

/**
 * 插件事件：玩家挖掘速度被计算事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code PlayerEvent.BreakSpeed} 时包装并分发。
 */
public final class PlayerBreakSpeedEvent implements Event, Cancellable {

    private final net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed neoEvent;

    public PlayerBreakSpeedEvent(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed getNeoEvent() {
        return neoEvent;
    }

    /** 返回正在挖掘的玩家。 */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }

    /** 返回计算后的挖掘速度。 */
    public float getNewSpeed() {
        return neoEvent.getNewSpeed();
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