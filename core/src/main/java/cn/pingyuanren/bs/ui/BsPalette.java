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

/**
 * Bootstrap 5 调色板：6 种 Variant 色。
 *
 * <p>所有 getter 无参，内部走 {@link BsUI#getSkin()} 取当前 skin。
 * 主题切换后下一次调用自动返回新主题色。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public final class BsPalette {

    public static final BsPalette PRIMARY   = new BsPalette("primary");
    public static final BsPalette SECONDARY = new BsPalette("secondary");
    public static final BsPalette SUCCESS   = new BsPalette("success");
    public static final BsPalette DANGER    = new BsPalette("danger");
    public static final BsPalette WARNING   = new BsPalette("warning");
    public static final BsPalette INFO      = new BsPalette("info");
    public static final BsPalette LIGHT     = new BsPalette("light");
    public static final BsPalette DARK      = new BsPalette("dark");

    /** Variant key（"primary"/"secondary"/...）。 */
    public final String key;

    private BsPalette(String key) { this.key = key; }

    public Color getMain()   { return BsTheme.colorOf(key); }
    public Color getHover()  { return BsTheme.hoverOf(key); }
    public Color getActive() { return BsTheme.activeOf(key); }
    public Color getSoftBg() { return BsTheme.softBgOf(key); }

    public int getMainHex() { return colorToHex(getMain()); }

    public Color getDisabled() { return BsTheme.td(); }

    public static BsPalette[] values() {
        return new BsPalette[]{PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO, LIGHT, DARK};
    }

    public String name() {
        if (key.isEmpty()) return "";
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

    @Override
    public String toString() { return name(); }

    private static int colorToHex(Color c) {
        return (Math.round(c.r * 255) << 16)
                | (Math.round(c.g * 255) << 8)
                | Math.round(c.b * 255);
    }
}
