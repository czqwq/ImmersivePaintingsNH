# 移植清单（1.21.1 → 1.7.10）

上游：`tmp/ImmersivePaintings-1.21.1`（common 58 个 Java 文件 + neoforge/fabric 平台层 + 资源）。
目标：`src/main/java/com/Lilith/immersivepaintings`（modid `ImmersivePaintingsNH`，资源域 `immersivepaintingsnh`）。

## Java 文件映射

### 直接对应（改 API）

| 上游 common | 目标 |
| --- | --- |
| `Main` | `Main` |
| `Painting`（record + Codec） | `Painting`（record + NBT 序列化） |
| `PaintingRegistration` | `PaintingRegistration` |
| `ClientPaintingManager` | `ClientPaintingManager` |
| `ServerPaintingManager`（SavedData） | `ServerPaintingManager`（WorldSavedData） |
| `client/gui/ImmersivePaintingScreen` | `client/gui/ImmersivePaintingScreen`（GuiScreen 重写） |
| `client/gui/widget/ExtendedSliderWidget` | 同名（GuiButton + 拖拽） |
| `client/gui/widget/IntegerSliderWidget` | 同名 |
| `client/gui/widget/PercentageSliderWidget` | 同名 |
| `client/gui/widget/PaintingWidget` | 同名（手写 gl 缩放贴图） |
| `client/gui/widget/TexturedButtonWidget` | 同名 |
| `client/render/ImmersivePaintingEntityRenderer` | 同名（Tessellator + GL11 重写） |
| `config/ClientConfig`、`config/CommonConfig` | `config/*`（Forge Configuration） |
| `dev/DatapackPaintingsGenerator` | `dev/DatapackPaintingsGenerator` |
| `entity/ImmersivePaintingEntity` 及 3 个子类 | `entity/*`（继承 Entity，dataWatcher 同步） |
| `item/ImmersivePaintingItem` 及 3 个子类 | `item/*`（onItemUse） |
| `network/NetworkHandler`、`LazyNetworkManager`、`SegmentManager` | 同名（SimpleNetworkWrapper） |
| `network/payload/ImmersivePayload` | 同名接口（IMessage + 静态读写 helper） |
| `network/payload/c2s/*`（6 个） | `network/payload/c2s/*`（record → class + Handler） |
| `network/payload/s2c/*`（4 个） | `network/payload/s2c/*` |
| `registration/Command` | 同名（CommandBase） |
| `registration/Configs` | 同名（Forge Configuration 注册） |
| `registration/Entities` | 同名（EntityRegistry） |
| `registration/Items` | 同名（GameRegistry + CreativeTabs） |
| `registration/Network` | 同名（SimpleNetworkWrapper 注册） |
| `registration/RegisterHelper` | 同名 |
| `registration/Renderers` | 同名（RenderingRegistry） |
| `resources/FrameLoader`、`ObjectLoader`、`PaintingsLoader` | 同名（classpath 扫描） |
| `util/Cache`、`ImageManipulations` | 同名（去掉 NativeImage/TwelveMonkeys 依赖） |
| `util/PaintingArgumentType` | 同名（Brigadier → 命令补全 helper） |
| `owens/oobjloader/*`（7 个） | 原样复制，包名不变 |

### 合并进平台层（fabric/neoforge → 1.7.10）

| 上游 | 目标 |
| --- | --- |
| `neoforge/CommonNeoForge`、`EventBus` | `init/CommonProxy` + Forge 事件 |
| `neoforge/ClientNeoForge` | `init/ClientProxy`（渲染器/资源加载注册） |
| `fabric/*`（8 个） | 同上（1.7.10 只有 Forge） |

### 删除（1.7.10 无对应物）

| 上游 | 原因 |
| --- | --- |
| `compat/JoyOfPaintingCompat`、`ZetterCompat`、`PaintingImporter`、`PaintingImports` | 依赖 1.12+/1.20+ 模组，1.7.10 不存在 |
| `mixin/DimensionDataStorageAccessor` | 只被 Zetter 兼容使用 |
| `mixin/EntityTypeMixin` | 1.7.10 用 `EntityRegistry` 的 updateFrequency 实现同等效果 |
| `platform/IPlatformHelper`、`Services` | 单加载器，不需要 ServiceLoader |

### 新增适配文件

`init/Config`（Forge 配置入口）、`client/gui/widget/CheckboxWidget`、`registration/Recipes`、
`util/Json`（Gson 帮助类）、`util/ModResources`（jar/classpath 扫描）、`util/Permissions`（权限等级）、
`util/ScheduledTasks`（网络线程 → 主线程队列）。

## 资源映射

| 上游 | 目标 |
| --- | --- |
| `assets/immersive_paintings/textures/{block,gui,item}` | `assets/immersivepaintingsnh/textures/{block,gui,items}`（item → items） |
| `assets/immersive_paintings/frames/**/*.json` | 同路径，`immersive_paintings:` → `immersivepaintingsnh:` |
| `assets/immersive_paintings/objects/**/*.obj` | 同路径原样复制 |
| `assets/immersive_paintings/lang/*.json`（121 个） | `assets/immersivepaintingsnh/lang/<Locale>.lang`（JSON → .lang，key 前缀替换） |
| `data/immersive_paintings/paintings/*.{json,png}` | `assets/immersivepaintingsnh/paintings/` |
| `data/.../recipe/*.json` | `registration/Recipes`（代码注册，染料用 `dye` 矿辞） |
| `data/.../advancement/*.json` | 跳过（只是配方解锁条件，1.7.10 无此系统） |
| `data/.../tags/item/dye.json` | 用 Forge `OreDictionary` 的 `dye` 替代 |
| `data/sable/**`、`data/.../vs_entities/**` | 跳过（Valkyrien Skies / Sable 兼容） |
| `assets/immersive_paintings/models/item/*.json` | 跳过（1.7.10 无 JSON 物品模型，改用 `setTextureName`） |
| `resources/icon.png` | `resources/logo.png`（mcmod.info logoFile） |
| `datapack/faithfulAddon`、`datapack/exampleDataPack` | 不打包（1.7.10 无数据包；对应流程见 README 的画作包目录） |

