# 动力背包 (CreateBackpack Fly Mod)

Minecraft Fabric 1.26.2 模组。给玩家一个穿在胸甲槽的"背架"，可以装 4 个"存储单元"，每个单元 27 格存储。

## 项目信息

- 路径：`D:\code\backpack`
- MC 版本：26.1.2，Fabric Loader 0.19.2，Loom 1.16.1
- Java 25，Gradle 9.4.0
- 包名：`com.exxxee.backpack`
- modid：`createbackpack-fly-mod`
- 构建：`./gradlew build`
- MC mapped source JAR：`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-52430b475d/26.1.2/minecraft-common-52430b475d-26.1.2-sources.jar`

## 源码结构 (9个Java文件)

```
src/main/java/com/exxxee/backpack/
├── CreatebackpackFlyMod.java              # 模组入口
├── Datacomponent/
│   ├── BackpackDataComponents.java        # 注册2个DataComponentType
│   └── ModuleInventoryData.java           # record(ItemContainerContents)
├── item/
│   ├── BackpackItem.java                  # 物品类，use()=SUCCESS(空壳)
│   ├── BackpackItems.java                 # 注册背架+存储单元
│   └── ModCreativeModeTabs.java           # 创造标签"动力背包"
├── container/
│   ├── ModuleContainer.java               # 27格Container实现
│   └── BackpackSlotUtil.java              # createSlots() 创建背包槽位
└── mixin/
    ├── AbstractContainerMenuAccessor.java  # @Invoker addSlot (interface)
    └── InventoryMenuMixin.java            # @Inject 构造器尾部插入槽位

src/client/java/com/exxxee/backpack/
├── client/CreatebackpackFlyModClient.java        # 空
└── client/CreatebackpackFlyModDataGenerator.java # 空
```

## 数据架构（已完成）

两个 DataComponent：

1. **SHELF_MODULES** — `DataComponentType<List<ItemStack>>`，挂在背架物品上，存 4 个模块物品
2. **MODULE_INVENTORY** — `DataComponentType<ModuleInventoryData>`（包装 ItemContainerContents），挂在存储单元上，27 格存储

数据全走 ItemStack DataComponent，不用 Fabric Attachment。

**物品注册：**
- `BACKPACK_SHELF` — 背架，equippable(EquipmentSlot.CHEST)，带 SHELF_MODULES 初始空列表
- `BACKPACK_MODULE` — 存储单元，带 MODULE_INVENTORY 初始 EMPTY

**ModuleContainer：** 实现 Container 接口，27 格。读写通过 DataComponent，每次 setItem/removeItem 重建 NonNullList → ItemContainerContents → 写回 moduleStack。

模块0被硬编码为当前选中模块。

## UI 设计（已确定，待实现）

当玩家打开物品栏时，背包面板挂在 **GUI 左侧**。布局如下：

