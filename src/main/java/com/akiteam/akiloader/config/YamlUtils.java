package com.akiteam.akiloader.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * SnakeYAML 的薄封装：负责把 YAML 字符串解析为 {@link Map}、并把配置数据序列化回 YAML。
 * <p>
 * SnakeYAML 的单个 {@link Yaml} 实例在多线程下可安全复用（加载与转储均为线程安全），
 * 因此此处使用一个共享实例。
 */
final class YamlUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger("AkiLoader.Config");

    /** 共享的 SnakeYAML 实例（线程安全）。 */
    private static final Yaml YAML = new Yaml();

    private YamlUtils() {
    }

    /**
     * 把 YAML 字符串解析为一个普通对象。
     * <p>
     * 标准 YAML 顶层为映射时返回 {@code Map<String, Object>}；解析失败时返回 {@code null}。
     *
     * @param content YAML 内容，可为空字符串
     * @return 解析结果；内容为空或非法时返回 {@code null}
     */
    static Object load(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            return YAML.load(content);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse YAML: {}", e.toString(), e);
            return null;
        }
    }

    /**
     * 把配置数据（应为普通 {@link Map}）序列化为 YAML 文本。
     *
     * @param data 配置映射
     * @return YAML 文本（不会为 {@code null}）
     */
    static String dump(Map<String, Object> data) {
        return YAML.dump(data != null ? data : new LinkedHashMap<>());
    }
}