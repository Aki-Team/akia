package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * 插件事件：玩家打开容器（合成台/箱子等界面）事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code PlayerContainerEvent.Open} 时包装并分发。
 */
public final class PlayerContainerOpenEvent implements Event {

    private final net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open neoEvent;

    public PlayerContainerOpenEvent(PlayerContainerEvent.Open neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open getNeoEvent() {
        return neoEvent;
    }

    /** 返回打开容器的玩家。 */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }
}