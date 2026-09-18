package com.akiteam.akia.api;

import java.io.File;
import java.util.List;

/**
 * 插件管理器接口：负责插件的加载、启用、禁用与查询。
 * <p>
 * 此处<b>只定义接口契约，不包含任何实现逻辑</b>。
 * 具体实现在 {@code com.akiteam.akia.loader} 包中完成（见后续步骤）。
 */
public interface PluginManager {

    /**
     * 加载一个插件 JAR 文件。
     * <p>
     * 加载成功会调用该插件主类的 {@link AkiPlugin#onLoad()}，但不代表已启用。
     *
     * @param jarFile 插件 JAR 文件
     * @return 加载成功返回 {@code true}，失败返回 {@code false}
     */
    boolean loadPlugin(File jarFile);

    /**
     * 启用指定名称的插件，会调用其 {@link AkiPlugin#onEnable()}。
     *
     * @param name 插件名称（见 {@link PluginInfo#name()}）
     * @return 成功启用返回 {@code true}；插件不存在或已启用时返回 {@code false}
     */
    boolean enablePlugin(String name);

    /**
     * 禁用指定名称的插件，会调用其 {@link AkiPlugin#onDisable()}。
     *
     * @param name 插件名称
     * @return 成功禁用返回 {@code true}；插件不存在或已禁用时返回 {@code false}
     */
    boolean disablePlugin(String name);

    /**
     * 根据名称获取已注入的插件实例。
     *
     * @param name 插件名称
     * @return 对应的 {@link AkiPlugin} 实例；若未加载则返回 {@code null}
     */
    AkiPlugin getPlugin(String name);

    /**
     * 获取所有已加载插件的元数据列表。
     *
     * @return 不可变或安全的元数据快照列表（即使之后加载了新插件也不受影响）
     */
    List<PluginInfo> getLoadedPlugins();

    /**
     * 热重载全部插件：禁用所有已启用插件、卸载全部插件（清理事件/调度任务/命令/
     * ClassLoader），然后重新扫描插件目录并加载、启用新插件。
     * <p>
     * 全程不重启游戏，但属于供开发调试使用的辅助能力；某插件失败只影响其自身。
     *
     * @return 长度为 2 的数组：{@code [启用成功数, 启用失败数]}
     */
    int[] reloadPlugins();

    /**
     * 按依赖拓扑序（依赖者靠后）启用所有已加载且未启用的插件。
     * <p>
     * 必须按拓扑序启用，才能保证"服务提供者先于消费者完成 {@code onEnable}"，
     * 消费者才能取到提供者注册的服务。相比逐个 {@link #enablePlugin(String)}，
     * 该方法能确保正确的依赖序（否则遍历哈希表顺序无法保证）。
     *
     * @return 长度为 2 的数组：{@code [启用成功数, 启用失败数]}
     */
    int[] enableAllPlugins();

    /**
     * 按依赖拓扑序的逆序（依赖者靠前）禁用所有已启用的插件。
     * <p>
     * 逆序禁用保证依赖者先释放资源，再卸载其依赖，避免消费者禁用时仍依赖提供者。
     *
     * @return 长度为 2 的数组：{@code [禁用成功数, 禁用失败数]}
     */
    int[] disableAllPlugins();

    /**
     * 获取所有已加载插件的诊断快照（按依赖拓扑序排列）。
     * 每个快照含元数据、启用状态、依赖/导出、注册的服务、监听器数与任务数。
     *
     * @return 诊断快照列表（可能为空，不为 {@code null}）
     */
    java.util.List<com.akiteam.akia.diag.PluginDiagnostic> getDiagnostics();

    /**
     * 获取单个插件的诊断快照。
     *
     * @param pluginName 插件名
     * @return 对应插件（且已加载）的诊断快照；插件不存在时返回 {@code null}
     */
    com.akiteam.akia.diag.PluginDiagnostic getDiagnostic(String pluginName);

    /**
     * 生成完整的诊断报告（插件列表、依赖拓扑序、服务注册表、导出索引、监听器/任务统计）。
     *
     * @return 诊断报告
     */
    com.akiteam.akia.diag.DumpReport getDumpReport();

    /**
     * 返回诊断导出目录（{@code <插件目录>/dump}），不存在则创建。
     *
     * @return 导出目录；尚未初始化插件目录时返回 {@code null}
     */
    java.nio.file.Path getDumpDirectory();
}