package com.akiteam.akia.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 游戏对象：一个实体的只读视图 + 常用操作包装。
 * <p>
 * 包装 {@link net.minecraft.world.entity.Entity}，向插件提供 Paper 开发者熟悉的 API：
 * 读实体类型 / 名称 / 坐标 / 世界 / 包围盒 / 状态，访问骑乘关系，改造自定义名，移动实体。
 * <p>
 * 遍历/操作须在服务端主线程调用。任何 new {@code Entity(...)} 需持有原生逃生口时，
 * 用 {@link #getHandle()}。本类为非 final 基类，{@link LivingEntity} 继承自它。
 * <p>
 * null 安全：构造器接受 null，此时所有只读 getter 返回安全默认值，mutator 为 no-op。
 */
public class Entity {

    private final net.minecraft.world.entity.Entity handle;

    public Entity(net.minecraft.world.entity.Entity handle) {
        this.handle = handle;
    }

    /** 返回被包装的原生实体（逃生口）。 */
    public net.minecraft.world.entity.Entity getHandle() {
        return handle;
    }

    // ---------- 身份 ----------

    public int getEntityId() {
        return handle == null ? 0 : handle.getId();
    }

    public UUID getUUID() {
        return handle == null ? null : handle.getUUID();
    }

    /** 实体注册 id（如 {@code minecraft:zombie}）；无法解析时返回空串。 */
    public String getType() {
        if (handle == null) {
            return "";
        }
        var key = handle.getType().builtInRegistryHolder().unwrapKey();
        if (key.isEmpty()) {
            return "";
        }
        return key.get().location().toString();
    }

    /** 实体的显示名文本（与其 {@code getName()} 一致）。 */
    public String getName() {
        return handle == null ? "" : handle.getName().getString();
    }

    /** 实体的原生显示名组件（对齐 {@code Player.getDisplayName()} 返回原生 Component）。 */
    public Component getDisplayName() {
        return handle == null ? Component.empty() : handle.getDisplayName();
    }

    // ---------- 位置 / 世界 ----------

    public double getX() {
        return handle == null ? 0.0D : handle.getX();
    }

    public double getY() {
        return handle == null ? 0.0D : handle.getY();
    }

    public double getZ() {
        return handle == null ? 0.0D : handle.getZ();
    }

    /** 实体当前位置（含朝向与维度）。 */
    public Location getLocation() {
        if (handle == null) {
            return null;
        }
        return new Location(handle.position(), handle.level().dimension(),
                handle.getYRot(), handle.getXRot());
    }

    /**
     * 实体当前所在世界；仅在 {@code handle.level()} 是 {@link ServerLevel} 时返回包装，
     * 否则（如客户端世界）返回 {@code null}。
     */
    public World getWorld() {
        if (handle == null) {
            return null;
        }
        return handle.level() instanceof ServerLevel sl ? World.from(sl) : null;
    }

    /** 实体所在维度 key（原生便捷，后续 World 已提供 {@code getDimension()}）。 */
    public ResourceKey<Level> getDimension() {
        return handle == null ? Level.OVERWORLD : handle.level().dimension();
    }

    /** 实体所在方块坐标（原生逃生）。 */
    public BlockPos getBlockPos() {
        return handle == null ? null : handle.blockPosition();
    }

    /** 实体包围盒（原生逃生）。 */
    public AABB getBoundingBox() {
        return handle == null ? null : handle.getBoundingBox();
    }

    // ---------- 状态 ----------

    /** 实体是否站在地面（对齐 Paper {@code isOnGround()}；原生方法名 {@code onGround()}）。 */
    public boolean isOnGround() {
        return handle != null && handle.onGround();
    }

    public boolean isAlive() {
        return handle != null && handle.isAlive();
    }

    public boolean isRemoved() {
        return handle != null && handle.isRemoved();
    }

    public boolean isInWater() {
        return handle != null && handle.isInWater();
    }

    public boolean isInLava() {
        return handle != null && handle.isInLava();
    }

    /** 是否潜行蹲下。 */
    public boolean isSneaking() {
        return handle != null && handle.isCrouching();
    }

    /** 是否作为乘客乘坐在其他实体上。 */
    public boolean isPassenger() {
        return handle != null && handle.isPassenger();
    }

    /** 是否承载了其他乘客。 */
    public boolean isVehicle() {
        return handle != null && handle.isVehicle();
    }

    // ---------- 自定义名 ----------

    public boolean isCustomNameVisible() {
        return handle != null && handle.isCustomNameVisible();
    }

    public void setCustomNameVisible(boolean visible) {
        if (handle != null) {
            handle.setCustomNameVisible(visible);
        }
    }

    /** 实体的自定义名组件（无则为 null）。 */
    public Component getCustomName() {
        return handle == null ? null : handle.getCustomName();
    }

    /** 设置/清除自定义名（传 null 表示清除）。 */
    public void setCustomName(String name) {
        if (handle != null) {
            handle.setCustomName(name == null ? null : Component.literal(name));
        }
    }

    // ---------- 骑乘关系 ----------

    /** 当前骑乘的载具；未骑乘时为 null。 */
    public Entity getVehicle() {
        net.minecraft.world.entity.Entity v = handle == null ? null : handle.getVehicle();
        return v == null ? null : new Entity(v);
    }

    /** 当前乘客列表（逐个包装；无则空列表）。 */
    public List<Entity> getPassengers() {
        if (handle == null) {
            return new ArrayList<>();
        }
        List<Entity> out = new ArrayList<>();
        for (net.minecraft.world.entity.Entity p : handle.getPassengers()) {
            out.add(new Entity(p));
        }
        return out;
    }

    // ---------- 移动 ----------

    /** 传送/移动到目标坐标（跨维度请用原生存口自行处理）。 */
    public void teleport(double x, double y, double z) {
        if (handle != null) {
            handle.teleportTo(x, y, z);
        }
    }

    /** 直接设置位置（无传送特效、不做碰撞校验）。 */
    public void setPos(double x, double y, double z) {
        if (handle != null) {
            handle.setPos(x, y, z);
        }
    }

    @Override
    public String toString() {
        return "Entity{" + getType() + " (" + getEntityId() + ")}";
    }
}