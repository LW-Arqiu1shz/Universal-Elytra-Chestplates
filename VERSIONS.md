# 万能鞘翅胸甲 26.1.2Fabric 版本笔记（最新在上）

## 1.0.1（2026-11-19，Claude）

- 双语适配：Mod Menu 语言键 `modmenu.{nameTranslation,summaryTranslation,descriptionTranslation}.<modid>`，Mod Menu 搜索本身就匹配原名+翻译名，不需要 mixin。
  元数据里的描述改成纯英文（原来中英写在一行），中文描述放在 zh_cn.json。
- 网络代码没改（NeoForge 版改了联机兼容，包 ID 和格式两边一致）。
- `gradle.properties` 删掉了 AutoMods 容器里的 Linux JDK 路径，才能在本机编译。

## 1.0.0（来源）

- 源码来自 新 zip `UEC_26_1_2_26_3Fb_NF.zip` 的主工程 `mod/`（不含 ports/、dist/、build/）。代码看起来是反编译整理过的。
- zip 里自带的 build jar 和 `成品\` 里的 1.0.0 jar 的 class 完全一致（MD5）。
