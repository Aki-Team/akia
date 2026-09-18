package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;

/**
 * 插件事件：玩家切换维度事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code PlayerEvent.PlayerChangedDimensionEvent} 时包装并分发。
 */
public final class PlayerChangedDimensionEvent implements Event {

    private final net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent neoEvent;

    public PlayerChangedDimensionEvent(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回切换维度的玩家。 */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }
}