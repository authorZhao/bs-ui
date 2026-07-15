# bs-ui Getting Started

> This guide covers two things: **① how to get bs-ui up and running; ② how to initialize the Skin and share fonts globally**.
> The latter is where newcomers trip up most — libGDX's `Skin.dispose()` disposes fonts along with it, so sharing fonts across multiple themes leads to double-dispose crashes. bs-ui solves this with a `BsSkin` global font cache, but you must follow the patterns in this guide.

---

## 1. Minimal Bootstrap (5 lines)

bs-ui uses a **global static access** style à la VISUI: components do not hold their own `Skin`; they all fetch it from `BsUI.getSkin()`.

```java
public class MyApp extends Game {
    @Override
    public void create() {
        BsUI.init();              // init: auto-registers dark/admin/light themes + bundled fonts
        BsI18n.init();            // i18n (defaults to zh_cn)
        setScreen(new MainScreen());
    }

    @Override
    public void dispose() {
        BsUI.disposeAllSkins();   // release all skins + globally shared fonts
        BsUI.dispose();
    }
}
```

Internally `BsUI.init()` calls `BsSkinLoader.loadAllThemes()`, which loads and registers the three bundled skins (`bs-light` / `bs-dark` / `bs-admin`, including fonts, colors, drawables) from `core/src/main/resources/com/git/bs/ui/skin/`. After that you can use anywhere:

```java
Skin skin = BsUI.getSkin();                 // get the current theme's skin
Color c = BsTheme.tp();                     // get the theme's primary text color (no skin arg needed)
BsButton btn = new BsButton("OK", skin,
        BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
```

The desktop launcher only configures the window and hands the `Game` to `Lwjgl3Application`:

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

## 2. Skin Init: The Recommended 3-Step Flow (injecting a custom font)

`BsUI.init()` uses the project's bundled font (LXGW WenKai). **When you want to swap in your own font**, use the recommended flow below — this is the most central usage of this project:

```java
// skinCp is the classpath path of the skin resources (pointing to the .json file)
String skinCp = "com/git/bs/ui/skin";                       // bs-ui's bundled skin directory
FileHandle jsonFile = Gdx.files.internal(skinCp + "/bs-light.json");
BsTheme bsTheme = BsLightTheme.INSTANCE;

// ===== Recommended 3-step flow: load Skin → overlay bs styles → register globally =====
var skin = new BsSkin(jsonFile);                             // 1. load from JSON (font enters global cache)
BsSkinFactory.augmentWithBsStyles(skin, bsTheme);           // 2. overlay the full bs-ui style set onto the skin
BsUI.registerTheme(bsTheme.name(), bsTheme, skin);          // 3. register into BsUI (first registration auto-activates)
```

Line-by-line explanation of the three steps:

| Step | What it does | Key point |
|---|---|---|
| ① `new BsSkin(jsonFile)` | Loads the skin from `.json` + `.atlas` + `.fnt` | **The parameter is a `FileHandle`**, not a string. `BsSkin` puts fonts into the global cache on construction (see Section 3). |
| ② `augmentWithBsStyles(skin, theme)` | Overlays bs-ui styles onto an existing skin: registers theme color tokens, programmatically generates rounded NinePatches, 6-color × {solid/outline/ghost} button styles, component styles | **Existing keys are not overwritten** — your JSON's fonts/drawables take priority; bs-ui only fills in what's missing. So "load your JSON first → then augment" is a safe overlay. |
| ③ `registerTheme(name, theme, skin)` | Stores the `(theme, skin)` pair by name into the `BsUI` registry | The first registration automatically becomes the active theme. After that `BsUI.getSkin()` returns it. |

> **These three steps are equivalent to `BsSkinLoader.loadAndRegisterBsTheme(...)`** — the latter is just a wrapper around them. For multiple themes, call it three times in a loop (changing json + theme each time).

### How to inject a custom font?

**Option A: declare it in the skin JSON (goes through the global font cache — recommended)**

Declare the font in your `bs-light.json`; bs-ui's `BsSkin` will load it and put it into the global cache:

```json
com.badlogic.gdx.graphics.g2d.BitmapFont: {
    my-font: { file: my-font.fnt }
}
```

Or generate at runtime via FreeType (from `.ttf` + charset file — no pre-baking):

```json
com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator: {
    my-font: {
        font: "com/git/bs/ui/skin/MyFont.ttf",
        size: 18,
        characters: "com/git/bs/ui/skin/chinese.txt"
    }
}
```

> Tip: if you name your font `lxgw` in the JSON, `augmentWithBsStyles` will **automatically pick it as the default font**.

