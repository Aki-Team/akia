package com.akiteam.akia.service.impl;

/**
 * 语义化版本（Semantic Versioning）解析与比较工具。
 * <p>
 * 支持的版本格式为 {@code major.minor.patch}（如 {@code 2.1.0}）。缺省的部分按 0 处理：
 * {@code "2"} → {@code 2.0.0}，{@code "2.1"} → {@code 2.1.0}。预发布 / 构建元数据标签
 * （如 {@code -rc1}、{@code +build}）会被忽略，只比较主 / 次 / 补丁三段数字。
 * <p>
 * 此类是纯静态工具，线程安全，可被 {@code VersionConstraint} 及查询逻辑复用。
 */
public final class VersionParser {

    /** 表示一个语义版本的三个数字分量 {@code [major, minor, patch]}。 */
    private VersionParser() {
    }

    /**
     * 解析版本字符串为数字分量数组。
     *
     * @param text 版本字符串（可为 {@code null} 或空串）
     * @return {@code [major, minor, patch]}，非法输入返回 {@code [0, 0, 0]}（不抛异常）
     */
    public static int[] parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new int[]{0, 0, 0};
        }
        String clean = text.trim();
        // 去掉预发布 / 构建元数据部分，只保留 x.y.z
        int dash = clean.indexOf('-');
        int plus = clean.indexOf('+');
        int cut = Integer.MAX_VALUE;
        if (dash >= 0) {
            cut = Math.min(cut, dash);
        }
        if (plus >= 0) {
            cut = Math.min(cut, plus);
        }
        if (cut != Integer.MAX_VALUE) {
            clean = clean.substring(0, cut);
        }
        String[] parts = clean.split("\\.");
        int major = 0, minor = 0, patch = 0;
        try {
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                major = Integer.parseInt(parts[0].trim());
            }
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                minor = Integer.parseInt(parts[1].trim());
            }
            if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                patch = Integer.parseInt(parts[2].trim());
            }
        } catch (NumberFormatException e) {
            return new int[]{0, 0, 0};
        }
        return new int[]{Math.max(0, major), Math.max(0, minor), Math.max(0, patch)};
    }

    /**
     * 比较两个版本分量数组（长度不足按 0 补齐）。
     *
     * @param a 版本 A
     * @param b 版本 B
     * @return 负数表示 {@code a < b}，0 表示相等，正数表示 {@code a > b}
     */
    public static int compare(int[] a, int[] b) {
        int[] x = normalize(a);
        int[] y = normalize(b);
        for (int i = 0; i < 3; i++) {
            int c = Integer.compare(x[i], y[i]);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    /**
     * 比较两个版本字符串的大小。
     *
     * @param av 版本 A 字符串
     * @param bv 版本 B 字符串
     * @return 负数 / 0 / 正数，语义同 {@link #compare(int[], int[])}
     */
    public static int compare(String av, String bv) {
        return compare(parse(av), parse(bv));
    }

    /** 确保分量数组长度为 3，不足补 0。 */
    private static int[] normalize(int[] v) {
        if (v == null) {
            return new int[]{0, 0, 0};
        }
        int[] out = new int[]{0, 0, 0};
        for (int i = 0; i < out.length && i < v.length; i++) {
            out[i] = v[i];
        }
        return out;
    }
}