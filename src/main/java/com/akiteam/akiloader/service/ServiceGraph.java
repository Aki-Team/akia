package com.akiteam.akiloader.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 服务依赖图快照：某一时刻整个服务生态的结构视图。
 * <p>
 * AkiLoader 的服务本身不声明对其它服务的依赖关系，因此这里的“图”是一个扁平的
 * 服务清单视图——按服务接口类型分组列出所有提供者（含插件、优先级、版本）。
 * 主要由诊断 / 调试命令（如 {@code /akiloader dump}）使用，用于导出可读的服务注册概览。
 * <p>
 * 线程安全：构造完成即为不可变快照。
 */
public final class ServiceGraph {

    private final List<ServiceNode> nodes;

    private ServiceGraph(List<ServiceNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * 由 {@link ServiceRegistry} 内部构建一个不可变服务图快照。
     *
     * @param providersByType 服务类型 → 提供者记录列表
     * @return 服务图快照
     */
    public static ServiceGraph snapshot(
            Map<? extends Class<?>, ? extends List<? extends RegisteredServiceProvider<?>>> providersByType) {
        List<ServiceNode> nodes = new ArrayList<>();
        for (Map.Entry<? extends Class<?>, ? extends List<? extends RegisteredServiceProvider<?>>> e
                : providersByType.entrySet()) {
            Class<?> type = e.getKey();
            List<ServiceNode.ProviderRef> refs = new ArrayList<>();
            for (RegisteredServiceProvider<?> p : e.getValue()) {
                refs.add(new ServiceNode.ProviderRef(
                        p.getPlugin().getName(),
                        String.valueOf(p.getPriority()),
                        p.getVersion()));
            }
            nodes.add(new ServiceNode(type, refs));
        }
        nodes.sort((a, b) -> a.serviceType().getName().compareTo(b.serviceType().getName()));
        return new ServiceGraph(nodes);
    }

    /** 返回所有服务节点（按类型名排序）。 */
    public List<ServiceNode> nodes() {
        return nodes;
    }

    /** 返回所有服务接口类型的集合。 */
    public Set<Class<?>> serviceTypes() {
        return nodes.stream().map(ServiceNode::serviceType).collect(Collectors.toUnmodifiableSet());
    }

    /** 转换为便于序列化 / 展示的 Map 结构（类型名 → 提供者摘要列表）。 */
    public Map<String, List<Map<String, String>>> toMap() {
        Map<String, List<Map<String, String>>> out = new LinkedHashMap<>();
        for (ServiceNode node : nodes) {
            List<Map<String, String>> refs = new ArrayList<>();
            for (ServiceNode.ProviderRef r : node.providers()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("plugin", r.plugin());
                m.put("priority", r.priority());
                m.put("version", r.version());
                refs.add(m);
            }
            out.put(node.serviceType().getName(), refs);
        }
        return out;
    }

    @Override
    public String toString() {
        return "ServiceGraph" + toMap();
    }

    /** 一个服务类型的图节点。 */
    public record ServiceNode(Class<?> serviceType, List<ProviderRef> providers) {

        /** 一个提供者的引用摘要。 */
        public record ProviderRef(String plugin, String priority, String version) {
        }
    }
}