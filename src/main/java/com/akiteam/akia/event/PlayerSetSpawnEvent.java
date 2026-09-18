package com.akiteam.akia.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * 插件事件：玩家设置出生点 / 睡床事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code PlayerSetSpawnEvent} 时包装并分发。
 * 取消后玩家不会真正修改出生点。
 */
public final class PlayerSetSpawnEvent implements Event, Cancellable {

    private final net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent neoEvent;

    public PlayerSetSpawnEvent(net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回设置出生点的玩家。 */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }

    /** 返回新的出生点坐标。 */
    public BlockPos getNewSpawn() {
        return neoEvent.getNewSpawn();
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