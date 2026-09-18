package com.akiteam.akia.scheduler;

import com.akiteam.akia.api.AkiPlugin;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

/**
 * Akia 的调度器（参考 Paper {@code BukkitScheduler} 的设计思路）。
 * <p>
 * 提供两类执行环境：
 * <ul>
 *     <li><b>同步任务</b>：在游戏主线程（{@code ServerThread}）上、于每游戏刻的
 *         {@code ServerTickEvent.Post} 中消费执行。因为和游戏逻辑同线程，可以安全地
 *         修改 {@code BlockState}、实体、方块等。</li>
 *     <li><b>异步任务</b>：提交到内部的后台线程池执行，适合做 IO、路径搜索、网络请求
 *         等耗时操作——切忌在异步任务里直接触碰游戏对象。</li>
 * </ul>
 * <p>
 * 任务归属某个插件，插件卸载时通过 {@link #cancelTasks(AkiPlugin)} 取消其全部任务，
 * 并清空对插件类加载器的引用，避免 {@code Metaspace} 内存泄漏。
 * <p>
 * 本类为全局单例 {@link #INSTANCE}，由 {@code Akia} 在每游戏刻注册的
 * {@code ServerTickEvent.Post} 回调驱动同步任务的执行。
 */
public class Scheduler {

    /** 全局唯一调度器实例。 */
    public static final Scheduler INSTANCE = new Scheduler();

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 下一个任务的全局 id。 */
    private final AtomicLong taskIds = new AtomicLong();

    /** 后台线程池：执行异步任务。线程设为 daemon，随 JVM 退出自动结束。 */
    private final ScheduledExecutorService asyncExecutor = Executors.newScheduledThreadPool(
            2, r -> {
                Thread t = new Thread(r, "Akia-Async");
                t.setDaemon(true);
                return t;
            });

    /** 当前已度过的服务器 tick 数（仅主线程在 {@code onServerTick} 中累加）。 */
    private volatile long currentTick = 0;

    /** 待执行的同步任务队列，按下次执行 tick 从小到大排序。 */
    private final PriorityBlockingQueue<Task> syncQueue = new PriorityBlockingQueue<>();

    /** 以任务 id 为 key 记录所有任务（同步 + 异步），用于快速查找与回收。 */
    private final ConcurrentHashMap<Long, Task> taskById = new ConcurrentHashMap<>();

    /** 以插件名称为 key 记录该插件名下的任务 id 集合，用于插件卸载时整体取消。 */
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<Long>> pluginTasks = new ConcurrentHashMap<>();

    /** 空构造器（单例模式）。 */
    private Scheduler() {
    }

    /**
     * 统计某插件名下当前登记的任务数量（同步 + 异步，含已取消但未回收的）。
     * 供诊断命令统计每个插件的调度任务数量。
     *
     * @param plugin 目标插件
     * @return 该插件当前的任务数
     */
    public int getTaskCount(AkiPlugin plugin) {
        if (plugin == null) {
            return 0;
        }
        CopyOnWriteArraySet<Long> ids = pluginTasks.get(plugin.getName());
        return ids == null ? 0 : ids.size();
    }

    /**
     * 在下一个游戏刻于主线程执行一次同步任务。
     *
     * @param plugin   任务所属插件
     * @param runnable 执行体
     * @return 创建的任务
     */
    public Task runTask(AkiPlugin plugin, Runnable runnable) {
        return schedule(plugin, true, runnable, 1, 0);
    }

    /**
     * 延迟若干游戏刻后，于主线程执行一次同步任务。
     *
     * @param plugin     任务所属插件
     * @param runnable   执行体
     * @param delayTicks 延迟刻数（至少 1；传入小于 1 按下 1 处理）
     * @return 创建的任务
     */
    public Task runTaskLater(AkiPlugin plugin, Runnable runnable, long delayTicks) {
        return schedule(plugin, true, runnable, Math.max(1, delayTicks), 0);
    }

    /**
     * 延迟若干刻后开始，并在主线程上按固定周期重复执行。
     *
     * @param plugin      任务所属插件
     * @param runnable    执行体
     * @param delayTicks  首次执行的延迟刻数（允许 0，即当前刻之后最近的 tick）
     * @param periodTicks 执行间隔刻数（至少 1）
     * @return 创建的任务
     */
    public Task runTaskTimer(AkiPlugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        return schedule(plugin, true, runnable, Math.max(0, delayTicks), Math.max(1, periodTicks));
    }

