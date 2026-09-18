package com.akiteam.akia.event;

import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.level.LevelEvent.Save;

/**
 * 插件事件：世界（维度）保存事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link Save} 时包装并分发。
 */
public final class WorldSaveEvent implements Event {

    private final Save neoEvent;

    public WorldSaveEvent(Save neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public Save getNeoEvent() {
        return neoEvent;
    }

    /** 返回被保存的维度（世界）。 */
    public LevelAccessor getLevel() {
        return neoEvent.getLevel();
    }
}