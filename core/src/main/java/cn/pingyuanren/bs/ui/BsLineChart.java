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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

/**
 * Bootstrap 风格折线图（LineChart）—— 基于 {@link BsChart}。
 *
 * <p>完整功能：</p>
 * <ul>
 *   <li>X/Y 坐标轴 + 刻度线 + 数值标签（轴值由 min/max 自动生成 5 等分刻度）</li>
 *   <li>多系列折线，每条独立颜色，可开关数据点</li>
 *   <li>图例（顶部/底部/左/右），点击切换系列显隐</li>
 *   <li>Hover tooltip：鼠标靠近数据点时显示坐标和数值</li>
 *   <li>点击隔离：{@link #setClickToIsolate(true)} 后单击只显示该系列，
 *       Shift+click 多选对比</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsLineChart chart = new BsLineChart();
 * chart.setSize(480, 240);
 * chart.setSkinFont(skin);
 * chart.setLegendPlacement(BsChart.LegendPlacement.TOP);
 * chart.setHoverEnabled(true);
 * chart.setClickToIsolate(true);
 * chart.setMultiSeries(Arrays.asList(
 *     new BsChart.Series("销量", BsChart.pointsOfY(3, 5, 4, 8, 7, 10, 6)),
 *     new BsChart.Series("库存", BsChart.pointsOfY(8, 7, 9, 5, 6, 4, 7))
 * ));
 * stage.addActor(chart);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsLineChart extends BsChart {

    protected boolean showPoints = true;
    protected boolean showGrid = true;
    protected float lineWidth = 2.5f;
    protected float pointRadius = 3.5f;
    /** 鼠标 hover 命中半径（屏幕像素）。 */
    protected float hitRadius = 12f;

    public BsLineChart setShowPoints(boolean v) { this.showPoints = v; return this; }
    public BsLineChart setShowGrid(boolean v) { this.showGrid = v; return this; }
    public BsLineChart setLineWidth(float w) { this.lineWidth = w; return this; }
    public BsLineChart setPointRadius(float r) { this.pointRadius = r; return this; }
    public BsLineChart setHitRadius(float r) { this.hitRadius = r; return this; }

    public BsLineChart() {
        super();
        // 点击隔离 / 图例点击切换
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // 1. 优先检测是否点中图例条目
                int legendIdx = hitTestLegend(x, y);
                if (legendIdx >= 0) {
                    toggleSeries(legendIdx);
                    return true;
                }
                // 2. 点击隔离：检查是否点中数据点附近
                if (clickToIsolate) {
                    int[] hit = hitTestPoint(x, y);
                    if (hit[0] >= 0) {
                        boolean additive = (GdxShift || GdxControl);   // 简化：仅 Shift 多选
                        isolateSeries(hit[0], additive);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    // 用静态字段读取 Shift 状态（避免依赖 Gdx.input 静态导入）
    private static boolean GdxShift = false;
    private static boolean GdxControl = false;
    /** 由外部周期性刷新 modifier 状态（简化方案：测试台用 Gdx.input.isKeyPressed 直接查）。 */
    public static void setModifiers(boolean shift, boolean ctrl) {
        GdxShift = shift; GdxControl = ctrl;
    }

    @Override
    protected void drawAxesAndGrid(ShapeRenderer sr) {
        if (!axesVisible) return;
        float w = getWidth();
        float h = getHeight();
        float plotH = h - padTop - padBottom - legendPadTop - legendPadBottom;
        float baseY = padBottom + legendPadBottom;
        float leftX = padLeft + legendPadLeft;
        float rightX = w - padRight;
        // 水平网格
        if (showGrid) {
            sr.setColor(gridColor);
            for (int i = 0; i <= yTickCount; i++) {
                float y = baseY + plotH * i / (float) yTickCount;
                rectLine(sr, leftX, y, rightX, y, 1);
            }
        }
        // 主轴
        sr.setColor(axisColor);
        rectLine(sr, leftX, baseY, leftX, h - padTop - legendPadTop, 1.5f);
        rectLine(sr, leftX, baseY, rightX, baseY, 1.5f);
    }

    @Override
    protected void drawChart(ShapeRenderer sr) {
        for (int idx = 0; idx < seriesList.size(); idx++) {
            if (hidden.get(idx)) continue;
            Series s = seriesList.get(idx);
            if (s.points.isEmpty()) continue;
            Color c = s.color != null ? s.color : Color.GRAY;
            // 折线
            sr.setColor(c);
            for (int i = 1; i < s.points.size(); i++) {
                Point a = s.points.get(i - 1);
                Point b = s.points.get(i);
                rectLine(sr, toScreenX(a.x), toScreenY(a.y), toScreenX(b.x), toScreenY(b.y), lineWidth);
            }
            // 数据点
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

    @Override
    protected void drawHoverHighlight(ShapeRenderer sr) {
        if (hoverSeriesIdx < 0 || hoverPointIdx < 0) return;
        if (hidden.get(hoverSeriesIdx)) return;
        Series s = seriesList.get(hoverSeriesIdx);
        if (hoverPointIdx >= s.points.size()) return;
        Point p = s.points.get(hoverPointIdx);
        float sx = toScreenX(p.x);
        float sy = toScreenY(p.y);
        Color c = s.color != null ? s.color : Color.GRAY;
        // 外圈白色
        sr.setColor(1, 1, 1, 0.9f);
        sr.circle(sx, sy, pointRadius + 5);
        // 中间 accent 色
        sr.setColor(c);
        sr.circle(sx, sy, pointRadius + 2);
        // 中心白色
        sr.setColor(Color.WHITE);
        sr.circle(sx, sy, pointRadius);
    }

    /** hover 检测：找最近的数据点。 */
    @Override
    protected void updateHover() {
        if (!hoverEnabled || hoverLocalX < 0) {
            hoverSeriesIdx = -1; hoverPointIdx = -1;
            return;
        }
        int[] hit = hitTestPoint(hoverLocalX, hoverLocalY);
        hoverSeriesIdx = hit[0];
        hoverPointIdx = hit[1];
    }

    /** 命中测试：返回 [seriesIdx, pointIdx]，未命中返回 [-1, -1]。 */
    private int[] hitTestPoint(float x, float y) {
        float bestDist = hitRadius;
        int bestS = -1, bestP = -1;
        for (int si = 0; si < seriesList.size(); si++) {
            if (hidden.get(si)) continue;
            Series s = seriesList.get(si);
            for (int pi = 0; pi < s.points.size(); pi++) {
                Point p = s.points.get(pi);
                float dx = toScreenX(p.x) - x;
                float dy = toScreenY(p.y) - y;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < bestDist) {
                    bestDist = d; bestS = si; bestP = pi;
                }
            }
        }
        return new int[]{bestS, bestP};
    }

    /** 图例命中测试复用基类的精确实现（基于 legendRects 缓存）。 */

    /** 粗线段（多次填充模拟线宽）。 */
    protected void rectLine(ShapeRenderer sr, float x1, float y1, float x2, float y2, float width) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.5f) return;
        float nx = -dy / length;
        float ny = dx / length;
        float half = width / 2f;
        float ax = x1 + nx * half, ay = y1 + ny * half;
        float bx = x2 + nx * half, by = y2 + ny * half;
        float cx = x2 - nx * half, cy = y2 - ny * half;
        float dx_ = x1 - nx * half, dy_ = y1 - ny * half;
        sr.triangle(ax, ay, bx, by, cx, cy);
        sr.triangle(ax, ay, cx, cy, dx_, dy_);
    }
}
