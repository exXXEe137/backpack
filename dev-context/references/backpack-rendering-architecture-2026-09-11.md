> 这份是 9 月快照，不要当现行。现行状态在 Hermes `coding-workflow/references/backpack-rendering-architecture-2026-09-11.md` 文首。

# backpack 项目：菜单/窗口/渲染架构（2026-09-11 定案）

项目 `D:\workspace\backpack`（Fabric 26.1.2）。**注**：项目自家 skill `backpack-dev` 已于 2026-09-11 按本口径**重写（v2.0.0）**——用户已授权 agent 更新它；它与本文件应保持同口径。项目侧还有 `D:\workspace\backpack\memory\backpack-architecture.md`（同一口径），两处不一致时以它们为准。

## 现状（2026-09-11 实测）

- `AbstractBackpackMenu` **保留并扩展**：`player` / `public final NonNullList<Slot> slots` / `clicked(int,int,ContainerInput,Player)` / `doClick` / `quickMoveStack`（abstract）/ quickcraft 状态（`quickcraftStatus/Type/Slots`）/ `getCarried/setCarried` —— 一副真 menu 的架子。
- `ShelfMenu(player, ItemStack)` / `ModuleMenu(player, ItemStack)` → 各造 `ShelfContainer` / `ModuleContainer`（`implements Container`，`getItem` 现读 `itemStack.get(组件)`、`setItem` 写回组件），slots = `new BackpackSlot(player, container, i, x, y)`（`BackpackSlot extends Slot`，`isActive()` = `hasShelf(player)`）。
- 宿主：`PlayerMixin`（main，服务端菜单的家）+ `AbstractContainerScreenMixin`（client）+ `PanelWidget.shelfMenu` —— 同一 menu 被三处零散引用，`IBackpackHost` (get/setBackpackMenu) 是接口强转桥。

## 架构定案

### 渲染：推倒重建（用户原话：旧 panel「太乱了」）

- **旧件归档不删**：`client/guirender/PanelWidget`、`ModulePage`、`ModulesButton`、`WidgetSlot` + `client/I_slot/*`（用户习惯：注释/归档，不删文件；注释类记得同步去 mixins.json 注册行）
- **新根 = `AbstractBackpackWindows`**（`client/gui/windows/`）走**容器线**：`extends AbstractContainerEventHandler implements Renderable`（原版 `Screen` 的同一形状，**不是**配方书组件那条——配方书子件写死、手写点名转发；我们件数可变、要通用分发）。自己写 `children()`（返回件的列表）+ `extractRenderState` + `tick()`；**不 extends Screen**（`mc.screen` 一次只一个，要靠宿主 screen 同屏共存）；由宿主混入钩子"传话"驱动（init / 渲染 / 输入 / tick 转发）。继承链白拿的 `mouseClicked` 分发（`getChildAt` → 件）**硬要求件实现 `isMouseOver`**，细节见 `vanilla-gui-event-dispatch.md`。**定稿形状（2026-09-24）**：双层入口 `public final init(...)`（判 `initialized`：首跑建件 / 重跑只挪位置）+ `protected init()` 钩子；字段 `children` + `renderables`（**无 narratables**）；加件口 `addRenderableWidget(<T extends GuiEventListener & Renderable>)`；`repositionElements()` 不许碰 `clearWidgets()`/`init()`/`rebuildWidgets()`（详见项目 `memory/backpack-architecture.md` 第 5 条 + `backpack-dev` 待办 #2）
- 旧 `Windows`（`extends AbstractBackpackEventHandler implements Renderable`）不是 `AbstractBackpackWindows` 的一支、且是空壳 → 归档
- **持有链**：房东 vanilla screen → 我们的 windows → **件** → 它那份 menu → 数据（服务端无 windows：`Player` → menu）。windows 持"**件的列表**"（件那层用列表，照原版 `Screen.renderables / children`），**menu 不列表**：一摊视图一个件，件自己持它那份 menu（以后加第三摊 = 加一个件，别处零改动）。`IBackpackHost` 以后分家：客户端收 windows、服务端还是 menu
- **开关：包驱动（2026-09-27 改，原"零个包"作废）**——四包一族（开/关、全量、增量、操作）。服务端穿脱建/拆 menu 并发开/关包；客户端收包建对（windows + 镜像 menu），**居所 = MenuWindows**（所有"创建"的归口，对位原版 `MenuScreens`）；共同时机 = **构造**（不挑 `AbstractContainerScreen` 那个互相转调的 ctor）
- **写：客户端预测**——客户端本地也跑一遍 `menu.clicked`（照原版 `handleContainerInput`：快照 → 本地 clicked → 报变更），服务端跑同一遍定音；机制见 `vanilla-menu-sync-and-container-patterns.md` §4
- **数据向自己那份 menu 要**（menu = 数据的家；原版 screen 画格子就是遍历 `menu.slots`）；**写的半也走 `menu.clicked`**（旧代码直发 `ModuleIventoryPayload` = 绕过自己的家）；`BackpackDataHelper` 不退场，退到 menu 背后当接线
- 细节见 `references/vanilla-menu-sync-and-container-patterns.md`（活视图 vs 镜子、广播机制、组件命名）

