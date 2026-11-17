# 原版 26.1.2 GUI 输入分发线（容器 vs 组件）+ `Screen` 唯一性

来源：`unzip -p <clientOnly-sources.jar>` 逐类读原文核实（`:行号` 为 26.1.2 源码行）。

## 1. 两条线，按"子件是否可变"选

| | 组件线（`RecipeBookComponent`） | 容器线（`AbstractContainerEventHandler`） |
|---|---|---|
| 声明 | `implements GuiEventListener, Renderable, NarratableEntry` | `extends AbstractContainerEventHandler implements Renderable`（= 原版 `Screen` 的形状） |
| 子件 | **写死的几个**（`tabButtons` / `searchBox` / `filterButton`），分发手写点名（`mouseClicked` 里 `for (RecipeBookTabButton tabButton : this.tabButtons) tabButton.mouseClicked(...)`；`extractRenderState` 同样 for 循环） | `children()` 返回**可变列表**，分发白拿 |
| tick | 自己的 `tick()`（宿主 `AbstractRecipeBookScreen.containerTick()` → `recipeBookComponent.tick()` :281） | 同样自己写（接口里没有 tick） |

判断规则：**子件会增减 / 以后还要加摊 ⇒ 容器线**（原版 `Screen` 就是：`children()` 返回 `this.children` :399，`addWidget` 往里塞）。子件写死就那几个 ⇒ 组件线，别硬上容器。

## 2. 容器线白拿什么、要写什么

`ContainerEventHandler extends GuiEventListener` 新增的抽象方法：`children()` / `getFocused()` / `setFocused(GuiEventListener)` / `isDragging()` / `setDragging(boolean)`。
`AbstractContainerEventHandler`（abstract）实现后四个（`isDragging`/`setDragging` 是 final 存取字段；`getFocused`/`setFocused` 管字段 + 通知新旧子件），**只把 `children()` 留给子类**。

default 给的分发（不用自己写）：

- `mouseClicked` → `getChildAt(event.x, event.y)` → 命中则转发给子件；子件吃掉且 `shouldTakeFocusAfterInteraction()` → `setFocused(child)`；左键再 `setDragging(true)`
- `mouseReleased` → `isDragging()` 时转给 `getFocused()`；`mouseDragged` → `getFocused() != null && isDragging() && 左键` 才转
- `mouseScrolled` → `getChildAt` 转发；`keyPressed`/`keyReleased`/`charTyped`/`preeditUpdated` → 转给 `getFocused()`
- ⚠️ 命中即 `return true`（**哪怕子件没吃掉**）——跟原版组件那套"没吃就交回宿主"语义不同；要"没吃透传"得覆盖 `mouseClicked`。

**硬前提**：`getChildAt` 挑人是看子件的 `isMouseOver(x, y)`，而它的 default 恒 `false` ⇒ **子件必须真实现 `isMouseOver`**，否则转发链永远找不到任何子件。

## 3. 宿主 `AbstractContainerScreen` 的四处拦截（决定事件从哪条路进来）

| 方法 | 行为 | 影响 |
|---|---|---|
| `mouseClicked`（:315） | **widgets 优先**：`if (super.mouseClicked(...)) return true;` 之后才处理槽 | 对"挂在 screen 上的 UI"有利 |
| `mouseDragged`（:411） | 只有 `slot == null && menu.getCarried().isEmpty()` 才 `super` | **手上有东西拖（quickcraft）时收不到拖拽** |
| `mouseReleased`（:447） | 先处理槽：`slotId = -999`（点在界面外）→ `slotClicked(... PICKUP)` = **把 carried 丢到地上**；末尾才 `return super.mouseReleased(event)` | 在自家 UI 上松手会被丢东西（不先拦就中招） |
| `mouseScrolled`（:175） | 房东**不调 super** | 滚轮永远到不了 widgets |

⇒ 两条入口路线**二选一，不能并用**（并用 = 同一个件被处理两次）：

- **A. `addWidget` 进房东 children**（原版配方书那样，参数需 `GuiEventListener & NarratableEntry`）：白拿分发，但要吃上表的拦截。
- **B. 混入钩子当入口**（`@Inject(method=..., at=@At("HEAD"), cancellable=true)` → 转发给根）：抢在房东前面 ⇒ 躲开全部拦截；根那边继续用 `ContainerEventHandler` 的 default 当"根 → 件"的分发器（自己调 `root.mouseClicked(...)`，default 顺手记 `focused`/`dragging`，后续 `mouseReleased`/`mouseDragged` 就能转给那个件）。
- 走 B 要按需补钩子：**`mouseDragged`（收拖拽）、`mouseScrolled`（房东不 super）默认都没有钩子**。

## 4. 为什么"骑在 screen 上的东西"不能 `extends Screen`

