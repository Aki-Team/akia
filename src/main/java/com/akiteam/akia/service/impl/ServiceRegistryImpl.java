package com.akiteam.akia.service.impl;

import com.akiteam.akia.Akia;
import com.akiteam.akia.api.AkiPlugin;
import com.akiteam.akia.event.EventBus;
import com.akiteam.akia.service.RegisteredServiceProvider;
import com.akiteam.akia.service.ServiceGraph;
import com.akiteam.akia.service.ServicePriority;
import com.akiteam.akia.service.ServiceQuery;
import com.akiteam.akia.service.ServiceRegisteredEvent;
import com.akiteam.akia.service.ServiceRegistry;
import com.akiteam.akia.service.ServiceUnregisteredEvent;
import com.akiteam.akia.service.ServiceWatch;
import com.akiteam.akia.service.ServiceWatcher;
import com.akiteam.akia.service.VersionConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link ServiceRegistry} 的默认实现（参考 Paper 的 {@code SimpleServicesManager}）。
 * <p>
 * 内部用 {@code ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<...>>} 存储，
 * 注册时按"优先级高者靠前，同优先级按插入顺序"有序插入；获取时直接返回列表首个。
 * 所有操作线程安全，适合多线程并发注册 / 获取。
 * <p>
 * 服务注册 / 注销时会通过注入的 {@link EventBus} 分发
 * {@link ServiceRegisteredEvent} / {@link ServiceUnregisteredEvent}，让插件能够感知服务上线与下线。
 * 此外还提供：按优先级 / 插件 / 版本组合筛选的 {@link ServiceQuery} 条件查询、按单个服务类型
 * 订阅的 {@link ServiceWatcher}（热插拔通知）、以及 {@link ServiceGraph} 服务快照。
 * <p>
 * <b>watch 清理</b>：{@link #unregisterAll(AkiPlugin)} 在注销某插件全部服务的同时，也会取消
 * 该插件注册的所有 watch，避免监听器生命周期泄漏。
 */
public final class ServiceRegistryImpl implements ServiceRegistry {

    private static final org.slf4j.Logger LOGGER = Akia.LOGGER;

    private final Map<Class<?>, CopyOnWriteArrayList<RegisteredServiceProvider<?>>> services =
            new ConcurrentHashMap<>();

    /** 服务类型 → 订阅该类型的监听器列表。 */
    private final Map<Class<?>, CopyOnWriteArrayList<WatchEntry>> watchers = new ConcurrentHashMap<>();

    /** 服务生命周期事件分发到的事件总线（与插件共享）。 */
    private final EventBus eventBus;

    /** 全局同步互斥对象：保证同一类型下的“检查旧主导者 + 插入”是原子的。 */
    private final Object mutex = new Object();

    /**
     * 构造服务注册表。
     *
     * @param eventBus 用于分发服务生命周期事件的共享事件总线（不为 {@code null}）
     */
    public ServiceRegistryImpl(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public <T> void register(Class<T> serviceClass, T provider, AkiPlugin plugin) {
        register(serviceClass, provider, plugin, ServicePriority.Normal);
    }

    @Override
    public <T> void register(Class<T> serviceClass, T provider, AkiPlugin plugin, ServicePriority priority) {
        register(serviceClass, provider, plugin, priority, RegisteredServiceProvider.DEFAULT_VERSION);
    }

    @Override
    public <T> void register(Class<T> serviceClass, T provider, AkiPlugin plugin, ServicePriority priority, String version) {
        if (serviceClass == null || provider == null || plugin == null) {
            LOGGER.warn("Rejected invalid service registration (class={}, plugin={}).",
                    serviceClass, plugin == null ? null : plugin.getName());
            return;
        }
        ServicePriority prio = priority == null ? ServicePriority.Normal : priority;
        if (!serviceClass.isInstance(provider)) {
            LOGGER.warn("Provider '{}' is not an instance of service interface '{}'; rejected.",
                    provider.getClass().getName(), serviceClass.getName());
            return;
        }
        RegisteredServiceProvider<T> entry =
                new RegisteredServiceProvider<>(serviceClass, provider, plugin, prio, version);

        RegisteredServiceProvider<?> oldTop;
        boolean becameTop;
        synchronized (mutex) {
            CopyOnWriteArrayList<RegisteredServiceProvider<?>> l = services.computeIfAbsent(
                    serviceClass, k -> new CopyOnWriteArrayList<>());
            oldTop = l.isEmpty() ? null : l.get(0);
            int index = insertIndex(l, prio);
            l.add(index, entry);
            becameTop = index == 0 && oldTop != entry;
        }

        eventBus.fireEvent(new ServiceRegisteredEvent(serviceClass, entry));
        LOGGER.info("Service '{}' registered by plugin '{}' (priority={}, version={}).",
                serviceClass.getName(), plugin.getName(), prio, entry.getVersion());
        notifyWatchersRegistered(serviceClass, entry, oldTop, becameTop);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceClass) {
        RegisteredServiceProvider<?> first = top(serviceClass);
        return first == null ? null : (T) first.getProvider();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<RegisteredServiceProvider<T>> getRegistrations(Class<T> serviceClass) {
        List<RegisteredServiceProvider<?>> list = services.get(serviceClass);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>((List<RegisteredServiceProvider<T>>) (List<?>) list);
    }

    @Override
    public List<RegisteredServiceProvider<?>> getRegistrations(AkiPlugin plugin) {
        if (plugin == null) {
            return Collections.emptyList();
        }
        List<RegisteredServiceProvider<?>> result = new ArrayList<>();
        for (List<RegisteredServiceProvider<?>> list : services.values()) {
            for (RegisteredServiceProvider<?> p : list) {
                if (p.getPlugin() == plugin) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<RegisteredServiceProvider<T>> query(ServiceQuery query) {
        if (query == null || query.serviceClass() == null) {
            return Collections.emptyList();
        }
        CopyOnWriteArrayList<RegisteredServiceProvider<?>> all = services.get(query.serviceClass());
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }
        List<RegisteredServiceProvider<?>> matched = new ArrayList<>();
        for (RegisteredServiceProvider<?> p : all) {
            if (matches(query, p)) {
                matched.add(p);
            }
        }
        if (query.latestOnly() && !matched.isEmpty()) {
            matched = new ArrayList<>(List.of(latestOf(matched)));
        }
        // 按优先级（高→低）排序，同优先级按注册顺序
        matched.sort((a, b) -> {
            int c = b.getPriority().compareTo(a.getPriority());
            if (c != 0) {
                return c;
            }
            return Integer.compare(indexOf(all, a), indexOf(all, b));
        });
        return (List<RegisteredServiceProvider<T>>) (List<?>) matched;
    }

    @Override
    public <T> T get(Class<T> serviceClass, String constraint) {
        List<RegisteredServiceProvider<T>> list =
                query(ServiceQuery.forClass(serviceClass).version(constraint));
        return list.isEmpty() ? null : list.get(0).getProvider();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getLatest(Class<T> serviceClass) {
        List<RegisteredServiceProvider<?>> list = services.get(serviceClass);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return (T) latestOf(new ArrayList<>(list)).getProvider();
    }

    @Override
    public <T> T get(Class<T> serviceClass, ServicePriority priority) {
        List<RegisteredServiceProvider<T>> list =
                query(ServiceQuery.forClass(serviceClass).priority(priority));
        return list.isEmpty() ? null : list.get(0).getProvider();
    }

    @Override
    public <T> ServiceWatch watch(Class<T> serviceClass, AkiPlugin plugin, ServiceWatcher<T> watcher) {
        if (serviceClass == null || plugin == null || watcher == null) {
            LOGGER.warn("Rejected invalid service watch (class={}, plugin={}).",
                    serviceClass, plugin == null ? null : plugin.getName());
            return InactiveWatch.INSTANCE;
        }
        WatchEntry entry = new WatchEntry(plugin, serviceClass, watcher);
        watchers.computeIfAbsent(serviceClass, k -> new CopyOnWriteArrayList<>()).add(entry);
        return entry;
    }

    @Override
    public ServiceGraph getServiceGraph() {
        return ServiceGraph.snapshot(services);
    }

    @Override
    public void unregisterAll(AkiPlugin plugin) {
        if (plugin == null) {
            return;
        }
        // 收集该插件注册的全部提供者，按服务类型分组，以便注销后计算新主导者
        Map<Class<?>, List<RegisteredServiceProvider<?>>> removedByType = new LinkedHashMap<>();
        for (Map.Entry<Class<?>, CopyOnWriteArrayList<RegisteredServiceProvider<?>>> e : services.entrySet()) {
            Class<?> type = e.getKey();
            CopyOnWriteArrayList<RegisteredServiceProvider<?>> list = e.getValue();
            for (RegisteredServiceProvider<?> p : list) {
                if (p.getPlugin() == plugin) {
                    removedByType.computeIfAbsent(type, k -> new ArrayList<>()).add(p);
                }
            }
        }
        // 结构收敛：逐个移除，并记住每类“被移除时是否占据首位”。
        Map<Class<?>, Set<RegisteredServiceProvider<?>>> wasTop = new LinkedHashMap<>();
        for (Map.Entry<Class<?>, List<RegisteredServiceProvider<?>>> e : removedByType.entrySet()) {
            Class<?> type = e.getKey();
            CopyOnWriteArrayList<RegisteredServiceProvider<?>> list = services.get(type);
            if (list == null) {
                continue;
            }
            for (RegisteredServiceProvider<?> p : e.getValue()) {
                if (!list.isEmpty() && list.get(0) == p) {
                    wasTop.computeIfAbsent(type, k -> new HashSet<>()).add(p);
                }
                list.remove(p);
            }
            if (list.isEmpty()) {
                services.remove(type, list);
            }
        }

        // 事件：先分发注销事件（此时该插件自己的 @EventHandler 仍在），再通知 watcher
        for (Map.Entry<Class<?>, List<RegisteredServiceProvider<?>>> e : removedByType.entrySet()) {
            for (RegisteredServiceProvider<?> p : e.getValue()) {
                eventBus.fireEvent(new ServiceUnregisteredEvent(p.getServiceClass(), p));
            }
        }
        for (Map.Entry<Class<?>, List<RegisteredServiceProvider<?>>> e : removedByType.entrySet()) {
            Class<?> type = e.getKey();
            RegisteredServiceProvider<?> newTop = top(type);
            for (RegisteredServiceProvider<?> p : e.getValue()) {
                boolean replacedTop = wasTop.getOrDefault(type, Collections.emptySet()).contains(p);
                notifyWatchersUnregistered(type, p, newTop, replacedTop);
            }
        }

        // 清理该插件注册的所有 watch（卸载时的自动清理）
        cancelWatchersOf(plugin);

        int total = removedByType.values().stream().mapToInt(List::size).sum();
        if (total > 0) {
            LOGGER.info("Unregistered {} service(s) for plugin '{}'.", total, plugin.getName());
        }
    }

    @Override
    public Set<Class<?>> getRegisteredServiceTypes() {
        return new HashSet<>(services.keySet());
    }

    @Override
    public boolean isRegistered(Class<?> serviceClass) {
        if (serviceClass == null) {
            return false;
        }
        List<RegisteredServiceProvider<?>> list = services.get(serviceClass);
        return list != null && !list.isEmpty();
    }

    @Override
    public Map<Class<?>, List<RegisteredServiceProvider<?>>> getServicesByPlugin(AkiPlugin plugin) {
        Map<Class<?>, List<RegisteredServiceProvider<?>>> result = new LinkedHashMap<>();
        if (plugin == null) {
            return result;
        }
        for (Map.Entry<Class<?>, CopyOnWriteArrayList<RegisteredServiceProvider<?>>> e : services.entrySet()) {
            for (RegisteredServiceProvider<?> p : e.getValue()) {
                if (p.getPlugin() == plugin) {
                    result.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(p);
                }
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- internal

    private RegisteredServiceProvider<?> top(Class<?> serviceClass) {
        List<RegisteredServiceProvider<?>> list = services.get(serviceClass);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private int insertIndex(List<RegisteredServiceProvider<?>> list, ServicePriority priority) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getPriority().compareTo(priority) < 0) {
                return i;
            }
        }
        return list.size();
    }

    private boolean matches(ServiceQuery query, RegisteredServiceProvider<?> p) {
        if (query.priority() != null && p.getPriority() != query.priority()) {
            return false;
        }
        if (query.pluginName() != null && !query.pluginName().equals(p.getPlugin().getName())) {
            return false;
        }
        if (query.versionConstraint() != null
                && !VersionConstraint.parse(query.versionConstraint()).matches(p.getVersion())) {
            return false;
        }
        return true;
    }

    /** 从候选列表选出版本号最高者；版本相同则选优先级最高者。 */
    private RegisteredServiceProvider<?> latestOf(List<RegisteredServiceProvider<?>> candidates) {
        RegisteredServiceProvider<?> best = candidates.get(0);
        for (RegisteredServiceProvider<?> p : candidates) {
            int vc = VersionParser.compare(p.getVersion(), best.getVersion());
            if (vc > 0 || (vc == 0 && p.getPriority().compareTo(best.getPriority()) > 0)) {
                best = p;
            }
        }
        return best;
    }

    private int indexOf(List<RegisteredServiceProvider<?>> list, RegisteredServiceProvider<?> target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == target) {
                return i;
            }
        }
        return list.size();
    }

    /** 注册新提供者后，通知该类型的订阅者。 */
    private void notifyWatchersRegistered(Class<?> serviceClass, RegisteredServiceProvider<?> added,
                                          RegisteredServiceProvider<?> oldTop, boolean becameTop) {
        CopyOnWriteArrayList<WatchEntry> list = watchers.get(serviceClass);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (WatchEntry e : list) {
            if (!e.active) {
                continue;
            }
            try {
                e.fireRegistered(added);
                if (becameTop && oldTop != null && oldTop != added) {
                    e.fireReplaced(oldTop, added);
                }
            } catch (Exception ex) {
                LOGGER.error("Error notifying service watcher from plugin '{}'.", e.plugin.getName(), ex);
            }
        }
    }

    /** 注销一个提供者后，通知该类型的订阅者（回调注销 + 若发生了主导者替换则回调替换）。 */
    private void notifyWatchersUnregistered(Class<?> serviceClass, RegisteredServiceProvider<?> removed,
                                            RegisteredServiceProvider<?> newTop, boolean removedWasTop) {
        CopyOnWriteArrayList<WatchEntry> list = watchers.get(serviceClass);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (WatchEntry e : list) {
            if (!e.active) {
                continue;
            }
            try {
                e.fireUnregistered(removed);
                if (removedWasTop && newTop != null && newTop != removed) {
                    e.fireReplaced(removed, newTop);
                }
            } catch (Exception ex) {
                LOGGER.error("Error notifying service watcher from plugin '{}'.", e.plugin.getName(), ex);
            }
        }
    }

    private void cancelWatchersOf(AkiPlugin plugin) {
        for (CopyOnWriteArrayList<WatchEntry> list : watchers.values()) {
            list.removeIf(e -> {
                if (e.plugin == plugin) {
                    e.active = false;
                    return true;
                }
                return false;
            });
        }
    }

    /** 一个已注册的订阅项。调用 {@link #cancel()} 后不再接收回调。 */
    private final class WatchEntry implements ServiceWatch {
        private final AkiPlugin plugin;
        private final Class<?> serviceClass;
        private final ServiceWatcher<?> watcher;
        private volatile boolean active = true;

        WatchEntry(AkiPlugin plugin, Class<?> serviceClass, ServiceWatcher<?> watcher) {
            this.plugin = plugin;
            this.serviceClass = serviceClass;
            this.watcher = watcher;
        }

        @Override
        public void cancel() {
            active = false;
            CopyOnWriteArrayList<WatchEntry> list = watchers.get(serviceClass);
            if (list != null) {
                list.remove(this);
            }
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        void fireRegistered(RegisteredServiceProvider<?> p) {
            ((ServiceWatcher) watcher).onServiceRegistered(p);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        void fireUnregistered(RegisteredServiceProvider<?> p) {
            ((ServiceWatcher) watcher).onServiceUnregistered(p);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        void fireReplaced(RegisteredServiceProvider<?> oldP, RegisteredServiceProvider<?> newP) {
            ((ServiceWatcher) watcher).onServiceReplaced(oldP, newP);
        }
    }

    /** 参数非法时返回的哑监听句柄。 */
    private enum InactiveWatch implements ServiceWatch {
        INSTANCE;

        @Override
        public void cancel() {
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }
}