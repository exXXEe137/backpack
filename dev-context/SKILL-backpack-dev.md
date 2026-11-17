> 这份是 9 月快照，不要当现行。现行状态在 Hermes skill `backpack-dev` 文首，进度长文在 Obsidian `03-工程/背包窗口进度-2026-11-01`。路径是 `D:/workspace/projects/backpack`。

---
name: backpack-dev
description: 背包 mod (D:\workspace\backpack) 开发工作笔记 — 26.1.2 Fabric mojmap、Menu 内核 + windows(容器线) 架构、路线图、已验证的坑、API 速查。开背包相关任务必先看。
version: 2.2.0
metadata:
  hermes:
    tags: [Minecraft, Fabric, Modding, Backpack, Menu, DataComponent]
    related: [minecraft-fabric-mod, coding-workflow]
---

# 背包 mod 开发笔记

> **2026-09-11：UI/Menu 架构重定向**。旧路线（`guirender` 的 PanelWidget/ModulesButton/ModulePage/WidgetSlot + `I_slot`）已作废归档；现行方向见下「架构方向」「UI 架构」。
> **2026-09-12 修正**：windows 走**容器线**（`extends AbstractContainerEventHandler implements Renderable`），不是配方书组件那条；件用原版两张脸；自建的 `BackpackEventHandler`/`AbstractBackpackEventHandler` 省掉。机制细节 → `coding-workflow/references/vanilla-gui-event-dispatch.md`
>
> 项目 memory 中 `memory/backpack-architecture.md` 与本文保持同一口径。

## 项目基础

- **路径**：`D:\workspace\backpack`
- **版本**：**Fabric 26.1.2**（不是 1.21.1 — 用户曾纠正过我的版本错误）
- **modid**：`createbackpack-fly-mod`
- **包名**：`com.exxxee.backpack`
- **构建**：Gradle + Fabric Loom 1.16.1，Java 25（兼容 21）
- **教学**：教学模式（用户敲，我引导）

## 源码边界（每次查原版行为先确认这）

**优先级**：
1. **`D:\workspace\backpack\mc_sources\net\minecraft\`**（Loom 解出，覆盖 inventory/screen/slot/item）— 直接 read_file
2. **Loom sources jar**：`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-52430b475d/26.1.2/minecraft-common-52430b475d-26.1.2-sources.jar` — 解到 `/tmp/mc26-all` 后全文 grep（2026-09-11 会话主要靠它）
3. **`C:\Users\ex\.gradle\caches\fabric-loom\26.1.2\minecraft-{common,client,server}.jar`**（原版 mc jar）— 用 javap 看签名
4. **跨项目借**：`D:\workspace\TheCity\build\neoForm\...transformed\net\minecraft\`（1.21.1 neoForm）— 只查前几步都查不到的

**javap 命令模板**：
```bash
/d/应用/java运行环境/java/java25.0.2/bin/javap.exe -p \
  -cp "C:/Users/ex/.gradle/caches/fabric-loom/26.1.2/minecraft-extracted_server.jar;C:/Users/ex/.gradle/caches/fabric-loom/26.1.2/minecraft-common.jar" \
  <完整类名>
