package com.akiteam.akia.event;

import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

/**
 * 插件事件：爆炸结算阶段事件（{@code Detonate}）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@code ExplosionEvent.Detonate} 时包装并分发。
 */
public final class ExplosionEvent implements Event {

    private final net.neoforged.neoforge.event.level.ExplosionEvent.Detonate neoEvent;

    public ExplosionEvent(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.level.ExplosionEvent.Detonate getNeoEvent() {
        return neoEvent;
    }

    /** 返回发生爆炸的世界。 */
    public Level getLevel() {
        return neoEvent.getLevel();
    }

    /** 返回本次爆炸对象。 */
    public Explosion getExplosion() {
        return neoEvent.getExplosion();
    }
}