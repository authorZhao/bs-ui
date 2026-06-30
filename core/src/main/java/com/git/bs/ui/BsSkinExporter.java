package com.git.bs.ui;

// FIXME: fastjson2 依赖未引入 core 模块，json 导出功能暂时禁用
// import com.alibaba.fastjson2.JSON;
// import com.alibaba.fastjson2.JSONWriter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.PixmapPacker.PixmapPackerRectangle;
import com.badlogic.gdx.graphics.g2d.PixmapPacker.Bounds;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane.SplitPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.git.bs.common.Platform;
import com.git.bs.common.PlatformStatic;
import lombok.extern.slf4j.Slf4j;

import static com.badlogic.gdx.net.HttpRequestBuilder.json;
import static com.git.bs.ui.BsTheme.lighten;
import static com.git.bs.ui.BsTheme.darken;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Skin 导出工具：把内存中的 Skin 导出为标准 libgdx skin 包（json + atlas + png + 字体）。
 *
 * <p>用途：</p>
 * <ul>
 *   <li>把代码生成的主题 skin 导出为皮肤包，让设计师在 Skin Composer 里二次编辑</li>
 *   <li>把 dark/light 主题预生成为静态 skin 包，启动时直接 load（启动快）</li>
 *   <li>分享/分发主题</li>
 * </ul>
 *
 * <h3>导出结构</h3>
 * <pre>{@code
 * bs-light/
 * ├── bs-light.json      所有 Color / Style / Drawable 引用
 * ├── bs-light.atlas     TextureAtlas
 * ├── bs-light.png       atlas 图集
 * ├── chinese.txt        字符集（FreeType 引用）
 * └── *.ttf          FreeType 字体文件
 * }</pre>
 *
 * <h3>字符集约定</h3>
 * <p>中文字体字符集**不内嵌 json**，而是引用外部 {@code chinese.txt} 文件
 * （由 {@link BsSkinLoader} 解析时读取）。这样 json 体积小、字符集可独立升级。</p>
 *
 * <h3>Drawable 占位说明</h3>
 * <p>不通过 consumePixmap 取真实像素（会污染 Texture GPU 资源导致 JVM 崩溃）。
 * 改为按 drawable 命名约定生成纯色占位 Pixmap。加载时由 BsSkinLoader →
 * augmentWithBsStyles 重新生成真实像素（圆角/9-patch）。</p>
 *
 * <h3>Json 序列化</h3>
 * <p>使用 fastjson2，输出干净（无 class 标签、key 不带引号、可缩进）。
 * Map<String, Object> 在 fastjson2 下只输出实际内容，不会写类型元信息。</p>
 */
@Slf4j
public final class BsSkinExporter {

    public static final  Class<?>[] defaultTagClasses = {BitmapFont.class, Color.class, FreeTypeFontGenerator.class,Skin.TintedDrawable.class, NinePatchDrawable.class,
            SpriteDrawable.class, TextureRegionDrawable.class, TiledDrawable.class, Button.ButtonStyle.class,
            CheckBox.CheckBoxStyle.class, ImageButton.ImageButtonStyle.class, ImageTextButton.ImageTextButtonStyle.class,
            Label.LabelStyle.class, List.ListStyle.class, ProgressBar.ProgressBarStyle.class, ScrollPane.ScrollPaneStyle.class,
            SelectBox.SelectBoxStyle.class, Slider.SliderStyle.class, SplitPane.SplitPaneStyle.class, TextButton.TextButtonStyle.class,
            TextField.TextFieldStyle.class, TextTooltip.TextTooltipStyle.class, Touchpad.TouchpadStyle.class, Tree.TreeStyle.class,
            Window.WindowStyle.class};

    private BsSkinExporter() {}

    /**
     * 导出 Skin 到指定目录。
     *
     * @param skin       要导出的 Skin
     * @param outputDir  输出目录（FileHandle，必须可写）
     * @param name       皮肤名（用作 json/atlas/png 文件名前缀）
     * @param ttfSource  FreeType TTF 源文件（可空，null 时不导出 TTF 配置）
     * @param charsFile  字符集 .txt 文件（可空，null 时不写 characters 字段）
     */
    public static void export(Skin skin, FileHandle outputDir, String name,
                              FileHandle ttfSource, FileHandle charsFile) {



        if (!outputDir.exists()) outputDir.mkdirs();
        log.info("BsSkinExporter: 开始导出到 {}", outputDir.path());

        // ===== 0. 主动注册 libgdx 标准 Color 到桶 =====
        // 原因：style 字段引用 Color.WHITE 等静态常量时，反查需要它在桶里有名字
        ensureStandardColor(skin, "white", new Color(1, 1, 1, 1));
        ensureStandardColor(skin, "black", new Color(0, 0, 0, 1));
        ensureStandardColor(skin, "gray", new Color(0.5f, 0.5f, 0.5f, 1));
        ensureStandardColor(skin, "light-gray", new Color(0.75f, 0.75f, 0.75f, 1));
        ensureStandardColor(skin, "dark-gray", new Color(0.25f, 0.25f, 0.25f, 1));
        ensureStandardColor(skin, "clear", new Color(0, 0, 0, 0));

        // ===== 1. PixmapPacker 收集所有 Drawable 的占位 Pixmap（带圆角 + split） =====
        PixmapPacker packer = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 2, true);
        Map<String, String> drawableToRegion = new LinkedHashMap<>();
        Map<String, Integer> regionSplits = new LinkedHashMap<>();  // key → corner split（0=不写 split）

        ObjectMap<String, Drawable> drawables = skin.getAll(Drawable.class);
        if (drawables != null) {
            for (ObjectMap.Entry<String, Drawable> e : drawables) {
                String key = e.key;
                int[] splitOut = new int[1];
                Pixmap pix = makePlaceholderPixmap(key, skin, splitOut);
                packer.pack(key, pix);
                drawableToRegion.put(key, key);
                regionSplits.put(key, splitOut[0]);
                pix.dispose();
            }
        }

        // ===== 2. 写 atlas + png =====
        FileHandle atlasFile = outputDir.child(name + ".atlas");
        FileHandle pngFile = outputDir.child(name + ".png");
        try {
            if (!packer.getPages().isEmpty()) {
                Pixmap page = packer.getPages().get(0).getPixmap();
                PixmapIO.writePNG(pngFile, page);
                writeAtlasFile(atlasFile, name + ".png", packer, regionSplits);
            }
            log.info("BsSkinExporter: atlas 写入 {} ({} 个 region)", atlasFile.path(),
                    drawableToRegion.size());
        } catch (Throwable t) {
            log.warn("写 atlas 失败", t);
        }

        // ===== 3. 复制 TTF + 字符集（扁平结构：与 json 同级，多主题共用同一份） =====
        // 字体放在 outputDir 根目录（不再用 ttf/ 子目录），导出多个主题时同一份 ttf 只复制一次。
        // json 内对字体的引用路径也相应改为同目录下的 <ttf 文件名>。
        if (ttfSource != null && ttfSource.exists()) {
            FileHandle ttfDest = outputDir.child(ttfSource.name());
            if (!ttfDest.exists()) ttfSource.copyTo(ttfDest);
            log.info("BsSkinExporter: TTF 复制到 {}", ttfDest.path());
        }
        // 字符集也放根目录，保留原文件名（不再写死 chinese.txt），多主题共用同一份。
        if (charsFile != null && charsFile.exists()) {
            FileHandle charsDest = outputDir.child(charsFile.name());
            if (!charsDest.exists()) charsFile.copyTo(charsDest);
            log.info("BsSkinExporter: 字符集复制到 {}", charsDest.path());
        }

