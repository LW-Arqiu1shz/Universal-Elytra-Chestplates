# 万能鞘翅胸甲 · Universal Elytra Chestplates

> 本分支 / This branch: **26.1.2NeoForge**

[中文](#中文) · [English](#english)

> Author / 作者：**Arqiu-owo** · Code: `LGPL-3.0-only` · Assets: `CC-BY-NC-SA-4.0` — see [LICENSE.md](LICENSE.md)

---

## 中文

一双好的鞘翅需要学会自己粘在胸甲上，不管那是什么胸甲~

### 功能

- **装上鞘翅**：把一件胸甲和一对鞘翅一起丢到**锻造台顶部**，两件会当场合成一件「加装鞘翅的胸甲」，既有护甲又能滑翔。
  胸甲按装备槽判定，所以其他模组加的胸甲也可以。胸甲原有的附魔、纹饰、染色、名字都会保留。
- **拆下鞘翅**：手持合并后的胸甲，**潜行 + 右键**，胸甲回到手上，鞘翅放进另一只手、背包，都满了就掉在脚下。
- **耐久分开算**：飞行只磨鞘翅那部分，挨打只磨胸甲那部分；胸甲和鞘翅的附魔各管各的。
- 合并后的胸甲仍然可以在锻造台上升级（比如钻石升下界合金）。

### 客户端 / 服务端

本模组**不注册任何新物品或方块**，状态都存在原版的物品数据里，所以：

- 只装在**服务端**就能用全部合成、拆解和飞行功能，玩家客户端不用装（原版客户端也能进服）。
- 客户端也装上会多这些：身上显示鞘翅翅膀、物品图标合在一起、鞘翅耐久提示，以及滑翔时**点一下疾跑键取消滑翔**。
- 取消滑翔需要服务端也装了本模组（Fabric 版和 NeoForge 版之间可以互通）；服务端没装时这个功能不生效，其余照常。

### 支持的版本

| 分支 | Minecraft | 加载器 |
|---|---|---|
| [`26.1.2Fabric`](../../tree/26.1.2Fabric) | 26.1.2 | Fabric（需要 Fabric API） |
| [`26.1.2NeoForge`](../../tree/26.1.2NeoForge) | 26.1.2 | NeoForge 26.1.2.103+ |
| [`26.3Fabric`](../../tree/26.3Fabric) | 26.3 | Fabric（需要 Fabric API） |
| [`26.3NeoForge`](../../tree/26.3NeoForge) | 26.3 | NeoForge 26.3.0.51-beta+ |

每个版本是仓库里的一个**分支**（`26.3Fabric` 是默认分支），分支根目录就是 Gradle 工程，更新记录见 [CHANGELOG.md](CHANGELOG.md)。

### 自己编译

需要 **JDK 25** 和 **Gradle 9.7+**（仓库里没有附带 Gradle Wrapper）。切换到想要的版本分支，在根目录运行：

```
gradle build
```

jar 会生成在 `build/libs/` 里。

---

## English

A good pair of elytra should know how to stick itself onto chestplates no matter what chestplates they are :)

### Features

- **Attach elytra**: toss a chestplate and an elytra onto the **top of a smithing table** together and they fuse into one
  "chestplate with elytra" — armor and gliding in one piece. Chestplates are detected by equipment slot, so modded chestplates work too.
  The chestplate keeps its enchantments, trim, dye and name.
- **Detach elytra**: hold the merged chestplate and **sneak + use**. The chestplate stays in your hand; the elytra goes to your other hand,
  then your inventory, and drops at your feet if both are full.
- **Separate durability**: flying only wears the elytra part, getting hit only wears the chestplate part; each keeps its own enchantments.
- A merged chestplate can still be upgraded on a smithing table (e.g. diamond → netherite).

### Client / server

The mod **registers no new items or blocks** — all state lives in vanilla item data, so:

- Installed on the **server only**, merging, detaching and flying all work; players don't need the mod (vanilla clients can join).
- With the mod on the client as well you also get: elytra wings rendered on the player, a combined item icon, an elytra durability tooltip,
  and **tap sprint while gliding to stop gliding**.
- Stopping gliding needs the mod on the server too (the Fabric and NeoForge builds talk to each other); without it that one feature is off and everything else still works.

### Supported versions

| Branch | Minecraft | Loader |
|---|---|---|
| [`26.1.2Fabric`](../../tree/26.1.2Fabric) | 26.1.2 | Fabric (requires Fabric API) |
| [`26.1.2NeoForge`](../../tree/26.1.2NeoForge) | 26.1.2 | NeoForge 26.1.2.103+ |
| [`26.3Fabric`](../../tree/26.3Fabric) | 26.3 | Fabric (requires Fabric API) |
| [`26.3NeoForge`](../../tree/26.3NeoForge) | 26.3 | NeoForge 26.3.0.51-beta+ |

Each version lives on its own **branch** (`26.3Fabric` is the default branch); the branch root is the Gradle project. See [CHANGELOG.md](CHANGELOG.md) for the changelog.

### Building

Requires **JDK 25** and **Gradle 9.7+** (no Gradle Wrapper is included). Check out the branch for the version you want and run, in the repository root:

```
gradle build
```

The jar is written to `build/libs/`.

---

## 贡献者 / Contributors

- **Arqiu-owo** — 设计、美术、文案、调试 / Design, art, writing and testing
- **ChatGPT**（OpenAI）— 程序（最初的大部分代码） / Programming (most of the original code)
- **Claude**（Anthropic）— 程序、项目管理 / Programming and project management

## License / 许可证

- Code / 代码：[GNU LGPL v3.0](COPYING.LESSER)（with [GPL v3.0](COPYING)）
- Assets (images) / 素材（图片）：[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)

Details / 详情：[LICENSE.md](LICENSE.md)
