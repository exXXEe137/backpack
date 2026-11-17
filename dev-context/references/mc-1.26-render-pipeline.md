# MC 1.26.2 GUI 渲染管线 + Widget 路线

实战总结自背包模组项目（`D:\workspace\backpack`）。**这是技术 knowledge**，不是教学规则。

## 渲染管线改名（1.21 → 1.26）

| 老版本（≤1.20.x） | 新版本（≥1.21） | 26.1.2 实际 |
|---|---|---|
| `render()` | `extractRenderState()` / `extractWidgetRenderState()` | 已彻底改名 |
| `GuiGraphics` | `GuiGraphicsExtractor` | 类型也改了 |

**`AbstractWidget.extractRenderState`**（widget 基类）：

```java
public final void extractRenderState(graphics, mouseX, mouseY, a) {
    if (this.visible) {
        this.isHovered = graphics.containsPointInScissor(mouseX, mouseY)
                      && this.areCoordinatesInRectangle(mouseX, mouseY);
        this.extractWidgetRenderState(graphics, mouseX, mouseY, a);   // ← 子类 override 这个
        this.tooltip.refreshTooltipForNextRenderPass(...);
    }
}
```

子类实现必须 override `extractWidgetRenderState`（**不是** `extractRenderState` ——父类是 `final`）。

## 完整链路：从触发到每帧渲染（类:行号，26.1.2 sources.jar 实读 2026-09-10）

**触发三条路，殊途同归 `Minecraft.setScreen`**：

1. 网络容器（箱子/熔炉）：`ServerPlayer.openMenu`（服务端：closeContainer → nextContainerCounter → createMenu → send ClientboundOpenScreenPacket → initMenu → containerMenu=新）→ 客户端 `ClientPacketListener.handleOpenScreen`(1369) → `MenuScreens.create`(43)（查 SCREENS 注册表 → ScreenConstructor.fromPacket **同时建客户端 Menu + Screen**）→ setScreen
2. 玩家背包按 E（**本地无包**）：Minecraft.handleKeybinds（每帧 runTick 里调）→ `keyInventory.consumeClick()` → `setScreen(new InventoryScreen(player))`（Minecraft.java:2062-2066）
3. 其他本地界面（聊天/暂停/创造）直接 setScreen

**setScreen 生命周期**（Minecraft.java:1166）：旧 screen.removed() → this.screen = 新 → 新 screen.added() → screen.init(width,height)（首次走 init()，重开走 repositionElements()）→ releaseMouse + releaseAll。之后每帧 runTick 调 screen.tick()（Minecraft.java:1904）。

**每帧渲染是两阶段架构**：`Minecraft.renderFrame`(1403) → `gameRenderer.extract(...)`（收集渲染指令阶段）→ GameRenderer.java:522-526：`if overlay != null` 画 overlay（**overlay 优先于 screen**，else-if 结构），`else if screen != null` 调 `screen.extractRenderStateWithTooltipAndSubtitles(...)` → 之后再 `gameRenderer.render(...)`（GPU 提交）。26.1 不是"render 一帧画完"，是 extract state → render 提交。

**容器屏层级陷阱（P6 widget 路线必读）**：`AbstractContainerScreen.extractContents`(112) 顺序 = `super.extractRenderState`（先画 renderables 里的 widget）→ extractLabels → 高亮背层 → extractSlots → 高亮前层。**Slot 永远压在 widget 上面**——自建 widget 画的背板/高亮会被槽位和物品盖住；想画"压在槽位上"的覆盖层只能走屏幕坐标的后画步骤（extractTooltip/extractCarriedItem）或单独 stratum。

## Screen 渲染入口（每帧调用一次）

```java
public final void extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, a) {
    graphics.nextStratum();                            // 第1层：背景层
    this.extractBackground(graphics, mouseX, mouseY, a);

    graphics.nextStratum();                            // 第2层：widget 层
    this.extractRenderState(graphics, mouseX, mouseY, a);   // 遍历 this.renderables

    this.extractDeferredElements(mouseX, mouseY, a);  // tooltip
}
```

**`Screen.extractRenderState`** 内部：

```java
public void extractRenderState(graphics, mouseX, mouseY, a) {
    for (Renderable renderable : this.renderables) {
        renderable.extractRenderState(graphics, mouseX, mouseY, a);
    }
}
```

