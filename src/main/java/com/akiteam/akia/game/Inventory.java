package com.akiteam.akia.game;

import net.minecraft.world.Container;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏对象：一个通用容器的包装（如玩家背包、箱子等 {@link Container}）。
 * <p>
 * 包装 {@link Container}，向插件提供 Paper 风格常用 API：
 * 读取格数 / 空判断，按格读写，添加 / 移除物品，清空，统计与查找。
 * <p>
 * 引用语义：{@link #getItem}/{@link #setItem} 直接操作被包装容器内的原生物品，
 * 无需额外写回。
 */
public final class Inventory {

    private final Container handle;

    public Inventory(Container handle) {
        this.handle = handle;
    }

    /** 返回被包装的原生容器（逃生口）。 */
    public Container getHandle() {
        return handle;
    }

    public int getSize() {
        return handle == null ? 0 : handle.getContainerSize();
    }

    public boolean isEmpty() {
        return handle == null || handle.isEmpty();
    }

    /** 读取指定格物品（包装，引用同一原生实例；越界返回 null）。 */
    public ItemStack getItem(int slot) {
        if (handle == null || slot < 0 || slot >= handle.getContainerSize()) {
            return null;
        }
        return new ItemStack(handle.getItem(slot));
    }

    /** 把物品写入指定格（越界忽略；null 视为清空该格）。 */
    public void setItem(int slot, ItemStack stack) {
        if (handle == null || slot < 0 || slot >= handle.getContainerSize()) {
            return;
        }
        handle.setItem(slot, stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack.getHandle());
    }

    /**
     * 尝试把若干物品加入容器（先堆叠到同类型未满槽位，再放入空格）。
     *
     * @return 无法放入、返回给调用方的剩余物品列表（可空，不会为 null）
     */
    public List<ItemStack> addItem(ItemStack... items) {
        List<ItemStack> leftover = new ArrayList<>();
        if (handle == null) {
            if (items != null) {
                for (ItemStack i : items) {
                    leftover.add(i);
                }
            }
            return leftover;
        }
        if (items != null) {
            for (ItemStack item : items) {
                if (item == null || item.isEmpty()) {
                    continue;
                }
                net.minecraft.world.item.ItemStack rest = addInternal(item.getHandle());
                if (rest != null && !rest.isEmpty()) {
                    leftover.add(new ItemStack(rest));
                }
            }
        }
        return leftover;
    }

    /** 把单个原生物品加入容器：优先堆叠到同类型未满槽，再放入空格；返回剩余。 */
    private net.minecraft.world.item.ItemStack addInternal(net.minecraft.world.item.ItemStack stack) {
        int size = handle.getContainerSize();
        int max = handle.getMaxStackSize(stack);

        // 1) 尝试堆叠到同类型未满的槽
        for (int i = 0; i < size; i++) {
            if (stack.isEmpty()) {
                break;
            }
            net.minecraft.world.item.ItemStack slot = handle.getItem(i);
            if (!slot.isEmpty() && canMerge(slot, stack)) {
                int room = max - slot.getCount();
                if (room > 0) {
                    int move = Math.min(room, stack.getCount());
                    slot.grow(move);
                    stack.shrink(move);
                    handle.setChanged();
                }
            }
        }
        // 2) 放入第一个空格
        if (!stack.isEmpty()) {
            for (int i = 0; i < size; i++) {
                net.minecraft.world.item.ItemStack slot = handle.getItem(i);
                if (slot.isEmpty()) {
                    handle.setItem(i, stack.copy());
                    handle.setChanged();
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    private static boolean canMerge(net.minecraft.world.item.ItemStack a, net.minecraft.world.item.ItemStack b) {
        return net.minecraft.world.item.ItemStack.isSameItemSameComponents(a, b);
    }

    /** 移除指定格内指定数量的物品，返回实际取出的物品（越界返回 null）。 */
    public ItemStack removeItem(int slot, int amount) {
        if (handle == null || slot < 0 || slot >= handle.getContainerSize()) {
            return null;
        }
        net.minecraft.world.item.ItemStack removed = handle.removeItem(slot, amount);
        return removed == null || removed.isEmpty() ? null : new ItemStack(removed);
    }

    public void clear() {
        if (handle != null) {
            handle.clearContent();
        }
    }

    /** 统计容器内与该物品同类型的总数（含数量）。 */
    public int countItem(ItemStack stack) {
        if (handle == null || stack == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < handle.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = handle.getItem(i);
            if (s != null && !s.isEmpty() && s.is(stack.getHandle().getItem())) {
                count += s.getCount();
            }
        }
        return count;
    }

    /** 容器内是否存在至少一个与该物品同类型的物品。 */
    public boolean contains(ItemStack stack) {
        if (handle == null || stack == null) {
            return false;
        }
        for (int i = 0; i < handle.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = handle.getItem(i);
            if (s != null && !s.isEmpty() && s.is(stack.getHandle().getItem())) {
                return true;
            }
        }
        return false;
    }

    /** 第一个空格下标；没有空格时返回 -1。 */
    public int firstEmpty() {
        if (handle == null) {
            return -1;
        }
        for (int i = 0; i < handle.getContainerSize(); i++) {
            if (handle.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}