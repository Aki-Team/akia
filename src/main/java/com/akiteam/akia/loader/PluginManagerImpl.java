package com.akiteam.akia.loader;

import com.akiteam.akia.Akia;
import com.akiteam.akia.api.AkiPlugin;
import com.akiteam.akia.api.PluginInfo;
import com.akiteam.akia.api.PluginManager;
import com.akiteam.akia.command.CommandRegistry;
import com.akiteam.akia.config.ConfigManager;
import com.akiteam.akia.diag.DumpReport;
import com.akiteam.akia.diag.PluginDiagnostic;
import com.akiteam.akia.diag.ServiceEntry;
import com.akiteam.akia.event.EventBus;
import com.akiteam.akia.persistence.AkiAdapterContext;
import com.akiteam.akia.persistence.impl.AkiAdapterContextImpl;
import com.akiteam.akia.scheduler.Scheduler;
import com.akiteam.akia.service.RegisteredServiceProvider;
import com.akiteam.akia.service.ServiceRegistry;
import com.akiteam.akia.service.impl.ServiceRegistryImpl;
import com.akiteam.akia.service.impl.VersionParser;
import com.akiteam.akia.text.AkiText;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * {@link PluginManager} 的默认实现，负责插件 JAR 的加载、启用、禁用与卸载。
 * <p>
 * <b>类隔离设计</b>（参考 Paper {@code SimplePluginManager}）：每个插件使用
 * 一个独立的 {@link URLClassLoader}，其父加载器是 Akia 自己的类加载器。
 * 这样插件能访问 Akia 的公共 API、Minecraft 与 NeoForge 的类，
 * 同时插件自身的类互相隔离、互不干扰，避免同名类冲突。
 * <p>
 * 所有操作都捕获异常并记录日志，不向上抛出未处理的异常，确保即使单个插件
 * 损坏也不会导致整局游戏崩溃。
 *
 * <p>线程安全：内部状态存放在 {@link ConcurrentHashMap} 中，可安全地在多个
 * 线程访问。实际加载（如 {@code runClient} 启动）通常在主线程完成，无需额外并发控制。
 */
public class PluginManagerImpl implements PluginManager {

    /** 记录插件 JAR 内元数据文件的标准文件名。 */
    public static final String PLUGIN_METADATA_FILE = "plugin.json";

    private static final org.slf4j.Logger LOGGER = Akia.LOGGER;

    /** 以插件名称为 key 存储每个已加载插件的运行态。 */
    private final ConcurrentHashMap<String, PluginEntry> plugins = new ConcurrentHashMap<>();

    /** 记录当前已启用（onEnable 已调用）的插件名称集合。 */
    private final Set<String> enabledNames = ConcurrentHashMap.newKeySet();

    /**
     * 插件的加载顺序快照（依赖者靠后），供 reload 时反向卸载。
     * 仅在主线程访问，故用普通 {@link ArrayList}。
     */
    private final List<String> loadOrder = new ArrayList<>();

    /** 插件共享的事件总线，Akia 的游戏事件桥接器与所有插件共用。 */
    private final EventBus eventBus = new EventBus();

    /** 插件共享的命令注册表，{@link RegisterCommandsEvent} 时统一注册到 Brigadier。 */
    private final CommandRegistry commandRegistry = new CommandRegistry();

    /** 插件共享的调度器：同步任务在主线程 tick 上执行，异步任务在后台线程池执行。 */
    private final Scheduler scheduler = Scheduler.INSTANCE;

    /** 所有插件配置的统一管理器。 */
    private final ConfigManager configManager = ConfigManager.INSTANCE;

    /** 插件共享的 PDC 适配上下文（用于创建持久化容器）。 */
    private final AkiAdapterContext persistenceContext = AkiAdapterContextImpl.INSTANCE;

    /** 插件共享的服务注册表：插件之间按接口类型提供 / 获取服务（卸载时自动清理）。 */
    private final ServiceRegistry serviceRegistry = new ServiceRegistryImpl(eventBus);

    /**
     * API 导出索引：包名 → 导出该包的插件类加载器（所有插件共享）。
     * 用于跨插件 API 类共享——消费者加载导出包时委派给提供者加载器，保证同一类只加载一次。
     */
    private final ConcurrentMap<String, AkiPluginClassLoader> exportIndex = new ConcurrentHashMap<>();

