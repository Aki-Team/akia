# AkiLoader 插件开发指南

> 面向：从零理解 AkiLoader 插件模型、并据此把 Paper 的 PlaceholderAPI(PAPI) 移植到 AkiLoader 的开发者 / AI。
> 阅读完本文后，你应该能：定义一个插件、注册事件/命令/调度器/配置/PDC/文本，以及最重要的——用 ServiceRegistry + exports 实现跨插件 API 调用。

***

## 1. 核心概念

### 1.1 插件是什么

插件（Plugin）是一个独立的 JAR 文件，实现 `com.akiteam.akiloader.api.AkiPlugin` 接口，被 AkiLoader 在游戏启动时扫入并管理。每个插件拥有独立的类加载器（ClassLoader），插件与插件之间默认互相不可见（类隔离）。

插件有明确的生命周期，由 AkiLoader 驱动，顺序固定：

- onLoad()：被加载时调用。只做轻量初始化（创建配置、注册数据结构）。此时服务器还未就绪。

- onEnable()：服务器启动完成后调用。真正开始干活（注册服务、监听事件、启动调度、注册命令）。也是注册/获取服务、读取配置的推荐时机。

- onDisable()：被禁用时调用（关服前或热重载时）。释放资源、停止线程、保存状态。

### 1.2 插件如何被加载

AkiLoader 扫描运行目录下的插件目录（运行时为 `run/akiloader/`），读取每个 `.jar` 里的 `plugin.json` 元数据，解析出插件名、主类、依赖等信息并实例化主类。

目录结构：

```text
run/
  akiloader/
    GreetingProvider.jar     <-- 插件 A
    ShopConsumer.jar         <-- 插件 B
    <插件名>/config.yml      <-- 每个插件自己的配置文件（运行时生成）
```

### 1.3 插件如何被注入能力（registerXXX 钩子）

AkiLoader 不要求插件从某个基类继承，而是用一组默认空实现的 `registerXXX` 钩子在主类上"注入能力"。每个钩子都在加载阶段被 AkiLoader 调用，插件把对应的能力对象存进自己的字段即可使用。

AkiPlugin 接口的全部钩子（均为 default，按需覆盖）：

```java
void onLoad();
void onEnable();
void onDisable();
PluginInfo getPluginInfo();                       // 必须实现：返回元数据
default String getName();                         // 由 getPluginInfo().name() 得到
default void registerEvents(EventBus bus);
default void registerCommands(CommandRegistry registry);
default void registerScheduler(Scheduler scheduler);
default void registerConfig(PluginConfig config);
default void registerPersistence(AkiAdapterContext context);
default AkiPersistentDataHolder getPersistentDataHolder();   // 可选，默认 null
default void registerText(AkiText text);
default void registerServices(ServiceRegistry registry);
```

主类写法示意：

```java
public class MyPlugin implements AkiPlugin {
    private AkiText text;
    private ServiceRegistry services;

    @Override
    public void registerText(AkiText t) { this.text = t; }
    @Override
    public void registerServices(ServiceRegistry r) { this.services = r; }

    @Override
    public PluginInfo getPluginInfo() {
        // 用 5 参构造（name, version, mainClass, author, description）；
        // 完整 9 参构造可额外传入 depend/softDepend/loadBefore/exports。
        return new PluginInfo("MyName", "1.0.0", getClass().getName(),
                              "AkiTeam", "My awesome plugin");
    }

    @Override
    public void onEnable() {
        // 在这里真正开始干活
    }
}
```

注意：PluginInfo 的完整构造器为 `(name, version, mainClass, author, description, depend, softDepend, loadBefore, exports)`，也有 `(name, version, mainClass, author, description)` 便捷构造器。name/version/mainClass 为必填，缺一抛 NullPointerException。便捷构造的 author 与 description 为默认值，前三个依赖相关字段与 exports 为空列表。

***

## 2. plugin.json 完整字段

插件 JAR 的根目录必须放一个 `plugin.json`。它由 Gson 解析，是 AkiLoader 认识插件的唯一入口。

