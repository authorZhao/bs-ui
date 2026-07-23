# bs-ui 与其他 GUI 框架对比

> 本文把 bs-ui 放在更广的 GUI 框架坐标系里，和 **Java 传统 GUI（Swing/JavaFX）、Dear ImGui、Qt** 做客观对比——既讲 bs-ui 的优势，也坦诚它的局限。
>
> 核心定位先说清楚：**bs-ui 是 libGDX 游戏引擎之上的 UI 组件库**，它的首要场景是游戏内 UI、引擎工具链、编辑器、数据看板，**不是**用来替代 Swing/JavaFX/Qt 做传统桌面应用。理解这一点，下面的对比才有意义。

---

## 一、一张表看懂定位

| 维度 | bs-ui (libGDX) | Swing / JavaFX | Dear ImGui | Qt |
|---|---|---|---|---|
| **定位** | 游戏/UI 引擎内的组件库 | 桌面应用 GUI | 即时模式调试/工具 UI | 跨平台应用框架（C++） |
| **渲染** | libGDX 渲染管线（GPU 批绘制） | 系统原生 / Prism | 各后端（OpenGL/DX/Vulkan） | 光栅 / QPainter |
| **能否进游戏渲染管线** | ✅ 原生支持 | ❌ 独立窗口，进不了游戏画面 | ✅ 但生态偏调试 | ❌ 独立窗口 |
| **跨平台** | 桌面 / Web / 移动（跟随 libGDX） | 桌面为主 | 桌面为主 | 桌面 / 移动 / 嵌入式 |
| **语言** | Java（Kotlin 亦可） | Java | C++（各语言绑定） | C++ / QML |
| **包体积** | 小（零原生依赖，纯 Java） | 中 | 小 | 大（原生库） |
| **布局能力** | 基础（栅格/流式，见第三节） | 强（Layout managers） | 极简 | 极强（QLayout/QML 锚点） |
| **文字排版** | 基础（无完整文本流，见第三节） | 强（StyledDocument） | 基础 | 强（QTextDocument） |
| **主题切换** | ✅ 运行时热切换 | 需重载 LAF | 配色即时 | 需重载 QSS |
| **学习曲线** | 低（Bootstrap 风格，会 Web 上手快） | 中 | 低（但思维不同） | 高 |

---

## 二、逐个对比

### 1. bs-ui vs Java 传统 GUI（Swing / JavaFX）

**Swing / JavaFX 的优势**：
- **布局系统成熟**：`BorderLayout` / `GridBagLayout` / JavaFX 的 `HBox/VBox/GridPane/AnchorPane` + CSS，能精确控制复杂表单、对话框排版。
- **文字排版完整**：富文本、多段落、图文混排、HTML 渲染（JavaFX `HTMLEditor`）。
- **原生交互**：系统文件对话框、系统菜单栏、拖拽、剪贴板、 accessibility。
- **桌面集成深**：系统托盘、全局快捷键、DPI 感知、真窗口装饰。

**bs-ui 的优势**：
- **能进游戏渲染管线**：这是 Swing/JavaFX 做不到的核心价值。UI 组件和游戏画面在同一个 Scene2D Stage 里，共享渲染、输入、资源管线——做游戏内 UI、引擎编辑器、工具链时，Swing/JavaFX 只能另开窗口，割裂体验。
- **Web 端可运行**：通过 TeaVM 后端，bs-ui 能跑在浏览器（WebGL）；Swing/JavaFX 进不了 Web。
- **视觉统一且现代**：Bootstrap 设计语言，开箱即用，比 Swing 默认外观现代得多。
- **轻量零原生依赖**：纯 Java，不需要 JCEF/JavaFX runtime。

**bs-ui 不如它们的地方**（坦诚）：
- 布局能力弱于 Swing/JavaFX 的 LayoutManager 体系（见第三节）。
- 文字排版只是基础级别，没有富文本流、没有完整图文混排（见第三节）。
- 桌面集成浅：没有真系统菜单栏、系统托盘、accessibility。

