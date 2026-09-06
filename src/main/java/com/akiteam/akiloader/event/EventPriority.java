package com.akiteam.akiloader.event;

/**
 * 事件监听方法的执行优先级。
 * <p>
 * 优先级从低到高依次为：
 * <ul>
 *     <li>{@link #LOWEST} —— 最先执行，通常用于观察或早期出局；</li>
 *     <li>{@link #NORMAL} —— 默认优先级；</li>
 *     <li>{@link #HIGHEST} —— 较晚执行；</li>
 *     <li>{@link #MONITOR} —— 最晚执行，一般只读，不应修改事件结果。</li>
 * </ul>
 * 同优先级内部按注册时的顺序执行。
 */
public enum EventPriority {

    /** 最低优先级，最先被调用。 */
    LOWEST,
    /** 默认优先级。 */
    NORMAL,
    /** 较高优先级。 */
    HIGHEST,
    /** 最高优先级（监控用），最晚被调用。 */
    MONITOR
}