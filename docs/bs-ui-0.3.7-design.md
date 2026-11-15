# bs-ui 0.3.7 升级设计：分卷 pak 集成（bs-res）+ 皮肤注册扩展点（core）

> 2026-11-16 设计稿。来源：cwwg9 游戏侧已落地的 pak 链路（多分卷 + 私有 key + 目录感知 + 三态开关）
> 与皮肤注册诉求，能力上移 bs-ui，游戏侧删本地副本改依赖库。
> 版本：0.3.6 → **0.3.7**（暂定）。

---

## 一、背景

### 1.1 pak 链路现状（两套）

| 能力 | bs-ui 现状（bs-res） | cwwg9 本地副本（`cn.pingyuanren.cwwg9.pak`） |
|---|---|---|
| 分卷 | ❌ 单卷 `assets.pak`（`PakBootstrap`） | ✅ `paks.txt` 索引逐卷加载，`CompositeResourcePack` 合并视图 |
| 私有 key | ❌ 固定公开 `PakKeys.KEY`（混淆级） | ✅ key 注入（`Cwwg9PakKeys.KEY`，游戏私有） |
| 目录句柄感知 | ❌ 只认精确文件条目——`Gdx.files.internal("particle")` 目录请求 miss 回退磁盘句柄，散目录删除后 `child()` 落空崩溃（2026-11-03 实修的 bug） | ✅ `Cwwg9PakFiles` 三处（internal/getFileHandle/classpath）加 `hasDir` 前缀探测 |
| 资源模式开关 | ❌ 无 | ✅ loose/pak/auto 三态（`-Dcwwg9.assets` > `CWWG9_ASSETS` > auto） |

cwwg9 三个类：`CompositeResourcePack`（多卷合并 + hasDir/size/volumeCount）、`Cwwg9PakFiles`（目录感知 Files 包装）、`Cwwg9PakBootstrap`（读索引 + 逐卷解密 + 三态开关 + 优雅回退）。**前两个与游戏无关可整体上移；第三个的"游戏特定部分"（key、开关名、索引名）需要参数化。**

### 1.2 皮肤注册现状（core）

- 入口链：`BsUI.init()` → `BsSkinLoader.loadAllThemes()` → classpath 读三主题烘焙 json（`assets-skin` 模块）→ `BsSkinFactory.augmentWithBsStyles(skin, theme)` 运行时生成全套 drawable/style（**非烘焙路线**：烘焙 atlas 只是占位色块，真实圆角 NinePatch 全部运行时画）→ `BsUI.registerTheme()`。
- 覆盖语义**隐式**：`putIfAbsent` / `skin.has` 跳过 = "先载入的 json 优先，augment 只补缺"。**没有 augment 之后的覆盖层**——使用方想改 `bs-btn-primary` 的圆角/内边距，只能整份 json 抄写；想纯代码注册自己的样式也没有正式挂载点。
- 主题体系已有（`BsTheme` 接口 + `BsAbstractTheme` hex 值表 + `registerTheme` + `setTheme`），但"自定义主题 + 内置 augment 自动生成全套 bs 样式"的组合流不顺；cwwg9 实际完全绕开 `BsUI` 自建 `GameUi/Cwwg9Skin`。

---

## 二、pak 集成设计（bs-res 模块）

### 2.1 原则：融合进现有体系，不是并列第二套

把 cwwg9 三类的能力直接并入 `PakBootstrap/PakFiles/ResourcePack` 一族，**cwwg9 删本地副本**（保留 `Cwwg9PakKeys`）。避免 bs-res 里出现"老单卷 PakFiles / 新分卷 VolumePakFiles"两套并行包装层。

### 2.2 类与接口变更

```
bs-res/src/main/java/cn/pingyuanren/bs/res/
├── ResourcePack.java            # +default boolean hasDir(String dir) { return false; }
├── FileResourcePack.java        # 实现 hasDir（paths() 前缀扫描；惰性建 dirIndex Set 优化）
├── MemoryResourcePack.java      # 实现 hasDir（同理）
├── CompositeResourcePack.java   # 【迁入】cwwg9 原样（包名换 bs.res），hasDir 合并子卷
├── PakFiles.java                # 升级目录感知：三处查询 has(path) || hasDir(path)
├── ResourceMode.java            # 【新】enum { LOOSE, PAK, AUTO }
├── ResourceModeResolver.java    # 【新】@FunctionalInterface ResourceMode resolve()
├── SysPropsResourceModeResolver.java # 【新】通用实现：sysprop > env > AUTO
├── VolumePakConfig.java         # 【新】builder 配置（见 2.4）
├── VolumePakBootstrap.java      # 【新】= Cwwg9PakBootstrap 换名参数化（见 2.3）
└── PakBootstrap.java            # 不动（单卷老入口，向后兼容）
```