**`this.renderables`** 是 `List<Renderable>`，你 `addRenderableWidget(widget)` 加进去的 widget 全在这里。每帧循环调每个 widget 渲染。

## 三层渲染分工

| 层 | 调用方 | 干什么 |
|---|---|---|
| 背景层 | `Screen.extractBackground` | `inventory.png` 这类贴图背景 |
| Widget 层 | 遍历 `this.renderables` | 所有 widget（按钮、滑块、自定义 panel）|
| Tooltip 层 | `extractDeferredElements` | 鼠标悬停的提示框 |

**层级间用 `graphics.nextStratum()` 切**——不同层互不重叠遮挡。

## `AbstractContainerScreen.extractRenderState` 内部管线（物品栏/箱子等容器界面）

这是 `Screen.extractRenderState` 在容器界面里**被覆盖**的版本。一共四步：

```java
public void extractRenderState(graphics, mouseX, mouseY, a) {
    this.extractContents(graphics, mouseX, mouseY, a);    // 画背景/widget/Slot/物品
    this.extractCarriedItem(graphics, mouseX, mouseY);    // 画鼠标上抓着的物品
    this.extractSnapbackItem(graphics);                    // 画拖拽取消的回弹动画
    this.extractTooltip(graphics, mouseX, mouseY);         // 画 tooltip
}
```

**关键**：

| 步骤 | 坐标 |
|---|---|
| `extractContents` | **GUI 局部坐标**（用 `pushMatrix/translate(leftPos, topPos)` 把原点移到 GUI 左上角）|
| `extractCarriedItem` | **屏幕全局坐标**（物品跟着鼠标走）|
| `extractSnapbackItem` | **屏幕全局坐标**（动画全局）|
| `extractTooltip` | **屏幕全局坐标** |

`extractContents` 内部把绘制原点推平后画 Slot/标签，所以 Slot 坐标不需要每次加 `leftPos`。后面三个用屏幕坐标因为跟随鼠标或全局动画。

## `extractCarriedItem` 内部 — `draggingItem` vs `menu.getCarried()`

```java
ItemStack carried = this.draggingItem.isEmpty()
    ? this.menu.getCarried()       // A 路径：没在拖拽 → 读服务器侧抓着的物品
    : this.draggingItem;            // B 路径：拖拽中 → 用客户端缓存
```

| 字段 | 类型 | 触发 | 用途 |
|---|---|---|---|
| `menu.getCarried()` | 服务侧同步 | 单击抓起物品 | 正式手抓物品 |
| `draggingItem` | 客户端暂存 | 左键快速拖多个槽 | 拖拽动画期间"手上还剩多少"的镜像 |

**拖拽过程**：起点抓起 → `draggingItem = clickedSlot.getItem().copy()` → 每过一个槽 `draggingItem.shrink(1)` → 端点清空。

**判定**：A 表示"鼠标上物品是正式抓取的"。B 表示"拖拽时画个剩余量"。

**写自定义 widget 一般不碰这个**，用 `menu.getCarried()` 就够。

## `Slot.getNoItemIcon()` — 重要陷阱

**默认返回 `null`**：

```java
public Identifier getNoItemIcon() {
    return null;
}
```

**渲染逻辑**（`AbstractContainerScreen.extractSlot`）：

```java
if (itemStack.isEmpty() && slot.isActive()) {
    Identifier icon = slot.getNoItemIcon();
    if (icon != null) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x, y, 16, 16);
        done = true;
    }
}
```

**含义**：

- 空槽 + `getNoItemIcon()` 有效 → 用这个 identifier 画空槽图
- 空槽 + `getNoItemIcon()` 返回 null → **不画图**

**实战教训**：`BackpackModuleSlot` 没 override `getNoItemIcon()`，所以玩家背包里 Tab 槽位**永远是空白的**，以为代码没跑，其实是渲染逻辑跳过了。

修复：

```java
public class BackpackModuleSlot extends Slot {
    @Override
    public Identifier getNoItemIcon() {
        return Identifier.withDefaultNamespace("container/slot/chestplate");
        // 或 modular_backpacks 自定义槽位图
    }
}
```

