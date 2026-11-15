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

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 分层皮肤注册 builder（经 {@link BsUI#builder(String)} 进入）。
 *
 * <p><b>四层加载顺序（低 → 高，高层覆盖低层）</b>：</p>
 * <ol>
 *   <li><b>基座</b>：{@link #baseJson(FileHandle)} 的烘焙/自定义 json（可选；不传 = 空 skin，
 *       可配合 {@link #defaultFont(BitmapFont)} 手工供字体）；</li>
 *   <li><b>内置 augment</b>：{@link BsSkinFactory#augmentWithBsStyles(Skin, BsTheme)} 按主题
 *       全套生成 bs-* drawable/style（{@code putIfAbsent} 只补缺，基座已有的不动）；</li>
 *   <li><b>jsonLayer</b>：使用方 json 依次加载，<b>同 key 整对象覆盖</b>。因为加载在内置
 *       augment <b>之后</b>，桶里已有全部 bs drawable，覆盖 json 只需按名引用（改某个
 *       {@code bs-btn-primary} 不必抄全量 json）；</li>
 *   <li><b>augmentLayer</b>：使用方代码层依次执行（{@code skin.add(...)} 新增或覆盖），
 *       最高优先。</li>
 * </ol>
 *
 * <h3>典型用法</h3>
 * <pre>{@code
 * BsUI.builder("my-game")
 *     .base(myTheme)                                        // 自定义主题（换组 hex 即整套换肤）
 *     .baseJson(Gdx.files.internal("skin/bs-light.json"))   // 或游戏自己的基座 json
 *     .jsonLayer(Gdx.files.internal("skin/my-overrides.json"))
 *     .augmentLayer((skin, theme) -> skin.add("cwwg_bg_label", style))
 *     .register();
 * }</pre>
 *
 * <p>覆盖语义为 <b>style 整对象替换</b>（与 libGDX Skin json 同口径），不做字段级合并。</p>
 *
 * @author authorZhao
 * @since 0.3.7
 */
@Slf4j
public final class BsSkinBuilder {

    private final String name;
    private BsTheme theme = BsLightTheme.INSTANCE;
    private FileHandle baseJson;
    private BitmapFont defaultFont;
    private Map<String, BitmapFont> sizeFonts;
    private final List<FileHandle> jsonLayers = new ArrayList<>();
    private final List<BiConsumer<Skin, BsTheme>> augmentLayers = new ArrayList<>();

    BsSkinBuilder(String name) {
        this.name = Objects.requireNonNull(name, "themeName");
    }

    /** 主题（默认 Light）。自定义主题继承 {@link BsAbstractTheme} 提供一组 hex 即可。 */
    public BsSkinBuilder base(BsTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        return this;
    }

    /** 基座 json（可选）。不传则空 skin 起步（字体用 {@link #defaultFont(BitmapFont)} 供）。 */
    public BsSkinBuilder baseJson(FileHandle json) {
        this.baseJson = json;
        return this;
    }

    /** 无基座 json 时的 default 字体（注册为 {@code default} + {@code font} 两 key）。 */
    public BsSkinBuilder defaultFont(BitmapFont font) {
        this.defaultFont = font;
        return this;
    }

    /** 尺寸字体（key 为 sm/md/lg/xl，与 {@link BsUI#buildSkin} 同口径，在 augment 前入桶）。 */
    public BsSkinBuilder sizeFonts(Map<String, BitmapFont> sizeFonts) {
        this.sizeFonts = sizeFonts;
        return this;
    }

    /** json 覆盖层（可叠多个，后载优先=覆盖先载）。在内置 augment 之后加载，可按名引用 bs-* drawable。 */
    public BsSkinBuilder jsonLayer(FileHandle json) {
        this.jsonLayers.add(Objects.requireNonNull(json, "json"));
        return this;
    }

    /** 代码覆盖层（可叠多个，按添加顺序执行，最高优先）。 */
    public BsSkinBuilder augmentLayer(BiConsumer<Skin, BsTheme> layer) {
        this.augmentLayers.add(Objects.requireNonNull(layer, "layer"));
        return this;
    }

    /** 按四层顺序构建 skin（不注册）。 */
    public Skin build() {
        Skin skin = baseJson != null ? new BsSkin(baseJson) : new BsSkin();
        if (defaultFont != null) {
            skin.add("default", defaultFont, BitmapFont.class);
            skin.add("font", defaultFont, BitmapFont.class);
            if (sizeFonts != null) {
                for (Map.Entry<String, BitmapFont> e : sizeFonts.entrySet()) {
                    if (e.getValue() != null) {
                        skin.add("font-" + e.getKey(), e.getValue(), BitmapFont.class);
                    }
                }
            }
        }
        // ② 内置 augment：只补缺（基座/字体优先）
        BsSkinFactory.augmentWithBsStyles(skin, theme);
        // ③ jsonLayer：整对象覆盖（augment 已就位全部 bs drawable，覆盖 json 按名引用即可）
        for (FileHandle layer : jsonLayers) {
            loadJsonLayer(skin, layer);
        }
        // ④ augmentLayer：代码层，最高优先
        for (BiConsumer<Skin, BsTheme> layer : augmentLayers) {
            layer.accept(skin, theme);
        }
        return skin;
    }

    /** 构建并注册进 {@link BsUI}（首个注册的 theme 自动成为当前主题）。 */
    public Skin register() {
        Skin skin = build();
        BsUI.registerTheme(name, theme, skin);
        return skin;
    }

    /** 载入一个 json 覆盖层：同名 sibling atlas 存在则自动挂 regions（与 BsSkinLoader.load 同口径）。 */
    private static void loadJsonLayer(Skin skin, FileHandle json) {
        FileHandle atlasFile = json.sibling(json.nameWithoutExtension() + ".atlas");
        if (atlasFile.exists()) {
            skin.addRegions(new TextureAtlas(atlasFile));
        }
        log.info("BsSkinBuilder: 加载 json 覆盖层 {}（atlas={}）", json.path(), atlasFile.exists());
        skin.load(json);
    }
}
