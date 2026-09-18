package com.akiteam.akia.service.impl;

import com.akiteam.akia.service.VersionConstraint;

/**
 * {@link VersionConstraint} 的默认实现（包内使用，对外通过静态工厂获取）。
 * <p>
 * 解析操作符前缀与版本号，比较时使用 {@link VersionParser}。内部匹配器以函数形式封装，
 * 保证单实例可安全复用于多条查询。
 */
public final class VersionConstraintImpl implements VersionConstraint {

    private final String expression;
    private final java.util.function.Predicate<String> matcher;

    private VersionConstraintImpl(String expression, java.util.function.Predicate<String> matcher) {
        this.expression = expression;
        this.matcher = matcher;
    }

    @Override
    public boolean matches(String version) {
        return matcher.test(version);
    }

    @Override
    public String expression() {
        return expression;
    }

    /**
     * 解析表达式。/  空表达式表示“匹配一切”。
     */
    public static VersionConstraint parse(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return new VersionConstraintImpl("", v -> true);
        }
        String expr = expression.trim();
        String op;
        String verPart;
        if (expr.startsWith(">=")) {
            op = ">=";
            verPart = expr.substring(2);
        } else if (expr.startsWith("<=")) {
            op = "<=";
            verPart = expr.substring(2);
        } else if (expr.startsWith(">")) {
            op = ">";
            verPart = expr.substring(1);
        } else if (expr.startsWith("<")) {
            op = "<";
            verPart = expr.substring(1);
        } else if (expr.startsWith("^")) {
            op = "^";
            verPart = expr.substring(1);
        } else if (expr.startsWith("~")) {
            op = "~";
            verPart = expr.substring(1);
        } else if (expr.startsWith("=")) {
            op = "=";
            verPart = expr.substring(1);
        } else {
            // 无操作符前缀，默认精确匹配
            op = "=";
            verPart = expr;
        }
        verPart = verPart.trim();
        int[] target = VersionParser.parse(verPart);
        if (!verPart.matches(".*\\d.*")) {
            // 版本部分不含任何数字（如 ">abc"）→ 返回永不匹配的安全匹配器
            return new VersionConstraintImpl(expr, v -> false);
        }
        switch (op) {
            case ">=":
                return new VersionConstraintImpl(expr, v -> VersionParser.compare(VersionParser.parse(v), target) >= 0);
            case "<=":
                return new VersionConstraintImpl(expr, v -> VersionParser.compare(VersionParser.parse(v), target) <= 0);
            case ">":
                return new VersionConstraintImpl(expr, v -> VersionParser.compare(VersionParser.parse(v), target) > 0);
            case "<":
                return new VersionConstraintImpl(expr, v -> VersionParser.compare(VersionParser.parse(v), target) < 0);
            case "^": {
                // 兼容版本：主版本相同，且 >= target
                int major = target[0];
                return new VersionConstraintImpl(expr, v -> {
                    int[] p = VersionParser.parse(v);
                    return p[0] == major && VersionParser.compare(p, target) >= 0;
                });
            }
            case "~": {
                // 近似版本：主、次版本相同，且 >= target
                int major = target[0];
                int minor = target[1];
                return new VersionConstraintImpl(expr, v -> {
                    int[] p = VersionParser.parse(v);
                    return p[0] == major && p[1] == minor && VersionParser.compare(p, target) >= 0;
                });
            }
            case "=":
            default:
                return new VersionConstraintImpl(expr, v -> VersionParser.compare(VersionParser.parse(v), target) == 0);
        }
    }
}