/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库
 * Copyright (c) 2026 bs-ui contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package cn.pingyuanren.bs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认会把字体缓存，避免多次加载
 * 对于独立的fnt或者ttf字体默认会进行缓存，对于skin图片里面的不缓存
 *
 * @author authorZhao
 * @since 2026-07-02
 */
@Slf4j
public class BsSkin extends Skin {

    private static final Map<String, BitmapFont> CACHE_FONT = new HashMap<>();

    private boolean useCacheFont = false;
    private boolean canLoad = false;

    public BsSkin(FileHandle skinFile) {
        this(skinFile, true);
    }

    public BsSkin(FileHandle skinFile, boolean useCacheFont) {
        super(skinFile);
        this.useCacheFont = useCacheFont;
        canLoad = true;
        load(skinFile);
    }

    public BsSkin(FileHandle skinFile, TextureAtlas atlas, boolean useCacheFont) {
        super(skinFile, atlas);
        this.useCacheFont = useCacheFont;
        canLoad = true;
        load(skinFile);
    }

    public BsSkin(TextureAtlas atlas) {
        super(atlas);
        canLoad = true;
    }

    @Override
    public void load(FileHandle skinFile) {
        if (!canLoad) {
            return;
        }
        try {
            getJsonLoader(skinFile).fromJson(Skin.class, skinFile);
        } catch (SerializationException ex) {
            throw new SerializationException("Error reading file: " + skinFile, ex);
        }
    }

    @Override
    protected Json getJsonLoader(FileHandle skinFile) {
        Json json = super.getJsonLoader(skinFile);
        var skin = this;
        json.setSerializer(BitmapFont.class, new BitmapFontReadOnlySerializer(skinFile, skin));
        json.setSerializer(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorReadOnlySerializer(skinFile, skin));
        return json;
    }

    /**
     * dispose 行为由 {@link #useCacheFont} 决定：
     * <ul>
     *   <li>{@code false}（不用全局缓存）：字体属于本 skin，{@code super.dispose()} 正常释放。</li>
     *   <li>{@code true}（用全局缓存）：先把 {@link #CACHE_FONT} 里跨 skin 共享字体从本 skin 摘除引用，
     *       避免 {@code super.dispose()}（Skin.dispose 会 dispose 所有 Disposable）连带 dispose
     *       这些被其他 skin 仍在使用的共享字体。</li>
     * </ul>
     *
     * <p><b>disposeFontCache 不由 skin / dispose 自动调用</b>：skin 对象不随便销毁，全局字体缓存的
     * 释放时机由<b>开发者自行决定</b>（通常 app 退出，或确定所有 skin 都不再使用字体时）。</p>
     *
     * <p><b>注意</b>：若字体 Texture 来自本 skin 的 TextureAtlas（region 配置命中 atlas），
     * 摘除引用也救不了 —— super.dispose 仍会 dispose atlas 破坏字体 Texture。
     * 多 skin 共享的字体应使用独立 Texture（FreeType 生成或独立 .png），不要放 atlas。</p>
     */
    @Override
    public void dispose() {
        if (!useCacheFont) {
            super.dispose();
            return;
        }

        for (String key : CACHE_FONT.keySet()) {
            try {
                remove(key, BitmapFont.class);
            } catch (Throwable ignored) {
            }
        }
        super.dispose();
    }

    /**
     * 释放所有跨 skin 共享的缓存字体。<b>由开发者显式调用</b>（skin / dispose 不会自动调），
     * 时机自行决定（通常 app 退出，或确定所有 skin 都不再使用字体时）。
     */
    public static void disposeFontCache() {
        for (BitmapFont f : CACHE_FONT.values()) {
            try {
                f.dispose();
            } catch (Throwable ignored) {
            }
        }
        CACHE_FONT.clear();
    }

    private static class BitmapFontReadOnlySerializer extends Json.ReadOnlySerializer<BitmapFont> {
        private final FileHandle skinFile;
        private final BsSkin skin;

        public BitmapFontReadOnlySerializer(FileHandle skinFile, BsSkin skin) {
            this.skinFile = skinFile;
            this.skin = skin;
        }

