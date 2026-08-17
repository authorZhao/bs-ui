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
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Emoji 图标加载：从预烘焙的 TextureAtlas 按 emoji 字符取 Drawable。
 *
 * <p><b>前置步骤</b>：先用 {@code BootstrapIconPackager} 把 Noto Emoji SVG 转成 atlas：
 * <pre>
 * core/src/main/resources/cn/pingyuanren/bs/ui/emoji/emoji.atlas
 * core/src/main/resources/cn/pingyuanren/bs/ui/emoji/emoji.png
 * core/src/main/resources/cn/pingyuanren/bs/ui/emoji/pack2.png ~ pack5.png
 * </pre>
 * Region 命名格式：{@code emoji_u{hex}}（单码点，如 {@code emoji_u1f600}），
 * 多码点组合用下划线连接（如 {@code emoji_u1f468_200d_1f469}）。
 * </p>
 *
 * <p><b>用法</b>：</p>
 * <pre>{@code
 * // 1. 应用启动时加载 atlas（一次）
 * BsEmoji.load();
 *
 * // 2. 按 emoji 字符取 Drawable
 * Drawable grin = BsEmoji.get("😀");      // U+1F600
 * Drawable heart = BsEmoji.get("❤");     // U+2764
 *
 * // 3. 配合 Image / Button 使用
 * Image img = new Image(BsEmoji.get("😀"));
 * BsButton btn = new BsButton(...);
 * btn.getTitleTable().add(new Image(BsEmoji.get("🏠"))).padRight(4);
 *
 * // 4. 应用退出时释放
 * BsEmoji.dispose();
 * }</pre>
 *
 * <p><b>原理</b>：emoji 字符的 Unicode 码点转小写 hex，拼成
 * {@code emoji_u{hex}} 去 atlas 里查 region。多字符序列（ZWJ 组合 emoji）
 * 会自动拼接成 {@code emoji_u{hex1}_{hex2}_...} 格式。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public final class BsEmoji {

    private BsEmoji() {}

    /** 已加载的 atlas。 */
    private static TextureAtlas atlas;
    /** 额外加载的 atlas 列表（头像等，dispose 时统一释放）。 */
    private static final java.util.List<TextureAtlas> extraAtlases = new java.util.ArrayList<>();
    /** region 缓存（按 region 名）。 */
    private static final Map<String, TextureRegion> cache = new HashMap<>();
    /** 头像 region 名列表（从 head_emoji.atlas 加载）。 */
    private static final java.util.List<String> headNames = new java.util.ArrayList<>();
    /** 默认 atlas 路径。 */
    public static final String DEFAULT_ATLAS_PATH = "cn/pingyuanren/bs/ui/emoji/emoji.atlas";
    /** 头像 atlas 路径。 */
    public static final String HEAD_ATLAS_PATH = "cn/pingyuanren/bs/ui/emoji/head_emoji.atlas";

    /**
     * 加载默认 atlas（{@link #DEFAULT_ATLAS_PATH}）。
     * @return true 成功
     */
    public static boolean load() {
        return load(DEFAULT_ATLAS_PATH);
    }

    /**
     * 加载指定 atlas。
     * @param atlasPath assets 内相对路径
     * @return true 成功；false=文件不存在或加载失败
     */
    public static boolean load(String atlasPath) {
        try {
            if (atlas != null) {
                atlas.dispose();
                atlas = null;
                cache.clear();
            }
            if (!Gdx.files.internal(atlasPath).exists()) {
                log.warn("BsEmoji.load: atlas 文件不存在: {}", atlasPath);
                return false;
            }
            atlas = new TextureAtlas(Gdx.files.internal(atlasPath));
            for (TextureAtlas.AtlasRegion r : atlas.getRegions()) {
                cache.put(r.name, r);
            }
            log.info("BsEmoji 加载成功: {}（{} 个 emoji）", atlasPath, cache.size());
            return true;
        } catch (Throwable t) {
            log.warn("BsEmoji.load 失败: " + atlasPath, t);
            return false;
        }
    }

    /** 是否已加载 atlas。 */
    public static boolean isLoaded() {
        return atlas != null;
    }

    /**
     * 加载头像 atlas（{@link #HEAD_ATLAS_PATH}），把 region 名存入 {@link #headNames}。
     * 头像 region 名形如 {@code emoji_u1f9d1}（人物 emoji），128×128。
     */
    public static boolean loadHeads() {
        try {
            if (!Gdx.files.internal(HEAD_ATLAS_PATH).exists()) {
                log.warn("BsEmoji.loadHeads: atlas 文件不存在: {}", HEAD_ATLAS_PATH);
                return false;
            }
            TextureAtlas headAtlas = new TextureAtlas(Gdx.files.internal(HEAD_ATLAS_PATH));
            extraAtlases.add(headAtlas);
            for (TextureAtlas.AtlasRegion r : headAtlas.getRegions()) {
                cache.put(r.name, r);
                headNames.add(r.name);
            }
            log.info("BsEmoji 头像加载成功: {}（{} 个头像）", HEAD_ATLAS_PATH, headNames.size());
            return true;
        } catch (Throwable t) {
            log.warn("BsEmoji.loadHeads 失败: " + HEAD_ATLAS_PATH, t);
            return false;
        }
    }

    /**
     * 随机取一个头像 Drawable（128×128 人物 emoji）。
     * @return 头像 Drawable；无头像 atlas 返回 null
     */
    public static Drawable randomHead() {
        if (headNames.isEmpty()) return null;
        String name = headNames.get(new Random().nextInt(headNames.size()));
        TextureRegion region = cache.get(name);
        return region == null ? null : new TextureRegionDrawable(region);
    }

    /**
     * 按 emoji 字符串取 Drawable。
     *
     * <p>传入的字符串可以是单个 emoji（如 "😀"）或多字符序列（如 "👨‍👩‍👧"）。
     * 内部按码点拆分，拼成 {@code emoji_u{hex}} 格式去 atlas 查 region。</p>
     *
     * @param emoji emoji 字符串（如 "😀"、"❤"、"🏠"）
     * @return Drawable；找不到返回 null
     */
    public static Drawable get(String emoji) {
        if (atlas == null) {
            if (!load()) return null;
        }
        String regionName = toRegionName(emoji);
        TextureRegion region = cache.get(regionName);
        if (region == null) {
            log.debug("BsEmoji.get: 未找到 emoji {} (region={})", emoji, regionName);
            return null;
        }
        return new TextureRegionDrawable(region);
    }

    /**
     * 按 Unicode 码点取 Drawable。
     *
     * @param codePoint Unicode 码点（如 0x1F600）
     * @return Drawable；找不到返回 null
     */
    public static Drawable get(int codePoint) {
        return get(new String(Character.toChars(codePoint)));
    }

    /**
     * 把 emoji 字符串转成 atlas region 名。
     *
     * <p>规则：按码点拆分，每个码点转小写 hex（不足 4 位不补零，≥4 位不截断），
     * 用下划线连接，加 {@code emoji_u} 前缀。</p>
     *
     * <ul>
     *   <li>"😀" (U+1F600) → "emoji_u1f600"</li>
     *   <li>"❤" (U+2764) → "emoji_u2764"</li>
     *   <li>"#" (U+0023) → "emoji_u0023"</li>
     *   <li>"👨‍👩‍👧" (U+1F468 U+200D U+1F469 U+200D U+1F467) → "emoji_u1f468_200d_1f469_200d_1f467"</li>
     * </ul>
     */
    static String toRegionName(String emoji) {
        if (emoji == null || emoji.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("emoji_u");
        int i = 0;
        boolean first = true;
        while (i < emoji.length()) {
            int cp = emoji.codePointAt(i);
            i += Character.charCount(cp);
            if (!first) sb.append('_');
            sb.append(Integer.toHexString(cp));
            first = false;
        }
        return sb.toString();
    }

    /** 获取所有已加载的 region 名。 */
    public static java.util.Set<String> getAllNames() {
        return new java.util.HashSet<>(cache.keySet());
    }

    /** 释放 atlas。 */
    public static void dispose() {
        if (atlas != null) {
            atlas.dispose();
            atlas = null;
        }
        for (TextureAtlas ta : extraAtlases) {
            try { ta.dispose(); } catch (Throwable ignored) {}
        }
        extraAtlases.clear();
        headNames.clear();
        cache.clear();
    }
}
