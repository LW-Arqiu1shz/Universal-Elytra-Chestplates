# 万能鞘翅胸甲 26.1.2NeoForge 版本笔记（最新在上）

## 1.0.1（2026-11-19，Claude）

- 双语适配：模组列表：`client/ModListNames` + `mixin/client/ModList{Entry,Screen,Sort}Mixin`（键 `fml.menu.mods.info.{displayname,description}.<modid>`，搜索 lambda `lambda$reloadMods$0`，require = 0）。
  元数据里的描述改成纯英文（原来中英写在一行），中文描述放在 zh_cn.json。
- 联机修复：取消滑翔包 `universal_elytra_chestplates:cancel_glide`（包体为空，StreamCodec.unit）改成 `registrar.optional()`，
  连 Fabric / 原版 / 没装 UEC 的服务器不再被拒；客户端发包前用 `connection.hasChannel(TYPE)` 判断服务端能不能收
  （NeoForge 对不认的包会直接抛 UnsupportedOperationException）。装了 UEC Fabric 版的 Fabric 服务器会用
  minecraft:register / c:register 声明这个包，所以取消滑翔在那里照样能用。
- `CommonHooksCombatWearMixin`（战斗磨损，挂在 `lambda$onArmorHurt$0`）补了 `require = 0`，防止 NeoForge 小版本改 lambda 编号后启动崩溃。
- 许可证改为 `LGPL-3.0-only (code), CC-BY-NC-SA-4.0 (assets)`（原来是 All Rights Reserved），开源到 GitHub `LW-Arqiu1shz/Universal-Elytra-Chestplates`。

## 1.0.0（来源）

- 源码来自 新 zip `UEC_26_1_2_26_3Fb_NF.zip` 的 `mod/ports/gen/26.1.2`（AutoMods 的 build-neoforge.sh 生成的完整源码）。
- 2026-11-19 整理时原样编译过一次，21 个 class 用 javap -c -p 比对，和 `成品\` 里的 1.0.0 jar 完全一致。
- 原来用 javac 直接打包；现在是 ModDevGradle 工程（`gradle build`）。
