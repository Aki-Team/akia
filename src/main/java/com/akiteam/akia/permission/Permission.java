package com.akiteam.akia.permission;

/**
 * 一个权限节点的声明（预留，供未来接入权限插件时登记节点的元数据与默认值）。
 * <p>
 * 当前 Akia 的命令权限校验直接落到 OP 身份（见 {@link CommandPermissible}），
 * 本类暂不参与运行时判定，仅提供权限节点规范化登记入口。
 */
public class Permission {

    /** 权限节点的默认授予策略。 */
    public enum Default {
        /** 默认授予（如所有玩家均可用的基础命令）。 */
        TRUE,
        /** 默认不授予。 */
        FALSE,
        /** 默认仅 OP 可用。 */
        OP_ONLY
    }

    private final String name;
    private final String description;
    private final Default defaultValue;

    /**
     * 构造一个仅含节点名的权限（默认策略 {@link Default#OP_ONLY}）。
     *
     * @param name 权限节点，形如 {@code "test.node"}
     */
    public Permission(String name) {
        this(name, "", Default.OP_ONLY);
    }

    /**
     * 构造一个完整声明的权限节点。
     *
     * @param name         权限节点
     * @param description  权限描述（可为空）
     * @param defaultValue 默认授予策略
     */
    public Permission(String name, String description, Default defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /** 返回权限节点。 */
    public String getName() {
        return name;
    }

    /** 返回权限描述。 */
    public String getDescription() {
        return description;
    }

    /** 返回默认授予策略。 */
    public Default getDefaultValue() {
        return defaultValue;
    }
}