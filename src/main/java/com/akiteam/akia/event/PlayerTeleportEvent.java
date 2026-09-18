package com.akiteam.akia.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

/**
 * 插件事件：玩家传送事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link EntityTeleportEvent} 时包装并分发
 * （桥内仅对传送主体为玩家的情形转发）。取消后传送不会发生。
 */
public final class PlayerTeleportEvent implements Event, Cancellable {

    private final EntityTeleportEvent neoEvent;

    public PlayerTeleportEvent(EntityTeleportEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public EntityTeleportEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回进行传送的玩家。 */
    public Player getPlayer() {
        return (Player) neoEvent.getEntity();
    }

    /** 返回传送的起点坐标。 */
    public Vec3 getFrom() {
        return neoEvent.getPrev();
    }

    /** 返回传送的目标坐标。 */
    public Vec3 getTo() {
        return neoEvent.getTarget();
    }

    /** 返回传送目标所在的方块位置。 */
    public BlockPos getToPos() {
        return BlockPos.containing(neoEvent.getTarget());
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