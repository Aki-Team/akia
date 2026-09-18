package com.akiteam.akia.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

/**
 * 插件命令的 <b>Tab 补全 / 建议</b> 提供者。
 * <p>
 * 用在带参数的插件命令上（配合 {@link CommandRegistry#registerArgumentSuggestions}），
 * 让玩家按 Tab 时能补全子命令、参数值或在线玩家名等。这是函数式接口，可以用 lambda 编写。
 * <p>
 * 例如给一个 {@code /papi} 命令补全子命令 {@code list|parse|reload}：
 * <pre>{@code
 * registry.registerArgumentSuggestions("papi", "args", executor,
 *     (ctx, remaining) -> List.of("list", "parse", "reload"));
 * }</pre>
 * 如果还想在输入到 {@code parse} 后继续补全玩家名，可检查 {@code remaining} 内容：
 * <pre>{@code
 * (ctx, remaining) -> {
 *     String t = remaining.trim();
 *     if (t.startsWith("parse")) {
 *         return ctx.getSource().getServer().getPlayerNames(); // 在线玩家名
 *     }
 *     return List.of("list", "parse", "reload");
 * }
 * }</pre>
 */
@FunctionalInterface
public interface AkiCommandSuggestion {

    /**
     * 根据当前命令上下文与已输入的部分，返回可补全的候选字符串列表。
     * <p>
     * 返回的候选会被 Brigadier 加入 Tab 建议；同一列表中的项按字母序/输入前缀自动过滤。
     *
     * @param ctx           命令上下文（可通过 {@link CommandContext#getSource()} 拿到
     *                      {@link CommandSourceStack}，进而访问服务器/玩家列表）
     * @param remainingInput 命令名之后、玩家已经输入的原始文本（可为空串）
     * @return 建议的候选字符串列表（可为空，表示无可补全项）
     */
    List<String> suggest(CommandContext<CommandSourceStack> ctx, String remainingInput);
}