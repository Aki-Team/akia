package com.akiteam.akia.game;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 游戏对象：一个不可变的位置（坐标 + 朝向 + 维度）。
 * <p>
 * Paper/Bukkit 风格的 {@code Location} 包装，核心数据为一个 {@link Vec3} 位置、
 * {@code yaw}/{@code pitch} 航向与维度 key {@link ResourceKey}{@literal <Level>}。
 * 不直接持有 {@code ServerLevel}，避免跨维度耦合（后续 World 封装再补 {@code getWorld()}）。
 * <p>
 * 不可变：所有字段都是 {@code final}；{@code add}/{@code setYaw}/{@code setPitch}
 * 都返回一个新 {@code Location}，不修改自身。
 */
public final class Location {

    private final Vec3 pos;
    private final float yaw;
    private final float pitch;
    private final ResourceKey<Level> dimension;

    public Location(Vec3 pos, ResourceKey<Level> dimension, float yaw, float pitch) {
        this.pos = pos;
        this.dimension = dimension;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /** 从玩家当前位置与朝向构造。 */
    public static Location from(ServerPlayer player) {
        return new Location(player.position(), player.level().dimension(), player.getYRot(), player.getXRot());
    }

    /** 从位置向量与维度 key 构造（朝向默认 0）。 */
    public static Location from(Vec3 vec3, ResourceKey<Level> dimension) {
        return new Location(vec3, dimension, 0.0F, 0.0F);
    }

    /** 返回底层位置向量（逃生口）。 */
    public Vec3 getHandle() {
        return pos;
    }

    public double getX() {
        return pos.x;
    }

    public double getY() {
        return pos.y;
    }

    public double getZ() {
        return pos.z;
    }

    /** 位置所在方块 X（向下取整，与 MC 方块坐标一致）。 */
    public int getBlockX() {
        return Mth.floor(pos.x);
    }

    /** 位置所在方块 Y（向下取整，与 MC 方块坐标一致）。 */
    public int getBlockY() {
        return Mth.floor(pos.y);
    }

    /** 位置所在方块 Z（向下取整，与 MC 方块坐标一致）。 */
    public int getBlockZ() {
        return Mth.floor(pos.z);
    }

    /** 水平朝向角（绕 Y 轴）。 */
    public float getYaw() {
        return yaw;
    }

    /** 俯仰角（绕 X 轴）。 */
    public float getPitch() {
        return pitch;
    }

    /** 维度 key。 */
    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    /**
     * 到另一点的空间直线距离。
     * <p>
     * 注意：不做维度校验，跨维度时仅按坐标向量计算欧氏距离。
     */
    public double distance(Location other) {
        if (other == null) {
            return 0.0D;
        }
        return pos.distanceTo(other.pos);
    }

    /** 返回平移后的新位置（原对象不变）。 */
    public Location add(double x, double y, double z) {
        return new Location(pos.add(x, y, z), dimension, yaw, pitch);
    }

    /** 返回改变了水平朝向角的新位置（原对象不变）。 */
    public Location setYaw(float newYaw) {
        return new Location(pos, dimension, newYaw, pitch);
    }

    /** 返回改变了俯仰角的新位置（原对象不变）。 */
    public Location setPitch(float newPitch) {
        return new Location(pos, dimension, yaw, newPitch);
    }

    /** 返回本位置的一份拷贝。 */
    public Location clone() {
        return new Location(pos, dimension, yaw, pitch);
    }

    @Override
    public String toString() {
        return "(" + getX() + ", " + getY() + ", " + getZ() + ")"
                + " in " + dimension.location() + " yaw=" + yaw + " pitch=" + pitch;
    }
}