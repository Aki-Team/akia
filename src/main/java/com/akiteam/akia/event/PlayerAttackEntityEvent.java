package com.akiteam.akia.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * 插件事件：玩家攻击实体事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link AttackEntityEvent} 时包装并分发。
 * 取消后本次攻击不产生实际效果。
 */
public final class PlayerAttackEntityEvent implements Event, Cancellable {

    private final AttackEntityEvent neoEvent;

    public PlayerAttackEntityEvent(AttackEntityEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public AttackEntityEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回发起攻击的玩家。 */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }

    /** 返回被攻击的目标实体。 */
    public Entity getTarget() {
        return neoEvent.getTarget();
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