### 旧渲染为什么"乱"（重建不许重犯）

1. `PanelWidget` 一锅端：布局（`leftPos - 32` 算命）+ 标签状态（`selectedTab`）+ 渲染 + 点击路由 + 取数
2. 父伸手进孙：`PanelWidget.mouseClicked/mouseReleased` 直接摸 `modulesButton.page`
3. 件在 tick 里现造/重建：`tabVisuals()` 建按钮、`button.pageVisuals()` 每 tick 跑
4. 三条数据路：helper（背架）/ 原版 `containerMenu.getCarried()`（光标）/ `pageInfo` 快照（页）—— menu 从不参与

### 重排 ≠ 重建：本项目的自创形状（2026-09-24 定；2026-09-25 修正坐标方案；2026-09-27 补鼠标）

原版重排靠**重建**：`resize → repositionElements()（默认实现）→ rebuildWidgets() → clearWidgets() + init()`——件在 `init()` 里重新 new，新位置随构造给出。原版敢这么干的前提 = **件不持状态**（无 menu / 无手势），重建无痛。

我们的件**持它那份 menu** ⇒ 不能重建（砸件 = 丢 menu + quickcraft 手势）⇒ 重排 = **只更新框、一个件都不碰**：件一律用**窗口内相对坐标**，渲染时**整框 `translate`**（原版对**槽**的形：`AbstractContainerScreen.extractContents :116-128`，`pushMatrix → translate(leftPos, topPos) → 画 → popMatrix`；槽的 `slot.x/y` 就是框内相对，`repositionElements()` 只写框的四个值）。「遍历 `children` 喂新位置给每个件」那版（2026-09-24）已废弃。⇒ 对齐的是原版**槽**那一档、不是原版**件**那一档（原版 `AbstractWidget` 件一直是屏幕坐标、画在 translate 之前 `:115`）。

**鼠标 = 同一坐标系的第二个入口（2026-09-27 源码核实）**：件在窗口相对系里 ⇒ 喂给件的 `mouseX/mouseY` 也必须换算（减窗口原点，在转发处做一次）。依据：`AbstractWidget.extractRenderState`（final）`:58` 判 hover 就是拿传入鼠标比自身矩形（`areCoordinatesInRectangle`），`isMouseOver`/`getChildAt` 命中同理；原版槽的先例 = 命中 `isHovering(slot)` `:543-550`「鼠标先减 `leftPos/topPos` 再比 `slot.x/y`」。只 translate 不换鼠标 ⇒ hover/命中错位。

### 面板矩形的两个来源（2026-09-24）

1. **锚点（外部给、会变）**：房东 `AbstractContainerScreen.leftPos/topPos`——房东 `init()` 每次重算（**:93-98**，`leftPos = (this.width - imageWidth) / 2`）；窗口缩放 / 改 GUI 缩放 / 切全屏都会让它变 ⇒ 这就是我们的重跑链必须重算位置的原因
2. **面板尺寸（我们自己的）**：对位原版 `imageWidth/imageHeight`——`protected final`，由**具体子类构造器**给（`AbstractContainerScreen:40-41 / :78-83`）；数值可以是常量 + 按内容算（`ContainerScreen:15`：`super(menu, inventory, title, 176, 114 + menu.getRowCount() * 18)`）。**不由房东传、不由件报**（件/slot 不参与面板尺寸；原版槽位坐标来自 menu）

命名分层（不许一个名字两个东西）：入参 = 房东锚点（`hostLeftPos/hostTopPos`）；我们字段 = **面板框**（对位房东 `leftPos/topPos` 那层角色，实现落成 `windowsLeft/windowsTop/windowsRight/windowsBottom`）；尺寸单独命名（`windowsWidth/windowsHeight`）。挂左侧 = **减**（`hostLeftPos - windowsWidth`），不是加。`right/bottom` 是派生（left+width / top+height），只在 `repositionElements()` 里写一份、别另立字段来源。

