# bs-ui 架构与设计细节

> 本文补充 [getting-started.md](./getting-started.md) 和 [components.md](./components.md) 没展开的部分：**主题系统的设计哲学、Skin 加载链路、全局 API、Skin 与字体的生命周期**，以及一些设计决策的「为什么」。

---

## 一、模块结构

```
bs-ui/
├── common/   平台无关接口（Platform、文件选择器签名、SkinUtil）
├── core/     ★ bs-ui 库本体：所有 Bs* 组件、主题、Skin 工厂/加载器、图表、i18n
├── demo/     平台无关演示（Game + Screen），展示全部能力
├── lwjgl3/   Desktop 启动器 + 平台实现 + iconpkg 打包工具（依赖 Batik）
└── teavm/    Web 启动器（TeaVM/WebGL 后端）
```

| 模块 | 职责 | 给谁用 |
|---|---|---|
| `common` | 平台无关接口（`Platform`、`SkinUtil` 等） | core 依赖它做平台抽象 |
| `core` | **库本体**，所有 `Bs*` 组件、主题、Skin 工厂/加载器、图表 | 业务依赖这个 |
| `demo` | 平台无关演示，90+ 组件的完整示例 | 学习参考 |
| `lwjgl3` | Desktop 启动器 + `BsIconPackager`（SVG→atlas，依赖 Apache Batik） | 桌面运行 |
| `teavm` | Web 启动器，复用 core 的所有组件 | Web 运行 |

### 跨平台架构：core + common 是平台无关核心

`core` 和 `common` 是**平台无关的核心库**，支持桌面（LWJGL3）、Web（TeaVM/WebGL）、移动端（Android/iOS）等 libGDX 所覆盖的所有后端。组件、主题、图表、Skin 加载等全部在 core 实现，不依赖任何平台特定代码。

**平台差异由各平台项目自行实现**，通过 `common` 的 `Platform` 接口注入。`Platform` 接口定义了平台能力的抽象（窗口图标、文件选择、定时任务、JSON 序列化、系统暗色模式等）：

```java
public interface Platform {
    String getPlatformName();
    void exit();
    boolean setWindowIcons(String windowTitle, String iconPath);
    String chooseJarFile();
    void schedule(Runnable r, long delay, long period, TimeUnit unit);

    default boolean isSystemDarkMode() { return false; }

    // JSON 序列化的平台差异点 ↓
    default String toJson(Object object) {
        return new Json().toJson(object);          // 默认走 libGDX 的 Json（全平台可用）
    }
    default <T> T fromJson(String json, Class<T> type) {
        return new Json().fromJson(type, json);
    }
}
```

各平台项目提供自己的 `Platform` 实现并注册（如桌面端 `DeskPlatform`、Web 端 `TeaVmPlatform`），按需覆写差异方法。例如：

```java
// 桌面端（lwjgl3 模块）：用 fastjson2 代替 libGDX 的 Json（功能更全、生态更好）
public class DeskPlatform implements Platform {
    @Override public String toJson(Object object) {
        return JSON.toJSONString(object);          // fastjson2
    }
    @Override public <T> T fromJson(String json, Class<T> type) {
        return JSON.parseObject(json, type);
    }
    // ... 其余平台能力
}
```

> 设计意图：**核心库不绑定具体平台的 JSON 库 / 文件选择器 / 调度器**。core 用 libGDX 自带的 `Json` 作为全平台兜底默认实现；如果某平台有更好的选择（如桌面端用 fastjson2），由该平台项目覆写 `Platform` 的对应方法即可，core 无需改动。Web 端同理——`teavm` 模块自带最小 `org.slf4j` 替换实现，避免官方 slf4j 在 TeaVM 下的类冲突。

> **关键**：业务逻辑（`demo` 的 Game/Screen）和 UI 组件是平台无关的，桌面、Web、移动端**多端复用同一套代码**，各端只需提供启动器和 `Platform` 实现。

### 新增平台（如 Android/iOS）怎么做

1. 新建平台模块（如 `android/`），配置 libGDX 对应后端（`gdx-backend-android`）。
2. 实现 `Platform` 接口（`AndroidPlatform`），覆写该平台的差异能力。
3. 写启动器（`AndroidApplication` 子类），`PlatformStatic.registerImpl(AndroidPlatform.class)` 注入。
4. core 的所有组件直接可用，无需改动。

