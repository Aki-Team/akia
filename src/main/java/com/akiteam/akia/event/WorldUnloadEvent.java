package com.akiteam.akia.event;

import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;

/**
 * 插件事件：世界（维度）卸载事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link Unload} 时包装并分发。
 */
public final class WorldUnloadEvent implements Event {

    private final Unload neoEvent;

    public WorldUnloadEvent(Unload neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public Unload getNeoEvent() {
        return neoEvent;
    }

    /** 返回被卸载的维度（世界）。 */
    public LevelAccessor getLevel() {
        return neoEvent.getLevel();
    }
}