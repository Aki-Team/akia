package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;

/**
 * 插件事件：玩家重生事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code PlayerEvent.PlayerRespawnEvent} 时包装并分发。
 */
public final class PlayerRespawnEvent implements Event {

    private final net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent neoEvent;

    public PlayerRespawnEvent(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回重生玩家。 */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }
}