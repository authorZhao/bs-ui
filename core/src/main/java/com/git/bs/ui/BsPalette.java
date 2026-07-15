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
package com.git.bs.ui;

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
