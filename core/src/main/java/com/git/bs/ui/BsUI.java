package com.git.bs.ui;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.common.SkinUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Bs UI 入口类（参考 VISUI 的 VisUI）。
 *
 * <p>管理当前激活的 skin + 主题，全局静态访问。组件构造时调 {@link #getSkin()} 取 skin，
 * 取色用 {@link BsTheme#tp()} 等无参静态方法（内部走 {@code getSkin()} 自动取最新）。</p>
 *
 * <h3>典型用法</h3>
 * <pre>{@code
 * // 应用启动：注册主题 + 默认 skin
 * BsUI.registerDefaultSkin(font);
 * BsUI.registerTheme("dark", BsDarkTheme.INSTANCE, buildSkin(BsDarkTheme.INSTANCE));
 *
 * // 任意位置取 skin / 取色
 * Skin skin = BsUI.getSkin();
 * Color c = BsTheme.tp();
 *
 * // 切主题（监听器会收到回调）
 * BsUI.get().addOnThemeChangeListener(theme -> game.setScreen(new MainScreen()));
 * BsUI.setTheme("dark");
 *
 * // 应用退出
 * BsUI.dispose();
 * }</pre>
 */
@Slf4j
public final class BsUI {

    // =================== 单例 ===================
    private static volatile BsUI instance;

    // =================== 状态（静态） ===================
    /** 当前激活的 skin。所有组件从这里取。 */
    private static Skin currentSkin;
    /** 当前主题。 */
    private static BsTheme currentTheme = BsLightTheme.INSTANCE;
    /** 已注册的主题 → skin 映射（按主题名索引）。 */
    private static final Map<String, Skin> registeredSkins = new LinkedHashMap<>();
    /** 已注册的主题列表（按主题名索引）。 */
    private static final Map<String, BsTheme> registeredThemes = new LinkedHashMap<>();

    /** 监听器：主题切换时触发，业务方在这里 setScreen 重建 UI。 */
    private final List<Consumer<BsTheme>> listeners = new CopyOnWriteArrayList<>();



    private BsUI() {}

    /** 获取单例（实例方法 addOnThemeChangeListener/removeOnThemeChangeListener 通过它调用）。 */
    public static BsUI get() {
        if (instance == null) {
            synchronized (BsUI.class) {
                if (instance == null) {
                    instance = new BsUI();
                }
            }
        }
        return instance;
    }

    /** 显式初始化（可选）。 */
    public static void init() {
        get();
        BsSkinLoader.loadAllThemes();
    }

    // =================== 全局 ShapeRenderer ===================
    /**
     * 全局共享 ShapeRenderer（render 线程单线程，组件 draw 时短暂独占 begin/end，安全）。
     * 替代各组件自己的 static SR（BsChart/BsRingProgress/BsRating/BsSteps/BsStatusBar），
     * 把 N 个 native OpenGL 资源收敛成 1 个。
     */
    private static ShapeRenderer shapeRenderer;

    /** 全局共享 ShapeRenderer（懒加载，首次调用时 new）。 */
    public static synchronized ShapeRenderer shapeRenderer() {
        if (shapeRenderer == null) shapeRenderer = new ShapeRenderer();
        return shapeRenderer;
    }

    /** 自定义全局 ShapeRenderer（如需特殊配置）。传 null 重置为懒加载默认。 */
    public static void setShapeRenderer(ShapeRenderer sr) {
        if (shapeRenderer != null && shapeRenderer != sr) {
            try { shapeRenderer.dispose(); } catch (Throwable ignored) {}
        }
        shapeRenderer = sr;
    }

    /** dispose：清空状态。 */
    public static void dispose() {
        if (shapeRenderer != null) {
            try { shapeRenderer.dispose(); } catch (Throwable ignored) {}
            shapeRenderer = null;
        }   // 释放 BsSkin 跨 skin 共享的缓存字体（多主题公用字体）
        if (instance != null) {
            instance.listeners.clear();
        }
        // 注意：不 dispose skin，skin 由 Game 持有生命周期
        registeredSkins.clear();
        registeredThemes.clear();
        currentSkin = null;
        currentTheme = null;
        instance = null;
    }

    public static void disposeAllSkins() {
        List<BitmapFont> fontList = new ArrayList<>();
        for (var skin : registeredSkins.values()) {
            Map<String, BitmapFont> fontCache = SkinUtil.getFontCache(skin);
            fontCache.forEach((name, font) -> {
                skin.remove(name, BitmapFont.class);
                fontList.add(font);
            });
        }
        fontList.forEach(BitmapFont::dispose);
    }


    // =================== 静态访问 API（VISUI 风格） ===================

    /** 当前激活的 skin。所有组件从这里取（不持有自己的字段）。 */
    public static Skin getSkin() {
        if (currentSkin == null) {
            throw new IllegalStateException("BsUI: skin 未初始化，先调 registerDefaultSkin");
        }
        return currentSkin;
    }

    /** 当前主题。 */
    public static BsTheme currentTheme() {
        return currentTheme;
    }

    /** 当前主题名。 */
    public static String currentThemeName() {
        return currentTheme == null ? null : currentTheme.name();
    }

    // =================== 注册 API ===================

    /**
     * 注册主题 + 对应 skin。同一主题名重复注册会覆盖。
     *
     * @param name  主题名（"light" / "dark" / 自定义）
     * @param theme 主题对象
     * @param skin  对应的 skin（已 augmentWithBsStyles 完成）
     */
    public static void registerTheme(String name, BsTheme theme, Skin skin) {
        if (name == null || theme == null || skin == null) {
            throw new IllegalArgumentException("name/theme/skin 都不能为 null");
        }
        registeredThemes.put(name, theme);
        registeredSkins.put(name, skin);
        log.info("BsUI: 注册主题 {} (theme={}, skin={})", name,
                theme.getClass().getSimpleName(), skin.getClass().getSimpleName());
        if (currentSkin == null) {
            currentTheme = theme;
            currentSkin = skin;
        }
    }

    public static void registerThemeWithDefaultFont(String name, BsTheme theme, Skin skin) {
        if (name == null || theme == null || skin == null) {
            throw new IllegalArgumentException("name/theme/skin 都不能为 null");
        }
        BsSkinFactory.augmentWithBsStyles(skin, theme);
        registerTheme(name, theme, skin);
    }

    /**
     * 便捷：用 default 字体 + Light 主题创建并注册 default skin。
     * 适合应用启动时第一次注册。
     */
    public static void registerDefaultSkin(com.badlogic.gdx.graphics.g2d.BitmapFont font) {
        registerDefaultSkin(font, null);
    }

    /**
     * 注册 default skin（Light 主题），可附带尺寸字体（sm/md/lg/xl）。
     * <p>尺寸字体会在 augmentWithBsStyles 之前注册进 skin 桶，这样派生 label-sm / bs-btn-primary-sm
     * 等尺寸变体时拿到的就是真字号 font，而不是 default 兜底。</p>
     *
     * @param font      default 字体（必需）
     * @param sizeFonts 尺寸字体映射，key 为 "sm"/"md"/"lg"/"xl"；为 null 则不注册尺寸字体
     */
    public static void registerDefaultSkin(com.badlogic.gdx.graphics.g2d.BitmapFont font,
                                           java.util.Map<String, com.badlogic.gdx.graphics.g2d.BitmapFont> sizeFonts) {
        Skin skin = buildSkin(BsLightTheme.INSTANCE, font, sizeFonts);
        registerTheme("light", BsLightTheme.INSTANCE, skin);
        currentTheme = BsLightTheme.INSTANCE;
        currentSkin = skin;
    }

    /** 已注册的所有主题名列表。 */
    public static List<String> registeredThemeNames() {
        return new java.util.ArrayList<>(registeredThemes.keySet());
    }

    // =================== 切换 API ===================

    /**
     * 切换主题（按已注册的主题名）。
     * <p>更新 currentTheme + currentSkin，触发监听器。
     * 监听器里业务方自己负责 setScreen 重建 UI。</p>
     */
    public static void setTheme(String themeName) {
        BsTheme theme = registeredThemes.get(themeName);
        Skin skin = registeredSkins.get(themeName);
        if (theme == null || skin == null) {
            log.warn("BsUI: 主题 {} 未注册，忽略", themeName);
            return;
        }
        if (theme == currentTheme) return;
        log.info("BsUI: 切换主题 {} -> {}", currentThemeName(), themeName);
        currentTheme = theme;
        currentSkin = skin;
        // 通知监听器
        if (instance == null) {
            return;
        }
        for (Consumer<BsTheme> l : instance.listeners) {
            try {
                l.accept(theme);
            } catch (Throwable t) {
                log.warn("onThemeChange listener error", t);
            }
        }

    }

    public static boolean hasTheme(BsTheme bsTheme) {
        return registeredThemes.containsKey(bsTheme.name());
    }

    /**
     * 切换主题（按主题对象，必须已注册）。
     */
    public static void setTheme(BsTheme theme) {
        if (theme == null) return;
        // 找到对应的 name
        String name = null;
        for (Map.Entry<String, BsTheme> e : registeredThemes.entrySet()) {
            if (e.getValue() == theme) { name = e.getKey(); break; }
        }
        if (name == null) {
            log.warn("BsUI: 主题 {} 未注册，忽略", theme.getClass().getSimpleName());
            return;
        }
        setTheme(name);
    }

    /** 用指定主题构建一个新 Skin（注册主题色 + 字体 + bs-* 资源）。 */
    public static Skin buildSkin(BsTheme theme, BitmapFont font) {
        return buildSkin(theme, font, null);
    }

    /**
     * 用指定主题构建一个新 Skin，可附带尺寸字体（sm/md/lg/xl）。
     * <p>尺寸字体在 augmentWithBsStyles 之前注册进 skin 桶，保证派生尺寸变体时字号正确。</p>
     */
    public static Skin buildSkin(BsTheme theme, BitmapFont font,
                                 java.util.Map<String, com.badlogic.gdx.graphics.g2d.BitmapFont> sizeFonts) {
        Skin skin = new Skin();
        skin.add("default", font, com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        skin.add("font", font, com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        // 尺寸字体先注册进桶，BsSkinFactory.augmentWithBsStyles 的 ensureSizeFont 见到就跳过（保留真字号）
        if (sizeFonts != null) {
            for (java.util.Map.Entry<String, com.badlogic.gdx.graphics.g2d.BitmapFont> e : sizeFonts.entrySet()) {
                if (e.getValue() != null) {
                    skin.add("font-" + e.getKey(), e.getValue(), com.badlogic.gdx.graphics.g2d.BitmapFont.class);
                }
            }
        }
        BsSkinFactory.augmentWithBsStyles(skin, theme);
        return skin;
    }

    public static java.util.List<Skin> registeredSkins() {
        return new java.util.ArrayList<>(registeredSkins.values());
    }

    // =================== 监听器（实例方法，通过 get() 调用） ===================

    public void addOnThemeChangeListener(Consumer<BsTheme> listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeOnThemeChangeListener(Consumer<BsTheme> listener) {
        listeners.remove(listener);
    }
}
