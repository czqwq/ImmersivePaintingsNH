# Immersive Paintings NH

Minecraft **1.7.10** Forge 版的 [Immersive Paintings](https://github.com/Luke100000/immersivePaintings) 移植。
用你自己的图片装饰世界：上传图片、选择尺寸/分辨率、套上画框，甚至做成天花板/地板上的涂鸦。

## 功能

- 4 种物品：绘画、发光绘画、涂鸦、发光涂鸦
- 完整 GUI 编辑器：选图、裁剪/缩放、抖动/减色、像素画模式、画框材质、深度偏移
- 已放置的画可以随时改尺寸（作者本人或 OP），同一张图能挂成不同大小
- 数据驱动画框（OBJ 模型 + 材质纹理）与内置画作包
- 服务器端图片存储、分片网络同步、客户端缩略图/LOD 缓存
- 本地优先：画作已在本机（内置画作包或 `ImmersivePaintingsNH_cache/`）时直接解码，不再从服务器下载
- 隐藏 / NSFW 标记、按作者批量删除、`/immersivepaintingsnh delete <author>` 管理员命令
- 服务端/客户端 Forge 配置

## 构建

```bash
./gradlew build
```

产物在 `build/libs/ImmersivePaintingsNH-<version>.jar`。

> 本工程使用 GTNH 模板的 `jvmDowngrader`（stubs provider = GTNHLib），因此运行环境需要
> **GTNHLib ≥ 0.11**（GTNH 整合包自带）。这是模板本身的依赖约定，不是本 mod 的业务依赖。
> `addon.late.gradle` 关闭了 jvmDowngrader 的 multi-release 输出，否则 1.7.10 Forge 的
> mod 发现器无法解析 `META-INF/versions/21` 中的 class，详见 [PORTING.md](PORTING.md)。

## 使用

1. 合成：纸环 + 染料 → 绘画；染料 + 火药 + 桶 → 8 涂鸦；发光版本用萤石粉合成。
2. 手持物品右键方块放置；对已放置的画右键打开编辑器。
3. 编辑器“New”页支持：输入本地文件路径、输入图片 URL、从 `screenshots/` 选择截图。
4. 服务器可把额外画作包放到 `config/ImmersivePaintingsNH/paintings/`（成对的 `.json` + `.png`，格式同内置包）。
5. 配置文件：`config/ImmersivePaintingsNH.cfg`。

### 本地优先读取（`client.general.preferLocalPaintings`）

客户端在发出 `ImageRequestPayload` 之前，先在**本机**找同一张画，命中就直接解码注册纹理（异步、按需，
不卡进服），完全不经过网络，`maxPacketsPerSecond` 队列里也不再排这些图：

1. `ImmersivePaintingsNH_cache/`（游戏根目录）里已下载过的文件；
2. `config/ImmersivePaintingsNH/paintings/` 的画作包（客户端也放一份时生效，整合包场景收益最直接）；
3. jar 内置画作包（`assets/immersivepaintingsnh/paintings/`）。

画作包（2/3）命中前都会比对同步元数据里的 `contentHash`（服务端 PNG 字节的 MD5）：客户端文件被改动、
或两端画作包版本不一致时直接丢弃本地副本并回落成正常下载，不会显示与服务端不一致的画面。
缓存（1）用旁边的 `<xxx>.contenthash` 戳记录上次下载对应的内容哈希，画作包更新后旧缓存会被丢弃重下，
不会一直显示旧图。

另外，画作包内容对所有客户端都一样，所以 datapack 画作下载后**总是**写进缓存目录，
不受 `cacheOtherPlayersPaintings`（那是限制“别的玩家上传的画作”的）影响。

关掉这个开关即回到“所有图片都从服务器拉”的旧行为。离开世界时日志会汇总本次会话省下的张数与流量。

### 画作同步距离（`common.advanced.paintingTrackingRange`）

画作是实体（`EntityTracker`），服务端只把实体发给**玩家已加载区块内**的玩家，超出追踪范围时连生成包都不发，
客户端上根本没有这个实体，于是画作看起来“凭空消失”。旧版本这个追踪范围写死成 10 格，现在提成配置项：

- 单位是**格**，默认 `160`（= 10 区块，原版服务端 `view-distance` 默认值），可选 16–512；
- 原版客户端默认渲染视距是 8 区块 = `128`，服务端默认 `view-distance` 是 10 区块 = `160`，
  取 160 可覆盖服务端已经加载的整片区域；服务端 view-distance 调低/调高时同步调整即可；
- 服务端读取，注册实体时生效，改完需要重启；画作是静态实体，只会多发一个生成包，成本可忽略。

### 管理员画框（`admin_painting`）

OP 专用的画框物品，贴图 `assets/immersivepaintingsnh/textures/items/admin_painting.png`，**没有配方**，
用 `/give <玩家> ImmersivePaintingsNH:admin_painting` 获取（创造模式物品栏里也能拿到，但拿得到不等于放得下）：

| 操作 | 非 OP | OP（权限等级 2） |
| --- | --- | --- |
| 放置 | 不放，聊天栏红字「权限不足」 | 正常放置 |
| 右键开编辑器改设置 | 不开，聊天栏红字「权限不足」 | 打开编辑器 |
| 左键打掉 | 不掉，聊天栏红字「权限不足」 | 两下击碎，掉落物品本身 |

- 判定全部在**服务端**：物品的 `onItemUse`、实体的 `interactFirst` / `attackEntityFrom`，以及
  `PaintingEditPayload` 里的编辑包校验。改客户端绕过 GUI 直接发编辑包也会被拒；
- 提示走原版聊天：服务端发 `ChatComponentTranslation`（`immersivepaintingsnh.error.admin_only`），
  只传翻译 key，客户端用自己的语言渲染（en_US `Insufficient permission!` / zh_CN `权限不足`），
  每个玩家 1 秒内最多提示一次（左键连点不会刷屏）；
- **悬浮**：实体重写了 `survives()` 恒为 true，不检查支撑方块、也不检查空间是否被占，
  拆掉背后的墙画框不会掉落，一直留在原地；
- **只认 OP 的直接左键**：`attackEntityFrom` 只放行“伤害来源实体就是 OP 本人且是直接命中”的攻击，
  爆炸、箭矢/投掷物、魔法、火焰、生物攻击全部返回 false（Thaumcraft 的 Primal 法杖就是靠
  `EntityPrimalOrb` 的 `createExplosion(null, …)` 把画炸下来的，爆炸伤害源 `getEntity() == null`，
  旧实现会落到父类的“两下击碎”逻辑里；现在连 OP 自己放的 TNT 都炸不掉）；
- **不可推动**：爆炸会无条件给范围内实体 `motionX/Y/Z` 加冲量（跟伤害结果无关），所以
  `moveEntity` / `addVelocity` 也被重写成空实现，画框不会被炸飞或推着走；
- **拦截直接删除**：服务端 `setDead()` 只接受本实体自己 OP 破坏路径的调用，其他模组/效果直接
  `entity.setDead()`、`world.removeEntity(entity)` 或清理命令都会被忽略，并打一条 warn 日志；
  客户端侧保留正常 `setDead`，否则销毁包（离开同步范围）会留下幽灵画框；
- 新实体 `admin_painting` 复用普通画的渲染器与同步范围（`common.advanced.paintingTrackingRange`）。

## 1.7.10 差异

| 1.21 能力 | 1.7.10 处理 |
| --- | --- |
| datapack 画作/画框 | 改为扫描 jar 内 `assets/immersivepaintingsnh/`，额外包放 `config/ImmersivePaintingsNH/` |
| WebP 支持（TwelveMonkeys） | 不支持，仅 PNG/JPG |
| Joy of Painting / Zetter 画作导入 | 不支持（1.7.10 无对应模组） |
| 拖拽图片进 GUI | 改为输入框填写路径/URL（支持引号、全角标点、相对游戏目录路径） |
| 发光墨囊 | 用萤石粉替代 |
| 配方/进度 JSON | 配方改为代码注册；进度 json 只是配方解锁，跳过 |
| 客户端→服务端 32k 包上限 | 内置 PacketFixer 式 mixin（`com.Lilith.immersivepaintings.mixin`），双向放宽到约 2MB |

## 许可

原 mod 与移植代码均为 GPL-3.0；OBJ 解析器 `owens.oobjloader` 为 Unlicense。
PacketFixer 式发包修复的思路参考自 [TonimatasDEV/PacketFixer](https://github.com/TonimatasDEV/PacketFixer)（LGPL-3.0）。
