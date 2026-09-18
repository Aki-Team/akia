package com.akiteam.akia.diag;

import java.util.List;

/**
 * 单个插件的诊断快照（用 Gson 序列化为 JSON，无需额外依赖）。
 * <p>
 * 由 {@code PluginManager.getDiagnostics()} / {@code getDiagnostic(String)} 收集，
 * 供 {@code /akia list}、{@code /akia info} 与 {@code /akia dump} 使用。
 */
public record PluginDiagnostic(
        String name,
        String version,
        String mainClass,
        String author,
        String description,
        boolean enabled,
        List<String> depend,
        List<String> softDepend,
        List<String> loadBefore,
        List<String> exports,
        List<String> services,
        int eventListenerCount,
        int taskCount
) {
}