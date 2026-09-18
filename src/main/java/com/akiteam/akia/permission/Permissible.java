package com.akiteam.akia.permission;

/**
 * 一个可校验字符串权限节点的对象（参考 Paper {@code Permissible}/{@code PermissibleBase}）。
 * <p>
 * Akia 提供两种视角的实现：
 * <ul>
 *     <li>{@link CommandPermissible}——包装某个 {@link net.minecraft.commands.CommandSourceStack}，
 *         {@link CommandRegistry} 在执行命令前用它校验 {@code permission("节点")}；</li>
 *     <li>{@link com.akiteam.akia.api.AkiPlugin#getPermissible()} 的默认实现——把插件本体
 *         视作可信方（等同 OP）。</li>
 * </ul>
 * 目前框架没有外挂权限插件，因此字符串节点默认退化为 OP 校验（op level 2）；未来接入
 * 权限插件时，只需在 {@link #hasPermission(String)} 中改为查询节点状态即可，接口不变。
 */
public interface Permissible {

    /**
     * 校验是否具备指定权限节点。
     *
     * @param permission 权限节点（形如 {@code "test.node"}）；空或 {@code null} 视为放行
     * @return 具备返回 {@code true}，否则 {@code false}
     */
    boolean hasPermission(String permission);

    /**
     * 当前对象是否具备 OP 身份。
     *
     * @return 是返回 {@code true}，否则 {@code false}
     */
    boolean isOp();

    /**
     * 设置 OP 身份。
     * <p>
     * 对不可变的来源（如控制台、或仅包装读取来源的实现）可能为空操作。
     *
     * @param op 设为 {@code true} 授予 OP，{@code false} 撤销
     */
    void setOp(boolean op);
}