| 字段          |  必填 | 类型        | 说明                                   |
| ----------- | :-: | --------- | ------------------------------------ |
| name        |  是  | string    | 插件名，作为唯一标识与 namespace 依据；含空格会被替换为下划线 |
| version     |  是  | string    | 版本号，如 "1.0.0"                        |
| mainClass   |  是  | string    | 插件主类全限定名，须实现 AkiPlugin               |
| author      |  否  | string    | 作者，缺省 "Unknown"                      |
| description |  否  | string    | 描述，缺省空                               |
| depend      |  否  | string\[] | 强依赖插件名列表。缺失任一项 → 本插件**不加载**          |
| softDepend  |  否  | string\[] | 软依赖列表。缺失忽略，不影响本插件                    |
| loadBefore  |  否  | string\[] | 请求在这些插件之前加载，只影响顺序                    |
| exports     |  否  | string\[] | 导出的 API 包名列表（跨插件类共享的关键，见第 4 节）       |

依赖字段参与依赖图构建，由 PluginManager 用**卡恩拓扑排序**决定加载顺序（提供者在前、depend 依赖者在后）。强依赖缺失或产生循环依赖时，该插件被跳过并记日志，不影响其他插件。

完整示例：

```json
{
  "name": "GreetingProvider",
  "version": "1.0.0",
  "mainClass": "com.example.greetingprovider.GreetingProvider",
  "author": "AkiTeam",
  "description": "Provides a GreetingService to the ecosystem.",
  "depend": [],
  "softDepend": [],
  "loadBefore": [],
  "exports": ["com.example.greetingapi"]
}
```

***

## 3. 可用能力清单（每个含最小示例）

### 3.1 事件监听

用 `@AkiEventHandler` 注解监听内建插件事件。插件主类自身会被自动注册为监听器，无需手动；若要注册其他对象作监听器，在 `registerEvents` 里调用 `bus.registerEvents(this, myListener)`。

注解属性：`priority`（EventPriority：LOWEST/LOW/NORMAL/HIGH/MONITOR，默认 NORMAL）、`ignoreCancelled`（默认 false）。

事件：目前内建 `AkiPlayerJoinEvent`（进服事件，有 `getPlayer()` 与 `getPlayerName()`）。取消语义：事件若实现 `Cancellable` 且已取消则停止传播。

```java
@AkiEventHandler
public void onJoin(AkiPlayerJoinEvent e) {
    e.getPlayer().sendSystemMessage(
        net.minecraft.network.chat.Component.literal("welcome " + e.getPlayerName()));
}
```

### 3.2 命令注册（CommandRegistry）

AkiLoader 命令基于 Brigadier，但封装成"扁平"模型：命令名 + 可选单个"贪心字符串参数"。\*\*AkiLoader 目前不建子命令树，所有子命令都要用"一个贪心参数 + 在 lambda 里按第一个单词 switch 派发"实现；Tab 补全则通过查看已输入文本动态返回候选。\*\*这是最有用的命令写法，务必掌握。

#### 3.2.1 命令源的语义

命令执行器 `CommandExecutor` 接收 `com.mojang.brigadier.context.CommandContext<CommandSourceStack>`，返回 `int`（惯例成功为 1，失败为 0 或负值）。通常需要 `import net.minecraft.network.chat.Component;` 与 `import net.minecraft.server.level.ServerPlayer;`。拿命令源与玩家、反馈：

```java
CommandSourceStack src = ctx.getSource();
ServerPlayer p = src.getPlayerOrException();   // 必须是玩家执行，否则抛异常
src.sendSuccess(() -> Component.literal("ok"), false);   // 成功；第二个参数 false = 不广播给所有人
src.sendFailure(Component.literal("no"));                // 失败，红字
```

#### 3.2.2 三种登记方式

- 字面量命令：`registry.register("name", ctx -> 1)`——无参数，不需要参数名。

- 带贪心参数：`registry.registerArgument("name", "参数名", ctx -> 1)`——接受命令名后的整段文本（含空格），执行器里用 `ctx.getArgument("参数名", String.class)` 取回。

- 带参数 + Tab 补全：`registry.registerArgumentSuggestions("name", "参数名", ctx -> 1, suggestion)`——在带贪心参数之余，用 `suggestion`（见下）返回可补全候选。

