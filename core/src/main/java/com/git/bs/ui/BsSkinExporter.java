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
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane.SplitPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.ObjectMap;
import lombok.extern.slf4j.Slf4j;

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
 * └── ttf/
 *     └── *.ttf          FreeType 字体文件
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

        // ===== 1. PixmapPacker 收集所有 Drawable 的占位 Pixmap =====
        PixmapPacker packer = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 2, true);
        Map<String, String> drawableToRegion = new LinkedHashMap<>();

        ObjectMap<String, Drawable> drawables = skin.getAll(Drawable.class);
        if (drawables != null) {
            for (ObjectMap.Entry<String, Drawable> e : drawables) {
                String key = e.key;
                Pixmap pix = makePlaceholderPixmap(key, skin);
                packer.pack(key, pix);
                drawableToRegion.put(key, key);
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
                writeAtlasFile(atlasFile, name + ".png", packer);
            }
            log.info("BsSkinExporter: atlas 写入 {} ({} 个 region)", atlasFile.path(),
                    drawableToRegion.size());
        } catch (Throwable t) {
            log.warn("写 atlas 失败", t);
        }

        // ===== 3. 复制 TTF + chars.txt =====
        if (ttfSource != null && ttfSource.exists()) {
            FileHandle ttfDir = outputDir.child("ttf");
            if (!ttfDir.exists()) ttfDir.mkdirs();
            FileHandle ttfDest = ttfDir.child(ttfSource.name());
            if (!ttfDest.exists()) ttfSource.copyTo(ttfDest);
            log.info("BsSkinExporter: TTF 复制到 {}", ttfDest.path());
        }
        if (charsFile != null && charsFile.exists()) {
            FileHandle charsDest = outputDir.child("chinese.txt");
            if (!charsDest.exists()) charsFile.copyTo(charsDest);
            log.info("BsSkinExporter: 字符集复制到 {}", charsDest.path());
        }

        // ===== 4. 写 json =====
        FileHandle jsonFile = outputDir.child(name + ".json");
        writeJsonFile(jsonFile, skin,
                ttfSource != null ? "ttf/" + ttfSource.name() : null,
                charsFile != null ? "chinese.txt" : null);
        log.info("BsSkinExporter: json 写入 {}", jsonFile.path());

        packer.dispose();
        log.info("BsSkinExporter: 导出完成");
    }

    // =================== 占位 Pixmap 生成 ===================

    /**
     * 按 drawable key 命名约定生成占位 Pixmap（不碰真实 Texture 数据）。
     * 加载时由 BsSkinLoader → augmentWithBsStyles 重新生成真实像素。
     */
    private static Pixmap makePlaceholderPixmap(String key, Skin skin) {
        // 确定尺寸
        int size;
        if (key.equals("white")) {
            size = 4;
        } else if (key.contains("cursor") || key.contains("v-line")) {
            Pixmap pix = new Pixmap(2, 16, Pixmap.Format.RGBA8888);
            pix.setColor(resolveColorByKey(key, skin));
            pix.fill();
            return pix;
        } else if (key.contains("check") || key.contains("radio") || key.contains("arrow")) {
            size = 24;
        } else {
            size = 32;
        }
        // 生成纯色 Pixmap
        Color c = resolveColorByKey(key, skin);
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setColor(c);
        pix.fill();
        return pix;
    }

    /** 根据 drawable key 推断对应的 Color 桶值。 */
    private static Color resolveColorByKey(String key, Skin skin) {
        try {
            if (key.startsWith("bs-")) {
                // bs-{color}-up / -hover / -active → bs-{color}
                String[] parts = key.split("-");
                if (parts.length >= 2) {
                    String colorName = parts[1];
                    if (skin.has("bs-" + colorName, Color.class)) {
                        return skin.get("bs-" + colorName, Color.class);
                    }
                }
                if (key.contains("outline-up") || key.contains("soft-bg")
                        || key.contains("text-field-bg") || key.contains("list-bg")) {
                    return skin.get("bs-bg-elevated", Color.class);
                }
                if (key.contains("window-bg")) return skin.get("bs-bg-surface", Color.class);
                if (key.contains("menu-bar-bg")) return skin.get("bs-bg-header", Color.class);
                if (key.contains("scrollpane") || key.contains("slider-bg")
                        || key.contains("progress-track")) {
                    return skin.get("bs-border-strong", Color.class);
                }
                if (key.contains("slider-knob") || key.contains("arrow-left")
                        || key.contains("arrow-right") || key.contains("menu-item-active")
                        || key.contains("check-on") || key.contains("radio-on")) {
                    return skin.get("bs-primary", Color.class);
                }
                if (key.contains("tooltip")) return new Color(0, 0, 0, 1f);
                if (key.contains("menu-item-hover")) return skin.get("bs-bg-hover", Color.class);
                if (key.contains("check-off") || key.contains("radio-off")) {
                    return skin.get("bs-border", Color.class);
                }
            }
        } catch (Throwable ignored) {}
        return new Color(0.3f, 0.3f, 0.3f, 1f);
    }

    // =================== atlas 文本写入 ===================

    /** 手写 .atlas 文本格式（参考 TextureAtlas.TextureAtlasData 输出格式）。 */
    private static void writeAtlasFile(FileHandle out, String pngName, PixmapPacker packer) {
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

        // ===== 补齐 style 中引用的 Color（如果 Color 桶里不存在，按命名推断追加） =====
        // 例如 style 里 "overFontColor": "white"，但 Color 桶没有 "white"
        // 就追加一个 white: {r:1,g:1,b:1,a:1}，避免加载时抛
        // "No Color registered with name: white"
        ensureReferencedColors(root, skin);

        // FIXME: fastjson2 依赖未引入 core 模块，json 序列化暂时禁用
        //       引入 com.alibaba.fastjson2:fastjson2 后恢复下面两行：
        // String jsonStr = JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
        // out.writeString(jsonStr, false);
        throw new UnsupportedOperationException(
                "BsSkinExporter.writeJsonFile 已禁用：core 模块未引入 fastjson2 依赖");
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
                "fontColor", "disabledFontColor", "overFontColor", "downFontColor",
                "checkedFontColor", "checkedOverFontColor",
                "fontColorSelected", "fontColorUnselected",
                "titleFontColor"));

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

    /** 从 skin Color 桶反查 Color 对象的 key 名。 */
    private static String resolveColorName(Skin skin, Color c) {
        ObjectMap<String, Color> colors = skin.getAll(Color.class);
        if (colors != null) {
            for (ObjectMap.Entry<String, Color> e : colors) {
                if (e.value == c) return e.key;
            }
        }
        return "white";
    }

    // =================== 各 style section ===================

    private static void addTextButtonSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, TextButtonStyle> ss = skin.getAll(TextButtonStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, TextButtonStyle> e : ss.entries()) {
            TextButtonStyle s = e.value;
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("font", "default-font");
            if (s.fontColor != null) v.put("fontColor", resolveColorName(skin, s.fontColor));
            if (s.disabledFontColor != null) v.put("disabledFontColor", resolveColorName(skin, s.disabledFontColor));
            if (s.overFontColor != null) v.put("overFontColor", resolveColorName(skin, s.overFontColor));
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
            v.put("font", "default-font");
            if (s.fontColor != null) v.put("fontColor", resolveColorName(skin, s.fontColor));
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
            v.put("font", "default-font");
            if (s.fontColor != null) v.put("fontColor", resolveColorName(skin, s.fontColor));
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
            if (s.background != null) v.put("background", "bs-slider-bg");
            if (s.knob != null) v.put("knob", "bs-slider-knob");
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.Slider$SliderStyle", section);
    }

    private static void addSplitPaneSection(Map<String, Object> root, Skin skin) {
        ObjectMap<String, SplitPaneStyle> ss = skin.getAll(SplitPaneStyle.class);
        if (ss == null || ss.size == 0) return;
        Map<String, Object> section = new LinkedHashMap<>();
        for (ObjectMap.Entry<String, SplitPaneStyle> e : ss.entries()) {
            Map<String, Object> v = new LinkedHashMap<>();
            if (e.value.handle != null) v.put("handle", "white");
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
            if (s.background != null) v.put("background", "bs-window-bg");
            if (s.hScroll != null) v.put("hScroll", "bs-scrollpane-h-bar");
            if (s.vScroll != null) v.put("vScroll", "bs-scrollpane-v-bar");
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
            v.put("font", "default-font");
            if (s.fontColorSelected != null) v.put("fontColorSelected", "white");
            if (s.fontColorUnselected != null) v.put("fontColorUnselected", "bs-text-primary");
            if (s.selection != null) v.put("selection", "bs-list-selection");
            if (s.background != null) v.put("background", "bs-list-bg");
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
            v.put("font", "default-font");
            if (s.fontColor != null) v.put("fontColor", "bs-text-primary");
            if (s.cursor != null) v.put("cursor", "bs-text-field-cursor");
            if (s.selection != null) v.put("selection", "bs-text-field-selection");
            if (s.background != null) v.put("background", "bs-text-field-bg");
            if (s.focusedBackground != null) v.put("focusedBackground", "bs-text-field-focus");
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
            v.put("font", "default-font");
            if (s.fontColor != null) v.put("fontColor", "bs-text-primary");
            if (s.background != null) v.put("background", "bs-text-field-bg");
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
            v.put("titleFont", "default-font");
            if (s.titleFontColor != null) v.put("titleFontColor", "bs-text-primary");
            if (s.background != null) v.put("background", "bs-window-bg");
            if (s.stageBackground != null) v.put("stageBackground", "white");
            section.put(e.key, v);
        }
        root.put("com.badlogic.gdx.scenes.scene2d.ui.Window$WindowStyle", section);
    }
}
