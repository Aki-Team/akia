package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent.LevelChange;

/**
 * 插件事件：玩家经验等级变化事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link LevelChange} 时包装并分发。
 * 取消后本次等级变化不会发生。
 */
public final class PlayerLevelChangeEvent implements Event, Cancellable {

    private final LevelChange neoEvent;

    public PlayerLevelChangeEvent(LevelChange neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public LevelChange getNeoEvent() {
        return neoEvent;
    }

    /** 返回等级变化的玩家。 */
    public Player getPlayer() {
        return (Player) neoEvent.getEntity();
    }

    /** 返回变化后的新等级。 */
    public int getNew() {
        return neoEvent.getLevels();
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