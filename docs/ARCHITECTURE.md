# 背包模组：设计思路与架构

面向接手的人（或下一个 agent）：读完这一篇应能说清楚「为什么这么拆」「每一层归谁管」「哪些地方是坑」。

- 代码：`d:\workspace\projects\backpack`
- 对照原版：Minecraft 26.1.2 / Fabric Loader 0.19.2 / Fabric API 0.149.0
- 分支：`fix/creative-absolute`（`main` 还停在换父类之前的 `325b4f0`）
- 相关记录：`FIX.md`（2026-11-18 的修复说明）、`RECONSTRUCTION.md`（18:37 版本的回放说明）

---

## 1. 一句话

动力背架装备在胸甲位，带四个模块位，每个模块自带 27 格存储；**背包窗口挂在任意容器界面的左侧，不替换当前屏幕**。

后面所有设计都是围绕「不换屏」这一个约束长出来的。

## 2. 为什么自建菜单

原版打开容器走 `openMenu` → `ClientboundOpenScreenPacket` → `MenuScreens.create` → `setScreen`，这条链必然换屏，和「叠在宿主界面旁边」冲突。

所以：

- 菜单**不放进** `player.containerMenu`，客户端也不换屏；
- 自己发开窗/关窗包，自己按 `windowId` 建菜单、建窗口；
- 窗口挂在宿主 `AbstractContainerScreen` 的 `backpackWindows` 表里，由宿主转发渲染与输入。

代价必须写清楚：原版给「当前菜单」的一切免费能力（开屏、同步、光标、创造模式点击）都要自己接。`§6` 是这套代价最集中的地方。

有一个例外：`AbstractBackpackMenu` **仍然继承** `AbstractContainerMenu`。继承本身不触发开屏，却能白拿 `slots`、`doClick`、`RemoteSlot`、`ContainerSynchronizer`、`stateId` 这些实现。抄一遍是 `2026-11-17` 之前的状态，已经废弃。

## 3. 分层

| 层 | 关键类 | 职责 |
| --- | --- | --- |
| 数据层 | `Datacomponent/BackpackDataComponents`、`ModuleInventoryData` | 物品上的组件：背架的 `SHELF_MODULES`、模块的 `MODULE_INVENTORY` |
| 容器层 | `container/ShelfContainer`、`ModuleContainer` | 把组件包成 `Container` 给菜单用；`flush()` 写回组件 |
| 菜单层 | `Inventory/menu/AbstractBackpackMenu`、`ShelfMenu`、`ModuleMenu`、`QuickMove` | 槽位、点击、Shift 搬运、模块菜单的建/改/关 |
| 网络层 | `Network/payload/*`、`Network/ServerNetWorking`、`WindowSync` | 开窗、关窗、全量、增量、点击、模块选中 |
| 客户端窗口层 | `client/gui/windows/*` | `MenuWindows` 按种类建窗口；`AbstractBackpackWindows` 管渲染、命中、输入；`ModuleTab` 是模块页的入口 |
| 宿主接入层 | `PlayerMixin`、`client/mixin/AbstractContainerScreenMixin` | 玩家身上两张表（菜单/容器）、发号器；宿主屏幕的窗口表与输入转发 |
| 装备生命周期 | `EquippedWindows`、`LivingEntityMixin` | 穿脱背架时开/关窗口、收尾 |

## 4. 数据模型

两个 `networkSynchronized` 组件，随物品本身同步，不另发背包包：

- `SHELF_MODULES`：`DataComponentType<List<ItemStack>>`，固定四格，挂背架；
- `MODULE_INVENTORY`：`ModuleInventoryData`（`ItemContainerContents`，27 格），挂模块。

写回链是「逐层向上」的：

```
ModuleContainer.flush()          改模块物品上的 MODULE_INVENTORY
  → ShelfContainer.flush()       把四个模块物品写进胸甲物品的 SHELF_MODULES
    → 胸甲物品本身随装备同步
```

两个注意点：

1. **不要用 `ItemContainerContents.fromItems` 做背架**：它会裁掉末尾空格，四格会缩成「最后一个有东西的位置」，菜单格数跟着变（见 `开发日志-2026-11-10`）。
2. 容器现在持有的是**拷贝**（构造时从组件抄一份 `items`），靠 `flush()` 写回；因此代码里凡是拿对象身份比较的地方（例如 `ShelfMenu.retarget` 的 `modules.stack() == stack`）都只能算优化，不能当正确性依赖。

## 5. 菜单层

`AbstractBackpackMenu` 只留必须的几样：

- 构造 `super(null, windowId)`：`MenuType` 传 `null`，这个 `int` 就是父类的 `containerId`，对外也当窗口号用；
- `stillValid` 返回 `true`（原版只会拿 `player.containerMenu` 调它，轮不到这份菜单）；
- `removed()` 重写成空：走父类会把光标塞回物品栏，而光标是玩家那份菜单的；
- `getCarried` / `setCarried` 指回 `player.containerMenu`：光标全局只有一份；
- `quickMoveToHost`：Shift 出去的公共实现。

`ShelfMenu implements ContainerListener`：架子格子一变，就建/改/关对应下标的 `ModuleMenu`。`ModuleMenu` 自己重写 `slotsChanged`：先让父类把模块格同步给客户端，再把内容写回背架。

**Shift 的落点**由 `QuickMove` 决定：

1. 先扫宿主菜单里容器不是玩家物品栏的槽（箱子那类）；
2. 再扫玩家物品栏，`reverse = true`，先填快捷栏；
3. 只认主背包 + 快捷栏（`getContainerSlot() < 36`），盔甲和副手不参与；
4. 反方向（物品栏 → 模块）由 `AbstractContainerMenuMixin` 接住宿主 `quickMoveStack` 的余量。

