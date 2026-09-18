package com.akiteam.akia.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.network.chat.MutableComponent;

/**
 * Adventure {@link Component} → Minecraft 原生组件的适配器。
 * <p>
 * 转换策略：直接遍历 Adventure 组件树，逐节点把文本内容、颜色、样式（粗体/斜体/下划线/删除线/混淆）、
 * 点击事件、悬停事件映射为 Minecraft 原生 {@link MutableComponent}，不再借助 JSON 序列化往返。
 * <p>
 * 注意：早期版本用 {@code GsonComponentSerializer.serializeToTree} + {@code Component.Serializer.fromJson}
 * 往返转换，实测客户端不认往返后的点击事件（悬停有手型、点击无响应），故改为直接构建。
 */
public final class AdventureAdapter {

    private AdventureAdapter() {
    }

    /**
     * 把 Adventure 文本组件转换为 Minecraft 原生 {@link MutableComponent}。
     *
     * @param component Adventure 组件；为 {@code null} 时按空组件处理
     * @return 原生可变组件，不会为 {@code null}
     */
    public static MutableComponent toNative(Component component) {
        if (component == null) {
            return net.minecraft.network.chat.Component.empty();
        }
        MutableComponent out = contentsOf(component);
        net.minecraft.network.chat.Style style = mapStyle(component.style());
        if (style != null && style != net.minecraft.network.chat.Style.EMPTY) {
            out = out.withStyle(style);
        }
        for (Component child : component.children()) {
            out.append(toNative(child));
        }
        return out;
    }

    private static MutableComponent contentsOf(Component component) {
        if (component instanceof TextComponent text) {
            return net.minecraft.network.chat.Component.literal(text.content());
        }
        if (component instanceof TranslatableComponent tr) {
            Object[] args = tr.arguments() == null ? new Object[0] : tr.arguments().toArray();
            return net.minecraft.network.chat.Component.translatable(tr.key(), args);
        }
        if (component instanceof KeybindComponent kb) {
            return net.minecraft.network.chat.Component.keybind(kb.keybind());
        }
        // 其余类型（选区 / 计分板 / NBT 等）暂以空组件承载样式与子节点，避免歧义
        return net.minecraft.network.chat.Component.empty();
    }

    private static net.minecraft.network.chat.Style mapStyle(Style style) {
        if (style == null || style.isEmpty()) {
            return net.minecraft.network.chat.Style.EMPTY;
        }
        net.minecraft.network.chat.Style s = net.minecraft.network.chat.Style.EMPTY;

        TextColor color = style.color();
        if (color != null) {
            s = s.withColor(net.minecraft.network.chat.TextColor.fromRgb(color.value()));
        }
        s = applyDecoration(s, style, TextDecoration.BOLD);
        s = applyDecoration(s, style, TextDecoration.ITALIC);
        s = applyDecoration(s, style, TextDecoration.UNDERLINED);
        s = applyDecoration(s, style, TextDecoration.STRIKETHROUGH);
        s = applyDecoration(s, style, TextDecoration.OBFUSCATED);

        ClickEvent click = style.clickEvent();
        if (click != null) {
            net.minecraft.network.chat.ClickEvent mc = mapClick(click);
            if (mc != null) {
                s = s.withClickEvent(mc);
            }
        }

        HoverEvent<?> hover = style.hoverEvent();
        if (hover != null && hover.action() == HoverEvent.Action.SHOW_TEXT) {
            Object value = hover.value();
            if (value instanceof Component hv) {
                s = s.withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, toNative(hv)));
            }
        }

        if (style.insertion() != null) {
            s = s.withInsertion(style.insertion());
        }
        return s;
    }

    private static net.minecraft.network.chat.Style applyDecoration(
            net.minecraft.network.chat.Style s, Style style, TextDecoration decoration) {
        TextDecoration.State state = style.decoration(decoration);
        if (state == TextDecoration.State.NOT_SET) {
            return s;
        }
        boolean on = state == TextDecoration.State.TRUE;
        return switch (decoration) {
            case BOLD -> s.withBold(on);
            case ITALIC -> s.withItalic(on);
            case UNDERLINED -> s.withUnderlined(on);
            case STRIKETHROUGH -> s.withStrikethrough(on);
            case OBFUSCATED -> s.withObfuscated(on);
        };
    }

    private static net.minecraft.network.chat.ClickEvent mapClick(ClickEvent event) {
        return switch (event.action()) {
            case RUN_COMMAND -> new net.minecraft.network.chat.ClickEvent(
                    net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, event.value());
            case SUGGEST_COMMAND -> new net.minecraft.network.chat.ClickEvent(
                    net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, event.value());
            case OPEN_URL -> new net.minecraft.network.chat.ClickEvent(
                    net.minecraft.network.chat.ClickEvent.Action.OPEN_URL, event.value());
            case COPY_TO_CLIPBOARD -> new net.minecraft.network.chat.ClickEvent(
                    net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, event.value());
            case CHANGE_PAGE -> new net.minecraft.network.chat.ClickEvent(
                    net.minecraft.network.chat.ClickEvent.Action.CHANGE_PAGE, event.value());
            default -> null;
        };
    }
}