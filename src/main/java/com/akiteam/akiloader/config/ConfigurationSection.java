package com.akiteam.akiloader.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一个配置节（Configuration Section）：封装一张字符串键到值的映射，支持通过点号路径
 * （如 {@code "database.host"}）访问嵌套的值，并提供类型安全的取值方法。
 * <p>
 * 对同一底层 {@link Map} 的引用保持共享：{@link #getSection(String)} 返回的是包装同一
 * 嵌套映射的新节，因此对其做的修改能反映回父节（进而能被保存）。
 */
public class ConfigurationSection {

    /** 本节的底层数据映射。 */
    private final Map<String, Object> data;

    /**
     * 用给定映射构造一个配置节。
     *
     * @param data 底层数据映射；若为 {@code null} 则视为空映射
     */
    public ConfigurationSection(Map<String, Object> data) {
        this.data = data != null ? data : new LinkedHashMap<>();
    }

    /**
     * 返回原始值（不做类型转换）。
     *
     * @param path 点号路径
     * @return 对应值；路径不存在时返回 {@code null}
     */
    public Object get(String path) {
        Object current = data;
        for (String key : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(key);
        }
        return current;
    }

    /**
     * 设置路径对应的值。
     * <p>
     * 若 {@code value} 为 {@code null}，则从配置中移除该路径。中间缺失的级层会自动创建。
     *
     * @param path  点号路径
     * @param value 新值；{@code null} 表示删除
     */
    public void set(String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.get(keys[i]);
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<String, Object>();
                current.put(keys[i], next);
            }
            current = asMap(next);
        }
        String last = keys[keys.length - 1];
        if (value == null) {
            current.remove(last);
        } else {
            current.put(last, value);
        }
    }

    /**
     * 返回路径对应的配置节。
     * <p>
     * 若该路径下还没有映射，会先在父映射中创建空映射，使返回的节与父节共享底层数据。
     *
     * @param path 点号路径
     * @return 包装共享数据的配置节（不会为 {@code null}）
     */
    public ConfigurationSection getSection(String path) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = data;
        for (String key : keys) {
            Object next = current.get(key);
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<String, Object>();
                current.put(key, next);
            }
            current = asMap(next);
        }
        return new ConfigurationSection(current);
    }

    /**
     * 判断配置中是否包含某路径（存在的值不能为 {@code null}）。
     *
     * @param path 点号路径
     * @return 存在且非 {@code null} 时为 {@code true}
     */
    public boolean contains(String path) {
        return get(path) != null;
    }

    /**
     * 返回本节的所有键。
     *
     * @param deep 为 {@code true} 时返回包括嵌套层级的完整点号路径
     * @return 键的集合
     */
    public Set<String> getKeys(boolean deep) {
        Set<String> keys = new LinkedHashSet<>();
        collectKeys(data, "", deep, keys);
        return keys;
    }

    private void collectKeys(Map<String, Object> map, String prefix, boolean deep, Set<String> out) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String full = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            out.add(full);
            if (deep && e.getValue() instanceof Map) {
                collectKeys(asMap(e.getValue()), full, true, out);
            }
        }
    }

    /**
     * 返回本节的副本，值为普通对象（嵌套 {@link Map}/{@link ConfigurationSection} 会被展开）。
     *
     * @param deep 为 {@code true} 时展开嵌套结构
     * @return 键值映射副本
     */
    public Map<String, Object> getValues(boolean deep) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (deep) {
                result.put(e.getKey(), unwrap(e.getValue()));
            } else {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * 递归把整个节与嵌套结构展开为普通 {@link Map}，用于 YAML 序列化。
     *
     * @return 纯映射副本
     */
    Map<String, Object> toPlainMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            result.put(e.getKey(), unwrap(e.getValue()));
        }
        return result;
    }

    private Object unwrap(Object value) {
        if (value instanceof ConfigurationSection) {
            return ((ConfigurationSection) value).toPlainMap();
        }
        if (value instanceof Map) {
            Map<String, Object> plain = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                plain.put(String.valueOf(e.getKey()), unwrap(e.getValue()));
            }
            return plain;
        }
        if (value instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) value) {
                out.add(unwrap(item));
            }
            return out;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    // -------------------- 类型安全的获取方法 --------------------

    /** 取字符串；不存在返回 {@code null}。 */
    public String getString(String path) {
        return asString(get(path));
    }

    /** 取字符串；不存在或类型不符返回默认值。 */
    public String getString(String path, String def) {
        Object v = get(path);
        return v == null ? def : String.valueOf(v);
    }

    /** 取整数；不存在或无法解析返回默认值。 */
    public int getInt(String path, int def) {
        Object v = get(path);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            try {
                return Integer.parseInt((String) v);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    /** 取整数；不存在返回 0。 */
    public int getInt(String path) {
        return getInt(path, 0);
    }

    /** 取布尔值；不存在或类型不符返回默认值。 */
    public boolean getBoolean(String path, boolean def) {
        Object v = get(path);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof String) {
            return Boolean.parseBoolean((String) v);
        }
        return def;
    }

    /** 取布尔值；不存在返回 {@code false}。 */
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    /** 取双精度；不存在或无法解析返回默认值。 */
    public double getDouble(String path, double def) {
        Object v = get(path);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    /** 取双精度；不存在返回 0.0。 */
    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    /** 取任意列表；不存在或类型不符返回 {@code null}。 */
    public List<?> getList(String path) {
        Object v = get(path);
        return v instanceof List ? (List<?>) v : null;
    }

    /** 取字符串列表；列表中的每个元素都转为字符串，不存在返回空列表。 */
    public List<String> getStringList(String path) {
        Object v = get(path);
        if (!(v instanceof List)) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        for (Object item : (List<?>) v) {
            out.add(item == null ? null : String.valueOf(item));
        }
        return out;
    }

    /**
     * 取映射列表（常用于“列表项是字典”的配置）；仅保留其中为 {@link Map} 的条目。
     *
     * @return 普通映射列表；不存在或类型不符返回空列表
     */
    public List<Map<?, ?>> getMapList(String path) {
        Object v = get(path);
        if (!(v instanceof List)) {
            return new ArrayList<>();
        }
        List<Map<?, ?>> out = new ArrayList<>();
        for (Object item : (List<?>) v) {
            if (item instanceof Map) {
                out.add((Map<?, ?>) item);
            }
        }
        return out;
    }

    private static String asString(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof String) {
            return (String) v;
        }
        return String.valueOf(v);
    }
}