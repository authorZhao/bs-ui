# bs-ui 快速入门

> 本文讲清楚两件事：**① 怎么把 bs-ui 跑起来；② Skin 怎么初始化、字体怎么全局共享**。
> 后者是新手最容易踩坑的地方——libGDX 的 `Skin.dispose()` 会连带销毁字体，多主题共享字体时会被重复 dispose 导致崩溃。bs-ui 用一套 `BsSkin` 全局字体缓存机制解决了它，但你要按本文的范式用。

---

## 一、最简启动（5 行代码）

bs-ui 采用 VISUI 风格的**全局静态访问**：组件不自己持有 `Skin`，统一从 `BsUI.getSkin()` 取。

```java
public class MyApp extends Game {
    @Override
    public void create() {
        BsUI.init();              // 初始化：自动注册 dark/admin/light 三主题 + 自带字体
        BsI18n.init();            // 国际化（默认 zh_cn）
        setScreen(new MainScreen());
    }

    @Override
    public void dispose() {
        BsUI.disposeAllSkins();   // 释放所有 skin + 全局共享字体
        BsUI.dispose();
    }
}
```

`BsUI.init()` 内部会调 `BsSkinLoader.loadAllThemes()`，把 `core/src/main/resources/cn/pingyuanren/bs/ui/skin/` 下自带的三套皮肤（`bs-light` / `bs-dark` / `bs-admin`，含字体、配色、drawable）自动加载并注册。之后任意位置都能用：

```java
Skin skin = BsUI.getSkin();                 // 取当前主题的 skin
Color c = BsTheme.tp();                     // 取主题文本主色（无需传 skin）
BsButton btn = new BsButton("确定", skin,
        BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
```

桌面启动器只需配窗口，把 `Game` 交给 `Lwjgl3Application`：

```java
public static void main(String[] args) {
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle("My App");
    config.setWindowedMode(1280, 800);
    config.useVsync(true);
    config.setForegroundFPS(60);
    new Lwjgl3Application(new MyApp(), config);
}
```

---

## 二、Skin 初始化：推荐三步法（注入自定义字体）

`BsUI.init()` 用的是项目自带字体（霞鹜文楷）。**当你想换成自己的字体时**，用下面这套推荐流程——这也是本项目最核心的用法：

```java
// skinCp 是皮肤资源的 classpath 路径（指向 .json 文件）
String skinCp = "cn/pingyuanren/bs/ui/skin";                       // bs-ui 自带皮肤目录
FileHandle jsonFile = Gdx.files.internal(skinCp + "/bs-light.json");
BsTheme bsTheme = BsLightTheme.INSTANCE;

// ===== 推荐三步法：加载 Skin → 叠加 bs 样式 → 注册到全局 =====
var skin = new BsSkin(jsonFile);                             // ① 从 JSON 加载（字体进全局缓存）
BsSkinFactory.augmentWithBsStyles(skin, bsTheme);           // ② 在 skin 上叠加 bs-ui 全套样式
BsUI.registerTheme(bsTheme.name(), bsTheme, skin);          // ③ 注册到 BsUI（首次注册自动成为当前主题）
```

三步逐行解释：

| 步骤 | 干什么 | 关键点 |
|---|---|---|
| ① `new BsSkin(jsonFile)` | 从 `.json` + `.atlas` + `.fnt` 加载皮肤 | **注意参数是 `FileHandle`**，不是字符串。`BsSkin` 构造时会把字体放进全局缓存（见第三节）。 |
| ② `augmentWithBsStyles(skin, theme)` | 在已有 skin 上叠加 bs-ui 样式：注册主题色 token、程序化生成圆角 NinePatch、6 色 × {实心/描边/幽灵} 按钮 style、各组件 Style | **已存在的 key 不覆盖**——你 JSON 里的字体/drawable 优先，bs-ui 只补缺失的。所以「先加载你的 JSON → 再 augment」是安全叠加。 |
| ③ `registerTheme(name, theme, skin)` | 把 `(theme, skin)` 按名字存进 `BsUI` 注册表 | 首次注册自动设为当前激活主题。之后 `BsUI.getSkin()` 拿到的就是它。 |

