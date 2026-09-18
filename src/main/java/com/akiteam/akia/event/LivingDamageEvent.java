package com.akiteam.akia.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 插件事件：生物受伤（受到伤害）事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link LivingIncomingDamageEvent} 时包装并分发。
 * 取消后伤害不会生效，玩家实际不掉血。
 */
public final class LivingDamageEvent implements Event, Cancellable {

    private final LivingIncomingDamageEvent neoEvent;

    public LivingDamageEvent(LivingIncomingDamageEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public LivingIncomingDamageEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回受伤的生物实体。 */
    public LivingEntity getEntity() {
        return neoEvent.getEntity();
    }

    /** 返回本次伤害的数值。 */
    public float getAmount() {
        return neoEvent.getAmount();
    }

    /** 返回伤害来源。 */
    public DamageSource getSource() {
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