权限默认 2（需 OP）；想要其它等级，给这些方法传第 4 个参数（如 `registerArgument(name, arg, ex, 0)` 表示任何玩家）。

#### 3.2.3 最小示例（字面量 + 贪心参数 + 反馈）

```java
@Override
public void registerCommands(CommandRegistry registry) {
    registry.register("hello", ctx -> {
        ctx.getSource().sendSuccess(
            () -> Component.literal("Hello from AkiLoader!"), false);
        return 1;
    });
    registry.registerArgument("echo", "msg", ctx -> {
        String msg = ctx.getArgument("msg", String.class);
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    });
}
```

#### 3.2.4 子命令 + Tab 补全（推荐做法）

因为不建命令树，子命令通过判断输入的第一个单词派发；补全通过 `remaining`（命令名后玩家已输入的部分）返回候选。要点：

- `ctx.getArgument("args", String.class)` 拿整段输入（可能为空串）。

- 用 `remaining.trim()` 判断用户已输入到第几个 token，从而决定返回哪一级候选——比如 `/papi `                  按 Tab 出 `list/register`，`/papi register `                  再接 Tab 出占位符名。

```java
@Override
public void registerCommands(CommandRegistry registry) {
    registry.registerArgumentSuggestions("papi", "args", ctx -> {
        String raw = ctx.getArgument("args", String.class);
        switch (firstToken(raw)) {
            case "list" -> {
                send(ctx, listExpansions());         // 你自己写的 helper
            }
            default -> ctx.getSource().sendSuccess(() -> Component.literal(
                "Usage: /papi list | /papi register <placeholder>"), false);
        }
        return 1;
    }, (cmdCtx, remaining) -> {
        String t = remaining.trim();
        String first = firstToken(t);
        if (first.equals("register")) {
            return List.of("player_name", "world", "online");               // 第二级：占位符名
        }
        if (!first.isEmpty()) {
            return List.of();                                               // 已是具体子命令，无补全
        }
        return List.of("list", "register");                                 // 第一级：子命令名
    });
}

private static String firstToken(String s) {
    if (s == null) return "";
    s = s.trim();
    int sp = s.indexOf(' ');
    return sp < 0 ? s : s.substring(0, sp);
}
```

说明：

- `remaining` 就是命令名之后玩家已输入的部分；根据它的内容返回哪一级候选即可，返回空列表表示无补全。

- 想补全"在线玩家名"：`cmdCtx.getSource().getServer().getPlayerNames()` 返回 `String[]`，用 `java.util.Arrays.asList(...)` 包成列表返回。

- 补全与执行是两回事：补全只负责"按 Tab 弹候选"，真正执行仍由执行器里的 switch 完成。

- `suggestion` 是函数式接口 `com.akiteam.akiloader.command.AkiCommandSuggestion`，签名 `List<String> suggest(CommandContext<CommandSourceStack> ctx, String remainingInput)`。

#### 3.2.5 命名与冲突

命令名登记时不带开头 `/`（带了也会被剥离）。同名登记会被拒绝并告警；与 vanilla 或其他插件命令重名时，真正注册阶段会跳过以保护他人命令，日志打印 `Command '/x' already exists; skipped.`。

#### 3.2.6 重载后命令重新生效

`/akiloader reload` 会先清除所有插件命令节点，再重新注册，因此新打包的命令无需重启游戏即生效。

### 3.3 调度器

在 `registerScheduler(Scheduler)` 里保存 `Scheduler`；任务归属本插件，插件卸载/重载时自动取消。

- `scheduler.runTask(plugin, r)`：下一游戏刻主线程执行一次

- `scheduler.runTaskLater(plugin, r, delayTicks)`：延迟若干刻执行

- `scheduler.runTaskTimer(plugin, r, delayTicks, periodTicks)`：周期重复

- `scheduler.runTaskAsynchronously(plugin, r)`：后台线程执行（勿碰游戏对象）

```java
private Scheduler scheduler;
@Override public void registerScheduler(Scheduler s) { this.scheduler = s; }
@Override public void onEnable() {
    scheduler.runTaskLater(this, () -> LOGGER.info("5 ticks later"), 5);
    scheduler.runTaskTimer(this, () -> { /* 每 20 刻一次 */ }, 0, 20);
}
```

