# 原版 26.1.2：菜单数据面（镜子 / 活视图）+ 组件模式 + 命名澄清

来源：`minecraft-common-*-26.1.2-sources.jar` 全量解包（解到 `/tmp/mc26-all`）逐条 grep 核实，2026-09-11。

## 1. 两端各持自己的 menu 实例；差别在 **slots 坐在什么上、谁写它**

- 服务端：`provider.createMenu(...)` → 拿到**真 `Container`**（如 `ChestBlockEntity` 自身）
- 客户端：`MenuScreens.create` → `type.create(containerId, inv)` → 走"只有行数"那条构造 → 内部 **`new SimpleContainer(9 * rows)`（本地空壳）**

```java
// ChestMenu.java —— 客户端这条路造空壳；服务端走 (…, Container, rows) 拿真容器
private ChestMenu(MenuType<?> menuType, int containerId, Inventory inventory, final int rows) {
    this(menuType, containerId, inventory, new SimpleContainer(9 * rows), rows);
}
```

**服务端 = 活视图（不缓存，没有"旧"的机会）**
- `Slot.getItem()` → `this.container.getItem(this.slot)`；`setByPlayer` → `this.container.setItem(...)` —— `Slot` 只是容器的一扇窗
- `AbstractContainerMenu.broadcastChanges()`：逐格 `getItem()` + `synchronizeSlotToRemote(i, current, copy)`（**只推变了的**）+ `synchronizeCarriedToRemote()` + dataSlots
- 调用方：`ServerPlayer` 每 tick 一处（doTick 路径）；已知变化处当场再调一次（`GiveCommand` / `LootCommand` / `ClearInventoryCommands` / `AdvancementRewards`…）

**客户端 = 镜子（自己从不查，全靠被写）**
- 开界面全量：`ClientboundContainerSetContentPacket` → `handleContainerContent` → `player.containerMenu.initializeContents(stateId, items, carried)`
  ```java
  public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
      for (int i = 0; i < items.size(); i++) this.getSlot(i).set(items.get(i));  // ← 写进 slots
      this.carried = carried; this.stateId = stateId;
  }
  ```
- 之后增量：`handleContainerSetSlot` → `player.containerMenu.setItem(slot, packet.getStateId(), itemStack)`
- 光标单独推：`handleSetCursorItem` → `player.containerMenu.setCarried(packet.contents())`
- ⚠️ **`handleSetPlayerInventory` → `player.getInventory().setItem(packet.slot(), packet.contents())` = 整个替换 Stack 实例**（装备槽同步同理替换 inventory 里的元素）

## 2. 由此推出的实施铁律

- **menu 当"数据的家" ⇒ slots 走活视图、每次现读**；不缓存 = 不给"旧"留机会。原版客户端当镜子是**被迫**（手里没有真容器）；我们手里有真东西（物品 + 组件就在本地）⇒ 走**服务端那半**的形状。
- **别攥着 `ItemStack` 实例**：同步会整个替换槽里的实例，攥着旧引用 = 旧账（"现读"读的是旧 Stack 的组件）。要持 `player` + 槽位，每次拿"现在那个"。
- 写：客户端别自己吞（点击 → 自写 payload → 服务端改真身 → 物品同步回来）；服务端 slots 写 = 直改组件。
- 原版**没有**"客户端定时刷新菜单"这回事；tick 只出现在**服务端的 diff + 推**。也不要在 tick 里刷新（两次刷新之间照样是旧的，白干活）。

## 3. "骑在 screen 上的第二套 UI"叫什么

- **配方书 = 组件样板**：`RecipeBookComponent<T extends RecipeBookMenu> implements GuiEventListener, Renderable, NarratableEntry`（**不是 Screen**）；组件构造时收到宿主那份 menu（`new CraftingRecipeBookComponent(player.inventoryMenu)`）。宿主 `AbstractRecipeBookScreen` 手动驱动它：`init(...)` / `extractRenderState(...)` / `mouseClicked` / `keyPressed` / `charTyped` / `mouseDragged` / `tick()` —— **"传话"这活原版自己就在干**。
- **`SubScreen` 不是这个概念**：全工程只有一个 `OptionsSubScreen`（选项子页，普通 Screen，靠 setScreen 安装）。
- **`Overlay` 是另一回事**：`public abstract class Overlay implements Renderable { isPauseScreen(); tick(); }`，配 `mc.setOverlay(...)`，`LoadingOverlay extends Overlay` —— "整层盖住"，游戏循环驱动。
- `mc.screen` 一次只有一个（`setScreen` 里 `this.screen.added()` :1197 + 随后 `screen.init(...)` :1201；26.1.2 的 `Screen.added()` 本体是空方法）⇒ 想跟房东界面**同屏共存**的东西**不能** `extends Screen`；同屏第二摊走**组件线**（`RecipeBookComponent`，子件写死）或**容器线**（`AbstractContainerEventHandler`，子件可变）——见 `vanilla-gui-event-dispatch.md`。

## 4. 客户端预测 + 服务端对账（"点一下立刻动"是怎么做到的）

点击**不是**"客户端只发意图、等服务端回包"——**客户端本地先执行**（零延迟就在这）：

