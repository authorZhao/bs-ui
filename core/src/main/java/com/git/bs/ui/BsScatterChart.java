package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Bootstrap 风格散点图（ScatterChart）—— X/Y 平面上的点分布，用于查看两个变量的相关性。
 *
 * <p>与折线图的区别：不连线，只画点；点可大可小、可透明（避免重叠遮挡）。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsScatterChart chart = new BsScatterChart();
 * chart.setSize(480, 320);
 * chart.setSkinFont(skin);
 * chart.setPointRadius(5);
 * chart.setMultiSeries(Arrays.asList(
 *     new BsChart.Series("男", BsChart.points(15, 60, 18, 65, 22, 70, 25, 75)),
 *     new BsChart.Series("女", BsChart.points(14, 55, 16, 60, 20, 68, 23, 72))
 * ));
 * stage.addActor(chart);
 * }</pre>
 */
public class BsScatterChart extends BsChart {

    private float pointRadius = 4;
    private float pointAlpha = 0.85f;

    public BsScatterChart setPointRadius(float r) { this.pointRadius = r; return this; }
    public BsScatterChart setPointAlpha(float a) { this.pointAlpha = a; return this; }

    public BsScatterChart() {
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
    protected void drawAxesAndGrid(ShapeRenderer sr) {
        if (!axesVisible) return;
        float w = getWidth();
        float h = getHeight();
        float plotH = h - padTop - padBottom - legendPadTop - legendPadBottom;
        float baseY = padBottom + legendPadBottom;
        float leftX = padLeft + legendPadLeft;
        float rightX = w - padRight;
        if (gridVisible) {
            sr.setColor(gridColor);
            for (int i = 0; i <= 5; i++) {
                float y = baseY + plotH * i / 5f;
                rectLine(sr, leftX, y, rightX, y, 1);
            }
        }
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
            sr.setColor(c.r, c.g, c.b, pointAlpha);
            for (Point p : s.points) {
                float sx = toScreenX(p.x);
                float sy = toScreenY(p.y);
                boolean isHovered = (hoverSeriesIdx == idx && hoverPointIdx >= 0
                        && s.points.get(hoverPointIdx) == p);
                float r = isHovered ? pointRadius + 2 : pointRadius;
                sr.circle(sx, sy, r);
                if (isHovered) {
                    // 外圈白色描边
                    sr.setColor(1, 1, 1, 0.6f);
                    sr.circle(sx, sy, r + 2);
                    sr.setColor(c.r, c.g, c.b, pointAlpha);
                }
            }
        }
    }

    @Override
    protected void updateHover() {
        if (!hoverEnabled || hoverLocalX < 0) {
            hoverSeriesIdx = -1; hoverPointIdx = -1;
            return;
        }
        float bestDist = pointRadius + 6;
        int bestS = -1, bestP = -1;
        for (int si = 0; si < seriesList.size(); si++) {
            if (hidden.get(si)) continue;
            Series s = seriesList.get(si);
            for (int pi = 0; pi < s.points.size(); pi++) {
                Point p = s.points.get(pi);
                float dx = toScreenX(p.x) - hoverLocalX;
                float dy = toScreenY(p.y) - hoverLocalY;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < bestDist) {
                    bestDist = d; bestS = si; bestP = pi;
                }
            }
        }
        hoverSeriesIdx = bestS;
        hoverPointIdx = bestP;
    }

    @Override
    protected void drawTooltip(Batch batch, float parentAlpha) {
        if (hoverSeriesIdx < 0 || hoverPointIdx < 0) return;
        if (hoverSeriesIdx >= seriesList.size()) return;
        Series s = seriesList.get(hoverSeriesIdx);
        if (hoverPointIdx >= s.points.size()) return;
        Point p = s.points.get(hoverPointIdx);
        String line1 = (s.label == null ? "系列" + (hoverSeriesIdx + 1) : s.label);
        String line2 = "x=" + fmt(p.x) + "  y=" + fmt(p.y);
        drawTooltipBox(batch, parentAlpha, new String[]{line1, line2},
                s.color != null ? s.color : Color.GRAY);
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