### 3.4 配置文件

在 `registerConfig(PluginConfig)` 里保存。对应磁盘 `run/akiloader/<插件名>/config.yml`；文件不存在时从插件 JAR 根目录的默认 `config.yml` 复制。

API：`reload()`、`save()`、`getString/getInt/getBoolean/getDouble/getList/getStringList(path)`、`set(path, value)`、`contains(path)`、`getConfigurationSection(path)` 等。支持点号嵌套键（如 "database.host"）。

```java
private PluginConfig config;
@Override public void registerConfig(PluginConfig c) { this.config = c; }
@Override public void onEnable() {
    int max = config.getInt("settings.maxPlayers", 20);  // 带默认值
    config.set("settings.maxPlayers", 30);
    config.save();
}
```

### 3.5 PDC（持久化数据容器）

在实体、物品、区块等对象上存取自定义数据，数据随存档持久化。

键：`AkiKey`。`AkiKey.of("namespace","key")` 或用插件名作命名空间 `AkiKey.of(plugin, "key")`；只允许小写字母/数字/`_-.` `/`，总长≤32768，序列化为 `namespace:key`。命名空间是隔离手段——不同插件即使 key 相同也不冲突。

类型：`BuiltInDataTypes.STRING / INTEGER / LONG / DOUBLE / BOOLEAN / UUID_TYPE / BYTE_ARRAY / INTEGER_ARRAY / TAG_CONTAINER`，以及 `BuiltInDataTypes.listOf(元素类型)` 生成列表类型。

容器：`AkiPersistentContainer`，方法 `set(key,type,value)`、`get(key,type)`、`getOrDefault`、`has`、`remove`、`getKeys`、`isEmpty`、`getSize`、`copyTo`、`toNbt()`、`fromNbt()`。

实体读写（写回实体需手动 set）：

```java
AkiPersistentContainer c = EntityPersistentDataBridge.get(player);
c.set(AkiKey.of(this, "kills"), BuiltInDataTypes.INTEGER, 42);
c.set(AkiKey.of(this, "home"), BuiltInDataTypes.TAG_CONTAINER,
      subContainer);                       // 嵌套容器
Integer kills = c.get(AkiKey.of(this, "kills"), BuiltInDataTypes.INTEGER);
EntityPersistentDataBridge.set(player, c); // 写回才生效
```

物品读写：`ItemPersistentDataBridge.get(ItemStack)` / `set(ItemStack, container)`，底层用 `DataComponents.CUSTOM_DATA`，随物品存档。

### 3.6 文本组件 TextComponent + AkiText

自研 Builder 包装 Minecraft 原生 `Component`，无第三方依赖。在 `registerText(AkiText)` 里保存 `AkiText.INSTANCE` 交给插件。

```java
TextComponent.text("Hello")
    .color(NamedTextColor.GOLD)            // 具名颜色（NamedTextColor.RED/BLUE/...）
    .color(0xffaa00)                       // 或 RGB
    .bold(true)
    .italic(true)
    .underlined(true)
    .clickEvent(ClickEvent.runCommand("/home"))          // 点击执行命令
    .clickEvent(ClickEvent.copyToClipboard("copy me"))   // 点击复制
    .clickEvent(ClickEvent.openUrl("https://..."))        // 打开链接
    .hoverEvent(HoverEvent.showText(TextComponent.text("提示")))  // 悬停文本
    .append(TextComponent.text("more"))                  // 拼接

TextComponent.empty();
TextComponent.newline();
```

发送：

```java
text.sendMessage(player, tc);              // 发给玩家
text.sendMessage(ctx.getSource(), tc);     // 发给命令源
text.broadcast(tc);                        // 全服广播
text.parseMiniMessage("<red>hi</red> <bold>bold</bold> <#ffaa00>hex</#ffaa00>"); // 轻量 MiniMessage
```

HoverEvent 还可显示物品/实体：`HoverEvent.showItem(ItemStack)`、`HoverEvent.showEntity(Entity)`。

### 3.7 服务注册 ServiceRegistry（重点）