**Option B: generate via FreeType at runtime + inject programmatically (no global cache; managed by your app)**

```java
BitmapFont defaultFont = generateFont(chars, 18);          // generated via FreeType
Map<String, BitmapFont> sizeFonts = Map.of(                // size variants
        "sm", generateFont(chars, 14),
        "md", generateFont(chars, 16),
        "lg", generateFont(chars, 20));
BsUI.registerDefaultSkin(defaultFont, sizeFonts);          // create & register with default font + Light theme
```

This uses a native `new Skin()` (not BsSkin); the font lifecycle is managed by your app. **The two options do not interoperate**: fonts in the global cache are not visible to the `buildSkin` path. Use Option A when you need cross-theme sharing.

---

## 3. Global Font Sharing (important — avoid crashes)

This is the biggest pitfall for newcomers. First, the problem:

### Why is global sharing needed?

bs-ui supports **runtime multi-theme switching** (Light/Dark/Admin). Each theme corresponds to an independent `Skin` instance. But fonts are heavy (a CJK font can reach tens of MB) — it is **impossible to load a separate copy per theme**. The correct approach: all theme skins share the same set of font instances.

### The libGDX native pitfall

`Skin.dispose()` disposes all internal `Disposable`s (including fonts and Textures). If your 3 theme skins all reference the same font instance, disposing any one skin destroys that font — the other skins will crash or render garbage when they use it.

### bs-ui's solution: `BsSkin.CACHE_FONT`

`BsSkin` maintains a **static global font cache**:

```java
private static final Map<String, BitmapFont> CACHE_FONT = new HashMap<>();   // shared by all BsSkins
```

The mechanism (`BsSkin` defaults to `useCacheFont=true`):

1. **On load**: when `new BsSkin(jsonFile)` parses the JSON, the font serializer puts the font into `CACHE_FONT` and adds a reference to the current skin. When multiple skins load a font **of the same name**, the second one reuses the cached instance instead of regenerating.

2. **On disposing a single skin**: `BsSkin.dispose()` first **removes the shared fonts in `CACHE_FONT` from this skin** (`remove`), then calls `super.dispose()`. This way the shared fonts are not destroyed by this dispose:

   ```java
   @Override public void dispose() {
       if (!useCacheFont) { super.dispose(); return; }       // no cache: font belongs to this skin, release normally
       for (String key : CACHE_FONT.keySet()) {
           try { remove(key, BitmapFont.class); } catch (Throwable ignored) {}   // detach references
       }
       super.dispose();                                       // the skin now has no shared-font references — safe
   }
   ```

3. **Global release**: you must **explicitly call** `BsSkin.disposeFontCache()` or `BsUI.disposeAllSkins()`. **skin / dispose never calls it automatically** — the release timing is decided by the developer (usually on app exit).

### Remember in one sentence

> **Fonts are shared globally and are NOT disposed together with a skin.** A single skin's dispose does not release fonts; only an explicit `disposeFontCache()` / `disposeAllSkins()` truly destroys global fonts.

### ⚠️ Important exception: fonts embedded in an atlas

If a font's Texture comes from the skin's `TextureAtlas` (the JSON region hits the atlas), even removing the font reference from the skin won't save it — `super.dispose()` still disposes the atlas, which destroys the font's Texture along with it.

> **Fonts shared across multiple skins must use an independent Texture** (FreeType-generated, or an independent `.png`-based `.fnt`) — **do not put font images into an atlas**.
>
> bs-ui's bundled fonts are independent-`.png` `.fnt` files (`font-sm.fnt` + `font-sm_0.png`), so they can be safely shared across skins.

---

## 4. Full Lifecycle Pattern (copy & use)

Below is a real Game's full init + theme-switch + exit-release pattern, covering the correct handling of global font sharing:

```java
public class MyApp extends Game {

    @Override
    public void create() {
        // 1. i18n (addBundle must come before init; core translations + your business translations, the latter overrides the former)
        BsI18n.addBundle("com/git/bs/myapp/i18n/");
        BsI18n.init();                        // defaults to zh_cn; init("en_us") for English

        // 2. Skin init (recommended 3-step flow; here we use the BsUI.init() wrapper directly)
        BsUI.init();                          // equivalent to looping loadAndRegisterBsTheme to register the three themes

        // 3. Register a theme-switch listener: rebuild the current screen on switch
        //    (a theme switch swaps the whole skin, so the UI must be rebuilt)
        BsUI.get().addOnThemeChangeListener(theme -> {
            Gdx.app.postRunnable(() -> setScreen(new MainScreen()));
        });

        setScreen(new MainScreen());
    }

    @Override
    public void dispose() {
        // ★ Font-release pattern: detach references first, then dispose fonts uniformly, finally BsUI.dispose()
        // bs-ui's built-in BsUI.disposeAllSkins() already wraps this logic:
        BsUI.disposeAllSkins();               // iterate all skins, detach font references + dispose fonts uniformly
        BsUI.dispose();                       // clear global state (does NOT dispose skins; skins are held by the Game)
    }
}
```

