package com.akiteam.akia.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * 插件事件：玩家发送聊天消息事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link ServerChatEvent} 时包装并分发。
 * 取消后该条聊天消息不会广播给其他玩家。
 */
public final class PlayerChatEvent implements Event, Cancellable {

    private final ServerChatEvent neoEvent;

    public PlayerChatEvent(ServerChatEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public ServerChatEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回发送聊天的玩家。 */
    public ServerPlayer getPlayer() {
        return neoEvent.getPlayer();
    }

    /** 返回即将广播的聊天内容。 */
    public Component getMessage() {
        return neoEvent.getMessage();
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