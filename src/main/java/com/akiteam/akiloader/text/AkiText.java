package com.akiteam.akiloader.text;

import com.akiteam.akiloader.text.impl.ComponentConverter;
import com.akiteam.akiloader.text.serializer.MiniMessageSerializer;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 插件文本工具类（插件入口，见 {@link com.akiteam.akiloader.api.AkiPlugin#registerText}）。
 * <p>
 * 提供快捷发送（对玩家 / 命令源 / 全服广播）与快捷构建（of / join / MiniMessage 解析）。
 * 通过 {@code registerText(AkiText.INSTANCE)} 注入给插件。
 * <p>
 * <b>推荐使用 Adventure 体系</b>：请优先使用接收 {@link Component}（Adventure）的重载，
 * 它支持彩色、可点击、可悬停等富文本；旧的基于 {@link TextComponent} 的方法已标记
 * {@link Deprecated @Deprecated}，仍可工作但建议迁移。
 */
public final class AkiText {

    /** 全局共享实例（无状态、线程安全）。 */
    public static final AkiText INSTANCE = new AkiText();

    /** 共享的 MiniMessage 解析器（无状态、线程安全）。 */
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private AkiText() {
    }

    // ------------------------------------------------------------------
    // Adventure 文本 API（推荐）
    // ------------------------------------------------------------------

    /** 向玩家发送一条 Adventure 富文本消息（仅该玩家可见）。 */
    public void sendMessage(Player player, Component message) {
        player.sendSystemMessage(AdventureAdapter.toNative(message));
    }

    /** 向命令源发送一条 Adventure 富文本消息（如命令执行者，计入成功反馈）。 */
    public void sendMessage(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> AdventureAdapter.toNative(message), false);
    }

    /** 向当前服务端所有在线玩家广播一条 Adventure 富文本消息。 */
    public void broadcast(Component message) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .broadcastSystemMessage(AdventureAdapter.toNative(message), false);
    }

    /**
     * 返回共享的 {@link MiniMessage} 解析器，供插件解析 MiniMessage 字符串为
     * Adventure {@link Component}。例如：
     * <pre>{@code
     * Component msg = text.miniMessage().deserialize("<bold><gold>Hello</bold> <aqua>World!</aqua>");
     * text.sendMessage(player, msg);
     * }</pre>
     *
     * @return MiniMessage 解析器（不会为 {@code null}）
     */
    public MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    // ------------------------------------------------------------------
    // 旧版 TextComponent 文本 API（已废弃，仍可用）
    // ------------------------------------------------------------------

    /**
     * 向玩家发送一条富文本消息（仅该玩家可见）。
     *
     * @deprecated 请迁移到 {@link #sendMessage(Player, Component)}。
     */
    @Deprecated
    public void sendMessage(Player player, TextComponent message) {
        player.sendSystemMessage(build(message));
    }

    /**
     * 向命令源发送一条富文本消息（如命令执行者，计入成功反馈）。
     *
     * @deprecated 请迁移到 {@link #sendMessage(CommandSourceStack, Component)}。
     */
    @Deprecated
    public void sendMessage(CommandSourceStack source, TextComponent message) {
        source.sendSuccess(() -> build(message), false);
    }

    /**
     * 向当前服务端所有在线玩家广播一条富文本消息。
     *
     * @deprecated 请迁移到 {@link #broadcast(Component)}。
     */
    @Deprecated
    public void broadcast(TextComponent message) {
        net.minecraft.network.chat.Component component = build(message);
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .broadcastSystemMessage(component, false);
    }

    /** 快捷创建纯文本组件。 */
    public TextComponent of(String text) {
        return TextComponent.text(text);
    }

    /** 把若干组件顺序拼接为一个组件。 */
    public TextComponent join(TextComponent... components) {
        TextComponent base = TextComponent.empty();
        if (components != null) {
            for (TextComponent c : components) {
                if (c != null) {
                    base.append(c);
                }
            }
        }
        return base;
    }

    /** 用指定分隔符把一组组件拼接为一个组件。 */
    public TextComponent join(Collection<TextComponent> components, TextComponent delimiter) {
        TextComponent base = TextComponent.empty();
        boolean first = true;
        if (components != null) {
            for (TextComponent c : components) {
                if (c == null) {
                    continue;
                }
                if (!first) {
                    if (delimiter != null) {
                        base.append(delimiter);
                    }
                }
                base.append(c);
                first = false;
            }
        }
        return base;
    }

    /**
     * 解析 MiniMessage 风格文本（可选进阶功能）。
     * <p>
     * 当前支持轻量标签：{@code <red>}、{@code <green>} 等具名颜色，{@code <#rrggbb>}
     * 十六进制颜色，以及 {@code <bold>} 等样式标签，均需配对的 {@code </tag>} 闭合。
     *
     * @deprecated 请改用 {@link #miniMessage()} 返回的标准 MiniMessage 解析器。
     * @param miniMessage MiniMessage 格式文本
     * @return 解析后的 {@link TextComponent}
     */
    @Deprecated
    public TextComponent parseMiniMessage(String miniMessage) {
        return MiniMessageSerializer.parse(miniMessage);
    }

    private static net.minecraft.network.chat.Component build(TextComponent message) {
        return ComponentConverter.build(message);
    }
}