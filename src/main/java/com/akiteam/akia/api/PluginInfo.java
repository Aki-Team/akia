package com.akiteam.akia.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 插件的元数据，是不可变的数据类（Java record）。
 * <p>
 * {@code name}、{@code version}、{@code mainClass} 为必填；
 * {@code author} 与 {@code description} 可选，缺省时在构造阶段自动填充默认值。
 * <p>
 * <b>依赖字段</b>（参考 Paper {@code PluginDescriptionFile}）：
 * <ul>
 *     <li>{@code depend}：强依赖，插件需要用到它们的 API；缺失时本插件<b>不会被加载</b>；</li>
 *     <li>{@code softDepend}：软依赖，缺失仅跳过，不影响本插件加载；</li>
 *     <li>{@code loadBefore}：请求在本插件之前加载某些插件，只影响加载顺序。</li>
 * </ul>
 * 三者都参与依赖图构建，由 {@code PluginManager} 通过拓扑排序决定加载顺序。
 */
public record PluginInfo(
        String name,
        String version,
        String mainClass,
        String author,
        String description,
        List<String> depend,
        List<String> softDepend,
        List<String> loadBefore,
        List<String> exports,
        String apiVersion
) {

    /** 作者未填写时的默认值。 */
    public static final String DEFAULT_AUTHOR = "Unknown";
    /** 描述未填写时的默认值。 */
    public static final String DEFAULT_DESCRIPTION = "";
    /** api-version 未填写时的默认值。 */
    public static final String DEFAULT_API_VERSION = "1.0";

    /**
     * 正向构造函数：校验必填字段，并把可空字段归一化为默认值；
     * 依赖列表缺省时被归一化为不可变的空列表（{@link List#of()}）。
     *
     * @throws NullPointerException 当 {@code name}、{@code version} 或 {@code mainClass} 为 {@code null} 时
     */
    public PluginInfo {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(mainClass, "mainClass");
        author = (author == null) ? DEFAULT_AUTHOR : author;
        description = (description == null) ? DEFAULT_DESCRIPTION : description;
        depend = (depend == null) ? List.of() : List.copyOf(depend);
        softDepend = (softDepend == null) ? List.of() : List.copyOf(softDepend);
        loadBefore = (loadBefore == null) ? List.of() : List.copyOf(loadBefore);
        exports = (exports == null) ? List.of() : List.copyOf(exports);
    }

    /**
     * 便捷构造函数，与旧版（无依赖字段）签名保持一致，供不依赖其他插件的插件使用。
     *
     * @throws NullPointerException 当 {@code name}、{@code version} 或 {@code mainClass} 为 {@code null} 时
     */
    public PluginInfo(String name, String version, String mainClass, String author, String description) {
        this(name, version, mainClass, author, description, List.of(), List.of(), List.of(), List.of(), DEFAULT_API_VERSION);
    }

    /**
     * 从 JSON 读取器解析一个插件元数据实例（对应 {@code plugin.json}）。
     * <p>
     * 解析规则：除了必填的 {@code name}/{@code version}/{@code mainClass}，
     * 可选的 {@code author}、{@code description}、{@code depend}、{@code softDepend}、
     * {@code loadBefore}、{@code api-version} 字段缺失时将被填充为默认值。
     *
     * @param reader 打开 {@code plugin.json} 的字符读取器，不要为 {@code null}
     * @return 解析得到的 {@link PluginInfo}
     * @throws com.google.gson.JsonSyntaxException 当 JSON 格式非法或必填字段缺失时
     */
    public static PluginInfo fromJson(Reader reader) {
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        String name = json.get("name").getAsString();
        String version = json.get("version").getAsString();
        String mainClass = json.get("mainClass").getAsString();
        String author = json.has("author") ? json.get("author").getAsString() : null;
        String description = json.has("description") ? json.get("description").getAsString() : null;
        List<String> depend = parseStringList(json, "depend");
        List<String> softDepend = parseStringList(json, "softDepend");
        List<String> loadBefore = parseStringList(json, "loadBefore");
        List<String> exports = parseStringList(json, "exports");
        String apiVersion = json.has("api-version") ? json.get("api-version").getAsString() : null;
        return new PluginInfo(name, version, mainClass, author, description, depend, softDepend, loadBefore, exports, apiVersion);
    }

    /**
     * 解析一个 JSON 数组为插件名列表；空白被替换为下划线，与 Paper 行为一致。
     * 字段缺失时返回不可变的空列表。
     */
    private static List<String> parseStringList(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }
        JsonArray arr = json.get(key).getAsJsonArray();
        List<String> out = new ArrayList<>(arr.size());
        for (JsonElement element : arr) {
            out.add(element.getAsString().replace(' ', '_'));
        }
        return out;
    }
}