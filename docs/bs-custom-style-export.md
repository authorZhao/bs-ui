# Bs 自研组件 · 自定义 Style 导出方案

- **包路径**：`dialogue/src/main/java/com/git/dialogue3/ui/bs/`
- **关联**：[[bs-ui-audit]]（组件盘点）、`BsSkinExporter`（导出器）、`BsSkinLoader`（加载器）、`BsSkinFactory`（程序化 skin 工厂）
- **日期**：2026-06-29

> **2026-06-29 实测补充（基于本次扫描）**：
> - 已确认 B 类容器（BsCard/BsTable/BsModal/BsAlert/BsNavbar/BsAccordion/BsCollapse）的主背景
>   均正确走 `skin.getDrawable("bs-window-bg" / "bs-menu-bar-bg" / "bs-{variant}-soft-bg")`，
>   主题切换时背景跟随主题色变化。**"切换主题后背景仍是白色" 的根因不在 B 类，而是少数遮罩/装饰色硬编码**：
>   已修复 `BsLoadingOverlay` 与 `BsWindow` 的 backdrop（改用 `bs-overlay` token）；
>   次要装饰色（BsCarousel 指示点 / BsTooltip 黑底 / BsStatusBar 灰线）属于视觉细节，不影响主背景。
> - 因此本文件「自定义 Style 要不要做」的结论**不变**：D 类几何常量是真正候选，B 类不要碰。
> - **当前建议**：方案 1（`BsMetrics`）暂不实施。理由：
>   ① 几何常量目前稳定，没有「设计师频繁调」的真实场景；
>   ② ROI 在「真有人需要外部化间距」之前不成立；
>   ③ 一旦需要，方案 1 实施成本可控（1 个 POJO + 1 个导出 section）。
>   等出现「同一组件要在运行时切换多套几何变体」或「设计师要求间距可配置」时再做。

---


## 一、问题：哪些东西还没进导出？

`BsSkinExporter` 现在已覆盖 libgdx 自带的 **17 个 Style 类**（Button / TextButton / CheckBox / ImageButton / ImageTextButton / Label / Slider / ProgressBar / SplitPane / ScrollPane / SelectBox / List / TextField / TextTooltip / Touchpad / Tree / Window）+ Color 桶 + Drawable atlas + FreeType 字体。

Bs 框架里大量组件是**自研的**（继承 `Table` / `Actor` / `Group`），不走 libgdx 标准 Style 机制。它们的视觉数据现在分三处：

| 数据类型 | 存放位置 | 是否已导出 |
|---|---|---|
| 颜色 | skin `Color` 桶（`bs-primary` 等） | ✅ 已导出 |
| 背景 / 图标 drawable | skin `Drawable` 桶 → atlas | ✅ 已导出 |
| **几何常量**（padding、圆角、尺寸、间距、动画时长） | **硬编码在组件 Java 里** | ❌ 未导出 |

> **结论先行**：「自定义 style 要不要导出」的本质，不是「组件要不要 style」，而是
> **「那些硬编码的几何常量，值不值得外部化进 skin」**。颜色和 drawable 已经在 skin 里了——
> 真正的缺口只有几何常量这一块。

---

## 二、组件分类（按继承关系，实测 grep）

### A. 标准 widget 薄封装 —— 已有 libgdx Style，已导出，**无需自建**

| 组件 | 继承 | 对应 Style |
|---|---|---|
| `BsButton` / `BsLink` / `BsColorPicker` | `TextButton` | `TextButtonStyle` |
| `BsCheckBox` / `BsRadioButton` | `CheckBox` | `CheckBoxStyle` |
| `BsSlider` | `Slider` | `SliderStyle` |
| `BsSplitPane` | `SplitPane` | `SplitPaneStyle` |
| `BsScrollPane` | `ScrollPane` | `ScrollPaneStyle` |
| `BsTextField` / `BsTextArea` / `BsDatePicker` | `TextField` | `TextFieldStyle` |
| `BsWindow` | `Window` | `WindowStyle` |
| `BsStatusLabel` | `Label` | `LabelStyle` |

### B. 自研复合容器（`extends Table` / `Group`）—— 无 Style 机制

