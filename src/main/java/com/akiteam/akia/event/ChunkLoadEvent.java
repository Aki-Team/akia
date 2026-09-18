package com.akiteam.akia.event;

import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.level.ChunkEvent.Load;

/**
 * 插件事件：区块加载事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link Load} 时包装并分发。
 */
public final class ChunkLoadEvent implements Event {

    private final Load neoEvent;

    public ChunkLoadEvent(Load neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public Load getNeoEvent() {
        return neoEvent;
    }

    /** 返回加载区块的坐标。 */
    public ChunkPos getChunkPos() {
        return neoEvent.getChunk().getPos();
    }

    /** 返回该区块是否为新建区块（首次生成）。 */
    public boolean isNewChunk() {
        return neoEvent.isNewChunk();
    }
}