> **这三个步骤等价于 `BsSkinLoader.loadAndRegisterBsTheme(...)`**——后者就是这三行的封装。多主题就循环调三次（每次换 json 和 theme）。

### 自定义字体怎么注入？

**方式 A：写进 skin JSON（走全局字体缓存，推荐）**

在你的 `bs-light.json` 里声明字体，bs-ui 的 `BsSkin` 会自动加载并放进全局缓存：

```json
com.badlogic.gdx.graphics.g2d.BitmapFont: {
    my-font: { file: my-font.fnt }
}
```

或 FreeType 运行时生成（从 `.ttf` + 字符集文件生成，免预先烘焙）：

```json
com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator: {
    my-font: {
        font: "cn/pingyuanren/bs/ui/skin/MyFont.ttf",
        size: 18,
        characters: "cn/pingyuanren/bs/ui/skin/chinese.txt"
    }
}
```

> 小技巧：如果你的字体在 JSON 里命名为 `lxgw`，`augmentWithBsStyles` 会**自动把它选作 default font**。

**方式 B：运行时 FreeType 生成 + 程序化注入（不走全局缓存，App 自管）**

```java
BitmapFont defaultFont = generateFont(chars, 18);          // FreeType 生成
Map<String, BitmapFont> sizeFonts = Map.of(                // 各档字号
        "sm", generateFont(chars, 14),
        "md", generateFont(chars, 16),
        "lg", generateFont(chars, 20));
BsUI.registerDefaultSkin(defaultFont, sizeFonts);          // 用默认字体 + Light 主题创建并注册
```

这种方式用原生 `new Skin()`（非 BsSkin），字体生命周期由你的 App 自己管理。**两种方式不互通**：全局缓存里的字体不会被 `buildSkin` 路径看到。需要跨主题共享就用方式 A。

---

## 三、字体全局共享机制（重点，避免崩溃）

这是新手最大的坑。先说清楚问题：

### 为什么需要全局共享？

bs-ui 支持**多主题运行时切换**（Light/Dark/Admin）。每个主题对应一个独立的 `Skin` 实例。但字体很重（中文字体可达数十 MB），**不可能每个主题 skin 各加载一份**。正确做法是：所有主题 skin 共享同一组字体实例。

### libGDX 原生的坑

`Skin.dispose()` 会把内部所有 `Disposable`（含字体、Texture）全部 dispose。如果你的 3 个主题 skin 都引用同一个字体实例，随便 dispose 一个 skin，这个字体就被销毁了——其余 skin 再用就会崩溃或乱码。

### bs-ui 的解决方案：`BsSkin.CACHE_FONT`

`BsSkin` 维护一个**静态全局字体缓存**：

```java
private static final Map<String, BitmapFont> CACHE_FONT = new HashMap<>();   // 所有 BsSkin 共享
```

机制（`BsSkin` 默认 `useCacheFont=true`）：

1. **加载时**：`new BsSkin(jsonFile)` 解析 JSON 时，字体序列化器把字体放进 `CACHE_FONT`，同时给当前 skin 加一份引用。多个 skin 加载**同名**字体时，第二次直接复用缓存里的同一个实例，不重新生成。

2. **dispose 单个 skin 时**：`BsSkin.dispose()` 先把 `CACHE_FONT` 里所有共享字体从**本 skin 摘除引用**（`remove`），再调 `super.dispose()`。这样共享字体不会被这次 dispose 销毁：

   ```java
   @Override public void dispose() {
       if (!useCacheFont) { super.dispose(); return; }       // 不用缓存：字体归本 skin，正常释放
       for (String key : CACHE_FONT.keySet()) {
           try { remove(key, BitmapFont.class); } catch (Throwable ignored) {}   // 摘引用
       }
       super.dispose();                                       // 此时 skin 里已无共享字体引用，安全
   }
   ```

3. **全局释放**：必须**显式调** `BsSkin.disposeFontCache()` 或 `BsUI.disposeAllSkins()`。**skin / dispose 不会自动调它**——释放时机由开发者决定（通常 app 退出时）。