---

## 二、主题系统（BsTheme）

### 设计哲学：颜色不写在代码里，注册到 skin 的 Color 桶

bs-ui 的主题系统核心思想：**颜色是 skin 的 Color 桶里的资源，不是硬编码常量**。

- 组件统一用 `skin.get("bs-primary", Color.class)` 取色，而不是 `new Color(...)`。
- 切换主题 = 换一个填了不同 hex 的 skin。
- 用户改 skin json 的颜色值就能定制，不用改组件代码。

### BsTheme 接口

`BsTheme`（`core/.../BsTheme.java`）定义主题契约：

```java
public interface BsTheme {
    String name();                  // "bs-light" / "bs-dark" / "bs-admin"
    boolean isDark();               // 是否暗色主题
    void applyColorsToSkin(Skin);   // 把所有色 token 注入 skin 的 Color 桶

    // 一堆无参静态取色方法（内部走 BsUI.getSkin() 自动取当前主题色）：
    static Color tp() { ... }       // text-primary（主文本色）
    static Color ts() { ... }       // text-secondary
    static Color tm() { ... }       // text-muted
    static Color bb() { ... }       // bg-body（页面背景）
    static Color bs() { ... }       // bg-surface（卡片表面）
    static Color be() { ... }       // bg-elevated（悬浮层）
    static Color pri() { ... }      // primary 主色
    // ...
}
```

> **无参取色是 VISUI 风格的精髓**：组件不持有 skin，任意位置 `BsTheme.tp()` 就能拿到当前主题的主文本色。切换主题后，下次调用自动返回新色。

### Color token 命名约定（skin Color 桶的 key）

| 类别 | token | 含义 |
|---|---|---|
| 文本 | `bs-text-primary` / `-secondary` / `-muted` / `-disabled` / `-on-primary` / `-on-dark` | 文本层级 |
| 背景 | `bs-bg-body` / `-surface` / `-elevated` / `-hover` / `-header` | 背景层级（body < surface < elevated） |
| 边框 | `bs-border` / `bs-border-strong` | 边框 |
| 6 色 | `bs-primary` / `-secondary` / `-success` / `-danger` / `-warning` / `-info` | Variant 主色 |
| 特殊 | `bs-overlay` / `-shadow` / `-link` / `-link-hover` / `-focus-ring` / `-cursor` | 遮罩/链接/焦点 |

### 自定义主题

继承 `BsAbstractTheme`，构造器里用 `put(token, hex)` 填一张 hex 值表即可，`applyColorsToSkin` 会自动把所有 hex（支持 6/8 位含 alpha）转 Color 注册到 skin：

```java
public class MyCustomTheme extends BsAbstractTheme {
    public static final MyCustomTheme INSTANCE = new MyCustomTheme();
    private MyCustomTheme() {
        super("my-theme", false);          // name, isDark
        put("bs-primary", "#FF6600");
        put("bs-bg-body", "#FFFFFF");
        put("bs-text-primary", "#1A1A1A");
        // ... 填全所有 token
    }
}
```

派生色（hover/active/soft-bg）由 `BsTheme` 用 HSL 数学自动计算：`hoverOf`(L+0.07)、`activeOf`(L−0.07)、`softBgOf`(主色:白 1:9 混合，Bootstrap alert 风格)。

### 三个内置主题

| 主题 | name | 风格 | 来源 |
|---|---|---|---|
| **BsLightTheme** | `bs-light` | Bootstrap5 默认配色，primary `#0D6EFD` | Bootstrap |
| **BsDarkTheme** | `bs-dark` | 背景分层 body `#212529`<surface<elevated，primary 提亮 `#3D8BFD` | Bootstrap Dark |
| **BsAdminTheme** | `bs-admin` | Element 蓝 `#409EFF`，蓝灰冷调 | Element Plus Dark + Ant Design Pro |

### 主题与 skin 的关系：一个主题对应一个 skin

`registerTheme(name, theme, skin)` 把二者按 name 绑定存进 Map。**切换主题不是在原 skin 上改色，而是整体把 `currentSkin` 指针换到该主题预构建好的另一个 `Skin` 实例**。每个主题 skin 由 `BsSkinFactory.augmentWithBsStyles(skin, theme)` 生成：先 `theme.applyColorsToSkin(skin)` 注入色桶，再程序化生成 drawable + style。