服务是插件之间互相调 API 的机制（参考 Paper 的 ServicesManager）。在 `registerServices(ServiceRegistry)` 里保存。

注册服务（在 onEnable 中做）：`registry.register(接口.class, 实现实例, this, ServicePriority.Normal)`
获取服务（在 onEnable 中做）：`registry.get(接口.class)`——返回当前优先级最高、同优先级先注册者；**不存在返回 null，不抛异常**。

优先级枚举：`ServicePriority.Lowest / Low / Normal / High / Highest`。

高级查询：

```java
List<RegisteredServiceProvider<Economy>> ps = registry.getRegistrations(Economy.class);
var one = ps.get(0).getProvider();   // 最优先提供者
serviceRecord.getPlugin();           // 提供者所属插件
serviceRecord.getPriority();         // 提供者声明的优先级
```

插件卸载/重载时，AkiLoader 自动 `unregisterAll(plugin)`，无需手动清理。

***

## 4. 跨插件 API 调用（核心）

这是本系统最关键、也是 PAPI 移植最依赖的部分。核心机制有两根支柱：

### 4.1 类共享：exports + 父优先委派

问题：默认每个插件用独立 ClassLoader 加载自己的类。如果消费者把"服务接口"也打进自己 jar，AkiLoader 父加载器（AkiLoader 自身）里又没有这个接口，那么提供者和消费者各自加载出**两份不同的 Class 对象**。而 ServiceRegistry 以 `Class` 为 key，消费者用自己那份 Class 去查提供者注册的服务，永远查不到（这是最常见的移植坑，务必注意）。

解决办法：提供者把 API 接口所在的**包**声明到 `exports`，AkiLoader 记录"包名 → 提供者 ClassLoader"的导出索引。之后任何插件（包括消费者）要加载该包下的类时，会被**父优先委派**给提供者加载器，从而保证同一个包只产生一份 Class。

规则：

- standard library（java.*/javax.*）永远父优先，委托给 JVM。

- 命中的导出包 → 委托给导出它的插件加载器。

- 其余包 → 仍走子优先（本插件 jar 优先），保持隔离。

- 多个插件导出同一包：先加载者生效，后者打印警告并忽略。

- 老插件不写 exports → 空列表，行为不变（完全隔离）。

> **关键约束**：`"exports": ["com.example.greetingapi"]` 必须和 API 接口所在的包完全一致。如果接口在 `com.example.greetingapi.GreetingService`，那 exports 就是 `"com.example.greetingapi"`——只写到包一级，而不是类；写错包名（如漏字母、只写 `com.example`）会导致委派不命中，消费者仍拿到两份 Class，服务获取失败。

### 4.2 顺序保证：depend + 拓扑加载

- 消费者在 `plugin.json` 声明 `"depend": ["提供者名"]`，拓扑排序保证提供者先 onEnable 注册服务，消费者后 onEnable 获取。

- 服务注册与获取都放在 onEnable 中。

### 4.3 完整示例：GreetProvider + ShopConsumer

先定义共享 API 包 `com.example.greetingapi`，里面放接口（该包由提供者导出，消费者不需要打包它）：

```java
// 文件：com/example/greetingapi/GreetingService.java （放在 GreetingProvider 项目里）
package com.example.greetingapi;

import net.minecraft.server.level.ServerPlayer;

public interface GreetingService {
    String greet(ServerPlayer player);
}
```

> **消费者编译时如何拿到 API 接口类**：消费者（ShopConsumer）编译时需要 API 接口的 class 或源文件，但不要把它打包进最终的 JAR。两种常见做法：
>
> 1. 编译时用 `-cp` 指向提供者 JAR 或 API 模块的 classpath 目录，让 javac 能索引到 `GreetingService` 的 class；打包时只打包消费者自己的类（不含该接口）。
> 2. 把 API 接口放到一个单独的共享源码目录，编译时引用、打包时不复制进消费者 JAR。
>    无论哪种，**接口类只出现在提供者 JAR 里**，由 exports 负责让消费者共享到同一份 Class。

提供者插件 GreetingProvider（导出 API 包 + 注册服务）：

