package com.akiteam.akia.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

/**
 * 包装 {@link CommandSourceStack} 的 {@link Permissible} 实现。
 * <p>
 * 用在 {@link com.akiteam.akia.command.CommandRegistry} 的命令执行前校验：
 * 玩家来源按真实 OP 身份判断，非玩家来源（控制台、命令方块、函数）一律视为 OP。
 * <p>
 * 由于 Akia 暂未接入权限插件，{@link #hasPermission(String)} 退化为按 OP 身份判断。
 */
public final class CommandPermissible implements Permissible {

    /** 命令的默认 OP 等级。 */
    public static final int OP_LEVEL = 2;

    private final CommandSourceStack source;

    public CommandPermissible(CommandSourceStack source) {
        this.source = source;
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        // 无外挂权限插件时，字符串节点默认等价于 OP 身份校验
        return isOp();
    }

    @Override
    public boolean isOp() {
        ServerPlayer player = source.getPlayer();
        return player == null || isOpPlayer(player);
    }

    @Override
    public void setOp(boolean op) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return;
        }
        PlayerList list = source.getServer().getPlayerList();
        if (op) {
            list.op(player.getGameProfile());
        } else {
            list.deop(player.getGameProfile());
        }
    }

    private static boolean isOpPlayer(ServerPlayer player) {
        return player.hasPermissions(OP_LEVEL);
    }
}