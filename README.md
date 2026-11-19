# exsbackpack

动力背架：装备在胸甲位，带四个模块位，每个模块自带 27 格存储。背包窗口挂在任意容器界面的左侧，不替换当前屏幕。

- Minecraft 26.1.2 / Fabric Loader 0.19.2+ / Fabric API 0.149.0+26.1.2

## 文档

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) —— 设计思路与架构（为什么这么拆、每层归谁管、坑在哪）
- [FIX.md](FIX.md) —— 2026-11-18 那一轮修复的说明与测试清单
- [RECONSTRUCTION.md](RECONSTRUCTION.md) —— 18:37 八字段版本的回放说明（只作复盘）

## 构建

```
gradlew build
```

产物在 `build/libs/exsbackpack-<version>.jar`。

## 安装

1. 安装 Fabric Loader 0.19.2 及以上
2. 把 jar 放进 `mods`
3. 同时需要 Fabric API 0.149.0+26.1.2

## 许可

见 [LICENSE](LICENSE)。