```json
{
  "name": "GreetingProvider",
  "version": "1.0.0",
  "mainClass": "com.example.greetprovider.GreetProvider",
  "author": "AkiTeam",
  "description": "Provides a GreetingService to the plugin ecosystem.",
  "exports": ["com.example.greetingapi"]
}
```

```java
// com/example/greetprovider/GreetingServiceImpl.java
package com.example.greetprovider;

import com.example.greetingapi.GreetingService;
import net.minecraft.server.level.ServerPlayer;

public class GreetingServiceImpl implements GreetingService {
    @Override public String greet(ServerPlayer p) {
        return "Hello, " + p.getGameProfile().getName() + "! (from GreetingService)";
    }
}
```

```java
// com/example/greetprovider/GreetProvider.java
package com.example.greetprovider;

import com.akiteam.akiloader.api.AkiPlugin;
import com.akiteam.akiloader.api.PluginInfo;
import com.akiteam.akiloader.service.ServiceRegistry;
import com.akiteam.akiloader.service.ServicePriority;
import com.example.greetingapi.GreetingService;

public class GreetProvider implements AkiPlugin {
    private ServiceRegistry registry;

    @Override public void registerServices(ServiceRegistry r) { this.registry = r; }

    @Override public PluginInfo getPluginInfo() {
        return new PluginInfo("GreetingProvider", "1.0.0", getClass().getName(),
                "AkiTeam", "Provides a GreetingService.");
    }

    @Override public void onEnable() {
        registry.register(GreetingService.class, new GreetingServiceImpl(),
                          this, ServicePriority.Normal);
        System.out.println("[GreetProvider] GreetingService registered.");
    }

    @Override public void onLoad() {}
    @Override public void onDisable() {}
}
```

消费者插件 ShopConsumer（声明 depend + 获取服务 + 命令）：

```json
{
  "name": "ShopConsumer",
  "version": "1.0.0",
  "mainClass": "com.example.shopconsumer.ShopConsumer",
  "depend": ["GreetingProvider"]
}
```

```java
// com/example/shopconsumer/ShopConsumer.java
// 注意：com.example.greetingapi 这个接口类 NOT 被 ShopConsumer 打包，
// 它由 AkiLoader 通过 exports 委派到 GreetingProvider 的加载器加载。
package com.example.shopconsumer;

import com.akiteam.akiloader.api.AkiPlugin;
import com.akiteam.akiloader.api.PluginInfo;
import com.akiteam.akiloader.command.CommandRegistry;
import com.akiteam.akiloader.service.ServiceRegistry;
import com.akiteam.akiloader.text.AkiText;
import com.akiteam.akiloader.text.TextComponent;
import com.akiteam.akiloader.text.NamedTextColor;
import com.example.greetingapi.GreetingService;
import net.minecraft.network.chat.Component;

public class ShopConsumer implements AkiPlugin {
    private ServiceRegistry services;
    private AkiText text;
    private GreetingService greeting;   // 从服务表拿到

    @Override public void registerServices(ServiceRegistry r) { this.services = r; }
    @Override public void registerText(AkiText t) { this.text = t; }

    @Override public PluginInfo getPluginInfo() {
        return new PluginInfo("ShopConsumer", "1.0.0", getClass().getName(),
                              "AkiTeam", "Cross-plugin service consumer.");
    }

    @Override public void onEnable() {
        greeting = services.get(GreetingService.class);   // null 表示无提供者
        if (greeting == null) {
            System.out.println("[ShopConsumer] GreetingService NOT available!");
        } else {
            System.out.println("[ShopConsumer] Acquired GreetingService. " + greeting.greet(null));
        }
    }

    @Override public void registerCommands(CommandRegistry registry) {
        registry.register("greet", ctx -> {
            if (greeting == null) {
                ctx.getSource().sendFailure(Component.literal("GreetingService unavailable."));
                return 0;
            }
            var player = ctx.getSource().getPlayerOrException();
            text.sendMessage(player,
                TextComponent.text(greeting.greet(player)).color(NamedTextColor.GREEN));
            return 1;
        });
    }

    @Override public void onLoad() {}
    @Override public void onDisable() {}
}
```

验证要点：启动后加载日志应出现

