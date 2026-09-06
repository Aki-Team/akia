package com.akiteam.akiloader.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

/**
 * 插件命令的处理器。
 * <p>
 * 这是 Brigadier {@link com.mojang.brigadier.Command} 的函数式形态：
 * 执行时接收一个 {@link CommandContext}，返回表示成功（通常为 1）或因失败抛出的
 * {@link CommandSyntaxException}。
 * <p>
 * 因此插件可以用 lambda 直接编写命令逻辑，例如：
 * <pre>{@code
 * registry.register("hello", ctx -> {
 *     ctx.getSource().sendSuccess(() -> Component.literal("Hello from AkiLoader!"), false);
 *     return 1;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface CommandExecutor {

    /**
     * 执行命令。
     *
     * @param context Brigadier 命令上下文（包含命令源 {@link CommandSourceStack} 与参数）
     * @return 命令执行结果（惯例：成功为 1，失败为 0 或负值）
     * @throws CommandSyntaxException 命令语法/逻辑错误，会把错误信息反馈给玩家
     */
    int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
}