**注意路径**：用 `/` 不是 `.` —— `container/slot/chestplate` ✓，`container.slot.chestplate` ✗（MC 容错会画默认灰色方块，但路径就是错的）。

**原版同名槽位是 inventory.png 里的硬纹理 18×18px**。MC 通过 sprite system 索引查找：

- `container/slot/empty` — 默认灰槽（盔甲/工具槽外框）
- `container/slot/helmet` `chestplate` `leggings` `boots` — 盔甲槽位

## ItemSlotMouseAction — 不是 Slot

`List<ItemSlotMouseAction>` 是"鼠标对物品的特殊交互"列表。默认注册 `BundleMouseActions`（捆绑包滚轮取物）。自定义 widget 不用管这个。

## 关键概念区分

### Screen 生命周期 vs Menu 生命周期

```java
player.inventoryMenu    // ← 同一个实例跨屏复用，玩家登录时构造一次
Minecraft.getInstance().setScreen(new InventoryScreen(player.inventoryMenu))   // ← 每次按 E 重开
```

- **`AbstractContainerMenu`** = 服务器侧，玩家登录构造一次，跨屏复用
- **`Screen`** = 客户端侧，每次按 E 重开（重新 init），关掉销毁

**重要后果**：往 `Menu.slots` 加 Slot 是单向累加（关屏不重建）。Screen.init() 每次重开都跑，**不能在那里不加判断地调 `invokerAddSlot`**。

## GLFW keycode 常量

MC 的 `KeyEvent.key()` 返回 int = GLFW keycode：

| 数字 | 键 |
|---|---|
| 256 | ESC |
| 257 | Enter |
| 258 | Tab |
| 262-265 | 方向键（右左下上）|
| 32 | 空格 |
| 48-57 | 0-9 |
| 65-90 | A-Z |

`Screen.keyPressed` 里 `switch (event.key())` 判断这些数字。

**`InputWithModifiers.isSelection()` 触发 button.onPress**（Enter=257 / Space=32 / KP_Enter=335）— `AbstractButton.keyPressed` 自动响应这三个键调 `onPress(event)`。**只要焦点路由通了，键盘激活 button 是免费的**（AbstractButton 行 62-71）。

## 三接口组合 — `Renderable` + `GuiEventListener` + `ContainerEventHandler`

**写自定义 widget 容器**（比如装 N 个 button 的面板）**必须实现这三个接口**，缺哪个补哪个 — 不要从零自己写：

```java
public class MyPanel implements Renderable, GuiEventListener, ContainerEventHandler
```

| 接口 | 提供什么 |
|---|---|
| `Renderable` | `extractRenderState(graphics, mouseX, mouseY, a)` — 被 `Screen.renderables` 遍历 |
| `GuiEventListener` | `mouseClicked/mouseReleased/mouseDragged/keyPressed/setFocused/isFocused` 事件合同 |
| `ContainerEventHandler extends GuiEventListener` | **默认 `mouseClicked` 路由**：`getChildAt(x, y)` 找子组件 → 调 `child.mouseClicked` → 自动 `setFocused` + `setDragging` |

**关键陷阱**：缺 `ContainerEventHandler` = 自己写坐标判定 + 找 children + 委派 — 等于重造 `ContainerEventHandler.mouseClicked` 的轮子。**原版 `RecipeBookComponent` 也 implements 三个接口**（+ `NarratableEntry` 第四个，无障碍朗读）。

`ContainerEventHandler` 强制实现 `children()`（abstract，返回 `List<? extends GuiEventListener>`）— 这是管子组件的入口。

## `Screen` 三个列表（addRenderableWidget / addWidget / addRenderableOnly）

```java
private final List<Renderable> renderables;     // 渲染遍历
private final List<GuiEventListener> children;  // 事件路由
private final List<NarratableEntry> narratables; // 无障碍朗读
```

| 方法 | 约束 | 挂哪个列表 |
|---|---|---|
| `addRenderableWidget<T extends GuiEventListener & Renderable & NarratableEntry>` | 三接口交集 | 三个都加 |
| `addWidget<T extends GuiEventListener & NarratableEntry>` | 二接口 | `children` + `narratables`（**不进** renderables）|
| `addRenderableOnly<T extends Renderable>` | 单接口 | 只挂 renderables（不响应事件）|