## 6. 网络与点击事务（最容易出问题的一层）

payload 一共七个：`OpenWindowPayload`、`CloseWindowPayload`、`WindowContentPayload`、`WindowSlotPayload`、`ModuleMenuPayload`、`ServerboundWindowClickPayload`、`ServerboundModuleInvokePayload`。

### 生存模式

客户端先在本地跑一遍 `clicked`（预测），把自己算出的背包格差异（`changedSlots`，哈希）发给服务端；服务端**再跑一遍**同一套 `clicked`，最后统一广播背包菜单和玩家菜单。

服务端整次点击用 `suppressRemoteUpdates()` / `resumeRemoteUpdates()` 包住，中间不推玩家菜单——否则一次点击会触发 N 次「点了一半」的状态回推。

### 创造模式

创造模式的光标只存在于客户端，服务端那份永远是空的。所以：

- **`QUICK_MOVE` 不在客户端预测**，只发「窗口号 + 格子 + Shift」，由服务端用它自己那份菜单执行 `clicked`；
- **光标类点击（`PICKUP` / `SWAP`）**才由客户端把「点完的格子里是什么」整份发过去，服务端按 `slot.mayPlace` 校验后直接写；
- 创造模式这条路径**不广播玩家菜单**：服务端光标是空的，推回去会把客户端的光标清掉（`ClientPacketListener.handleSetCursorItem` 在创造界面打开时本来也跳过，但别赌）。

这一层的教训写在 `复盘-创造模式五轮`（知识库）：不要在两端各跑一次 `doClick` 指望它收敛，创造模式没有可重放的输入。

## 7. 客户端窗口层

- `WindowMenuType` / `WindowBuiltInRegistries`：种类 → 容器工厂 + 菜单工厂；
- `MenuWindows`：种类 → 窗口工厂（对照原版 `MenuScreens`，但不 `setScreen`）；
- `AbstractBackpackWindows`：一个窗口的渲染、命中、鼠标/键盘分发，由宿主屏幕的 mixin 转发；
- `BackpackWindowHost`：宿主屏幕上的窗口表，`setBackpackWindow` / `getBackpackWindow`；
- `ModuleTab`：模块页入口，选中时建 `ModuleWindow` 挂到宿主表，取消时摘掉。

**窗口的“在不在”以宿主表为准**，不要只信组件自己的字段。2026-11-19 那个「模块页只能开一次」就是 `unselect` 没清字段、`select` 又按字段提前返回。

## 8. Mixin 清单

| Mixin | 作用 |
| --- | --- |
| `PlayerMixin` | 玩家身上两张 `Map<Integer, …>`（菜单/容器）+ `windowId` 发号器 |
| `client/mixin/AbstractContainerScreenMixin` | 宿主屏幕：窗口表、init 挂窗口、渲染、鼠标/键盘转发 |
| `LivingEntityMixin` | `handleEquipmentChanges` 尾部 → `EquippedWindows.sync`（穿脱开关窗口） |
| `InventoryMixin` | `add` 的 HEAD：物品栏满了溢出进模块；`fillStackedContents` 的 TAIL：配方书把模块材料算进去 |
| `ServerPlaceRecipeMixin` | `moveItemToGrid` 的 HEAD：点配方前从模块补货 |
| `AbstractContainerMenuAccessor` | 调 `moveItemStackTo` |
| `AbstractContainerMenuMixin` | 接宿主 `quickMoveStack` 的余量（物品栏 → 模块） |
| `ServerPlayerMixin` / `PlayerListMixin` | 登录/重生时补状态 |

## 9. 两个「顺手」功能

- **溢出**（`BackpackOverflow`）：`Inventory.add` 返回 false（本体一点都放不下）时才接管，按架子下标顺序先叠后占格；模块菜单开着就写菜单里那份容器。
- **配方书**（`BackpackRecipeSupport`）：`StackedContents` 计数时把模块内容算进去；点配方时在 `moveItemToGrid` 前把缺口从模块搬进物品栏，让原版照常去拿。

两个都跳过创造模式，理由不同：溢出的判断在创造模式会被原版「清零并返回 true」短路（所以钩子在 HEAD），配方书则是原版创造模式根本不看材料。

## 10. 已知限制

- 创造模式装卸模块会响穿装备音效（组件 `equals` 仍是列表对象比较）；
- 创造模式配方书没把模块算进材料；
- 菜单不在 `containerMenu`，所以同步、光标、创造模式点击全部自建，改动要小心；
- 多人环境未完整验证；
- 组件的对象身份不可靠，写回链依赖 `flush()`，不要假设槽里的 `ItemStack` 一直同一个实例。

## 11. 文件地图

```
src/main/java/com/exxxee/backpack/
├── Datacomponent/     组件与数据形状
├── container/         容器 + flush + 溢出 + 配方书支持
├── Inventory/
│   ├── menu/          菜单基类、架子/模块菜单、Shift 搬运、窗口种类
│   └── slot/          背架槽、模块槽
├── Network/           payload 族与服务端收包
├── api/               宿主接口、窗口号发号器
├── mixin/             服务端 mixin
├── item/              物品注册与创造模式标签页
├── WindowSync.java    开/关窗与全量同步
└── EquippedWindows.java 穿脱驱动的窗口生命周期

src/client/java/com/exxxee/backpack/client/
├── gui/windows/       窗口、窗口工厂、ModuleTab
├── mixin/             宿主屏幕注入
├── network/           客户端收包
└── multiplayer/       本地点击预测与发包
```
