# 原版 26.1.2 — 创建链 + 装备同步 源码核实（2026-09-11，backpack 项目实战）

> 全部从 loom-cache sources jar 逐行核实（读法见 §5）。用于：「出生级 vs init 级」创建位次、客户端为什么知道穿没穿、渲染该从 menu 取数。

## §1 客户端「创建」链 — menu 与 screen 的出生位次

**有包容器（箱子/熔炉…）**
- 服务端 `ServerPlayer.openMenu(MenuProvider)`（:1327）：若 `containerMenu != inventoryMenu` 先 `closeContainer()`（:1332）→ `nextContainerCounter()`（新 containerId）→ `provider.createMenu(...)`（:1336，原版 menu 出生）→ `connection.send(new ClientboundOpenScreenPacket(...))`（:1344，只装 containerId / MenuType / 标题）→ `initMenu(menu)` + `containerMenu = menu`。
  - 「关旧」链（`closeContainer:1402-1405`）：**发 `ClientboundContainerClosePacket`**（:1403）+ `doCloseContainer`（:1408-1412：旧 menu `removed` → `transferState` → 引用回 `inventoryMenu`）。
- 客户端 `ClientPacketListener.handleOpenScreen`（:1369-1372，就一行）→ `MenuScreens.create(type, mc, containerId, title)`——**`MenuScreens.create` 的全源码唯一调用者**（grep 全 clientOnly 核实）。
- `MenuScreens.ScreenConstructor.fromPacket`（全部就三行）：
  ```java
  U screen = this.create((T)type.create(containerId, minecraft.player.getInventory()), minecraft.player.getInventory(), title); // 先 new menu 再造 screen
  minecraft.player.containerMenu = screen.getMenu();
  minecraft.setScreen(screen);
  ```
  ⇒ **menu 出生早于 screen；两者是同一条出生链**（`getMenu()` 依据：`AbstractContainerScreen implements MenuAccess<T>` :30，menu 存 `protected final T menu` :47，构造器 :78-83 赋值，`getMenu()` :677-679）。
- **客户端的两个「关」都不在 openScreen 链里**：① 服务端先关旧容器时，客户端收的是**另一个包**（`ClientboundContainerClosePacket`）→ `handleContainerClose`（:1480-1483）→ `player.clientSideCloseContainer()`（`LocalPlayer:338-341` = `super.closeContainer()` + **`setScreen(null)`**）；② 兜底：任何 `Minecraft.setScreen`（:1166）先 `旧 screen.removed()`（:1171-1172）→ 赋新 → `added()`（:1196-1198）→ `init(w,h)`（:1203）。

**按 E 背包界面（本地打开，无包）**
- `InventoryScreen` 构造器第一行就 `super(player.inventoryMenu, ...)` — menu 是玩家常驻的 `inventoryMenu`；打开点在 `Minecraft.setScreen(new InventoryScreen(this.player))`。

**`Minecraft.setScreen` 顺序**
- `this.screen = screen; screen.added(); … screen.init(w, h)` — init 在安装时调一次。

**`Screen.init` 26.1 语义**
- 每个 screen 安装只跑一次；窗口缩放 = `Screen.resize(w,h)` → `repositionElements()`（`Screen.java:487-491`）。⚠️ **修正 2026-09-24**：`repositionElements()` 的默认实现就是 `rebuildWidgets()` = `clearWidgets() + clearFocus() + init() + setInitialFocus()`（`Screen.java:383-388 / 483-485`）⇒ 子类钩子 `init()` **会**被重跑（“不重跑 init”只对 `init(int,int)` 入口成立，它走 `else { repositionElements(); }`，`Screen.java:364-372`）。想不重建必须**覆写 `repositionElements()`**；我们的件持 menu，正是必须覆写的那一类（只挪位置）。
- `AbstractContainerScreen.leftPos/topPos` 在 `init()` 才赋值（构造器不赋）。⇒ "要坐标的布置类对象"属 init 级。

## §2 创建位次阶梯（用来对账「同等级创建」）

| 级 | 成员 | 备注 |
|---|---|---|
| 出生级 | 原版 menu（先）+ screen（后） | 同一个出生动作 |
| init 级 | **backpack 客户端 menu 现址**（`AbstractContainerScreenMixin.init` TAIL） | screen 成形后被补建 |
| 渲染 / tick 级 | （创建放这 = 更晚且在循环里） | 不做 |

