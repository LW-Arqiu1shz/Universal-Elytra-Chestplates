# 万能鞘翅胸甲 26.3Fabric 版本笔记（最新在上）

## 1.0.1（2026-11-19，Claude）

- 双语适配：同 26.1.2 Fabric：只加 Mod Menu 语言键。
  元数据里的描述改成纯英文（原来中英写在一行），中文描述放在 zh_cn.json。
- 网络代码没改（NeoForge 版改了联机兼容，包 ID 和格式两边一致）。
- `gradle.properties` 删掉了 AutoMods 容器里的 Linux JDK 路径，才能在本机编译。
- 许可证改为 LGPL-3.0-only + CC-BY-NC-SA-4.0，开源到 GitHub `LW-Arqiu1shz/Universal-Elytra-Chestplates`。

## 1.0.0（来源）

- 源码来自 旧 zip `Universal_Elytra_Chestpl.zip` 的 `mod/`（不含 backup/、build/）。
- zip 里自带的 build jar 和 `成品\` 里的 1.0.0 jar 的 class 完全一致（MD5）。