---

## 三、Skin 加载链路

bs-ui 有四个 Skin 相关类，职责清晰：

| 类 | 职责 |
|---|---|
| **BsSkin** | 带**全局字体缓存**的 Skin（继承 libGDX `Skin`），解决多主题字体共享 |
| **BsSkinLoader** | 从 `json + atlas + png + ttf + 字符集` 加载皮肤；`loadAllThemes()` 自动注册三主题 |
| **BsSkinFactory** | 无状态 Skin 构造器：`augmentWithBsStyles(skin, theme)` 在已有 skin 上叠加 bs-ui 样式 |
| **BsSkinExporter** | 导出含 FreeType 字体引用的皮肤（供 Skin Composer 二次编辑） |

### 加载方式

```java
// 方式 1：标准加载（.fnt + .png 已烘焙好）
Skin skin = BsSkinLoader.load(Gdx.files.internal("skins/my-skin.json"));

// 方式 2：FreeType 加载（从 .ttf + 字符集文件运行时生成字体）
Skin skin = BsSkinLoader.loadWithFreeType(Gdx.files.internal("skins/bs-light.json"));

// 方式 3：直接用 BsSkin（推荐，走全局字体缓存）
Skin skin = new BsSkin(Gdx.files.internal("skins/bs-light.json"));
```

### BsSkinLoader.loadAllThemes() 做了什么

`BsUI.init()` 内部调它，自动注册三主题，等价于：

```java
for (BsTheme theme : List.of(BsDarkTheme.INSTANCE, BsAdminTheme.INSTANCE, BsLightTheme.INSTANCE)) {
    if (BsUI.hasTheme(theme)) continue;
    BsSkinLoader.loadAndRegisterBsTheme(SKIN_CP, theme, fontHashMap);
}
```

