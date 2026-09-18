package com.akiteam.akia.event;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;

import java.util.UUID;

/**
 * 插件事件：玩家登录握手协商事件（异步，认证过滤 / 改名 / 皮肤修复）。
 * <p>
 * 在 NeoForge 的 {@code PlayerNegotiationEvent} 触发时，{@link NeoForgeEventBridge}
 * 会通过事件自带的 {@code enqueueWork} 把分发切回服务端线程，再转发给插件。
 * 此事件暴露玩家 ID 与连接字段，供在进服前做协商处理。
 */
public final class PlayerNegotiationEvent implements Event {

    private final net.neoforged.neoforge.event.entity.player.PlayerNegotiationEvent neoEvent;

    public PlayerNegotiationEvent(net.neoforged.neoforge.event.entity.player.PlayerNegotiationEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public net.neoforged.neoforge.event.entity.player.PlayerNegotiationEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回正在协商的玩家 UUID。 */
    public UUID getPlayerId() {
        return neoEvent.getProfile().getId();
    }

    /** 返回玩家游戏档案（含名称）。 */
    public GameProfile getProfile() {
        return neoEvent.getProfile();
    }

    /** 返回玩家对应的网络连接。 */
    public Connection getConnection() {
        return neoEvent.getConnection();
    }
}