- 服务端我们已在出生级（`ServerPlayerMixin` @ `openMenu`；原版在同一方法里 `createMenu`）。
- 用户目标（2026-09-11 原话："我说的是同等级创建，而不是等原版menu创建完了，然后screen也渲染完了，我们的menu才进行创建"）：客户端创建点提到**出生级**——"我们的 menu 要跟 screen 在同一个出生动作里出现"。

## §3 装备状态在客户端的可见性（"不发包客户端怎么知道穿没穿"）

- **状态本来就是客户端本地镜像**（原版常驻同步，不是我们发包）：
  - 自己物品栏槽位推送：`ClientboundSetPlayerInventoryPacket` ← `Inventory.createInventoryUpdatePacket(slot)`（原版 `placeItemBackInInventory` 就这么推；backpack 的 `BackpackDataHelper.updateBackpack` 也用它推 CHEST）。
  - 客户端应用：`handleSetPlayerInventory` → `player.getInventory().setItem(slot, contents)`。
  - 容器会话内：`handleContainerSetSlot` containerId==0 → `player.inventoryMenu.setItem(...)`。
  - 实体装备：`handleSetEquipment` → `livingEntity.setItemSlot(...)`。
- **`onEquipItem` 客户端也会跑**（触发路径）：
  - `LivingEntity.setItemSlot` → `this.onEquipItem(slot, equipment.set(...), stack)` — 过 setItemSlot 的路（含 `handleSetEquipment`）都触发。
  - `ArmorSlot.setByPlayer`（InventoryMenu 的 4 个装备槽用的 Slot 子类）直接 `owner.onEquipItem(...)` — 玩家在物品栏里换装备时**两端都跑**。
  - `onEquipItem` 本体只对音效/gameEvent 做 `!isClientSide` 判断；**方法本身两端都会被调**（mixin TAIL 双端吃到）。
- ⚠️ **不是每条同步路都触发**：`Inventory.setItem` 写装备槽走 `this.equipment.set(...)`，而 `EntityEquipment` 是普通 EnumMap（`set` = 裸 put，无回调）；`handleSetPlayerInventory` / `handleContainerSetSlot` 应用同步时不触发 onEquipItem。⇒ **"读状态"（`hasShelf`）永远是准的；事件信号只当顺手用。**

## §4 渲染配对：原版 screen 从 menu 取数（证据）

`AbstractContainerScreen` 里：`for (Slot slot : this.menu.slots)`（画格/判 hover）、`this.menu.getCarried()`、点击 `handleContainerInput(this.menu.containerId, ...)`、`this.menu.removed(...)`。⇒ "原版 menu 被渲染" = 屏幕把 menu 当唯一取数源。

backpack 现状（2026-09-11 grep 全工程核实）：`getBackpackMenu()` 只有 Mixin 自己在存 + `PanelWidget` 构造时接了一把；**无一处渲染读 `menu.slots`、无一处点击调 `menu.clicked`**——渲染链直读 `BackpackDataHelper`，交互走 payload。渲染配对待定（创建级定完再对账）。

## §5 查源码方法（2026-09-11 实测最顺路径）

```bash
cd /d/code/backpack
C=.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-52430b475d/26.1.2/minecraft-common-52430b475d-26.1.2-sources.jar
CL=.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-52430b475d/26.1.2/minecraft-clientOnly-52430b475d-26.1.2-sources.jar
mkdir -p /tmp/mc26-all
unzip -o -q "$C" -d /tmp/mc26-all && unzip -o -q "$CL" -d /tmp/mc26-all
grep -rn "onEquipItem(" /tmp/mc26-all --include="*.java" | head
sed -n '1369,1395p' /tmp/mc26-all/net/minecraft/client/multiplayer/ClientPacketListener.java
```

