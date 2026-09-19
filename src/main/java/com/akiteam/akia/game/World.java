package com.akiteam.akia.game;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 游戏对象：一个世界的只读视图 + 常用操作包装。
 * <p>
 * 包装 {@link ServerLevel}，向插件提供 Bukkit/Paper 开发者熟悉的 API：
 * 读世界信息（名称/种子/时间/难度/出生点）、获取玩家/实体列表、按坐标取 {@link Block}。
 * <p>
 * 玩家列表返回 {@link Player} 包装；实体列表暂返原生 {@link Entity}（实体包装由后续任务补齐）。
 */
public final class World {

    private final ServerLevel handle;

    public World(ServerLevel handle) {
        this.handle = handle;
    }

    /** 返回被包装的原生世界（逃生口）。 */
    public ServerLevel getHandle() {
        return handle;
    }

    /** 世界名（Paper 风格的 {@code getName()}，如 {@code overworld}）。 */
    public String getName() {
        return handle.dimension().location().getPath();
    }

    public ResourceKey<Level> getDimension() {
        return handle.dimension();
    }

    public boolean isOverworld() {
        return handle.dimension().equals(Level.OVERWORLD);
    }

    public boolean isNether() {
        return handle.dimension().equals(Level.NETHER);
    }

    public boolean isEnd() {
        return handle.dimension().equals(Level.END);
    }

    public long getSeed() {
        return handle.getSeed();
    }

    /** 世界白天时刻（0~24000 内循环，Paper 的 {@code getTime()}）。 */
    public long getTime() {
        return handle.getDayTime();
    }

    /** 世界总游戏刻（自创建以来的 ttick 数）。 */
    public long getFullTime() {
        return handle.getGameTime();
    }

    public Difficulty getDifficulty() {
        return handle.getDifficulty();
    }

    /** 世界出生点位置。 */
    public Location getSpawnLocation() {
        BlockPos p = handle.getSharedSpawnPos();
        return new Location(p.getCenter(), handle.dimension(), handle.getSharedSpawnAngle(), 0.0F);
    }

    /** 设置世界出生点（方位角可选，默认沿坐标值）。 */
    public void setSpawnLocation(Location loc) {
        if (loc == null) {
            return;
        }
        handle.setDefaultSpawnPos(new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                loc.getYaw());
    }

    /** 当前在线玩家列表（包装为 {@link Player}）。 */
    public List<Player> getPlayers() {
        return handle.players().stream().map(Player::new).toList();
    }

    /** 世界中所有实体（原生 {@link Entity}，暂不包装）。 */
    public List<Entity> getEntities() {
        List<Entity> out = new java.util.ArrayList<>();
        for (Entity e : handle.getAllEntities()) {
            out.add(e);
        }
        return out;
    }

    /** 取指定方块坐标处的方块（快照包装）。 */
    public Block getBlockAt(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return new Block(handle, pos, handle.getBlockState(pos));
    }

    /** 取指定位置处的方块（快照包装）。 */
    public Block getBlockAt(Location loc) {
        if (loc == null) {
            return null;
        }
        return getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /** 直接取自世界的记分板（服务端记分板，自动广播给所有在线玩家）。 */
    public Scoreboard getScoreboard() {
        return new Scoreboard(handle.getScoreboard());
    }

    /** 直接从世界取到某个 {@link ServerLevel} 的包装。 */
    public static World from(ServerLevel level) {
        return new World(level);
    }
}