        // ===== 4. 写 json =====
        FileHandle jsonFile = outputDir.child(name + ".json");
        writeJsonFile(jsonFile, skin,
                ttfSource != null ? ttfSource.name() : null,
                charsFile != null ? charsFile.name() : null);
        log.info("BsSkinExporter: json 写入 {}", jsonFile.path());

        packer.dispose();
        log.info("BsSkinExporter: 导出完成");
    }

    // =================== 占位 Pixmap 生成（带圆角） ===================

    /**
     * 按 drawable key 命名约定生成圆角 Pixmap（与 BsSkinFactory.roundRect 同一套算法）。
     * 这样导出的 png/atlas 直接就是圆角 + 9-patch split，普通 load() 即可使用，
     * 不再依赖 augmentWithBsStyles 重画。
     *
     * <p>非圆角类型（white / cursor / v-line / arrow / checkbox / radio）保留直角占位。</p>
     *
     * @param splitOut  出参：返回此 region 的 NinePatch split 值（corner）；
     *                  0 表示不写 split（普通 TextureRegion）；null 表示调用方不关心
     */
    private static Pixmap makePlaceholderPixmap(String key, Skin skin, int[] splitOut) {
        // ---- 1. 非圆角特例（复用 BsSkinFactory 同款算法，保证一致） ----
        if (key.equals("white")) {
            if (splitOut != null) splitOut[0] = 0;
            Pixmap pix = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
            pix.setColor(Color.WHITE);
            pix.fill();
            return pix;
        }
        // BsSkinFactory 里 newDrawable("white", tint) 派生但主动注册到桶的特殊 drawable
        // 必须按派生时的 tint 颜色画对应 pixmap，否则会掉进后面的灰色兜底（bug 根因）
        if (key.equals("bs-transparent")) {
            // bs-link 用作 up/over/down，完全透明（无背景）
            if (splitOut != null) splitOut[0] = 0;
            Pixmap pix = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
            pix.setColor(new Color(0, 0, 0, 0));
            pix.fill();
            return pix;
        }
        if (key.equals("bs-overlay-bg")) {
            // WindowStyle.stageBackground 用，半透明黑色遮罩
            if (splitOut != null) splitOut[0] = 0;
            Color c = colorOf(skin, "bs-overlay");
            Pixmap pix = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
            pix.setColor(c);
            pix.fill();
            return pix;
        }
        if (key.equals("bs-split-handle")) {
            // SplitPane handle 用，纯色（border-strong）
            if (splitOut != null) splitOut[0] = 0;
            Color c = colorOf(skin, "bs-border-strong");
            Pixmap pix = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
            pix.setColor(c);
            pix.fill();
            return pix;
        }
        if (key.contains("cursor") || key.contains("v-line") || key.endsWith("-cursor")) {
            // 文本光标：2×16 竖线
            if (splitOut != null) splitOut[0] = 0;
            return BsSkinFactory.vLinePixmap(colorOf(skin, "bs-cursor"));
        }
        if (key.contains("arrow")) {
            // 箭头：24×24 三角形（左/右/上/下 + 禁用色）
            if (splitOut != null) splitOut[0] = 0;
            Color c = key.contains("disabled") ? colorOf(skin, "bs-text-disabled") : colorOf(skin, "bs-primary");
            if (key.endsWith("up")) return BsSkinFactory.arrowVerticalPixmap(c, true);
            if (key.endsWith("down")) return BsSkinFactory.arrowVerticalPixmap(c, false);
            return BsSkinFactory.arrowPixmap(c, key.endsWith("right"));
        }
        if (key.startsWith("bs-check-") || key.startsWith("bs-radio-")) {
            // checkbox / radio：复用 BsSkinFactory 工厂，确保勾形完整
            if (splitOut != null) splitOut[0] = 0;
            boolean isOn = key.endsWith("-on");
            boolean isRadio = key.startsWith("bs-radio-");
            Color primary = colorOf(skin, "bs-primary");
            Color border = colorOf(skin, "bs-border");
            Color surface = colorOf(skin, "bs-bg-elevated");
            return isRadio
                    ? BsSkinFactory.radioPixmap(isOn, border, surface, primary)
                    : BsSkinFactory.checkboxPixmap(isOn, border, surface, primary);
        }
        if (key.equals("bs-circle")) {
            // 白色圆点（BsCircularProgress 等运行时 batch.setColor 染色）
            if (splitOut != null) splitOut[0] = 0;
            return BsSkinFactory.circlePixmap(16);
        }

        // ---- 2. 圆角矩形：按 key 命名约定解析 (fillColor, borderColor, corner, border) ----
        Spec spec = resolveRoundRectSpec(key, skin);
        if (spec == null) {
            // 不识别的 key：直角灰色兜底
            if (splitOut != null) splitOut[0] = 0;
            Pixmap pix = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
            pix.setColor(new Color(0.3f, 0.3f, 0.3f, 1f));
            pix.fill();
            return pix;
        }
        int size = spec.size;
        int corner = spec.corner;
        int borderPx = spec.borderPx;
        if (splitOut != null) splitOut[0] = corner;

        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setBlending(Pixmap.Blending.None);
        boolean diffBorder = borderPx > 0 && !sameRgb(spec.fillColor, spec.borderColor);
        if (diffBorder) {
            fillRoundRect(pix, spec.borderColor, 0, 0, size, size, corner);
            fillRoundRect(pix, spec.fillColor, borderPx, borderPx,
                    size - 2 * borderPx, size - 2 * borderPx,
                    Math.max(0, corner - borderPx));
        } else {
            fillRoundRect(pix, spec.fillColor, 0, 0, size, size, corner);
        }
        return pix;
    }

    /** 圆角矩形规格。 */
    private static class Spec {
        final Color fillColor, borderColor;
        final int corner, borderPx, size;
        Spec(Color fill, Color border, int corner, int borderPx) {
            this.fillColor = fill; this.borderColor = border;
            this.corner = corner; this.borderPx = borderPx;
            this.size = Math.max(8, corner * 3 + borderPx * 2);
        }
    }

    /**
     * 按 BsSkinFactory.roundRect 的命名约定解析 key → (fill, border, corner, borderPx)。
     * 返回 null 表示不是圆角矩形。
     */
    private static Spec resolveRoundRectSpec(String key, Skin skin) {
        try {
            // 6 色循环：bs-{name}-(up|hover|active|disabled|outline-up|outline-hover|outline-active|checked|soft-bg)
            String[] palette = {"primary", "secondary", "success", "danger", "warning", "info"};
            for (String name : palette) {
                if (!key.equals("bs-" + name + "-up") && !key.equals("bs-" + name + "-hover")
                        && !key.equals("bs-" + name + "-active") && !key.equals("bs-" + name + "-disabled")
                        && !key.equals("bs-" + name + "-checked")
                        && !key.equals("bs-" + name + "-outline-up")
                        && !key.equals("bs-" + name + "-outline-hover")
                        && !key.equals("bs-" + name + "-outline-active")
                        && !key.equals("bs-" + name + "-soft-bg")) {
                    continue;
                }
                Color main = colorOf(skin, "bs-" + name);
                Color hover = lighten(main, 0.07f);
                Color active = darken(main, 0.07f);
                Color softBg = softBlend(main);
                Color td = colorOf(skin, "bs-text-disabled");
                Color be = colorOf(skin, "bs-bg-elevated");
                Color outlineHover = main; // outline-hover 用主色填充
                switch (key.substring(("bs-" + name + "-").length())) {
                    case "up":           return new Spec(main, main, 8, 1);
                    case "hover":        return new Spec(hover, hover, 8, 1);
                    case "active":       return new Spec(active, active, 8, 1);
                    case "disabled":     return new Spec(td, td, 8, 1);
                    case "checked":      return new Spec(active, active, 8, 1);
                    case "outline-up":   return new Spec(be, main, 8, 1);
                    case "outline-hover":return new Spec(outlineHover, outlineHover, 8, 1);
                    case "outline-active":return new Spec(active, active, 8, 1);
                    case "soft-bg":      return new Spec(softBg, softBg, 6, 0);
                }
            }

            // 通用 bs-* 圆角 drawable
            Color bd = colorOf(skin, "bs-border");
            Color bds = colorOf(skin, "bs-border-strong");
            Color be = colorOf(skin, "bs-bg-elevated");
            Color bs = colorOf(skin, "bs-bg-surface");
            Color primary = colorOf(skin, "bs-primary");
            Color focusRing = colorOf(skin, "bs-focus-ring");
            Color bgH = colorOf(skin, "bs-bg-header");
            switch (key) {
                case "bs-window-bg":           return new Spec(bs, bd, 10, 1);
                case "bs-text-field-bg":       return new Spec(be, bd, 6, 1);
                case "bs-text-field-focus":    return new Spec(be, focusRing, 6, 1);
                case "bs-text-field-selection":return new Spec(primary, primary, 4, 0);
                case "bs-list-bg":             return new Spec(be, bds, 6, 1);
                case "bs-list-selection":      return new Spec(primary, primary, 6, 0);
                case "bs-scrollpane-h-bar":    return new Spec(bds, bds, 3, 1);
                case "bs-scrollpane-v-bar":    return new Spec(bds, bds, 3, 1);
                case "bs-tooltip-bg":          return new Spec(new Color(0,0,0,1f), new Color(0,0,0,1f), 6, 1);
                case "bs-menu-bar-bg":         return new Spec(bgH, bds, 4, 1);
                case "bs-slider-bg":           return new Spec(bds, bds, 4, 0);
                case "bs-slider-knob":         return new Spec(primary, primary, 8, 0);
                case "bs-progress-track":      return new Spec(softBlend(colorOf(skin, "bs-secondary")), null, 6, 0);
                case "bs-btn-group-inactive":  return new Spec(softBlend(colorOf(skin, "bs-secondary")), null, 6, 0);
                case "bs-menu-item-up":        return new Spec(be, be, 4, 0);
                case "bs-menu-item-hover":     return new Spec(colorOf(skin, "bs-bg-hover"), colorOf(skin, "bs-bg-hover"), 4, 0);
                case "bs-menu-item-active":    return new Spec(primary, primary, 4, 0);
                case "bs-menu-title-up":       return new Spec(bgH, bgH, 4, 0);
                case "bs-menu-title-hover":    return new Spec(softBlend(colorOf(skin, "bs-secondary")), null, 4, 0);
                case "bs-menu-title-open":     return new Spec(bds, bds, 4, 0);
                // BsColorPicker 色块：必须白底圆角，运行时 newDrawable(name, userColor) 才能正确 tint。
                // 与 BsSkinFactory 注册时 roundRect(Color.WHITE, Color.WHITE, 8, 0) 对齐。
                case "bs-color-swatch-up":     return new Spec(Color.WHITE, Color.WHITE, 8, 0);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /** 主色 + 白色 9:1 混合（softBg）。 */
    private static Color softBlend(Color main) {
        float f = 0.88f;
        return new Color(
                main.r + (1 - main.r) * f,
                main.g + (1 - main.g) * f,
                main.b + (1 - main.b) * f, 1f);
    }

    private static boolean sameRgb(Color a, Color b) {
        if (a == null || b == null) return a == b;
        return Math.round(a.r * 255) == Math.round(b.r * 255)
                && Math.round(a.g * 255) == Math.round(b.g * 255)
                && Math.round(a.b * 255) == Math.round(b.b * 255);
    }

    /** 安全取 Color 桶，找不到返回灰色兜底。 */
    private static Color colorOf(Skin skin, String key) {
        if (skin.has(key, Color.class)) return skin.get(key, Color.class);
        return new Color(0.3f, 0.3f, 0.3f, 1f);
    }

    /** 注册 libgdx 标准 Color 到桶（已存在则不动，避免覆盖主题自定义同名色）。 */
    private static void ensureStandardColor(Skin skin, String key, Color c) {
        if (!skin.has(key, Color.class)) {
            skin.add(key, c, Color.class);
        }
    }

    /** 与 BsSkinFactory.fillRoundRect 同算法：十字矩形 + 4 个 fillCircle。 */
    private static void fillRoundRect(Pixmap pix, Color c, int x, int y, int w, int h, int r) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r < 0) r = 0;
        pix.setColor(c);
        if (r == 0) {
            pix.fillRectangle(x, y, w, h);
            return;
        }
        pix.fillRectangle(x + r, y, w - 2 * r, h);
        pix.fillRectangle(x, y + r, r, h - 2 * r);
        pix.fillRectangle(x + w - r, y + r, r, h - 2 * r);
        pix.fillCircle(x + r, y + r, r);
        pix.fillCircle(x + w - r - 1, y + r, r);
        pix.fillCircle(x + r, y + h - r - 1, r);
        pix.fillCircle(x + w - r - 1, y + h - r - 1, r);
    }

    // =================== atlas 文本写入 ===================
    private static void writeAtlasFile(FileHandle out, String pngName, PixmapPacker packer,
                                       Map<String, Integer> regionSplits) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(pngName).append("\n");
        Pixmap pagePixmap = packer.getPages().get(0).getPixmap();
        sb.append("size: ").append(pagePixmap.getWidth())
                .append(",").append(pagePixmap.getHeight()).append("\n");
        sb.append("format: RGBA8888\n");
        sb.append("filter: Linear,Linear\n");
        sb.append("repeat: none\n");

        PixmapPacker.Page page = packer.getPages().get(0);
        for (ObjectMap.Entry<String, PixmapPackerRectangle> e : page.getRects()) {
            String name = e.key;
            PixmapPackerRectangle r = e.value;
            PixmapPacker.Bounds b = r.bounds;
            sb.append(name).append("\n");
            sb.append("rotate: false\n");
            sb.append("xy: ").append(b.x).append(", ").append(b.y).append("\n");
            sb.append("size: ").append(b.width).append(", ").append(b.height).append("\n");
            sb.append("orig: ").append(b.width).append(", ").append(b.height).append("\n");
            sb.append("offset: 0, 0\n");
            // NinePatch split：libgdx TextureAtlas 读到 split 字段会自动构造成 NinePatch
            int split = regionSplits != null && regionSplits.containsKey(name)
                    ? regionSplits.get(name) : 0;
            if (split > 0) {
                sb.append("split: ").append(split).append(",")
                        .append(split).append(",")
                        .append(split).append(",")
                        .append(split).append("\n");
            }
            sb.append("index: -1\n");
        }

        out.writeString(sb.toString(), false);
    }

    // =================== json 写入（使用 fastjson2） ===================

    /**
     * 用 fastjson2 序列化 skin 为 libgdx skin json 格式。
     * <p>fastjson2 对 Map<String, Object> 只输出实际内容，不写 class 类型标签。</p>
     */
    private static void writeJsonFile(FileHandle out, Skin skin, String ttfPath, String charsFile) {
        Map<String, Object> root = new LinkedHashMap<>();

        // ===== BitmapFont 桶（FreeType 引用） =====
        if (ttfPath != null) {
            Map<String, Object> fontSection = new LinkedHashMap<>();
            String[] suffixes = {"default", "sm", "md", "lg", "xl"};
            int[] sizes = {18, 14, 18, 24, 32};
            for (int i = 0; i < suffixes.length; i++) {
                String skinKey = i == 0 ? "default" : "font-" + suffixes[i];
                if (skin.has(skinKey, BitmapFont.class)) {
                    String jsonKey = i == 0 ? "default-font" : "font-" + suffixes[i];
                    Map<String, Object> f = new LinkedHashMap<>();
                    f.put("font", ttfPath);
                    if (charsFile != null) f.put("characters", charsFile);
                    f.put("size", sizes[i]);
                    f.put("hinting", "AutoMedium");
                    f.put("minFilter", "Linear");
                    f.put("magFilter", "Linear");
                    fontSection.put(jsonKey, f);
                }
            }
            if (!fontSection.isEmpty()) {
                root.put("com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator", fontSection);
            }
        }

        // ===== Color 桶 =====
        ObjectMap<String, Color> skinColors = skin.getAll(Color.class);
        if (skinColors != null && skinColors.size > 0) {
            Map<String, Object> colorSection = new LinkedHashMap<>();
            for (ObjectMap.Entry<String, Color> e : skinColors) {
                Color c = e.value;
                Map<String, Object> cv = new LinkedHashMap<>();
                cv.put("r", round4(c.r));
                cv.put("g", round4(c.g));
                cv.put("b", round4(c.b));
                cv.put("a", round4(c.a));
                colorSection.put(e.key, cv);
            }
            root.put("com.badlogic.gdx.graphics.Color", colorSection);
        }

        // ===== Style 桶 =====
        addTextButtonSection(root, skin);
        addCheckBoxSection(root, skin);
        addLabelSection(root, skin);
        addSliderSection(root, skin);
        addSplitPaneSection(root, skin);
        addScrollPaneSection(root, skin);
        addListSection(root, skin);
        addTextFieldSection(root, skin);
        addSelectBoxSection(root, skin);
        addWindowSection(root, skin);
        // libgdx 自带 style 补全：覆盖 defaultTagClasses 里其余 Style 类。
        // 子类样式（ImageButton/ImageTextButton/Slider）把父类字段一并显式写出，不依赖 JSON parent。
        addButtonSection(root, skin);
        addImageButtonSection(root, skin);
        addImageTextButtonSection(root, skin);
        addProgressBarSection(root, skin);
        addTextTooltipSection(root, skin);
        addTouchpadSection(root, skin);
        addTreeSection(root, skin);

        // ===== 补齐 style 中引用的 Color（如果 Color 桶里不存在，按命名推断追加） =====
        // 例如 style 里 "overFontColor": "white"，但 Color 桶没有 "white"
        // 就追加一个 white: {r:1,g:1,b:1,a:1}，避免加载时抛
        // "No Color registered with name: white"
        ensureReferencedColors(root, skin);

        // fastjson2 序列化（带缩进、不写 class 标签）
        String jsonStr = PlatformStatic.getPlatform().toJson(root);
        out.writeString(jsonStr, false);
    }

    private static float round4(float v) {
        return Math.round(v * 10000f) / 10000f;
    }

    /**
     * 扫描所有 style section 的颜色引用字段，如果引用的 Color 名在 Color 桶里不存在，
     * 按命名约定推断颜色追加进去。
     *
     * <p>扫描字段：fontColor / disabledFontColor / overFontColor / downFontColor /
     * fontColorSelected / fontColorUnselected / titleFontColor</p>
     *
     * <p>颜色推断规则（libgdx 命名习惯）：</p>
     * <ul>
     *   <li>white / clear-white → 1,1,1,1</li>
     *   <li>black / clear-black → 0,0,0,1</li>
     *   <li>gray / grey → 0.5,0.5,0.5,1</li>
     *   <li>light-gray / light-grey → 0.75,0.75,0.75,1</li>
     *   <li>dark-gray / dark-grey → 0.25,0.25,0.25,1</li>
     *   <li>clear → 0,0,0,0（完全透明）</li>
     *   <li>其他未知名 → 兜底灰色 0.5,0.5,0.5,1（避免加载崩溃）</li>
     * </ul>
     */
    private static void ensureReferencedColors(Map<String, Object> root, Skin skin) {
        // 已经存在的 Color 名集合（主题色已有）
        java.util.Set<String> existingColorNames = new java.util.HashSet<>();
        ObjectMap<String, Color> skinColors = skin.getAll(Color.class);
        if (skinColors != null) {
            for (ObjectMap.Entry<String, Color> e : skinColors) {
                existingColorNames.add(e.key);
            }
        }

        // 颜色引用字段名（这些字段的 value 是 Color 名引用）
        java.util.Set<String> colorRefFields = new java.util.HashSet<>(java.util.Arrays.asList(
                "fontColor", "downFontColor", "overFontColor", "focusedFontColor", "disabledFontColor",
                "checkedFontColor", "checkedDownFontColor", "checkedOverFontColor", "checkedFocusedFontColor",
                "fontColorSelected", "fontColorUnselected",
                "titleFontColor",
                "focusedFontColor", "messageFontColor"));

        // 收集所有需要补齐的颜色名
        java.util.Map<String, Color> needAdd = new LinkedHashMap<>();

        for (Map.Entry<String, Object> rootEntry : root.entrySet()) {
            String rootKey = rootEntry.getKey();
            // 只看 style section（com.badlogic.gdx.scenes.scene2d.ui.*$*Style）
            if (!rootKey.contains("$") || !rootKey.endsWith("Style")) continue;
            Object rootValue = rootEntry.getValue();
            if (!(rootValue instanceof Map)) continue;

            for (Object styleEntry : ((Map<?, ?>) rootValue).values()) {
                if (!(styleEntry instanceof Map)) continue;
                Map<?, ?> styleMap = (Map<?, ?>) styleEntry;
                for (Map.Entry<?, ?> field : styleMap.entrySet()) {
                    String fieldName = String.valueOf(field.getKey());
                    if (!colorRefFields.contains(fieldName)) continue;
                    Object fieldValue = field.getValue();
                    if (!(fieldValue instanceof String)) continue;
                    String colorName = (String) fieldValue;
                    // 已存在或已加入 → 跳过
                    if (existingColorNames.contains(colorName) || needAdd.containsKey(colorName)) {
                        continue;
                    }
                    Color inferred = inferColorByName(colorName);
                    if (inferred != null) {
                        needAdd.put(colorName, inferred);
                    }
                }
            }
        }

        if (needAdd.isEmpty()) return;

        // 追加到 Color section
        Object colorSectionObj = root.get("com.badlogic.gdx.graphics.Color");
        if (colorSectionObj == null) {
            colorSectionObj = new LinkedHashMap<String, Object>();
            root.put("com.badlogic.gdx.graphics.Color", colorSectionObj);
        }
        if (colorSectionObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> colorSection = (Map<String, Object>) colorSectionObj;
            for (Map.Entry<String, Color> e : needAdd.entrySet()) {
                Color c = e.getValue();
                Map<String, Object> cv = new LinkedHashMap<>();
                cv.put("r", round4(c.r));
                cv.put("g", round4(c.g));
                cv.put("b", round4(c.b));
                cv.put("a", round4(c.a));
                colorSection.put(e.getKey(), cv);
                log.info("补齐 Color 引用: {} -> {},{},{},{}", e.getKey(),
                        round4(c.r), round4(c.g), round4(c.b), round4(c.a));
            }
        }
    }

    /** 按命名约定推断 libgdx 标准 Color。返回 null 表示无法推断。 */
    private static Color inferColorByName(String name) {
        switch (name) {
            case "white":       return new Color(1, 1, 1, 1);
            case "black":       return new Color(0, 0, 0, 1);
            case "gray":
            case "grey":        return new Color(0.5f, 0.5f, 0.5f, 1);
            case "light-gray":
            case "light-grey":  return new Color(0.75f, 0.75f, 0.75f, 1);
            case "dark-gray":
            case "dark-grey":   return new Color(0.25f, 0.25f, 0.25f, 1);
            case "clear":       return new Color(0, 0, 0, 0);
            case "transparent": return new Color(0, 0, 0, 0);
            case "red":         return new Color(1, 0, 0, 1);
            case "green":       return new Color(0, 1, 0, 1);
            case "blue":        return new Color(0, 0, 1, 1);
            case "yellow":      return new Color(1, 1, 0, 1);
            case "cyan":        return new Color(0, 1, 1, 1);
            case "magenta":     return new Color(1, 0, 1, 1);
            case "orange":      return new Color(1, 0.5f, 0, 1);
            case "pink":        return new Color(1, 0.68f, 0.68f, 1);
            default:            return new Color(0.5f, 0.5f, 0.5f, 1);  // 兜底灰色
        }
    }

    /**
     * 从 skin Color 桶反查 Color 对象的 key 名。
     * 1. 引用相等（==）优先；
     * 2. 失败则按 RGBA 值相等回退（Color.WHITE / new Color(1,1,1,1) 都能匹配到桶里的 "white"）；
     * 3. 都失败则返回 null（让调用方决定是否写这个字段）。
     */
    private static String resolveColorName(Skin skin, Color c) {
        if (c == null) return null;
        ObjectMap<String, Color> colors = skin.getAll(Color.class);
        if (colors != null) {
            // pass 1：引用相等
            for (ObjectMap.Entry<String, Color> e : colors) {
                if (e.value == c) return e.key;
            }
            // pass 2：值相等
            int cr = Math.round(c.r * 255), cg = Math.round(c.g * 255), cb = Math.round(c.b * 255);
            int ca = Math.round(c.a * 255);
            for (ObjectMap.Entry<String, Color> e : colors) {
                Color o = e.value;
                if (Math.round(o.r * 255) == cr && Math.round(o.g * 255) == cg
                        && Math.round(o.b * 255) == cb && Math.round(o.a * 255) == ca) {
                    return e.key;
                }
            }
        }
        return null;
    }

    /**
     * 从 skin Drawable 桶反查 Drawable 对象的 key 名（引用相等）。
     * 找不到时返回 fallback（若 fallback 在 skin 中也不存在则返回 null，跳过该字段）。
     */
    private static String resolveDrawableName(Skin skin, Drawable d, String fallback) {
        if (d == null) return null;
        ObjectMap<String, Drawable> drawables = skin.getAll(Drawable.class);
        if (drawables != null) {
            for (ObjectMap.Entry<String, Drawable> e : drawables) {
                if (e.value == d) return e.key;
            }
        }
        // 反查不到 → 用兜底 key（仅当 skin 里确实存在该 key 时才返回）
        if (fallback != null && skin.has(fallback, Drawable.class)) {
            return fallback;
        }
        return null;
    }

    /** 便捷重载：无兜底。 */
    private static String resolveDrawableName(Skin skin, Drawable d) {
        return resolveDrawableName(skin, d, null);
    }

    /**
     * 把 drawable 字段写入 style map：反查优先，找不到用 fallback，再找不到跳过。
     * 同时把反查到的 key 加进 drawableToRegion（其实 atlas 阶段已收集，这里只是 no-op 占位）。
     */
    private static void putDrawable(Map<String, Object> v, String field,
                                    Skin skin, Drawable d, String fallback) {
        String name = resolveDrawableName(skin, d, fallback);
        if (name != null) v.put(field, name);
    }

    /**
     * 把 Color 字段写入 style map：反查（引用 + 值相等）失败则跳过，避免写 null。
     */
    private static void putColor(Map<String, Object> v, String field, Skin skin, Color c) {
        if (c == null) return;
        String name = resolveColorName(skin, c);
        if (name != null) v.put(field, name);
    }

    // =================== 各 style section ===================

    private static void addTextButtonSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, TextButtonStyle> ss = skin.getAll(TextButtonStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, TextButtonStyle> e : ss.entries()) {
            TextButtonStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            // font
            if (s.font != null) v.put("font", "default-font");
            // fontColor 全套（TextButtonStyle 本类）
            putColor(v, "fontColor", skin, s.fontColor);
            putColor(v, "downFontColor", skin, s.downFontColor);
            putColor(v, "overFontColor", skin, s.overFontColor);
            putColor(v, "focusedFontColor", skin, s.focusedFontColor);
            putColor(v, "disabledFontColor", skin, s.disabledFontColor);
            putColor(v, "checkedFontColor", skin, s.checkedFontColor);
            putColor(v, "checkedDownFontColor", skin, s.checkedDownFontColor);
            putColor(v, "checkedOverFontColor", skin, s.checkedOverFontColor);
            putColor(v, "checkedFocusedFontColor", skin, s.checkedFocusedFontColor);
            // Drawable 全套（继承自 ButtonStyle）
            if (s.up != null) putDrawable(v, "up", skin, s.up, "bs-primary-up");
            if (s.down != null) putDrawable(v, "down", skin, s.down, "bs-primary-active");
            if (s.over != null) putDrawable(v, "over", skin, s.over, "bs-primary-hover");
            if (s.focused != null) putDrawable(v, "focused", skin, s.focused, null);
            if (s.disabled != null) putDrawable(v, "disabled", skin, s.disabled, "bs-primary-disabled");
            if (s.checked != null) putDrawable(v, "checked", skin, s.checked, "bs-primary-checked");
            if (s.checkedOver != null) putDrawable(v, "checkedOver", skin, s.checkedOver, null);
            if (s.checkedDown != null) putDrawable(v, "checkedDown", skin, s.checkedDown, null);
            if (s.checkedFocused != null) putDrawable(v, "checkedFocused", skin, s.checkedFocused, null);
            // float offset（libgdx 默认 0，非 0 才导出）
            if (s.pressedOffsetX != 0f) v.put("pressedOffsetX", s.pressedOffsetX);
            if (s.pressedOffsetY != 0f) v.put("pressedOffsetY", s.pressedOffsetY);
            if (s.unpressedOffsetX != 0f) v.put("unpressedOffsetX", s.unpressedOffsetX);
            if (s.unpressedOffsetY != 0f) v.put("unpressedOffsetY", s.unpressedOffsetY);
            if (s.checkedOffsetX != 0f) v.put("checkedOffsetX", s.checkedOffsetX);
            if (s.checkedOffsetY != 0f) v.put("checkedOffsetY", s.checkedOffsetY);
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.TextButton$TextButtonStyle", section);
    }

    private static void addCheckBoxSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, CheckBoxStyle> ss = skin.getAll(CheckBoxStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, CheckBoxStyle> e : ss.entries()) {
            CheckBoxStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            // font（CheckBoxStyle 继承 TextButtonStyle）
            if (s.font != null) v.put("font", "default-font");
            // fontColor 全套（继承自 TextButtonStyle）
            putColor(v, "fontColor", skin, s.fontColor);
            putColor(v, "downFontColor", skin, s.downFontColor);
            putColor(v, "overFontColor", skin, s.overFontColor);
            putColor(v, "focusedFontColor", skin, s.focusedFontColor);
            putColor(v, "disabledFontColor", skin, s.disabledFontColor);
            putColor(v, "checkedFontColor", skin, s.checkedFontColor);
            putColor(v, "checkedDownFontColor", skin, s.checkedDownFontColor);
            putColor(v, "checkedOverFontColor", skin, s.checkedOverFontColor);
            putColor(v, "checkedFocusedFontColor", skin, s.checkedFocusedFontColor);
            // Drawable 继承自 ButtonStyle
            if (s.up != null) putDrawable(v, "up", skin, s.up, null);
            if (s.down != null) putDrawable(v, "down", skin, s.down, null);
            if (s.over != null) putDrawable(v, "over", skin, s.over, null);
            if (s.focused != null) putDrawable(v, "focused", skin, s.focused, null);
            if (s.disabled != null) putDrawable(v, "disabled", skin, s.disabled, null);
            if (s.checked != null) putDrawable(v, "checked", skin, s.checked, null);
            if (s.checkedOver != null) putDrawable(v, "checkedOver", skin, s.checkedOver, null);
            if (s.checkedDown != null) putDrawable(v, "checkedDown", skin, s.checkedDown, null);
            if (s.checkedFocused != null) putDrawable(v, "checkedFocused", skin, s.checkedFocused, null);
            // CheckBoxStyle 本类专属 drawable
            if (s.checkboxOn != null) putDrawable(v, "checkboxOn", skin, s.checkboxOn, "bs-check-on");
            if (s.checkboxOff != null) putDrawable(v, "checkboxOff", skin, s.checkboxOff, "bs-check-off");
            if (s.checkboxOnOver != null) putDrawable(v, "checkboxOnOver", skin, s.checkboxOnOver, null);
            if (s.checkboxOver != null) putDrawable(v, "checkboxOver", skin, s.checkboxOver, null);
            if (s.checkboxOnDisabled != null) putDrawable(v, "checkboxOnDisabled", skin, s.checkboxOnDisabled, null);
            if (s.checkboxOffDisabled != null) putDrawable(v, "checkboxOffDisabled", skin, s.checkboxOffDisabled, null);
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.CheckBox$CheckBoxStyle", section);
    }

    private static void addLabelSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, LabelStyle> ss = skin.getAll(LabelStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, LabelStyle> e : ss.entries()) {
            LabelStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            if (s.font != null) v.put("font", "default-font");
            putColor(v, "fontColor", skin, s.fontColor);
            if (s.background != null) putDrawable(v, "background", skin, s.background, null);
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.Label$LabelStyle", section);
    }

    private static void addSliderSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, SliderStyle> ss = skin.getAll(SliderStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, SliderStyle> e : ss.entries()) {
            SliderStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            // 继承自 ProgressBarStyle
            if (s.background != null) putDrawable(v, "background", skin, s.background, "bs-slider-bg");
            if (s.disabledBackground != null) putDrawable(v, "disabledBackground", skin, s.disabledBackground, null);
            if (s.knob != null) putDrawable(v, "knob", skin, s.knob, "bs-slider-knob");
            if (s.disabledKnob != null) putDrawable(v, "disabledKnob", skin, s.disabledKnob, null);
            if (s.knobBefore != null) putDrawable(v, "knobBefore", skin, s.knobBefore, null);
            if (s.disabledKnobBefore != null) putDrawable(v, "disabledKnobBefore", skin, s.disabledKnobBefore, null);
            if (s.knobAfter != null) putDrawable(v, "knobAfter", skin, s.knobAfter, null);
            if (s.disabledKnobAfter != null) putDrawable(v, "disabledKnobAfter", skin, s.disabledKnobAfter, null);
            // SliderStyle 专属
            if (s.backgroundOver != null) putDrawable(v, "backgroundOver", skin, s.backgroundOver, null);
            if (s.backgroundDown != null) putDrawable(v, "backgroundDown", skin, s.backgroundDown, null);
            if (s.knobOver != null) putDrawable(v, "knobOver", skin, s.knobOver, null);
            if (s.knobDown != null) putDrawable(v, "knobDown", skin, s.knobDown, null);
            if (s.knobBeforeOver != null) putDrawable(v, "knobBeforeOver", skin, s.knobBeforeOver, null);
            if (s.knobBeforeDown != null) putDrawable(v, "knobBeforeDown", skin, s.knobBeforeDown, null);
            if (s.knobAfterOver != null) putDrawable(v, "knobAfterOver", skin, s.knobAfterOver, null);
            if (s.knobAfterDown != null) putDrawable(v, "knobAfterDown", skin, s.knobAfterDown, null);
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.Slider$SliderStyle", section);
    }

    private static void addSplitPaneSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, SplitPaneStyle> ss = skin.getAll(SplitPaneStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, SplitPaneStyle> e : ss.entries()) {
            SplitPaneStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            if (s.handle != null) putDrawable(v, "handle", skin, s.handle, "white");
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.SplitPane$SplitPaneStyle", section);
    }

    private static void addScrollPaneSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, ScrollPaneStyle> ss = skin.getAll(ScrollPaneStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, ScrollPaneStyle> e : ss.entries()) {
            ScrollPaneStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            if (s.background != null) putDrawable(v, "background", skin, s.background, "bs-window-bg");
            if (s.corner != null) putDrawable(v, "corner", skin, s.corner, null);
            if (s.hScroll != null) putDrawable(v, "hScroll", skin, s.hScroll, "bs-scrollpane-h-bar");
            if (s.hScrollKnob != null) putDrawable(v, "hScrollKnob", skin, s.hScrollKnob, null);
            if (s.vScroll != null) putDrawable(v, "vScroll", skin, s.vScroll, "bs-scrollpane-v-bar");
            if (s.vScrollKnob != null) putDrawable(v, "vScrollKnob", skin, s.vScrollKnob, null);
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.ScrollPane$ScrollPaneStyle", section);
    }

    private static void addListSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, ListStyle> ss = skin.getAll(ListStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, ListStyle> e : ss.entries()) {
            ListStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            if (s.font != null) v.put("font", "default-font");
            putColor(v, "fontColorSelected", skin, s.fontColorSelected);
            putColor(v, "fontColorUnselected", skin, s.fontColorUnselected);
            if (s.selection != null) putDrawable(v, "selection", skin, s.selection, "bs-list-selection");
            if (s.down != null) putDrawable(v, "down", skin, s.down, null);
            if (s.over != null) putDrawable(v, "over", skin, s.over, null);
            if (s.background != null) putDrawable(v, "background", skin, s.background, "bs-list-bg");
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.List$ListStyle", section);
    }

    private static void addTextFieldSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, TextFieldStyle> ss = skin.getAll(TextFieldStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, TextFieldStyle> e : ss.entries()) {
            TextFieldStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            if (s.font != null) v.put("font", "default-font");
            putColor(v, "fontColor", skin, s.fontColor);
            putColor(v, "focusedFontColor", skin, s.focusedFontColor);
            putColor(v, "disabledFontColor", skin, s.disabledFontColor);
            if (s.messageFont != null) v.put("messageFont", "default-font");
            putColor(v, "messageFontColor", skin, s.messageFontColor);
            if (s.background != null) putDrawable(v, "background", skin, s.background, "bs-text-field-bg");
            if (s.focusedBackground != null) putDrawable(v, "focusedBackground", skin, s.focusedBackground, "bs-text-field-focus");
            if (s.disabledBackground != null) putDrawable(v, "disabledBackground", skin, s.disabledBackground, null);
            if (s.cursor != null) putDrawable(v, "cursor", skin, s.cursor, "bs-text-field-cursor");
            if (s.selection != null) putDrawable(v, "selection", skin, s.selection, "bs-text-field-selection");
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.TextField$TextFieldStyle", section);
    }

    private static void addSelectBoxSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, SelectBoxStyle> ss = skin.getAll(SelectBoxStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, SelectBoxStyle> e : ss.entries()) {
            SelectBoxStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            if (s.font != null) v.put("font", "default-font");
            putColor(v, "fontColor", skin, s.fontColor);
            putColor(v, "overFontColor", skin, s.overFontColor);
            putColor(v, "disabledFontColor", skin, s.disabledFontColor);
            if (s.background != null) putDrawable(v, "background", skin, s.background, "bs-text-field-bg");
            if (s.backgroundOver != null) putDrawable(v, "backgroundOver", skin, s.backgroundOver, "bs-text-field-focus");
            if (s.backgroundOpen != null) putDrawable(v, "backgroundOpen", skin, s.backgroundOpen, "bs-text-field-focus");
            if (s.backgroundDisabled != null) putDrawable(v, "backgroundDisabled", skin, s.backgroundDisabled, null);
            // 嵌套 style 引用：按用户选择固定 "default"
            if (s.scrollStyle != null) v.put("scrollStyle", "default");
            if (s.listStyle != null) v.put("listStyle", "default");
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.SelectBox$SelectBoxStyle", section);
    }

    private static void addWindowSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, WindowStyle> ss = skin.getAll(WindowStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, WindowStyle> e : ss.entries()) {
            WindowStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            if (s.titleFont != null) v.put("titleFont", "default-font");
            putColor(v, "titleFontColor", skin, s.titleFontColor);
            if (s.background != null) putDrawable(v, "background", skin, s.background, "bs-window-bg");
            if (s.stageBackground != null) putDrawable(v, "stageBackground", skin, s.stageBackground, "white");
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.Window$WindowStyle", section);
    }

    // =================== libgdx 自带 style 补全（defaultTagClasses 中之前漏导出的 7 种） ===================
    //
    // 继承关系处理：
    //   ImageButtonStyle      extends ButtonStyle
    //   ImageTextButtonStyle  extends TextButtonStyle (extends ButtonStyle)
    //   SliderStyle           extends ProgressBarStyle   ← 已有 addSliderSection，这里补 ProgressBar 基类
    //   CheckBoxStyle/TextButtonStyle 同理（已有）
    //
    // 做法：子类 section 把父类字段（up/down/over/checked/offset、font/fontColor、background/knob...）
    //       **全部显式写出**，和现有 addCheckBoxSection / addSliderSection 一致。
    // 不使用 JSON 的 "parent" 字段：内存里的 Style 对象没有记录「我是从哪个具名 style 继承来的」，
    // 靠对象引用相等去猜父子关系不可靠；而全字段导出后，加载端得到的样式和 in-memory 完全一致，
    // parent 只是 JSON 的简写（让设计师手写更省字），导出场景下没必要。

    private static void addButtonSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, Button.ButtonStyle> ss = skin.getAll(Button.ButtonStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, Button.ButtonStyle> e : ss.entries()) {
            Button.ButtonStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            if (s.up != null) putDrawable(v, "up", skin, s.up, "bs-primary-up");
            if (s.down != null) putDrawable(v, "down", skin, s.down, "bs-primary-active");
            if (s.over != null) putDrawable(v, "over", skin, s.over, "bs-primary-hover");
            if (s.focused != null) putDrawable(v, "focused", skin, s.focused, null);
            if (s.disabled != null) putDrawable(v, "disabled", skin, s.disabled, "bs-primary-disabled");
            if (s.checked != null) putDrawable(v, "checked", skin, s.checked, "bs-primary-checked");
            if (s.checkedOver != null) putDrawable(v, "checkedOver", skin, s.checkedOver, null);
            if (s.checkedDown != null) putDrawable(v, "checkedDown", skin, s.checkedDown, null);
            if (s.checkedFocused != null) putDrawable(v, "checkedFocused", skin, s.checkedFocused, null);
            // float offset（libgdx 默认 0，非 0 才导出）
            if (s.pressedOffsetX != 0f) v.put("pressedOffsetX", s.pressedOffsetX);
            if (s.pressedOffsetY != 0f) v.put("pressedOffsetY", s.pressedOffsetY);
            if (s.unpressedOffsetX != 0f) v.put("unpressedOffsetX", s.unpressedOffsetX);
            if (s.unpressedOffsetY != 0f) v.put("unpressedOffsetY", s.unpressedOffsetY);
            if (s.checkedOffsetX != 0f) v.put("checkedOffsetX", s.checkedOffsetX);
            if (s.checkedOffsetY != 0f) v.put("checkedOffsetY", s.checkedOffsetY);
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle", section);
    }

    private static void addImageButtonSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, ImageButton.ImageButtonStyle> ss = skin.getAll(ImageButton.ImageButtonStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, ImageButton.ImageButtonStyle> e : ss.entries()) {
            ImageButton.ImageButtonStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            // 本类专属：image* 系列（图标）
            if (s.imageUp != null) putDrawable(v, "imageUp", skin, s.imageUp, "bs-primary-up");
            if (s.imageDown != null) putDrawable(v, "imageDown", skin, s.imageDown, "bs-primary-active");
            if (s.imageOver != null) putDrawable(v, "imageOver", skin, s.imageOver, "bs-primary-hover");
            if (s.imageChecked != null) putDrawable(v, "imageChecked", skin, s.imageChecked, "bs-primary-checked");
            if (s.imageCheckedDown != null) putDrawable(v, "imageCheckedDown", skin, s.imageCheckedDown, null);
            if (s.imageCheckedOver != null) putDrawable(v, "imageCheckedOver", skin, s.imageCheckedOver, null);
            if (s.imageDisabled != null) putDrawable(v, "imageDisabled", skin, s.imageDisabled, "bs-primary-disabled");
            // 继承自 ButtonStyle：up/down/over/checked + offset（同 addButtonSection）
            if (s.up != null) putDrawable(v, "up", skin, s.up, "bs-primary-up");
            if (s.down != null) putDrawable(v, "down", skin, s.down, "bs-primary-active");
            if (s.over != null) putDrawable(v, "over", skin, s.over, "bs-primary-hover");
            if (s.focused != null) putDrawable(v, "focused", skin, s.focused, null);
            if (s.disabled != null) putDrawable(v, "disabled", skin, s.disabled, "bs-primary-disabled");
            if (s.checked != null) putDrawable(v, "checked", skin, s.checked, "bs-primary-checked");
            if (s.checkedOver != null) putDrawable(v, "checkedOver", skin, s.checkedOver, null);
            if (s.checkedDown != null) putDrawable(v, "checkedDown", skin, s.checkedDown, null);
            if (s.checkedFocused != null) putDrawable(v, "checkedFocused", skin, s.checkedFocused, null);
            if (s.pressedOffsetX != 0f) v.put("pressedOffsetX", s.pressedOffsetX);
            if (s.pressedOffsetY != 0f) v.put("pressedOffsetY", s.pressedOffsetY);
            if (s.unpressedOffsetX != 0f) v.put("unpressedOffsetX", s.unpressedOffsetX);
            if (s.unpressedOffsetY != 0f) v.put("unpressedOffsetY", s.unpressedOffsetY);
            if (s.checkedOffsetX != 0f) v.put("checkedOffsetX", s.checkedOffsetX);
            if (s.checkedOffsetY != 0f) v.put("checkedOffsetY", s.checkedOffsetY);
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.ImageButton$ImageButtonStyle", section);
    }

    private static void addImageTextButtonSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, ImageTextButton.ImageTextButtonStyle> ss = skin.getAll(ImageTextButton.ImageTextButtonStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, ImageTextButton.ImageTextButtonStyle> e : ss.entries()) {
            ImageTextButton.ImageTextButtonStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            // 继承自 TextButtonStyle：font + fontColor 全套
            if (s.font != null) v.put("font", "default-font");
            putColor(v, "fontColor", skin, s.fontColor);
            putColor(v, "downFontColor", skin, s.downFontColor);
            putColor(v, "overFontColor", skin, s.overFontColor);
            putColor(v, "focusedFontColor", skin, s.focusedFontColor);
            putColor(v, "disabledFontColor", skin, s.disabledFontColor);
            putColor(v, "checkedFontColor", skin, s.checkedFontColor);
            putColor(v, "checkedDownFontColor", skin, s.checkedDownFontColor);
            putColor(v, "checkedOverFontColor", skin, s.checkedOverFontColor);
            putColor(v, "checkedFocusedFontColor", skin, s.checkedFocusedFontColor);
            // 本类专属：image* 系列
            if (s.imageUp != null) putDrawable(v, "imageUp", skin, s.imageUp, "bs-primary-up");
            if (s.imageDown != null) putDrawable(v, "imageDown", skin, s.imageDown, "bs-primary-active");
            if (s.imageOver != null) putDrawable(v, "imageOver", skin, s.imageOver, "bs-primary-hover");
            if (s.imageChecked != null) putDrawable(v, "imageChecked", skin, s.imageChecked, "bs-primary-checked");
            if (s.imageCheckedDown != null) putDrawable(v, "imageCheckedDown", skin, s.imageCheckedDown, null);
            if (s.imageCheckedOver != null) putDrawable(v, "imageCheckedOver", skin, s.imageCheckedOver, null);
            if (s.imageDisabled != null) putDrawable(v, "imageDisabled", skin, s.imageDisabled, "bs-primary-disabled");
            // 继承自 ButtonStyle：up/down/over/checked + offset
            if (s.up != null) putDrawable(v, "up", skin, s.up, "bs-primary-up");
            if (s.down != null) putDrawable(v, "down", skin, s.down, "bs-primary-active");
            if (s.over != null) putDrawable(v, "over", skin, s.over, "bs-primary-hover");
            if (s.focused != null) putDrawable(v, "focused", skin, s.focused, null);
            if (s.disabled != null) putDrawable(v, "disabled", skin, s.disabled, "bs-primary-disabled");
            if (s.checked != null) putDrawable(v, "checked", skin, s.checked, "bs-primary-checked");
            if (s.checkedOver != null) putDrawable(v, "checkedOver", skin, s.checkedOver, null);
            if (s.checkedDown != null) putDrawable(v, "checkedDown", skin, s.checkedDown, null);
            if (s.checkedFocused != null) putDrawable(v, "checkedFocused", skin, s.checkedFocused, null);
            if (s.pressedOffsetX != 0f) v.put("pressedOffsetX", s.pressedOffsetX);
            if (s.pressedOffsetY != 0f) v.put("pressedOffsetY", s.pressedOffsetY);
            if (s.unpressedOffsetX != 0f) v.put("unpressedOffsetX", s.unpressedOffsetX);
            if (s.unpressedOffsetY != 0f) v.put("unpressedOffsetY", s.unpressedOffsetY);
            if (s.checkedOffsetX != 0f) v.put("checkedOffsetX", s.checkedOffsetX);
            if (s.checkedOffsetY != 0f) v.put("checkedOffsetY", s.checkedOffsetY);
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton$ImageTextButtonStyle", section);
    }

    private static void addProgressBarSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, ProgressBar.ProgressBarStyle> ss = skin.getAll(ProgressBar.ProgressBarStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, ProgressBar.ProgressBarStyle> e : ss.entries()) {
            ProgressBar.ProgressBarStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            // ProgressBarStyle 是 SliderStyle 的基类；字段与 addSliderSection 中的基类部分一致
            if (s.background != null) putDrawable(v, "background", skin, s.background, "bs-progress-track");
            if (s.disabledBackground != null) putDrawable(v, "disabledBackground", skin, s.disabledBackground, null);
            if (s.knob != null) putDrawable(v, "knob", skin, s.knob, "bs-slider-knob");
            if (s.disabledKnob != null) putDrawable(v, "disabledKnob", skin, s.disabledKnob, null);
            if (s.knobBefore != null) putDrawable(v, "knobBefore", skin, s.knobBefore, "bs-primary-up");
            if (s.disabledKnobBefore != null) putDrawable(v, "disabledKnobBefore", skin, s.disabledKnobBefore, null);
            if (s.knobAfter != null) putDrawable(v, "knobAfter", skin, s.knobAfter, null);
            if (s.disabledKnobAfter != null) putDrawable(v, "disabledKnobAfter", skin, s.disabledKnobAfter, null);
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.ProgressBar$ProgressBarStyle", section);
    }

    private static void addTextTooltipSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, TextTooltip.TextTooltipStyle> ss = skin.getAll(TextTooltip.TextTooltipStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, TextTooltip.TextTooltipStyle> e : ss.entries()) {
            TextTooltip.TextTooltipStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            // label 是嵌套 LabelStyle 引用：反查名字，找不到回退 "default"
            if (s.label != null) {
                String ref = resolveStyleRef(skin, s.label, "default");
                if (ref != null) v.put("label", ref);
            }
            if (s.background != null) putDrawable(v, "background", skin, s.background, "bs-tooltip-bg");
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.TextTooltip$TextTooltipStyle", section);
    }

    private static void addTouchpadSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, Touchpad.TouchpadStyle> ss = skin.getAll(Touchpad.TouchpadStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, Touchpad.TouchpadStyle> e : ss.entries()) {
            Touchpad.TouchpadStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            if (s.background != null) putDrawable(v, "background", skin, s.background, "bs-slider-bg");
            if (s.knob != null) putDrawable(v, "knob", skin, s.knob, "bs-slider-knob");
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.Touchpad$TouchpadStyle", section);
    }

    private static void addTreeSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, Tree.TreeStyle> ss = skin.getAll(Tree.TreeStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, Tree.TreeStyle> e : ss.entries()) {
            Tree.TreeStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            // plus/minus 是树形展开收起图标，bs 主题没有专属资源 → 无 fallback，反查不到就跳过
            if (s.plus != null) putDrawable(v, "plus", skin, s.plus, null);
            if (s.minus != null) putDrawable(v, "minus", skin, s.minus, null);
            if (s.over != null) putDrawable(v, "over", skin, s.over, null);
            if (s.selection != null) putDrawable(v, "selection", skin, s.selection, "bs-list-selection");
            if (s.background != null) putDrawable(v, "background", skin, s.background, null);
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.Tree$TreeStyle", section);
    }

    /**
     * 反查嵌套 style 引用名（引用相等优先），用于 SelectBoxStyle.scrollStyle/listStyle、
     * TextTooltipStyle.label 等字段。找不到时返回 fallback（如 "default"）；fallback 为 null 则返回 null。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String resolveStyleRef(Skin skin, Object style, String fallback) {
        if (style == null) return null;
        ObjectMap<String, ?> m = skin.getAll((Class) style.getClass());
        if (m != null) {
            for (ObjectMap.Entry<String, ?> en : m) {
                if (en.value == style) return en.key;
            }
        }
        return fallback;
    }
}