### 一句话记忆

> **字体全局共享，不和 skin 一起销毁。** 单个 skin 的 dispose 不会释放字体；只有显式调 `disposeFontCache()` / `disposeAllSkins()` 才真正销毁全局字体。

### ⚠️ 重要例外：嵌入 atlas 的字体

如果字体的 Texture 来自 skin 的 `TextureAtlas`（JSON 里 region 命中 atlas），即使从 skin 摘除字体引用也救不了——`super.dispose()` 仍会 dispose atlas，连带把字体 Texture 毁掉。

> **多 skin 共享的字体必须用独立 Texture**（FreeType 生成的、或独立 `.png` 的 `.fnt`），**不要把字体图放进 atlas**。
>
> bs-ui 自带字体就是独立 `.png` 的 `.fnt`（`font-sm.fnt` + `font-sm_0.png`），所以能安全跨 skin 共享。

---

## 四、完整生命周期范式（抄就能用）

下面是一个真实 Game 的完整初始化 + 主题切换 + 退出释放范式，覆盖字体全局共享的正确处理：

```java
public class MyApp extends Game {

    @Override
    public void create() {
        // 1. 国际化（addBundle 必须在 init 前，core 翻译 + 你的业务翻译，后者覆盖前者）
        BsI18n.addBundle("cn/pingyuanren/bs/myapp/i18n/");
        BsI18n.init();                        // 默认 zh_cn；init("en_us") 切英文

        // 2. Skin 初始化（推荐三步法，这里直接用 BsUI.init() 的封装）
        BsUI.init();                          // 等价于循环 loadAndRegisterBsTheme 注册三主题

        // 3. 注册主题切换监听：切主题时重建当前 screen（主题是整体换 skin，UI 需重建）
        BsUI.get().addOnThemeChangeListener(theme -> {
            Gdx.app.postRunnable(() -> setScreen(new MainScreen()));
        });

        setScreen(new MainScreen());
    }

    @Override
    public void dispose() {
        // ★ 字体释放范式：先摘引用，再统一 dispose 字体，最后 BsUI.dispose()
        // bs-ui 自带 BsUI.disposeAllSkins() 已封装好这套逻辑：
        BsUI.disposeAllSkins();               // 遍历所有 skin 摘字体引用 + 统一 dispose 字体
        BsUI.dispose();                       // 清空全局状态（不销毁 skin，skin 由 Game 持有）
    }
}
```

### 手动管理字体的范式（如果你没用 BsSkin，而是 buildSkin 路径）

```java
@Override public void dispose() {
    Set<BitmapFont> fontSet = new HashSet<>();              // Set 去重：多 skin 共享同一字体实例
    for (Skin s : BsUI.registeredSkins()) {
        ObjectMap<String, BitmapFont> all = s.getAll(BitmapFont.class);
        all.forEach(i -> {
            fontSet.add(i.value);
            s.remove(i.key, BitmapFont.class);              // 先从每个 skin 摘引用
        });
    }
    for (Skin s : BsUI.registeredSkins()) s.dispose();      // 再 dispose skin（此时已无字体引用）
    for (BitmapFont f : fontSet) { try { f.dispose(); } catch (Throwable ignored) {} }  // 字体最后统一释放
    BsUI.dispose();
}
```

> 用了 `BsUI.disposeAllSkins()` 就**不用**手写上面这段——它内部就是这个逻辑。

---

## 五、切换主题

```java
BsUI.setTheme("dark");                      // 切到已注册的 dark 主题
// 或者
BsUI.setTheme(BsDarkTheme.INSTANCE);
```

`setTheme` 做的事：把 `currentSkin` / `currentTheme` 指针整体换到目标主题预构建好的另一个 `Skin` 实例，然后回调所有监听器。**注意：切换主题不是在原 skin 上改色，而是整体换 skin**，所以业务 UI 通常要在监听器里重建 screen。