```
**两个坑（实测）**：① classpath 必须写 Windows 原生路径 `C:/...`，MSYS 的 `/c/...` 传进 javap 不认（报"找不到类"，实际类在 jar 里）② 26.1.2 的 class 是 Java 25 编译的（class 版本 69），**JDK17 的 javap 读不了**，报"找不到类"——必须用 java25.0.2 的 javap。③ 服务端类（ServerPlayer 等）在 `minecraft-extracted_server.jar`，不在 minecraft-server.jar。Windows javap 用 `;` 分隔 classpath。

**已用 javap 确认的签名**：
- `LivingEntity.onEquipItem(EquipmentSlot, ItemStack oldItem, ItemStack newItem)` — public
- `ServerPlayer.openMenu(MenuProvider)` — public OptionalInt
- `ClientPacketListener.handleOpenScreen(ClientboundOpenScreenPacket)` — public void
- `LivingEntity.getItemBySlot(EquipmentSlot)` / `setItemSlot(EquipmentSlot, ItemStack)` — public

**26.1 vs 1.21.1 重命名（跨项目借要小心）**：
- `ClickType` → `ContainerInput`（枚举值同名）
- `LivingEntity.onEquipChanged` → `onEquipItem(slot, oldItem, newItem)`
- `MultiPlayerGameMode.handleInventoryMouseClick` → **`handleContainerInput(containerId, slotNum, buttonNum, containerInput, player)`**

---

## 架构方向（已确认 ✅）

### 核心初心（不可妥协）
1. **任意容器左挂**——背架面板必须能挂在箱子/工作台/熔炉/玩家背包**等任意 AbstractContainerScreen 界面**的左侧
2. **穿脱架无 bug**——脱下背架的瞬间槽消失/拒绝交互，不留死对象
3. **原版手感**——点击零延迟（客户端预测 + 服务端权威）
4. **跟原版 95%+ 一致**——左/右/中键、shift、双击、拖拽全支持

### 关键架构决定
- ❌ **不继承 `AbstractContainerMenu`**——继承就得注册 MenuType、右击背架开全屏，跟"任意容器左挂"互斥
- ✅ **自建 `AbstractBackpackMenu`**——仿 ACM 结构（`slots` + `clicked` + `doClick` + `quickMoveStack` + quickcraft 状态机 + carried 转发），但**不进 MC 协议**
- ✅ **服务端 menu 挂 `Player`**：`PlayerMixin.@Unique backpackMenu`（`ServerPlayerMixin` 在 `openMenu` RETURN 建）
- ✅ **客户端：房东 screen → `AbstractBackpackWindows`（持"件的列表"）→ 件 → 它那份 menu**（旧口径"客户端 menu 直接挂 Screen"已弃）。windows 走**容器线**：`extends AbstractContainerEventHandler implements Renderable`（= 原版 `Screen` 的形状；**不是**配方书组件那条——配方书子件写死、手写点名转发，我们件数可变、要通用分发）；件照 widget 的形（两张脸 + 靠 `children()` 分发）
- ✅ **不走 `ClientboundOpenScreenPacket`**：那包终点是 `handleOpenScreen` → `MenuScreens.create` → `setScreen`，会换掉当前房东。`AbstractBackpackWindows` 不是 `Screen`，要挂在已经打开的房东上（`IBackpackHost`）。按 E 开背包也不发这个包。
- ✅ **包按对象分，层是触发**：`OpenShelf`/`CloseShelf`/`OpenPage`/`ClosePage`（开包形状照 `fromPacket`，内容换成上桌）。不要为「屏幕开了吗」再打一圈探测包。
- ✅ **创建归口 MenuWindows**：客户端收开包自己 `new` windows 和镜子 menu，挂到当前房东；menu 实例不跨端。服务端在 `openMenu` RETURN / 穿脱 / 屏幕内开关 建收自己的 `ShelfMenu`/`ModuleMenu` 再发包。
- ✅ **手抄 quickcraft 状态机**——不依赖继承，`quickcraftStatus / quickcraftType / quickcraftSlots` 持在 menu 实例上
- ✅ **shift 用 `AbstractContainerMenuAccessor.@Invoker moveItemStackTo`**——让玩家当前打开的原版菜单替我们做目标搜索

### 数据真相链
```
背架物品 (ItemStack)
  └─ SHELF_MODULES (List<ItemStack>)              ← 4 个模块
  └─ MODULE_INVENTORY (ItemContainerContents)     ← 27 格（per module）

ShelfContainer / ModuleContainer implements Container  ← 座儿：活视图，每次现读 itemStack.get(组件)、setItem 写回
BackpackSlot extends Slot, isActive() = hasShelf(player)

