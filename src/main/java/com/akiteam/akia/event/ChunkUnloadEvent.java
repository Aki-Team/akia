package com.akiteam.akia.event;

import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.level.ChunkEvent.Unload;

/**
 * 插件事件：区块卸载事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link Unload} 时包装并分发。
 */
public final class ChunkUnloadEvent implements Event {

    private final Unload neoEvent;

    public ChunkUnloadEvent(Unload neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public Unload getNeoEvent() {
        return neoEvent;
    }

    /** 返回卸载区块的坐标。 */
    public ChunkPos getChunkPos() {
        return neoEvent.getChunk().getPos();
    }
}