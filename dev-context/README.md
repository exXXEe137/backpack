# dev-context — Hermes 交接给 Cursor 的开发上下文

这是 Hermes（backpack-dev / coding-workflow 两个 skill + 项目 memory + MC 源码核证 reference）的完整导出。**开工前先按顺序读完本目录。**

## 阅读顺序

1. **`SKILL-backpack-dev.md`** — 最重要：架构方向、UI 架构（17 条）、路线图（P1–P7）、已定设计决定、踩过的坑、API 速查。与 `memory/backpack-architecture.md` 同口径，冲突时以 SKILL 为准（更新）。
2. **`memory/`** — 项目工作笔记。`MEMORY.md` 是索引；`backpack-progress.md` 是进度快照（注意：memory 是旧会话快照，**讨论代码前以实际源码为准**）。
3. **`SKILL-coding-workflow.md`** — 协作铁律（教学模式：引导不代写、一次一问、源码先读再答）+ MC 26.1 陷阱大全。
4. **`references/`** — MC 26.1.2 源码核证笔记：
   - `mc-1.26-render-pipeline.md` — 26.1 渲染管线（`extractRenderState` 体系），写渲染必读
   - `backpack-rendering-architecture-2026-09-11.md` — 本项目渲染架构定案
   - `vanilla-gui-event-dispatch.md` — GUI 输入分发（容器线 vs 组件线）
   - `vanilla-menu-sync-and-container-patterns.md` — 原版 menu 同步 / 客户端镜子
   - `vanilla-26.1.2-creation-and-sync.md` — 创建链 / 装备同步路径
   - `mixin-interface-and-invoker.md`、`mc-live-view-container-refactor.md` — Mixin 模式与活视图容器

## 关键事实（防踩）

- 项目：**Fabric 26.1.2**（不是 1.21），modid `exxxeebackpack`，包 `com.exxxeebackpack`，Java 25。
- 源码查证顺序：项目 `mc_sources/` → loom-cache sources.jar → 原版 jar javap（java25.0.2 的 javap，classpath 用 `C:/` 原生路径）。
- 现行架构一句话：**包驱动四包（开/关 + 全量 + 增量 + 操作）；服务端 menu 活视图、客户端 menu 镜子；客户端 windows（`AbstractBackpackWindows`，容器线，非 Screen）挂房东 screen 下，件挂 windows 下；menu 挂 windows，件从 windows 拿。**

## 本次会话（2026-10-17）讨论进展 — 明天从这里接着写，代码还没动

推进方向：**服务端往客户端**。先定 shelf 的开/关包何时发，再谈渲染。page 不动。点击入口还没排到。

### 代码停在哪

- `LivingEntityMixin.onEquipItem`：胸甲是背架就 `new ShelfMenu(player)` 挂到 `PlayerMixin.shelfMenu`，否则置 `null`。双端都跑。
- `ServerPlayerMixin.openMenu` 整段注释掉了。那段在 RETURN 里再建一份 `ShelfMenu`，把「背架在不在」和「容器开没开」写成了同一次调用，作废。
- `OpenWindowPayload` / `CloseWindowPayload` 已在 `clientboundPlay` 注册类型。没有 `send`，客户端没有接收器。旧笔记里写的 serverbound 已过时。
- `AbstractBackpackMenu.clicked` 的点击操作已经写好，全项目没有调用者。操作包还没立。
- `BackpackDataHelper` 是旧架构残留（直接改组件 + 装备同步包）。`ShelfContainer` 的读写和 `BackpackSlot.isActive` 仍走它，还没从现役路径拿掉。
- 渲染未接：`AbstractBackpackWindows.extractContents` 里有槽绘制，`extractRenderState` 是空的；房东 mixin 五个钩子是空的；`ShelfWindow` 没有创建点；`MenuWindows` 是空类。

### 已定口径（明天按这个写，不要退回）

1. **发包条件是两个状态的与**，不是某个原版方法被调用：`shelfMenu != null`，并且玩家正看着一个要侧挂的界面（含按 E 的 `InventoryScreen`）。与成立发开包，变假发关包。记住上一回发出去的是开还是关，没变就不重发。**2026-10-18 否掉** `containerMenu != inventoryMenu`：没开任何界面时 `containerMenu` 也等于 `inventoryMenu`，这个不等式只表示「另一个容器会话」，把背包界面排除掉了。按 E 服务端仍无信号，这一半的来源还没定。
2. **判断放在自己的方法里**，不写进原版方法体。`openWindowMenu` 只发开包，`closeWindowMenu` 只发关包。上面的与放在单独入口里，由入口二选一。
3. **入口不要挂在 `openMenu` / `closeContainer` / `onEquipItem` 三个 mixin 上。** 原版没有 `closeMenu`。`openHorseInventory`（`ServerPlayer.java:1368`）和 `openNautilusInventory`（`:1381`）自己给 `containerMenu` 赋值，不经过 `openMenu`。`ServerPlayer.tick` 里 `stillValid` 失败也会关容器。调用点放在 **`ServerPlayer.tick` 末尾**：那时这些写法都已经落进 `containerMenu`。
4. **`onEquipItem` 继续只建/清 `shelfMenu`，不发包。** 发包入口先确认是 `ServerPlayer`。`openMenu` 一次右键只跑一次，容器开着时背架仍可能变，所以不能在 `openMenu` 里抽样 `shelfMenu`。
5. **打开 window 分两条写（2026-10-18）。** 容器：服务端在 `ServerPlayer.tick` 末尾读 `containerMenu != inventoryMenu` 且 `shelfMenu != null`，走 `openWindowMenu` / `closeWindowMenu`。原版这条有 `ClientboundOpenScreenPacket` / `ClientboundContainerClosePacket`，客户端自己换 `containerMenu`。按 E：0 个这种包，客户端 `setScreen(new InventoryScreen(LocalPlayer))`，用的是 `Player.inventoryMenu`（`LocalPlayer` 继承下来的字段）。服务端 `containerMenu` 仍是 `inventoryMenu`，tick 看不出背包界面开了。骑乘按 E 会发 `OPEN_INVENTORY`，随后变成马/鹦鹉螺容器，归容器那条。
6. **画和监听同在 `AbstractBackpackWindows`。** 不在 `GameRenderer.extractGui` 再开一条 `GuiRenderState`，也不把画和鼠标拆到两层。`Player.interactOn` 是对实体交互，右键箱子不走它。

### 明天的第一刀

容器那条可以先写：`ServerPlayer.tick` 末尾的入口只覆盖 `containerMenu != inventoryMenu`。按 E 那条另写，因为服务端没有「背包界面开了」的包。不要先接点击，不要先接渲染。
