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

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap 风格饼图（PieChart）—— 基于 {@link BsChart}，扇形填充。
 *
 * <p>数据模型：每个 slice 一个 (label, value, color)。整体占比按所有 value 之和归一化。</p>
 *
 * <p>完整功能：</p>
 * <ul>
 *   <li>饼图 / 环形图（{@link #setDonutHole}）</li>
 *   <li>Hover 显示百分比 tooltip</li>
 *   <li>图例（外置）：每行 = 颜色块 + 标签 + 百分比</li>
 *   <li>点击 slice 切换显隐（隐藏的 slice 从总数中扣除，重新归一化）</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsPieChart chart = new BsPieChart();
 * chart.setSize(360, 280);
 * chart.setSkinFont(skin);
 * chart.setLegendPlacement(BsChart.LegendPlacement.RIGHT);
 * chart.setSlices(
 *     "Chrome", 65,
 *     "Firefox", 15,
 *     "Safari", 12,
 *     "Edge", 5,
 *     "Other", 3
 * );
 * stage.addActor(chart);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsPieChart extends BsChart {

    public static class Slice {
        public final String label;
        public final float value;
        public Color color;
        public Slice(String label, float value) { this.label = label; this.value = value; }
        public Slice(String label, float value, Color color) {
            this.label = label; this.value = value; this.color = color;
        }
    }

    private final List<Slice> slices = new ArrayList<>();
    private float donutHole = 0f;
    /** 每个 slice 的扇形参数 [startDeg, sweepDeg, cx, cy, r]，hover 检测用。 */
    private final List<float[]> sliceRects = new ArrayList<>();

    public BsPieChart setSlices(Object... labelValuePairs) {
        slices.clear();
        for (int i = 0; i + 1 < labelValuePairs.length; i += 2) {
            String label = String.valueOf(labelValuePairs[i]);
            float v = ((Number) labelValuePairs[i + 1]).floatValue();
            slices.add(new Slice(label, v, palette(i / 2)));
        }
        hidden.clear();
        return this;
    }

    public BsPieChart addSlice(String label, float value) {
        slices.add(new Slice(label, value, palette(slices.size())));
        return this;
    }

    public BsPieChart setDonutHole(float ratio) {
        this.donutHole = Math.max(0, Math.min(0.9f, ratio));
        return this;
    }

    public BsPieChart() {
        super();
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                int legendIdx = hitTestLegend(x, y);
                if (legendIdx >= 0) {
                    hidden.flip(legendIdx);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void drawAxesAndGrid(ShapeRenderer sr) {
        // 饼图不画坐标轴
    }

    @Override
    protected void drawAxisLabels(Batch batch, float parentAlpha) {
        // 饼图不画坐标轴标签
    }

    /** 饼图把 hidden 看作 slice 的隐藏；用 slicesList 同步 seriesList 方便基类图例。 */
    @Override
    public BsChart setMultiSeries(List<Series> series) {
        // 饼图数据用 setSlices，不走多系列
        return this;
    }

    @Override
    protected void drawChart(ShapeRenderer sr) {
        if (slices.isEmpty()) return;
        // 饼图把 slices 同步到 seriesList 让基类图例能用
        syncSlicesToSeries();

        // 计算可见 slice 总值
        float total = 0;
        for (int i = 0; i < slices.size(); i++) {
            if (hidden.get(i)) continue;
            total += slices.get(i).value;
        }
        if (total <= 0) return;

        float size = computePieSize();
        float cx = computePieCx(size);
        float cy = computePieCy();
        float r = size / 2f;
        // 缓存稳定参考（不含外推），hover 检测用
        lastPieCx = cx; lastPieCy = cy; lastPieR = r;

        sliceRects.clear();
        float startAngleDeg = 90f;   // 从顶部开始
        for (int i = 0; i < slices.size(); i++) {
            if (hidden.get(i)) continue;
            Slice s = slices.get(i);
            float sweep = s.value / total * 360f;
            Color c = s.color != null ? s.color : Color.GRAY;
            // 记录稳定的 slice 参数（原始 cx/cy/r，不含外推），用于 hover 检测
            // arc 数学坐标系：startAngleDeg - sweep 是起始角（顺时针）
            sliceRects.add(new float[]{i, startAngleDeg - sweep, sweep});
            // hover 时扇形稍微外推（不影响 sliceRects 的稳定参数）
            boolean isHovered = (hoverSeriesIdx == i);
            float ox = 0, oy = 0;
            if (isHovered) {
                float midDeg = (float) Math.toRadians(startAngleDeg - sweep / 2f);
                ox = (float) Math.cos(midDeg) * 6;
                oy = (float) Math.sin(midDeg) * 6;
            }
            sr.setColor(c);
            sr.arc(cx + ox, cy + oy, r, startAngleDeg - sweep, sweep, Math.max(6, (int) (sweep / 4)));
            startAngleDeg -= sweep;
        }

        // 环形：挖空中心
        if (donutHole > 0) {
            sr.setColor(BsTheme.be());
            sr.circle(cx, cy, r * donutHole);
        }
    }

    /** 计算饼图直径（让出图例区）。 */
    private float computePieSize() {
        float wAvail = getWidth() - (legendPlacement == LegendPlacement.RIGHT ? measureLegendColWidth() + 12 : 0)
                - (legendPlacement == LegendPlacement.LEFT ? measureLegendColWidth() + 12 : 0);
        float hAvail = getHeight() - (legendPlacement == LegendPlacement.TOP ? 30 : 0)
                - (legendPlacement == LegendPlacement.BOTTOM ? 30 : 0);
        return Math.max(40, Math.min(wAvail, hAvail) - 8);
    }

    /** RIGHT 图例时图例的起始 x（drawChart 阶段计算，drawLegend 阶段消费）。-1 = 未设置。 */
    private float legendStartXComputed = -1;

    private float computePieCx(float size) {
        if (legendPlacement == LegendPlacement.RIGHT) {
            float legendW = measureLegendColWidth();
            float gap = 12f;
            // 饼图水平居中，图例紧贴右侧
            float cx = getWidth() / 2f;
            legendStartXComputed = cx + size / 2f + gap;
            // 图例溢出右边界时，退回饼图+图例整体居中
            if (legendStartXComputed + legendW > getWidth() - 4) {
                float groupW = size + gap + legendW;
                float startX = Math.max(4, (getWidth() - groupW) / 2f);
                legendStartXComputed = startX + size + gap;
                return startX + size / 2f;
            }
            return cx;
        }
        if (legendPlacement == LegendPlacement.LEFT) return size / 2f + measureLegendColWidth() + 12;
        legendStartXComputed = -1;
        return getWidth() / 2f;
    }

    @Override
    protected float legendVerticalStartX() {
        return legendStartXComputed;
    }

    private float computePieCy() {
        return getHeight() / 2f;
    }

    /** 上次绘制的饼图中心/半径（稳定，不含外推），hover 检测参考。 */
    protected float lastPieCx, lastPieCy, lastPieR;

    @Override
    protected void drawHoverHighlight(ShapeRenderer sr) {
        // 已在 drawChart 中处理（外推效果）
    }

    /**
     * 鼠标 hover 检测：基于稳定的 cx/cy/r 与每个 slice 的起始角/扇形角，
     * 用严格的顺时针区间判断。
     */
    @Override
    protected void updateHover() {
        if (!hoverEnabled || hoverLocalX < 0 || sliceRects.isEmpty()) {
            hoverSeriesIdx = -1; hoverPointIdx = -1;
            return;
        }
        float dx = hoverLocalX - lastPieCx;
        float dy = hoverLocalY - lastPieCy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        // 不在饼范围内
        if (dist > lastPieR) { hoverSeriesIdx = -1; hoverPointIdx = -1; return; }
        // 环形中心空洞
        if (donutHole > 0 && dist < lastPieR * donutHole) {
            hoverSeriesIdx = -1; hoverPointIdx = -1; return;
        }
        // 鼠标在数学坐标系下的角度（弧度 → 度），atan2 返回 [-180, 180]
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        // 标准化到 [0, 360)
        float mouseAngle = ((angle % 360) + 360) % 360;

        // 遍历每个 slice，检查鼠标角度是否落在 [startAngle, endAngle]（顺时针区间）
        for (float[] r : sliceRects) {
            int sliceIdx = (int) r[0];
            float start = r[1];   // arc 起始角（数学坐标系）
            float sweep = r[2];   // 扇形角（正值）
            // arc 的语义：从 start 起，逆时针扫过 sweep 度（libgdx arc 是 CCW）
            // 但视觉上 start = startAngleDeg - sweep 是从顶部"顺时针"分布
            // 这里直接用 arc 的 [start, start+sweep] 作为数学坐标系下的扇形覆盖
            float s = ((start % 360) + 360) % 360;
            float e = s + sweep;   // 可能 > 360
            // 判断 mouseAngle 是否在 [s, e] 内（考虑跨 0° 的情况）
            if (isAngleInRange(mouseAngle, s, e)) {
                hoverSeriesIdx = sliceIdx;
                hoverPointIdx = 0;
                return;
            }
        }
        hoverSeriesIdx = -1; hoverPointIdx = -1;
    }

    /** 判断角度 m 是否在 [s, e]（度，e 可 > 360 表示跨 0°），允许 1° 容差。 */
    private static boolean isAngleInRange(float m, float s, float e) {
        // 把 m 平移两次比较，覆盖跨 0°
        if (m >= s - 1 && m <= e + 1) return true;
        float m2 = m + 360;
        if (m2 >= s - 1 && m2 <= e + 1) return true;
        return false;
    }

    @Override
    protected void drawTooltip(Batch batch, float parentAlpha) {
        if (hoverSeriesIdx < 0 || hoverSeriesIdx >= slices.size()) return;
        Slice s = slices.get(hoverSeriesIdx);
        float total = 0;
        for (int i = 0; i < slices.size(); i++) {
            if (hidden.get(i)) continue;
            total += slices.get(i).value;
        }
        if (total <= 0) return;
        float pct = s.value / total * 100f;
        drawTooltipBox(batch, parentAlpha,
                new String[]{s.label, fmt(s.value) + "  (" + fmt(pct) + "%)"},
                s.color != null ? s.color : Color.GRAY);
    }

    /** 把 slices 同步到 seriesList，让基类图例能渲染。 */
    private void syncSlicesToSeries() {
        if (seriesList.size() == slices.size()) return;
        seriesList.clear();
        for (Slice s : slices) {
            // 用 label 做系列名；points 用 slice.value 构造一个点
            List<Point> pts = new ArrayList<>();
            pts.add(new Point(0, s.value));
            seriesList.add(new Series(s.label, pts, s.color));
        }
    }

    @Override
    protected void drawLegend(Batch batch, float parentAlpha) {
        syncSlicesToSeries();
        if (slices.isEmpty()) return;
        // 计算可见 slice 总值（隐藏的从总数扣除）
        float total = 0;
        for (int i = 0; i < slices.size(); i++) {
            if (hidden.get(i)) continue;
            total += slices.get(i).value;
        }
        final float totalFinal = Math.max(0.0001f, total);
        // 横向布局标签只显示名字；纵向（左/右）显示 名字+百分比
        boolean horizontal = (legendPlacement == LegendPlacement.TOP || legendPlacement == LegendPlacement.BOTTOM);
        // 复用基类的精确测量 + legendRects 填充
        super.drawLegend(batch, parentAlpha, i -> {
            Slice s = slices.get(i);
            if (horizontal) return s.label;
            return s.label + " " + fmt(s.value / totalFinal * 100) + "%";
        });
    }

    public List<Slice> getSlices() { return slices; }
}
