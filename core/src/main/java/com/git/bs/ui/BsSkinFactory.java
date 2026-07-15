/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库。
 * Copyright (c) 2026 bs-ui contributors
 *
 * 基于 Apache License 2.0 开源，允许商用、修改和再分发。
 * 使用本库的产品须在“关于”界面标注本项目，详见 LICENSE。
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
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
import com.badlogic.gdx.utils.ObjectMap;
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
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public final class BsSkinFactory {

    /** 尺寸档位（与 json 的 font-{size}.fnt 一一对应，6 档）。md 字号与 default 相同，但 key 仍要注册全。 */
    private static final String[] SIZE_SUFFIXES = {"xs", "sm", "md", "lg", "xl", "xxl"};

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

        // ===== Step 1.5: 确保 font-sm / font-md / font-lg / font-xl 四档可用 =====
        // 优先复用 skin 已加载的（json 提供的 font-{size}.fnt）；
        // 没有则降级为 default font（字号不准但不报错）。
        // 决策：不在 core 强制 FreeType 依赖，正常路径走 json 加载 .fnt。
        for (String size : SIZE_SUFFIXES) {
            ensureSizeFont(skin, "font-" + size, font);
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
        // 白色圆点：供 BsCircularProgress 等按 batch.setColor 染色（统一生成，随 skin 可导出/换主题）
        putIfAbsent(skin, "bs-circle", circleDrawable(16));
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
        // 上下箭头：TimePicker 步进按钮等用（不依赖字体字符，主题主色，多主题对比稳定）
        putIfAbsent(skin, "bs-arrow-up", arrowVerticalDrawable(primary, true));
        putIfAbsent(skin, "bs-arrow-down", arrowVerticalDrawable(primary, false));
        putIfAbsent(skin, "bs-arrow-up-disabled", arrowVerticalDrawable(td, true));
        putIfAbsent(skin, "bs-arrow-down-disabled", arrowVerticalDrawable(td, false));

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
            putIfAbsent(skin, "bs-" + name + "-outline-up", outlineRoundRect(main, 8, 1));
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
                        skin.getDrawable("bs-" + name + "-active"),    // down（按下）= active
                        skin.getDrawable("bs-" + name + "-active"),    // checked
                        font);
                solid.over = skin.getDrawable("bs-" + name + "-hover"); // over（悬停）= hover ★关键：原构造未设 over
                solid.disabled = skin.getDrawable("bs-" + name + "-disabled");
                solid.fontColor = textColorFor(p, true, skin);
                solid.disabledFontColor = td;
                putStyle(skin, solidKey, solid);
            }
            if (!skin.has(outlineKey, TextButtonStyle.class)) {
                TextButtonStyle outline = new TextButtonStyle(
                        skin.getDrawable("bs-" + name + "-outline-up"),      // up：透明描边
                        skin.getDrawable("bs-" + name + "-outline-active"),  // down（按下）= active 填充
                        skin.getDrawable("bs-" + name + "-outline-active"),  // checked
                        font);
                outline.over = skin.getDrawable("bs-" + name + "-outline-hover"); // over（悬停）= 主色填充 ★关键
                outline.disabled = skin.getDrawable("bs-" + name + "-disabled");
                outline.fontColor = textColorFor(p, false, skin);       // up：主色文字
                outline.overFontColor = textColorFor(p, true, skin);    // 悬停填充：对比文字
                outline.downFontColor = textColorFor(p, true, skin);    // 按下填充：对比文字
                outline.disabledFontColor = td;
                putStyle(skin, outlineKey, outline);
            }
            // GHOST（Win11 风格）：与 OUTLINE 完全一致，仅 up 去掉边框（透明）。这里预注册是为了
            // 让 BsSkinExporter 能导出（否则 ensureGhostStyle 是延迟派生，导出时 skin 里没有）。
            // 运行时 BsButton.ensureGhostStyle 的 skin.has 会命中，不再重复派生。
            String ghostKey = "bs-btn-ghost-" + name;
            if (!skin.has(ghostKey, TextButtonStyle.class)) {
                TextButtonStyle ghost = new TextButtonStyle(skin.get(outlineKey, TextButtonStyle.class));
                ghost.up = null;   // up 无边框透明；over/down/fontColor 等均与 OUTLINE 一致
                putStyle(skin, ghostKey, ghost);
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
            // over 背景（bh=bs-bg-hover）只是浅色调，不是主色填充：
            // 字色保持与 up 一致（darkText 随主题），避免 light 主题下「浅底白字」看不清。
            // 只有 down/checked（primary 蓝填充）才用白字。
            mi.overFontColor = darkText;
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
                        drawableOf(new Color(0, 0, 0, 0)),
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

        // ===== Step 9.5: 所有尺寸变体（sm/md/lg/xl）统一派生 =====
        // 放在所有 default style（Step 6-9）注册完之后，按依赖顺序克隆 + 换 font。
        // 依赖关系：label/list/window 只依赖 font；text-field/check-box/select-box 依赖各自的 default style；
        //          bs-btn-{variant} 依赖 Step 6 的 solid/outline style。
        registerAllSizeVariants(skin, tp);
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

    // =================== 尺寸变体 sm / md / lg / xl ===================

    /**
     * 确保 skin 里存在指定 key 的 BitmapFont：json 已加载则直接复用，否则降级为 defaultFont。
     * 不强制 FreeType 依赖（避免运行时必须带 TTF）。
     */
    private static void ensureSizeFont(Skin skin, String key, BitmapFont defaultFont) {
        if (skin.has(key, BitmapFont.class)) return;
        skin.add(key, defaultFont, BitmapFont.class);
    }

    /**
     * 派生所有尺寸变体（sm/md/lg/xl）。必须在 Step 6-9 的 default style 全部注册完后调用。
     * <p>依赖关系：</p>
     * <ul>
     *   <li>label / list / window：只依赖 font + Color，无 style 依赖</li>
     *   <li>text-field：依赖 default TextFieldStyle（Step 9）</li>
     *   <li>check-box：依赖 default CheckBoxStyle（Step 8）</li>
     *   <li>select-box：依赖 default SelectBoxStyle（Step 9）</li>
     *   <li>bs-btn-{variant}：依赖 Step 6 的 solid/outline TextButtonStyle</li>
     * </ul>
     * <p>drawable 完全复用（NinePatch 可拉伸，视觉尺寸靠 font 大小共同体现），仅替换 font 字段。</p>
     * <p>功能型 style（menu-item / menu-title / color-picker / link）跳过，不派生尺寸变体。</p>
     */
    private static void registerAllSizeVariants(Skin skin, Color tp) {
        for (String size : SIZE_SUFFIXES) {
            BitmapFont f = skin.getFont("font-" + size);
            String suf = "-" + size;

            // A. LabelStyle
            String labelKey = "label" + suf;
            if (!skin.has(labelKey, LabelStyle.class)) {
                putStyle(skin, labelKey, new LabelStyle(f, tp));
            }

            // B. TextFieldStyle（依赖 default）
            if (skin.has("default", TextFieldStyle.class)) {
                String tfKey = "text-field" + suf;
                if (!skin.has(tfKey, TextFieldStyle.class)) {
                    TextFieldStyle tf = new TextFieldStyle(skin.get("default", TextFieldStyle.class));
                    tf.font = f;
                    if (tf.messageFont != null) tf.messageFont = f;
                    putStyle(skin, tfKey, tf);
                }
            }

            // C. CheckBoxStyle（依赖 default）
            if (skin.has("default", CheckBoxStyle.class)) {
                String cbKey = "check-box" + suf;
                if (!skin.has(cbKey, CheckBoxStyle.class)) {
                    CheckBoxStyle cb = new CheckBoxStyle(skin.get("default", CheckBoxStyle.class));
                    cb.font = f;
                    putStyle(skin, cbKey, cb);
                }
            }

            // D. ListStyle（依赖 default，default 的 font 也一并替换）
            if (skin.has("default", ListStyle.class)) {
                String lsKey = "list" + suf;
                if (!skin.has(lsKey, ListStyle.class)) {
                    ListStyle ls = new ListStyle(skin.get("default", ListStyle.class));
                    ls.font = f;
                    putStyle(skin, lsKey, ls);
                }
            }

            // E. SelectBoxStyle（依赖 default）
            if (skin.has("default", SelectBoxStyle.class)) {
                String sbKey = "select-box" + suf;
                if (!skin.has(sbKey, SelectBoxStyle.class)) {
                    SelectBoxStyle sb = new SelectBoxStyle(skin.get("default", SelectBoxStyle.class));
                    sb.font = f;
                    putStyle(skin, sbKey, sb);
                }
            }

            // F. WindowStyle（依赖 default，替换 titleFont）
            if (skin.has("default", WindowStyle.class)) {
                String wsKey = "window" + suf;
                if (!skin.has(wsKey, WindowStyle.class)) {
                    WindowStyle ws = new WindowStyle(skin.get("default", WindowStyle.class));
                    ws.titleFont = f;
                    putStyle(skin, wsKey, ws);
                }
            }

            // G. TextButtonStyle：遍历 bs-btn-* 派生（含 solid 与 outline）
            ObjectMap<String, TextButtonStyle> tbss = skin.getAll(TextButtonStyle.class);
            if (tbss != null) {
                // 收集需要派生的 key（遍历中不能直接修改 ObjectMap，先收集）
                com.badlogic.gdx.utils.Array<String> deriveKeys = new com.badlogic.gdx.utils.Array<>();
                for (ObjectMap.Entry<String, TextButtonStyle> e : tbss) {
                    String k = e.key;
                    if (!k.startsWith("bs-btn-")) continue;
                    // 已是尺寸变体的不再派生
                    if (isSizeSuffix(k)) continue;
                    // 目标 key 已存在（json 已提供）也跳过
                    if (skin.has(k + suf, TextButtonStyle.class)) continue;
                    deriveKeys.add(k);
                }
                for (String key : deriveKeys) {
                    TextButtonStyle base = skin.get(key, TextButtonStyle.class);
                    TextButtonStyle variant = new TextButtonStyle(base);
                    variant.font = f;
                    putStyle(skin, key + suf, variant);
                }
            }
        }
    }

    /** 判断 key 是否已带尺寸后缀（避免对 bs-btn-primary-sm 再派生 -sm/-lg）。 */
    private static boolean isSizeSuffix(String key) {
        for (String size : SIZE_SUFFIXES) {
            if (key.endsWith("-" + size)) return true;
        }
        return false;
    }

    private static Color textColorFor(BsPalette p, boolean solid, Skin skin) {
        // 浅填充（light / warning / info）需深色文字才达标对比度
        boolean useDarkText = (p == BsPalette.WARNING || p == BsPalette.INFO || p == BsPalette.LIGHT);
        if (!solid) {
            // outline 未填充：文字 = 变体主色（与描边一致，对齐 Bootstrap）
            return skin.get("bs-" + p.key, Color.class);
        }
        // solid（及 outline hover/active 实色填充）：浅填充用深字，否则白字
        return useDarkText ? skin.get("bs-text-primary", Color.class) : Color.WHITE;
    }

    // =================== 圆角 NinePatch 生成 ===================

    /** 包内可见：圆角 NinePatchDrawable 生成（供 BsLayoutAdmin 等同包类复用，避免重复算法）。 */
    static NinePatchDrawable roundRect(Color fillColor, Color borderColor, int corner, int borderPx) {
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

    /** 透明底 + 主色描边的圆角（Bootstrap outline-up 用）：内部用 alpha=0 挖空，透出父级背景。 */
    static NinePatchDrawable outlineRoundRect(Color borderColor, int corner, int borderPx) {
        int size = Math.max(8, corner * 3 + borderPx * 2);
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setBlending(Blending.None);
        fillRoundRect(pix, borderColor, 0, 0, size, size, corner);
        if (borderPx > 0) {
            fillRoundRect(pix, Color.CLEAR, borderPx, borderPx,
                    size - 2 * borderPx, size - 2 * borderPx, Math.max(0, corner - borderPx));
        }
        Texture tex = new Texture(pix);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pix.dispose();
        return new NinePatchDrawable(new NinePatch(new TextureRegion(tex), corner, corner, corner, corner));
    }

    private static int colorToHex(Color c) {
        int r = Math.round(c.r * 255) & 0xFF;
        int g = Math.round(c.g * 255) & 0xFF;
        int b = Math.round(c.b * 255) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    private static void fillRoundRect(Pixmap pix, int fillHex, int x, int y, int w, int h, int r) {
        fillRoundRect(pix, hexToColor(fillHex), x, y, w, h, r);
    }

    /** Color 版（保留 alpha，供透明填充用）。 */
    private static void fillRoundRect(Pixmap pix, Color c, int x, int y, int w, int h, int r) {
        pix.setColor(c);
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

    /** 上下方向箭头 Pixmap（24×24，实心三角形）。供 BsTimePicker 步进按钮等使用。 */
    static Pixmap arrowVerticalPixmap(Color color, boolean pointUp) {
        int size = 24;
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setBlending(Blending.None);
        pix.setColor(color);
        float cx = size / 2f, cy = size / 2f, h = size * 0.3f, w = size * 0.35f;
        if (pointUp) {
            fillTriangle(pix, cx - w, cy + h / 2f,   cx + w, cy + h / 2f,   cx, cy - h / 2f);
        } else {
            fillTriangle(pix, cx - w, cy - h / 2f,   cx + w, cy - h / 2f,   cx, cy + h / 2f);
        }
        return pix;
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable arrowDrawable(Color color, boolean pointRight) {
        return toDrawable(arrowPixmap(color, pointRight));
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable arrowVerticalDrawable(Color color, boolean pointUp) {
        return toDrawable(arrowVerticalPixmap(color, pointUp));
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

    /**
     * 白色实心圆点 Pixmap（统一生成入口）。供 BsSkinExporter 复用 + BsCircularProgress 兜底，
     * 用 batch.setColor 染成任意颜色。
     */
    static Pixmap circlePixmap(int size) {
        int s = Math.max(4, size);
        Pixmap pix = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        pix.setBlending(Blending.None);
        pix.setColor(Color.WHITE);
        pix.fillCircle(s / 2, s / 2, s / 2 - 1);
        return pix;
    }

    /** 白色圆点 Drawable（TextureRegionDrawable）。走兜底路径时由调用方按需 dispose 其 Texture。 */
    static com.badlogic.gdx.scenes.scene2d.utils.Drawable circleDrawable(int size) {
        return toDrawable(circlePixmap(size));
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
        Texture tex = null;
        try {
            tex = new Texture(com.badlogic.gdx.Gdx.files.internal(internalPath));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            // makeRoundDrawable 只从源 Texture 读像素到新 Pixmap，读完源 Texture 即可释放。
            // 若不 dispose，每次调用都会泄漏一个从文件加载的源 Texture。
            return makeRoundDrawable(
                    new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new TextureRegion(tex)),
                    size);
        } catch (Throwable t) {
            return null;
        } finally {
            // 新 Drawable 已用新 Texture（makeRoundDrawable 内部 new 的），源 Texture 不再需要
            if (tex != null) tex.dispose();
        }
    }

    /**
     * 全局纯色 Drawable 缓存：颜色 rgba int → 共享 Drawable。
     *
     * <p>所有 2×2 纯色块（hover 背景、分隔线、区间填充、斑马纹等）走这里。
     * 相同颜色返回同一个 Drawable 实例，纹理只创建一次，永不释放（全进程共享，
     * 颜色总数有限，内存占用可忽略：每个 2×2 RGBA = 16 字节像素）。</p>
     *
     * <p>调用方约定：只用于 setBackground 等只读场景，不得 dispose 返回的 Drawable
     * （它是全局共享的）。需要独立生命周期的调用方用 {@link #drawableOfUncached(Color)}。</p>
     */
    private static final ObjectMap<Integer, com.badlogic.gdx.scenes.scene2d.utils.Drawable> SOLID_CACHE = new ObjectMap<>();

    /**
     * 取一个纯色 Drawable（Pixmap 2×2 染色）。<b>默认走全局缓存</b>：相同颜色返回同一实例，
     * 不会重复创建 Texture。用于 setBackground 等只读场景；返回的 Drawable <b>不得 dispose</b>。
     *
     * <p>比 {@code skin.newDrawable("white", color)} 更可靠——不依赖 skin 里 "white" drawable 的类型，
     * 也不会踩 NinePatch 切边坑。比每次 new Pixmap+Texture 节省 GPU 内存与 GC。</p>
     *
     * <p>需要自行管理生命周期（如浮层 close 时释放）的调用方用 {@link #drawableOfUncached(Color)}。</p>
     */
    public static com.badlogic.gdx.scenes.scene2d.utils.Drawable drawableOf(Color color) {
        int key = colorKey(color);
        com.badlogic.gdx.scenes.scene2d.utils.Drawable cached = SOLID_CACHE.get(key);
        if (cached != null) return cached;
        com.badlogic.gdx.scenes.scene2d.utils.Drawable d = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new TextureRegion(solidTexture(color)));
        SOLID_CACHE.put(key, d);
        return d;
    }

    /**
     * 取一个纯色 Drawable，<b>不走缓存</b>：每次新建独立的 Texture + Drawable，调用方自行 dispose。
     *
     * <p>用于需要独立生命周期管理的场景（如临时浮层、动态色板，关闭时统一释放 Texture）。
     * 调用方可通过 {@code ((TextureRegionDrawable)d).getRegion().getTexture().dispose()} 释放底层纹理。</p>
     *
     * <p>常规场景（setBackground 等只读）应优先用 {@link #drawableOf(Color)} 走缓存。</p>
     */
    public static com.badlogic.gdx.scenes.scene2d.utils.Drawable drawableOfUncached(Color color) {
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new TextureRegion(solidTexture(color)));
    }

    /** Color → rgba packed int（用作缓存 key，避免按对象引用比较导致 new Color 同色不命中）。 */
    private static int colorKey(Color c) {
        int r = Math.round(c.r * 255) & 0xFF;
        int g = Math.round(c.g * 255) & 0xFF;
        int b = Math.round(c.b * 255) & 0xFF;
        int a = Math.round(c.a * 255) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 用指定颜色构造一个纯色 2×2 Texture（Pixmap 染色），返回给调用方自行 dispose。
     * <p>用于需要生命周期管理（如浮层 close 时统一释放）的场景；{@link #drawableOf} 是对本方法的简单包装。</p>
     */
    public static Texture solidTexture(Color color) {
        Pixmap pix = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pix.setColor(color);
        pix.fill();
        Texture tex = new Texture(pix);
        pix.dispose();
        return tex;
    }
}
