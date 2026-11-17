# 容器/视图重构：从"照片"到"窗户"（MC Fabric — 2026-09-11 backpack 实战）

**什么时候用**：把"构造时抓一份数据"的容器/视图改成"每次现查当前数据源"（活视图）。典型触发：menu 成了渲染的取数源，而底座还攥着构造时那只 `ItemStack`。

## 为什么（用户会问"改这些的原因是什么"——用这个口径讲）

- **照片 vs 窗户**：`new XContainer(stack)` 那一刻抓死**那一只** `ItemStack`；而 Stack 实例会被**整个替换**（客户端 `handleSetPlayerInventory` → `inventory.setItem(...)`；装备槽 `armor.set(i, stack)`）⇒ 攥着的引用从此收不到更新，"每次现读"读的是旧对象的组件。
- **以前为什么没炸**：旧渲染走 helper，helper 每次 `player.getItemBySlot(CHEST)` 现查——**它本来就是窗户**。渲染搬到 menu 上以后，底座必须同样"活"，否则"menu 是数据的家"变成"家里住着一份旧账"。
- **写和回滚也靠它**：写必须落在"现在那只"上；客户端预测 → 服务端定音 → 组件同步回来 → 客户端换新 Stack——攥旧实例就吃不到这一步。
- 一句话：**不缓存，就没有旧的机会。**

## 改锚清单（一个字段牵动全类）

1. `private final ItemStack itemStack` → `private final Player player`（需要时再加 `int moduleIndex` 之类的锚）
2. 构造签名跟着换；**调用点**（客户端创建处 / `ServerPlayerMixin.openMenu` / `LivingEntityMixin.onEquipItem`）一起改
3. 每处 `itemStack.get(组件)` → **每次现查**，优先用现成的 helper 读口（`getBackpack` / `getModules` / `getModuleDataInList`……）
4. 写回 → helper 的写口（`updateBackpack(player, list)` / `update(player, moduleIndex, slot, stack)`）——**别把"副本替换 + 发包 + syncFromModules"再抄一遍**
5. 搭在别的方法上的成员（`isEmpty` / `removeItem` / `removeItemNoUpdate`…）**不用碰**，它们会自动跟着活

## 守卫清单（复核时逐条对）

- [ ] **null**：数据源拿不到（没穿/没组件）→ 返回 `0` / `ItemStack.EMPTY`，不要放行
- [ ] `new ArrayList<>(getX())` —— 拿不到就是 `new ArrayList<>(null)` = **NPE**：先取局部变量、判 null 再 return
- [ ] **越界**：`slot >= list.size()` → `EMPTY` / return。**别写 `slot > list.size()`**——差一个等号，`slot == size` 会被放行，`list.set(size, …)` 抛 `IndexOutOfBounds`
- [ ] 负下标（低风险，看情况）
- [ ] 同一查询别在一行里调两次：提成局部变量（少一次查询 + 好读）
- [ ] 清掉因改动失效的 import（如 `BackpackDataComponents`）和**误加的** import
- [ ] 签名里已经没人用的参数（构造还收着 `ItemStack`）——收掉，调用点跟着简化

## 复核（老师侧）——build 是唯一真相

```bash
cd /d/code/backpack
./gradlew compileJava --offline > /tmp/gb.log 2>&1; echo "GRADLE_EXIT=$?"; grep -E "error:|BUILD" /tmp/gb.log
```

**别**写 `… | tail; echo $?`——那拿到的是 `tail` 的退出码，会误报成功。

回复形状：**"对的 ✓"**（逐条点出，含他顺手多做的）＋ **"还有几个口子"**（行号 + 一行改法，不代写整段）。
本步能验证的极限 = 编译通过——渲染还没接上来时**看不到运行时差别**，如实说明，不要吹"跑通了"。

## 附带：架构定案后的"文档同步扫"（2026-09-11 用户主动要求）

架构重定向后一次扫齐，否则旧文档会把下个会话带沟里。**本项目实例**：旧路线图写着"删掉 `AbstractBackpackMenu`（继承 ACM 的死代码）"，而事实是"保留它、并把整套 menu 长在上面"——照着旧文档做会反向施工。

1. 项目 `D:\workspace\backpack\memory\*.md`（architecture / progress / MEMORY 索引；旧的按用户习惯**留档标作废，不删**）
2. 项目 skill（`backpack-dev`——用户所有，改前先要授权）
3. `coding-workflow/references/*`（本 skill）
4. 顺手查别的 skill 的 `references/` 有没有挂着该项目文档（一次 find/grep 列全，别漏）
