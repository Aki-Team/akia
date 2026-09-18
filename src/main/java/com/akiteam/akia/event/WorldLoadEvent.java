package com.akiteam.akia.event;

import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.level.LevelEvent.Load;

/**
 * 插件事件：世界（维度）加载事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link Load} 时包装并分发。
 */
public final class WorldLoadEvent implements Event {

    private final Load neoEvent;

    public WorldLoadEvent(Load neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public Load getNeoEvent() {
        return neoEvent;
    }

    /** 返回被加载的维度（世界）。 */
    public LevelAccessor getLevel() {
        return neoEvent.getLevel();
    }
}