```
  ╭─ 动力背包 ──────────────────────╮ ╭─ 玩家物品栏 ──────────────────────╮
  │                                 │ │                                   │
  │  ┌────┐  ┌──┐┌──┐┌──┐┌──┐┌──┐  │ │  ┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐  │
  │  │模块│  │01││02││03││04││05│  │ │  │  ││  ││  ││  ││  ││  ││  ││  │  │
  │  │ 0  │  ├──┤├──┤├──┤├──┤├──┤  │ │  ├──┤├──┤├──┤├──┤├──┤├──┤├──┤├──┤  │
  │  ├────┤  │06││07││08││09││10│  │ │  │  ││  ││  ││  ││  ││  ││  ││  │  │
  │  │模块│  ├──┤├──┤├──┤├──┤├──┤  │ │  ├──┤├──┤├──┤├──┤├──┤├──┤├──┤├──┤  │
  │  │ 1  │  │11││12││13││14││15│  │ │  │  ││  ││  ││  ││  ││  ││  ││  │  │
  │  ├────┤  ├──┤├──┤├──┤├──┤├──┤  │ │  ├──┤├──┤├──┤├──┤├──┤├──┤├──┤├──┤  │
  │  │模块│  │16││17││18││19││20│  │ │  │  ││  ││  ││  ││  ││  ││  ││  │  │
  │  │ 2  │  ├──┤├──┤├──┤├──┤├──┤  │ │  ├──┤├──┤├──┤├──┤├──┤├──┤├──┤├──┤  │
  │  ├────┤  │21││22││23││24││25│  │ │  │  ││  ││  ││  ││  ││  ││  ││  │  │
  │  │模块│  ├──┤├──┤├──┤├──┤├──┤  │ │  ├──┤├──┤├──┤├──┤├──┤├──┤├──┤├──┤  │
  │  │ 3  │  │26││27│              │ │  │  ││  ││  ││  ││  ││  ││  ││  │  │
  │  └────┘  └──┘└──┘              │ │  └──┘└──┘└──┘└──┘└──┘└──┘└──┘└──┘  │
  │  (选中模块 0, 蓝色高亮)         │ │                                   │
  │                                 │ │  ┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐  │
  │  ┌──┐┌──┐                      │ │  │H1││H2││H3││H4││H5││H6││H7││H8│  │
  │  │全 ││排 │  (功能按钮)         │ │  ├──┤├──┤├──┤├──┤├──┤├──┤├──┤├──┤  │
  │  │部 ││序 │                      │ │  │H9│  │  │  │  │  │  │  │  │  │
  │  │放 ││整 │                      │ │  └──┘└──┘└──┘└──┘└──┘└──┘└──┘└──┘  │
  │  │入 ││理 │                      │ │        快捷栏 (1-9)                │
  │  └──┘└──┘                      │ │                                   │
  ╰─────────────────────────────────╯ ╰───────────────────────────────────╯
```

**布局说明：**
- **最左侧竖排 Tab 列**：4 个模块槽，竖排堆叠。当前选中模块蓝色高亮。点击切换。
- **Tab 右侧 27 格存储区**：当前选中模块的内容，3 行 × 9 列 = 27 格（ASCII 图中因宽度限制每行只画 5 格示意）。
- **左下角按钮区**："全部放入"、"整理排序"。
- **右侧**：MC 原版物品栏 GUI（不变）。

## Mixin 架构（2026-07-05 已解决）

- `@Mixin(AbstractContainerMenu.class)` + `interface` — `AbstractContainerMenuAccessor` 用 `@Invoker("addSlot")` 暴露 protected addSlot()
- `@Mixin(InventoryMenu.class)` + `abstract class` — `InventoryMenuMixin implements Accessor`，`@Inject` 到构造器 `<init>` 尾部
- **关键发现：** `@Invoker` 必须写在方法声明的原始类（`AbstractContainerMenu`）上，不能跨多层继承在子类（`InventoryMenu`）上查找

## 关键 MC API 参考

**InventoryMenu 构造器：**
```java
public InventoryMenu(Inventory inventory, boolean active, Player owner)
```

**AbstractContainerMenu.addSlot(Slot)** — protected，返回 Slot。通过 `AbstractContainerMenuAccessor` (@Invoker) 桥接调用。

**AbstractContainerMenu.slots** — `public final NonNullList<Slot>`，可直接 @Shadow 操作（备选方案）。

**装备槽：** `player.getItemBySlot(EquipmentSlot.CHEST)` 拿胸甲槽物品。

## 2026-07-05 进度

- ✅ 数据架构（DataComponent、物品注册、ModuleContainer）
- ✅ 两个 Mixin 写好了，游戏不崩溃
- ✅ `BackpackSlotUtil.createSlots()` 读取模块+创建 27 格 Slot
- ❌ **尚未实现：4 个模块的装备槽**（SHELF_MODULES 初始为空列表，没地方放模块）
- ❌ `createSlots` 目前硬编码 `modules.get(0)`，没有 4 模块 Tab 切换

## 待做清单

- [ ] **下一步：BackpackSlotUtil 里创建 4 个模块槽**（让玩家能往 SHELF_MODULES 里放模块）
- [ ] 4 模块 Tab 切换（目前硬编码模块0）
- [ ] 扩展到其他容器（ChestMenu/CraftingMenu 等 Mixin）
- [ ] Screen/Menu 让模块可以单独打开
- [ ] "全部放入""整理排序"按钮
- [ ] 纹理/渲染
