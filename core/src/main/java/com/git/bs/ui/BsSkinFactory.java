package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane.SplitPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 5 风格 Skin 构造器（V2：无状态）。
 *
 * <p>核心入口：</p>
 * <ul>
 *   <li>{@link #create(BitmapFont, BsTheme)}：从零生成全套 Skin（含主题色 + drawable + style）</li>
 *   <li>{@link #augmentWithBsStyles(Skin, BsTheme)}：在已有 Skin（可能是用户从 json 加载的）上叠加 bs-* 资源</li>
 * </ul>
 *
 * <p><b>V2 改动</b>：</p>
 * <ul>
 *   <li>删除 registeredBsKeys / forceRebuild / rebuildDrawables / disposeDrawable / disposeAll</li>
 *   <li>主题切换不再走"在原 skin 上重建"，而是整体 new Skin（在 Game 层 applyTheme）</li>
 *   <li>颜色全部从 skin 的 Color 桶取（augmentWithBsStyles 第一步是 applyColorsToSkin）</li>
 * </ul>
 */
@Slf4j
public final class BsSkinFactory {

    private BsSkinFactory() {}

    /** 创建默认 Skin（Light 主题，default 字体）。 */
    public static Skin create() {
        return create(new BitmapFont(), BsLightTheme.INSTANCE);
    }

    /** 创建默认 Skin（指定字体，Light 主题）。 */
    public static Skin create(BitmapFont font) {
        return create(font, BsLightTheme.INSTANCE);
    }

    /** 创建 Skin（指定字体 + 主题）。 */
    public static Skin create(BitmapFont font, BsTheme theme) {
        Skin skin = new Skin();
        skin.add("default", font, BitmapFont.class);
        skin.add("font", font, BitmapFont.class);
        augmentWithBsStyles(skin, theme);
        return skin;
    }

    /**
     * 在已有 Skin 上叠加 Bs 风格资源与样式。
     * <p>调用顺序：</p>
     * <ol>
     *   <li>{@link BsTheme#applyColorsToSkin(Skin)}：把主题色注册到 skin.Color 桶</li>
     *   <li>注册 1×1 white Drawable</li>
     *   <li>注册通用圆角 NinePatch（用 skin 的 bs-X 色生成）</li>
     *   <li>注册 6 色 × {up/hover/active/disabled/outline-*} NinePatch</li>
     *   <li>注册 {@code bs-btn-{color}} / {@code bs-btn-outline-{color}} TextButtonStyle</li>
     *   <li>注册 default LabelStyle/TextFieldStyle/CheckBoxStyle/SliderStyle 等</li>
     * </ol>
     * <p>已存在的 key 不覆盖（用户提供的 Skin 若已注册同名资源，以用户的为准）。</p>
     */
    public static void augmentWithBsStyles(Skin skin, BsTheme theme) {
        // ===== Step 1: 把主题色注册到 skin Color 桶 =====
        theme.applyColorsToSkin(skin);

        BitmapFont font;
        try {
            font = skin.has("lxgw", BitmapFont.class) ? skin.getFont("lxgw")
                    : skin.has("font", BitmapFont.class) ? skin.getFont("font")
                      : skin.has("default", BitmapFont.class) ? skin.getFont("default")
                        : skin.has("default-font", BitmapFont.class) ? skin.getFont("default-font")
                        : new BitmapFont();
        } catch (Throwable t) {
            font = new BitmapFont();
        }
        if (!skin.has("default", BitmapFont.class)) {
            skin.add("default", font, BitmapFont.class);
        }
        if (!skin.has("font", BitmapFont.class)) {
            skin.add("font", font, BitmapFont.class);
        }

        // ===== Step 2: 注册 white Drawable（用作 newDrawable 基础） =====
        if (!skin.has("white", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            Pixmap whitePix = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
            whitePix.setColor(Color.WHITE);
            whitePix.fill();
            Texture whiteTex = new Texture(whitePix);
            whitePix.dispose();
            skin.add("white", new NinePatchDrawable(new NinePatch(new TextureRegion(whiteTex), 1, 1, 1, 1)),
                    com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
        }

        // ===== Step 3: 从 skin Color 桶取主题色 =====
        // 直接用 skin.get(...) 而不是 BsTheme.tp()，避免循环依赖：
        // registerDefaultSkin 内部调 augmentWithBsStyles 时 BsUI.getSkin() 还没设
        Color tp = skin.get("bs-text-primary", Color.class);
        Color ts = skin.get("bs-text-secondary", Color.class);
        Color tm = skin.get("bs-text-muted", Color.class);
        Color td = skin.get("bs-text-disabled", Color.class);
        Color bb = skin.get("bs-bg-body", Color.class);
        Color bs = skin.get("bs-bg-surface", Color.class);
        Color be = skin.get("bs-bg-elevated", Color.class);
        Color bh = skin.get("bs-bg-hover", Color.class);
        Color bgH = skin.get("bs-bg-header", Color.class);
        Color bd = skin.get("bs-border", Color.class);
        Color bds = skin.get("bs-border-strong", Color.class);
        Color ov = skin.get("bs-overlay", Color.class);
        Color primary = skin.get("bs-primary", Color.class);
        Color focusRing = skin.get("bs-focus-ring", Color.class);
        Color cursor = skin.get("bs-cursor", Color.class);
        Color linkColor = skin.get("bs-link", Color.class);
        Color linkHover = skin.get("bs-link-hover", Color.class);

        // ===== Step 4: 通用圆角资源（带 bs- 前缀） =====
        putIfAbsent(skin, "bs-window-bg", roundRect(bs, bd, 10, 1));
        putIfAbsent(skin, "bs-text-field-bg", roundRect(be, bd, 6, 1));
        putIfAbsent(skin, "bs-text-field-focus", roundRect(be, focusRing, 6, 1));
        putIfAbsent(skin, "bs-text-field-selection", roundRect(primary, primary, 4, 0));
        putIfAbsent(skin, "bs-text-field-cursor", vLineDrawable(cursor));
        putIfAbsent(skin, "bs-list-bg", roundRect(be, bds, 6, 1));
        putIfAbsent(skin, "bs-list-selection", roundRect(primary, primary, 6, 0));
        putIfAbsent(skin, "bs-scrollpane-h-bar", roundRect(bds, bds, 3, 1));
        putIfAbsent(skin, "bs-scrollpane-v-bar", roundRect(bds, bds, 3, 1));
        putIfAbsent(skin, "bs-tooltip-bg", roundRect(new Color(0,0,0,1f), new Color(0,0,0,1f), 6, 1));
        putIfAbsent(skin, "bs-menu-bar-bg", roundRect(bgH, bds, 4, 1));
        putIfAbsent(skin, "bs-slider-bg", roundRect(bds, bds, 4, 0));
        putIfAbsent(skin, "bs-slider-knob", roundRect(primary, primary, 8, 0));
        // progress-track：用 secondary softBg（暂用 secondary 主色计算，6 色循环里会重新算）
        Color secMain = skin.get("bs-secondary", Color.class);
        Color secSoft = new Color(
                secMain.r + (1 - secMain.r) * 0.88f,
                secMain.g + (1 - secMain.g) * 0.88f,
                secMain.b + (1 - secMain.b) * 0.88f, 1f);
        putIfAbsent(skin, "bs-progress-track", roundRect(secSoft, secSoft, 6, 0));

        // 箭头
        putIfAbsent(skin, "bs-arrow-left", arrowDrawable(primary, false));
        putIfAbsent(skin, "bs-arrow-right", arrowDrawable(primary, true));
        putIfAbsent(skin, "bs-arrow-left-disabled", arrowDrawable(td, false));
        putIfAbsent(skin, "bs-arrow-right-disabled", arrowDrawable(td, true));

        // CheckBox / RadioButton
        putIfAbsent(skin, "bs-check-off", checkboxDrawable(false, bd, bs));
        putIfAbsent(skin, "bs-check-on",  checkboxDrawable(true, primary));
        putIfAbsent(skin, "bs-radio-off", radioDrawable(false, bd, bs));
        putIfAbsent(skin, "bs-radio-on",  radioDrawable(true, primary));

        // ===== Step 5: 6 色基础 NinePatch + 派生色 =====
        // 直接从 skin Color 桶取色，调 BsTheme 派生算法（lighten/darken）
        for (BsPalette p : BsPalette.values()) {
            String name = p.key;
            Color main = skin.get("bs-" + name, Color.class);
            Color hover = BsTheme.lighten(main, 0.07f);
            Color active = BsTheme.darken(main, 0.07f);
            // softBg：主色 + 白色 9:1 混合
            float factor = 0.88f;
            Color softBg = new Color(
                    main.r + (1 - main.r) * factor,
                    main.g + (1 - main.g) * factor,
                    main.b + (1 - main.b) * factor, 1f);
            putIfAbsent(skin, "bs-" + name + "-up", roundRect(main, main, 8, 1));
            putIfAbsent(skin, "bs-" + name + "-hover", roundRect(hover, hover, 8, 1));
            putIfAbsent(skin, "bs-" + name + "-active", roundRect(active, active, 8, 1));
            putIfAbsent(skin, "bs-" + name + "-disabled", roundRect(td, td, 8, 1));
            putIfAbsent(skin, "bs-" + name + "-outline-up", roundRect(be, main, 8, 1));
            putIfAbsent(skin, "bs-" + name + "-outline-hover", roundRect(main, main, 8, 1));
            putIfAbsent(skin, "bs-" + name + "-outline-active", roundRect(active, active, 8, 1));
            putIfAbsent(skin, "bs-" + name + "-checked", roundRect(active, active, 8, 1));
            putIfAbsent(skin, "bs-" + name + "-soft-bg", roundRect(softBg, softBg, 6, 0));
        }

        // ButtonGroup inactive 底色
        Color secondaryMain = skin.get("bs-secondary", Color.class);
        Color secondarySoftBg = new Color(
                secondaryMain.r + (1 - secondaryMain.r) * 0.88f,
                secondaryMain.g + (1 - secondaryMain.g) * 0.88f,
                secondaryMain.b + (1 - secondaryMain.b) * 0.88f, 1f);
        putIfAbsent(skin, "bs-btn-group-inactive", roundRect(secondarySoftBg, secondarySoftBg, 6, 0));

        Color darkText = tp;

        // ===== Step 6: TextButtonStyle（bs-btn-X / bs-btn-outline-X） =====
        for (BsPalette p : BsPalette.values()) {
            String name = p.key;
            String solidKey = "bs-btn-" + name;
            String outlineKey = "bs-btn-outline-" + name;
            if (!skin.has(solidKey, TextButtonStyle.class)) {
                TextButtonStyle solid = new TextButtonStyle(
                        skin.getDrawable("bs-" + name + "-up"),
                        skin.getDrawable("bs-" + name + "-hover"),
                        skin.getDrawable("bs-" + name + "-active"),
                        font);
                solid.disabled = skin.getDrawable("bs-" + name + "-disabled");
                solid.fontColor = textColorFor(p, true, skin);
                solid.disabledFontColor = td;
                putStyle(skin, solidKey, solid);
            }
            if (!skin.has(outlineKey, TextButtonStyle.class)) {
                TextButtonStyle outline = new TextButtonStyle(
                        skin.getDrawable("bs-" + name + "-outline-up"),
                        skin.getDrawable("bs-" + name + "-outline-hover"),
                        skin.getDrawable("bs-" + name + "-outline-active"),
                        font);
                outline.disabled = skin.getDrawable("bs-" + name + "-disabled");
                outline.fontColor = textColorFor(p, false, skin);
                outline.overFontColor = Color.WHITE;
                outline.disabledFontColor = td;
                putStyle(skin, outlineKey, outline);
            }
        }

        // default / toggle TextButtonStyle
        if (!skin.has("default", TextButtonStyle.class)) {
            putStyle(skin, "default", skin.get("bs-btn-primary", TextButtonStyle.class));
        }
        if (!skin.has("toggle", TextButtonStyle.class)) {
            putStyle(skin, "toggle", skin.get("bs-btn-secondary", TextButtonStyle.class));
        }

        // ===== Step 7: menu-item / menu-title / link style =====
        putIfAbsent(skin, "bs-menu-item-up", roundRect(be, be, 4, 0));
        putIfAbsent(skin, "bs-menu-item-hover", roundRect(bh, bh, 4, 0));
        putIfAbsent(skin, "bs-menu-item-active", roundRect(primary, primary, 4, 0));
        if (!skin.has("bs-menu-item", TextButtonStyle.class)) {
            TextButtonStyle mi = new TextButtonStyle();
            mi.up = skin.getDrawable("bs-menu-item-up");
            mi.over = skin.getDrawable("bs-menu-item-hover");
            mi.down = skin.getDrawable("bs-menu-item-active");
            mi.checked = skin.getDrawable("bs-menu-item-active");
            mi.font = font;
            mi.fontColor = darkText;
            mi.overFontColor = skin.get("bs-text-on-dark", Color.class);
            mi.downFontColor = Color.WHITE;
            mi.disabledFontColor = td;
            putStyle(skin, "bs-menu-item", mi);
        }

        putIfAbsent(skin, "bs-menu-title-up", roundRect(bgH, bgH, 4, 0));
        putIfAbsent(skin, "bs-menu-title-hover", roundRect(secondarySoftBg, secondarySoftBg, 4, 0));
        putIfAbsent(skin, "bs-menu-title-open", roundRect(bds, bds, 4, 0));
        if (!skin.has("bs-menu-title", TextButtonStyle.class)) {
            TextButtonStyle mt = new TextButtonStyle();
            mt.up = skin.getDrawable("bs-menu-title-up");
            mt.over = skin.getDrawable("bs-menu-title-hover");
            mt.down = skin.getDrawable("bs-menu-title-hover");
            mt.checked = skin.getDrawable("bs-menu-title-open");
            mt.checkedOver = skin.getDrawable("bs-menu-title-open");
            mt.font = font;
            mt.fontColor = darkText;
            mt.overFontColor = linkColor;
            mt.downFontColor = linkColor;
            putStyle(skin, "bs-menu-title", mt);
        }

        if (!skin.has("bs-link", TextButtonStyle.class)) {
            // 派生的透明 drawable 必须主动注册到桶，否则导出时反查失败、
            // 会 fallback 到 bs-primary-up 导致 link 按钮变成蓝色色块
            if (!skin.has("bs-transparent", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                skin.add("bs-transparent",
                        com.git.bs.ui.BsUI.drawableOf(new Color(0, 0, 0, 0)),
                        com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
            }
            com.badlogic.gdx.scenes.scene2d.utils.Drawable transparent =
                    skin.getDrawable("bs-transparent");
            TextButtonStyle lk = new TextButtonStyle();
            lk.up = transparent;
            lk.over = transparent;
            lk.down = transparent;
            lk.font = font;
            lk.fontColor = linkColor;
            lk.overFontColor = linkHover;
            lk.downFontColor = linkHover;
            putStyle(skin, "bs-link", lk);
        }

        // ===== Step 7.5: BsColorPicker swatch style（圆角白底色块，配合 setStyle tint 显示选中色）=====
        // 必须主动注册到桶才能被 BsSkinExporter 导出，加载导出包后才能继续工作
        if (!skin.has("bs-color-swatch-up", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            // 白底圆角 8px，corner=8；后续 newDrawable(name, userColor) 派生 tint 出目标色
            skin.add("bs-color-swatch-up",
                    roundRect(Color.WHITE, Color.WHITE, 8, 0),
                    com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
        }
        if (!skin.has("bs-color-picker", TextButtonStyle.class)) {
            TextButtonStyle cp = new TextButtonStyle();
            cp.up = skin.getDrawable("bs-color-swatch-up");
            cp.over = skin.getDrawable("bs-color-swatch-up");
            cp.down = skin.getDrawable("bs-color-swatch-up");
            cp.font = font;
            cp.fontColor = darkText;
            putStyle(skin, "bs-color-picker", cp);
        }

        // ===== Step 8: 兜底 CheckBox / Slider / Button style =====
        // 注意：CheckBoxStyle 必须先于 TextButtonStyle 判断（子类优先）
        if (!skin.has("default", CheckBoxStyle.class)) {
            CheckBoxStyle cb = new CheckBoxStyle(
                    skin.getDrawable("bs-check-off"),
                    skin.getDrawable("bs-check-on"),
                    font, darkText);
            putStyle(skin, "default", cb);
        }
        if (!skin.has("radio", CheckBoxStyle.class)) {
            CheckBoxStyle rb = new CheckBoxStyle(
                    skin.getDrawable("bs-radio-off"),
                    skin.getDrawable("bs-radio-on"),
                    font, darkText);
            putStyle(skin, "radio", rb);
        }
        if (!skin.has("default-horizontal", SliderStyle.class)) {
            putStyle(skin, "default-horizontal", new SliderStyle(
                    skin.getDrawable("bs-slider-bg"),
                    skin.getDrawable("bs-slider-knob")));
        }
        if (!skin.has("default-vertical", SliderStyle.class)) {
            putStyle(skin, "default-vertical", new SliderStyle(
                    skin.getDrawable("bs-slider-bg"),
                    skin.getDrawable("bs-slider-knob")));
        }
        // 派生 handle drawable 注册到桶（避免导出时反查失败 fallback 到错误 drawable）
        if (!skin.has("bs-split-handle", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            skin.add("bs-split-handle",
                    skin.newDrawable("white", bds),
                    com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
        }
        if (!skin.has("default-horizontal", SplitPaneStyle.class)) {
            SplitPaneStyle sp = new SplitPaneStyle();
            sp.handle = skin.getDrawable("bs-split-handle");
            putStyle(skin, "default-horizontal", sp);
        }
        if (!skin.has("default-vertical", SplitPaneStyle.class)) {
            SplitPaneStyle sp = new SplitPaneStyle();
            sp.handle = skin.getDrawable("bs-split-handle");
            putStyle(skin, "default-vertical", sp);
        }
        if (!skin.has("default", ButtonStyle.class)) {
            putStyle(skin, "default", new ButtonStyle());
        }

        // ===== Step 9: 兜底 Label/TextField/List/ScrollPane/SelectBox/Window =====
        if (!skin.has("default", LabelStyle.class)) {
            putStyle(skin, "default", new LabelStyle(font, darkText));
        }
        if (!skin.has("default", TextFieldStyle.class)) {
            TextFieldStyle tf = new TextFieldStyle(
                    font, darkText,
                    skin.getDrawable("bs-text-field-cursor"),
                    skin.getDrawable("bs-text-field-selection"),
                    skin.getDrawable("bs-text-field-bg"));
            tf.focusedBackground = skin.getDrawable("bs-text-field-focus");
            putStyle(skin, "default", tf);
        }
        if (!skin.has("default", ListStyle.class)) {
            ListStyle ls = new ListStyle(font, Color.WHITE, darkText, skin.getDrawable("bs-list-bg"));
            ls.selection = skin.getDrawable("bs-list-selection");
            // bs-text-primary 是 Bs 主题的「深色文本」常量；显式注册到 skin 桶避免反查失败
            if (!skin.has("bs-text-primary", Color.class)) {
                skin.add("bs-text-primary", darkText, Color.class);
            }
            // 显式注册 white Color（ListStyle.fontColorSelected 用 Color.WHITE 实例，
            // 导出器反查需要它存在于桶里）
            if (!skin.has("white", Color.class)) {
                skin.add("white", Color.WHITE, Color.class);
            }
            putStyle(skin, "default", ls);
        }
        if (!skin.has("default", ScrollPaneStyle.class)) {
            putStyle(skin, "default", new ScrollPaneStyle(
                    skin.getDrawable("bs-window-bg"),
                    skin.getDrawable("bs-scrollpane-h-bar"),
                    null,
                    skin.getDrawable("bs-scrollpane-v-bar"),
                    null));
        }
        if (!skin.has("default", SelectBoxStyle.class)) {
            SelectBoxStyle sb = new SelectBoxStyle(
                    font, darkText,
                    skin.getDrawable("bs-text-field-bg"),
                    skin.get(ScrollPaneStyle.class),
                    skin.get(ListStyle.class));
            sb.backgroundOver = skin.getDrawable("bs-text-field-focus");
            sb.backgroundOpen = skin.getDrawable("bs-text-field-focus");
            putStyle(skin, "default", sb);
        }
        if (!skin.has("default", WindowStyle.class)) {
            if (!skin.has("bs-overlay-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                skin.add("bs-overlay-bg",
                        skin.newDrawable("white", ov),
                        com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
            }
            WindowStyle ws = new WindowStyle(font, darkText, skin.getDrawable("bs-window-bg"));
            ws.stageBackground = skin.getDrawable("bs-overlay-bg");
            putStyle(skin, "default", ws);
            putStyle(skin, "bs-modal", ws);
        }
    }

    // =================== putIfAbsent / putStyle ===================

    private static void putIfAbsent(Skin skin, String key, NinePatchDrawable d) {
        if (!skin.has(key, com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            skin.add(key, d, com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
        }
    }

    private static void putIfAbsent(Skin skin, String key, com.badlogic.gdx.scenes.scene2d.utils.Drawable d) {
        if (!skin.has(key, com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            skin.add(key, d, com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
        }
    }

    private static void putStyle(Skin skin, String key, Object style) {
        // 子类先于父类（CheckBoxStyle extends TextButtonStyle）
        if (style instanceof CheckBoxStyle) {
            skin.add(key, (CheckBoxStyle) style, CheckBoxStyle.class);
        } else if (style instanceof TextButtonStyle) {
            skin.add(key, (TextButtonStyle) style, TextButtonStyle.class);
        } else if (style instanceof SliderStyle) {
            skin.add(key, (SliderStyle) style, SliderStyle.class);
        } else if (style instanceof SplitPaneStyle) {
            skin.add(key, (SplitPaneStyle) style, SplitPaneStyle.class);
        } else if (style instanceof ButtonStyle) {
            skin.add(key, (ButtonStyle) style, ButtonStyle.class);
        } else if (style instanceof LabelStyle) {
            skin.add(key, (LabelStyle) style, LabelStyle.class);
        } else if (style instanceof TextFieldStyle) {
            skin.add(key, (TextFieldStyle) style, TextFieldStyle.class);
        } else if (style instanceof ListStyle) {
            skin.add(key, (ListStyle) style, ListStyle.class);
        } else if (style instanceof ScrollPaneStyle) {
            skin.add(key, (ScrollPaneStyle) style, ScrollPaneStyle.class);
        } else if (style instanceof SelectBoxStyle) {
            skin.add(key, (SelectBoxStyle) style, SelectBoxStyle.class);
        } else if (style instanceof WindowStyle) {
            skin.add(key, (WindowStyle) style, WindowStyle.class);
        }
    }

    private static Color textColorFor(BsPalette p, boolean solid, Skin skin) {
        boolean useDarkText = (p == BsPalette.WARNING || p == BsPalette.INFO);
        if (!solid) {
            return useDarkText ? skin.get("bs-text-primary", Color.class)
                    : skin.get("bs-" + p.key, Color.class);
        }
        return useDarkText ? skin.get("bs-text-primary", Color.class) : Color.WHITE;
    }

    // =================== 圆角 NinePatch 生成 ===================

    private static NinePatchDrawable roundRect(Color fillColor, Color borderColor, int corner, int borderPx) {
        int fillHex = colorToHex(fillColor);
        int borderHex = colorToHex(borderColor);
        int size = Math.max(8, corner * 3 + borderPx * 2);
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setBlending(Blending.None);
        if (borderPx > 0 && fillHex != borderHex) {
            fillRoundRect(pix, borderHex, 0, 0, size, size, corner);
            fillRoundRect(pix, fillHex, borderPx, borderPx,
                    size - 2 * borderPx, size - 2 * borderPx,
                    Math.max(0, corner - borderPx));
        } else {
            fillRoundRect(pix, fillHex, 0, 0, size, size, corner);
        }
        Texture tex = new Texture(pix);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pix.dispose();
        NinePatch np = new NinePatch(new TextureRegion(tex), corner, corner, corner, corner);
        return new NinePatchDrawable(np);
    }

    private static int colorToHex(Color c) {
        int r = Math.round(c.r * 255) & 0xFF;
        int g = Math.round(c.g * 255) & 0xFF;
        int b = Math.round(c.b * 255) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    private static void fillRoundRect(Pixmap pix, int fillHex, int x, int y, int w, int h, int r) {
        pix.setColor(hexToColor(fillHex));
        pix.fillRectangle(x + r, y, w - 2 * r, h);
        pix.fillRectangle(x, y + r, r, h - 2 * r);
        pix.fillRectangle(x + w - r, y + r, r, h - 2 * r);
        pix.fillCircle(x + r, y + r, r);
        pix.fillCircle(x + w - r - 1, y + r, r);
        pix.fillCircle(x + r, y + h - r - 1, r);
        pix.fillCircle(x + w - r - 1, y + h - r - 1, r);
    }

    // =================== 箭头 / CheckBox / RadioButton ===================

    /** 箭头 Pixmap（24×24，实心三角形）。供 BsSkinExporter 复用同一算法。 */
    static Pixmap arrowPixmap(Color color, boolean pointRight) {
        int size = 24;
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setBlending(Blending.None);
        pix.setColor(color);
        float cx = size / 2f, cy = size / 2f, h = size * 0.35f, w = size * 0.3f;
        if (pointRight) {
            fillTriangle(pix, cx - w / 2f, cy - h,   cx - w / 2f, cy + h,   cx + w / 2f, cy);
        } else {
            fillTriangle(pix, cx + w / 2f, cy - h,   cx + w / 2f, cy + h,   cx - w / 2f, cy);
        }
        return pix;
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable arrowDrawable(Color color, boolean pointRight) {
        return toDrawable(arrowPixmap(color, pointRight));
    }

    private static void fillTriangle(Pixmap pix, float x1, float y1, float x2, float y2, float x3, float y3) {
        float[] ys = {y1, y2, y3};
        float[] xs = {x1, x2, x3};
        for (int i = 0; i < 2; i++) {
            for (int j = i + 1; j < 3; j++) {
                if (ys[j] < ys[i]) {
                    float ty = ys[i]; ys[i] = ys[j]; ys[j] = ty;
                    float tx = xs[i]; xs[i] = xs[j]; xs[j] = tx;
                }
            }
        }
        float ya = ys[0], yb = ys[1], yc = ys[2];
        float xa = xs[0], xb = xs[1], xc = xs[2];
        for (int y = (int) ya; y <= (int) yc; y++) {
            float xL, xR;
            xL = lerp(xa, xc, (y - ya) / (yc - ya));
            if (y < yb) {
                xR = lerp(xa, xb, (y - ya) / (yb - ya));
            } else {
                xR = (y == yb) ? xb : lerp(xb, xc, (y - yb) / (yc - yb));
            }
            if (xL > xR) { float t = xL; xL = xR; xR = t; }
            pix.fillRectangle((int) xL, y, (int) Math.ceil(xR - xL), 1);
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    private static Color hexToColor(int hex) {
        return new Color(((hex >> 16) & 0xFF) / 255f, ((hex >> 8) & 0xFF) / 255f, (hex & 0xFF) / 255f, 1f);
    }

    /**
     * CheckBox Pixmap（24×24）。供 BsSkinExporter 复用同一算法。
     * 未选 = 灰边白底方框；选中 = 主色底 + 白勾（drawThickLine 两笔覆盖完整勾形）。
     */
    static Pixmap checkboxPixmap(boolean checked, Color border, Color surface, Color primary) {
        Pixmap pix = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        pix.setBlending(Blending.None);
        if (!checked) {
            int borderHex = colorToHex(border);
            int surfaceHex = colorToHex(surface);
            fillRoundRect(pix, borderHex, 0, 0, 24, 24, 5);
            fillRoundRect(pix, surfaceHex, 2, 2, 20, 20, 3);
        } else {
            fillRoundRect(pix, colorToHex(primary), 0, 0, 24, 24, 4);
            pix.setColor(Color.WHITE);
            drawThickLine(pix, 6, 12, 10, 17, 3);
            drawThickLine(pix, 10, 17, 18, 6, 3);
        }
        return pix;
    }

    /** CheckBox 图标：未选 = 灰边白底方框；选中 = 主色底 + 白勾。 */
    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable checkboxDrawable(
            boolean checked, Color border, Color surface, Color primary) {
        return toDrawable(checkboxPixmap(checked, border, surface, primary));
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable checkboxDrawable(
            boolean checked, Color border, Color surface) {
        // 兼容老签名（无 primary）：用 skin 的 bs-primary
        return checkboxDrawable(checked, border, surface, new Color(0x0D/255f, 0x6E/255f, 0xFD/255f, 1f));
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable checkboxDrawable(
            boolean checked, Color primary) {
        return checkboxDrawable(checked, Color.GRAY, Color.WHITE, primary);
    }

    /** RadioButton 图标：未选 = 圆环；选中 = 圆环 + 中心实心圆点。 */
    /** RadioButton Pixmap（24×24）。供 BsSkinExporter 复用同一算法。 */
    static Pixmap radioPixmap(boolean checked, Color border, Color surface, Color primary) {
        Pixmap pix = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        pix.setBlending(Blending.None);
        int r = 11;
        int cx = 12, cy = 12;
        pix.setColor(surface);
        pix.fillCircle(cx, cy, r);
        pix.setColor(border);
        pix.drawCircle(cx, cy, r);
        pix.drawCircle(cx, cy, r - 1);
        if (checked) {
            pix.setColor(primary);
            pix.fillCircle(cx, cy, 6);
        }
        return pix;
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable radioDrawable(
            boolean checked, Color border, Color surface, Color primary) {
        return toDrawable(radioPixmap(checked, border, surface, primary));
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable radioDrawable(
            boolean checked, Color border, Color surface) {
        return radioDrawable(checked, border, surface, new Color(0x0D/255f, 0x6E/255f, 0xFD/255f, 1f));
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable radioDrawable(
            boolean checked, Color primary) {
        return radioDrawable(checked, Color.GRAY, Color.WHITE, primary);
    }

    private static void drawThickLine(Pixmap pix, int x1, int y1, int x2, int y2, int thickness) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) * 2;
        int r = Math.max(1, thickness / 2);
        for (int i = 0; i <= steps; i++) {
            float t = steps == 0 ? 0 : i / (float) steps;
            int px = Math.round(x1 + (x2 - x1) * t);
            int py = Math.round(y1 + (y2 - y1) * t);
            pix.fillCircle(px, py, r);
        }
    }

    /** 文本光标 Pixmap（2×16）。供 BsSkinExporter 复用同一算法。 */
    static Pixmap vLinePixmap(Color color) {
        Pixmap pix = new Pixmap(2, 16, Pixmap.Format.RGBA8888);
        pix.setBlending(Blending.None);
        pix.setColor(color);
        pix.fillRectangle(0, 0, 2, 16);
        return pix;
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable vLineDrawable(Color color) {
        return toDrawable(vLinePixmap(color));
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable toDrawable(Pixmap pix) {
        Texture tex = new Texture(pix);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pix.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new TextureRegion(tex));
    }

    // =================== 头像裁剪工具（保留公用 API） ===================

    public static com.badlogic.gdx.scenes.scene2d.utils.Drawable makeRoundDrawable(
            com.badlogic.gdx.scenes.scene2d.utils.Drawable source, int size) {
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setBlending(Blending.None);
        if (source instanceof com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable) {
            TextureRegion region = ((com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable) source).getRegion();
            com.badlogic.gdx.graphics.TextureData td = region.getTexture().getTextureData();
            if (!td.isPrepared()) td.prepare();
            pix.drawPixmap(td.consumePixmap(),
                    region.getRegionX(), region.getRegionY(), region.getRegionWidth(), region.getRegionHeight(),
                    0, 0, size, size);
        } else {
            pix.setColor(new Color(0xDD / 255f, 0xE2 / 255f, 0xE6 / 255f, 1f));
            pix.fill();
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dx = x - size / 2, dy = y - size / 2;
                if (dx * dx + dy * dy > (size / 2) * (size / 2)) pix.drawPixel(x, y, 0);
            }
        }
        Texture tex = new Texture(pix);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pix.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new TextureRegion(tex));
    }

    public static com.badlogic.gdx.scenes.scene2d.utils.Drawable makeRoundDrawableFromPath(
            String internalPath, int size) {
        try {
            Texture tex = new Texture(com.badlogic.gdx.Gdx.files.internal(internalPath));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return makeRoundDrawable(
                    new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new TextureRegion(tex)),
                    size);
        } catch (Throwable t) {
            return null;
        }
    }
}