- "GreetingProvider exports API package 'com.example.greetingapi'."

- "\[GreetProvider] GreetingService registered."

- "\[ShopConsumer] Acquired GreetingService."（而不是 NOT available）
  游戏内 `/greet` 应返回绿色富文本。

***

## 5. 插件项目结构、编译与打包

### 5.1 推荐项目结构

提供一个示例插件（如 ShopConsumer），建议在 AkiLoader 仓库旁的独立目录组织源码：

```text
shopconsumer/
  src/
    main/
      java/
        com/example/shopconsumer/ShopConsumer.java
      resources/
        plugin.json
  build.bat            （或 .sh，见下）
  out/                  （javac 输出）
  ShopConsumer.jar      （打包产物，拷到 run/akiloader/）
```

要点：

- plugin.json 放 `src/main/resources/`，打包时须位于 JAR 根目录。

- API 类的全限定名必须和 plugin.json 的 mainClass / exports 完全对应。

### 5.2 编译命令

编译需要 classpath 包含 AkiLoader 主类、Minecraft 与 NeoForge 的类。**建议把 classpath 写成一个固定的文件列表**，或用 Gradle 的 compileClasspath 输出（`gradlew dependencies` 或让脚本 Dump 出 classpath）。最简方案：把 AkiLoader 主类 output + NeoForge merged jar + Brigadier jar 加入 `-cp`。

一个可直接用的 Windows 批处理示例（AkiLoader 输出 + 用 `dir /s` 收集其 libs jar，失败容忍）：

```bat
@echo off
set ROOT=E:\mc\JavaLearn\AkiLoader\MDK-1.21.1-ModDevGradle-main
rem 最简：AkiLoader 主类 output
set CP=%ROOT%\build\classes\java\main
rem 再追加 NeoForge merged jar 与 Brigadier jar（按实际路径补充）
set CP=%CP%;%ROOT%\build\moddev\artifacts\neoforge-21.1.248.jar
for /f %%j in ('dir /s /b "%ROOT%\build\libs\*.jar" 2^>nul') do set "CP=!CP!;%%j"
mkdir out 2>nul
javac -encoding UTF-8 -cp "%CP%" -d out ^
    src\main\java\com\example\shopconsumer\ShopConsumer.java
```

> 若把 API 接口也编译需要，临时把它的源/class 目录加进 `-cp`（如指向 GreetingProvider 的 out 目录），但打包时不要包含它。

### 5.3 打包命令

必须让 `plugin.json` 位于 JAR 根目录。用命令行 jar 工具并显式指定：

```bat
mkdir package 2>nul
copy /y src\main\resources\plugin.json package\plugin.json
xcopy /y out\com package\com /s /e >nul
cd package
jar cf ..\ShopConsumer.jar plugin.json com
cd ..
copy /y ShopConsumer.jar %ROOT%\run\akiloader\ShopConsumer.jar
```

### 5.4 重启 / 热重载

- 启动：`gradlew runClient`，进入世界后插件即生效。

- 热重载（改代码重打包后，无需重启游戏）：OP 权限下在聊天框执行 `/akiloader reload`，观察 `enabled ... failed` 统计。

- 若加新命令，重载后命令会重新注册生效。

***

## 6. 诊断与排错

AkiLoader 自带一组 OP 命令（权限等级 2），强烈建议开发时使用：

```text
/akiloader list                    列出插件及状态/服务数/监听器数/任务数
/akiloader info <插件名>            单插件完整诊断（含 exports、注册的服务），Tab 可补全插件名
/akiloader dump                    把完整报告导出为 run/akiloader/dump/dump-<时间戳>.json
/akiloader reload                  热重载全部插件
```

**依赖解析失败排查**：如果插件因依赖缺失没加载，`/akiloader list` 不会显示它，`/akiloader info` 也查不到。这时要去看**日志**里 `Could not load plugin ... missing required dependency`（强依赖缺失）或 `circular dependency or unsatisfiable dependency chain`（循环依赖）的报错，据此补全 `depend` 或调整依赖声明。

***

## 7. 写给 PAPI 移植者的映射要点

PlaceholderAPI 移植到 AkiLoader 时的对应关系：

