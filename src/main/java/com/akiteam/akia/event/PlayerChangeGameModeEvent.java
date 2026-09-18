package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/**
 * 插件事件：玩家切换游戏模式事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code PlayerEvent.PlayerChangeGameModeEvent} 时包装并分发。
 * 取消后玩家游戏模式不会被改变，{@link #getNewGameMode()} 的改动同样被忽略。
 */
public final class PlayerChangeGameModeEvent implements Event, Cancellable {

    private final net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent neoEvent;

    public PlayerChangeGameModeEvent(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回切换游戏模式的玩家。 */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }

    /** 返回切换后的新游戏模式。 */
    public GameType getNewGameMode() {
        return neoEvent.getNewGameMode();
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