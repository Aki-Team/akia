package com.akiteam.akia.game;

/**
 * 游戏对象：一个目标下某 entry 的分数读写句柄（对齐 Bukkit {@code Score}）。
 * <p>
 * 包装原生 {@code net.minecraft.world.scores.ScoreAccess}：{@link #getScore}/{@link #setScore}
 * 直接命中该 entry 在该目标下的实时分数，无需额外写回。由 {@link Objective#getScore} 取得。
 */
public final class Score {

    private final Objective objective;
    private final String entry;
    private final net.minecraft.world.scores.ScoreAccess handle;

    Score(Objective objective, String entry, net.minecraft.world.scores.ScoreAccess handle) {
        this.objective = objective;
        this.entry = entry;
        this.handle = handle;
    }

    /** 返回底层分数访问器（逃生口，覆盖读写/锁定等原生能力）。 */
    public net.minecraft.world.scores.ScoreAccess getHandle() {
        return handle;
    }

    /** 该分数的 entry 名（分数所有者）。 */
    public String getEntry() {
        return entry;
    }

    /** 当前分数值。 */
    public int getScore() {
        return handle.get();
    }

    /** 设置分数值。 */
    public void setScore(int value) {
        handle.set(value);
    }

    /** 该 entry 是否已有分数记录（即分数是否真正写入过）。 */
    public boolean isScoreSet() {
        return objective.getScoreboard().hasPlayerScore(entry, objective.getHandle());
    }

    /** 该分数所属的目标。 */
    public Objective getObjective() {
        return objective;
    }

    /** 复位该 entry 在该目标下的分数（删除其分数记录并广播移除）。 */
    public void resetScore() {
        objective.getScoreboard().resetPlayerScore(entry, objective.getHandle());
    }

    @Override
    public String toString() {
        return "Score{" + entry + " = " + getScore() + " @ " + objective.getName() + "}";
    }
}