要点：
- **全量解包比 mc_sources（只覆盖部分包）+ javap 都顺**；**common sources 含服务端类**（`net/minecraft/server/level/ServerPlayer.java` 在 common jar 里），clientOnly sources 装客户端类。
- hash 目录名（`...-52430b475d`）随项目 loom 配置变：先 `ls .gradle/loom-cache/minecraftMaven/net/minecraft/` 看实际名。
- `grep -n` 拿行号 → **字面数字** `sed -n 'a,bp'`（命令替换嵌套会被 Hermes 拦）。
- **查「谁调用了 X」（"全源码唯一调用者"的举证方式）→ 解包整册 + `grep -rn`**：`unzip -oq <sources.jar> -d <scratch>` 全解一次，再 `grep -rn "X(" <scratch> --include="*.java"`。`zipgrep` / `unzip -p "<jar>" "*"` 这类整册扫描实测都不产出（前者卡死、后者空结果）——整册搜索就走解包这条。实例：`MenuScreens.create` 全 clientOnly 唯一调用者 = `ClientPacketListener:1371`。解包目录放 Hermes scratch（`/d/AI_Agent/hermes/cache/scratch/`）或 `$TMPDIR`，用完即删。
- 重建目录时 `rm -rf /tmp/mc26-all` 会弹"删除根路径"审批；换个新目录名可绕过。
- **一次只抽要查的文件**（查几个类时比全量解包快得多，也不会把 scratch 塞满）：`unzip -o -q -j "<sources.jar>" "net/minecraft/world/inventory/AbstractContainerMenu.java" -d "$TMPDIR/mcsrc" && grep -n -B4 -A10 "关键词" "$TMPDIR/mcsrc/AbstractContainerMenu.java"`。Windows 上临时目录用 `$TMPDIR`，别写裸 `/tmp`。
- **javap 只在要看签名/字节码时用**，直接用 PATH 上那份（就是 JDK 25）：`javap -p -cp "D:/IDE/dev-environment/gradle/caches/fabric-loom/26.1.2/minecraft-client.jar;D:/IDE/dev-environment/gradle/caches/fabric-loom/26.1.2/minecraft-common.jar" <全类名>`。三个坑：① classpath 必须写 **Windows 原生路径**（`C:/…` 才认，MSYS 的 `/c/…` 报"找不到类"）② 用 `;` 分隔 ③ 服务端类在 `minecraft-extracted_server.jar`。**本机 `GRADLE_USER_HOME=D:/IDE/dev-environment/gradle`**（loom 缓存随它在 D 盘），`C:/Users/ex/.gradle/caches/fabric-loom` 是空的；旧笔记里的 `/d/应用/java运行环境/.../javap.exe` 在本机不存在，别再照抄。

## §6 本日核实的文件索引（sources jar 内路径）

- `net/minecraft/client/gui/screens/MenuScreens.java`（create / fromPacket）
- `net/minecraft/client/Minecraft.java`（setScreen ~1166-1210；E 打开点 ~2066）
- `net/minecraft/client/gui/screens/Screen.java`（init / resize / added）
- `net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.java`（构造器/init/leftPos ~57-100；menu.slots 循环多处）
- `net/minecraft/client/gui/screens/inventory/InventoryScreen.java`（构造器 ~27）
- `net/minecraft/client/multiplayer/ClientPacketListener.java`（handleOpenScreen 1369；handleContainerSetSlot 1374；handleSetPlayerInventory 1414；handleSetEquipment 1473）
- `net/minecraft/world/entity/LivingEntity.java`（onEquipItem 682；setItemSlot 2253）
- `net/minecraft/world/entity/player/Inventory.java`（setItem 376；createInventoryUpdatePacket 325）
- `net/minecraft/world/entity/EntityEquipment.java`（set = 裸 put）
- `net/minecraft/world/inventory/ArmorSlot.java`（setByPlayer → onEquipItem ~34）
- `net/minecraft/world/inventory/InventoryMenu.java`（ArmorSlot 注册 59）
- `net/minecraft/world/entity/player/Player.java`（构造器 `:181-182` 两引用同一实例；`closeContainer:422`；`hasContainerOpen:1290`；`removed:1231`）[common jar]
- `net/minecraft/world/inventory/ChestMenu.java`（`:58` `addStandardInventorySlots` 玩家背包槽进箱子 menu；`:64` 箱体槽）[common jar]
- `net/minecraft/server/level/ServerPlayer.java`（openMenu :1327 起；closeContainer :1402-1405；doCloseContainer :1408-1412）[common jar]