```
AbstractContainerScreen.slotClicked → MultiPlayerGameMode.handleContainerInput(containerId, slotNum, buttonNum, containerInput, player)
  ├─ 1. 快照所有格子 itemsBeforeClick（副本）
  ├─ 2. containerMenu.clicked(slotNum, buttonNum, containerInput, player)      ← 本地预测执行！
  ├─ 3. 逐格 diff：!ItemStack.matches(before, after) → changedSlots.put(i, HashedStack.create(after, hashOps))
  │        （报的是**哈希**，不是物品本身 —— 防客户端伪造内容）
  └─ 4. send(ServerboundContainerClickPacket(containerId, containerMenu.getStateId(), slotNum, buttonNum, containerInput, changedSlots, carriedItem))
```

服务端 `ServerGamePacketListenerImpl.handleContainerClick`：

```
校验 containerId / slotIndex / stillValid / 非旁观
  fullResyncNeeded = packet.stateId() != containerMenu.getStateId()
  containerMenu.suppressRemoteUpdates()
  containerMenu.clicked(slotIndex, packet.buttonNum(), packet.containerInput(), player)   ← 也跑同一遍
  packet.changedSlots() → containerMenu.setRemoteSlotUnsafe(i, hashedStack)               ← 记下客户端预测值
  → 对不上（或 fullResyncNeeded）：sendAllDataToRemote()（整份重发 = 客户端回滚）
```

**要点**：
- 敢预测的地基 = **点击逻辑确定性、两端跑同一套 `clicked`**（客户端预测、服务端定音）
- 客户端报 `HashedStack`（内容哈希）+ `stateId`（据以预测的版本号）；服务端跑完自己的 `clicked` 再拿结果比对，对不上整份重发
- 落点：客户端的 `clicked` 写的是**客户端 menu 的 slots**（原版 = 空壳 `SimpleContainer`）⇒ 原版客户端那个空壳同时充当"预测写字板"
- 我们照抄的形状：客户端本地也跑我们的 `menu.clicked` + 发自己的 payload；服务端跑同一遍定音；分歧靠**背架物品的组件同步**归位（服务端改真身 → 物品同步 → 客户端换新 Stack）

## 5. 两条"白拿"的原版行为（2026-09-11 查证）

**① "槽在、但不画 / 不能点" = 原版内置的 `isActive()`**——不用自己写开关逻辑：

```java
// AbstractContainerScreen.extractSlots(...)     line ~168
if (slot.isActive()) { this.extractSlot(graphics, slot, mouseX, mouseY); }   // inactive 的槽根本不画

// 悬停 / tooltip                                 line ~306
if (slot.isActive() && this.isHovering(slot, x, y)) { ... }                  // 也不响应悬停

// 点击：inactive 槽被拒（AbstractContainerMenu 一路判）
```

⇒ **"槽在，但暂时不显示 / 不可用"是原版内置能力**——**UI 开关 = 一个布尔 + `isActive()` 里带上它**，不用 add/remove 槽。

**② carried 只有一份，而且就是会话那份**：

```java
// AbstractContainerScreen:132 —— 画光标上那件
ItemStack carried = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
```

`this.menu` = 宿主 screen 的 menu（会话 menu）= `player.containerMenu`。我们 `AbstractBackpackMenu` 的 get/setCarried 正是转发到它：

```java
private ItemStack getCarried() { return player.containerMenu.getCarried(); }
private void setCarried(ItemStack itemStack) { player.containerMenu.setCarried(itemStack); }
```

⇒ 光标 / 我们的各 menu 天然共用同一份 carried（不用另设共享对象）；服务端那份的同步也白拿（`broadcastChanges` 里 `synchronizeCarriedToRemote()`）。

## 6. 一屏两摊：原版是"一个 menu 装全部槽"，本项目最终选了"两个 menu"

原版遇到"一屏要显示两摊东西"的答案是**一个 menu 装全部槽**——`ChestMenu` 的构造就是这个：

```java
addSlot(new Slot(chestContainer, i, ...));      // 27 个箱子槽
addSlot(new Slot(playerInventory, j, ...));     // 36 个玩家背包槽
```

因为 shift 转移 / 拖拽 / carried 这些**跨槽动作**都建在"同一个 `slots` 列表"上（`quickMoveStack` → `moveItemStackTo(stack, start, end, reverse)` 扫的就是 `this.slots` 的一段区间）。

**本项目的最终决定（2026-09-11，用户口径）：分成两个 menu**——`ShelfMenu`（4 槽，长命）+ `ModuleMenu`（27 槽，开/关即建删）。它成立的前提（都核过了）：

- **两个 menu 的槽内容不重叠**（背架槽只收 MODULE；模块格 `canPlaceItem` 定成"不收模块"）⇒ 没有"跨 menu 搬运"的需求，`quickMoveStack` 各管自己那摊；跨到原版背包 / 容器走宿主那个原版 menu。
- **carried 天然共用同一份**（见 §5②）⇒ 不存在"两个 carried 谁说话"。
- **menu 挂在哪**：一摊视图一个"件"，件自己持它那份 menu（件生出 → 建；件关掉 → 删）。
- ⚠️ 关件（删/置空 menu）**直接关**，不设"拖拽中拒绝关"的闸（2026-09-24 修正）：carried 转发到 `player.containerMenu`（会话那份）⇒ 不丢；丢的只是那一次拖拽手势（本就该作废）。收尾 = `resetQuickCraft()` 复位本地状态机 + 丢实例。原版同形：`ServerPlayer.tick` 每 tick 查 `stillValid` → `closeContainer()` → `menu.removed(player)`（`ServerPlayer.java:602-605 / 1402-1412`）——箱体被破坏时正在用的界面直接关、不等。
