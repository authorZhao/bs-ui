package com.git.bs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author authorZhao
 * @since 2026-07-02
 */
@Slf4j
public class BsSkin extends Skin {

    private static final Map<String,BitmapFont> CACHE_FONT = new HashMap<>();


    public BsSkin(FileHandle skinFile) {
        super(skinFile);
    }

    public BsSkin(FileHandle skinFile, TextureAtlas atlas) {
        super(skinFile, atlas);
    }

    public BsSkin(TextureAtlas atlas) {
        super(atlas);
    }

    @Override
    protected Json getJsonLoader(FileHandle skinFile) {
        Json json = super.getJsonLoader(skinFile);
        var skin = this;
        json.setSerializer(BitmapFont.class, new Json.ReadOnlySerializer<BitmapFont>() {
            @Override
            public BitmapFont read (Json json, JsonValue jsonData, Class type) {
                String fontKey = jsonData.name;
                if(CACHE_FONT.containsKey(fontKey)){
                    return CACHE_FONT.get(fontKey);
                }

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
                        if (region != null)
                            font = new BitmapFont(fontFile, region, flip);
                        else {
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
        });


        json.setSerializer(FreeTypeFontGenerator.class, new Json.ReadOnlySerializer<FreeTypeFontGenerator>() {
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
                    return  null;
                }
            }
        });


        return json;
    }
}
