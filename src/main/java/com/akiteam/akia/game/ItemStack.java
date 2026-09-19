package com.akiteam.akia.game;

import com.akiteam.akia.component.AkiComponents;
import com.akiteam.akia.component.Components;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏对象：一个物品堆（ItemStack）的包装。
 * <p>
 * 包装 {@link net.minecraft.world.item.ItemStack}，向插件提供 Paper 风格常用 API：
 * 读类型 / 数量 / 堆叠上限，复制，读写显示名 / lore / 自定义模型数据。
 * <p>
 * 显示名底层使用 {@link net.minecraft.core.component.DataComponents#CUSTOM_NAME}
 * （仅在非空时显示，替代物品原显示名）；lore 复用框架的 {@link Components#LORE} 数据组件封装，
 * 不自行写底层组件。mutating 方法直接作用于被包装的原生物品（引用语义，无需写回）。
 */
public final class ItemStack {

    private final net.minecraft.world.item.ItemStack handle;

    public ItemStack(net.minecraft.world.item.ItemStack handle) {
        this.handle = handle;
    }

    /** 返回被包装的原生物品堆（逃生口）。 */
    public net.minecraft.world.item.ItemStack getHandle() {
        return handle;
    }

    // ---------- 类型与数量 ----------

    /** 物品的注册 id（如 {@code minecraft:apple}）。集成原生物品的注册 key。 */
    public String getType() {
        if (handle == null || handle.isEmpty()) {
            return "minecraft:air";
        }
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(handle.getItem());
        return id == null ? "minecraft:air" : id.toString();
    }

    public int getAmount() {
        return handle.getCount();
    }

    public void setAmount(int amount) {
        if (handle == null || handle.isEmpty()) {
            return;
        }
        handle.setCount(amount);
    }

    public boolean isEmpty() {
        return handle == null || handle.isEmpty();
    }

    public int getMaxStackSize() {
        return handle == null ? 0 : handle.getMaxStackSize();
    }

    public boolean isStackable() {
        return handle != null && handle.getMaxStackSize() > 1;
    }

    // ---------- 复制 ----------

    /** 返回一份内容一致的全新包装（脱离原物品，改动互不影响）。 */
    public ItemStack copy() {
        if (handle == null) {
            return new ItemStack(net.minecraft.world.item.ItemStack.EMPTY);
        }
        return new ItemStack(handle.copy());
    }

    /** 返回一份指定数量的副本（其余属性一致）。 */
    public ItemStack copyWithCount(int count) {
        if (handle == null) {
            return new ItemStack(net.minecraft.world.item.ItemStack.EMPTY);
        }
        return new ItemStack(handle.copyWithCount(count));
    }

    // ---------- 显示名（CUSTOM_NAME，非空才显示） ----------

    /**
     * 当前显示名：有自定义名返回其纯文本；否则返回物品默认显示名。
     */
    public String getDisplayName() {
        if (handle == null || handle.isEmpty()) {
            return "";
        }
        Component name = handle.get(DataComponents.CUSTOM_NAME);
        return name != null ? name.getString() : handle.getHoverName().getString();
    }

    /** 设置自定义显示名（写 {@link DataComponents#CUSTOM_NAME} 组件）。 */
    public void setDisplayName(String name) {
        if (handle == null || handle.isEmpty() || name == null) {
            return;
        }
        handle.set(DataComponents.CUSTOM_NAME, Component.literal(name));
    }

    // ---------- Lore（复用 Components.LORE） ----------

    /** 读取所有 lore 行（无则空列表）。 */
    public List<String> getLore() {
        if (handle == null || handle.isEmpty()) {
            return new ArrayList<>();
        }
        ItemLore lore = AkiComponents.getOrDefault(handle, Components.LORE, ItemLore.EMPTY);
        if (lore == null || lore.lines() == null) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        for (Component c : lore.lines()) {
            out.add(c.getString());
        }
        return out;
    }

    /** 覆盖设置全部 lore 行（空列表 = 清空）。 */
    public void setLore(List<String> lines) {
        if (handle == null || handle.isEmpty()) {
            return;
        }
        if (lines == null || lines.isEmpty()) {
            clearLore();
            return;
        }
        List<Component> comps = new ArrayList<>();
        for (String s : lines) {
            comps.add(Component.literal(s));
        }
        AkiComponents.set(handle, Components.LORE, new ItemLore(comps));
    }

    /** 追加一行 lore（保留已有行）。 */
    public void addLoreLine(String line) {
        if (handle == null || handle.isEmpty() || line == null) {
            return;
        }
        List<String> cur = getLore();
        cur.add(line);
        setLore(cur);
    }

    /** 清除全部 lore（移除 LORE 组件）。 */
    public void clearLore() {
        if (handle == null || handle.isEmpty()) {
            return;
        }
        AkiComponents.remove(handle, Components.LORE);
    }

    // ---------- 自定义模型数据 ----------

    public int getCustomModelData() {
        if (handle == null) {
            return 0;
        }
        CustomModelData v = handle.get(DataComponents.CUSTOM_MODEL_DATA);
        return v == null ? 0 : v.value();
    }

    public void setCustomModelData(int value) {
        if (handle == null || handle.isEmpty()) {
            return;
        }
        handle.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(value));
    }

    @Override
    public String toString() {
        if (handle == null || handle.isEmpty()) {
            return "ItemStack{empty}";
        }
        return "ItemStack{" + getType() + " x" + handle.getCount() + "}";
    }
}