```java
BsUI.get().addOnThemeChangeListener(theme -> {
    // theme 是切换后的新主题；重建 screen 让组件用上新 skin
    Gdx.app.postRunnable(() -> setScreen(new MainScreen()));
});
```

---

## 六、国际化（BsI18n）

```java
BsI18n.addBundle("cn/pingyuanren/bs/myapp/i18n/");   // 注册你的业务翻译目录（init 前调）
BsI18n.init();                                // 默认 zh_cn
BsI18n.init("en_us");                         // 或直接指定 locale

BsI18n.get("btn.ok");                         // 取文案，key 缺失返回 key 本身（不抛异常）
BsI18n.get("table.page_info", total, page, totalPages, pageSize);   // 带占位符 {0}{1}
BsI18n.setLocale("ja_jp");                    // 运行时切语言，重载并触发监听器
```

- 支持 locale：`zh_cn`（默认）、`en_us`、`ja_jp`。
- 翻译文件放 `classpath/{bundle}/{locale}.properties`（UTF-8，bs-ui 自写解析器，不走 `java.util.Properties` 的 ISO-8859-1）。
- 加载顺序：core 翻译 → 业务翻译（后者覆盖前者）。

---

## 七、常见错误

| 现象 | 原因 | 解决 |
|---|---|---|
| 切主题后乱码/崩溃 | 字体被某个 skin dispose 销毁了，其余 skin 还在用 | 用 `BsSkin`（默认 `useCacheFont=true`）；退出时调 `BsUI.disposeAllSkins()`，不要单独 dispose 某个 skin |
| 字体 dispose 报错/重复 dispose | 多个 skin 引用同一字体，逐个 dispose 导致二次释放 | 用 `Set` 去重统一 dispose，或直接用 `BsUI.disposeAllSkins()` |
| `getSkin()` 抛 `IllegalStateException` | 还没注册任何 skin 就取用 | 先 `BsUI.init()` 或 `registerTheme(...)` |
| 自定义字体没生效 | `augmentWithBsStyles` 之前没把字体加进 skin | 先 `new BsSkin(json)` 加载（字体进 skin），再 `augmentWithBsStyles` |
| 切主题 UI 没变化 | 主题是整体换 skin，旧组件还持有老 skin 引用 | 监听器里 `setScreen(...)` 重建 UI |

---

## 八、下一步

- **组件总览与 libGDX 原生对比**：见 [components.md](./components.md)
- **主题系统、Skin 加载器、架构细节**：见 [architecture.md](./architecture.md)
- **自定义皮肤导出**：见 [bs-custom-style-export.md](./bs-custom-style-export.md)
- **资源加密（pak）**：把素材打成加密 `assets.pak` 分发，见 [pak-consumer-guide.md](./pak-consumer-guide.md)
- **运行 demo**：`./gradlew :lwjgl3:run`（桌面端，含 90+ 组件演示）
- **English version**: [getting-started-en.md](./getting-started-en.md)

---

## 九、真实项目用法：不引入 bs-assets-*，用自己的资源

**依赖只有 `bs-ui-core`**（它不依赖任何 `bs-assets-*` 资源模块）：

```groovy
dependencies {
    implementation 'cn.pingyuanren:bs-ui-core:0.3.2'
}
```

`bs-assets-skin` / `bs-assets-icons` / `bs-assets-emoji` 是三个**纯资源 jar**（本项目自用的皮肤、图标、emoji 烘焙产物）。真实项目通常**体积敏感、品牌定制**，自带资源才是常态。`core` 本身内置了三套默认皮肤（light/dark/admin），所以只引 core 也能直接 `BsUI.init()` 跑起来；下面讲怎么把默认资源整体换成你自己的。

### 1. 用自己的皮肤 + 字体（替代 bs-assets-skin）

用第二节的「三步法」，把 json 指向你自己的皮肤文件即可（json + atlas + png 放你自己项目的 assets）：

