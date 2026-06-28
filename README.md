# bs-ui

> Bootstrap-style UI components for libGDX — bridging the gap between a game engine and a traditional GUI framework.

---

## 中文

### 这是什么

`bs-ui` 是一套跑在 **libGDX** 之上的 UI 组件库，把 Web 前端里 **Bootstrap** 的设计语言与组件形态（按钮、表单、表格、对话框、导航栏、卡片、图表……）搬进了游戏引擎的 Scene2D 体系。

它的意图很直接：**打通游戏引擎与传统 GUI 框架的壁垒**。

游戏引擎擅长渲染、动画、批绘制，但自带的 UI 控件粗糙、丑陋、缺组件；传统 GUI 框架（Swing / JavaFX / Web）控件丰富却进不了游戏的渲染管线。`bs-ui` 让你能在同一套 Scene2D Stage 里，既享受到 Bootstrap 那种"开箱即用、视觉统一"的控件生态，又不脱离 libGDX 的渲染、输入、资源管线——做工具链、编辑器、游戏内 UI、数据看板都顺手。

### 当前状态

**不是最终版，但已经能用了。**

核心组件（90+）、双主题（Light / Dark）、FreeType 中文字体、Skin 加载/导出、跨平台启动器均已落地，可以拿来构建真实界面。仍有打磨空间（性能、字符集瘦身、Web 端适配、文档），欢迎试用与反馈。

### 特性

- **90+ Bootstrap 风格组件**：Button / Form / InputGroup / SelectBox / Table / DataTable / Pagination / Navbar / MenuBar / Breadcrumb / Card / Modal / Window / Toast / Alert / Dialog / Tooltip / Popover / Tabs / Accordion / Collapse / Offcanvas / Drawer / Carousel / Slider / Progress / Spinner / Badge / Tag / Avatar / Tree / Steps / Timeline / Transfer / ColorPicker / DatePicker / FileItem …
- **9 种图表**：Line / Bar / Area / Pie / Doughnut / Scatter / Spline / Radar（基于 Scene2D 自绘，无第三方图表库）
- **4 种对话框**：Alert / Confirm / Prompt / Choice
- **Light / Dark 双主题**：纯代码生成主题色 + Drawable，可运行时切换；支持自定义主题
- **VISUI 风格的全局 API**：`BsUI.getSkin()` / `BsTheme.tp()`，组件无需自己持有 Skin
- **FreeType 中文字体**：开箱即用，支持分帧预热、字号分级
- **Skin 工具链**：`BsSkinLoader`（json + atlas + FreeType 加载）、`BsSkinExporter`（导出供 Skin Composer 二次编辑）、`BsIconPackager`（SVG → atlas，desktop 端）
- **跨平台**：LWJGL3（Desktop）+ TeaVM（Web）启动器；demo 模块（Game / Screen）平台无关，两端复用

### 模块结构

| 模块 | 职责 |
|------|------|
| `common` | 平台无关接口（`Platform`、文件选择器签名等） |
| `core` | **bs-ui 库本体**：所有 `Bs*` 组件、主题、Skin 工厂/加载器、图表 |
| `demo` | 平台无关的演示（`Game` + `Screen`），展示 bs-ui 全部能力 |
| `lwjgl3` | Desktop 启动器 + 平台实现 + iconpkg 打包工具（依赖 Batik） |
| `teavm` | Web 启动器 |

### 快速上手

```java
// 应用启动
BsUI.init();
BitmapFont defaultFont = ...; // FreeType 生成
BsUI.registerDefaultSkin(defaultFont);
BsUI.registerTheme("dark", BsDarkTheme.INSTANCE, buildSkin(BsDarkTheme.INSTANCE));

// 任意位置取用
Skin skin = BsUI.getSkin();
BsButton btn = new BsButton("确定", skin,
        BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);

// 切主题（监听器回调里重建 UI）
BsUI.setTheme("dark");
```

运行 demo：

```bash
./gradlew :lwjgl3:run     # Desktop
```

### 致谢

本项目站在以下优秀开源项目的肩膀上，没有它们就没有 `bs-ui`：

