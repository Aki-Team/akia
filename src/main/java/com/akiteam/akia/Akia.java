package com.akiteam.akia;

import com.akiteam.akia.event.NeoForgeEventBridge;
import com.akiteam.akia.loader.PluginManagerImpl;
import com.akiteam.akia.scheduler.Scheduler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

// The value here should match the modId field in neoforge.mods.toml
@Mod(Akia.MODID)
public class Akia {
    public static final String MODID = "akia";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 插件默认存放目录（位于游戏运行目录下）：{@code run/akia/plugins}。 */
    public static final String PLUGINS_DIR = "akia/plugins";

    /**
     * Akia 当前对外开放的 API 版本。
     * <p>
     * 插件可通过 {@code plugin.json} 的 {@code api-version} 声明其所需 API 版本；
     * 加载器会拒绝声明版本高于此值（不兼容）的插件。遵循语义化版本 {major.minor.patch}。
     */
    public static final String API_VERSION = "1.0";

    private static PluginManagerImpl pluginManager;

    /** NeoForge → Akia 事件桥接器，把游戏事件转发为插件事件。 */
    private final NeoForgeEventBridge eventBridge;

    // The constructor is the first code run when the mod is loaded.
    public Akia(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Akia has been initialized!");
        pluginManager = new PluginManagerImpl();
        pluginManager.loadAllFromDirectory(FMLPaths.GAMEDIR.get().resolve(PLUGINS_DIR));
        this.eventBridge = new NeoForgeEventBridge(pluginManager.getEventBus());
        // 每游戏刻结束时，在（服务器逻辑所在）主线程消费调度器里到期的同步任务
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) ->
                Scheduler.INSTANCE.onServerTick());
    }

    /**
     * 返回全局插件管理器实例。
     * <p>
     * {@link ServerLifecycleHandler} 等静态事件订阅者需要通过它访问已加载的插件。
     *
     * @return 由 {@code Akia} 构造时创建的插件管理器（正常情况下不会为 {@code null}）
     */
    public static PluginManagerImpl getPluginManager() {
        return pluginManager;
    }
}