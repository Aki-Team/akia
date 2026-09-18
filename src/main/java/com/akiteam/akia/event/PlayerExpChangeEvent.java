package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent.XpChange;

/**
 * 插件事件：玩家经验条变化事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link XpChange} 时包装并分发。
 * 取消后本次经验变化不会发生。
 */
public final class PlayerExpChangeEvent implements Event, Cancellable {

    private final XpChange neoEvent;

    public PlayerExpChangeEvent(XpChange neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public XpChange getNeoEvent() {
        return neoEvent;
    }

    /** 返回经验变化的玩家。 */
    public Player getPlayer() {
        return (Player) neoEvent.getEntity();
    }

    /** 返回本次变化的经验值增量。 */
    public int getAmount() {
        return neoEvent.getAmount();
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