- **[libGDX](https://libgdx.com/)** —— 跨平台游戏引擎，bs-ui 的基石
- **[VISUI](https://github.com/kotcrab/vis-ui)** —— Scene2D UI 库的标杆，bs-ui 的全局 API 设计直接受其启发
- **[Bootstrap](https://getbootstrap.com/)** —— 设计语言与组件形态的来源
- **[Bootstrap Icons](https://icons.getbootstrap.com/)** —— 图标体系
- **[霞鹜文楷 / LXGW WenKai](https://github.com/lxgw/LxgwWenKai)** —— 内置中文字体，清新易读
- **[gdx-liftoff](https://github.com/libgdx/gdx-liftoff)** —— 项目脚手架生成
- **[gdx-teavm](https://github.com/xpenatan/gdx-teavm)** —— libGDX 的 TeaVM/Web 后端
- **[gdx-nativefilechooser](https://github.com/spdqdd/gdx-nativefilechooser)** —— 跨平台原生文件选择
- **[Apache Batik](https://xmlgraphics.apache.org/batik/)** —— iconpkg 工具用于 SVG → PNG/atlas 转换
- **[Lombok](https://projectlombok.org/) / [SLF4J](http://www.slf4j.org/) / [Fastjson2](https://github.com/alibaba/fastjson2)** —— 开发基础设施

> 若您认为某个致谢遗漏或标注不当，欢迎指出。

---

## License（中文）

本项目基于 **Apache License 2.0**，允许商业使用、修改、分发、私用。

**附加条件（Attribution Condition）**：任何把 bs-ui 直接使用或修改后使用、并分发给最终用户的产品，**必须在程序内的 About / 关于 / Credits 页面清晰标注**：

1. 库名 `bs-ui`
2. 声明本产品使用了 bs-ui
3. 声明是否修改过（如 "Uses bs-ui (unmodified)" 或 "Uses a modified version of bs-ui"）
4. 上游项目链接，或版权行 `Copyright (c) 2026 bs-ui contributors`

仅在源码注释、README、LICENSE 文件里提到 **不算** 满足条件——必须出现在最终用户能通过 UI 看到的 About 页面。完整条款见 [LICENSE](./LICENSE)。

**一行代码合规**：库自带 `BsAboutDialog`，使用方调一行即可弹出满足上述要求的 About 弹窗：

```java
// modified=true 表示你改过 bs-ui 源码；false 表示未改
BsAboutDialog.show(stage, skin, "我的应用", false);

// 或完整定制：产品名 + 版本 + 追加自家/其他依赖致谢
new BsAboutDialog(skin)
    .product("我的应用", "1.0.0")
    .modified(false)
    .appendSection("开源依赖",
        "libGDX (Apache-2.0)", "VISUI (Apache-2.0)", "Bootstrap (MIT)")
    .showModal(stage);
```

---

## English

### What is this

`bs-ui` is a UI component library that runs on top of **libGDX**, bringing the design language and component patterns of **Bootstrap** from the Web front-end into the Scene2D world of a game engine.

Its intent is straightforward: **to bridge the gap between a game engine and a traditional GUI framework.**

Game engines excel at rendering, animation, and batch drawing, yet their built-in UI widgets tend to be crude, ugly, and few. Traditional GUI frameworks (Swing / JavaFX / Web) offer rich widgets but cannot enter a game's render pipeline. `bs-ui` lets you enjoy Bootstrap's "uniform, ready-to-use" component ecosystem inside the very same Scene2D Stage — without stepping outside libGDX's rendering, input, or asset pipelines. It is equally handy for toolchains, in-house editors, in-game UI, and data dashboards.

### Current status

**Not the final release, but already usable.**

Core components (90+), Light/Dark themes, FreeType CJK fonts, Skin load/export, and cross-platform launchers are all in place and can be used to build real interfaces. There is still room for polish (performance, charset trimming, web adaptation, docs). Feedback is welcome.

### Features

- **90+ Bootstrap-style components**: Button / Form / InputGroup / SelectBox / Table / DataTable / Pagination / Navbar / MenuBar / Breadcrumb / Card / Modal / Window / Toast / Alert / Dialog / Tooltip / Popover / Tabs / Accordion / Collapse / Offcanvas / Drawer / Carousel / Slider / Progress / Spinner / Badge / Tag / Avatar / Tree / Steps / Timeline / Transfer / ColorPicker / DatePicker / FileItem …
- **9 chart types**: Line / Bar / Area / Pie / Doughnut / Scatter / Spline / Radar (hand-drawn on Scene2D, no third-party chart lib)
- **4 dialogs**: Alert / Confirm / Prompt / Choice
- **Light / Dark themes**: theme colors and drawables generated purely in code, switchable at runtime; custom themes supported
- **VISUI-style global API**: `BsUI.getSkin()` / `BsTheme.tp()`, components hold no Skin reference of their own
- **FreeType CJK fonts**: works out of the box, with frame-spread preheating and multi-size scaling
- **Skin toolchain**: `BsSkinLoader` (json + atlas + FreeType), `BsSkinExporter` (export for Skin Composer re-editing), `BsIconPackager` (SVG → atlas, desktop)
- **Cross-platform**: LWJGL3 (Desktop) + TeaVM (Web) launchers; the demo module (Game + Screen) is platform-agnostic and reused by both

### Module layout

| Module | Responsibility |
|--------|----------------|
| `common` | Platform-agnostic interfaces (`Platform`, file-chooser signatures, etc.) |
| `core` | **The bs-ui library itself**: every `Bs*` component, themes, Skin factory/loader, charts |
| `demo` | Platform-agnostic demo (`Game` + `Screen`) showcasing all bs-ui capabilities |
| `lwjgl3` | Desktop launcher + platform impl + iconpkg packaging tool (depends on Batik) |
| `teavm` | Web launcher |

### Quick start

```java
// App bootstrap
BsUI.init();
BitmapFont defaultFont = ...; // generated via FreeType
BsUI.registerDefaultSkin(defaultFont);
BsUI.registerTheme("dark", BsDarkTheme.INSTANCE, buildSkin(BsDarkTheme.INSTANCE));

// Anywhere in your code
Skin skin = BsUI.getSkin();
BsButton btn = new BsButton("OK", skin,
        BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);

// Switch theme (rebuild UI inside the listener)
BsUI.setTheme("dark");
```

Run the demo:

```bash
./gradlew :lwjgl3:run     # Desktop
```

### Acknowledgements

This project stands on the shoulders of these excellent open-source projects; without them, `bs-ui` would not exist:

- **[libGDX](https://libgdx.com/)** — the cross-platform game engine, the foundation of bs-ui
- **[VISUI](https://github.com/kotcrab/vis-ui)** — the gold standard of Scene2D UI libraries; bs-ui's global API is directly inspired by it
- **[Bootstrap](https://getbootstrap.com/)** — the source of the design language and component patterns
- **[Bootstrap Icons](https://icons.getbootstrap.com/)** — the icon system
- **[LXGW WenKai / 霞鹜文楷](https://github.com/lxgw/LxgwWenKai)** — the built-in CJK font, clean and readable
- **[gdx-liftoff](https://github.com/libgdx/gdx-liftoff)** — project scaffolding generator
- **[gdx-teavm](https://github.com/xpenatan/gdx-teavm)** — TeaVM/Web backend for libGDX
- **[gdx-nativefilechooser](https://github.com/spdqdd/gdx-nativefilechooser)** — cross-platform native file chooser
- **[Apache Batik](https://xmlgraphics.apache.org/batik/)** — used by the iconpkg tool for SVG → PNG/atlas conversion
- **[Lombok](https://projectlombok.org/) / [SLF4J](http://www.slf4j.org/) / [Fastjson2](https://github.com/alibaba/fastjson2)** — development infrastructure

> If any credit is missing or mis-attributed, please let us know.

---

## License (English)

This project is licensed under the **Apache License 2.0**, which permits
commercial use, modification, distribution, and private use.

**Additional Attribution Condition**: Any Product that uses bs-ui (whether
verbatim or modified) and is distributed to end-users **MUST display a clear,
discoverable "About" / "Credits" screen in the UI** stating:

1. The library name `bs-ui`
2. That the Product uses bs-ui
3. Whether bs-ui has been modified (e.g. "Uses bs-ui (unmodified)" or
   "Uses a modified version of bs-ui")
4. The upstream project link, or the copyright line
   `Copyright (c) 2026 bs-ui contributors`

Mentioning bs-ui only in source comments, READMEs, or LICENSE files does
**not** satisfy this condition — it must appear in an About screen that
end-users can reach through the UI. Full text in [LICENSE](./LICENSE).

**One-line compliance**: the library ships a `BsAboutDialog` so users can pop
up a compliant About dialog in a single call:

```java
// modified=true if you modified the bs-ui source; false otherwise
BsAboutDialog.show(stage, skin, "My App", false);

// or fully customized: product name + version + your own / other-dep credits
new BsAboutDialog(skin)
    .product("My App", "1.0.0")
    .modified(false)
    .appendSection("Open-source dependencies",
        "libGDX (Apache-2.0)", "VISUI (Apache-2.0)", "Bootstrap (MIT)")
    .showModal(stage);
```
