package com.akiteam.akiloader.service;

import com.akiteam.akiloader.service.impl.VersionConstraintImpl;

/**
 * 服务版本约束（参考 Maven / npm 的版本范围语义，采用轻量自建实现，无外部依赖）。
 * <p>
 * 通过 {@link #parse(String)} 把人类可读的约束表达式解析为匹配器，供
 * {@link ServiceRegistry#query(ServiceQuery)} 等按版本筛选服务。
 * <p>
 * 支持的表达式：
 * <ul>
 *     <li>{@code "1.2.3"} 或 {@code "=1.2.3"} —— 精确匹配该版本</li>
 *     <li>{@code ">=1.2.0"} —— 大于等于</li>
 *     <li>{@code ">1.2.0"}、{@code "<2.0.0"}、{@code "<=1.2.0"} —— 相应比较</li>
 *     <li>{@code "^1.2.0"} —— 兼容：主版本相同，次版本 {@code >= 1.2.0}（即 {@code [1.2.0, 2.0.0)}）</li>
 *     <li>{@code "~1.2.0"} —— 近似：主、次版本相同，补丁版本 {@code >= 0}（即 {@code [1.2.0, 1.3.0)}）</li>
 * </ul>
 * 版本均按语义化版本（major.minor.patch）比较，缺省分量按 0 处理。
 */
public interface VersionConstraint {

    /**
     * 判断给定版本字符串是否满足此约束。
     *
     * @param version 目标版本字符串（如 {@code "2.1.0"}）
     * @return {@code true} 表示满足；非法版本始终返回 {@code false}
     */
    boolean matches(String version);

    /**
     * 返回原始约束表达式（如 {@code ">=2.0"}），便于展示与调试。
     *
     * @return 表达式字符串
     */
    String expression();

    /**
     * 解析约束表达式为可复用的匹配器。
     * <p>
     * 非法表达式（无法识别操作符或版本）会返回一个恒为 {@code false} 的安全匹配器，
     * 不抛异常，避免查询路径被破坏。
     *
     * @param expression 约束表达式，若为 {@code null} 或空白，返回“匹配所有版本”的匹配器
     * @return 匹配器实例
     */
    static VersionConstraint parse(String expression) {
        return VersionConstraintImpl.parse(expression);
    }
}