- PAPI 是一个提供占位符服务、并让其他插件注册/使用占位符的插件 → 本质就是 AkiLoader 的 ServiceRegistry 上的一个服务。

- 定义你的"接口"（如 `PlaceholderService`：`String onPlaceholder(String id, ServerPlayer p, String args)`），放在一个**由有 exports 的插件导出的 API 包**里，其余插件声明 `depend: ["你的插件"]` 后 `registry.get(PlaceholderService.class)` 取用。

- 消费者插件不要自己打包这个接口类，否则会因类加载器隔离导致 `get(...)` 永远返回 null——这是移植最容易踩的坑。

- PAPI 的指令（如 /papi set、/papi parse）就是 `registerCommands` / `registerArgument` 里登记的命令；文本反馈用 `AkiText` 做出彩色/可点击效果。

- PAPI 可为玩家/物品持久化数据 → 用 `EntityPersistentDataBridge` / `ItemPersistentDataBridge` + `AkiKey`。

- 每服务器/每玩家配置 → `PluginConfig`（路径 `run/akiloader/<插件名>/config.yml`）。

### 7.1 PAPI 移植代码骨架

```java
// 服务接口（放在导出包里，如 com.example.papi）
package com.example.papi;

import net.minecraft.server.level.ServerPlayer;

public interface PlaceholderService {
    String onPlaceholder(ServerPlayer player, String identifier, String params);
}
```

```java
// 服务实现（提供者插件内）
package com.example.papiprovider;

public class PlaceholderServiceImpl implements PlaceholderService {
    @Override public String onPlaceholder(ServerPlayer player, String id, String params) {
        if ("player_name".equals(id)) return player.getName().getString();
        if ("world".equals(id)) return player.level().dimension().location().toString();
        return null;   // 未识别的占位符返回 null（表示没匹配到）
    }
}
```

```java
// 提供者插件 onEnable 中注册（exports 导出 com.example.papi）
registry.register(PlaceholderService.class, new PlaceholderServiceImpl(), this, ServicePriority.Normal);
```

```java
// 消费者插件 onEnable 中获取并使用（plugin.json 声明 depend: ["PapiProvider"]）
PlaceholderService papi = services.get(PlaceholderService.class);
if (papi != null) {
    String name = papi.onPlaceholder(player, "player_name", null);
    // name 即玩家名
}
```

#### 7.1.1 给 `/papi` 群命令补 Tab 补全

PAPI 的 `/papi` 是一组子命令，用 `registerArgumentSuggestions` 给子命令/占位符做补全（详见 §3.2.4）：

```java
registry.registerArgumentSuggestions("papi", "args",
    ctx -> { /* 内部按 firstToken 派发 list/parse/...，见 §3.2.4 */ },
    (cmdCtx, remaining) -> {
        String t = remaining.trim();
        String first = t.indexOf(' ') < 0 ? t : t.substring(0, t.indexOf(' '));
        if (first.equals("parse")) {
            String[] players = cmdCtx.getSource().getServer().getPlayerNames();
            return java.util.Arrays.asList(players);        // 第二级：在线玩家名
        }
        if (first.isEmpty()) {
            return List.of("list", "parse", "reload");      // 第一级：子命令
        }
        return List.of();                                    // 已在子命令内部，无可补全
    });
```

这样玩家输入 `Tab /papi `                会弹出子命令，`/papi parse `                再接 Tab 会补全在线玩家名，无需手动键入。

***

## 8. 快速起步清单

1. 写 `plugin.json`（name/version/mainClass 必填；如提供服务加上 `exports` 与 `depend`）。
2. 写主类实现 `AkiPlugin`，覆盖需要的 `registerXXX` 钩子并保存能力对象。
3. 在 onEnable 中注册服务 / 获取服务、注册命令、启动任务。
4. 编译（classpath 含 AkiLoader build/classes + game/neoforge 类）、打包（plugin.json 在 JAR 根）。
5. 放入 `run/akiloader/`，`gradlew runClient` 进世界；用 `/akiloader list` + `/akiloader info <名>` 核对，或 `/akiloader dump` 导出报告排查。
6. 改代码后重打包，`/akiloader reload` 热加载。

