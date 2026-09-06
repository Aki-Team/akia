package com.akiteam.akiloader.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

/**
 * NeoForge → AkiLoader 的事件桥接器（实现约定中的“方案 A：直接监听”）。
 * <p>
 * 它显式地 {@link NeoForge#EVENT_BUS} 订阅若干游戏事件，在收到时构造对应的
 * 插件事件（实现 {@link AkiEvent}）并通过 {@link EventBus#fireEvent(AkiEvent)} 转发给插件。
 * 这样插件只面向 AkiLoader 自己的事件类型编程，不直接依赖 NeoForge 事件细节。
 * <p>
 * 需要开放更多游戏事件时，在这个类里按同样的模式添加 {@code @SubscribeEvent} 方法即可。
 */
public final class NeoForgeEventBridge {

    private final EventBus eventBus;

    /**
     * 构造桥接器并把自己注册到 NeoForge 事件总线。
     *
     * @param eventBus 插件的 {@link EventBus}，用于转发包装后的事件
     */
    public NeoForgeEventBridge(EventBus eventBus) {
        this.eventBus = eventBus;
        NeoForge.EVENT_BUS.register(this);
    }

    /**
     * 玩家登录（进服）事件 → 包装为 {@link AkiPlayerJoinEvent} 并转发。
     *
     * @param event NeoForge 登录事件
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        eventBus.fireEvent(new AkiPlayerJoinEvent(event));
    }
}