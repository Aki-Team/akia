package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;

/**
 * 插件事件：玩家退出（退服）事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link PlayerLoggedOutEvent} 时包装并分发。
 */
public final class PlayerQuitEvent implements Event {

    private final PlayerLoggedOutEvent neoEvent;

    public PlayerQuitEvent(PlayerLoggedOutEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public PlayerLoggedOutEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回退服玩家。 */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }
}