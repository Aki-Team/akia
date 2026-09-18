package com.akiteam.akia.service;

/**
 * 服务提供者优先级（参考 Paper 的 {@code ServicePriority}）。
 * <p>
 * 提供服务的插件选择优先级声明其服务；获取服务时返回当前注册中优先级最高者，
 * 同优先级则返回先注册者。语义（如“Normal 是默认推荐”）由插件作者约定。
 */
public enum ServicePriority {
    /** 最低优先级。 */
    Lowest,
    /** 低优先级。 */
    Low,
    /** 普通优先级（推荐默认）。 */
    Normal,
    /** 高优先级。 */
    High,
    /** 最高优先级。 */
    Highest
}