> 命名说明：`Cwwg9PakBootstrap` 在 bs-ui 中换名为 **`VolumePakBootstrap`**（"分卷 pak 启动器"，与现有 `PakBootstrap` 平行、语义对称）。

### 2.3 VolumePakBootstrap：init 传入策略类决定文件方式

```java
/** 资源模式三态。 */
public enum ResourceMode { LOOSE, PAK, AUTO }

/** 使用方实现：决定本次运行走哪种文件方式（init 时传入）。 */
@FunctionalInterface
public interface ResourceModeResolver {
    ResourceMode resolve();
}

/** bs-ui 内置通用实现：JVM 属性 > 环境变量 > AUTO（= cwwg9 resourceMode() 逻辑参数化）。 */
public final class SysPropsResourceModeResolver implements ResourceModeResolver {
    public SysPropsResourceModeResolver(String propKey, String envKey) { ... }
}
```

行为与 cwwg9 现版一致：

- `LOOSE`——强制散包，跳过 pak 安装（开发态改散资源立即生效）；
- `PAK`——强制 pak，探测不到索引/分卷时 warn 并回退明文磁盘；
- `AUTO`——探测到 `paks.txt` 即 pak，否则明文磁盘；
- 索引存在但部分分卷缺失：跳过缺失卷 + warn，全缺才整体回退；
- 任何异常 catch 后 warn 回退明文磁盘（启动期永不炸）。

### 2.4 VolumePakConfig（builder）

```java
VolumePakBootstrap.init(VolumePakConfig.builder()
        .indexPath("paks.txt")                                  // 分卷索引文件名
        .cipher(new ChaCha20Cipher(Cwwg9PakKeys.KEY))           // 密码器（key 游戏私有）
        .modeResolver(new SysPropsResourceModeResolver(         // 三态策略
                "cwwg9.assets", "CWWG9_ASSETS"))
        .logTag("cwwg9")                                        // 日志前缀（默认 "pak"）
        .build());
```

构造参数即 cwwg9 剥离出的全部游戏特定点；不传 cipher 时默认 `ChaCha20Cipher(PakKeys.KEY)`（与 `FileResourcePack.open(byte[])` 同口径）。

### 2.5 特定 key 清单（集成时的"关键 key"）

| key | 归属 | 集成后 |
|---|---|---|
| `assets.pak` | bs-ui 单卷默认名 | `PakBootstrap` 老入口不动 |
| `paks.txt` | cwwg9 分卷索引名 | `VolumePakConfig.indexPath`，默认 `paks.txt` |
| `PakKeys.KEY` | bs-ui 公开默认 key | 老入口默认值不动 |
| `Cwwg9PakKeys.KEY / SALT` | cwwg9 私有 key + 幂等打包 salt | **留在 cwwg9**——私有 key 是游戏侧价值（库不放游戏 key）；SALT 仅打包用（`PakWriter.write(entries, cipher, salt)` 已支持，bs-res 不动） |
| `-Dcwwg9.assets` / `CWWG9_ASSETS` | cwwg9 模式开关 | 参数化为 resolver 的 `propKey/envKey` |
| salt→nonce 派生（salt[0..8]+ordinal） | `ChaCha20Cipher` 既有 | 不动（幂等打包=固定 salt 已成立） |

### 2.6 cwwg9 侧迁移步骤（bs-ui 0.3.7 发布后）

1. 删 `core/.../cwwg9/pak/` 下 `CompositeResourcePack / Cwwg9PakFiles / Cwwg9PakBootstrap` 三类；`Cwwg9PakKeys` 保留；
2. 启动处（`GdxGame`/ui-test 入口）换 `VolumePakBootstrap.init(...)`（resolver 实例可存静态字段，`ui-test` 的 PakAssets 共用同一开关）；
3. 测试 `Cwwg9PakFilesDirTest` 改引 bs 类（用例逻辑不变，仍读 `../pak/` 真卷回归）；
4. `gradle.properties` `bsUiVersion=0.3.5 → 0.3.7`；
5. pak-only 环境复验一次（build/pakrun + `-Dcwwg9.assets=pak`）。

---

## 三、皮肤注册扩展点设计（core 模块）

### 3.1 设计原则

- **代码注册（非烘焙）路线保持默认**不动——`augmentWithBsStyles` 运行时生成 drawable 的机制是现状优点（主题换色即重生成全套）；
- 把现有隐式语义**显式化为分层**：低层先建、高层可覆盖；
- 新 API 只做"编排"，不改 `BsSkinFactory` 内部（mega-method 后续再拆，不在本次范围）。

### 3.2 BsSkinBuilder：分层注册 API

