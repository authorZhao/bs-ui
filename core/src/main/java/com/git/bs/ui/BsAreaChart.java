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
 * Bootstrap 风格面积图（AreaChart）—— 折线图变体，折线下方填充半透明色块。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsAreaChart chart = new BsAreaChart();
 * chart.setSize(480, 240);
 * chart.setSkinFont(skin);
 * chart.setMultiSeries(Arrays.asList(
 *     new BsChart.Series("访问量", BsChart.pointsOfY(20, 35, 40, 55, 70, 85, 90))
 * ));
 * stage.addActor(chart);
 * }</pre>
 *
 * <p>实现：先画填充（多边形 = 折线顶点 + 右下角 + 左下角），
 * 再画折线轮廓（与折线图共用 rectLine）。
 * 填充色 = 系列色 × alpha 0.3。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsAreaChart extends BsLineChart {

    private float fillAlpha = 0.3f;

    public BsAreaChart setFillAlpha(float a) { this.fillAlpha = a; return this; }

    @Override
    protected void drawChart(ShapeRenderer sr) {
        float baseY = padBottom + legendPadBottom;
        for (int idx = 0; idx < seriesList.size(); idx++) {
            if (hidden.get(idx)) continue;
            Series s = seriesList.get(idx);
            if (s.points.isEmpty()) continue;
            Color c = s.color != null ? s.color : Color.GRAY;

            // 1. 填充：用三角形扇覆盖（顶点序列 → baseY → 闭合）
            sr.setColor(c.r, c.g, c.b, fillAlpha);
            // 第一个点
            float firstX = toScreenX(s.points.get(0).x);
            float firstY = toScreenY(s.points.get(0).y);
            float lastX = toScreenX(s.points.get(s.points.size() - 1).x);
            // 用 fan：基线左 → 各顶点 → 基线右
            // ShapeRenderer 没有 polygon filled，手动拆三角形
            for (int i = 0; i < s.points.size() - 1; i++) {
                Point p0 = (i == 0) ? null : null;
                Point a = s.points.get(i);
                Point b = s.points.get(i + 1);
                float ax = toScreenX(a.x), ay = toScreenY(a.y);
                float bx = toScreenX(b.x), by = toScreenY(b.y);
                // 三角形：(ax,ay) → (bx,by) → (bx, baseY)
                sr.triangle(ax, ay, bx, by, bx, baseY);
                // 三角形：(ax,ay) → (bx, baseY) → (ax, baseY)  ← 填补底部矩形
                sr.triangle(ax, ay, bx, baseY, ax, baseY);
            }

            // 2. 折线轮廓
            sr.setColor(c);
            for (int i = 1; i < s.points.size(); i++) {
                Point a = s.points.get(i - 1);
                Point b = s.points.get(i);
                rectLine(sr, toScreenX(a.x), toScreenY(a.y), toScreenX(b.x), toScreenY(b.y), lineWidth);
            }

            // 3. 数据点
            if (showPoints) {
                for (Point p : s.points) {
                    float sx = toScreenX(p.x);
                    float sy = toScreenY(p.y);
                    sr.setColor(c);
                    sr.circle(sx, sy, pointRadius + 1);
                    sr.setColor(Color.WHITE);
                    sr.circle(sx, sy, pointRadius - 1);
                }
            }
        }
    }
}