### 2. bs-ui vs Dear ImGui

**Dear ImGui 的优势**：
- **即时模式（Immediate Mode）**：无状态保留，每帧重绘，调试工具/编辑器开发极快，代码即 UI。
- **性能**：极低开销，单帧绘制海量控件。
- **生态**：游戏行业事实标准，大量引擎（Unity/Unreal/自研）的内嵌工具用它。
- **GPU 友好**：纯绘制，深度嵌入任意渲染管线。

**bs-ui 的优势**：
- **保留模式（Retained Mode）+ 组件丰富**：ImGui 自带控件少，复杂表格/树/图表/对话框都要自己搭；bs-ui 提供 130+ 现成组件（含 10 种图表、DataTable、Timeline 等）。
- **声明式 + 事件驱动**：更像传统 GUI 开发（`addListener`），而非每帧手写绘制逻辑，复杂状态管理更清晰。
- **Java 生态**：对 Java/libGDX 开发者无缝，无 JNI/C++ 绑定成本。

**bs-ui 不如 ImGui 的地方**：
- 性能开销略高（保留模式维护组件树）；但游戏 UI 场景这点开销可忽略。
- 即时模式的"代码即 UI"在快速调试工具上确实更爽。

> 注意：bs-ui 和 ImGui **不是直接竞争**——bs-ui 面向 libGDX 的 Java 生态，ImGui 面向 C++/多引擎生态。如果你在 libGDX 里且想要现成的丰富组件，选 bs-ui；如果你在 C++ 引擎里做调试工具，选 ImGui。

### 3. bs-ui vs Qt

**Qt 的优势**：
- **工业级全能框架**：不只是 GUI，还有网络、数据库、多媒体、QML 声明式 UI。
- **布局与排版极强**：`QLayout` 体系 + QML 锚点/定位器，文字排版（`QTextDocument`）达到浏览器级别。
- **原生体验**：各平台原生外观（Qt Widgets）或 GPU 加速流畅动画（Qt Quick）。
- **移动端成熟**：Android/iOS 部署完善。

**bs-ui 的优势**：
- **能进游戏渲染管线**：Qt 是独立应用框架，UI 进不了游戏画面。
- **Java + libGDX**：对已有 libGDX 项目零迁移成本。
- **轻量**：Qt 是重量级框架（几十 MB 原生库 + moc/uic 工具链），bs-ui 是纯 Java 轻量库。

**bs-ui 不如 Qt 的地方**：
- Qt 是全功能应用框架，bs-ui 只是 UI 组件库——能力广度完全不在一个量级。
- 布局、文字排版、原生集成，Qt 全面碾压。
- 但这正印证定位：**bs-ui 不是 Qt 的替代品**，它是 libGDX 游戏侧的 UI 补充。

---

## 三、bs-ui 相比传统 GUI 缺什么（坦诚清单）

bs-ui 主要面向游戏/UI 引擎场景，因此**有意不追求**传统桌面 GUI 的某些能力。下面如实列出：

### 1. 布局系统：偏弱

传统 GUI 有成熟的 LayoutManager 体系（Swing 的 `GridBagLayout`、JavaFX 的 `AnchorPane`、Qt 的 `QGridLayout`），支持精确的弹性布局、对齐、权重分配、响应式换行。

bs-ui 提供：
- `BsRow` / `BsCol`（横排/纵排）
- `BsGrid`（固定列数网格）
- `BsFlow`（流式自适应换行）
- 底层基于 libGDX `Table`（行/列/grow/pad/align）

够用，但**不及传统 GUI 灵活**——比如没有 CSS Grid/Flexbox 那种弹性收缩、没有 AnchorPane 那种任意锚定。

