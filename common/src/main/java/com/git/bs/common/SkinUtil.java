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
 * 建议多用BsSkin
 *
 * @author authorZhao
 * @since 2025-06-26
 */
@Slf4j
public class SkinUtil {


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
        return load(jsonFile, atlasFile);

    }


    public static synchronized Skin load(FileHandle jsonPath, FileHandle atlasPath) {
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

                        long startTime = System.currentTimeMillis();
                        var generator = new FreeTypeFontGenerator(skinFile.parent().child(path));
                        BitmapFont font = generator.generateFont(parameter);

                        skin.add(jsonData.name, font);
                        long generationTime = System.currentTimeMillis() - startTime;
                        log.info("生成字体 {} 耗时: {}ms", jsonData.name, generationTime);


                        if (parameter.incremental) {
                            if (generator != null) {
                                generator.dispose();
                            }
                            return null;
                        } else {
                            return null;
                        }
                    }
                });
                log.info("====================skin初始化结束==================");
                return json;
            }
        };
    }


}