而 `loadAndRegisterBsTheme` 就是 [getting-started.md](./getting-started.md#二skin-初始化推荐三步法注入自定义字体) 的三步法封装。

### augmentWithBsStyles 的「不覆盖」约定

```java
BsSkinFactory.augmentWithBsStyles(skin, theme);
```

这个方法在已有 skin 上叠加 bs-ui 全套样式（主题色、圆角 NinePatch、6 色按钮 style、各组件 Style）。**关键约定：已存在的 key 不覆盖**——你 JSON 里已有的资源优先。所以：

```
先加载你的 JSON（你的字体/drawable）  →  再 augmentWithBsStyles（补 bs-ui 缺的）
```

是安全的叠加顺序。它会：
- `theme.applyColorsToSkin(skin)` —— 注册主题色 token
- 选 default font（优先级 `lxgw` → `font` → `default` → `default-font`，找不到 `new BitmapFont()` 兜底）
- `ensureSizeFont` —— 确保 6 档字号字体（`font-xs/sm/md/lg/xl/xxl`）存在，已存在则跳过（保留你的）
- 注册 white Drawable、圆角 NinePatch、箭头、CheckBox/RadioButton 图标
- 注册 6 色 × {实心/描边/幽灵} TextButtonStyle（`bs-btn-{color}` 等）
- 注册 default LabelStyle/TextFieldStyle/CheckBoxStyle/SliderStyle 等
- `registerAllSizeVariants` —— 派生所有 `*-sm/md/lg/xl/xxl` 尺寸变体（只换 font，drawable 复用）

---

## 四、字体全局共享机制（生命周期详解）

> 这里的细节是 [getting-started.md 第三节](./getting-started.md#三字体全局共享机制重点避免崩溃) 的展开，源码依据见 `BsSkin.java`。

### 两套并存的共享机制

bs-ui 实际上有两条字体共享路径，**不互通**：

| 机制 | 触发条件 | 字体存在哪 | 谁释放 |
|---|---|---|---|
| **`BsSkin.CACHE_FONT`（静态）** | 用 `new BsSkin(json)` 加载（默认 `useCacheFont=true`） | `BsSkin` 的 static Map，所有 BsSkin 共享 | `BsSkin.disposeFontCache()` / `BsUI.disposeAllSkins()` |
| **App 级 `Map<String,BitmapFont>`** | 用 `BsUI.buildSkin()` / `registerDefaultSkin()`（原生 `new Skin()`） | App 自己持有的 Map，注册到每个 skin | App 在 `dispose()` 手动遍历摘引用 + 统一 dispose |

> 需要跨主题共享字体，**推荐用 BsSkin 路径**（方式 A），它自动走全局缓存。

### useCacheFont 开关

```java
public BsSkin(FileHandle skinFile) { this(skinFile, true); }   // 默认 true
public BsSkin(FileHandle skinFile, boolean useCacheFont) { ... }
```

- `true`（默认）：字体进全局缓存，dispose 时自动摘引用保护共享字体。
- `false`：字体归本 skin，`dispose()` 正常连带释放（不共享）。

### dispose 的三种姿势

```java
// 1. 单个 BsSkin.dispose()（useCacheFont=true 时）
//    只摘引用 + dispose skin 本身，共享字体不释放
skin.dispose();

// 2. 释放所有全局缓存字体（skin 对象不销毁）
BsSkin.disposeFontCache();

// 3. bs-ui 推荐的退出范式：摘字体引用 + dispose 字体（skin 仍由 Game 持有）
BsUI.disposeAllSkins();
```

### 陷阱：`disposeAllSkins()` 的方法名有误导

它实际**只 dispose 字体，不 dispose skin**——内部遍历所有 skin 摘字体引用后 dispose 字体，skin 对象本身仍由 Game 持有。这是故意的：skin 生命周期归 Game 管，`BsUI` 只管字体。

### 陷阱：嵌入 atlas 的字体无法安全共享

如果字体 Texture 来自 skin 的 `TextureAtlas`（JSON 里 region 命中 atlas），即使从 skin 摘除字体引用也救不了——`super.dispose()` 会 dispose atlas，连带毁掉字体 Texture。

> **多 skin 共享的字体必须用独立 Texture**（FreeType 生成的、或独立 `.png` 的 `.fnt`）。
> bs-ui 自带字体就是独立 `.png`（`font-XX.fnt` + `font-XX_0.png`），所以安全。

---

## 五、BsUI 全局门面 API 速查

`BsUI`（`core/.../BsUI.java`）是 VISUI 风格的全局静态访问，参考 libGDX 社区的 `VisUI`。

### 生命周期

| 方法 | 作用 |
|---|---|
| `BsUI.init()` | 初始化 + `loadAllThemes()`（注册三主题 + 自带字体） |
| `BsUI.dispose()` | 释放全局 ShapeRenderer、清空监听器和注册表（**不 dispose skin**） |
| `BsUI.disposeAllSkins()` | 释放所有 skin 跨主题共享的缓存字体 |

### 全局访问

| 方法 | 作用 |
|---|---|
| `BsUI.getSkin()` | 当前激活 skin（组件从这里取，未初始化抛异常） |
| `BsUI.currentTheme()` / `currentThemeName()` | 当前主题对象 / 名字 |
| `BsUI.shapeRenderer()` | 全局共享 `ShapeRenderer`（图表/进度条/评分等共用，把 N 个 native GL 资源收敛成 1 个） |

### 注册 API

| 方法 | 作用 |
|---|---|
| `registerTheme(name, theme, skin)` | 注册主题+skin，首个自动成为当前 |
| `registerThemeWithDefaultFont(name, theme, skin)` | 注册并自动 `augmentWithBsStyles` |
| `registerDefaultSkin(font)` / `(font, sizeFonts)` | 用默认字体 + Light 主题创建并注册 |
| `buildSkin(theme, font, sizeFonts)` | 用指定主题构建新 Skin |
| `registeredThemeNames()` / `registeredSkins()` / `hasTheme(theme)` | 查询注册表 |

### 切换 API

| 方法 | 作用 |
|---|---|
| `setTheme(String)` / `setTheme(BsTheme)` | 切换主题（整体换 skin + 回调监听器） |
| `get().addOnThemeChangeListener(Consumer<BsTheme>)` | 注册切换监听（通常在回调里 `setScreen` 重建 UI） |

---

## 六、设计决策：为什么这么设计

### Q：为什么组件不持有 Skin 字段，而用全局 `BsUI.getSkin()`？

参考 VISUI。好处：
- 组件实例化时少传一个参数（`new BsButton("OK")` 而不是 `new BsButton("OK", skin)`）。
- 切主题时不用逐个组件换 skin——重建 screen 即可，所有组件自动用新 skin。
- 组件库内部协作时不用层层传 skin。

### Q：为什么切主题是「整体换 skin」而不是「在原 skin 改色」？

libGDX 的 `Skin` 是不可变资源包（drawable 是 NinePatch/TextureRegion，一旦生成就固定）。要支持 Light/Dark 这种背景色完全不同的主题，最干净的方式是每个主题预构建一个独立 Skin，切换时整体换指针。bs-ui 早期试过「rebuildDrawables」在原 skin 改色，但维护复杂、易出 bug，最终改为整体换 skin。

### Q：为什么 `BsSkin.dispose()` 默认不释放字体？

因为多主题共享。Light/Dark/Admin 三个 skin 引用同一组字体实例，任何一个 skin 的 dispose 都不能销毁字体——否则其余 skin 崩溃。所以 `BsSkin` 把共享字体从自身摘除后才 dispose，真正的字体释放延后到 `disposeFontCache()` / `disposeAllSkins()`。

### Q：为什么图表用 ShapeRenderer 自绘，不用第三方库？

- **零依赖**：不引入 MPAndroidChart 之类，减少包体积和兼容性问题。
- **TeaVM/WebGL 兼容**：bs-ui 要跑在 Web 端（teavm 模块），第三方原生库可能无法编译到 TeaVM。
- **随主题变色**：自绘直接用 `BsTheme` 取色，切主题自动新色。
- 代价是图表功能不如专业库丰富，但常见类型（折线/柱状/饼/环形/雷达/散点/面积/平滑曲线/3D 柱）够用。

---

## 七、跨平台：Desktop + Web 双端

bs-ui 的核心价值之一是**同一套 UI 代码跑在 desktop 和 web**：

```
                        ┌─ lwjgl3（Desktop, LWJGL3 后端）
demo 的 Game/Screen ────┤
                        └─ teavm（Web, TeaVM/WebGL 后端）
```

- `common` 模块定义平台无关接口（`Platform`、文件选择器签名）。
- `lwjgl3` 和 `teavm` 各提供平台实现。
- 业务 UI（组件、Screen）完全平台无关，两端复用。

> 注意：desktop 独有的 `BsIconPackager`（SVG→atlas，依赖 Apache Batik）只在 lwjgl3 模块，因为 Batik 是重量级 AWT 库，无法编入 TeaVM。

---

## 八、Vendored 代码（拷贝的第三方源码）

为保证 TeaVM/Web 端可编译与零外部依赖，bs-ui 直接拷贝了少量第三方源码（均保留原协议）：

| 代码 | 来源 | 协议 | 文件 |
|---|---|---|---|
| `BitmapFontWriter` | libGDX gdx-tools | Apache 2.0 | `core/.../bmfont/BitmapFontWriter.java` |
| SLF4J jul 绑定 | SLF4J | MIT | `lwjgl3/.../com/git/log/jul/*` |
| libGDX LWJGL3 后端片段 | libGDX | Apache 2.0 | `lwjgl3` 模块内 vendored 后端类 |

详见 [NOTICE](../NOTICE) 和 [README](../README.md) 的致谢段。

---

## 九、License 合规：About 界面

bs-ui 基于 Apache License 2.0，但有一个**附加署名条件**：任何把 bs-ui 直接或修改后使用、并分发给最终用户的产品，**必须在程序内的 About/Credits 页面标注** bs-ui。

库自带 `BsAboutDialog`，一行代码合规：

```java
BsAboutDialog.show(stage, skin, "我的应用", false);   // false=未改源码
```

详见 [LICENSE](../LICENSE) 的「ADDITIONAL ATTRIBUTION CONDITION」段。

---

## 相关文档

- [getting-started.md](./getting-started.md) —— 快速入门、Skin 初始化、字体全局共享
- [components.md](./components.md) —— 组件总览与 libGDX 对比
- [bs-custom-style-export.md](./bs-custom-style-export.md) —— 自定义皮肤导出方案
- [bs-ui-defects-audit.md](./bs-ui-defects-audit.md) —— 组件缺陷审计
- [admin-template.md](./admin-template.md) —— Admin 后台模板