    /** 最近扫描的插件目录（也由 reloadPlugins 复用以重新扫描）。 */
    private volatile Path pluginDir;

    /** 最近一次依赖图扫描中“存在但未能加载”的插件数（强依赖缺失 / 成环 / 依赖链未满足）。 */
    private volatile int lastFailedToLoad;

    /**
     * 返回插件共享的事件总线。
     *
     * @return 事件总线实例（不会为 {@code null}）
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * 返回插件共享的命令注册表。
     *
     * @return 命令注册表实例（不会为 {@code null}）
     */
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * 返回插件共享的调度器。
     *
     * @return 调度器实例（不会为 {@code null}）
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * 扫描指定目录，并自动加载其中的全部 {@code .jar} 插件。
     * <p>
     * 目录不存在时会先创建。单个插件加载失败只记录日志，不影响其余插件加载与游戏启动。
     * 该目录会被记住，供 {@link #reloadPlugins()} 复用。
     * <p>
     * <b>依赖解析</b>（参考 Paper {@code SimplePluginManager}）：加载前会先解析全部
     * {@code plugin.json}，构建依赖图并按拓扑序加载。
     * <ul>
     *     <li>{@code depend}（强依赖）缺失 → 该插件跳过且报错；</li>
     *     <li>{@code softDepend}（软依赖）缺失 → 仅跳过该项，不影响加载；</li>
     *     <li>{@code loadBefore} → 请求在该插件之前加载目标插件；</li>
     *     <li>依赖成环或无法满足 → 相关插件不加载并报错。</li>
     * </ul>
     *
     * @param dir 插件目录
     */
    public void loadAllFromDirectory(Path dir) {
        if (dir == null) {
            LOGGER.error("Cannot scan plugins directory: directory is null.");
            return;
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("Failed to create plugins directory {}: {}", dir, e.toString(), e);
            return;
        }
        this.pluginDir = dir;

        List<File> jarFiles = new ArrayList<>();
        try (Stream<Path> entries = Files.list(dir)) {
            entries.filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                    .sorted()
                    .forEach(p -> jarFiles.add(p.toFile()));
        } catch (IOException e) {
            LOGGER.error("Failed to scan plugins directory {}: {}", dir, e.toString(), e);
            return;
        }

        loadInDependencyOrder(jarFiles);
        LOGGER.info("Akia: {} plugin(s) loaded from {}.", getLoadedPlugins().size(), dir);
    }

    /**
     * 按依赖关系拓扑排序后依次加载插件（Kahn 算法）。
     * <p>
     * 一个扫描到的插件 JAR 只有三种结局：
     * <ul>
     *     <li>成功进入 {@link #plugins} 表（调用过 {@link #loadPlugin}）；</li>
     *     <li>无法加载：存在缺失的强依赖（{@code depend}），或依赖链无法满足/成环——仅记录日志，
     *         不加载，不影响其余插件；</li>
     *     <li>元数据解析失败或重名——在收集阶段已跳过并记录。</li>
     * </ul>
     *
     * @param jarFiles 候选插件 JAR 列表
     */
    private void loadInDependencyOrder(List<File> jarFiles) {
        int loadedBefore = plugins.size();

        // Phase 1：解析全部候选元数据，去重
        Map<String, Candidate> byName = new LinkedHashMap<>();
        List<Candidate> candidates = new ArrayList<>();
        for (File file : jarFiles) {
            PluginInfo info;
            try {
                info = readPluginInfo(file);
            } catch (IOException e) {
                LOGGER.error("Failed to read plugin metadata from {}: {}", file, e.toString(), e);
                continue;
            }
            if (info == null) {
                continue; // 缺失 plugin.json 或解析失败，readPluginInfo 已记录
            }
            Candidate candidate = new Candidate(file, info);
            if (byName.putIfAbsent(info.name(), candidate) != null) {
                LOGGER.error("Duplicate plugin name '{}' (in {}); skipping {}.", info.name(), file.getName(), file.getName());
                continue;
            }
            candidates.add(candidate);
        }

        // Phase 2：构建依赖图（前置插件 → 依赖它的插件）
        for (Candidate candidate : candidates) {
            for (String hardDep : candidate.info.depend()) {
                Candidate dep = byName.get(hardDep);
                if (dep == null) {
                    candidate.hardDepMissing = true; // 强依赖缺失：本插件的可加载性被否定
                } else {
                    candidate.hardDeps.add(hardDep);
                    addDependencyEdge(dep, candidate);
                }
            }
            for (String softDep : candidate.info.softDepend()) {
                Candidate dep = byName.get(softDep);
                if (dep != null) {
                    addDependencyEdge(dep, candidate); // 软依赖缺失直接忽略
                }
            }
            for (String loadBefore : candidate.info.loadBefore()) {
                Candidate target = byName.get(loadBefore);
                if (target != null) {
                    addDependencyEdge(candidate, target); // 本插件要先于目标加载
                }
            }
        }

        // Phase 3：Kahn 拓扑排序 + 顺次加载
        Deque<Candidate> ready = new ArrayDeque<>();
        for (Candidate candidate : candidates) {
            if (!candidate.hardDepMissing && candidate.inDegree == 0) {
                ready.add(candidate);
            }
        }
        while (!ready.isEmpty()) {
            Candidate candidate = ready.poll();
            if (allHardDepsLoaded(candidate)) {
                if (!loadPlugin(candidate.file)) {
                    LOGGER.error("Could not load plugin '{}'.", candidate.info.name());
                }
            }
            // 无论如何都递减依赖者入度，让其后继能到达队首并被判定
            for (Candidate dependent : candidate.dependents) {
                if (--dependent.inDegree == 0) {
                    ready.add(dependent);
                }
            }
        }

        // Phase 4：汇总仍未加载的插件（强依赖缺失 / 成环 / 依赖链未满足）
        for (Candidate candidate : candidates) {
            if (plugins.containsKey(candidate.info.name())) {
                continue;
            }
            if (candidate.hardDepMissing) {
                LOGGER.error("Could not load plugin '{}': missing required dependency {}.", candidate.info.name(), candidate.info.depend());
            } else {
                LOGGER.warn("Could not load plugin '{}': circular dependency or unsatisfiable dependency chain.", candidate.info.name());
            }
        }

        // 记录本次扫描“存在但未能加载”的数量，供热重载如实统计失败数
        this.lastFailedToLoad = candidates.size() - (plugins.size() - loadedBefore);
    }

    /**
     * 添加一条依赖边：{@code from} 必须先于 {@code to} 加载。
     * 重复边会被去重，避免重复计数入度。
     */
    private static void addDependencyEdge(Candidate from, Candidate to) {
        if (to.prerequisites.contains(from)) {
            return;
        }
        to.prerequisites.add(from);
        to.inDegree++;
        from.dependents.add(to);
    }

    /** 判断某候选的强依赖是否已全部成功加载（进入 {@link #plugins} 表）。 */
    private boolean allHardDepsLoaded(Candidate candidate) {
        for (String hardDep : candidate.hardDeps) {
            if (!plugins.containsKey(hardDep)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 依赖图构建过程中的一个候选插件节点。
     * <p>
     * 仅在 {@link #loadInDependencyOrder} 内部使用，不对外暴露。
     */
    private static final class Candidate {
        final File file;
        final PluginInfo info;
        /** 已确认存在的强依赖（depend）名称，用于加载前最终校验。 */
        final List<String> hardDeps = new ArrayList<>();
        /** 必须先于本插件加载的前置节点。 */
        final List<Candidate> prerequisites = new ArrayList<>();
        /** 依赖本插件的后继节点。 */
        final List<Candidate> dependents = new ArrayList<>();
        /** 存在候选集中找不到的强依赖 → 本插件必然无法加载。 */
        boolean hardDepMissing;
        /** 尚未满足的前置数量（Kahn 入度）。 */
        int inDegree;

        Candidate(File file, PluginInfo info) {
            this.file = file;
            this.info = info;
        }
    }

    /**
     * 热重载全部插件（参考 Paper 的 reload 流程，但更注重资源清理）。
     * <p>
     * 流程：禁用所有已启用插件（{@code onDisable}）→ 卸载所有插件（释放 ClassLoader、
     * 清理事件监听、取消调度任务）→ 清空命令 → 重新扫描加载（{@code onLoad}）→
     * 重新注册命令 → 启用全部新插件（{@code onEnable}）。
     * <p>
     * 仅供开发调试使用。全程在主线程（服务器线程）执行，某个插件失败仅记录日志，
     * 不影响其余插件与服务器稳定性。
     *
     * @return 长度为 2 的数组：{@code [启用成功数, 启用失败数]}
     */
    public int[] reloadPlugins() {
        LOGGER.info("Akia: reloading plugins...");

        // 0. 重载前先保存所有插件配置，避免卸载后未保存的改动丢失
        configManager.saveAllConfigs();

        // 1. 按加载顺序反向卸载所有插件（依赖者先卸载）。
        //    卸载统一由 unloadPlugin 完成：它内部负责调用 onDisable、取消调度任务、
        //    清理事件监听与配置、关闭 ClassLoader，保证每个插件只 onDisable 一次。
        List<String> unloadOrder = new ArrayList<>(loadOrder);
        Collections.reverse(unloadOrder);
        for (String name : unloadOrder) {
            unloadPlugin(name);
        }

        // 2. 清空内部状态（防御性；unloadPlugin 已移除大部分条目）
        plugins.clear();
        enabledNames.clear();
        loadOrder.clear();

        // 3. 清空命令注册表与已注册节点
        commandRegistry.clearPluginCommands();

        // 4. 重新扫描并加载（如果尚未扫描过任何目录，则无可重载）
        if (pluginDir == null) {
            LOGGER.warn("Akia: no plugin directory known; skipping rescan.");
        } else {
            loadAllFromDirectory(pluginDir);
        }

        // 5. 重新注册命令（RegisterCommandsEvent 不会再次触发，需手动调用）
        commandRegistry.registerAllCommands();

        // 6. 启用所有新加载的插件，按依赖拓扑序（见 enableAllPlugins 注释）
        int[] result = enableAllPlugins();
        LOGGER.info("Akia: reload finished. {} plugin(s) enabled, {} failed.", result[0], result[1]);
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 按 {@code loadOrder}（依赖拓扑序，依赖者靠后）逐个启用。必须用拓扑序而非
     * {@link #getLoadedPlugins()}（映射到 {@code ConcurrentHashMap.values()}，顺序不保证），
     * 否则消费者可能先于提供者执行 {@code onEnable}，取不到后者注册的服务。
     * 失败数把依赖解析阶段加载失败的插件也一并计入，保证统计如实。
     */
    @Override
    public int[] enableAllPlugins() {
        int enabled = 0;
        int failed = lastFailedToLoad;
        for (String name : new ArrayList<>(loadOrder)) {
            if (enablePlugin(name)) {
                enabled++;
            } else {
                failed++;
            }
        }
        return new int[] { enabled, failed };
    }

    /**
     * {@inheritDoc}
     * <p>
     * 按 {@code loadOrder} 的逆序（依赖者靠前）禁用，保证消费者先释放、再回头禁用其依赖。
     */
    @Override
    public int[] disableAllPlugins() {
        int disabled = 0;
        int failed = 0;
        List<String> order = new ArrayList<>(loadOrder);
        Collections.reverse(order);
        for (String name : order) {
            if (disablePlugin(name)) {
                disabled++;
            } else {
                failed++;
            }
        }
        return new int[] { disabled, failed };
    }

    /**
     * 一个已加载插件的内部运行态：
     *
     * @param plugin 插件主类实例
     * @param info   插件元数据
     * @param loader 专属类加载器，用于加载/卸载该插件的类
     */
    private record PluginEntry(AkiPlugin plugin, PluginInfo info, AkiPluginClassLoader loader) {
    }

    /** 空构造器。 */
    public PluginManagerImpl() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * 加载流程：读取 {@code plugin.json} → 校验名称未重复 → 创建独立 ClassLoader →
     * 反射实例化并强制转换为 {@link AkiPlugin} → 调用 {@link AkiPlugin#onLoad()} →
     * 存入内部表。任何一步失败都会关闭 ClassLoader 并返回 {@code false}。
     */
    @Override
    public boolean loadPlugin(File jarFile) {
        if (jarFile == null || !jarFile.isFile() || !jarFile.getName().toLowerCase().endsWith(".jar")) {
            LOGGER.error("Invalid plugin jar file: {}", jarFile);
            return false;
        }

        AkiPluginClassLoader loader = null;
        try {
            PluginInfo info = readPluginInfo(jarFile);
            if (info == null) {
                return false;
            }
            if (plugins.containsKey(info.name())) {
                LOGGER.error("A plugin named '{}' is already loaded; skipping {}.", info.name(), jarFile.getName());
                return false;
            }
            // api-version 校验：插件声明的 API 版本不能高于 Akia 当前版本，否则视为不兼容
            if (VersionParser.compare(info.apiVersion(), Akia.API_VERSION) > 0) {
                LOGGER.error("Plugin '{}' requires api-version '{}', but Akia is at API version '{}'. "
                        + "Refusing to load {}; update Akia or the plugin.", info.name(), info.apiVersion(),
                        Akia.API_VERSION, jarFile.getName());
                return false;
            }

            loader = new AkiPluginClassLoader(
                    new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader(),
                    info.name(), exportIndex);
            // 在实例化 / onLoad 之前注册 API 导出索引，确保插件类加载阶段即可跨插件共享 API
            registerExports(info, loader);
            AkiPlugin plugin = instantiatePlugin(info, loader);

            // 先触发 onLoad，随后注册插件事件监听器，全部成功后才登记，避免失败时残留条目
            plugin.onLoad();
            eventBus.registerEvents(plugin, plugin);   // 自动注册插件主类上的 @EventHandler 方法
            plugin.registerEvents(eventBus);           // 插件自定义事件注册钩子（默认空实现）
            plugin.registerCommands(commandRegistry);  // 插件命令注册钩子（默认空实现）
            plugin.registerScheduler(scheduler);       // 插件调度器注册钩子（默认空实现）
            plugin.registerConfig(configManager.getConfig(plugin)); // 插件配置文件注入（默认空实现）
            plugin.registerPersistence(persistenceContext);        // 插件 PDC 适配上下文注入（默认空实现）
            plugin.registerText(AkiText.INSTANCE);                 // 插件文本组件工具注入（默认空实现）
            plugin.registerServices(serviceRegistry);              // 插件服务注册表注入（默认空实现）
            plugins.put(info.name(), new PluginEntry(plugin, info, loader));
            loadOrder.add(info.name());

            LOGGER.info("Plugin '{}' v{} loaded from {}.", info.name(), info.version(), jarFile.getName());
            return true;
        } catch (Exception e) {
            closeQuietly(loader);
            LOGGER.error("Failed to load plugin from {}: {}", jarFile, e.toString(), e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 只有已加载（存在）且尚未启用的插件才能被启用；重复启用或插件不存在都返回 {@code false}。
     */
    @Override
    public boolean enablePlugin(String name) {
        PluginEntry entry = plugins.get(name);
        if (entry == null) {
            LOGGER.error("Cannot enable plugin '{}': not loaded.", name);
            return false;
        }
        if (!enabledNames.add(name)) {
            LOGGER.warn("Plugin '{}' is already enabled.", name);
            return false;
        }
        try {
            entry.plugin().onEnable();
            LOGGER.info("Plugin '{}' enabled.", name);
            return true;
        } catch (Exception e) {
            enabledNames.remove(name);
            LOGGER.error("Failed to enable plugin '{}': {}", name, e.toString(), e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 只有已加载且已启用的插件才能被禁用；插件不存在或尚未启用都返回 {@code false}。
     */
    @Override
    public boolean disablePlugin(String name) {
        PluginEntry entry = plugins.get(name);
        if (entry == null) {
            LOGGER.error("Cannot disable plugin '{}': not loaded.", name);
            return false;
        }
        if (!enabledNames.remove(name)) {
            LOGGER.warn("Plugin '{}' is not enabled; nothing to disable.", name);
            return false;
        }
        try {
            entry.plugin().onDisable();
            LOGGER.info("Plugin '{}' disabled.", name);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to disable plugin '{}': {}", name, e.toString(), e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AkiPlugin getPlugin(String name) {
        PluginEntry entry = plugins.get(name);
        return entry == null ? null : entry.plugin();
    }

    /**
     * {@inheritDoc}
     * <p>
     * 返回元数据的一份新列表，不持有内部表引用，避免调用方并发修改问题。
     */
    @Override
    public List<PluginInfo> getLoadedPlugins() {
        return plugins.values().stream()
                .map(PluginEntry::info)
                .collect(Collectors.toList());
    }

    /**
     * 卸载指定插件（接口暂未声明，此为 {@link PluginManagerImpl} 的扩展能力）。
     * <p>
     * <b>防 Metaspace 内存泄漏的关键</b>：先调用 {@link AkiPlugin#onDisable()} 释放业务资源，
     * 再从内部表移除引用，最后主动 {@link URLClassLoader#close()} 关闭类加载器，
     * 使插件类的 Class 对象及其关联方法区数据可被 GC。
     *
     * @param name 插件名称
     * @return 成功卸载返回 {@code true}；插件不存在返回 {@code false}
     */
    public boolean unloadPlugin(String name) {
        PluginEntry entry = plugins.remove(name);
        if (entry == null) {
            LOGGER.error("Cannot unload plugin '{}': not loaded.", name);
            return false;
        }
        enabledNames.remove(name);
        loadOrder.remove(name);

        try {
            entry.plugin().onDisable();
        } catch (Exception e) {
            LOGGER.error("Error while disabling plugin '{}' during unload: {}", name, e.toString(), e);
        }
        // 取消该插件的全部调度任务，防止其继续占用引用导致 Metaspace 泄漏
        scheduler.cancelTasks(entry.plugin());
        eventBus.unregisterAll(entry.plugin());
        serviceRegistry.unregisterAll(entry.plugin());  // 注销该插件注册的全部服务
        configManager.removeConfig(name);   // 释放旧插件持有的配置引用，防止实例泄漏
        unregisterExports(entry.info(), entry.loader());  // 移除该插件导出的 API 包索引
        closeQuietly(entry.loader());

        LOGGER.info("Plugin '{}' unloaded.", name);
        return true;
    }

    /**
     * 从 JAR 内读取并解析 {@code plugin.json}。
     *
     * @param file 插件 JAR 文件
     * @return 解析出的 {@link PluginInfo}；若缺少元数据文件或解析失败返回 {@code null}
     * @throws IOException 当无法读取 JAR 时
     */
    private PluginInfo readPluginInfo(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            ZipEntry entry = jar.getEntry(PLUGIN_METADATA_FILE);
            if (entry == null) {
                LOGGER.error("'{}' is missing {}; not a valid Akia plugin.", file.getName(), PLUGIN_METADATA_FILE);
                return null;
            }
            try (InputStream in = jar.getInputStream(entry);
                 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return PluginInfo.fromJson(reader);
            }
        }
    }

    /**
     * 通过反射实例化插件主类并校验其实现了 {@link AkiPlugin}。
     *
     * @param info   插件元数据（其中 {@code mainClass} 为全限定名）
     * @param loader 用于加载插件主类的类加载器
     * @return 已强转为 {@link AkiPlugin} 的插件实例
     * @throws ReflectiveOperationException 主类不存在/无无参构造/不是 {@link AkiPlugin} 时
     */
    private AkiPlugin instantiatePlugin(PluginInfo info, URLClassLoader loader) throws ReflectiveOperationException {
        Class<?> main = Class.forName(info.mainClass(), true, loader);
        // URLClassLoader 的父加载器是 Akia 自己，因此这里的 AkiPlugin 是共享的同一个接口，
        // instanceof / 强转都能跨加载器正确工作。
        if (!AkiPlugin.class.isAssignableFrom(main)) {
            throw new IllegalArgumentException(info.mainClass() + " does not implement AkiPlugin");
        }
        return (AkiPlugin) main.getDeclaredConstructor().newInstance();
    }

    /** 将插件声明的每个导出包注册进全局导出索引；同包已有其他插件导出时第一个生效，后者告警。 */
    private void registerExports(PluginInfo info, AkiPluginClassLoader loader) {
        for (String pkg : info.exports()) {
            if (pkg == null || pkg.isEmpty()) {
                continue;
            }
            AkiPluginClassLoader previous = exportIndex.putIfAbsent(pkg, loader);
            if (previous != null && previous != loader) {
                LOGGER.warn("API package '{}' already exported by plugin '{}', ignoring export from '{}'.",
                        pkg, previous.getPluginName(), info.name());
            } else {
                LOGGER.info("Plugin '{}' exports API package '{}'.", info.name(), pkg);
            }
        }
    }

    /** 插件卸载时移除其导出的包；仅当导出索引当前确实指向本插件时才移除，避免误删他人导出。 */
    private void unregisterExports(PluginInfo info, AkiPluginClassLoader loader) {
        for (String pkg : info.exports()) {
            exportIndex.remove(pkg, loader);
        }
    }

    /** 静默关闭类加载器（已为 null 或不支持关闭时忽略）。 */
    private static void closeQuietly(URLClassLoader loader) {
        if (loader == null) {
            return;
        }
        try {
            loader.close();
        } catch (IOException e) {
            LOGGER.warn("Failed to close class loader cleanly: {}", e.toString());
        }
    }

    @Override
    public java.util.List<PluginDiagnostic> getDiagnostics() {
        java.util.List<PluginDiagnostic> list = new ArrayList<>();
        for (String name : loadOrder) {  // 按依赖拓扑序输出，顺序稳定
            PluginEntry entry = plugins.get(name);
            if (entry != null) {
                list.add(buildDiagnostic(entry));
            }
        }
        return list;
    }

    @Override
    public PluginDiagnostic getDiagnostic(String pluginName) {
        if (pluginName == null) {
            return null;
        }
        PluginEntry entry = plugins.get(pluginName);
        return entry == null ? null : buildDiagnostic(entry);
    }

    /** 收集单个已加载插件的诊断快照（元数据、状态、服务、监听器数、任务数）。 */
    private PluginDiagnostic buildDiagnostic(PluginEntry entry) {
        PluginInfo info = entry.info();
        boolean enabled = enabledNames.contains(info.name());
        java.util.List<String> services = new ArrayList<>();
        for (RegisteredServiceProvider<?> p : serviceRegistry.getRegistrations(entry.plugin())) {
            services.add(p.getServiceClass().getName());
        }
        return new PluginDiagnostic(
                info.name(), info.version(), info.mainClass(), info.author(), info.description(),
                enabled,
                new ArrayList<>(info.depend()), new ArrayList<>(info.softDepend()), new ArrayList<>(info.loadBefore()),
                new ArrayList<>(info.exports()), services,
                eventBus.getListenerCount(entry.plugin()),
                scheduler.getTaskCount(entry.plugin()));
    }

    @Override
    public DumpReport getDumpReport() {
        java.util.List<ServiceEntry> services = new ArrayList<>();
        for (PluginEntry entry : plugins.values()) {
            for (RegisteredServiceProvider<?> p : serviceRegistry.getRegistrations(entry.plugin())) {
                services.add(new ServiceEntry(p.getServiceClass().getName(),
                        p.getProvider().getClass().getName(), p.getPlugin().getName(), p.getPriority().name()));
            }
        }
        Map<String, String> index = new LinkedHashMap<>();
        for (Map.Entry<String, AkiPluginClassLoader> e : exportIndex.entrySet()) {
            index.put(e.getKey(), e.getValue().getPluginName());
        }
        int totalEvents = 0;
        int totalTasks = 0;
        for (PluginEntry entry : plugins.values()) {
            totalEvents += eventBus.getListenerCount(entry.plugin());
            totalTasks += scheduler.getTaskCount(entry.plugin());
        }
        String stamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return new DumpReport(stamp, new ArrayList<>(loadOrder), getDiagnostics(),
                services, index, totalEvents, totalTasks);
    }

    @Override
    public Path getDumpDirectory() {
        if (pluginDir == null) {
            return null;
        }
        Path dir = pluginDir.getParent().resolve("dump");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("Failed to create dump directory {}: {}", dir, e.toString(), e);
        }
        return dir;
    }
}