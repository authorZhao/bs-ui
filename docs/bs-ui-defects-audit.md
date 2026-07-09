# bs-ui 缺陷审计报告

> 日期：2026-07-10
> 范围：core 模块 + demo 模块全量代码审计
> 目的：除"废电（未接 BsRenderLoop）"和"drawableOf 每帧泄漏（已修）"之外，找出其他缺点并给出修复建议

---

## 严重（建议优先修）

### 1. ShapeRenderer 静态单例 / 每实例 new —— native OpenGL 资源泄漏

**影响**：ShapeRenderer 持有 native OpenGL 资源，泄漏会累积占用显存句柄，最终可能导致 GL 错误或性能下降。**BsSwitch 那个最严重**——界面有 N 个开关就泄漏 N 个。

| 文件 | 行号 | 问题 |
|------|------|------|
| `core/.../BsChart.java` | 152 | `private static ShapeRenderer shapeRenderer` 静态单例，永不 dispose |
| `core/.../BsRingProgress.java` | 28 | `private static ShapeRenderer sharedSR` 同上 |
| `core/.../BsRating.java` | 50 | `private static ShapeRenderer sharedSR` 同上 |
| `core/.../BsSteps.java` | 78 | `private static ShapeRenderer sharedSR` 同上 |
| `core/.../BsStatusBar.java` | 101 | `private static ShapeRenderer sr` 同上 |
| **`core/.../BsSwitch.java`** | **113** | **`sr = new ShapeRenderer()` 每个 Switch 实例 new 一个，无 dispose** |

**修复建议**：
- 方案 A：共享一个 static 单例（注意 ShapeRenderer 非线程安全，但 libgdx UI 都在 render 线程，实际安全）
- 方案 B：BsSwitch 的 Track 改用 batch.draw 画矩形/圆角，不用 ShapeRenderer
- 方案 C：给每个持有 ShapeRenderer 的组件 override `remove()` 时 dispose
- **BsSwitch 必须改**，其余 static 单例可暂留（单例泄漏 1 个 vs 每实例泄漏 N 个）

---

### 2. BsChart 构造函数 new BitmapFont 泄漏

**文件**：`core/.../BsChart.java:162`

```java
protected BsChart() {
    this.font = new BitmapFont();   // 每个 BsChart 实例泄漏一个
```

**问题**：每个图表子类（BsBarChart/BsLineChart/BsPieChart/BsBarChart3D...）构造时 new 一个默认 BitmapFont。如果后续调 `setSkinFont` 换字体，原字体不 dispose；如果没调，actor remove 时也不 dispose。Dashboard 频繁翻页会累积泄漏 native 字体内存。

**影响**：严重。图表多/翻页频繁的场景累积泄漏。

**修复建议**：
- 延迟初始化：构造时不 new，首次 draw 时才从 skin 取字体
- 或在 `setSkinFont` 时 dispose 旧字体
- 或 override `remove()` 时 dispose 字体（前提是确认 font 归属该组件）

---

### 3. BsSkin.CACHE_FONT 跨主题串音 + dispose 后返回死字体

**文件**：`core/.../BsSkin.java:29`

```java
private static final Map<String,BitmapFont> CACHE_FONT = new HashMap<>();
```

**问题**：static Map 跨所有 BsSkin 实例共享。两个后果：
1. **串音**：Light 主题加载的 `"default"` 字体会被 Dark 主题复用（如果两主题字体配置不同就出错）
2. **死字体**：字体被 dispose 后 cache 还持有引用，下次加载返回已 dispose 的字体 → 渲染崩溃

**影响**：严重。主题切换累积字体内存泄漏，或切换后渲染异常。

**修复建议**：
- cache 按 skin 实例隔离（改成实例字段而非 static）
- 或 dispose 时同步清 cache 对应条目
- 或 cache 的 value 改成 WeakReference，dispose 后自动失效

---

### 4. BsSkinExporter 错误的 static import

**文件**：`core/.../BsSkinExporter.java:35`

```java
import static com.badlogic.gdx.net.HttpRequestBuilder.json;
```

**问题**：导入了一个无关的静态字段（HttpRequestBuilder 的 json 实例）。当前没触发（导出走手动 StringBuilder），但谁要是用 `json` 变量就 NPE。属于残留代码。

**影响**：严重（潜在 NPE），当前未触发但属于隐患。

**修复建议**：直接删掉这行 import。

---

## 中等

### 5. 零单元测试

**现状**：整个项目没有任何 JUnit/TestNG 测试。`test/` 目录下全是手动启动的 Launcher demo。

**未覆盖的关键逻辑**：
- BsColors 的 HSL ↔ RGB 转换
- BsI18n 的占位符替换（{0}{1} → String.format）
- BsSkinFactory 的圆角 Pixmap 生成算法
- BsChart 的坐标映射
- drawableOf 的缓存命中

