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

/**
 * RGB / HSL 颜色工具：lighten / darken / grayscale。所有输入输出都是 0~1 浮点。
 * @author authorZhao
 * @since 2026-07-16
 */
public final class BsColors {

    private BsColors() {}

    /** RGB→HSL。返回 float[3]={h(0~360), s(0~1), l(0~1)}。 */
    public static float[] rgbToHsl(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f;
        float h, s;
        if (max == min) {
            h = 0; s = 0;
        } else {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6f : 0f);
            } else if (max == g) {
                h = (b - r) / d + 2f;
            } else {
                h = (r - g) / d + 4f;
            }
            h *= 60f;
        }
        return new float[]{h, s, l};
    }

    /** HSL→RGB。返回 float[3]={r,g,b}，每个 0~1。 */
    public static float[] hslToRgb(float h, float s, float l) {
        if (s == 0) return new float[]{l, l, l};
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float hp = h / 60f;
        float x = c * (1f - Math.abs((hp % 2f) - 1f));
        float r, g, b;
        if (hp < 1)       { r = c; g = x; b = 0; }
        else if (hp < 2)  { r = x; g = c; b = 0; }
        else if (hp < 3)  { r = 0; g = c; b = x; }
        else if (hp < 4)  { r = 0; g = x; b = c; }
        else if (hp < 5)  { r = x; g = 0; b = c; }
        else              { r = c; g = 0; b = x; }
        float m = l - c / 2f;
        return new float[]{r + m, g + m, b + m};
    }

    /** 提亮：把 L 加上 amount（0~1）。 */
    public static float[] lighten(float r, float g, float b, float amount) {
        float[] hsl = rgbToHsl(r, g, b);
        hsl[2] = Math.min(1f, hsl[2] + amount);
        return hslToRgb(hsl[0], hsl[1], hsl[2]);
    }

    /** 加深：把 L 减去 amount（0~1）。 */
    public static float[] darken(float r, float g, float b, float amount) {
        float[] hsl = rgbToHsl(r, g, b);
        hsl[2] = Math.max(0f, hsl[2] - amount);
        return hslToRgb(hsl[0], hsl[1], hsl[2]);
    }

    /** 灰度化：S 设为 0。 */
    public static float[] grayscale(float r, float g, float b) {
        float[] hsl = rgbToHsl(r, g, b);
        hsl[1] = 0f;
        return hslToRgb(hsl[0], hsl[1], hsl[2]);
    }

    /** int(0xRRGGBB)→RGB 三元组（每个 0~1）。 */
    public static float[] hexToRgb(int hex) {
        return new float[]{
                ((hex >> 16) & 0xFF) / 255f,
                ((hex >> 8) & 0xFF) / 255f,
                (hex & 0xFF) / 255f
        };
    }

    /** RGB→int(0xRRGGBB)。 */
    public static int rgbToHex(float r, float g, float b) {
        int ri = Math.round(Math.max(0, Math.min(1, r)) * 255);
        int gi = Math.round(Math.max(0, Math.min(1, g)) * 255);
        int bi = Math.round(Math.max(0, Math.min(1, b)) * 255);
        return (ri << 16) | (gi << 8) | bi;
    }
}
