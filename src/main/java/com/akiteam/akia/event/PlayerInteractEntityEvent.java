package com.akiteam.akia.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;

/**
 * 插件事件：玩家右键实体交互事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link EntityInteract} 时包装并分发。
 * 取消后交互不会触发。
 */
public final class PlayerInteractEntityEvent implements Event, Cancellable {

    private final EntityInteract neoEvent;

    public PlayerInteractEntityEvent(EntityInteract neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public EntityInteract getNeoEvent() {
        return neoEvent;
    }

    /** 返回交互的玩家。 */
    public Player getPlayer() {
        return (Player) neoEvent.getEntity();
    }

    /** 返回被交互的实体。 */
    public Entity getClickedEntity() {
        return neoEvent.getTarget();
    }

    /** 返回交互使用的手部（主手/副手）。 */
    public InteractionHand getHand() {
        return neoEvent.getHand();
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