### Pattern for managing fonts manually (if you didn't use BsSkin but the buildSkin path)

```java
@Override public void dispose() {
    Set<BitmapFont> fontSet = new HashSet<>();              // Set dedupes: multiple skins share the same font instance
    for (Skin s : BsUI.registeredSkins()) {
        ObjectMap<String, BitmapFont> all = s.getAll(BitmapFont.class);
        all.forEach(i -> {
            fontSet.add(i.value);
            s.remove(i.key, BitmapFont.class);              // detach references from each skin first
        });
    }
    for (Skin s : BsUI.registeredSkins()) s.dispose();      // then dispose skins (no font references remain)
    for (BitmapFont f : fontSet) { try { f.dispose(); } catch (Throwable ignored) {} }  // fonts released last, uniformly
    BsUI.dispose();
}
```

> If you used `BsUI.disposeAllSkins()` you **don't** need to write the above manually — it implements exactly this logic internally.

---

## 5. Switching Themes

```java
BsUI.setTheme("dark");                      // switch to the registered "dark" theme
// or
BsUI.setTheme(BsDarkTheme.INSTANCE);
```

What `setTheme` does: swaps the `currentSkin` / `currentTheme` pointers entirely to the target theme's pre-built `Skin` instance, then notifies all listeners. **Note: switching a theme is not "re-coloring the existing skin" but a whole-skin swap**, so business UI usually rebuilds the screen inside the listener.

```java
BsUI.get().addOnThemeChangeListener(theme -> {
    // theme is the new theme after the switch; rebuild the screen so components use the new skin
    Gdx.app.postRunnable(() -> setScreen(new MainScreen()));
});
```

---

## 6. Internationalization (BsI18n)

```java
BsI18n.addBundle("com/git/bs/myapp/i18n/");   // register your business translation directory (call before init)
BsI18n.init();                                // defaults to zh_cn
BsI18n.init("en_us");                         // or specify a locale directly

BsI18n.get("btn.ok");                         // fetch text; a missing key returns the key itself (no exception)
BsI18n.get("table.page_info", total, page, totalPages, pageSize);   // with placeholders {0}{1}
BsI18n.setLocale("ja_jp");                    // switch language at runtime; reloads and fires listeners
```

- Supported locales: `zh_cn` (default), `en_us`, `ja_jp`.
- Translation files go in `classpath/{bundle}/{locale}.properties` (UTF-8; bs-ui ships its own parser, not `java.util.Properties`'s ISO-8859-1).
- Load order: core translations → business translations (the latter overrides the former).

---

## 7. Common Mistakes

| Symptom | Cause | Fix |
|---|---|---|
| Garbage text / crash after switching theme | The font was destroyed by some skin's dispose; other skins still use it | Use `BsSkin` (default `useCacheFont=true`); call `BsUI.disposeAllSkins()` on exit; do not dispose a single skin in isolation |
| Font dispose error / double dispose | Multiple skins reference the same font; disposing them one by one causes a second release | Dedupe with a `Set` and dispose uniformly, or simply use `BsUI.disposeAllSkins()` |
| `getSkin()` throws `IllegalStateException` | You called it before registering any skin | Call `BsUI.init()` or `registerTheme(...)` first |
| Custom font has no effect | You didn't add the font to the skin before `augmentWithBsStyles` | `new BsSkin(json)` to load first (font enters skin), then `augmentWithBsStyles` |
| UI doesn't change after switching theme | A theme switch swaps the whole skin; old components still hold the old skin reference | Rebuild the UI via `setScreen(...)` in the listener |

---

## 8. Next Steps

- **Component overview & comparison with libGDX native**: see [components.md](./components.md)
- **Theme system, Skin loader, architecture details**: see [architecture.md](./architecture.md)
- **Comparison with other GUI frameworks (Swing/JavaFX/ImGui/Qt)**: see [comparison.md](./comparison.md)
- **Custom skin export**: see [bs-custom-style-export.md](./bs-custom-style-export.md)
- **Run the demo**: `./gradlew :lwjgl3:run` (desktop, with 90+ component demos)
- **中文版**: [getting-started.md](./getting-started.md)
