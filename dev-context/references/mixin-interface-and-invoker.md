# 自建 Menu 架构 — Mixin 接口模式 & 访问 protected 方法 & 原版分层

（2026-09-05/06 backpack 实战，26.1.2 Fabric mojmap）

## 1. 为什么不能直接引用 Mixin 类做强转（"Mixin class cannot be referenced directly"）

```java
// ❌ 编译错：Mixin 类不允许被外部直接引用
((AbstractContainerScreenMixin)(Object) screen).setBackpackMenu(...)

// ✅ 正解：接口模式
// 定义普通接口（非 Mixin）：
public interface IBackpackHost {
    AbstractBackpackMenu getBackpackMenu();
    void setBackpackMenu(AbstractBackpackMenu menu);
}
// Mixin implements 接口：
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements IBackpackHost {
    @Unique private AbstractBackpackMenu backpackMenu;
    @Override public AbstractBackpackMenu getBackpackMenu() { return this.backpackMenu; }
    @Override public void setBackpackMenu(AbstractBackpackMenu menu) { this.backpackMenu = menu; }
}
// 外部调用：
((IBackpackHost)(Object) screen).getBackpackMenu();
```

要点：
- `(SomeType)(Object) obj` 两步强转**语法本身合法**（unchecked cast），问题只在 `SomeType` 是 Mixin 类
- 同一接口可被多个 Mixin implements（PlayerMixin 与 AbstractContainerScreenMixin 都 implements IBackpackHost）
- `@Unique` 只标**字段**；Mixin 类一律 `public abstract class`（不取决目标类 abstract）

## 2. @Accessor vs @Invoker

| 注解 | 用途 | 类必须是 |
|---|---|---|
| `@Accessor("field")` | 暴露 **字段** getter/setter | interface |
| `@Invoker("method")` | 暴露 **protected/private 方法** | interface |

```java
@Mixin(Slot.class)
public interface SlotAccessor {          // 必须是 interface，不能 abstract class
    @Invoker("onSwapCraft")
    void invokeOnSwapCraft(int count);   // 方法体不能写 {}，否则覆盖生成代码
}
```

## 3. protected 方法跨包调用（原版能调我们调不了）

`Slot.onSwapCraft(int)` / `Slot.onQuickCraft(ItemStack,int)` 是 **protected**。
- 原版 `AbstractContainerMenu` 能调：它跟 `Slot` **同包**（net.minecraft.world.inventory）
- 我们 `AbstractBackpackMenu` 在 `com.exxxee.backpack.Inventory.menu` 包 → **不允许**
- `onSwapCraft` 在 vanilla 基类是**空实现**，无任何子类覆写（ResultSlot 也不覆写）→ 删除调用行为等价，但保留扩展点用 Invoker
- `Slot.onQuickCraft(ItemStack, ItemStack)` 是 **public**（直接调）；protected 重载 `(ItemStack, int)` 才要 Invoker

## 4. 反编译源码变量名不一致 bug

mc_sources 反编译的 `AbstractContainerMenu.doClick` 中：
```java
Slot slotx = this.slots.get(slotId);      // 声明用 slotx
...
slot.onTake(player, itemsTaken);           // 调用用 slot —— 反编译产物自相矛盾，本身编译不过
```
**抄写时统一用自己声明的变量名**，不要照抄混合写法。抄完先 grep `slot\.` / `slotx\.` 核对再编译。

## 5. 原版 Menu / Screen / Slot 分层（渲染改造依据）

| 层 | 管什么 | 证据 |
|---|---|---|
| `AbstractContainerMenu` | 只 slots 数据 + 点击逻辑 | `InventoryMenu` 构造器只有 addSlot |
| `AbstractContainerScreen` | 按钮、渲染、widget | tab 按钮是 InventoryScreen 字段 |
| `Slot` | 单槽：容器引用 + 坐标锚点 | 纯数据单元 |

- **RecipeBookComponent（配方书）= 常驻字段 + isVisible 状态**：构造一次（AbstractRecipeBookScreen 传参存字段），`toggleVisibility()` 切换，每帧 `isVisible()` 判断画不画 → 不"每次 new"
- **menu 与 screen 同生共死**：`MenuScreens.create` 一步创建 menu+screen+setScreen；menu 是 final 非空字段 → 原版渲染无"menu 是否创建"检查
- **我们差异**：backpackMenu 是 @Nullable 动态创建/销毁 → 渲染必须 `menu == null` 判断（面板开关），这是合理差异

## 6. 自建 Menu 架构当前状态（backpack 项目，P3/P4）

- `AbstractBackpackMenu`：player + slots(NonNullList) + clicked/doClick（7 手势全抄）+ quickMoveStack abstract
- doClick 完整分支：QUICK_CRAFT / PICKUP / QUICK_MOVE / SWAP / CLONE / THROW / PICKUP_ALL
- `tryItemClickBehaviourOverride` + `createCarriedSlotAccess` 是 PICKUP 分支隐藏依赖（private 必须连带抄）
- `quickMoveStack` 子类现返回 EMPTY（shift 未实现，后续用 `AbstractContainerMenuAccessor.invokeMoveItemStackTo(stack, 0, 36, false)` + (36,45)）
- 注意：`player.containerMenu.getCarried()` 是唯一 carried 数据源（自建 menu 不持有 carried 状态）
