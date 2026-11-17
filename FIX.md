# 修复说明（fix/creative-absolute）

基线：`exp/creative-absolute`（2026-11-17 18:37 八字段版回放，编译通过）。
本分支只做复盘里排在「先拆掉每格一广播和三层拷贝」的那两件，加上创造模式事务边界。没有重写协议结构。

## 改了什么

### 0. 模块格子按专用黑名单拒收

`ModuleContainer.canPlaceItem` 改成 `!stack.is(MODULE_BLACKLIST)`，黑名单单独一个标签，只给 `ModuleContainer` 用：

`data/exxxeebackpack/tags/item/module_blacklist.json`

```json
{
  "replace": false,
  "values": [
    "exxxeebackpack:backpacks/backpack_module",
    "exxxeebackpack:backpacks/backpack_shelf"
  ]
}
```

现在模块格子拒绝模块和背架两样。以后要再拒一类，改这个 JSON 就行。

原来的 `#exxxeebackpack:modules`（`modules.json`）保持原职责——「什么算模块」，由 `ShelfContainer.canPlaceItem` 和 `ShelfMenu.slotChanged` 用；背架的识别（`ShelfContainer`、`BackpackSlot.isActive`）仍按 `BackpackItems.BACKPACK_SHELF` 这个具体物品。

已经在模块里的旧背架仍然拿得出来（`mayPickup` 不受影响）。

### 0.5 创造模式的快速移动改成服务端菜单交互

之前创造模式 Shift 是：客户端先在本地 `clicked()` 搬一遍，再把「背包格 / 库存格 / 宿主格」的最终内容整份发给服务端，服务端按下标 `setByPlayer` / `setItem` 直接写。这等于绕过菜单交互直接改数据，落点算错就会写进盔甲、写进不相干的槽。

现在：

- 客户端遇到创造模式 + `QUICK_MOVE` **不做本地预测**，只发「窗口号 + 格子 + Shift」，三张最终状态表都是空的。
- 服务端收到后照常 `menu.clicked(...)`（`suppressRemoteUpdates` 包住），搬运用的是 `QuickMove` → 宿主菜单的 `moveItemStackTo`，全部走 `Slot.mayPlace` / `Slot.setByPlayer` 这一层，然后统一广播背包菜单和玩家菜单。
- `QuickMove.isPlayerInventory` 只认主背包和快捷栏（`getContainerSlot() < 36`），盔甲和副手不再作为 Shift 落点。
- `applyInventory` 拒绝 36 及以上的下标，作为创造模式整格写入的兜底。

创造模式下「光标上拿着东西」的点击（`PICKUP` / `SWAP`）仍然要用整格写入——光标本来就只存在于客户端，服务端没有可重放的输入。

### 1. `ShelfContainer.flush()` 只写数据

原来每写一格就 `createInventoryUpdatePacket(CHEST)` + `player.containerMenu.broadcastChanges()`。一次点击会触发 N 次「把点了一半的玩家菜单推回客户端」。创造模式下这些中间态会被创造界面当成本地改动回写成 `SetCreativeModeSlot`，就是 Shift 之后背包显示迟一拍的原因。

现在 `flush()` 只把快照写进身上那件背架的 `SHELF_MODULES`，不发包不广播；广播由调用方在事务末尾统一做。

同时删掉 `this.shelf` 这个「打开菜单时抓的那一份」旧引用。`flush()` 只认当前胸甲槽，身上没有背架就直接返回——不再往已经脱下来那件上写。

### 2. 模块内容写回背架

`ModuleMenu` 重写 `slotsChanged`：先走父类把模块格子同步给客户端，再把模块内容写回背架。`ModuleContainer` 里的 `itemStack` 就是架子槽里的那个实例，`flush()` 改它，`ShelfContainer.flush()` 再把整份快照写进胸甲。

这样「改模块内容 → 脱背架 → 再穿上」不会丢，也不需要等卸下时那次 `EquippedWindows.flush`（那时胸甲槽已经换过，写到的是一份脱开的物品）。

### 3. 点击是一个事务

`ServerNetWorking.windowClicky`：

- 整次点击用 `player.containerMenu.suppressRemoteUpdates()` / `resumeRemoteUpdates()` 包住，中间不往客户端推玩家菜单；
- 创造模式（`hasInfiniteMaterials` 且服务端光标为空）仍然不调 `clicked()`，只按客户端给的三张完整物品表写背包、库存、宿主容器，然后只 `menu.broadcastChanges()`，**不广播玩家菜单**——服务端光标是空的，广播回去会把客户端的创造光标清掉；
- 生存模式点完统一做一次：`flushShelf` → `menu.broadcastChanges()` → `playerMenu.broadcastChanges()`。

## 没做的

- `stateId` 和 `changedSlots` 对账：这是复盘里「生存模式补三件事」的内容，范围比这次大，留作下一步。
- 创造模式真正的 `MenuType` 化：那是另一条路线，需要产品上接受「开背包即关掉当前容器」。
- 没进游戏验证，只过了 `compileJava` / `compileClientJava`。

## 测试清单

创造模式：

1. 创造物品栏拿模块 → 放上架子：应该立刻出现、能打开、切生存再拿出仍然在。
2. 背包 Shift 进物品栏：背包应该立刻变空，不等一拍。
3. 背包 Shift 进箱子：背包空、箱子有；重进世界仍然有。
4. 数字键往外拿（背包→热键栏）：不翻倍；往里放（热键栏→背包）：不复制。
5. 改模块内容 → 脱背架 → 穿上：内容还在。
6. 改模块内容 → 卸模块 → 再装上：内容还在。

生存模式：

7. 背包 ↔ 物品栏、箱子双向 Shift：和以前一致。
8. 单点拿起/放下、拖拽、右键分半：和以前一致。
9. 关背包窗口时手上还拿着东西：不被吞。

多人（如果测）：

10. 另一个玩家看到你装备的背架组件更新可能滞后到下次同步；这是创造模式不广播玩家菜单的代价，重开界面会刷新。