        @Override
        public BitmapFont read(Json json, JsonValue jsonData, Class type) {
            String fontKey = jsonData.name;
//                if(CACHE_FONT.containsKey(fontKey) && !useCacheFont){
//                    return CACHE_FONT.get(fontKey);
//                }
            var cached = skin.useCacheFont;
            String path = json.readValue("file", String.class, jsonData);
            float scaledSize = json.readValue("scaledSize", float.class, -1f, jsonData);
            Boolean flip = json.readValue("flip", Boolean.class, false, jsonData);
            Boolean markupEnabled = json.readValue("markupEnabled", Boolean.class, false, jsonData);
            Boolean useIntegerPositions = json.readValue("useIntegerPositions", Boolean.class, true, jsonData);

            FileHandle fontFile = skinFile.parent().child(path);
            if (!fontFile.exists()) fontFile = Gdx.files.internal(path);
            if (!fontFile.exists()) throw new SerializationException("Font file not found: " + fontFile);

            // Use a region with the same name as the font, else use a PNG file in the same directory as the FNT file.
            String regionName = fontFile.nameWithoutExtension();
            try {
                BitmapFont font;
                Array<TextureRegion> regions = skin.getRegions(regionName);
                if (regions != null)
                    font = new BitmapFont(new BitmapFont.BitmapFontData(fontFile, flip), regions, true);
                else {
                    TextureRegion region = skin.optional(regionName, TextureRegion.class);
                    if (region != null) {
                        font = new BitmapFont(fontFile, region, flip);
                    } else {
                        if (cached && CACHE_FONT.containsKey(fontKey)) {
                            return CACHE_FONT.get(fontKey);
                        }

                        FileHandle imageFile = fontFile.parent().child(regionName + ".png");
                        if (imageFile.exists())
                            font = new BitmapFont(fontFile, imageFile, flip);
                        else
                            font = new BitmapFont(fontFile, flip);
                    }
                }
                font.getData().markupEnabled = markupEnabled;
                font.setUseIntegerPositions(useIntegerPositions);
                // Scaled size is the desired cap height to scale the font to.
                if (scaledSize != -1) font.getData().setScale(scaledSize / font.getCapHeight());
                CACHE_FONT.put(fontKey, font);
                return font;
            } catch (RuntimeException ex) {
                throw new SerializationException("Error loading bitmap font: " + fontFile, ex);
            }
        }
    }

    private static class FreeTypeFontGeneratorReadOnlySerializer extends Json.ReadOnlySerializer<FreeTypeFontGenerator> {
        private final FileHandle skinFile;
        private final BsSkin skin;

        public FreeTypeFontGeneratorReadOnlySerializer(FileHandle skinFile, BsSkin skin) {
            this.skinFile = skinFile;
            this.skin = skin;
        }

        @Override
        public FreeTypeFontGenerator read(Json json, JsonValue jsonData, Class type) {
            String path = json.readValue("font", String.class, jsonData);
            jsonData.remove("font");

            FreeTypeFontGenerator.Hinting hinting = FreeTypeFontGenerator.Hinting.valueOf(json.readValue("hinting",
                    String.class, "AutoMedium", jsonData));
            jsonData.remove("hinting");

            Texture.TextureFilter minFilter = Texture.TextureFilter.valueOf(
                    json.readValue("minFilter", String.class, "Nearest", jsonData));
            jsonData.remove("minFilter");

            Texture.TextureFilter magFilter = Texture.TextureFilter.valueOf(
                    json.readValue("magFilter", String.class, "Nearest", jsonData));
            jsonData.remove("magFilter");


            var characters = json.readValue("characters", String.class, jsonData);
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = json.readValue(FreeTypeFontGenerator.FreeTypeFontParameter.class, jsonData);
            parameter.hinting = hinting;
            parameter.minFilter = minFilter;
            parameter.magFilter = magFilter;

            if (characters != null && characters.endsWith(".txt")) {
                // 相对 skin 目录解析（与 font ttf 路径一致），支持子目录下的字符集文件
                FileHandle charsFile = skinFile.parent().child(parameter.characters);
                parameter.characters = charsFile.readString(StandardCharsets.UTF_8.name());
            }

            // 使用字体缓存避免重复生成
            String fontKey = jsonData.name;
            BitmapFont cachedFont = CACHE_FONT.get(fontKey);
            FreeTypeFontGenerator generator = null;

            if (cachedFont == null) {
                long startTime = System.currentTimeMillis();
                generator = new FreeTypeFontGenerator(skinFile.parent().child(path));
                BitmapFont font = generator.generateFont(parameter);
                CACHE_FONT.put(fontKey, font);
                cachedFont = font;
                skin.add(jsonData.name, cachedFont);
                long generationTime = System.currentTimeMillis() - startTime;
                log.info("生成字体 {} 耗时: {}ms", jsonData.name, generationTime);
            } else {
                skin.add(jsonData.name, cachedFont);
                log.info("使用缓存的字体: {}", jsonData.name);
            }

            if (!parameter.incremental) {
                if (generator != null) {
                    generator.dispose();
                }
                return null;
            } else {
                return null;
            }
        }
    }


    public static Map<String, BitmapFont> getFontCache(Skin skin) {
        var all = skin.getAll(BitmapFont.class);
        var map = new HashMap<String, BitmapFont>();
        for (var entry : all) {
            map.put(entry.key, entry.value);
        }
        return map;
    }

}
