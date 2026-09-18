package com.akiteam.akia.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerNegotiationEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent.Detonate;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent.XpChange;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent.LevelChange;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.world.entity.player.Player;

/**
 * NeoForge → Akia 的事件桥接器（实现约定中的“方案 A：直接监听”）。
 * <p>
 * 它显式地 {@link NeoForge#EVENT_BUS} 订阅若干游戏事件，在收到时构造对应的
 * 插件事件（实现 {@link Event}）并通过 {@link EventBus#fireEvent(Event)} 转发给插件。
 * 这样插件只面向 Akia 自己的事件类型编程，不直接依赖 NeoForge 事件细节。
 * <p>
 * 需要开放更多游戏事件时，在这个类里按同样的模式添加 {@code @SubscribeEvent} 方法即可。
 */
public final class NeoForgeEventBridge {

    private static final org.slf4j.Logger LOGGER = com.akiteam.akia.Akia.LOGGER;

    private final EventBus eventBus;

    /**
     * 构造桥接器并把自己注册到 NeoForge 事件总线。
     *
     * @param eventBus 插件的 {@link EventBus}，用于转发包装后的事件
     */
    public NeoForgeEventBridge(EventBus eventBus) {
        this.eventBus = eventBus;
        NeoForge.EVENT_BUS.register(this);
    }

