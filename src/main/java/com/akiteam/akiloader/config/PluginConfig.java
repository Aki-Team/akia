package com.akiteam.akiloader.config;

import com.akiteam.akiloader.api.AkiPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单个插件的配置文件对象，对应磁盘上的 {@code run/akiloader/<插件名>/config.yml}。
 * <p>
 * 提供 {@link #reload()} 重新从磁盘加载（文件不存在时，从插件 JAR 根目录复制默认
 * {@code config.yml} 作为初始内容）、{@link #save()} 保存，以及类型安全的取值/设值方法。
 * 内部数据由一个 {@link ConfigurationSection} 承载，也随 {@link ConfigManager} 统一管理。
 */
public class PluginConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("AkiLoader.Config");

    /** 配置文件在插件目录中的文件名。 */
    public static final String CONFIG_FILE_NAME = "config.yml";

    /** 所属插件。 */
    private final AkiPlugin plugin;

    /** 配置文件路径。 */
    private final Path file;

    /** 承载当前配置数据的配置节。 */
    private ConfigurationSection section;

    /**
     * 为给定插件构造配置对象（会自动从磁盘加载；文件不存在则从插件 JAR 复制默认配置）。
     *
     * @param plugin 目标插件
     */
    public PluginConfig(AkiPlugin plugin) {
        this.plugin = plugin;
        this.file = configDir().resolve(CONFIG_FILE_NAME);
        reload();
    }

    /** 当前配置目录：{@code run/akiloader/<插件名>/}。 */
    private Path configDir() {
        return FMLPaths.GAMEDIR.get().resolve("akiloader").resolve(plugin.getPluginInfo().name());
    }

    /**
     * 重新从磁盘加载配置。
     * <p>
     * 文件存在则解析之；解析失败时回退为空配置（不崩溃）。文件不存在时从插件 JAR 根目录
     * 的默认 {@code config.yml} 读取初始化内容（JAR 中没有则生成空配置）。
     */
    public void reload() {
        try {
            Files.createDirectories(file.getParent());
            Map<String, Object> data = null;
            if (Files.exists(file)) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                Object loaded = YamlUtils.load(content);
                if (loaded instanceof Map) {
                    data = toMap(loaded);
                }
            }
            if (data == null) {
                data = loadDefaultsFromJar();
            }
            this.section = new ConfigurationSection(data);
        } catch (IOException e) {
            LOGGER.error("Failed to read config for plugin '{}': {}", plugin.getName(), e.toString(), e);
            this.section = new ConfigurationSection(new LinkedHashMap<>());
        }
    }

    /** 从插件 JAR 根目录读取默认 {@code config.yml}；JAR 中没有则返回空映射。 */
    private Map<String, Object> loadDefaultsFromJar() {
        @SuppressWarnings("resource")
        ClassLoader loader = plugin.getClass().getClassLoader();
        try (InputStream in = loader.getResourceAsStream(CONFIG_FILE_NAME)) {
            if (in != null) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                Object loaded = YamlUtils.load(content);
                if (loaded instanceof Map) {
                    return toMap(loaded);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read default config from JAR for plugin '{}': {}", plugin.getName(), e.toString());
        }
        return new LinkedHashMap<>();
    }

    /**
     * 保存当前配置到磁盘。
     * <p>
     * 目录不存在时会先创建。即使配置为空也会生成一个空的 {@code config.yml}。
     */
    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, YamlUtils.dump(section.toPlainMap()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save config for plugin '{}': {}", plugin.getName(), e.toString(), e);
        }
    }

    /** 配置文件路径。 */
    public Path getFile() {
        return file;
    }

    /** 返回内部配置节（供高级用法）。 */
    public ConfigurationSection getSection() {
        return section;
    }

    // -------------------- 委托给 ConfigurationSection --------------------

    public Object get(String path) {
        return section.get(path);
    }

    public void set(String path, Object value) {
        section.set(path, value);
    }

    public String getString(String path) {
        return section.getString(path);
    }

    public String getString(String path, String def) {
        return section.getString(path, def);
    }

    public int getInt(String path) {
        return section.getInt(path);
    }

    public int getInt(String path, int def) {
        return section.getInt(path, def);
    }

    public boolean getBoolean(String path) {
        return section.getBoolean(path);
    }

    public boolean getBoolean(String path, boolean def) {
        return section.getBoolean(path, def);
    }

    public double getDouble(String path) {
        return section.getDouble(path);
    }

    public double getDouble(String path, double def) {
        return section.getDouble(path, def);
    }

    public List<?> getList(String path) {
        return section.getList(path);
    }

    public List<String> getStringList(String path) {
        return section.getStringList(path);
    }

    public List<Map<?, ?>> getMapList(String path) {
        return section.getMapList(path);
    }

    public ConfigurationSection getConfigurationSection(String path) {
        return section.getSection(path);
    }

    public boolean contains(String path) {
        return section.contains(path);
    }

    public Set<String> getKeys(boolean deep) {
        return section.getKeys(deep);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object value) {
        return (Map<String, Object>) value;
    }
}