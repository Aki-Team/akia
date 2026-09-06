package com.akiteam.akiloader.text.impl;

import com.akiteam.akiloader.text.TextComponent;
import net.minecraft.network.chat.Component;

/**
 * AkiLoader 文本组件 → Minecraft 原生组件的转换器。
 * <p>
 * 由于 {@link TextComponent} 底层本身就是原生 {@link Component} 的构建状态，
 * 这里的转换只是"取回"已构建的组件（保持单一转换入口，便于未来扩展）。
 */
public final class ComponentConverter {

    private ComponentConverter() {
    }

    /**
     * 把 {@link TextComponent} 转换为可直接发送的原生 {@link Component}。
     *
     * @param text 待转换的文本组件
     * @return 原生组件，不会为 {@code null}
     */
    public static Component build(TextComponent text) {
        return text == null ? Component.empty() : text.asMutable();
    }
}