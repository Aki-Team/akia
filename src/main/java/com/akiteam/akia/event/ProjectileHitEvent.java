package com.akiteam.akia.event;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * 插件事件：抛射物命中（撞到方块/实体）事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link ProjectileImpactEvent} 时包装并分发。
 * 取消后命中不再结算。
 */
public final class ProjectileHitEvent implements Event, Cancellable {

    private final ProjectileImpactEvent neoEvent;

    public ProjectileHitEvent(ProjectileImpactEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public ProjectileImpactEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回命中目标的抛射物。 */
    public Projectile getEntity() {
        return neoEvent.getProjectile();
    }

    /** 返回命中点坐标。 */
    public Vec3 getHitPos() {
        return neoEvent.getRayTraceResult().getLocation();
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