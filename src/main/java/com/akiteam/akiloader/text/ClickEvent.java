package com.akiteam.akiloader.text;

/**
 * 文本点击事件（参考 Paper 的 {@code ClickEvent}），底层封装 Minecraft 原生
 * {@code net.minecraft.network.chat.ClickEvent}，通过
 * {@code TextComponent#clickEvent(ClickEvent)} 附加到文本上。
 */
public final class ClickEvent {

    private final net.minecraft.network.chat.ClickEvent mc;

    private ClickEvent(net.minecraft.network.chat.ClickEvent mc) {
        this.mc = mc;
    }

    /** 点击后执行一条命令（如 {@code /home}）。 */
    public static ClickEvent runCommand(String command) {
        return new ClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, command));
    }

    /** 点击后把命令填入输入框（不执行，供玩家编辑）。 */
    public static ClickEvent suggestCommand(String command) {
        return new ClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, command));
    }

    /** 点击后打开指定链接。 */
    public static ClickEvent openUrl(String url) {
        return new ClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.OPEN_URL, url));
    }

    /** 点击后打开本地文件（通常仅对服务端日志有效）。 */
    public static ClickEvent openFile(String path) {
        return new ClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.OPEN_FILE, path));
    }

    /** 点击后把一段文字复制到剪贴板。 */
    public static ClickEvent copyToClipboard(String text) {
        return new ClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, text));
    }

    /** 转换为 Minecraft 原生 {@code ClickEvent}。 */
    net.minecraft.network.chat.ClickEvent toMc() {
        return mc;
    }
}