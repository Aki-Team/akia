package com.akiteam.akia.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

/**
 * 内建的插件事件：玩家登录（进服）事件。
 * <p>
 * 由 {@link NeoForgeEventBridge} 在收到 NeoForge 的 {@link PlayerLoggedInEvent}
 * 时包装并分发。插件侧监听它即可得知有玩家进入服务器。
 */
public final class PlayerJoinEvent implements Event {

    private final PlayerLoggedInEvent neoEvent;

    /**
     * 构造一个包装事件。
     *
     * @param neoEvent 对应的 NeoForge 原生事件
     */
    public PlayerJoinEvent(PlayerLoggedInEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /**
     * 返回被包装的 NeoForge 原生事件（需要底层字段时可取用）。
     *
     * @return 原生登录事件
     */
    public PlayerLoggedInEvent getNeoEvent() {
        return neoEvent;
    }

    /**
     * 返回本次登录的玩家。
     *
     * @return 登录玩家
     */
    public Player getPlayer() {
        return neoEvent.getEntity();
    }

    /**
     * 返回登录玩家的游戏名。
     *
     * @return 玩家名（字符串）
     */
    public String getPlayerName() {
        return neoEvent.getEntity().getName().getString();
    }
}