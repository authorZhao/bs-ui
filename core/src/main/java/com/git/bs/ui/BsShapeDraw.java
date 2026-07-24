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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * ShapeRenderer 自绘工具集合 —— 供进度类组件（BsSpinner / BsRingProgress 等）复用，
 * 统一"三角形带画粗圆弧"算法，避免每个组件自己写一份。
 *
 * <p>所有方法要求调用方已 {@code sr.begin(ShapeType.Filled)} 且已设置好投影/变换矩阵。
 * 方法内部只负责 setColor + triangle 拼弧。</p>
 *
 * @author authorZhao
 * @since 2026-07-24
 */
final class BsShapeDraw {

    private BsShapeDraw() {}

    /**
     * 画粗圆环弧（三角形带拼接，边缘平滑）。
     *
     * <p>从 {@code startDeg}（90 = 顶部 12 点，顺时针为正）开始，扫 {@code sweepDeg} 度，
     * 每 ~2° 一段，内外双半径拼成厚度 {@code thickness} 的弧带。</p>
     *
     * @param sr        调用方持有，需已 begin(Filled)
     * @param cx        圆心 x
     * @param cy        圆心 y
     * @param r         中心半径（弧的几何中线）
     * @param thickness 弧的粗细（厚度），实际内半径 = r - thickness/2，外半径 = r + thickness/2
     * @param startDeg  起始角度（度），90 = 顶部
     * @param sweepDeg  扫过角度（度），正值顺时针。<=0 直接 return
     * @param color     弧颜色（alpha 由调用方在 Color 里设好）
     */
    static void drawRingArc(ShapeRenderer sr, float cx, float cy, float r, float thickness,
                            float startDeg, float sweepDeg, Color color) {
        if (sweepDeg <= 0) return;
        sr.setColor(color);
        float inner = r - thickness / 2f;
        float outer = r + thickness / 2f;
        int segs = Math.max(2, (int) Math.ceil(sweepDeg / 2f));
        double a0 = Math.toRadians(startDeg);
        float ix0 = cx + (float) Math.cos(a0) * inner, iy0 = cy + (float) Math.sin(a0) * inner;
        float ox0 = cx + (float) Math.cos(a0) * outer, oy0 = cy + (float) Math.sin(a0) * outer;
        for (int i = 1; i <= segs; i++) {
            double a = Math.toRadians(startDeg - sweepDeg * i / segs);   // 递减 = 顺时针
            float ix = cx + (float) Math.cos(a) * inner;
            float iy = cy + (float) Math.sin(a) * inner;
            float ox = cx + (float) Math.cos(a) * outer;
            float oy = cy + (float) Math.sin(a) * outer;
            sr.triangle(ix0, iy0, ox0, oy0, ox, oy);
            sr.triangle(ix0, iy0, ox, oy, ix, iy);
            ix0 = ix; iy0 = iy; ox0 = ox; oy0 = oy;
        }
    }
}