**续核（2026-09-24，查「窗口缩放/关屏会不会重跑钩子」时用的类）**：
- `net/minecraft/client/Minecraft.java`：`resizeGui` `:1533-1541`（内含 `screen.resize(...)` `:1536-1538`）；`init(w,h)` 在整个 `Minecraft` 里的**唯一**调用点 = `setScreen:1203`（`:1195` 赋引用 → `:1197-1198` `added()` → `:1203` `init`）。
- `net/minecraft/client/Window.java`：GLFW 回调注册 `:113`（framebuffer）/`:115`（window size）；`onFramebufferResize:266-292` 内 `:278` 调 `this.eventHandler.resizeGui()`；另有 `changeFullscreenVideoMode:345`、`updateFullscreen:403` 同样调。⇒ resize → 房东 `init()` 重跑这条链闭合。
- `net/minecraft/client/gui/ComponentPath.java`：`applyFocus(false)` `:33-48`——`Path` 那层先 `component.setFocused(null)`，叶子层 `setFocused(false)`；而 `AbstractContainerEventHandler.setFocused(null)` 本来就会对旧 focused 调 `setFocused(false)` ⇒ **`clearFocus()` 与直接 `setFocused(null)` 等效**。
- `net/minecraft/world/inventory/AbstractContainerMenu.java`：`removed` `:596-604`（carried 收尾整段包在 `instanceof ServerPlayer` 内）、`transferState` `:793-807`。
- `net/minecraft/server/network/ServerGamePacketListenerImpl.java`：`handleContainerClose` `:1884-1887` → `player.doCloseContainer()`。
- `net/minecraft/server/level/ServerPlayer.java`：`tick` `:602-605`（`!containerMenu.stillValid` → `closeContainer()`）、`doCloseContainer` `:1408-1412`（`menu.removed(this)` → `transferState` → 换引用）。

**续核（创建/关闭链对账）**：
- `net/minecraft/client/multiplayer/ClientPacketListener.java`：`handleOpenScreen` `:1369-1372`（其中 `:1371` = `MenuScreens.create` 唯一调用点）；`handleContainerClose` `:1480-1483` → `player.clientSideCloseContainer()`。
- `net/minecraft/client/player/LocalPlayer.java`：`closeContainer` `:333-336`（发 `ServerboundContainerClosePacket`）；`clientSideCloseContainer` `:338-341`（超类收尾 + **`setScreen(null)`**）。
- `net/minecraft/network/protocol/game/ClientboundOpenScreenPacket.java`（common jar）：3 个 final 字段（containerId / MenuType / Component）+ `STREAM_CODEC` composite——「开屏」载荷就这么点。
- `net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.java`：`implements MenuAccess<T>` `:30`、`menu` 字段 `:47`、构造器 `:78-83`、`getMenu()` `:677-679`。

## §7 `InventoryMenu` 常驻 vs 其它 ACM 子类开时才建

`AbstractContainerMenu` 只是基类，没有一份 JVM 启动就有的全局实例。

- **`InventoryMenu`**：`Player` 构造里 `new`（`Player.java:181`），`:182` `containerMenu = inventoryMenu` —— **出生时一份实例、两个引用，不是两套系统**（`containerMenu` 只是"当前打开谁"的指针）。活到玩家实体 `remove`（`Player.java:1231` 调 `inventoryMenu.removed`）。按 E 开/关 `InventoryScreen` 不 `new`、不拆它。`closeContainer()`（`Player.java:422`）只是把 `containerMenu` 指回这份同一对象。
- **箱子 / 熔炉 / 工作台等**：`openMenu` 成功才 `new`，赋到 `containerMenu`。关上 `removed` 这份，指针拨回常驻 `InventoryMenu`。
- **打开容器期间 `inventoryMenu` 的真实状态（"空转"的边界）**：
  - 不参与交互这半成立：`ServerPlayer.tick:601` 只 `containerMenu.broadcastChanges()`，点击入口是 `player.containerMenu` —— 打开期间 `inventoryMenu` 不广播、不收点击。
  - 玩家背包格照样能点，但**不经过 `inventoryMenu`**：`ChestMenu:58` `addStandardInventorySlots(inventory, ...)` 把玩家 `Inventory` 的槽 add 进了箱子 menu 自己的 slots（`:64` 箱体槽同理）——数据坐同一个 `Inventory` 容器，槽对象归 `ChestMenu` 管。
  - 收尾时它有活：`doCloseContainer` 调 `inventoryMenu.transferState(containerMenu)`（`AbstractContainerMenu:793-805`，按 `(container, slotIndex)` 对位把对方 `lastSlots` 快照抄回自己）——不抄则切回背包界面时 diff 快照过期、整包误重发。
- 服务端「有没有对应屏幕的 menu」= `containerMenu` 有没有被换成一份新的（`hasContainerOpen()` = `containerMenu != inventoryMenu`）。这表示开了别的容器，**不**表示开了背包屏幕。`clicked`/`broadcastChanges` 进 ACM 不能当开屏信号。
- 自建 windows 不要走 `ClientboundOpenScreenPacket`：那条链 `setScreen` 会换掉房东；windows 挂在已打开的 `AbstractContainerScreen` 上。