    /**
     * 在后台线程池中立即异步执行一次。
     * <p>
     * 用于耗时且不触碰游戏对象的操作。异步回调里若要修改游戏状态，
     * 应在回调内用 {@code runTask(plugin, ...)} 切回主线程。
     *
     * @param plugin   任务所属插件
     * @param runnable 执行体
     * @return 创建的任务（正在执行时调用 {@code cancel()} 无法中断线程本体）
     */
    public Task runTaskAsynchronously(AkiPlugin plugin, Runnable runnable) {
        return schedule(plugin, false, runnable, 0, 0);
    }

    /**
     * 由主线程的每游戏刻回调调用：消费到期的同步任务。
     * <p>
     * （不应由插件直接调用。）对到期的每个同步任务：未取消则执行；重复任务重新
     * 按周期入队；一次性任务执行完后从内部结构回收，释放对插件类的引用。
     */
    public void onServerTick() {
        currentTick++;

        List<Task> due = new ArrayList<>();
        Task head;
        while ((head = syncQueue.peek()) != null && head.getNextExecuteTick() <= currentTick) {
            syncQueue.poll();
            due.add(head);
        }

        for (Task task : due) {
            if (task.isCancelled()) {
                cleanup(task);
                continue;
            }
            try {
                task.runNow();
            } catch (Throwable t) {
                // 单个任务异常不影响其余任务与游戏本身
                LOGGER.error("Error executing task {} of plugin '{}': {}", task.getTaskId(),
                        task.getPluginName(), t.toString(), t);
            }
            if (task.isRepeating() && !task.isCancelled()) {
                task.setNextExecuteTick(currentTick + task.getPeriod());
                syncQueue.offer(task);
            } else {
                cleanup(task);
            }
        }
    }

    /**
     * 取消某插件名下的所有任务，并清理引用。
     * <p>
     * 供插件卸载流程调用（见 {@code PluginManagerImpl#unloadPlugin}），
     * 防止插件被卸载后其任务仍在运行或持有其类加载器引用。
     *
     * @param plugin 目标插件
     */
    public void cancelTasks(AkiPlugin plugin) {
        if (plugin == null) {
            return;
        }
        CopyOnWriteArraySet<Long> ids = pluginTasks.remove(plugin.getName());
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            Task task = taskById.remove(id);
            if (task != null) {
                task.cancel();
                syncQueue.remove(task);
            }
        }
        LOGGER.info("Cancelled {} task(s) for plugin '{}'.", ids.size(), plugin.getName());
    }

    /**
     * 创建一个任务并登记到内部结构中。
     *
     * @param plugin     所属插件
     * @param sync       是否同步（主线程）任务
     * @param runnable   执行体
     * @param delayTicks 同步任务的延迟刻数
     * @param periodTicks 周期刻数（{@code <=0} 为一次性）
     * @return 创建的任务
     */
    private Task schedule(AkiPlugin plugin, boolean sync, Runnable runnable,
                          long delayTicks, long periodTicks) {
        if (runnable == null) {
            throw new IllegalArgumentException("runnable must not be null");
        }
        long id = taskIds.incrementAndGet();
        String pluginName = (plugin == null) ? "unknown" : plugin.getName();
        Task task = new Task(id, pluginName, sync, runnable, delayTicks, periodTicks, currentTick);

        taskById.put(id, task);
        pluginTasks.computeIfAbsent(pluginName, k -> new CopyOnWriteArraySet<>()).add(id);

        if (sync) {
            syncQueue.offer(task);
        } else {
            // 异步一次性任务：提交后台线程池，执行完成后清理引用
            asyncExecutor.execute(() -> {
                if (task.isCancelled()) {
                    return;
                }
                try {
                    task.runNow();
                } catch (Throwable t) {
                    LOGGER.error("Async task {} of plugin '{}' threw: {}", task.getTaskId(),
                            task.getPluginName(), t.toString(), t);
                } finally {
                    cleanup(task);
                }
            });
        }
        return task;
    }

    /** 关闭后台线程池（供游戏退出时调用；正常情况下 daemon 线程随 JVM 结束）。 */
    public void shutdown() {
        asyncExecutor.shutdownNow();
        try {
            if (!asyncExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                LOGGER.warn("Async executor did not terminate within timeout.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 从所有登记结构中移除某个任务，释放其对插件类的引用。 */
    private void cleanup(Task task) {
        taskById.remove(task.getTaskId());
        CopyOnWriteArraySet<Long> ids = pluginTasks.get(task.getPluginName());
        if (ids != null) {
            ids.remove(task.getTaskId());
        }
    }
}