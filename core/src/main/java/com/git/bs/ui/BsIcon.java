package com.git.bs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Bootstrap 图标加载：按名字从生成的 TextureAtlas 取 Drawable。
 *
 * <p><b>前置步骤</b>：先用 {@code BootstrapIconPackager} 把 SVG 转成 atlas：
 * <pre>
 * assets/bs/icons/bootstrap-icons.atlas
 * assets/bs/icons/bootstrap-icons.png
 * </pre>
 * </p>
 *
 * <p><b>用法</b>：</p>
 * <pre>{@code
 * // 1. 应用启动时加载 atlas（一次）
 * BsIcon.load("bs/icons/bootstrap-icons.atlas");
 *
 * // 2. 按名字取图标 Drawable
 * Drawable house = BsIcon.get("house");
 * Drawable gear = BsIcon.get("gear-fill");
 *
 * // 3. 配合按钮/菜单使用
 * BsButton btn = new BsButton(...);
 * btn.getTitleTable().add(new Image(BsIcon.get("gear"))).padRight(4);
 *
 * // 4. 染色（图标默认白底，用 Color 乘法染色）
 * Drawable colored = BsIcon.get("house").tint().tint(Color.BLUE);  // 简化示意
 *
 * // 5. 应用退出时释放
 * BsIcon.dispose();
 * }</pre>
 *
 * <p><b>按前缀过滤</b>：Bootstrap Icons 命名约定：
 * <ul>
 *   <li>{@code xxx-fill} —— 填充版</li>
 *   <li>{@code xxx} —— 线条版（默认）</li>
 * </ul>
 * </p>
 */
@Slf4j
public final class BsIcon {

    private BsIcon() {}

    /** 已加载的 atlas。 */
    private static TextureAtlas atlas;
    /** region 缓存（按图标名）。 */
    private static final Map<String, TextureRegion> cache = new HashMap<>();
    /** 默认 atlas 路径（assets 内相对路径）。 */
    public static final String DEFAULT_ATLAS_PATH = "com/git/bs/ui/icons/bootstrap-icons.atlas";

    /**
     * 加载默认 atlas（{@link #DEFAULT_ATLAS_PATH}）。
     * @return true 成功
     */
    public static boolean load() {
        return load(DEFAULT_ATLAS_PATH);
    }

    /**
     * 加载指定 atlas。
     * @param atlasPath assets 内相对路径（如 "com/git/bs/ui/icons/bootstrap-icons.atlas"）
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
                log.warn("BsIcon.load: atlas 文件不存在: {}", atlasPath);
                return false;
            }
            atlas = new TextureAtlas(Gdx.files.internal(atlasPath));
            // 预热缓存：所有 region 一次性放入 map
            for (TextureAtlas.AtlasRegion r : atlas.getRegions()) {
                cache.put(r.name, r);
            }
            log.info("BsIcon 加载成功: {}（{} 个图标）", atlasPath, cache.size());
            return true;
        } catch (Throwable t) {
            log.warn("BsIcon.load 失败: " + atlasPath, t);
            return false;
        }
    }

    /** 是否已加载 atlas。 */
    public static boolean isLoaded() {
        return atlas != null;
    }

    /**
     * 按名字取图标 Drawable。
     * @param name 图标名（如 "house"、"gear-fill"、"person-circle"）
     * @return Drawable（白底，可染色）；找不到返回 null
     */
    public static Drawable get(String name) {
        if (atlas == null) {
            // 自动尝试加载默认 atlas
            if (!load()) return null;
        }
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
    public static Drawable get(String name, com.badlogic.gdx.graphics.Color color) {
        Drawable d = get(name);
        if (d == null) return null;
        // TextureRegionDrawable 没有 setColor 方法，用 tint 创建新 drawable
        // 简化：用 SpriteDrawable 替代（SpriteDrawable 有 setColor）
        if (d instanceof TextureRegionDrawable) {
            com.badlogic.gdx.graphics.g2d.Sprite sprite = new com.badlogic.gdx.graphics.g2d.Sprite(
                    ((TextureRegionDrawable) d).getRegion());
            sprite.setColor(color);
            return new com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable(sprite);
        }
        return d;
    }

    /** 获取所有已加载的图标名。 */
    public static java.util.Set<String> getAllNames() {
        return new java.util.HashSet<>(cache.keySet());
    }

    /** 释放 atlas（应用退出或模块切换时调）。 */
    public static void dispose() {
        if (atlas != null) {
            atlas.dispose();
            atlas = null;
            cache.clear();
        }
    }
}
