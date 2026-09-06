package com.akiteam.akiloader.loader;

import com.akiteam.akiloader.AkiLoader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentMap;

/**
 * 插件专用的类加载器，在 {@link URLClassLoader} 基础上支持"API 导出 / 父优先委派"。
 * <p>
 * 背景：每个插件使用独立类加载器实现隔离，但若插件 A 提供、插件 B 消费的 API 类
 * 由各自的类加载器各自加载，会得到两个不同的 {@link Class} 对象（loader identity），
 * 导致以 {@code Class} 为 key 的 {@code ServiceRegistry} 无法跨插件匹配。
 * <p>
 * 解决方案：插件可在 {@code plugin.json} 的 {@code exports} 中声明若干导出的 API 包。
 * AkiLoader 维护 {@code exportIndex}（包名 → 拥有该包导出的插件类加载器）。加载某类时：
 * <ol>
 *     <li>若该类所在包被<b>其他</b>插件导出 → 委托给该插件加载器加载（父优先，保证
 *         同一个包在所有插件里共享同一个类）；</li>
 *     <li>否则：<b>JVM 标准库</b>（{@code java.*} 等）始终交给父加载器；其余先试父加载器
 *         （AkiLoader 自身）再试自身 URL 资源（子优先，保持原有隔离）。</li>
 * </ol>
 * 类加载遵循 Java 规范使用加载锁，保证线程安全。
 */
public class AkiPluginClassLoader extends URLClassLoader {

    private static final org.slf4j.Logger LOGGER = AkiLoader.LOGGER;

    /** 当前插件的名字（仅用于日志）。 */
    private final String pluginName;

    /** 全局导出索引：包名 → 导出该包的插件类加载器，所有插件共享。 */
    private final ConcurrentMap<String, AkiPluginClassLoader> exportIndex;

    public AkiPluginClassLoader(URL[] urls, ClassLoader parent,
                                String pluginName, ConcurrentMap<String, AkiPluginClassLoader> exportIndex) {
        super(urls, parent);
        this.pluginName = pluginName;
        this.exportIndex = exportIndex;
    }

    public String getPluginName() {
        return pluginName;
    }

    /**
     * 自定义类加载顺序，实现"导出包父优先委派"，其余保持子优先。
     *
     * @param name 类全限定名
     * @param resolve 是否完成链接
     * @return 加载得到的 {@link Class}
     * @throws ClassNotFoundException 当所有来源都无法加载该类时
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 1. 已加载直接返回
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }

            // 2. 计算所属包名（默认包不参与导出委派）
            int dot = name.lastIndexOf('.');
            String pkg = dot > 0 ? name.substring(0, dot) : "";

            String clause = pkg == null || pkg.isEmpty() ? null : pkg;

            // 3. JVM 标准库始终交给父（引导）加载器，绝不交给插件加载器
            if (clause != null
                    && (clause.startsWith("java.") || clause.startsWith("javax.")
                    || clause.startsWith("sun.") || clause.startsWith("jdk."))) {
                c = getParent().loadClass(name);
            } else if (clause != null) {
                // 4. 导出包：委派给导出该包的插件加载器（父优先，保证跨插件共享同一类）
                AkiPluginClassLoader exporter = exportIndex.get(clause);
                if (exporter != null && exporter != this) {
                    try {
                        c = exporter.loadClass(name);
                    } catch (ClassNotFoundException ignore) {
                        // 导出者也无法加载，落到本加载器自身解析
                    }
                }
                if (c == null) {
                    c = findLocalFirst(name);
                }
            } else {
                c = findLocalFirst(name);
            }

            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    /** 子优先：先父加载器（AkiLoader 自身）、再自身 URL 资源。 */
    private Class<?> findLocalFirst(String name) throws ClassNotFoundException {
        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException parentNotFound) {
            return findClass(name);
        }
    }

    @Override
    public String toString() {
        return "AkiPluginClassLoader[" + pluginName + "]";
    }
}