package com.akiteam.akia.text;

import net.minecraft.network.chat.TextColor;

/**
 * 具名颜色常量（参考 Paper 的 {@code NamedTextColor}），底层复用 Minecraft
 * 原生 {@link TextColor}。每个常量包装一个 RGB 值，可直接用于
 * {@code TextComponent#color(NamedTextColor)}。
 */
public final class NamedTextColor {

    /** 黑色。 */
    public static final NamedTextColor BLACK = new NamedTextColor(0x000000);
    /** 深蓝。 */
    public static final NamedTextColor DARK_BLUE = new NamedTextColor(0x0000AA);
    /** 深绿。 */
    public static final NamedTextColor DARK_GREEN = new NamedTextColor(0x00AA00);
    /** 深青。 */
    public static final NamedTextColor DARK_AQUA = new NamedTextColor(0x00AAAA);
    /** 深红。 */
    public static final NamedTextColor DARK_RED = new NamedTextColor(0xAA0000);
    /** 深紫。 */
    public static final NamedTextColor DARK_PURPLE = new NamedTextColor(0xAA00AA);
    /** 金色。 */
    public static final NamedTextColor GOLD = new NamedTextColor(0xFFAA00);
    /** 灰色。 */
    public static final NamedTextColor GRAY = new NamedTextColor(0xAAAAAA);
    /** 深灰。 */
    public static final NamedTextColor DARK_GRAY = new NamedTextColor(0x555555);
    /** 蓝色。 */
    public static final NamedTextColor BLUE = new NamedTextColor(0x5555FF);
    /** 绿色。 */
    public static final NamedTextColor GREEN = new NamedTextColor(0x55FF55);
    /** 青色。 */
    public static final NamedTextColor AQUA = new NamedTextColor(0x55FFFF);
    /** 红色。 */
    public static final NamedTextColor RED = new NamedTextColor(0xFF5555);
    /** 亮紫 / 粉红。 */
    public static final NamedTextColor LIGHT_PURPLE = new NamedTextColor(0xFF55FF);
    /** 黄色。 */
    public static final NamedTextColor YELLOW = new NamedTextColor(0xFFFF55);
    /** 白色。 */
    public static final NamedTextColor WHITE = new NamedTextColor(0xFFFFFF);

    private final TextColor textColor;

    private NamedTextColor(int rgb) {
        this.textColor = TextColor.fromRgb(rgb);
    }

    /**
     * 用任意 RGB 整数构造一个颜色。
     *
     * @param rgb 0xRRGGBB
     * @return 新颜色
     */
    public static NamedTextColor fromRgb(int rgb) {
        return new NamedTextColor(rgb);
    }

    /**
     * 转换为 Minecraft 原生 {@link TextColor}。
     *
     * @return 原生颜色对象
     */
    public TextColor toTextColor() {
        return textColor;
    }

    /**
     * 返回十六进制颜色表示（对齐 Minecraft/Paper 的 {@code #RRGGBB} 形式），
     * 如 {@code #FF5555}。供命令回显 / 日志使用，不再打印对象地址。
     *
     * @return {@code #RRGGBB} 形式的颜色串（始终为大写十六进制）
     */
    @Override
    public String toString() {
        return String.format("#%06X", textColor.getValue());
    }
}