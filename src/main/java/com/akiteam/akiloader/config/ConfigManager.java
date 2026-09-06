package com.akiteam.akiloader.config;

import com.akiteam.akiloader.api.AkiPlugin;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 所有插件配置文件的统一管理器。
 * <p>
 * 以插件名（来自 {@link AkiPlugin#getPluginInfo()} 的 {@code name()}）为键，
 * 为每个插件维护一个 {@link PluginConfig}。首次调用 {@link #getConfig(AkiPlugin)} 时创建；
 * 支持整体保存/重载全部配置，以及卸载插件时 {@link #removeConfig(String)} 释放引用，
 * 使旧插件实例可被 GC，避免内存泄漏。
 */
public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("AkiLoader.Config");

    /** 全局单例。 */
    public static final ConfigManager INSTANCE = new ConfigManager();

    private static final ConcurrentMap<String, PluginConfig> CONFIGS = new ConcurrentHashMap<>();

    private ConfigManager() {
    }

    /**
     * 获取（或按需创建）指定插件的配置对象。
     * <p>
     * 若插件还没有配置对象，会根据"默认配置存在则复制、否则置空"的规则新建一个。
     *
     * @param plugin 目标插件
     * @return 插件的配置对象（不会为 {@code null}）
     */
    public static PluginConfig getConfig(AkiPlugin plugin) {
        String name = plugin.getPluginInfo().name();
        return CONFIGS.computeIfAbsent(name, n -> new PluginConfig(plugin));
    }

    /**
     * 移除指定插件的配置对象并返回它。
     * <p>
     * 在卸载插件时调用，用于释放其持有的旧插件实例引用。若插件未配置，返回 {@code null}。
     *
     * @param pluginName 插件名
     * @return 被移除的配置对象，不存在时返回 {@code null}
     */
    public PluginConfig removeConfig(String pluginName) {
        PluginConfig removed = CONFIGS.remove(pluginName);
        if (removed != null) {
            LOGGER.debug("Removed config for plugin '{}'.", pluginName);
        }
        return removed;
    }

    /** 重新加载所有插件的配置（从磁盘读回）。 */
    public void reloadAllConfigs() {
        CONFIGS.values().forEach(PluginConfig::reload);
        LOGGER.info("AkiLoader: reloaded {} plugin config(s).", CONFIGS.size());
    }

    /** 保存所有插件的配置到磁盘。 */
    public void saveAllConfigs() {
        CONFIGS.values().forEach(PluginConfig::save);
        LOGGER.info("AkiLoader: saved {} plugin config(s).", CONFIGS.size());
    }
}