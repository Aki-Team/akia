package com.akiteam.akia.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

/**
 * 插件事件：玩家右键方块交互事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code PlayerInteractEvent.RightClickBlock} 时包装并分发。
 */
public final class PlayerInteractEvent implements Event, Cancellable {

    private final net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock neoEvent;

    public PlayerInteractEvent(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public RightClickBlock getNeoEvent() {
        return neoEvent;
    }

    /** 返回交互的玩家。 */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }

    /** 返回交互使用的手部（主手/副手）。 */
    public InteractionHand getHand() {
        return neoEvent.getHand();
    }

    /** 返回被交互方块的位置。 */
    public BlockPos getPos() {
        return neoEvent.getPos();
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