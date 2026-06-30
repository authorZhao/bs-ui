package com.git.bs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.git.bs.common.SkinUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Skin 文件加载器：从 json + atlas + png + ttf + chars.txt 加载皮肤。
 *
 * <p>支持两种来源：</p>
 * <ul>
 *   <li>{@link #load(FileHandle)}：标准 libgdx skin json（不含 FreeType 字体引用扩展）</li>
 *   <li>{@link #loadWithFreeType(FileHandle)}：支持 BsSkinExporter 扩展的 FreeType 字体引用
 *       （{@code characters: chinese.txt} 自动读取外部文件）</li>
 * </ul>
 *
 * <h3>BsSkinExporter 约定</h3>
 * <ul>
 *   <li>字体配置：{@code { font: ttf/xxx.ttf, characters: chinese.txt, size: 18 }}</li>
 *   <li>{@code characters} 字段值若以 {@code .txt} 结尾，会读取文件内容作为字符集</li>
 *   <li>所有路径相对于 json 文件所在目录</li>
 * </ul>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * // 加载标准 skin（用户自己写的 VisUI 风格）
 * Skin skin = BsSkinLoader.load(Gdx.files.internal("skins/my-skin.json"));
 *
 * // 加载 BsSkinExporter 导出的皮肤（含 FreeType 字体）
 * Skin skin = BsSkinLoader.loadWithFreeType(Gdx.files.internal("skins/bs-light/bs-light.json"));
 *
 * // 直接在加载好的 skin 上叠加 Bs 主题资源
 * BsSkinFactory.augmentWithBsStyles(skin, BsLightTheme.INSTANCE);
 * }</pre>
 */
@Slf4j
public final class BsSkinLoader {

    private BsSkinLoader() {}

    /**
     * 标准加载（libgdx 原生 skin json 格式）。
     * <p>不支持 BsSkinExporter 扩展的 FreeType 引用，字体需要在 json 里用
     * {@code BitmapFont: {file: xxx.fnt}} 标准格式定义。</p>
     */
    public static Skin load(FileHandle jsonFile) {
        log.info("BsSkinLoader: 加载 {}", jsonFile.path());
        FileHandle atlasFile = jsonFile.sibling(jsonFile.nameWithoutExtension() + ".atlas");
        if (!atlasFile.exists()) {
            log.warn("BsSkinLoader: atlas 文件不存在 {}，仅加载 json", atlasFile.path());
            return new Skin(jsonFile);
        }
        TextureAtlas atlas = new TextureAtlas(atlasFile);
        return new Skin(jsonFile, atlas);
    }

    /**
     * 带 FreeType 字体扩展的加载（BsSkinExporter 约定）。
     *
     * <p>流程：</p>
     * <ol>
     *   <li>读 atlas 创建 Skin</li>
     *   <li>读 json 但**跳过** FreeTypeFontGenerator 桶（libgdx 标准 Skin 不识别这个）</li>
     *   <li>手动解析 FreeType 配置，按约定读取 TTF + chinese.txt 生成 BitmapFont</li>
     *   <li>把 BitmapFont 注册到 skin</li>
     *   <li>其他标准桶（Color/Style）正常加载</li>
     * </ol>
     */
    public static Skin loadWithFreeType(FileHandle jsonFile, Map<String,BitmapFont> fontCache) {
        log.info("BsSkinLoader: loadWithFreeType {}", jsonFile.path());
        FileHandle baseDir = jsonFile.parent();
        FileHandle atlasFile = baseDir.child(jsonFile.nameWithoutExtension() + ".atlas");
        return SkinUtil.load(jsonFile, atlasFile, fontCache);
    }

    public static Skin loadWithFreeType(FileHandle jsonFile) {
        return loadWithFreeType(jsonFile, new HashMap<>());
    }

    /** 把 "default-font" → "default"，"font-sm" → "font-sm"（保持不变）。 */
    private static String mapFontNameToKey(String fontName) {
        if ("default-font".equals(fontName)) return "default";
        return fontName;
    }

    /** 按 BsSkinExporter 约定生成 FreeType 字体。 */
    private static BitmapFont generateFreeTypeFont(FileHandle baseDir, JsonValue config) {
        String fontPath = config.getString("font");
        FileHandle ttfFile = baseDir.child(fontPath);
        if (!ttfFile.exists()) {
            throw new RuntimeException("TTF 文件不存在: " + ttfFile.path());
        }

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(ttfFile);
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = config.getInt("size", 18);
            // characters 字段：若以 .txt 结尾则读文件内容
            String chars = config.getString("characters", "");
            if (chars.endsWith(".txt")) {
                FileHandle charsFile = baseDir.child(chars);
                if (charsFile.exists()) {
                    p.characters = charsFile.readString(StandardCharsets.UTF_8.name());
                    log.info("BsSkinLoader: 字符集从 {} 读取 ({} 字符)", charsFile.path(),
                            p.characters.length());
                } else {
                    log.warn("BsSkinLoader: 字符集文件不存在 {}，用默认", charsFile.path());
                    p.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
                }
            } else if (!chars.isEmpty()) {
                // 直接内嵌的字符集
                p.characters = chars;
            }

            // filter
            String minFilter = config.getString("minFilter", "Linear");
            String magFilter = config.getString("magFilter", "Linear");
            p.minFilter = Texture.TextureFilter.valueOf(minFilter);
            p.magFilter = Texture.TextureFilter.valueOf(magFilter);

            // hinting
            String hinting = config.getString("hinting", "AutoMedium");
            try {
                p.hinting = FreeTypeFontGenerator.Hinting.valueOf(hinting);
            } catch (IllegalArgumentException e) {
                p.hinting = FreeTypeFontGenerator.Hinting.AutoMedium;
            }

            return gen.generateFont(p);
        } finally {
            gen.dispose();
        }
    }


    public static Skin loadAndAugmentWithCache(FileHandle jsonFile, BsTheme theme, Map<String, BitmapFont> fontCache) {
        Skin skin;
        // 检测是否含 FreeType（简单启发：json 含 "freetype" 字符串）
        String text = jsonFile.readString(StandardCharsets.UTF_8.name());
        if (text.contains("FreeTypeFontGenerator")) {
            skin = loadWithFreeType(jsonFile, fontCache);
        } else {
            skin = load(jsonFile);
        }
        BsSkinFactory.augmentWithBsStyles(skin, theme);
        return skin;
    }

    public static Skin loadAndAugmentWithCache(FileHandle jsonFile, BsTheme theme) {
        return loadAndAugmentWithCache(jsonFile, theme, new HashMap<>());
    }

    /** 把一个字体绑定到 skin 的 font-{suffix} + label-{suffix} 桶（用主题色作为字色）。 */
    public static void bindFontStyles(Skin skin, String suffix, BitmapFont font) {
        String fontKey = "font-" + suffix;
        String labelKey = "label-" + suffix;
        if (skin.has(fontKey, BitmapFont.class)) skin.remove(fontKey, BitmapFont.class);
        skin.add(fontKey, font, BitmapFont.class);
        if (skin.has(labelKey, Label.LabelStyle.class)) skin.remove(labelKey, Label.LabelStyle.class);
        skin.add(labelKey, new Label.LabelStyle(font, BsTheme.tp()), Label.LabelStyle.class);
    }

    public static Skin loadAndRegisterTheme(String skinCp, BsTheme bsTheme, Map<String, BitmapFont> fontCache) {
        var fileHandle = Gdx.files.internal(skinCp + "/" + bsTheme.name() + ".json");
        var skin = BsSkinLoader.loadAndAugmentWithCache(fileHandle, bsTheme, fontCache);
        BsUI.registerTheme(bsTheme.name(), bsTheme, skin);
        return skin;
    }


}
