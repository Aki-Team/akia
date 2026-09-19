package com.akiteam.akia.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 游戏对象：一个方块位置的快照包装。
 * <p>
 * 包装位置所属的 {@link ServerLevel}、{@link BlockPos} 与读取到的当前 {@link BlockState}
 * （快照，不随世界实时变化）。向插件提供 Bukkit 开发者熟悉的 API：
 * 读方块类型 / 坐标 / 是否空气 / 所属世界，并可用 {@code setType/getBlockData} 改方块。
 * <p>
 * 注意：本对象保存的是构造时的状态快照，若想读最新状态请调用 {@link #blockData()} 重新获取。
 */
public final class Block {

    private final ServerLevel level;
    private final BlockPos pos;
    private final BlockState state;

    public Block(ServerLevel level, BlockPos pos, BlockState state) {
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    /** 返回当前保存的方块状态（逃生口）。 */
    public BlockState getHandle() {
        return state;
    }

    /** 方块类型（原生 {@link net.minecraft.world.level.block.Block}）。 */
    public net.minecraft.world.level.block.Block getBlock() {
        return state.getBlock();
    }

    /** 方块类型的简洁名，如 {@code stone}、{@code air}。 */
    public String getTypeAsString() {
        return state.getBlock().getDescriptionId().replace("block.minecraft.", "");
    }

    public boolean isAir() {
        return state.isAir();
    }

    /** 从世界重新读取当前方块状态（覆盖快照）。 */
    public BlockState blockData() {
        return level.getBlockState(pos);
    }

    public int getX() {
        return pos.getX();
    }

    public int getY() {
        return pos.getY();
    }

    public int getZ() {
        return pos.getZ();
    }

    public Location getLocation() {
        return new Location(pos.getCenter(), level.dimension(), 0.0F, 0.0F);
    }

    public World getWorld() {
        return new World(level);
    }

    /** 用新方块状态替换此位置处的方块（{@code update=3} 标记，客户端可感知刷新）。 */
    public void setType(BlockState newState) {
        if (newState == null) {
            return;
        }
        level.setBlock(pos, newState, 3);
    }

    @Override
    public String toString() {
        return "Block{" + getTypeAsString() + " at (" + getX() + ", " + getY() + ", " + getZ() + ")}";
    }
}