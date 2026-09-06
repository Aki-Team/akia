package com.akiteam.akiloader.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为 AkiLoader 事件监听器（参考 Paper 的 {@code EventHandler}）。
 * <p>
 * 被标注的方法必须满足：
 * <ul>
 *     <li>返回类型为 {@code void}；</li>
 *     <li>恰好有一个参数，且该参数实现了 {@link AkiEvent}（即它监听的插件事件类型）。</li>
 * </ul>
 * 插件的主类（或注册到 {@code EventBus} 的监听器对象）中的这些方法，
 * 会在 {@link EventBus#fireEvent(AkiEvent)} 分发时按优先级被调用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AkiEventHandler {

    /**
     * 该监听器的执行优先级，默认 {@link EventPriority#NORMAL}。
     *
     * @return 优先级
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * 是否忽略已取消的事件。
     * <p>
     * 为 {@code true} 时，事件一旦被标记为已取消（实现了 {@link Cancellable} 且
     * {@code isCancelled()} 为真），该监听器就不会被调用。
     *
     * @return 是否忽略已取消事件
     */
    boolean ignoreCancelled() default false;
}