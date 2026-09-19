package com.akiteam.akia.game;

import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏对象：一个记分板的包装（对齐 Bukkit {@code Scoreboard}）。
 * <p>
 * 包装 {@code net.minecraft.world.scores.Scoreboard}，向插件提供 Paper 风格常用 API：
 * 注册/查询目标与队伍、读写计分、清理槽位、复位分数。所有读写都走服务端主线程。
 * <p>
 * 记分板从服务端世界取得：{@link World#getScoreboard()}（底层为
 * {@code net.minecraft.server.ServerScoreboard}，自动广播给所有在线玩家）。
 */
public final class Scoreboard {

    private final net.minecraft.world.scores.Scoreboard handle;

    public Scoreboard(net.minecraft.world.scores.Scoreboard handle) {
        this.handle = handle;
    }

    /** 返回被包装的原生记分板（逃生口）。 */
    public net.minecraft.world.scores.Scoreboard getHandle() {
        return handle;
    }

    /** 从原生记分板构造包装（沿袭 {@link World#from} 风格）。 */
    public static Scoreboard from(net.minecraft.world.scores.Scoreboard handle) {
        return new Scoreboard(handle);
    }

    // ---------- 目标（Objective） ----------

    /**
     * 注册一个新目标。
     *
     * @param name        目标内部名（唯一，如 {@code "rank"}）
     * @param criteria    判据（如 {@code "dummy"}、{@code "health"}）；未知判据回退到 {@code dummy}
     * @param displayName 客户端显示标题
     * @return 新目标；同名目标已存在时返回 {@code null}
     */
    public Objective registerNewObjective(String name, String criteria, String displayName) {
        if (handle == null || name == null || name.isEmpty()) {
            return null;
        }
        if (handle.getObjective(name) != null) {
            return null;
        }
        try {
            ObjectiveCriteria c = ObjectiveCriteria.byName(criteria == null || criteria.isEmpty() ? "dummy" : criteria)
                    .orElse(ObjectiveCriteria.DUMMY);
            net.minecraft.world.scores.Objective nativeObj = handle.addObjective(
                    name, c, Component.literal(displayName == null ? name : displayName),
                    c.getDefaultRenderType(), false, null);
            return new Objective(this, nativeObj);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 按内部名取目标；不存在返回 {@code null}。 */
    public Objective getObjective(String name) {
        if (handle == null || name == null) {
            return null;
        }
        net.minecraft.world.scores.Objective nativeObj = handle.getObjective(name);
        return nativeObj == null ? null : new Objective(this, nativeObj);
    }

    /** 取指定显示槽上当前挂载的目标；无则返回 {@code null}。 */
    public Objective getObjective(DisplaySlot slot) {
        if (handle == null || slot == null) {
            return null;
        }
        net.minecraft.world.scores.Objective nativeObj = handle.getDisplayObjective(slot.toNative());
        return nativeObj == null ? null : new Objective(this, nativeObj);
    }

    /** 当前全部已注册目标。 */
    public List<Objective> getObjectives() {
        List<Objective> out = new ArrayList<>();
        if (handle != null) {
            for (net.minecraft.world.scores.Objective o : handle.getObjectives()) {
                out.add(new Objective(this, o));
            }
        }
        return out;
    }

    /** 清空指定显示槽（把槽位上的目标取消挂载）。 */
    public void clearSlot(DisplaySlot slot) {
        if (handle != null && slot != null) {
            handle.setDisplayObjective(slot.toNative(), null);
        }
    }

    // ---------- 队伍（Team） ----------

    /** 注册一个新队伍；同名队伍已存在时返回已有队伍（与 Paper 行为一致，不抛异常）。 */
    public Team registerNewTeam(String name) {
        if (handle == null || name == null || name.isEmpty()) {
            return null;
        }
        net.minecraft.world.scores.PlayerTeam nativeTeam = handle.addPlayerTeam(name);
        return nativeTeam == null ? null : new Team(this, nativeTeam);
    }

    /** 按名字取队伍；不存在返回 {@code null}。 */
    public Team getTeam(String name) {
        if (handle == null || name == null) {
            return null;
        }
        net.minecraft.world.scores.PlayerTeam nativeTeam = handle.getPlayerTeam(name);
        return nativeTeam == null ? null : new Team(this, nativeTeam);
    }

    /** 当前全部已注册队伍。 */
    public List<Team> getTeams() {
        List<Team> out = new ArrayList<>();
        if (handle != null) {
            for (net.minecraft.world.scores.PlayerTeam t : handle.getPlayerTeams()) {
                out.add(new Team(this, t));
            }
        }
        return out;
    }

    /** 指定 entry 所属的队伍；不属于任何队伍返回 {@code null}。 */
    public Team getEntryTeam(String entry) {
        if (handle == null || entry == null) {
            return null;
        }
        net.minecraft.world.scores.PlayerTeam nativeTeam = handle.getPlayersTeam(entry);
        return nativeTeam == null ? null : new Team(this, nativeTeam);
    }

    /** 记分板中所有已被追踪（有分数记录）的 entry 名。 */
    public List<String> getEntries() {
        List<String> out = new ArrayList<>();
        if (handle == null) {
            return out;
        }
        for (net.minecraft.world.scores.ScoreHolder holder : handle.getTrackedPlayers()) {
            out.add(holder.getScoreboardName());
        }
        return out;
    }

    /** 复位指定 entry 在所有目标下的分数（等同于删除其分数记录）。 */
    public void resetScores(String entry) {
        if (handle != null && entry != null) {
            handle.resetAllPlayerScores(net.minecraft.world.scores.ScoreHolder.forNameOnly(entry));
        }
    }

    // ---------- 内部辅助（供 Objective/Score 使用） ----------

    /** 取得某 entry 在指定原生目标下可写的分数访问器（逃生口用途）。 */
    net.minecraft.world.scores.ScoreAccess getOrCreatePlayerScore(String entry,
                                                                   net.minecraft.world.scores.Objective objective) {
        return handle.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly(entry), objective);
    }

    /** 查询某 entry 在指定原生目标下是否已有分数记录（逃生口用途）。 */
    boolean hasPlayerScore(String entry, net.minecraft.world.scores.Objective objective) {
        return handle.getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly(entry), objective) != null;
    }

    /** 复位某 entry 在指定原生目标下的分数（逃生口用途）。 */
    void resetPlayerScore(String entry, net.minecraft.world.scores.Objective objective) {
        handle.resetSinglePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly(entry), objective);
    }
}