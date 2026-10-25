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

## 验证

- `./gradlew build` 通过（含 Spotless、Checkstyle、JVM Downgrader、reobf）。
- 产物 `build/libs/ImmersivePaintingsNH-*.jar` 已确认：无 `META-INF/versions/*`、类文件 major version 52。
- 按约定本轮只要求编译验证，未做客户端/服务端运行时冒烟测试；渲染与 GUI 为 1.7.10 手写实现，
  首次实机运行需重点回归：放置朝向、天花板/地板旋转、GUI 页签、图片上传链路。
