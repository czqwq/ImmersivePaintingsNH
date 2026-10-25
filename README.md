# Immersive Paintings NH

Minecraft **1.7.10** Forge 版的 [Immersive Paintings](https://github.com/Luke100000/immersivePaintings) 移植。
用你自己的图片装饰世界：上传图片、选择尺寸/分辨率、套上画框，甚至做成天花板/地板上的涂鸦。

## 功能

- 4 种物品：绘画、发光绘画、涂鸦、发光涂鸦
- 完整 GUI 编辑器：选图、裁剪/缩放、抖动/减色、像素画模式、画框材质、深度偏移
- 数据驱动画框（OBJ 模型 + 材质纹理）与内置画作包
- 服务器端图片存储、分片网络同步、客户端缩略图/LOD 缓存
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

## 1.7.10 差异

| 1.21 能力 | 1.7.10 处理 |
| --- | --- |
| datapack 画作/画框 | 改为扫描 jar 内 `assets/immersivepaintingsnh/`，额外包放 `config/ImmersivePaintingsNH/` |
| WebP 支持（TwelveMonkeys） | 不支持，仅 PNG/JPG |
| Joy of Painting / Zetter 画作导入 | 不支持（1.7.10 无对应模组） |
| 拖拽图片进 GUI | 改为输入框填写路径/URL |
| 发光墨囊 | 用萤石粉替代 |
| 配方/进度 JSON | 配方改为代码注册；进度 json 只是配方解锁，跳过 |

## 许可

原 mod 与移植代码均为 GPL-3.0；OBJ 解析器 `owens.oobjloader` 为 Unlicense。