- `Minecraft.screen` 是**单个引用**：`setScreen` 先 `this.screen.removed()`（:1172）再 `this.screen = screen`（:1195）+ `added()`（:1197）+ `screen.init(getGuiScaledWidth(), getGuiScaledHeight())`（:1201）——**替换**语义；
- 驱动只认它：`this.screen.tick()`（:1910）、`this.screen.resize(...)`（:1537）；
- 输入只认它：`MouseHandler` → `screen.mouseClicked` / `mouseReleased` / `mouseDragged`、`this.minecraft.screen.mouseScrolled`；`KeyboardHandler` → `screen.keyPressed` / `charTyped`。

⇒ 谁 `extends Screen` 谁就占"当前界面"那个位子（把房东顶掉）。同屏多挂一摊 = 容器线或组件线，不是第二个 Screen。

## 5. 本项目的落点（backpack）

- `AbstractBackpackWindows` 走**容器线**：`extends AbstractContainerEventHandler implements Renderable`；自己写 `children()`（返回件的列表）+ `extractRenderState(...)` + `tick()`；入口走 B（混入钩子 `HEAD + cancellable`）。
- 件那层用原版两张脸 `GuiEventListener` + `Renderable`（要实现 `isMouseOver`）——**自建的 `BackpackEventHandler` / `AbstractBackpackEventHandler` 省掉**，原版这条线上没有对应物。

## 6. 照 `Screen` 抄一个「骑在 screen 上的容器」时的取舍

判据：**凡属「当前屏幕这个身份」的都去掉，属「容器 + 渲染」的留下。**

留下：`children()`（件的列表）+ `extractRenderState` + `tick()` + 字段 + 构造器。

别抄：
- `keyPressed` 整块（含 `super.keyPressed` 的前半 + `nextFocusPath` / `createTabEvent` / `createArrowEvent` / `clearFocus` / `changeFocus`）——那是**整屏的键盘焦点导航**（Tab / 方向键换焦点）；而且**没注 `keyPressed` 钩子 ⇒ 键盘事件根本到不了它**，这些代码永不执行。将来真要键盘（快捷键）再按需补钩子。
- 构造器里的 `Minecraft` —— tick 走房东 `containerTick` 钩子转 `windows.tick()` 就够；注 `Minecraft` 只会放大范围、还得自己判场景。
- `@VisibleForTesting` 之类的测试脚手架。

**列表用两个**（`List<Renderable> renderables` + `List<GuiEventListener> children`），别合成「两张脸合一」的自造类型：跟原版 `Screen` 一字不差、加新件零改动、不用写新类型（`narratables` 不需要）。代价 = 同一个件两个列表各进一次 ⇒ **增删必须成对**，收口成两个方法（定稿名 `addRenderableWidget` / `removeWidget`，2026-09-24），外面不许直接摸列表。

## 7. 件（widget）层形状事实（26.1.2 行号，照 `AbstractWidget` 抄件基类时用）

- 字段：`private int x; private int y;`（:31-32）+ `protected int width; protected int height;`（:29-30）；构造 `AbstractWidget(int x, int y, int width, int height, Component)`（:42）
- 存取：`getX/setX`（:251/:256）、`getY/setY`（:261/:266）、`getWidth`（:171）、`getHeight`（:51）、`setRectangle(width, height, x, y)`（:297-299，内部 `setSize` + `setPosition`）
- 命中：`isMouseOver(mx,my) = isActive() && areCoordinatesInRectangle(mx,my)`（:158-159）；`areCoordinatesInRectangle` **左闭右开**：`x >= getX() && y >= getY() && x < getRight() && y < getBottom()`（:293-295）
- `getRectangle()`（:289）委托 `LayoutElement.super.getRectangle()`；`ScreenRectangle` 是 record，便捷构造 `ScreenRectangle(int x, int y, int width, int height)`（`ScreenRectangle.java:11`）
- 子类唯一必画的口 = `protected abstract extractWidgetRenderState(GuiGraphicsExtractor, int mouseX, int mouseY, float a)`（:86）；叙述口 `updateWidgetNarration`（:237）不做叙述就别抄
- 容器侧消费：`ContainerEventHandler.getChildAt`（:28-36）用 `isMouseOver` 挑人；`mouseClicked`（:45-50）命中后 `this.setFocused(child)` ⇒ 件可以不实现键盘导航，但 `setFocused/isFocused` 必须给实现（它们是 `GuiEventListener` 仅有的两个抽象）
- 父级给件位置 = 调件的 `setX/setY`（原版 `AbstractWidget` 的 `setRectangle/setPosition` 一族）——注意原版**不在重排时遍历喂位置**，原版靠重建；本项目为什么必须遍历喂，见 `backpack-rendering-architecture-2026-09-11.md`「重排 ≠ 重建」