约 45 个：`BsAlert`、`BsCard`、`BsModal`、`BsNavbar`、`BsAccordion`、`BsCollapse`、`BsToast`、
`BsForm`、`BsTable`、`BsTree`、`BsPagination`、`BsBreadcrumb`、`BsButtonGroup`、`BsListGroup`、
`BsProfileCard/Panel`、`BsOffcanvas`、`BsDrawer`、`BsMenuBar`、`BsToolbar`、`BsSearchBar`、
`BsInputGroup`、`BsInputNumber`、`BsAutoComplete`、`BsTagInput`、`BsFloatingLabel`、
`BsStatistic`、`BsEmpty`、`BsResult`、`BsPlaceholder`、`BsFigure`、`BsFileItem`、`BsDescriptionList`、
`BsTimeline`、`BsSteps`、`BsRating`、`BsProgress`、`BsSwitch`、`BsAvatar`、`BsBadge`、
`BsIconLabel`、`BsDataTable`、`BsPropertySheet`、`BsInspectorPanel`、`BsNodePalette`、
`BsStatusBar`、`BsTransfer`、`BsLayoutAdmin`、`BsAffix`、`BsBadgeButton`(Group)、`BsCarousel`、
`BsTooltip`、`BsTabPane`、各 Dialog（继承 `BsModal`）。

它们靠「组装带 style 的子 widget + `setBackground(skin.getDrawable("bs-…"))`」出样式。

### C. 纯自绘（`extends Actor`，override `draw`）

`BsSpinner`、`BsMiniMap`、`BsChart` 基类 + 各图表子类（`BsLineChart` / `BsBarChart` / `BsPieChart` /
`BsAreaChart` / `BsSplineChart` / `BsDoughnutChart` / `BsScatterChart` / `BsRadarChart`）。

### D. 自绘几何（`extends Table`，但内部有自绘 Actor + 明确几何参数）

`BsSwitch`、`BsRating`、`BsProgress`、`BsSteps`、`BsTimeline`、`BsSlider`（自绘 knob/track）。

---

## 三、评估：到底哪些需要自建 Style

### 判断标准

一个组件值得自建 Style，**当且仅当**同时满足：

1. 有**可调几何参数**（不是颜色、不是 drawable）；且
2. 这些参数**会随皮肤 / 设计变**（不是写死一次永远不动）；且
3. 外部化后**真有人去改**（设计师或二次开发者）。

颜色、drawable 已经在 skin 里 → 真正的候选只可能出在 **C / D** 两类。

### 逐类结论

| 类 | 结论 | 理由 |
|---|---|---|
| **A 标准封装** | ❌ 不需要 | 已有 libgdx Style，已导出 |
| **B 复合容器（~45）** | ❌ 不建议 | 组装的是已带 style 的子 widget；背景走 drawable；padding 是稳定设计值，不随主题变。给 `Table` 子类加 `getStyle()/setStyle()` 管道是 libgdx 非惯用法，**成本高、收益低** |
| **C 纯自绘** | ⚠️ 可选 | 几何参数（环宽、轴 padding、系列色）确实可调；但**颜色走 `BsPalette`、尺寸走 `setSize()`**，只有「想给设计师调几何」时才值得 |
| **D 自绘几何** | ✅ 最值得 | 几何参数多且语义明确（knob 比例、圆点尺寸、track 高），是自建 Style 的**真正候选** |

> 一句话：**只有 D 类（外加可选的 C 类）值得考虑；B 类那一大片 Table 容器不要碰。**

---

## 四、推荐方案

下面两条路都成立，**推荐方案 1**。

### ✅ 方案 1（推荐）：单个共享 `BsMetrics` 设计令牌 POJO

不给每个组件建 Style 类。建**一个** `BsMetrics`，把所有几何常量集中：

```java
package com.git.dialogue3.ui.bs;

import com.badlogic.gdx.graphics.Color;

/** Bs 框架全局几何 / 间距设计令牌。public 字段，供 libgdx Json 反射序列化。 */
public class BsMetrics {
    // button size padding（BsButton.Size 驱动）
    public float btnPadVSm = 4,  btnPadVMd = 7,  btnPadVLg = 12;
    public float btnPadHSm = 12, btnPadHMd = 14, btnPadHLg = 20;
    // steps / timeline / rating / switch / progress ...
    public float stepCircleSize = 32, stepTitleGap = 6, stepTitleHeight = 18;
    public float timelineDotSize = 12, timelineLineWidth = 2;
    public float ratingStarSize = 24, ratingGap = 4;
    public float switchTrackW = 44, switchTrackH = 22, switchKnobRatio = 0.8f;
    public float progressTrackHeight = 8;
    public float badgePadH = 4;
}
```

- **注册**：`BsSkinFactory` 里 `skin.add("default", new BsMetrics(), BsMetrics.class);`（一次）。
- **导出**：`BsSkinExporter` 加 `addBsMetricsSection`，输出
  `"com.git.dialogue3.ui.bs.BsMetrics": { "default": { ... } }`。
  用反射枚举字段：`float/int` 直出，`Color` 走现成的 `resolveColorName`。
- **加载**：标准 `Skin.load()` + libgdx `Json` 反射还原 public 字段（无参构造 + public 字段即可，
  **不用写反序列化器**）。`BsSkinLoader.loadWithFreeType` 走的就是这条标准路径。
