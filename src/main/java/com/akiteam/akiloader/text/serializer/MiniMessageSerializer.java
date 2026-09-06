package com.akiteam.akiloader.text.serializer;

import com.akiteam.akiloader.text.NamedTextColor;
import com.akiteam.akiloader.text.TextComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 轻量 MiniMessage 解析器（可选进阶功能）。
 * <p>
 * 支持以下标签（均需与 {@code </tag>} 配对的闭合标签，支持嵌套）：
 * <ul>
 *     <li>具名颜色：{@code <red>}、{@code <gold>}、{@code <aqua>} 等（见 {@link #NAMED}）；</li>
 *     <li>十六进制颜色：{@code <#ff0000>}；</li>
 *     <li>样式：{@code <bold>}、{@code <italic>}、{@code <underlined>}、{@code <strikethrough>}、
 *         {@code <obfuscated>}；</li>
 *     <li>{@code <reset>} 重置当前所有样式。</li>
 * </ul>
 * 非法的尖括号会被当作普通文本原样输出。
 */
public final class MiniMessageSerializer {

    private static final Map<String, NamedTextColor> NAMED = Map.ofEntries(
            Map.entry("black", NamedTextColor.BLACK),
            Map.entry("dark_blue", NamedTextColor.DARK_BLUE),
            Map.entry("dark_green", NamedTextColor.DARK_GREEN),
            Map.entry("dark_aqua", NamedTextColor.DARK_AQUA),
            Map.entry("dark_red", NamedTextColor.DARK_RED),
            Map.entry("dark_purple", NamedTextColor.DARK_PURPLE),
            Map.entry("gold", NamedTextColor.GOLD),
            Map.entry("gray", NamedTextColor.GRAY),
            Map.entry("dark_gray", NamedTextColor.DARK_GRAY),
            Map.entry("blue", NamedTextColor.BLUE),
            Map.entry("green", NamedTextColor.GREEN),
            Map.entry("aqua", NamedTextColor.AQUA),
            Map.entry("red", NamedTextColor.RED),
            Map.entry("light_purple", NamedTextColor.LIGHT_PURPLE),
            Map.entry("yellow", NamedTextColor.YELLOW),
            Map.entry("white", NamedTextColor.WHITE));

    private static final List<String> FORMAT_TAGS = List.of(
            "bold", "italic", "underlined", "strikethrough", "obfuscated");

    private MiniMessageSerializer() {
    }

    /** 解析 MiniMessage 风格文本为 {@link TextComponent}。 */
    public static TextComponent parse(String input) {
        TextComponent out = TextComponent.empty();
        if (input == null || input.isEmpty()) {
            return out;
        }
        List<String> open = new ArrayList<>();
        for (Token token : tokenize(input)) {
            if (token.tag) {
                applyTag(token.value, open);
            } else if (!token.value.isEmpty()) {
                out.append(styledText(token.value, open));
            }
        }
        return out;
    }

    private static void applyTag(String tag, List<String> open) {
        if (tag.equals("reset")) {
            open.clear();
            return;
        }
        if (tag.startsWith("/")) {
            String name = tag.substring(1);
            for (int i = open.size() - 1; i >= 0; i--) {
                if (open.get(i).equals(name)) {
                    open.remove(i);
                    return;
                }
            }
            return;
        }
        if (isOpenTag(tag)) {
            open.add(tag);
        }
    }

    private static TextComponent styledText(String content, List<String> open) {
        TextComponent t = TextComponent.text(content);
        for (String tag : open) {
            NamedTextColor color = resolveColor(tag);
            if (color != null) {
                t.color(color);
            } else if (tag.equals("bold")) {
                t.bold(true);
            } else if (tag.equals("italic")) {
                t.italic(true);
            } else if (tag.equals("underlined")) {
                t.underlined(true);
            } else if (tag.equals("strikethrough")) {
                t.strikethrough(true);
            } else if (tag.equals("obfuscated")) {
                t.obfuscated(true);
            }
        }
        return t;
    }

    private static NamedTextColor resolveColor(String tag) {
        NamedTextColor named = NAMED.get(tag);
        if (named != null) {
            return named;
        }
        if (tag.length() == 7 && tag.charAt(0) == '#') {
            try {
                return NamedTextColor.fromRgb(Integer.parseInt(tag.substring(1), 16));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean isOpenTag(String tag) {
        return resolveColor(tag) != null || FORMAT_TAGS.contains(tag);
    }

    private record Token(String value, boolean tag) {
    }

    private static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = input.length();
        while (i < n) {
            int open = input.indexOf('<', i);
            if (open < 0) {
                tokens.add(new Token(input.substring(i), false));
                break;
            }
            if (open > i) {
                tokens.add(new Token(input.substring(i, open), false));
            }
            int close = input.indexOf('>', open + 1);
            if (close < 0) {
                tokens.add(new Token(input.substring(open), false));
                break;
            }
            String inner = input.substring(open + 1, close);
            String name = inner.startsWith("/") ? inner.substring(1) : inner;
            if (isOpenTag(name) || (inner.startsWith("/") && isOpenTag(name)) || name.equals("reset")) {
                tokens.add(new Token(inner, true));
            } else {
                tokens.add(new Token("<", false));
                i = open + 1;
                continue;
            }
            i = close + 1;
        }
        return tokens;
    }
}