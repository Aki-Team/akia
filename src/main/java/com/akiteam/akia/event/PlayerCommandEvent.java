package com.akiteam.akia.event;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.CommandEvent;

/**
 * 插件事件：命令被调用事件（可取消）。
 * <p>
 * 由 {@link NeoForgeEventBridge} 收到 NeoForge 的 {@link CommandEvent} 时包装并分发。
 * 取消后该命令不会真正执行。
 */
public final class PlayerCommandEvent implements Event, Cancellable {

    private final CommandEvent neoEvent;

    public PlayerCommandEvent(CommandEvent neoEvent) {
        this.neoEvent = neoEvent;
    }

    /** 返回被包装的 NeoForge 原生事件。 */
    public CommandEvent getNeoEvent() {
        return neoEvent;
    }

    /** 返回命令的解析结果（含源与上下文）。 */
    public ParseResults<CommandSourceStack> getParseResults() {
        return neoEvent.getParseResults();
    }

    /** 返回命令执行的命令源。 */
    public CommandSourceStack getSource() {
        return neoEvent.getParseResults().getContext().getSource();
    }

    /** 返回本次解析到的命令对象。 */
    public Command<CommandSourceStack> getCommand() {
        return neoEvent.getParseResults().getContext().getCommand();
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