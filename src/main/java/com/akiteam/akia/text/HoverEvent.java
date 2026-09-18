package com.akiteam.akia.text;

import com.akiteam.akia.text.impl.ComponentConverter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * 文本悬停提示（参考 Paper 的 {@code HoverEvent}），底层封装 Minecraft 原生
 * {@code net.minecraft.network.chat.HoverEvent}，通过
 * {@code TextComponent#hoverEvent(HoverEvent)} 附加到文本上。
 */
public final class HoverEvent {

    private final net.minecraft.network.chat.HoverEvent mc;

    private HoverEvent(net.minecraft.network.chat.HoverEvent mc) {
        this.mc = mc;
    }

    /** 悬停显示一段富文本。 */
    public static HoverEvent showText(TextComponent text) {
        Component nativeComponent = ComponentConverter.build(text);
        return new HoverEvent(new net.minecraft.network.chat.HoverEvent(
                net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, nativeComponent));
    }

    /** 悬停显示一个物品的悬浮说明。 */
    public static HoverEvent showItem(ItemStack stack) {
        return new HoverEvent(new net.minecraft.network.chat.HoverEvent(
                net.minecraft.network.chat.HoverEvent.Action.SHOW_ITEM,
                new net.minecraft.network.chat.HoverEvent.ItemStackInfo(stack)));
    }

    /** 悬停显示一个实体的悬浮说明（名称 + 实体类型）。 */
    public static HoverEvent showEntity(Entity entity) {
        return new HoverEvent(new net.minecraft.network.chat.HoverEvent(
                net.minecraft.network.chat.HoverEvent.Action.SHOW_ENTITY,
                new net.minecraft.network.chat.HoverEvent.EntityTooltipInfo(
                        entity.getType(), entity.getUUID(), entity.getName())));
    }

    /** 转换为 Minecraft 原生 {@code HoverEvent}。 */
    net.minecraft.network.chat.HoverEvent toMc() {
        return mc;
    }
}