**影响**：中等。重构困难，回归靠肉眼。

**修复建议**：
- 优先给纯逻辑类加测试：BsColors、BsI18n、BsTheme（lighten/darken）
- build.gradle 加 `testImplementation 'org.junit.jupiter:junit-jupiter:5.x'`
- 这些类无 libgdx 依赖（或可 mock），单测成本低

---

### 6. getter 返回内部集合的可变引用

**影响**：外部能直接改内部数据且不触发 UI 刷新，状态不一致。

| 文件 | 行号 | 方法 |
|------|------|------|
| `BsChart.java` | 297 | `getSeriesList()` 返回内部 List |
| `BsDataTable.java` | 170 | `getData()` 返回内部 List |
| `BsListGroup.java` | 116 | `getItems()` 返回内部 List |
| `BsTagInput.java` | 101 | `getTags()` 返回内部 List |
| `BsTimeline.java` | 97 | `getItems()` 返回内部 List |
| `BsChart.Series` | 56-66 | `points` / `label` 是 public 可变字段 |

**修复建议**：
- 返回 `Collections.unmodifiableList(xxx)`（零成本视图）
- 或提供专门的 `addSeries`/`removeSeries` 方法替代直接暴露 List

---

### 7. BsI18n 的 messages 并发不安全

**文件**：`core/.../BsI18n.java:62, 143-152`

```java
private static final Map<String, String> messages = new LinkedHashMap<>();  // 非 synchronized

public static String get(String key) {       // 无 synchronized
    String v = messages.get(key);            // 无锁读
```

**问题**：`setLocale` 是 synchronized 的（会 `messages.clear()` + put），但 `get` 不是。如果异步表单校验回调（BsFormValidator 支持异步 checker）在其他线程调 `BsI18n.get()`，碰上主线程切语言，会 `ConcurrentModificationException`。

**影响**：中等。并发场景下可能 CME 或读到空值。

**修复建议**（任选）：
- get/set 都加 `synchronized`
- messages 换成 `ConcurrentHashMap`（注意 clear+put 要原子，可能需要先 build 新 map 再整体替换引用）

---

### 8. FreeType 字体在 TeaVM/Web 端不可用

**文件**：`core/build.gradle:13` 硬依赖 `gdx-freetype`

**问题**：TeaVM/Web 后端没有 native FreeType 绑定。Web 端必须用预烘焙的 BitmapFont，不能运行时 FreeType 生成。`BsSkin.java:98-157` 的 json 反序列化器会尝试 `new FreeTypeFontGenerator(...)`，Web 端加载含 FreeType 配置段的 skin json 会崩溃。

**影响**：中等。"桌面/Web 同源"卖点打折扣——Web 端字体流程更繁琐。

**修复建议**：
- 短期：文档明确说明 Web 端必须用预烘焙 .fnt
- 长期：BsSkin 反序列化时检测后端类型，FreeType 不可用时跳过并 warn

---

### 9. BsImage 没有 dispose 生命周期

**文件**：`core/.../BsImage.java:185-194`

**问题**：BsImage 加载图片后自己托管 Texture（`ownedTexture`），但 extends Table 没有 dispose 钩子。BsSpinner/BsCircularProgress 都 override 了 `remove()` 自动 dispose，**BsImage 没有**。使用方必须手动调 `BsImage.dispose()`，否则 GPU 内存泄漏。

**影响**：中等。容易忘记手动 dispose。

**修复建议**：
```java
@Override
public boolean remove() {
    if (ownedTexture != null) { ownedTexture.dispose(); ownedTexture = null; }
    return super.remove();
}
```

---

### 10. BsSpinner 图形画错了（注释自己承认）

**文件**：`core/.../BsSpinner.java:141-142`

```java
// 实际上 Bootstrap 是 3/4 圆环旋转。我把上方擦法做反了 —— 现在剩下的是左上 1/4 圆弧
// 但旋转视觉效果还可以（一个小钩绕中心转）。继续。
```

**问题**：Bootstrap spinner-border 是 3/4 圆环旋转，当前实现画成了 1/4 圆弧。视觉差异明显。

**影响**：中等（视觉缺陷）。

**修复建议**：重写 `arc` 绘制逻辑，画 270° 圆弧（3/4 圆）而非 90°。

---

## 轻微

### 11. BsRenderLoop.shouldRender() 实际不跳帧

**文件**：`core/.../BsRenderLoop.java:144-152`

```java
public boolean shouldRender() {
    ...
    return true;   // 始终返回 true
}
```

