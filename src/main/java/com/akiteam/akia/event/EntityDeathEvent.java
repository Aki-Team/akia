package com.akiteam.akia.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 插件事件：任意生物死亡事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link LivingDeathEvent} 时包装并分发。
 */
public final class EntityDeathEvent implements Event, Cancellable {

    private final LivingDeathEvent neoEvent;

    public EntityDeathEvent(LivingDeathEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public LivingDeathEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回死亡的生物实体。 */
    public LivingEntity getEntity() {
        return neoEvent.getEntity();
    }

    /** 返回导致死亡的原因。 */
    public DamageSource getDamageSource() {
        return neoEvent.getSource();
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