**反向操作**：`removeWidget` / `clearWidgets`。

## `AbstractContainerScreen.init()` 钩入点（Mixin 必读）

```java
protected void init() {
    this.leftPos = (this.width - this.imageWidth) / 2;
    this.topPos = (this.height - this.imageHeight) / 2;
    this.itemSlotMouseActions.clear();                  // ← 清掉旧的鼠标动作
    this.addItemSlotMouseAction(new BundleMouseActions(this.minecraft));  // ← 默认加捆绑包滚轮
}
```

**Mixin 钩 `init()` 时**：必须在 `super.init()` 之后才安全 — `leftPos` / `topPos` / `imageWidth` / `imageHeight` 在 super 里计算；`children` / `renderables` / `narratables` 在 `Screen.init()` 父级初始化。

**InventoryScreenMixin 钩 `<init>` 时机过早**（玩家登录构造，那时玩家还没穿装备）— 所以钩 `init()` 不是钩构造器。

## `LayoutElement.getRectangle()` 默认实现

```java
default ScreenRectangle getRectangle() {
    return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
}
```

`AbstractWidget.getRectangle()` 转调这个 — `LayoutElement.super.getRectangle()`。

**方向键焦点导航依赖这个**：键盘按方向键 → `ContainerEventHandler.handleArrowNavigation` 调 `child.getRectangle()` 算 child 几何 → 决定焦点跳到哪个 child。

**含义**：自定义 widget 容器 + 子 button 都要正确实现 `getX/getY/getWidth/getHeight` 才能让 Tab/方向键焦点工作正常。

## 反模式 — 自己写 mouseClicked 路由

```java
// ❌ 错（重复造 ContainerEventHandler.mouseClicked 的轮子）
@Override
public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    for (GuiEventListener child : this.children) {
        if (child.isMouseOver(event.x(), event.y())) {
            return child.mouseClicked(event, doubleClick);
        }
    }
    return false;
}
```

如果 `implements ContainerEventHandler` 了，上面的代码**完全等于默认实现**。**写了 = 白写 = 容易错**。不要写。

## `Slot` 加进 Menu 不重建 = 永久积压

**这是背架项目的核心踩坑**：

```java
@Inject(method = "init", at = @At("TAIL"))
private void addBackpackSlots(CallbackInfo ci) {
    List<Slot> slots = BackpackSlotUtil.createSlots(...);
    for (Slot slot : slots) {
        ((AbstractContainerMenuAccessor) menu).invokerAddSlot(slot);  // ← 累加!
    }
}
```

玩家每次按 E，菜单里的 Slot 越来越多。`Menu.slots` 是 `final NonNullList`，没有去重。

**三种处理方案**：

1. **`@Unique boolean backpackSlotsAdded` 标记**（用户称"屎山"）
2. **`@Invoker removeSlot` 桥接，每次重建先清再 add**（接近正解）
3. **彻底换 widget 路线**（最佳方案，不污染 Menu）

## 通用常量

```java
public static final int SLOT_CLICKED_OUTSIDE = -999;          // 点击 Slot 外
public static final int QUICKCRAFT_TYPE_CHARITABLE = 0;       // 均分拖拽
public static final int QUICKCRAFT_TYPE_GREEDY = 1;           // 每槽取1
public static final int QUICKCRAFT_TYPE_CLONE = 2;            // 整堆复制
public static final int QUICKCRAFT_HEADER_START = 0;          // 拖拽起点
public static final int QUICKCRAFT_HEADER_CONTINUE = 1;        // 拖拽中段
public static final int QUICKCRAFT_HEADER_END = 2;             // 拖拽终点
public static final int CARRIED_SLOT_SIZE = Integer.MAX_VALUE;// 鼠标虚拟槽
public static final int SLOTS_PER_ROW = 9;                    // 一行9格
public static final int SLOT_SIZE = 18;                       // 每格 18×18 像素
```

**拖拽检测**：每次鼠标移动重新 `isHovering(slot, x, y)` 检查 18×18 矩形内，没划到的 Slot 不参与事件。

## 详细路径速查

