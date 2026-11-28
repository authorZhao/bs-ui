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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 图标 / emoji 图集：所有 atlas（Bootstrap 图标、彩色 emoji、头像、应用自己打的小图集）共用一份
 * region 缓存，按 region 名取 {@link Drawable}。
 *
 * <p>原 {@code BsIcon}（图标）和 {@code BsEmoji}（emoji）是两份几乎一样的代码，已合成这一个类：
 * 图标按名字查（{@code get("gear")}），emoji 按字符查（{@code emoji("😀")}，内部转成
 * {@code emoji_u1f600} 这种 region 名，规则见 {@link #toRegionName}）。两者查的是同一份缓存，
 * 所以可以放在同一张 atlas 里。</p>
 *
 * <p><b>用法</b>：</p>
 * <pre>{@code
 * // 1. 启动时加载（一次）。load 是换掉已加载的；loadEmoji / loadHeads / add 是往后追加
 * BsIcon.load();                         // Bootstrap 图标（默认 atlas）
 * BsIcon.loadEmoji();                    // 彩色 emoji（追加）
 * BsIcon.loadHeads();                    // 头像（追加），之后 randomHead() 可用
 * BsIcon.load("icons/my-icons.atlas");   // 自己打的 atlas：换掉已加载的，只留这一张
 *
 * // 2. 取 Drawable
 * Drawable gear = BsIcon.get("gear");            // 图标：白底，可染色
 * Drawable blue = BsIcon.get("gear", Color.BLUE);
 * Drawable grin = BsIcon.emoji("😀");            // emoji：彩色
 *
 * // 3. 应用退出时释放
 * BsIcon.dispose();
 * }</pre>
 *
 * <p><b>瘦身</b>：不想带整套图标 / emoji 时，用 TexturePacker 只把用到的那些打进一张 atlas
 * （emoji 的 region 名按 {@link #toRegionName} 的规则起），启动时 {@code BsIcon.load("自己的.atlas")}。
 * {@link #get(String)} / {@link #emoji(String)} 只在一张 atlas 都没加载时才会去自动加载默认图集。</p>
 *
 * <p><b>Bootstrap Icons 命名约定</b>：{@code xxx-fill} 是填充版，{@code xxx} 是线条版。</p>
 *
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public final class BsIcon {

    private BsIcon() {}

    /** 默认图标 atlas 路径（Bootstrap Icons，32×32 白底可染色）。 */
    public static final String DEFAULT_ATLAS_PATH = "cn/pingyuanren/bs/ui/icons/bootstrap-icons.atlas";
    /** 彩色 emoji atlas 路径（32×32）。 */
    public static final String EMOJI_ATLAS_PATH = "cn/pingyuanren/bs/ui/emoji/emoji.atlas";
    /** 头像 atlas 路径（128×128 人物 emoji）。 */
    public static final String HEAD_ATLAS_PATH = "cn/pingyuanren/bs/ui/emoji/head_emoji.atlas";

    /** 已加载的 atlas（load 换成一张；loadEmoji / loadHeads / add 往后追加）。 */
    private static final List<TextureAtlas> atlases = new ArrayList<>();
    /** 已加载过的 atlas 路径（同一路径不重复加载）。 */
    private static final Set<String> loadedPaths = new HashSet<>();
    /** region 缓存（按 region 名：图标名，或 emoji 的 region 名）。 */
    private static final Map<String, TextureRegion> cache = new HashMap<>();
    /** 头像 region 名列表（loadHeads 填，randomHead 用）。 */
    private static final List<String> headNames = new ArrayList<>();

    /** 加载默认图标 atlas（{@link #DEFAULT_ATLAS_PATH}），换掉已加载的。 */
    public static boolean load() {
        return load(DEFAULT_ATLAS_PATH);
    }

    /**
     * 加载指定 atlas：先释放已加载的，只留这一张。
     * @param atlasPath assets 内相对路径（如 "icons/my-icons.atlas"）
     * @return true 成功；false=文件不存在或加载失败（之前加载的已被释放）
     */
    public static boolean load(String atlasPath) {
        dispose();
        return add(atlasPath);
    }

    /** 追加彩色 emoji atlas（{@link #EMOJI_ATLAS_PATH}）。 */
    public static boolean loadEmoji() {
        return add(EMOJI_ATLAS_PATH);
    }

    /** 追加头像 atlas（{@link #HEAD_ATLAS_PATH}），之后 {@link #randomHead()} 可用。 */
    public static boolean loadHeads() {
        List<String> names = addAtlas(HEAD_ATLAS_PATH);
        if (names == null) return false;
        headNames.addAll(names);
        return true;
    }

    /**
     * 追加一个 atlas（不清已加载的；同一路径加载过就跳过）。
     * @param atlasPath assets 内相对路径
     * @return true 成功（或之前已加载过）；false=文件不存在或加载失败
     */
    public static boolean add(String atlasPath) {
        return addAtlas(atlasPath) != null;
    }

    /** 是否已加载过 atlas。 */
    public static boolean isLoaded() {
        return !atlases.isEmpty();
    }

    /**
     * 按名字取图标 Drawable。
     * @param name 图标名（如 "house"、"gear-fill"、"person-circle"）
     * @return Drawable（白底，可染色）；找不到返回 null
     */
    public static Drawable get(String name) {
        if (!isLoaded() && !load()) return null;
        TextureRegion region = cache.get(name);
        if (region == null) {
            log.debug("BsIcon.get: 未找到图标 {}", name);
            return null;
        }
        return new TextureRegionDrawable(region);
    }

    /**
     * 取图标并染色（返回新的 Drawable，不改原图）。
     * @param name 图标名
     * @param color 染色（libgdx 用 Color 乘法染色，白底 × color = 目标色）
     * @return 染色后的 Drawable；找不到返回 null
     */
    public static Drawable get(String name, Color color) {
        Drawable d = get(name);
        if (!(d instanceof TextureRegionDrawable)) return d;
        // TextureRegionDrawable 没有 setColor，用 SpriteDrawable 包一层
        Sprite sprite = new Sprite(((TextureRegionDrawable) d).getRegion());
        sprite.setColor(color);
        return new SpriteDrawable(sprite);
    }

    /**
     * 按 emoji 字符串取 Drawable。
     *
     * <p>传入的字符串可以是单个 emoji（如 "😀"）或多字符序列（如 "👨‍👩‍👧"），
     * 内部按 {@link #toRegionName} 转成 region 名去查。</p>
     *
     * @param emoji emoji 字符串（如 "😀"、"❤"、"🏠"）
     * @return Drawable（彩色）；找不到返回 null
     */
    public static Drawable emoji(String emoji) {
        if (!isLoaded() && !loadEmoji()) return null;
        String regionName = toRegionName(emoji);
        TextureRegion region = cache.get(regionName);
        if (region == null) {
            log.debug("BsIcon.emoji: 未找到 emoji {} (region={})", emoji, regionName);
            return null;
        }
        return new TextureRegionDrawable(region);
    }

    /** 按 Unicode 码点取 emoji Drawable（如 0x1F600）。 */
    public static Drawable emoji(int codePoint) {
        return emoji(new String(Character.toChars(codePoint)));
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
    public static String toRegionName(String emoji) {
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

    /**
     * 随机取一个头像 Drawable（128×128 人物 emoji）。
     * @return 头像 Drawable；没加载头像 atlas 返回 null
     */
    public static Drawable randomHead() {
        if (headNames.isEmpty()) return null;
        String name = headNames.get(new Random().nextInt(headNames.size()));
        TextureRegion region = cache.get(name);
        return region == null ? null : new TextureRegionDrawable(region);
    }

    /** 获取所有已加载的 region 名。 */
    public static Set<String> getAllNames() {
        return new HashSet<>(cache.keySet());
    }

    /** 释放全部 atlas（应用退出或模块切换时调）。 */
    public static void dispose() {
        for (TextureAtlas ta : atlases) {
            try { ta.dispose(); } catch (Throwable ignored) {}
        }
        atlases.clear();
        loadedPaths.clear();
        headNames.clear();
        cache.clear();
    }

    /** 读 atlas 并入缓存；返回新进来的 region 名，失败返回 null。 */
    private static List<String> addAtlas(String atlasPath) {
        if (loadedPaths.contains(atlasPath)) {
            return Collections.emptyList();
        }
        try {
            if (!Gdx.files.internal(atlasPath).exists()) {
                log.warn("BsIcon.load: atlas 文件不存在: {}", atlasPath);
                return null;
            }
            TextureAtlas ta = new TextureAtlas(Gdx.files.internal(atlasPath));
            atlases.add(ta);
            loadedPaths.add(atlasPath);
            List<String> names = new ArrayList<>();
            for (TextureAtlas.AtlasRegion r : ta.getRegions()) {
                cache.put(r.name, r);
                names.add(r.name);
            }
            log.info("BsIcon 加载成功: {}（{} 个 region，缓存共 {} 个）", atlasPath, names.size(), cache.size());
            return names;
        } catch (Throwable t) {
            log.warn("BsIcon.load 失败: " + atlasPath, t);
            return null;
        }
    }
}