```java
public class MyApp extends Game {
    @Override
    public void create() {
        // 用自己的皮肤注册主题（而不是 BsUI.init() 的默认三件套）
        registerTheme("light", BsLightTheme.INSTANCE,  "skins/my-light.json");
        registerTheme("dark",  BsDarkTheme.INSTANCE,   "skins/my-dark.json");
        // 不注册的主题就不存在，用户切不过去——只发你要的
        setScreen(new MainScreen());
    }

    private void registerTheme(String name, BsTheme theme, String jsonPath) {
        Skin skin = new BsSkin(Gdx.files.internal(jsonPath));   // ① 你的 json + atlas + 字体
        BsSkinFactory.augmentWithBsStyles(skin, theme);         // ② 叠加 bs-ui 全套组件样式
        BsUI.registerTheme(name, theme, skin);                  // ③ 注册（首次注册自动激活）
    }
}
```

你的皮肤 json 里声明自己的字体（FreeType 从 `.ttf` 运行时生成，不必预先烘焙）：

```json
com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator: {
    lxgw: {
        font: "fonts/MyFont.ttf",
        size: 18,
        characters: "fonts/chinese.txt"
    }
}
```

> **技巧**：字体在 JSON 里命名为 `lxgw`，`augmentWithBsStyles` 会自动把它选作 bs-ui 的默认字体，所有组件直接用你的字。其余 key（drawable、颜色）同理由你的 JSON 优先，bs-ui 只补缺失项。
>
> **皮肤 json 怎么来**：可以复制 bs-ui 自带的 `bs-light.json` 改颜色/字体；也可以用 `BsSkinExporter` 从运行时导出后二次编辑（见 [bs-custom-style-export.md](./bs-custom-style-export.md)）。
>
> **注意**：多主题共享的字体要用 FreeType 生成的或独立 `.png` 的 `.fnt`，**不要把字体图塞进 atlas**（第三节的重要例外）。

### 2. 用自己的图标集（替代 bs-assets-icons）

图标体系与皮肤无关，走 `BsIcon` 的独立 atlas：

```java
// 启动时加载一次（你自己的 atlas + png，任意路径）
BsIcon.load("icons/my-icons.atlas");

// 按名字取（命名就是 atlas 里的 region 名，你自己定）
Image icon = new Image(BsIcon.get("house"));
btn.getTitleTable().add(icon).padRight(4);

// 退出时释放
BsIcon.dispose();
```

图标 atlas 怎么生成：

- **SVG 素材**：用本项目 desktop 端的 `BootstrapIconPackager`（Batik 渲染，支持按颜色烘焙、多尺寸）；
- **PNG 素材**：libGDX 的 `TexturePacker` 打成 atlas 即可，region 名对上你代码里 `BsIcon.get(...)` 的名字就行。

> 图标默认按白色烘焙，放进浅色容器记得 `Image.setColor(...)` 染色（见组件文档）。

### 3. 用自己的 emoji（替代 bs-assets-emoji）

彩色 emoji 走的是**字体合并**路线：皮肤导出时把 emoji TTF 的字形烘焙进主位图字体的 atlas（`BsSkinExporter` 的 `emojiTtf + emojiCharsFile` 参数；运行时由 App 实现 `emojiTtfPath()` / `emojiCharsPath()` 返回你自己的文件）。

```java
// App 里返回你自己的 emoji 字体与字符集（文件不存在返回 null 即关闭该能力）
@Override public String emojiTtfPath()   { return "fonts/NotoColorEmoji.ttf"; }
@Override public String emojiCharsPath() { return "fonts/emoji.txt"; }
```

两个限制要知道：

- libGDX 位图字体只能索引 BMP（U+0000–U+FFFF），**非 BMP emoji（U+1F300+ 的 surrogate pair）无法渲染**，烘焙时会被跳过——只用 BMP 内的 emoji（如 ☀ ☁ ☂ ❤ ✈）或接受降级；
- 不需要 emoji 就什么都不配（返回 null），文本照常渲染，零开销。

### 4. 什么都不想换？

那就 `BsUI.init()` + 三资源包聚合依赖一步到位（等同本项目 demo 的用法）：

```groovy
implementation 'cn.pingyuanren:bs-ui-core-all:0.3.2'   // core + skin + icons + emoji
```
