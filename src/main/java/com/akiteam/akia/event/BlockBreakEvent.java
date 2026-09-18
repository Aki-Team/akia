package com.akiteam.akia.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 插件事件：方块被破坏事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code BlockEvent.BreakEvent} 时包装并分发。
 * 取消后该方块不会被破坏。
 */
public final class BlockBreakEvent implements Event, Cancellable {

    private final net.neoforged.neoforge.event.level.BlockEvent.BreakEvent neoEvent;

    public BlockBreakEvent(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.level.BlockEvent.BreakEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回破坏方块的玩家。 */
    public Player getPlayer() {
        return neoEvent.getPlayer();
    }

    /** 返回被破坏方块的位置。 */
    public BlockPos getPos() {
        return neoEvent.getPos();
    }

    /** 返回被破坏方块的方块状态。 */
    public BlockState getBlock() {
        return neoEvent.getState();
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