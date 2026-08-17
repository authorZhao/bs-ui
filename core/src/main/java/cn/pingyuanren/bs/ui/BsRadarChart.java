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
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.Arrays;
import java.util.List;

/**
 * Bootstrap 风格雷达图（RadarChart）—— 多边形雷达图，用于多维度数据对比。
 *
 * <p>每个维度（axis）从中心向外辐射，{@link Series} 的 {@code points} 中
 * 第 i 个点的 y 值代表第 i 个维度的数值（x 忽略），y ∈ [0, maxValue]。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsRadarChart chart = new BsRadarChart();
 * chart.setSize(360, 360);
 * chart.setSkinFont(skin);
 * chart.setMaxValue(100);
 * chart.setAxes("攻击", "防御", "速度", "智力", "运气");
 * chart.setMultiSeries(Arrays.asList(
 *     new BsChart.Series("战士", BsChart.pointsOfY(80, 90, 40, 50, 60)),
 *     new BsChart.Series("法师", BsChart.pointsOfY(30, 40, 60, 95, 70))
 * ));
 * stage.addActor(chart);
 * }</pre>
 *
 * <p>实现：维度轴均匀分布在 360°，从顶部开始顺时针。
 * 每个 series 画一个多边形（filled + outline）+ 顶点圆。
 * 维度名画在每个轴端点外。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsRadarChart extends BsChart {

    private String[] axes;
    private float maxValue = 100f;
    private float fillAlpha = 0.25f;
    /** 各维度轴端点的屏幕坐标缓存（label 用）。 */
    private float[][] axisEnds;

    public BsRadarChart setAxes(String... names) { this.axes = names; return this; }
    public BsRadarChart setMaxValue(float v) { this.maxValue = v; return this; }
    public BsRadarChart setFillAlpha(float a) { this.fillAlpha = a; return this; }

    public BsRadarChart() {
        super();
        addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                int legendIdx = hitTestLegend(x, y);
                if (legendIdx >= 0) {
                    toggleSeries(legendIdx);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void recomputeBounds() {
        // 雷达图不用 minX/maxX/minY/maxY，但基类调用，简单初始化避免 NPE
        minX = 0; maxX = 1; minY = 0; maxY = maxValue;
        // 根据 series 自动算 maxValue
        float m = 0;
        for (Series s : seriesList) {
            for (Point p : s.points) if (p.y > m) m = p.y;
        }
        if (m > maxValue) maxValue = m;
        if (maxValue == 0) maxValue = 1;
        maxY = maxValue;
    }

    @Override
    protected void drawAxesAndGrid(ShapeRenderer sr) {
        // 雷达图自己画轴/网格（不是水平线）
    }

    @Override
    protected void drawChart(ShapeRenderer sr) {
        if (seriesList.isEmpty()) return;
        int dimCount = (axes != null) ? axes.length : seriesList.get(0).points.size();
        if (dimCount < 3) return;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 2f
                - (legendPlacement == LegendPlacement.TOP ? 24 : 12) - 30;   // 留出标签空间

        axisEnds = new float[dimCount][2];

        // 1. 画背景网格：每个维度的轴线 + 同心多边形（25%/50%/75%/100%）
        sr.setColor(gridColor);
        for (int level = 1; level <= 4; level++) {
            float lr = r * level / 4f;
            for (int i = 0; i < dimCount; i++) {
                float a1 = angleFor(i, dimCount);
                float a2 = angleFor(i + 1, dimCount);
                float x1 = cx + (float) Math.cos(a1) * lr;
                float y1 = cy + (float) Math.sin(a1) * lr;
                float x2 = cx + (float) Math.cos(a2) * lr;
                float y2 = cy + (float) Math.sin(a2) * lr;
                rectLine(sr, x1, y1, x2, y2, 1);
            }
        }
        // 轴线（从中心到外）
        sr.setColor(axisColor);
        for (int i = 0; i < dimCount; i++) {
            float a = angleFor(i, dimCount);
            float x = cx + (float) Math.cos(a) * r;
            float y = cy + (float) Math.sin(a) * r;
            rectLine(sr, cx, cy, x, y, 1);
            axisEnds[i][0] = x;
            axisEnds[i][1] = y;
        }

        // 2. 画每个系列的多边形
        for (int idx = 0; idx < seriesList.size(); idx++) {
            if (hidden.get(idx)) continue;
            Series s = seriesList.get(idx);
            if (s.points.size() < dimCount) continue;
            Color c = s.color != null ? s.color : Color.GRAY;

            // 计算各维度顶点
            float[] xs = new float[dimCount];
            float[] ys = new float[dimCount];
            for (int i = 0; i < dimCount; i++) {
                float a = angleFor(i, dimCount);
                float v = s.points.get(i).y / maxValue;
                v = Math.max(0, Math.min(1, v));
                xs[i] = cx + (float) Math.cos(a) * r * v;
                ys[i] = cy + (float) Math.sin(a) * r * v;
            }
            // 填充（三角形扇：中心 + 相邻两顶点）
            sr.setColor(c.r, c.g, c.b, fillAlpha);
            for (int i = 0; i < dimCount; i++) {
                int j = (i + 1) % dimCount;
                sr.triangle(cx, cy, xs[i], ys[i], xs[j], ys[j]);
            }
            // 描边
            sr.setColor(c);
            for (int i = 0; i < dimCount; i++) {
                int j = (i + 1) % dimCount;
                rectLine(sr, xs[i], ys[i], xs[j], ys[j], 2);
            }
            // 顶点
            for (int i = 0; i < dimCount; i++) {
                sr.circle(xs[i], ys[i], 3.5f);
                sr.setColor(Color.WHITE);
                sr.circle(xs[i], ys[i], 2);
                sr.setColor(c);
            }
        }
    }

    @Override
    protected void drawAxisLabels(Batch batch, float parentAlpha) {
        // 标注维度名（在轴端外 12px）
        if (axes == null || axisEnds == null) return;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float oldColor = packColor(font.getColor());
        font.setColor(textColor.r, textColor.g, textColor.b, textColor.a * parentAlpha);
        for (int i = 0; i < axes.length && i < axisEnds.length; i++) {
            float ex = axisEnds[i][0];
            float ey = axisEnds[i][1];
            // 从中心向外的方向，偏移 14px
            float dx = ex - cx, dy = ey - cy;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.5f) continue;
            float lx = ex + dx / len * 14;
            float ly = ey + dy / len * 14;
            glyphLayout.setText(font, axes[i]);
            // 文字垂直居中
            font.draw(batch, axes[i],
                    getX() + lx - glyphLayout.width / 2f,
                    getY() + ly + glyphLayout.height / 2f);
        }
        font.setColor(unpackColor(oldColor));
    }

    /** 计算第 i 个维度（共 dimCount 个）的角度（弧度，数学坐标系）。
     *  从顶部（90°）开始，顺时针均匀分布。
     */
    private static float angleFor(int i, int dimCount) {
        // 顶部 = 90°，顺时针 → 角度递减
        float deg = 90f - i * (360f / dimCount);
        return (float) Math.toRadians(deg);
    }

    protected void rectLine(ShapeRenderer sr, float x1, float y1, float x2, float y2, float width) {
        float dx = x2 - x1, dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.5f) return;
        float nx = -dy / length, ny = dx / length;
        float half = width / 2f;
        float ax = x1 + nx * half, ay = y1 + ny * half;
        float bx = x2 + nx * half, by = y2 + ny * half;
        float cx = x2 - nx * half, cy = y2 - ny * half;
        float dx_ = x1 - nx * half, dy_ = y1 - ny * half;
        sr.triangle(ax, ay, bx, by, cx, cy);
        sr.triangle(ax, ay, cx, cy, dx_, dy_);
    }
}
