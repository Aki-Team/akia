package com.akiteam.akia.service;

/**
 * 服务查询条件（参考 Spring / Guice 的服务查询思路）。
 * <p>
 * 通过链式方法组合多种筛选维度，最后交给
 * {@link ServiceRegistry#query(ServiceQuery)} 执行。条件之间为“与”关系，未设置的条件不做筛选。
 * 需至少指定 {@link #forClass(Class)} 明确查询的服务接口类型。
 * <p>
 * 不可变对象：每次设置条件都会返回新的实例，可安全复用。
 * <pre>{@code
 * List<...> matches = ServiceRegistry.query(ServiceQuery.forClass(EconomyService.class)
 *         .priority(ServicePriority.High)
 *         .plugin("EconomyPlugin")
 *         .version(">=2.0"));
 * }</pre>
 */
public final class ServiceQuery {

    private final Class<?> serviceClass;
    private final ServicePriority priority;
    private final String pluginName;
    private final String versionConstraint;
    private final boolean latest;

    private ServiceQuery(Class<?> serviceClass, ServicePriority priority, String pluginName,
                         String versionConstraint, boolean latest) {
        this.serviceClass = serviceClass;
        this.priority = priority;
        this.pluginName = pluginName;
        this.versionConstraint = versionConstraint;
        this.latest = latest;
    }

    /**
     * 创建一个针对指定服务接口类型的空查询。
     *
     * @param serviceClass 要查询的服务接口类型（不应为 {@code null}）
     * @return 查询对象
     */
    public static ServiceQuery forClass(Class<?> serviceClass) {
        return new ServiceQuery(serviceClass, null, null, null, false);
    }

    /** 设置要求的优先级（精确匹配该优先级）。 */
    public ServiceQuery priority(ServicePriority priority) {
        return new ServiceQuery(serviceClass, priority, pluginName, versionConstraint, latest);
    }

    /** 设置要求的注册插件名（精确匹配插件名）。 */
    public ServiceQuery plugin(String pluginName) {
        return new ServiceQuery(serviceClass, priority, pluginName, versionConstraint, latest);
    }

    /** 设置版本约束表达式（如 {@code ">=2.0"}、{@code "^1.2"}）。 */
    public ServiceQuery version(String versionConstraint) {
        return new ServiceQuery(serviceClass, priority, pluginName, versionConstraint, latest);
    }

    /** 开启“只需最新（版本号最高者）者”模式，与其它条件叠加使用。 */
    public ServiceQuery latest() {
        return new ServiceQuery(serviceClass, priority, pluginName, versionConstraint, true);
    }

    /** 返回服务接口类型。 */
    public Class<?> serviceClass() {
        return serviceClass;
    }

    /** 返回已设置的优先级，未设置则为 {@code null}。 */
    public ServicePriority priority() {
        return priority;
    }

    /** 返回已设置的插件名，未设置则为 {@code null}。 */
    public String pluginName() {
        return pluginName;
    }

    /** 返回已设置的版本约束表达式，未设置则为 {@code null}。 */
    public String versionConstraint() {
        return versionConstraint;
    }

    /** 返回是否只需最新版本者。 */
    public boolean latestOnly() {
        return latest;
    }
}