## 关键 API 替换

| 1.21 | 1.7.10 |
| --- | --- |
| `ResourceLocation` / `Main.locate` | 同名，域名为 `immersivepaintingsnh` |
| `SavedData` + `DimensionDataStorage` | `WorldSavedData` + `MapStorage` |
| `SynchedEntityData` | `DataWatcher`（String/Integer/Byte） |
| `EntityType` + `registerModEntity` 取代 `EntityRegistry` | `EntityRegistry.registerModEntity` |
| `CustomPacketPayload` + `StreamCodec` | `IMessage`/`IMessageHandler` + `ByteBufUtils` |
| `IThreadListener.enqueueWork` | 服务端/客户端主线程任务队列（`ScheduledTasks`） |
| `PoseStack`/`VertexConsumer`/`RenderType` | `GL11` + `Tessellator` |
| `DynamicTexture(NativeImage)` | `DynamicTexture(BufferedImage)`（1.7.10 自带） |
| `GuiGraphics`、`Button`、`Checkbox`、`AbstractSliderButton` | `GuiScreen`、`GuiButton` + 自绘控件 |
| `Screen.onFilesDrop` | 无（输入框填写路径/URL） |
| `fzzy_config` | `net.minecraftforge.common.config.Configuration` |
| `Component.translatable` | `StatCollector.translateToLocal(Formatted)` |
| `player.hasPermissions` | OP 检查（`ServerConfigurationManager.func_152596_g`） |

## 构建适配（重要）

模板默认开启 `enableModernJavaSyntax = jvmDowngrader`，会产出 **multi-release jar**
（`META-INF/versions/21`、`/25` 内含 Java 21+ class）。1.7.10 Forge 的 `JarDiscoverer`
用旧版 ASM 扫描 jar 内**所有** class，遇到 Java 21 class 会直接判定 jar 损坏并忽略整个 mod。
因此新增 `addon.late.gradle`，在 JVMDowngrader 任务上关闭 multi-release 输出：

```groovy
tasks.withType(xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar).configureEach {
    multiReleaseOriginal.set(false)
    multiReleaseVersions.set([])
}
```

效果：jar 根目录只有降级到 Java 8 的 class（Java 9+ API 调用被 jvmdg 改写到 GTNHLib 提供的
stub），Java 8~21 运行时均可加载，且 Forge 能正常发现 mod。代价是新 JVM 上失去原生 class
快路径（功能等价）。运行环境需要 **GTNHLib ≥ 0.11**（stub 提供方，GTNH 整合包自带）。

## 网络包大小修复（PacketFixer 移植）

1.7.10 的 `C17PacketCustomPayload`（客户端 → 服务端）把 payload 硬编码限制在 32k，上传图片分片
（默认 64KB）会触发 `Payload may not be larger than 32k` 并断开连接。服务端 → 客户端方向 Forge
早已用 var-short 编码放宽到约 2MB（`S3FPacketCustomPayload`）。

参考 [PacketFixer](https://github.com/TonimatasDEV/PacketFixer) 的 mixin 思路，在
`com.Lilith.immersivepaintings.mixin` 包中实现：

| Mixin | 作用 |
| --- | --- |
| `C17PacketCustomPayloadMixin`（common） | 把读/写长度字段换成 FML 的 `ByteBufUtils.readVarShort/writeVarShort`；<32768 时与原生 short 编码完全一致，其他 mod/未打补丁的端不受影响 |
| `C17PacketCustomPayloadClientMixin`（client） | 把 `@SideOnly(CLIENT)` 构造函数里 `32767` 的上限常量改成 Forge 同款 `0x1FFF9A`（约 2MB） |

因此 `packetSize` 配置可以保持上游默认的 64KB。双向限制对照：客户端→服务端约 2MB（本 mixin），
服务端→客户端约 2MB（Forge 原生），Netty 帧上限仍是 3 字节 varint（2^21-1）。

另外图片路径输入做了归一化：去掉首尾引号、全角冒号/反斜杠、`file://` 前缀、Windows 盘符前的多余
`/`；相对路径会再尝试相对游戏目录解析。

## 验证

- `./gradlew build` 通过（含 Spotless、Checkstyle、JVM Downgrader、Mixin refmap/reobf）。
- 产物 `build/libs/ImmersivePaintingsNH-*.jar` 已确认：无 `META-INF/versions/*`、类文件 major version 52、
  含 `mixins.ImmersivePaintingsNH.json` 与 `mixins.ImmersivePaintingsNH.refmap.json`。
- 已根据实机反馈修复：放置实体 DataWatcher id 冲突、右键无法交互（1.7.10 `canBeCollidedWith`
  兼作准星拾取判定）、上传分片超过 32k 断线。渲染朝向、GUI 各页签、图片上传链路仍建议继续回归。
