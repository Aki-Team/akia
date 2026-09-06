package com.akiteam.akiloader.event;

import com.akiteam.akiloader.AkiLoader;
import com.akiteam.akiloader.api.AkiPlugin;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AkiLoader 的插件事件总线（参考 Paper {@code SimplePluginManager} 的事件分发模型）。
 * <p>
 * <b>注册</b>：{@link #registerEvents(AkiPlugin, Object)} 扫描监听器对象上所有带
 * {@link AkiEventHandler} 的方法，按事件类型分组、按优先级排序后存储。
 * <p>
 * <b>分发</b>：{@link #fireEvent(AkiEvent)} 按 事件类型 → 优先级(LOWEST→NORMAL→HIGHEST→MONITOR)
 * 依次反射调用监听器。取消语义：
 * <ul>
 *     <li>若事件实现了 {@link Cancellable} 且已被取消，则停止向后续监听器传播；</li>
 *     <li>带 {@code ignoreCancelled = true} 的监听器，对已取消事件会被跳过。</li>
 * </ul>
 * <b>异常安全</b>：单个监听器抛出异常（含反射包装的 {@link InvocationTargetException}）
 * 会被捕获并记日志，不影响其他监听器与游戏运行。
 * <p>
 * 线程安全：注册表使用 {@link ConcurrentHashMap}，事件目标列表使用
 * {@link CopyOnWriteArrayList}，可安全并发注册与分发。
 */
public class EventBus {

    private static final org.slf4j.Logger LOGGER = AkiLoader.LOGGER;

    /** 事件类型 → 该事件的所有监听项（已按优先级排序）。 */
    private final ConcurrentMap<Class<?>, List<HandlerEntry>> handlersByType = new ConcurrentHashMap<>();
    /** 插件 → 它注册过的所有事件类型（用于卸载时反注册）。 */
    private final ConcurrentMap<AkiPlugin, Set<Class<?>>> pluginRegistrations = new ConcurrentHashMap<>();

    /**
     * 一个已注册的监听项。
     *
     * @param owner         所属插件（卸载时据此反注册）
     * @param listener      持有回调方法的监听器对象
     * @param method        带 {@link AkiEventHandler} 注解的回调方法
     * @param priority      优先级
     * @param ignoreCancelled 是否忽略已取消事件
     */
    private record HandlerEntry(AkiPlugin owner, Object listener, Method method,
                                EventPriority priority, boolean ignoreCancelled) {
    }

    /** 空构造器。 */
    public EventBus() {
    }

    /**
     * 统计某插件当前注册的监听器方法总数（按 {@code @AkiEventHandler} 方法计）。
     * 供诊断命令统计每个插件的事件监听数量。
     *
     * @param plugin 目标插件
     * @return 该插件注册的监听器方法数
     */
    public int getListenerCount(AkiPlugin plugin) {
        if (plugin == null) {
            return 0;
        }
        int count = 0;
        for (List<HandlerEntry> list : handlersByType.values()) {
            for (HandlerEntry e : list) {
                if (e.owner() == plugin) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 注册一个监听器对象中所有带 {@link AkiEventHandler} 的方法。
     * <p>
     * 会沿类的继承链向上收集方法。无效的处理器（参数不是单个 {@link AkiEvent}、
     * 返回类型不是 {@code void}）会被跳过并记录警告，不中断其余注册。
     *
     * @param plugin   拥有该监听器的插件（不允许为 {@code null}）
     * @param listener 监听器对象（通常即插件主类实例）
     */
    public void registerEvents(AkiPlugin plugin, Object listener) {
        if (plugin == null || listener == null) {
            LOGGER.warn("Ignoring event registration with null plugin/listener.");
            return;
        }
        for (Method method : collectEventMethods(listener.getClass())) {
            AkiEventHandler annotation = method.getAnnotation(AkiEventHandler.class);
            if (annotation == null) {
                continue;
            }
            if (method.getReturnType() != void.class
                    || method.getParameterCount() != 1
                    || !AkiEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                LOGGER.warn("Skipping invalid @AkiEventHandler '{}': needs one {} parameter and void return.",
                        method, AkiEvent.class.getSimpleName());
                continue;
            }

            Class<?> eventType = method.getParameterTypes()[0];
            if (!method.canAccess(listener)) {
                method.setAccessible(true);
            }
            HandlerEntry entry = new HandlerEntry(plugin, listener, method,
                    annotation.priority(), annotation.ignoreCancelled());
            List<HandlerEntry> list = handlersByType.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
            list.add(entry);
            list.sort(Comparator.comparingInt(h -> h.priority().ordinal()));
            pluginRegistrations.computeIfAbsent(plugin, k -> ConcurrentHashMap.newKeySet()).add(eventType);
        }
    }

    /**
     * 反注册某个插件注册过的全部监听器，并清理空的事件类型表。
     * <p>
     * 插件卸载（{@code onDisable} 后、关闭 ClassLoader 前）时应调用，避免残留
     * 反射句柄与插件 Class 引用导致 ClassLoader/Metaspace 无法回收。
     *
     * @param plugin 要清理的插件
     */
    public void unregisterAll(AkiPlugin plugin) {
        Set<Class<?>> types = pluginRegistrations.remove(plugin);
        if (types == null) {
            return;
        }
        for (Class<?> eventType : types) {
            List<HandlerEntry> list = handlersByType.get(eventType);
            if (list == null) {
                continue;
            }
            list.removeIf(entry -> entry.owner() == plugin);
            if (list.isEmpty()) {
                handlersByType.remove(eventType);
            }
        }
        LOGGER.info("Unregistered event handlers for plugin '{}'.", plugin.getName());
    }

    /**
     * 分发一个插件事件。
     * <p>
     * 按 事件类型匹配监听器，并按 优先级从低到高 依次调用。
     * 若事件实现 {@link Cancellable} 且已被取消，则停止向后续监听器传播；
     * 标注了 {@code ignoreCancelled = true} 的监听器对已取消事件被跳过。
     *
     * @param event 要分发的事件，不允许为 {@code null}
     */
    public void fireEvent(AkiEvent event) {
        List<HandlerEntry> handlers = handlersByType.get(event.getClass());
        if (handlers == null || handlers.isEmpty()) {
            return;
        }
        for (HandlerEntry entry : handlers) {
            boolean cancelled = event instanceof Cancellable c && c.isCancelled();
            if (cancelled && entry.ignoreCancelled()) {
                continue;
            }
            if (cancelled) {
                break; // 事件已取消，停止继续传播
            }
            try {
                entry.method().invoke(entry.listener(), event);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                LOGGER.error("Error invoking event handler '{}': {}", entry.method(), cause.toString(), cause);
            } catch (Exception e) {
                LOGGER.error("Failed to invoke event handler '{}': {}", entry.method(), e.toString(), e);
            }
        }
    }

    /** 收集类的继承链上所有声明的成员方法（不含 Object）。 */
    private List<Method> collectEventMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            Collections.addAll(methods, c.getDeclaredMethods());
        }
        return methods;
    }
}