Shadow Slot (extraContent) in InventoryMixin          ← 老方案自动入包，独立保留
```

**数据面铁律（2026-09-11 定）**
- **menu = 数据的家**：渲染从 menu 读，不再直接问 `BackpackDataHelper`（helper 退到 menu 背后当接线）
- **座儿两个数据面（2026-09-27 改）**：服务端座儿 = 活视图（不存副本，现读组件，照原版服务端 menu 坐真 Container）；客户端座儿 = **镜子**（空壳容器，包喂，照原版客户端 menu）。原"两端都活视图、不造镜子"作废（组件整物替换 = 背架 31 个 ItemStack 全量重同步，带宽放大）
- ⚠️ **待修**：容器别攥构造时那只 `ItemStack` 实例（客户端同步是**整个替换实例**）→ 每次取"现在那只"（`player.getItemBySlot(CHEST)`）

---

## UI 架构（2026-09-11 重定向）

1. **包驱动创建/拆除**：服务端先建自己的 menu，再发开包；客户端收包 `new` windows + 镜子。关包对称：服务端收尾自己的 menu，再发关包，客户端拆 windows 和镜子。menu 实例不跨端。
2. **两层开关横切 shelf 和 page**（不要按对象拆成两套生命期，也不要把两层合成一个关包）：
   - **跟屏幕**：房东容器会话起来时，当时开着的 shelf / page 都发开包，客户端挂到这个房东上；房东会话收掉（关箱子、换屏、按 E 关背包界面）当时挂着的都发关包，客户端拆掉。下次再开屏幕再发开包、重新挂。
   - **屏幕里面**：房东还在。只开/关那一个对象。关 shelf 不动 page，关 page 不动 shelf。
   - 穿脱走同一套对象包：脱架服务端收尾再关包；不是第三种包。
3. **居所 MenuWindows + 成对交付**：创建规矩写在一处（对位 `MenuScreens` / `fromPacket`）；调用点 = 开/关包处理器。成对：造对 → 接上 → 上桌。
4. **持有链（2026-09-27 改）**：房东 screen → `AbstractBackpackWindows`（持 menu，构造喂入）→ 件（从 windows 拿）→ 数据（服务端不到 windows 这节：`Player` 直接持 menu）。现状散着三份引用（`AbstractContainerScreenMixin.backpackMenu` / `PanelWidget.shelfMenu` / `PlayerMixin.backpackMenu`）待清理
5. **windows 不是 Screen**（`mc.screen` 是**单个引用**：`setScreen` 先 `removed()` 旧的再换上、驱动/输入只认它 ⇒ 谁 `extends Screen` 谁把房东顶掉）。走**容器线**：`extends AbstractContainerEventHandler implements Renderable`——白拿 `mouseClicked` 那套"`getChildAt` 挑命中的件再转发"（**硬前提：件必须真实现 `isMouseOver`**，default 恒 false 就永远挑不中）；自己只写 `children()`（返回件的列表）+ `extractRenderState` + `tick()`；由房东混入钩子驱动（init / 渲染 / 输入 / tick 传话）
6. **原版两种 menu 姿势（对照）**：服务端 menu 的 slots 坐**真容器**（活视图）；客户端 menu 的 slots 坐**空壳 `SimpleContainer`**、靠包喂（镜子：`initializeContents` 全量 + `handleContainerSetSlot` 增量）。我们照原版两个数据面：服务端坐真容器（活视图），客户端坐空壳容器（镜子，包喂）——2026-09-27 改，原"两边都走活"作废
7. **写 = 客户端预测**：客户端本地也跑一遍 `clicked`（改镜子），服务端跑同一遍定音（改组件）；分歧靠**增量包**归位（2026-09-27 改，原"背架物品组件同步归位"作废）
8. **渲染推倒重建**：根 = windows → 件挂根下，**带着四笔账重开**：①一个类一锅端 ②父伸手进孙 ③件在 tick 里现造 ④三条数据路
9. **件（UI 件）= 一摊视图**：原版两张脸 `GuiEventListener` + `Renderable`（`setFocused` / `isFocused` 是仅有的两个抽象方法；`isMouseOver` 要实现——`getChildAt` 靠它挑人）；**件从 windows 拿 menu**（2026-09-27 改：menu 挂 windows）、知道自己的位置（父级给）。加一摊视图 = 加一个件，别处零改动。⇒ 自建的 `BackpackEventHandler` / `AbstractBackpackEventHandler` **省掉**（这条线上没有对应物：原版组件和件都直接 `implements`）
10. **windows 持"件的列表"，不列表 menu**：件那层用列表（原版 `Screen.renderables / children` 就是这个形）；menu 那层按角色挂在件上——不写 `Map<角色, Menu>`，也不写成一排 menu 再猜谁是谁
11. **menu 的生死**：`ShelfMenu` / `ModuleMenu` 各自一份对象包；两层触发都作用在这两份上。关 page 丢 `moduleMenu`（同一时刻最多一个）；关 shelf 丢 `ShelfMenu` 对，不自动关 page（用户口径：关 shelf 不动 page）。⚠️ 收尾 = **直接关**，不设"拖拽中拒绝关"的闸：复位 `resetQuickCraft()` + 丢实例。carried 转发到 `player.containerMenu`（会话那份）⇒ 关/重建都不丢手上物。整体关件的执行者 = windows（遍历 `children`，`List.copyOf` 拷防 CME）。
12. **"槽在、但不画/不能点"不用自己写**：原版内置——`AbstractContainerScreen.extractSlots` 里 `if (slot.isActive()) { extractSlot(...) }`，悬停那条也判 ⇒ 视图开关只要让对应槽的 `isActive()` 返 false
13. **两个 menu 的槽内容不重叠**（背架槽只收 MODULE；模块格 `canPlaceItem` 定成"不收模块"）⇒ 没有"跨 menu 搬东西"的需求，`quickMoveStack` 各管自己那摊；跨到原版背包/容器走宿主那个原版 menu
14. **事件入口两条路线二选一，不能并用**（并用 = 同一个件被处理两次）：A. `addWidget` 进房东 children（原版配方书那样，白拿分发但要吃房东拦截）；**B. 混入钩子当入口**（`@Inject(HEAD) + cancellable` → 转发给根）——抢在房东前面 ⇒ 躲开拦截，根那边照旧用 `ContainerEventHandler` 的 default 做"根 → 件"那一级分发。⚠️ 房东四处拦截（`AbstractContainerScreen`）：`mouseDragged`(:411) 只有 `slot == null && carried 空` 才 super（**手上拿东西拖时收不到**）；`mouseReleased`(:447) 先处理槽（`slotId = -999` ⇒ **把 carried 丢地上**）才 super；`mouseScrolled`(:175) **不调 super**（滚轮到不了 widgets）；`mouseClicked`(:315) 反过来是 widgets 优先。⇒ 原"走 B 补 mixin 钩子"作废（2026-09-27）：改走 **Fabric ScreenEvent**（见下「钩子挂载」节），房东拦截的坑已被 Fabric 自家 mixin 抹平
15. **抄原版前必问三件（2026-09-24 定，举证在裁剪侧）**：① 谁调它——调用者在原版是不是 Screen 专属（引擎 / `Minecraft.setScreen` / `resizeGui`）② 它依赖什么前提（协议包 / `mc.screen` / 叙述 / 引擎生命周期）③ 照抄的后果能不能承受。三问都过 ⇒ 照原版；任一条不过 ⇒ **抄形状、改内容，且改动要写得出依据**。已判过不抄的四处：`extends Screen`（`mc.screen` 单引用 ⇒ 顶掉房东）、`narratables` + `addWidget` 的 `NarratableEntry` 约束（叙述出口在 Screen；javap：逼每个件实现 `narrationPriority()`+`updateNarration()`）、“`init(int,int)` 的参数语义”（原版是屏幕尺寸，我们是面板矩形）、`repositionElements` 默认= `rebuildWidgets()` = `clearWidgets()+init()`（会砸件 ⇒ 连带 menu）
16. **服务端用容器会话当「屏幕开没开」，不要问客户端、也不要扫 ACM 方法调用**：原版「对应屏幕的 menu」就是 `ServerPlayer.containerMenu`。`openMenu` 成功后换成箱子/熔炉/工作台那份；`closeContainer` 再拨回 `inventoryMenu`。`hasContainerOpen()` = `containerMenu != inventoryMenu`。`clicked`/`broadcastChanges`/构造都会进 `AbstractContainerMenu`，**不能**当开屏信号。看的是：**有没有一份容器 menu 被赋到 `containerMenu` 上**，以及它什么时候被收掉。
17. **`InventoryMenu` 跟 `InventoryScreen` 脱钩**：玩家实体构造时 `new InventoryMenu`，同时赋给 `inventoryMenu` 和初始 `containerMenu`，活到玩家 `remove`。按 E 开/关背包界面不 `new`、不拆它。关箱子只是 `closeContainer()` 把 `containerMenu` 指回同一对象。因此 `hasContainerOpen()` **不能**表示「开了背包屏幕」；按 E 服务端从不 `openMenu`。其它容器（要覆盖的那一类）可以靠会话。背包界面要挂同一套 windows，不能只靠「服务端有没有这份 menu」。
18. **关屏幕也拆对**：跟屏幕那一层的关包拆 windows 和镜子，不是只 `removed` 下桌留实例。屏幕里面那一层只拆被关的那一个对象。
19. **槽（格子）= 数据对象，不是件（2026-09-25 核证）**：原版每格一个 `Slot` 对象（`ChestMenu:64` 循环 `new Slot(container, i, left + x*18, top + y*18)`），但**不进件/事件树**——渲染、命中（`AbstractContainerScreen.isHovering(slot):543-544`，鼠标减 `leftPos/topPos` 再比 `slot.x/y` :547-554）、点击（`slotClicked:565`）**全归屏幕**（`extractSlot:228`）。坐标 = **框内相对坐标**（`Slot.x/y` final，建槽时定），屏幕渲染时 `pose().pushMatrix(); translate(leftPos, topPos);`（:116-128）⇒ 窗口/缩放变化**槽坐标一个不动**、只更新框的位置 ⇒ **原版没有"逐个喂位置"这回事**（`repositionElements` 不遍历喂件）；`WidgetSlot`（每槽一个渲染+交互对象）是自创形状、原版无此物。对照：`AbstractButton extends AbstractWidget`（:16）点击 `onClick→onPress`（:57-58）⇒ **按钮管自己、格子归屏幕**。
20. **menu 挂点 + page 双前提（2026-09-25 拆题，2026-09-27 定稿）**：原版**屏幕持 menu**（`AbstractContainerScreen.menu` 单一字段 / `MenuAccess`）——"件持 menu"是自创（历史理由=分层生命周期对齐）。原版先例：`Player` 同时挂 `inventoryMenu`（常驻）+ `containerMenu`（动态，关后归位）⇒ "一主多 menu"不违原版。我们两层：`ShelfMenu:14`（竖排槽）/ `ModuleMenu:15`（网格槽）。**page 的存在前提 = shelf ∧ module（双前提，不是树）；建/拆 page 的决策者必须同时看得到两个条件 ⇒ 交汇点天然在 windows**。✅已定（2026-09-27）：menu 挂 **windows**（构造喂入），件从 windows 拿（原"件持 menu"作废）。
21. **件基类不用自建（2026-09-25 核证）**：直接用原版 `AbstractWidget`。白拿：`x/y/width/height` + `getX/setX/...`（:251-299）、`isMouseOver`（:158）、`getRectangle`（:289）、`focused` + `isFocused/setFocused`（:200/218）、渲染入口 `extractRenderState`（**final**，:56-60，管 hover/tooltip/光标，回调抽象 `extractWidgetRenderState`:86）。要写的只有三件：构造传 `Component`（:42）、实现 `extractWidgetRenderState`、实现 `updateWidgetNarration`（空实现即可，:237）。⇒ `AbstractBackpackWidget` 不写；具体件直接 `extends AbstractWidget`。

---

## 钩子挂载 = Fabric ScreenEvent（2026-09-27 核证，替代 `AbstractContainerScreenMixin`）

fabric-screen-api-v1 5.0.1（项目 fabric-api 0.149.0+26.1.2 自带）事件面全覆盖：
- 上桌 = `ScreenEvents.AFTER_INIT` / `BEFORE_INIT`（实例级事件在 BEFORE_INIT 里注册）
- 渲染 = `ScreenEvents.afterExtract(screen)`（26.1 extract 系；beforeExtract/afterBackground 也有）
- 输入 = `ScreenMouseEvents.beforeMouseClick/Release/Drag/Scroll`（每类 allow/before/after 三档）
- 下桌 = `ScreenEvents.remove(screen)`；tick = `afterTick/beforeTick(screen)`
- 红利：房东四处拦截的坑（UI 架构 14）被 Fabric 抹平——他们的 `MouseHandlerMixin` 在输入入口层挂事件 + `AbstractContainerScreenMixin` 把 `mouseReleased`/`mouseDragged` 的 super 提前调
- ⚠️ `AFTER_INIT` 只给 `(client, screen, scaledWidth, scaledHeight)`，不给框位置——用 `getGuiLeft()/getGuiTop()`（待核 26.1 是否公开）或小 `@Accessor` 读 `leftPos/topPos`
- 连带：我们的 `AbstractContainerScreenMixin` 整个退役（空钩子全被事件替代；IBackpackHost/backpackMenu 散引用本来就在清理清单）
- **NeoForge 对照（21.1.249 实锤；26.1.2 大概率同形、未核）**：`ScreenEvent.Init.Pre/Post`、`Render.Pre/Post`、`Closing`、`MouseButtonPressed/Released/Dragged/Scrolled.Pre/Post`、`Key*/CharacterTyped.Pre/Post` + `Opening` + `ContainerScreenEvent`；**无 tick 事件**（要 `ClientTickEvent` 兜）

---

## 路线图（2026-09-11 重排）

### 🟢 P1 数据地基 — 已完成 ✅
| | |
|---|---|
| ✅ `ModuleContainer`（27 格 ↔ MODULE_INVENTORY，活视图） | 删了旧 MouduleContainer 拼写 |
| ✅ `ShelfContainer`（4 格 ↔ SHELF_MODULES，活视图） | canPlaceItem 用 MOD tag 校验 |
| ✅ 集合操作"复制后替换"（identity 替换原则） | 包装器自己保证 |
| ✅ `getContainerSize()` 问数据 | 不硬编码 27 |

### 🟢 P2 槽位抽象 — 已完成 ✅
| | |
|---|---|
| ✅ `BackpackSlot extends Slot` | 路径 `Inventory/slot/` |
| ✅ `isActive()` 现查穿戴 | `BackpackDataHelper.hasShelf(player)` |
| 🟡 Tag `__MODULE` 白名单 | `BackpackItems.MODULE` 常量（json `data/.../tags/item/modules.json` 待确认建没建）|

### 🟢 P3 Menu 内核 — 已完成 ✅（与旧计划相反：**保留** `AbstractBackpackMenu` 并在其上长）
| | |
|---|---|
| ✅ `AbstractBackpackMenu`（slots / clicked / doClick / quickMoveStack / quickcraft / carried） | `Inventory/menu/` |
| ✅ `ShelfMenu`（ShelfContainer + BackpackSlot）/ `ModuleMenu`（ModuleContainer + BackpackSlot） | |
| ✅ `PlayerMixin`（`@Unique backpackMenu`）/ `ServerPlayerMixin`（`openMenu` RETURN 建）/ `LivingEntityMixin`（`onEquipItem` 穿脱） | |
| ❌ ~~`ClientPacketListenerMixin`~~ **已删**——按 E 不走 `handleOpenScreen`（见坑 12）；客户端创建点 = **开包处理器**（2026-09-27） | |

### 🟢 P4 点击内核 — 已落地 ✅
`AbstractBackpackMenu.clicked/doClick/quickMoveStack/resetQuickCraft` 已写（PICKUP / QUICK_MOVE / SWAP / CLONE / QUICK_CRAFT 分支在）。

### 🔴 P5 网络与预测 — 2026-09-27 定：包一族四个
- 旧 payload + `ServerNetWorking` + handler 已删
- **四族**：对象开/关包（shelf/page 各一对；触发 = 跟屏幕 / 屏幕内 / 穿脱）· 全量包（对位 `initMenu`→`initializeContents`）· 增量包（`broadcastChanges` diff）· 操作包（点击上报）
- 归位 = 增量包覆盖镜子；carried 转发 `player.containerMenu` 不自己同步；包带"哪份 menu"标识（shelf/module）顶替 containerId 路由

### 🔴 P6 渲染重建 — **当前主线**
- 旧 `guirender/`（PanelWidget / ModulesButton / ModulePage / WidgetSlot）+ `I_slot/` 归档
- 新树：根 = `AbstractBackpackWindows` → 件挂根下；数据从 menu 读（见「UI 架构」1–8）

### 🟡 P7 老 InventoryMixin 影子槽策略
| 选项 | |
|---|---|
| A. 保留 | Mixin 管"自动入包"，Menu 管"手动点击" |
| B. 部分删 | 删除 `fillStackedContents` 等死代码 |

---

## 已确认的设计决定（不许重新推）

### 1. 槽的动态性 = `isActive()` 现查
- 不做"加进 Menu 再 remove"的死路
- 脱架瞬间 `isActive()` 返回 false，所有点击被拒，物品**不丢**（还在 DataComponent 里）

### 2. 数据访问协议三件套（铁律）
| 操作 | 原则 |
|---|---|
| `ItemContainerContents` 不可变 | `getItem()` 必须 `toList()` 拷副本 |
| `setItem` 必须读 → 改 → 造新 → 写回 | 原地改 record 无意义，且不同步 |
| `List<ItemStack>` 集合类 | `new ArrayList<>(原)` → 改 → `itemStack.set(SHELF_MODULES, 新列表)` |
| **格数永远问数据** | `getContainerSize()` = `toList().size()`，代码里不出现 "27" |

### 3. 字段 vs 参数同名 = 坑
```java
// 错误：
public void setItem(int slot, ItemStack itemStack) {
    List<ItemStack> list = itemStack.get(...).toList();  // ← 错！参数遮蔽字段
}
// 正确：参数改名 stack（根治）
public void setItem(int slot, ItemStack stack) { ... }
```

### 4. `taken` 属于玩家，格子永远住 `current`
```java
ItemStack taken = current.split(count);
setItem(slot, current.isEmpty() ? ItemStack.EMPTY : current);  // ← current
return taken;                                                  // ← taken 离开
```

### 5. 客户端预测 + 服务端权威 + 增量包归位（2026-09-27 改）
- 客户端 `menu.clicked()` = 预测（改镜子，零延迟手感）
- 服务端 `menu.clicked()` = 权威（改组件，防作弊、真落盘）
- 增量包（`broadcastChanges` 形状）覆盖客户端镜子 = 自我回滚（不需要单独写回滚）
- 客户端镜子是缓存，**不是真理**

### 6. 双端 Menu 实例按端挂
| 端 | 挂哪 | 创建销毁触发 |
|---|---|---|
| 服务端 | `Player` 上的 `ShelfMenu`/`ModuleMenu` | 容器会话（`containerMenu` 赋上/拨回）+ 屏幕内开/关 + 穿脱 |
| 客户端 | windows → 镜像 menu（构造喂入） | 收对象开/关包建/拆；不另报「屏幕开了吗」 |

### 7. InventoryMenu 内穿脱 → 实时销毁
- 玩家在 InventoryMenu 拖出背架 → `onEquipItem` 触发 → 立即销毁
- 其他容器界面 → 装备槽玩家碰不到 → 不动

### 8. 读从 menu、写走 menu（2026-09-11 定）
- 渲染只认自己的 menu（helper 退居其后台）
- 点击/拖拽也走 `menu.clicked`，不另开发包路径

---

## 踩过的坑（不许重犯）

### 1. 参数遮蔽（必看）
`setItem(int, ItemStack itemStack)` 形参和字段同名 → 形参优先 → 读错对象 → NPE 或不同步。**根治：参数改名**。

### 2. 副本必须写回
- `ItemContainerContents.toList()` 返**副本**，改了不写回 = 白干
- `List<ItemStack>` 改原对象不换 = identity 相同 = MC 不同步
- **铁律**：每次读写都是"读出 → 改 → 造新 → 写回"

### 3. `NonNullList.withSize(27, EMPTY).isEmpty()` 恒为 false
- 长度 27 ≠ 空
- 判空必须遍历 27 格问 `isEmpty()`

### 4. `Slot.clicked` 26.1 不存在
- 旧版 `Slot.clicked()` 1.21.4+ 拆成 `safeInsert / safeTake / tryRemove`
- 1.21.4+ 点击语义全在 `AbstractContainerMenu.doClick` 状态机里
- 1.21.4+ `Slot` 是纯数据单元，无点击逻辑

### 5. `AbstractContainerMenu.moveItemStackTo` 是 protected
- 不继承调不到
- 解法：`AbstractContainerMenuAccessor.@Invoker` 借玩家当前打开的原版菜单替我们做

### 6. Player.onEquipChanged（不存在）
- 26.1 改名 `LivingEntity.onEquipItem(slot, oldItem, newItem)`
- 是 public 方法，双端都会调（自身做 `isClientSide` 判断）

### 7. `ClickType` 26.1 改名 `ContainerInput`
- 类名换了，枚举值同名

### 8. mc_sources 不含 Player/Server/ClientPacketListener/LivingEntity/ContainerInput
- 查这些类要跨项目借（TheCity/neoForm 1.21.1）
- 借完必须按 26.1 重命名修正

### 9. 旧 `InventoryMixin.extraContent` 影子槽仍保留
- 跟 Menu 职责分：Mixin 管"自动入包"，Menu 管"手动点击"
- **不要删**——跟新架构共存

### 10. `AbstractContainerScreenMixin` 已注册
- 在 `exxxeebackpack.client.mixins.json` 里已注册（`ClientPacketListenerMixin` 已删，别再注册它）

### 11. Identifier 命名空间只能小写（MOD_ID 坑）
- `MOD_ID = "ExxxeeBackpack"`（类名大写）→ 启动即 `IdentifierException: Non [a-z0-9_.-] character`
- MOD_ID 必须与 `fabric.mod.json` 的 `"id"` 一致（当前 `exxxeebackpack`）
- 改 modid 要联动：fabric.mod.json、mixins json 文件名、assets/ 和 data/ 目录名、所有 `modid:` 引用

### 12. 按 E 背包界面不走 handleOpenScreen（关键！）
- 原版 `ClientPacketListener.handleOpenScreen` **只覆盖实体容器**（箱子/熔炉等，有 `ClientboundOpenScreenPacket`）
- **玩家背包 InventoryScreen 是客户端本地打开**（`inventoryMenu` 常驻 containerId=0，无协议包）
- 所以**客户端创建不能挂 handleOpenScreen**，也不能用这份包换房东 Screen；落在自己的开/关包处理器，windows 挂现房东。
- `InventoryMenu` 常驻 ≠ 背包屏幕开着。其它容器才用 `containerMenu != inventoryMenu` 当会话。按 E 要挂同一套 windows 时，不能只靠服务端这份 menu 在不在。

### 13. 教学铁律（排障时）
- 排障也要引导：给线索/方向，让用户自己改；不要连续代写（本次教训：用户明确"你本应该让我自己做的"）

### 14. 原版"创建"的两扇门（2026-09-11 查证）
- 包门：`openMenu` 发包 → `handleOpenScreen` → `MenuScreens.create`（`type.create` 造 menu 再造 screen；`MenuScreens.create` 全原版**唯一**调用者就是 `handleOpenScreen`）
- 按 E 门：直接 `new InventoryScreen(player)`，常驻 `inventoryMenu` 一出生就挂着
- 共同道口 = `AbstractContainerScreen`，共同时机 = **构造**（不是 `init`；`leftPos/topPos` 到 `init` 才算得出）

### 15. 原版客户端 menu 是"镜子"，不是"活视图"（2026-09-11 查证）
- 客户端 new 的 `ChestMenu` 走"只有行数"那条路 → 里面 `new SimpleContainer(9*rows)` **空壳**
- 内容靠包喂：`initializeContents`（全量）/ `handleContainerSetSlot` → `menu.setItem(...)` / `handleSetCursorItem` → `setCarried`
- 服务端 `ChestMenu` 拿的是箱子本体（`ChestBlockEntity` 实现 Container）
- **同一个 menu 类、两个数据面**；客户端当镜子是因为它手里没有真数据

---

## 关键 API 速查（26.1.2）

### Container 接口（要实现的 8 方法）
```
getContainerSize()
isEmpty()
getItem(int)
removeItem(int, int) → ItemStack         // split 切块
removeItemNoUpdate(int) → ItemStack
setItem(int, ItemStack)
setChanged()                              // 我们空实现（不是方块实体）
stillValid(Player) → boolean
```
- 其余全是 default（`clearContent`/`canPlaceItem`/`canTakeItem`/`getMaxStackSize`）

### Slot 关键方法
```
Slot(Container, int, int, int)             // 构造器
isActive()                                 // 我们覆写，查穿戴
mayPlace(ItemStack) → boolean              // 委托 container.canPlaceItem
mayPickup(Player) → boolean                // 默认 true
safeInsert(ItemStack, int) → ItemStack     // 返回放不下的剩余
safeTake(int, int, Player) → ItemStack
remove(int) → ItemStack                     // 委托 container.removeItem
getMaxStackSize() / getMaxStackSize(ItemStack)
getItem()                                   // = container.getItem(slot) ← 活视图就靠它
```

### AbstractContainerMenu 关键方法
```
clicked(int slotId, int buttonNum, ContainerInput type, Player)   // public
quickMoveStack(Player, int) → ItemStack                            // abstract
protected moveItemStackTo(ItemStack, int start, int end, boolean reverse) → boolean
getCarried() / setCarried(ItemStack)
setItem(int slot, int stateId, ItemStack)                          // 客户端收包落点
initializeContents(int stateId, List<ItemStack>, ItemStack carried) // 全量推的落点
broadcastChanges()                                                 // 服务端每 tick diff + 只推变了的
```

### 原版点击链路（客户端，26.1 已改名）
```
AbstractContainerScreen.slotClicked / mouseClicked
  └─ MultiPlayerGameMode.handleContainerInput(containerId, slotNum, buttonNum, containerInput, player)
       ├─ 1. 快照所有格子 (itemsBeforeClick)
       ├─ 2. containerMenu.clicked(...)            ← 本地预测！点完立刻变
       ├─ 3. diff 出变了的格子 → HashedStack.create(...)  ← 只报哈希，不报物品
       └─ 4. 发 ServerboundContainerClickPacket(stateId, changedSlots, carriedItem)