- **组件改造**：硬编码处改为 `BsUI.getSkin().get("default", BsMetrics.class).stepCircleSize`。

**优点**：1 个类、1 个导出 section、改动集中；设计师在一处调完全部间距；不破坏现有组件 API。
**缺点**：全局共享一份 metrics（绝大多数场景够用；真要按组件分，再升级到方案 2）。

### 方案 2（重，粒度细）：每个自绘组件一个 Style 类

给 D 类（+ 可选 C 类）每个建 `BsXxxStyle`（public 字段：`Drawable` + `Color` + `float`），
各自注册、各自加导出 section。

**优点**：粒度细，符合 libgdx 习惯，Skin Composer 里能看到每个组件。
**缺点**：要建 6~10 个类 + 6~10 个导出方法 + loader 反射注册；且字段里大量 `Color` / `Drawable`
和 skin 桶重复（导出时还得反查去重）。**ROI 明显低于方案 1**。

> 如果只是为了「设计师能调间距/圆角/尺寸」，方案 1 已经够；方案 2 只在「同一组件需要多套具名
> 变体并在运行时切换」时才必要——目前 Bs 组件用的是 enum 变体（`Variant`/`Size`），用不到。

---

## 五、实施方案 1 的步骤（准备加入到导出）

1. **新建** `BsMetrics.java`（public 字段 POJO，无参构造），把第五节清单里的魔法数字搬进来。
2. **注册**：`BsSkinFactory.augmentWithBsStyles` 末尾 `skin.add("default", new BsMetrics(), BsMetrics.class);`。
3. **组件改造**：D 类组件把硬编码常量改为读 `BsMetrics`（保持 enum 变体逻辑不变，只把数字来源换成 metrics）。
4. **导出**：`BsSkinExporter.writeJsonFile` 里加 `addBsMetricsSection(root, skin)`——反射枚举
   `BsMetrics` 字段，`float`/`int` 直写，`Color` 走 `resolveColorName`，找不到的引用走 `ensureReferencedColors` 补齐。
5. **加载校验**：确认 `BsSkinLoader.loadWithFreeType` 能反射还原 `BsMetrics`（应无需改代码；
   若 skin 里没有该 section，组件侧 `skin.get(..., BsMetrics.class)` 失败时要回退 `new BsMetrics()` 兜底）。
6. **Graal native image**：`BsSkinExporter.defaultTagClasses` 末尾追加 `BsMetrics.class`
   （供 `GdxFeatures` 反射注册，否则 native 包里 Json 还原会失败）。

> 关键点：导出/加载是**两端配套**的——只要 `BsMetrics` 是 public 字段 POJO，libgdx `Json` 双向
> 反射都能跑；`BsSkinExporter` 只需新增一个 section，`BsSkinLoader` 不用动。

---

## 六、几何常量清单（实测 grep，按组件）

> 动画时长 / 行为参数（如 `BsToast.DEFAULT_DURATION`、`BsSpinner` 旋转速度）属于交互逻辑，
> **不建议进 metrics**——它们不是「外观」。下表只列外观几何。

| 组件 | 当前硬编码 | 建议字段 |
|---|---|---|
| `BsButton` | padV 4 / 7 / 12，padH 12 / 14 / 20（`Size` 驱动） | `btnPadV{Sm,Md,Lg}`、`btnPadH{Sm,Md,Lg}` |
| `BsBadge` | `pad(0,4,0,4)`，minSize = 字体行高 | `badgePadH` |
| `BsSteps` | circleSize 32，titleGap，titleHeight | `stepCircleSize` / `stepTitleGap` / `stepTitleHeight` |
| `BsTimeline` | dotSize 12，lineWidth | `timelineDotSize` / `timelineLineWidth` |
| `BsProgress` | track 走 drawable，fill 圆角 | `progressTrackHeight` |
| `BsRating` | star size / count / gap | `ratingStarSize` / `ratingGap` |
| `BsSwitch` | track 44×22，knob ratio | `switchTrackW` / `switchTrackH` / `switchKnobRatio` |
| `BsSpinner` | size 32（颜色走 palette，setSize 可覆盖） | `spinnerSize`（可选） |

---

## 七、决策摘要

| 范围 | 动作 |
|---|---|
| A 类 13 个标准封装 | 不动（已导出） |
| B 类 ~45 个 Table 容器 | **不建 Style**；颜色/drawable 已在 skin |
| C / D 类自绘几何 | **走方案 1**：建单个 `BsMetrics`，导出 1 个 section |
| 字体 / 颜色 / drawable | 已导出，不动 |
| Graal | `defaultTagClasses` 追加 `BsMetrics.class` |