注释说"idleFps=5 时每 12 帧渲染 1 次"，但代码总是返回 true。省电效果依赖使用方配合 `sleepMs()` 主动 sleep。这是 winsettings 废电的根因之一。

**修复建议**：shouldRender 根据 idleFps 和上次渲染时间真正判断是否跳帧。

---

### 12. BsToast.ACTIVE 静态列表泄漏风险

**文件**：`core/.../BsToast.java:58`

Toast 的 static 列表，Screen 切换时如果 Actions 没执行完会持有死 Toast 引用。需要使用方主动调 `BsToast.clearAll()`。

**修复建议**：Screen dispose 时自动 clearAll，或 ACTIVE 改用 WeakReference。

---

### 13. 硬编码魔法数字散落各处

| 位置 | 数字 | 含义 |
|------|------|------|
| `BsChart.java:107-110` | 48/16/16/32 | 图表边距 |
| `BsChart.java:337` | 24f | 图例尺寸 |
| `BsChart.java:422` | 18f | 图例间距 |
| `BsPopover.java:47` | 8f | 间距 |
| `BsTooltip.java:32-35` | 0.9f alpha / 6f gap / 0.3s delay | Tooltip 配置 |
| `BsModal.java:88` | 14/18 pad, 28 size | 弹窗内边距 |
| `BsCircularProgress.java:44` | 48 segments | 圆弧分段 |
| `BsSpinner.java:110,117` | 64 size, 6 thickness | spinner 尺寸 |

**影响**：轻微。改一致风格要全局搜索。

**修复建议**：集中到 `BsMetrics` 或 `BsTheme` 的常量区。

---

### 14. 神类（职责过多）

| 文件 | 行数 | 问题 |
|------|------|------|
| `BsSkinExporter.java` | 1451 | Drawable 生成 + Style 构造 + NinePatch 打包 + 颜色计算 + 字体管理 |
| `BsSkinFactory.java` | 996 | 同上，运行时版本 |
| `BsLayoutAdmin.java` | 632 | 顶部栏 + 侧边栏 + 折叠动画 + 多级菜单 + 内容区切换 |

**影响**：轻微。可维护性下降。

**修复建议**：BsSkinExporter 可拆成 JsonWriter / AtlasWriter / FontBaker 三个类。优先级低。

---

### 15. 几处完全静默吞异常

大部分 `catch(Throwable)` 都 log 了，但以下几处完全静默（连 log 都没有）：

| 位置 | 说明 |
|------|------|
| `BsI18n.java:225` | listener 回调 `catch (Throwable ignored) {}` |
| `BsI18n.java:278` | loadBundleFile `catch (Throwable ignored)` |
| `BsChart.java:191` | setSkinFont 取字体失败静默 |
| `BsModal.java:294` | 动画异常连 log 都没写 |
| `BsToast.java:246` | clearAll 静默 |

**影响**：轻微。调试时问题被隐藏。

**修复建议**：至少加 `log.warn("...", t)`。

---

## 非缺陷确认（这些没问题）

- **跨平台 API 使用**：core 模块没有 java.awt / javax.swing / new File() / org.lwjgl / GLxx 类，纯 libgdx API，平台兼容良好
- **Pixmap dispose**：core 中所有 `new Pixmap` 调用点都有对应 `pix.dispose()`（BsSkinFactory / BsColorPickerPopup / BsSpinner / BsSkinExporter 逐一确认）
- **Stage dispose**：demo 每个 Screen 的 dispose() 都调了 stage.dispose()
- **drawableOf 缓存**：已修复，走全局 SOLID_CACHE
- **BsModal.drawableFromPath**：已加 PATH_DRAWABLE_CACHE 缓存
- **BsColorPickerPopup**：close() 完整 dispose 了所有 Texture
- **防御性 catch(Throwable)**：80+ 处，大部分 UI 框架的合理防御

---

## 修复优先级建议

| 优先级 | 缺点编号 | 描述 | 工作量 |
|--------|---------|------|--------|
| **立刻修** | #1 | BsSwitch 的 ShapeRenderer 每实例 new | 小 |
| **立刻修** | #2 | BsChart 构造泄漏 BitmapFont | 小 |
| **立刻修** | #4 | BsSkinExporter 错误 static import（删一行） | 极小 |
| **值得修** | #3 | BsSkin.CACHE_FONT 串音 | 中 |
| **值得修** | #7 | BsI18n 并发安全 | 小 |
| **值得修** | #11 | BsRenderLoop 真正跳帧（配合废电问题） | 中 |
| **值得修** | #9 | BsImage 加 remove() dispose | 小 |
| **可选** | #6 | getter 返回不可变视图 | 小 |
| **长期** | #5 | 加单元测试 | 大 |
| **长期** | #14 | 拆神类 | 大 |
