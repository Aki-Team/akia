package com.akiteam.akia.game;

import com.akiteam.akia.text.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 游戏对象：玩家的只读视图 + 常用操作包装。
 * <p>
 * 包装 {@link ServerPlayer}，向插件提供 Bukkit 开发者熟悉的 API：读玩家属性、
 * 发消息、传送、踢出、切模式、发经验、设置坐标。
 * <p>
 * 涉及 World / ItemStack / Inventory / FoodData 的返回值按约定暂返原生类型，
 * 由后续任务再行包装。玩家操作须在服务端线程调用。
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

    // ---------- 暂返原生（后续任务再包装） ----------

    public Inventory getInventory() {
        return handle.getInventory();
    }

    public ItemStack getMainHandItem() {
        return handle.getMainHandItem();
    }

    public ItemStack getOffhandItem() {
        return handle.getOffhandItem();
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