    /**
     * 玩家登录（进服）事件 → 包装为 {@link PlayerJoinEvent} 并转发。
     *
     * @param event NeoForge 登录事件
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        eventBus.fireEvent(new PlayerJoinEvent(event));
    }

    /**
     * 玩家退出（退服）事件 → {@link PlayerQuitEvent}。
     */
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        safe(() -> eventBus.fireEvent(new PlayerQuitEvent(event)));
    }

    /**
     * 玩家重生事件 → {@link PlayerRespawnEvent}。
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        safe(() -> eventBus.fireEvent(new com.akiteam.akia.event.PlayerRespawnEvent(event)));
    }

    /**
     * 玩家切换维度事件 → {@link PlayerChangedDimensionEvent}。
     */
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        safe(() -> eventBus.fireEvent(new com.akiteam.akia.event.PlayerChangedDimensionEvent(event)));
    }

    /**
     * 玩家设置出生点 / 睡床事件 → {@link PlayerSetSpawnEvent}。
     */
    @SubscribeEvent
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        safe(() -> eventBus.fireEvent(new com.akiteam.akia.event.PlayerSetSpawnEvent(event)));
    }

    /**
     * 玩家切换游戏模式事件 → {@link PlayerChangeGameModeEvent}。
     */
    @SubscribeEvent
    public void onPlayerChangeGameMode(PlayerChangeGameModeEvent event) {
        safe(() -> eventBus.fireEvent(new com.akiteam.akia.event.PlayerChangeGameModeEvent(event)));
    }

    /**
     * 玩家聊天事件 → {@link PlayerChatEvent}。
     */
    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        safe(() -> eventBus.fireEvent(new PlayerChatEvent(event)));
    }

    /**
     * 命令被调用事件 → {@link PlayerCommandEvent}。
     */
    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        safe(() -> eventBus.fireEvent(new PlayerCommandEvent(event)));
    }

    /**
     * 生物死亡事件 → {@link EntityDeathEvent}。
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        safe(() -> eventBus.fireEvent(new EntityDeathEvent(event)));
    }

    /**
     * 生物受伤（受到伤害）事件 → {@link LivingDamageEvent}。
     */
    @SubscribeEvent
    public void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        safe(() -> eventBus.fireEvent(new LivingDamageEvent(event)));
    }

    /**
     * 玩家攻击实体事件 → {@link PlayerAttackEntityEvent}。
     */
    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        safe(() -> eventBus.fireEvent(new PlayerAttackEntityEvent(event)));
    }

    /**
     * 方块被破坏事件 → {@link BlockBreakEvent}。
     */
    @SubscribeEvent
    public void onBlockBreak(BreakEvent event) {
        safe(() -> eventBus.fireEvent(new BlockBreakEvent(event)));
    }

    /**
     * 方块被放置事件 → {@link BlockPlaceEvent}。
     */
    @SubscribeEvent
    public void onBlockPlace(EntityPlaceEvent event) {
        safe(() -> eventBus.fireEvent(new BlockPlaceEvent(event)));
    }

    /**
     * 爆炸结算阶段事件 → {@link ExplosionEvent}。
     */
    @SubscribeEvent
    public void onExplosionDetonate(Detonate event) {
        safe(() -> eventBus.fireEvent(new ExplosionEvent(event)));
    }

    /**
     * 玩家右键方块交互事件 → {@link PlayerInteractEvent}。
     */
    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        safe(() -> eventBus.fireEvent(new com.akiteam.akia.event.PlayerInteractEvent(event)));
    }

    /**
     * 玩家挖掘速度被计算事件 → {@link PlayerBreakSpeedEvent}。
     */
    @SubscribeEvent
    public void onPlayerBreakSpeed(BreakSpeed event) {
        safe(() -> eventBus.fireEvent(new PlayerBreakSpeedEvent(event)));
    }

    /**
     * 玩家打开容器事件 → {@link PlayerContainerOpenEvent}。
     */
    @SubscribeEvent
    public void onPlayerContainerOpen(PlayerContainerEvent.Open event) {
        safe(() -> eventBus.fireEvent(new PlayerContainerOpenEvent(event)));
    }

    /**
     * 玩家关闭容器事件 → {@link PlayerContainerCloseEvent}。
     */
    @SubscribeEvent
    public void onPlayerContainerClose(PlayerContainerEvent.Close event) {
        safe(() -> eventBus.fireEvent(new PlayerContainerCloseEvent(event)));
    }

    /**
     * 玩家拾取掉落物事件 → {@link PlayerItemPickupEvent}。
     */
    @SubscribeEvent
    public void onPlayerItemPickup(ItemEntityPickupEvent.Pre event) {
        safe(() -> eventBus.fireEvent(new PlayerItemPickupEvent(event)));
    }

    /**
     * 实体生成进入世界事件 → {@link EntitySpawnEvent}。
     */
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        safe(() -> eventBus.fireEvent(new EntitySpawnEvent(event)));
    }

    /**
     * 实体跨维度传送事件 → {@link EntityTravelToDimensionEvent}。
     */
    @SubscribeEvent
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        safe(() -> eventBus.fireEvent(new com.akiteam.akia.event.EntityTravelToDimensionEvent(event)));
    }

    /**
     * 玩家登录握手协商事件（异步）→ {@link PlayerNegotiationEvent}。
     * <p>
     * 该事件在协商线程触发，必须通过 {@code enqueueWork} 切回服务端线程再分发。
     */
    @SubscribeEvent
    public void onPlayerNegotiation(PlayerNegotiationEvent event) {
        event.enqueueWork(() -> safe(() -> eventBus.fireEvent(
                new com.akiteam.akia.event.PlayerNegotiationEvent(event))));
    }

    /**
     * 玩家丢出物品事件 → {@link PlayerDropItemEvent}。
     */
    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        safe(() -> eventBus.fireEvent(new PlayerDropItemEvent(event)));
    }

    /**
     * 玩家传送事件 → {@link PlayerTeleportEvent}（仅当传送主体为玩家）。
     */
    @SubscribeEvent
    public void onEntityTeleport(EntityTeleportEvent event) {
        safe(() -> {
            if (event.getEntity() instanceof Player) {
                eventBus.fireEvent(new PlayerTeleportEvent(event));
            }
        });
    }

    /**
     * 玩家死亡掉落事件 → {@link PlayerDeathEvent}（仅当死亡主体为玩家）。
     * <p>
     * 对应 NeoForge 的 {@code LivingDropsEvent}；任务七的 {@code EntityDeathEvent}
     * 已占用 {@code LivingDeathEvent}，故死亡本体事件不再重复桥接。
     */
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        safe(() -> {
            if (event.getEntity() instanceof Player) {
                eventBus.fireEvent(new PlayerDeathEvent(event));
            }
        });
    }

    /**
     * 玩家进食/饮用完物品事件 → {@link PlayerItemConsumeEvent}（仅当使用主体为玩家）。
     * <p>
     * 对应 NeoForge 的 {@code LivingEntityUseItemEvent.Finish}。
     */
    @SubscribeEvent
    public void onLivingEntityUseItemFinish(Finish event) {
        safe(() -> {
            if (event.getEntity() instanceof Player) {
                eventBus.fireEvent(new PlayerItemConsumeEvent(event));
            }
        });
    }

    /**
     * 玩家右键实体事件 → {@link PlayerInteractEntityEvent}。
     */
    @SubscribeEvent
    public void onPlayerInteractEntity(EntityInteract event) {
        safe(() -> eventBus.fireEvent(new PlayerInteractEntityEvent(event)));
    }

    /**
     * 玩家经验条变化事件 → {@link PlayerExpChangeEvent}。
     */
    @SubscribeEvent
    public void onPlayerXpChange(XpChange event) {
        safe(() -> eventBus.fireEvent(new PlayerExpChangeEvent(event)));
    }

    /**
     * 玩家经验等级变化事件 → {@link PlayerLevelChangeEvent}。
     */
    @SubscribeEvent
    public void onPlayerLevelChange(LevelChange event) {
        safe(() -> eventBus.fireEvent(new PlayerLevelChangeEvent(event)));
    }

    /**
     * 抛射物命中事件 → {@link ProjectileHitEvent}。
     */
    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        safe(() -> eventBus.fireEvent(new ProjectileHitEvent(event)));
    }

    /**
     * 区块加载事件 → {@link ChunkLoadEvent}。
     */
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        safe(() -> eventBus.fireEvent(new ChunkLoadEvent(event)));
    }

    /**
     * 区块卸载事件 → {@link ChunkUnloadEvent}。
     */
    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        safe(() -> eventBus.fireEvent(new ChunkUnloadEvent(event)));
    }

    /**
     * 世界（维度）加载事件 → {@link WorldLoadEvent}。
     */
    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        safe(() -> eventBus.fireEvent(new WorldLoadEvent(event)));
    }

    /**
     * 世界（维度）保存事件 → {@link WorldSaveEvent}。
     */
    @SubscribeEvent
    public void onLevelSave(LevelEvent.Save event) {
        safe(() -> eventBus.fireEvent(new WorldSaveEvent(event)));
    }

    /**
     * 世界（维度）卸载事件 → {@link WorldUnloadEvent}。
     */
    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        safe(() -> eventBus.fireEvent(new WorldUnloadEvent(event)));
    }

    /**
     * 兜底：包裹一条无参逻辑，异常时仅记日志，绝不让桥内异常影响游戏运行。
     *
     * @param action 无参动作
     */
    private void safe(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            LOGGER.error("Error bridging NeoForge event to Akia event bus.", ex);
        }
    }
}