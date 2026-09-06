package com.akiteam.akiloader.diag;

import java.util.List;
import java.util.Map;

/**
 * {@code /akiloader dump} 导出的完整诊断报告（用 Gson 序列化为 JSON 文件）。
 *
 * @param timestamp          生成时间戳（yyyyMMdd-HHmmss），用于文件名唯一化
 * @param loadOrder          依赖拓扑序（提供者靠前，依赖者靠后）
 * @param plugins            所有已加载插件的诊断快照
 * @param services           全部已注册服务提供者的扁平化列表
 * @param exportIndex        API 导出索引：包名 → 导出它的插件名
 * @param totalEventListeners 所有插件的事件监听器方法总数
 * @param totalTasks          所有插件的调度任务总数
 */
public record DumpReport(
        String timestamp,
        List<String> loadOrder,
        List<PluginDiagnostic> plugins,
        List<ServiceEntry> services,
        Map<String, String> exportIndex,
        int totalEventListeners,
        int totalTasks
) {
}