package com.akiteam.akia.scheduler;

/**
 * 一个由 {@link Scheduler} 调度执行的任务。
 * <p>
 * 任务可以是一次性的同步任务、延迟执行、周期循环（{@code timer}），
 * 或是一次性的异步（后台线程）任务。所有任务都归属某个插件，插件卸载时
 * 该插件的全部任务会被 {@link Scheduler#cancelTasks} 取消。
 * <p>
 * 典型用法：插件在 {@code registerScheduler} 里拿到 {@link Scheduler}，再调用
 * {@code scheduler.runTaskLater(plugin, runnable, delayTicks)} 等返回本对象，
 * 之后若想提前取消，可调用 {@link #cancel()}。
 */
public class Task implements Comparable<Task> {

    /** 全局自增的任务序号，也用作 {@link Scheduler} 内部存储的 key。 */
    private final long id;

    /** 归属插件名称（用于插件卸载时统一取消）。 */
    private final String pluginName;

    /** 是否为同步任务（在主线程上于 {@code ServerTickEvent} 中执行）。 */
    private final boolean sync;

    /** 真正的执行体，由创建时的 {@link Runnable} 包装而来。 */
    private final Runnable runnable;

    /** 周期（ticks）。{@code <= 0} 表示一次性任务；否则为循环任务的执行间隔。 */
    private final long period;

    /** 同步任务下一次应执行的 tick 号；异步任务不使用该字段。 */
    private volatile long nextExecuteTick;

    /** 本任务已被执行的次数。 */
    private volatile int timesRun;

    /** 是否已被取消。 */
    private volatile boolean cancelled;

    /**
     * 由 {@link Scheduler} 创建任务。
     *
     * @param id         任务全局 id
     * @param pluginName 归属插件名称
     * @param sync       是否为同步（主线程）任务
     * @param runnable   执行体
     * @param delayTicks 同步任务的延迟 tick 数（异步任务传 0）
     * @param periodTicks 周期 tick 数（{@code <= 0} 表示一次性）
     * @param currentTick 当前已经过的服务器 tick 数
     */
    Task(long id, String pluginName, boolean sync, Runnable runnable,
         long delayTicks, long periodTicks, long currentTick) {
        this.id = id;
        this.pluginName = pluginName;
        this.sync = sync;
        this.runnable = runnable;
        this.period = periodTicks;
        // 同步任务把"当前 tick + 延迟"换算成绝对的执行 tick；异步任务不参与 tick 计数
        this.nextExecuteTick = sync ? currentTick + Math.max(1, delayTicks) : 0;
    }

    /** @return 任务全局唯一 id */
    public long getTaskId() {
        return id;
    }

    /** @return 归属插件名称 */
    public String getPluginName() {
        return pluginName;
    }

    /** @return 是否为同步（主线程）任务 */
    public boolean isSync() {
        return sync;
    }

    /** @return 是否为周期循环任务 */
    public boolean isRepeating() {
        return period > 0;
    }

    /** @return 本任务已执行的次数 */
    public int getTimesRun() {
        return timesRun;
    }

    /** @return 是否已被取消 */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * 取消本任务。
     * <p>
     * 同步任务尚未执行时会从调度队列中移除；一次性异步任务若已提交后台线程，
     * 无法中断正在执行的线程，但会阻止其后续被记录/再次调度。
     */
    public void cancel() {
        this.cancelled = true;
    }

    /** 供 {@link Scheduler} 在同步 tick 时调用：实际执行体并累计执行次数。 */
    void runNow() {
        timesRun++;
        runnable.run();
    }

    /** @return 同步任务下一次应执行的 tick（仅供 {@link Scheduler} 使用） */
    long getNextExecuteTick() {
        return nextExecuteTick;
    }

    /** 更新同步任务下一次执行 tick（循环任务重新入队时使用）。 */
    void setNextExecuteTick(long tick) {
        this.nextExecuteTick = tick;
    }

    /** @return 周期 tick 数（仅供 {@link Scheduler} 使用） */
    long getPeriod() {
        return period;
    }

    /** @return 执行体（仅供 {@link Scheduler} 在回调完成后做资源回收判断时使用梯度） */
    Runnable getRunnable() {
        return runnable;
    }

    /**
     * 按执行 tick 排序（仅同步任务使用），保证调度队列按时间先后出队。
     *
     * @param o 另一个任务
     * @return 比较结果；同 tick 时按 id 保证稳定顺序
     */
    @Override
    public int compareTo(Task o) {
        int tickCmp = Long.compare(this.nextExecuteTick, o.nextExecuteTick);
        return tickCmp != 0 ? tickCmp : Long.compare(this.id, o.id);
    }
}