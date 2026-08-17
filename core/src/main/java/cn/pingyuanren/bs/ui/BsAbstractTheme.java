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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * BsTheme 抽象基类（V2）。
 *
 * <p>子类只需提供一个 hex 值表（通过构造函数传入），
 * {@link #applyColorsToSkin(Skin)} 会自动把所有 hex 转成 Color 注册到 skin。</p>
 *
 * <p>这样：</p>
 * <ul>
 *   <li>BsLightTheme / BsDarkTheme 只需声明 hex 值表，无需逐个 getter</li>
 *   <li>新增主题（如 "brand-theme"）只要提供一组 hex 即可</li>
 *   <li>颜色数据集中在一处（hex 值表），易于维护</li>
 * </ul>
 * @author authorZhao
 * @since 2026-07-16
 */
public abstract class BsAbstractTheme implements BsTheme {

    private final String name;
    private final boolean dark;

    /** 所有 hex 值表（按 token 名索引）。 */
    private final java.util.Map<String, Integer> hexValues = new java.util.HashMap<>();

    protected BsAbstractTheme(String name, boolean dark) {
        this.name = name;
        this.dark = dark;
    }

    @Override
    public String name() { return name; }

    @Override
    public boolean isDark() { return dark; }

    /** 注册一个 hex 值（builder 风格，子类构造函数调用）。 */
    protected BsAbstractTheme put(String token, int hex) {
        hexValues.put(token, hex);
        return this;
    }

    @Override
    public void applyColorsToSkin(Skin skin) {
        for (java.util.Map.Entry<String, Integer> e : hexValues.entrySet()) {
            String key = "bs-" + e.getKey();
            int hex = e.getValue();
            Color c;
            // 8 位 hex（含 alpha）：0xAARRGGBB
            // 6 位 hex（无 alpha）：0xRRGGBB → alpha=1
            // 注意：alpha ≥ 0x80 时 int 字面量为负数（如 0xB3000000 = -1291845632），
            // 有符号比较 hex > 0xFFFFFF 会误判为 6 位（走 else 丢 alpha）。
            // 用无符号比较：任何 8 位 ARGB 的无符号值都 > 0xFFFFFF。
            if (Integer.compareUnsigned(hex, 0xFFFFFF) > 0) {
                c = new Color(
                        ((hex >> 16) & 0xFF) / 255f,
                        ((hex >> 8) & 0xFF) / 255f,
                        (hex & 0xFF) / 255f,
                        ((hex >> 24) & 0xFF) / 255f);
            } else {
                c = new Color(
                        ((hex >> 16) & 0xFF) / 255f,
                        ((hex >> 8) & 0xFF) / 255f,
                        (hex & 0xFF) / 255f, 1f);
            }
            // 覆盖式注册（applyColorsToSkin 总是构建新 skin 或重建 skin，应该覆盖）
            if (skin.has(key, Color.class)) skin.remove(key, Color.class);
            skin.add(key, c, Color.class);
        }
    }
}
