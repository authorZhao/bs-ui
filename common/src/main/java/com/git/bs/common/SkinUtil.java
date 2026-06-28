package com.git.bs.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author authorZhao
 * @since 2025-06-26
 */
@Slf4j
public class SkinUtil {

    private static final Map<String, BitmapFont> fontCache = new HashMap<>();

    public static Map<String, BitmapFont> getFontCache(Skin skin) {
        ObjectMap<String, BitmapFont> all = skin.getAll(BitmapFont.class);
        var map = new HashMap<String, BitmapFont>();
        for (var entry : all) {
            map.put(entry.key, entry.value);
        }
        return map;
    }


    public static synchronized Skin load(String jsonPath) {
        var jsonFile = Gdx.files.internal(jsonPath);
        FileHandle atlasFile = jsonFile.sibling(jsonFile.nameWithoutExtension() + ".atlas");
        return load(jsonFile, atlasFile, new HashMap<>());
    }

    public static synchronized Skin load(String jsonPath, Map<String, BitmapFont> fontCache) {
        var jsonFile = Gdx.files.internal(jsonPath);
        FileHandle atlasFile = jsonFile.sibling(jsonFile.nameWithoutExtension() + ".atlas");
        return load(jsonFile, atlasFile, fontCache);
    }


    public static synchronized Skin load(FileHandle jsonPath, FileHandle atlasPath, Map<String, BitmapFont> fontCache) {
        log.info("====================skin初始化开始==================");
        return new Skin(jsonPath, new TextureAtlas(atlasPath)) {
            @Override
            protected Json getJsonLoader(final FileHandle skinFile) {
                Json json = super.getJsonLoader(skinFile);
                final Skin skin = this;

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
                        BitmapFont cachedFont = fontCache.get(fontKey);
                        FreeTypeFontGenerator generator = null;

                        if (cachedFont == null) {
                            long startTime = System.currentTimeMillis();
                            generator = new FreeTypeFontGenerator(skinFile.parent().child(path));
                            BitmapFont font = generator.generateFont(parameter);
                            fontCache.put(fontKey, font);
                            cachedFont = font;
                            skin.add(jsonData.name, cachedFont);
                            long generationTime = System.currentTimeMillis() - startTime;
                            log.info("生成字体 {} 耗时: {}ms", jsonData.name, generationTime);
                        } else {
                            skin.add(jsonData.name, cachedFont);
                            log.info("使用缓存的字体: {}", jsonData.name);
                        }

                        if (parameter.incremental) {
                            if (generator != null) {
                                generator.dispose();
                            }
                            return null;
                        } else {
                            return  null;
                        }
                    }
                });
                log.info("====================skin初始化结束==================");
                return json;
            }
        };
    }


}