```
服务端 `ServerGamePacketListenerImpl.handleContainerClick`：校验 stateId → **也跑一遍 `clicked(...)`** → 拿自己结果跟客户端报的哈希对 → 不一致 `sendAllDataToRemote()`（客户端回滚）。

### 原版 openMenu 链路
```
服务端 ServerPlayer.openMenu(MenuProvider)
  ├─ 1. close 旧 container
  ├─ 2. createMenu(containerCounter, ...) → Menu 服务端实例
  └─ 3. 发 ClientboundOpenScreenPacket（只装 containerId / MenuType / Component title）
客户端 ClientPacketListener.handleOpenScreen(packet)
  └─ MenuScreens.create(packet.getType(), mc, containerId, title)
       ├─ Menu 客户端实例（数据面是空壳）
       └─ Screen 实例 + mc.setScreen
```

### 两条线怎么选 + 我们的落点（2026-09-12 源码核实）

| | 组件线（`RecipeBookComponent`） | 容器线（`AbstractContainerEventHandler`） |
|---|---|---|
| 声明 | `implements GuiEventListener, Renderable, NarratableEntry`（**不是** Screen） | `extends AbstractContainerEventHandler implements Renderable`（= 原版 `Screen` 的形状） |
| 子件 | **写死的几个**（`tabButtons`/`searchBox`/`filterButton`），分发手写点名 | `children()` 返回**可变列表**，分发白拿 |
| tick | 自己的 `tick()`（宿主 `AbstractRecipeBookScreen.containerTick()` 调） | 同样自己写（接口里没有 tick） |

判断规则：**子件会增减 / 以后还要加摊 ⇒ 容器线**；写死就那几个 ⇒ 组件线。**我们走容器线。**

- 容器线白拿的：`mouseClicked` → `getChildAt` → 子件（顺手记 `focused`/`dragging`）、`mouseReleased`/`mouseDragged` 转 `getFocused()`、`mouseScrolled`/键同理；`AbstractContainerEventHandler` 已给 `isDragging`/`setDragging`（final）/`getFocused`/`setFocused`，**只把 `children()` 留给子类**
- 别混：`Overlay`（`mc.setOverlay(...)`）是"整层盖住"的独立层，不是 screen 手里的组件

### LivingEntity.onEquipItem（双端调用）
```java
@Inject(method = "onEquipItem", at = @At("TAIL"))
private void backpack$onEquipItem(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, CallbackInfo ci) {
    if (slot != EquipmentSlot.CHEST) return;
    // 服务端 + 客户端都跑（依据：唯一调用点 LivingEntity.setItemSlot:2253-2255；客户端由 ClientPacketListener.handleSetEquipment:1473-1476 驱动 ⇒ 双端必触发）
}
```

---

## 待办（点头就开干）

1. 座儿拿"现在那只 Stack"：`ShelfContainer` / `ModuleContainer` 别攥构造时的实例（每次 `player.getItemBySlot(CHEST)` / 走 helper）
2. `AbstractBackpackWindows` 定稿形状（2026-09-24，代码未完）：`extends AbstractContainerEventHandler implements Renderable`；字段只留 `children` + `renderables`（**无 `narratables`**）+ 面板矩形；双层入口 `public final init(...)`（判 `initialized`：首跑 `init()` 建件 / 重跑只挪位置）+ `protected init()` 钩子；加件口 `addRenderableWidget(<T extends GuiEventListener & Renderable>)`（`children`+`renderables` 一起进）；`removeWidget`/`clearWidgets` 去掉叙述那半、摘焦点用 `setFocused(null)`；`repositionElements()` **不许** 碰 `clearWidgets()`/`init()`/`rebuildWidgets()`；驱动 = 房东混入钩子（入口 = B：`HEAD + cancellable`；待补 `mouseDragged` / `mouseScrolled` / `removed`）。**2026-09-25 修**：`repositionElements()` 不需要"遍历喂件"——原版对应物是整框 `translate`（见 UI 架构 18）；件基类直接用原版 `AbstractWidget`（见 20）；**menu 挂点已定（2026-09-27）：挂 windows（构造喂入），件从 windows 拿**
3. `MenuWindows` 居所 + **开包处理器**（客户端创建点 = 收开包建对；居所形状照 `MenuScreens` 不变）
4. 新渲染树照原版搭（根 = windows → 件挂根下），不许再犯四笔账
5. 点击走 `menu.clicked`（客户端预测 + 服务端定音）
6. 清理散引用（三份 `*backpackMenu` / `shelfMenu`）+ 归档 `Windows`；`IBackpackHost` 分家（客户端持 windows / 服务端持 menu）

---

## 协作风格（来自用户记忆）

- **教学**：用户敲，我引导；用户"我不会"也要继续引导，不能反问
- **深读项目**：不表象思考；每次回复先 read_file 看最新代码
- **不要追加**：布置作业后立即停笔，禁止"惯性填充"
- **推结论 = 跑题**：不要把推理路径当答案
- **禁 ABC 清单**：能直接给答案就别列选项
- **追问条件不全时直接给答案+解释**
- **版本校正**：项目是 26.1.2 不是 1.21.1，用户已纠正过
