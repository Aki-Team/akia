package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * 插件事件：玩家关闭容器（界面）事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code PlayerContainerEvent.Close} 时包装并分发。
 */
public final class PlayerContainerCloseEvent implements Event {

    private final net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close neoEvent;

    public PlayerContainerCloseEvent(PlayerContainerEvent.Close neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close getNeoEvent() {
        return neoEvent;
    }

    /** 返回关闭容器的玩家。 */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }
}