```java
BsUI.builder("my-theme")                        // 或独立 BsSkinBuilder 类
   .base(BsLightTheme.INSTANCE)                 // ① 内置主题，或使用方自定义 BsTheme
   .jsonLayer(skin1JsonHandle)                  // ③ 使用方 json 覆盖层（可叠多个，后载优先）
   .jsonLayer(skin2JsonHandle)
   .augmentLayer((skin, theme) -> {             // ④ 使用方代码层（可叠多个）
        skin.add("cwwg_bg_label", labelStyle);  //    新增或覆盖，最高优先
   })
   .register();                                 // 挂进 BsUI.registerTheme（首个注册自动成为当前主题）
```

加载顺序固定四级，**从低到高**：

| 层 | 内容 | 覆盖语义 |
|---|---|---|
| ① 基座 | `BsSkin`（烘焙 json 或空）+ 字体 | — |
| ② 内置 augment | `BsSkinFactory.augmentWithBsStyles(skin, theme)` 全套 bs 样式 | `putIfAbsent` 只补缺（不变） |
| ③ jsonLayer | 使用方 json 依次 `skin.loadRegions/load` | 同 key **覆盖**（libGDX Skin 加载天然 add 覆盖） |
| ④ augmentLayer | 使用方代码依次执行 | **显式覆盖**（`skin.add` 直接压） |

关键点：**jsonLayer 在内置 augment 之后加载**——此时桶里已有全部 `bs-*` drawable，覆盖 json 只需按名引用，不会出现"占位 atlas 名字解析不到直接抛异常"的问题（这正是现在"先 json 后 augment"约定做不到的：想覆盖就得抄全量）。

### 3.3 自定义主题

- 已有底子：`BsAbstractTheme`（builder 式 `put(token, hex)`）+ 派生色 HSL 计算 + `applyColorsToSkin` 自动注入 `bs-*` Color 桶；
- 本次补齐：`builder.base(myTheme)` 直接传自定义主题——augment 全自动按新色表重生成圆角/按钮/菜单全套（**换一组 hex = 一套新皮肤**）；
- `setTheme` 切换时各层重放（换主题重建 Skin 全链），行为与现状一致。

### 3.4 决策点（已定稿 2026-11-16）

1. **覆盖粒度**：✅ 定案 **style 整对象替换**（libGDX 惯例、json 同语义；不做字段级合并）；
2. **Builder 挂哪**：`BsUI.builder()` 静态入口（与 `registerTheme` 同居，发现性好）；
3. `BsUI.init()` 老路径不动，等价于"无 ③④ 层的 builder"（完全向后兼容）。

### 3.5 cwwg9 采纳（本次不改）

`GameUi/Cwwg9Skin` 可继续独立（绕开 BsUI 全局态是当初的刻意选择）；后续若要吃 BsUI 的主题切换/控件样式，可用 `builder.base(自定义主题).augmentLayer(注册 cwwg9 样式)` 平移。仅作示例写入文档，不在本次范围。

---

## 四、版本与发布

- `projectVersion=0.3.6 → 0.3.7`（0.3.6 刚提交 4dfc128 推送，尚未发 mavenLocal，直接跳 0.3.7）；
- 发布 `publish` → mavenLocal；cwwg9 侧 `bsUiVersion=0.3.7` 对齐；
- 测试计划：
  - bs-res 单测：`CompositeResourcePackTest`（多卷 has/read 顺序 + hasDir）、`VolumePakBootstrapTest`（LOOSE 跳过 / AUTO 探测 / PAK 缺卷回退）、`PakFilesDirTest`（目录句柄 child 链路，即 cwwg9 修的 bug 的库级回归）；
  - demo：`BsControlsSkinScreen` 加分卷 pak 演示开关 + builder 覆盖层演示页；
  - cwwg9：迁移后 170 测试全绿 + pakrun 实机复验。

## 五、实施清单（顺序）

| # | 内容 | 模块 |
|---|---|---|
| 1 | `ResourcePack.hasDir` default + `FileResourcePack/MemoryResourcePack` 实现（惰性 dirIndex） | bs-res |
| 2 | `CompositeResourcePack` 迁入 | bs-res |
| 3 | `PakFiles` 目录感知升级（三处 `has \|\| hasDir`） | bs-res |
| 4 | `ResourceMode/ResourceModeResolver/SysPropsResourceModeResolver/VolumePakConfig/VolumePakBootstrap` | bs-res |
| 5 | `BsUI.builder()` 分层注册（jsonLayer/augmentLayer） | core |
| 6 | demo 演示 + 单测 | demo/bs-res |
| 7 | 版本 0.3.7 + publish | 根 |
| 8 | cwwg9 迁移（删三类/换入口/测试改引/版本对齐/pakrun 复验） | cwwg9 |
