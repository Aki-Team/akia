package com.akiteam.akia.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.util.function.UnaryOperator;

/**
 * 文本组件构建器（参考 Paper 的 {@code Component} / Adventure 的 Builder 风格）。
 * <p>
 * 采用<b>链式</b>方式构建富文本，底层完全使用 Minecraft 原生 {@link Component}
 * 与 {@link Style}，不引入任何第三方依赖。支持：
 * <ul>
 *     <li>颜色（{@link #color}）、样式（{@link #bold}、{@link #italic} 等）；</li>
 *     <li>点击事件（{@link #clickEvent}）与悬停提示（{@link #hoverEvent}）；</li>
 *     <li>拼接（{@link #append}），最终用 {@link #build()} 得到原生组件直接发送。</li>
 * </ul>
 * 样式方法（颜色 / 加粗 / 事件）作用于"当前片段"：初始为根文本，每次
 * {@code append(...)} 后切换到新拼接的片段，因此链式调用语义直观。
 */
public class TextComponent {

    private final MutableComponent root;
    private MutableComponent current;

    private TextComponent(String content) {
        this.root = Component.literal(content);
        this.current = this.root;
    }

    /** 创建一段指定文本的组件。 */
    public static TextComponent text(String content) {
        return new TextComponent(content);
    }

    /** 创建一段空文本组件，通常作为 {@link #append} 的起点。 */
    public static TextComponent empty() {
        return new TextComponent("");
    }

    /** 创建一段换行符文本。 */
    public static TextComponent newline() {
        return new TextComponent("\n");
    }

    private TextComponent style(UnaryOperator<Style> op) {
        current.withStyle(op);
        return this;
    }

    /** 设置当前片段的前景色。 */
    public TextComponent color(NamedTextColor color) {
        return style(s -> s.withColor(color.toTextColor()));
    }

    /** 用 RGB 整数设置当前片段的前景色。 */
    public TextComponent color(int rgb) {
        return style(s -> s.withColor(rgb));
    }

    /** 设置当前片段是否加粗。 */
    public TextComponent bold(boolean bold) {
        return style(s -> s.withBold(bold));
    }

    /** 设置当前片段是否斜体。 */
    public TextComponent italic(boolean italic) {
        return style(s -> s.withItalic(italic));
    }

    /** 设置当前片段是否下划线。 */
    public TextComponent underlined(boolean underlined) {
        return style(s -> s.withUnderlined(underlined));
    }

    /** 设置当前片段是否删除线。 */
    public TextComponent strikethrough(boolean strikethrough) {
        return style(s -> s.withStrikethrough(strikethrough));
    }

    /** 设置当前片段是否混淆（不断变化的乱码）。 */
    public TextComponent obfuscated(boolean obfuscated) {
        return style(s -> s.withObfuscated(obfuscated));
    }

    /** 附加上一个点击事件（执行命令 / 打开链接等）。 */
    public TextComponent clickEvent(ClickEvent event) {
        return style(s -> s.withClickEvent(event.toMc()));
    }

    /** 附加上一个悬停提示（显示文本 / 物品 / 实体）。 */
    public TextComponent hoverEvent(HoverEvent event) {
        return style(s -> s.withHoverEvent(event.toMc()));
    }

    /**
     * 拼上另一个组件，并把后续样式作用到该片段。
     *
     * @param other 要拼接的组件
     * @return 本构建器
     */
    public TextComponent append(TextComponent other) {
        if (other == null) {
            return this;
        }
        this.current = other.root;
        this.root.append(this.current);
        return this;
    }

    /**
     * 拼上一段纯文本（无样式），并把后续样式作用到该片段。
     *
     * @param text 纯文本
     * @return 本构建器
     */
    public TextComponent append(String text) {
        MutableComponent literal = Component.literal(text);
        this.root.append(literal);
        this.current = literal;
        return this;
    }

    /** 拼接多个维度（保留 null 跳过语义）。 */
    public TextComponent append(TextComponent first, TextComponent... others) {
        if (first != null) {
            append(first);
        }
        if (others != null) {
            for (TextComponent t : others) {
                if (t != null) {
                    append(t);
                }
            }
        }
        return this;
    }

    /** 返回当前所有文本（不含样式）拼接后的纯文本，用于调试/日志。 */
    public String asPlainText() {
        return asMutable().getString();
    }

    /**
     * 构建为 Minecraft 原生 {@link Component}，可直接发送。
     *
     * @return 原生组件
     */
    public Component build() {
        return asMutable();
    }

    /** 返回底层可变组件（供 {@code ComponentConverter} 使用），构建为原生组件的基础。 */
    public MutableComponent asMutable() {
        return root;
    }
}