### 容器（座儿）改锚：攥"人"不攥"那只 Stack"

客户端同步会**整个替换**槽里的 Stack 实例（`handleSetPlayerInventory` → `inventory.setItem(...)`）⇒ 攥构造时那只 = 旧账。正解：持 `player`（+ `moduleIndex`），每次现查。

- ✅ `ShelfContainer(player)`：读走 `BackpackDataHelper.getModules(player)`（null / 越界守卫齐）、写走 `updateBackpack(player, list)`（副本替换 + 发包）；`ShelfMenu(player)` 跟着简化。
- 🟡 `ModuleContainer`：待照同法改（锚 = `player` + `moduleIndex`；读 `getModuleDataInList(player, i)`，写 `update(player, i, slot, stack)`）。

## 查源码（本项目，比 `mc_sources/` 覆盖全）

`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-<hash>/<ver>/minecraft-common-<hash>-<ver>-sources.jar` → `unzip -o -q` 全量解到 `/tmp/mc26-all` 再 grep（菜单/屏幕/网络/Slot/recipebook 全有；`mc_sources/` 只有部分包）。

## 分层职责表（谁喊谁 / 各层装什么，2026-09-12 定）

房东（`AbstractContainerScreen` 的混入钩子）→ **根 = `AbstractBackpackWindows`** → 件 → 件那份 menu → 数据。

| 层 | 收什么 / 装什么 |
|---|---|
| 房东钩子：`init` / `containerTick` / `extractContents` / `mouseClicked` / `mouseReleased` | 全部落到**根**（windows）——被房东喊的是根，不是件；**`mouseDragged` / `mouseScrolled` 还没有钩子**（要收拖拽、要用滚轮得补 `HEAD + cancellable`） |
| `AbstractBackpackWindows` | 五钩子的落点；**tick 在这层**（`containerTick` 转发进来的每帧逻辑）；持"件的列表"，遍历件做画 / 事件转发 |
| 自建的 `BackpackEventHandler` / `AbstractBackpackEventHandler` | **省掉**：原版 `ContainerEventHandler`（接口）+ `AbstractContainerEventHandler`（abstract，2 字段 4 方法，`children()` 留子类）就是这两层，直接继承；自建一份"行为完全一样"的只会让以后升级对口两份 |
| 件（如 `ShelfBarPiece`） | 持自己那份 menu + 自己的 x/y + `extractRenderState`；命中/点击的活（点到哪个槽 → `menu.clicked`）到用时才重写 `mouseClicked` |

判断标准（本会话踩过）：**归属看调用链**——"宿主有这条钩子"只说明根要收它，不代表件要收；件要不要同款成员 = 根要不要往下转发。

**对位不是一对一（2026-09-12）**：原版 `AbstractContainerScreen` 是五样捆一起——①持 `menu`（`getMenu()` :677）②画槽（`extractSlots` :167 `for (Slot slot : this.menu.slots)` 判 `isActive()`）③槽点击（`slotClicked` :565 → `minecraft.gameMode.handleContainerInput(this.menu.containerId, ...)`）④被驱动（`tick()` :645 final → `containerTick()` :654 留子类）⑤容器能力（children/focused/dragging，来自 `Screen`）。我们的树按截分：**①②③ → 件**（件自持 menu、遍历自己那份 `menu.slots` 画、点击交 `menu.clicked`）；**④⑤ → windows**（驱动者从 `Minecraft` 换成房东钩子）；"Screen 身份"不要（房东占 `mc.screen`）。把 menu 搬回 windows = 让 windows 等同 `AbstractContainerScreen`——加第三摊时要动 windows，正是要避开的。

26.1.2 事件脸契约（`unzip -p <clientOnly-sources.jar>` 实测）：`GuiEventListener` 只有 `setFocused(boolean)` / `isFocused()` 两个抽象，其余（`mouseClicked`/`mouseReleased`/`mouseDragged`/`mouseScrolled`/`keyPressed`/`keyReleased`/`charTyped`/`preeditUpdated`/`isMouseOver`/`getRectangle`/`nextFocusPath`…）全是 default；`TabOrderedElement` 只有 default `getTabOrderGroup`；`Renderable` 只有 `extractRenderState`。`AbstractWidget` 另重写了 `isMouseOver`/`mouseClicked`/`mouseReleased`/`mouseDragged`/`getRectangle`/`nextFocusPath`/`getTabOrderGroup`/`isActive`——每条都有具体业务（矩形命中 / `onClick`+音效 / 键盘导航 / 自身 active 字段）。