> **为什么不补全？** 原因有二：① bs-ui 的主场景是游戏内 UI 和工具界面，这些场景布局需求相对简单（表单、卡片、列表为主），`Table` + 栅格组件已覆盖绝大多数；② **完整布局引擎实现难度大**（参考 CSS/Flex/Grid 的复杂度），投入产出比对本项目不划算。

### 2. 文字排版：基础级别

传统 GUI 有完整文本流引擎（富文本、多段落、图文混排、自动断行连字、两端对齐、垂直书写等）。

bs-ui 当前：
- `BsText` 提供字号档 × 颜色 × 粗斜体，支持简单 `setWrap` 换行。
- **没有**完整富文本流、图文混排、复杂断行算法、垂直排版、文本选中编辑的高级能力。

> **为什么不补全？** 文字排版是 GUI 领域最深的水之一（一个完整的文本布局引擎堪比半个浏览器）。bs-ui 依托 libGDX 的 `BitmapFont`/FreeType，做到基础展示和换行已覆盖游戏 UI 的主流需求；完整排版引擎的实现成本极高，超出本项目目标。

### 3. 桌面原生集成：浅

- 无真系统菜单栏（只有自绘的 `BsMenuBar`）。
- 无系统托盘、无全局快捷键、无 accessibility。
- 文件选择等通过 `Platform` 接口委托各平台实现。

> 这些在"应用型桌面软件"里很重要，但在"游戏/UI 引擎"场景里基本用不到，故未实现。

---

## 四、那 bs-ui 到底差在哪、好在哪？（结论）

### 差在哪（相对传统 GUI）
- 布局不如 Swing/JavaFX/Qt 灵活。
- 文字排版是基础级别，无富文本流。
- 桌面原生集成浅。

### 好在哪（相对所有对比对象）
- **唯一能进 libGDX 游戏渲染管线的丰富组件库**——这是 Swing/JavaFX/Qt 都做不到的。游戏内 UI、引擎编辑器、工具链不用再割裂成"游戏窗口 + 另一个 GUI 窗口"。
- **Web 端可运行**（TeaVM/WebGL），Swing/JavaFX/Qt 都进不了浏览器。
- **130+ 现成组件 + 10 种图表**，开箱即用，视觉现代（Bootstrap 风格），远超 ImGui 的裸控件。
- **运行时主题热切换**，传统 GUI 切主题往往要重载 LAF/QSS。
- **Java 轻量零原生依赖**，对比 Qt 的重量级工具链。

### 一句话总结

> **bs-ui 不追求成为"最好的 GUI 框架"，它追求成为"libGDX 游戏生态里最好用的 UI 库"。** 布局和文字排版确实不如传统桌面 GUI，但这对游戏/UI 引擎场景影响很小；而它"进游戏渲染管线 + 跑 Web + 组件丰富"的能力，是传统 GUI 给不了的。
>
> 反正也不差那一点——做游戏内 UI 和工具界面，bs-ui 的能力绰绰有余；如果你要做的是纯桌面办公软件（重表单/重文档/重系统集成），那 Swing/JavaFX/Qt 更合适。

---

## 五、选型建议

| 你的场景 | 推荐 |
|---|---|
| libGDX 游戏内 UI / 引擎编辑器 / 工具链 | **bs-ui** |
| 需要 UI 跑在浏览器（WebGL）+ 丰富组件 | **bs-ui**（TeaVM 后端） |
| 纯 Java 桌面应用（表单/办公/重布局） | JavaFX / Swing |
| C++ 游戏引擎内的调试/工具 UI | Dear ImGui |
| 跨平台重型应用（C++/QML，含移动端） | Qt |
| 既要游戏画面又要复杂桌面 GUI（混合） | 游戏用 bs-ui，桌面壳用 JavaFX/WebView，各取所长 |

---

## 相关文档

- [getting-started.md](./getting-started.md) —— 快速入门
- [components.md](./components.md) —— 组件总览与 libGDX 对比
- [architecture.md](./architecture.md) —— 架构与跨平台设计
