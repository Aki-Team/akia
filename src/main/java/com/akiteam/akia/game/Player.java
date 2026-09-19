package com.akiteam.akia.game;

import com.akiteam.akia.text.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 游戏对象：玩家的只读视图 + 常用操作包装。
 * <p>
 * 包装 {@link ServerPlayer}，向插件提供 Bukkit 开发者熟悉的 API：读玩家属性、
 * 发消息、传送、踢出、切模式、发经验、设置坐标。
 * <p>
 * 常用对象均返回框架包装版（World / ItemStack / Inventory / Location）；
 * FoodData / Component / ServerLevel / MinecraftServer 等暂返原生逃生类型。
 * 玩家操作须在服务端线程调用。
 */
public final class Player {

    private static final org.slf4j.Logger LOGGER = com.akiteam.akia.Akia.LOGGER;

    private final ServerPlayer handle;

    public Player(ServerPlayer handle) {
        this.handle = handle;
    }

    /** 返回被包装的原生玩家（逃生口）。 */
    public ServerPlayer getHandle() {
        return handle;
    }

    // ---------- 只读 ----------

    public UUID getUUID() {
        return handle.getUUID();
    }

    public String getName() {
        return handle.getName().getString();
    }

    public Component getDisplayName() {
        return handle.getDisplayName();
    }

    public Location getLocation() {
        return Location.from(handle);
    }

    public boolean isAlive() {
        return handle.isAlive();
    }

    /** 玩家是否仍在线（未断线）。 */
    public boolean isOnline() {
        return !handle.hasDisconnected();
    }

    /** 玩家是否具备管理员（OP）身份。 */
    public boolean isOp() {
        return handle.hasPermissions(handle.getServer().getOperatorUserPermissionLevel());
    }

    /**
     * 是否具备指定权限节点。
     * <p>
     * 说明：Akia 当前未接入权限插件，框架的 {@link com.akiteam.akia.permission.Permissible#hasPermission}
     * 已退化为按 OP 身份判定。此处级联同一策略：节点缺失或为空视为放行，否则按 OP 判定。
     * 未来接入权限插件时，仅需改此方法为查询权限服务即可。
     */
    public boolean hasPermission(String node) {
        if (node == null || node.isEmpty()) {
            return true;
        }
        return isOp();
    }

    /** 玩家当前所在维度世界（包装版）。 */
    public World getWorld() {
        return World.from(handle.serverLevel());
    }

    public GameType getGameMode() {
        return handle.gameMode.getGameModeForPlayer();
    }

    public int getExpLevel() {
        return handle.experienceLevel;
    }

    public int getTotalExperience() {
        return handle.totalExperience;
    }

    /** 经验条进度（0.0 ~ 1.0）。 */
    public float getExp() {
        return handle.experienceProgress;
    }

    public int getFoodLevel() {
        return handle.getFoodData().getFoodLevel();
    }

    public boolean isSneaking() {
        return handle.isCrouching();
    }

    public boolean isSprinting() {
        return handle.isSprinting();
    }

    public boolean isSleeping() {
        return handle.isSleeping();
    }

    // ---------- 操作 ----------

    /** 发送一条 Akia 文本消息（Adventure 风格）。 */
    public void sendMessage(TextComponent message) {
        if (message == null) {
            return;
        }
        handle.sendSystemMessage(message.build());
    }

    /** 以纯文本形式发送一条系统消息（重载，不破坏现有 {@link #sendMessage(TextComponent)}）。 */
    public void sendMessage(String message) {
        if (message == null) {
            return;
        }
        handle.sendSystemMessage(Component.literal(message));
    }

    /**
     * 传送玩家到指定位置。
     * <p>
     * 同维度用 {@code teleportTo(x,y,z)} 再设朝向；跨维度先经
     * {@code server.getLevel(dim)} 取目标维度再 {@code teleportTo}。目标维度缺失时安全降级不传送。
     */
    public void teleport(Location to) {
        if (to == null) {
            return;
        }
        ServerLevel current = handle.serverLevel();
        ResourceKey<Level> dim = to.getDimension();
        if (dim.equals(current.dimension())) {
            handle.teleportTo(to.getX(), to.getY(), to.getZ());
            handle.setYRot(to.getYaw());
            handle.setXRot(to.getPitch());
            return;
        }
        ServerLevel target = handle.server.getLevel(dim);
        if (target == null) {
            LOGGER.warn("Cannot teleport player {} to unknown dimension {}", getName(), dim.location());
            return;
        }
        handle.teleportTo(target, to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());
    }

    /**
     * 以指定原因踢出玩家。
     * <p>
     * 说明：MC 1.21.1 的 {@link ServerPlayer} 没有公开的 {@code kick(...)}
     * 方法，带原因的断线只能经由 {@code connection.disconnect(Component)}。
     */
    public void kick(Component reason) {
        handle.connection.disconnect(reason);
    }

    /** 以纯文本原因踢出玩家。 */
    public void kick(String reason) {
        handle.connection.disconnect(Component.literal(reason));
    }

    public void setGameMode(GameType gameType) {
        handle.setGameMode(gameType);
    }

    public void giveExperienceLevels(int levels) {
        handle.giveExperienceLevels(levels);
    }

    public void giveExperiencePoints(int points) {
        handle.giveExperiencePoints(points);
    }

    public void setPos(double x, double y, double z) {
        handle.setPos(x, y, z);
    }

    // ---------- 背包 / 手持物品 / 原生逃生 ----------

    /**
     * 玩家的随身背包（包装为 {@link Inventory}，底层为原生
     * {@link net.minecraft.world.entity.player.Inventory}）。
     * <p>
     * 若要原生逃生类型请用 {@link Inventory#getHandle()}。
     */
    public Inventory getInventory() {
        return new Inventory(handle.getInventory());
    }

    /** 主手物品（包装版，视图语义）。 */
    public ItemStack getMainHandItem() {
        return new ItemStack(handle.getMainHandItem());
    }

    /** 副手物品（包装版，视图语义）。 */
    public ItemStack getOffhandItem() {
        return new ItemStack(handle.getOffhandItem());
    }

    public FoodData getFoodData() {
        return handle.getFoodData();
    }

    public ServerLevel getLevel() {
        return handle.serverLevel();
    }

    public MinecraftServer getServer() {
        return handle.server;
    }

    @Override
    public String toString() {
        return "Player{" + getName() + " (" + getUUID() + "), gamemode=" + getGameMode() + "}";
    }
}