| 类 | JAR 位置 |
|---|---|
| `AbstractContainerMenu` | `net/minecraft/world/inventory/AbstractContainerMenu.java` |
| `Screen` | `net/minecraft/client/gui/screens/Screen.java` |
| `AbstractContainerScreen` | `net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.java` |
| `AbstractWidget` | `net/minecraft/client/gui/components/AbstractWidget.java` |
| `InventoryScreen` | `net/minecraft/client/gui/screens/inventory/InventoryScreen.java` |
| `RecipeBookComponent` | `net/minecraft/client/gui/screens/recipebook/RecipeBookComponent.java` |

所有源码都在：`D:\workspace\backpack\.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-{common,clientOnly}-52430b475d\26.1.2\*.jar`

**查询命令**（git-bash）：
```bash
unzip -p <jar路径> <类路径>
unzip -l <jar路径> | grep <关键字>
```

## 反编译外部 Mod / Jar

外网挂掉时无法用浏览器查第三方 mod 的实现细节（Oracle/Wikipedia/MCP wiki 都连不上）。但已经下载的 mod 的 `.jar` 直接 unzip 读就行。

**路径常量**：`net/minecraft/...` 是 mojang 源码；其他 mod 通常是 `it/something/modid/...`、`com/...`、`io/...`。

**`.class` 文件怎么读**——Hermes 没有 javap。**Python 解析常量池**就够用：

```python
import struct
with open(r'path\to\Class.class', 'rb') as f:
    data = f.read()

# 跳过 magic + version
pos = 8
cp_count = struct.unpack('>H', data[pos:pos+2])[0]
pos += 2

pool = [None] * cp_count
i = 1
while i < cp_count:
    tag = data[pos]
    pos += 1
    if tag == 1:  # Utf8
        length = struct.unpack('>H', data[pos:pos+2])[0]
        pos += 2
        pool[i] = data[pos:pos+length].decode('utf-8', errors='replace')
        pos += length
    elif tag in (7, 8, 16, 19, 20):
        pool[i] = struct.unpack('>H', data[pos:pos+2])[0]
        pos += 2
    elif tag in (3, 4):  # Integer/Float
        pos += 4
    elif tag in (9, 10, 11, 18):
        pos += 4
    elif tag in (5, 6):  # Long/Double (i 额外 +1)
        pos += 8; i += 1
    elif tag == 12:  # NameAndType
        pos += 4
    elif tag == 15:
        pos += 3
    elif tag == 17:
        pos += 4
    i += 1

# 列出所有常量字符串
for s in pool:
    if isinstance(s, str):
        # 用关键词过滤：'addSlot' 'Inject' 'extends' 'MenuType' 等
        print(s)
```

**或者更简单**：用 `strings` 提取（git-bash 有 /usr/bin/strings.exe via mingw64）：

```bash
strings path/to/Class.class | head -50
```

**实用技巧**：

- 找 `extends` → 看父类
- 找 `@Mixin` `@Invoker` `@Inject` → 看 Mixin 结构
- 找类名（`it/mralxart/...`）→ 知道这个 class 在哪个文件
- 找方法名 + 描述符（Lnet/minecraft/...;）→ 看返回值/参数类型

**结合 `unzip -p + strings`**：先用 `unzip -p ... Class.class > /tmp/x.class` 然后 `strings /tmp/x.class | grep`。比装 CFR/Vineflower decompiler 快得多——反编译时间太长，且 decompile 出来的代码不一定准确，常量池看就够。

## 视频/图像分析定位 Bug

用户上传视频或截图时，**用 `ffmpeg` 抽帧 + `vision_analyze`** 能快速确认客户端渲染结果：

```bash
ffmpeg -i video.mp4 -vf fps=1 -q:v 2 frame_%03d.jpg
```

抽 1fps（每秒 1 帧），NV录屏 30s 出 30 张。然后 `vision_analyze` 提问："这个 Minecraft 界面里发生了什么？玩家在做什么操作？物品栏打开了吗？"——视觉模型回答的事实能当 status report，给用户少走弯路。

**实际案例**：用户传了 31 秒视频，"未穿背架所以 widget 不显示"。原本 agent 不确定 widget 是不是真的跑了，看到视频里"装备栏胸甲位空"确认是用户没穿背架，立刻省了重新调试 round-trip。
