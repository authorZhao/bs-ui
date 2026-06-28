package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap 风格柱状图（BarChart）—— 基于 {@link BsChart}。
 *
 * <p>支持：垂直/水平方向、多系列分组、可选间距、Hover tooltip（柱子高亮 + 数值）。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsBarChart chart = new BsBarChart();
 * chart.setSize(480, 240);
 * chart.setSkinFont(skin);
 * chart.setOrientation(BsBarChart.Orientation.VERTICAL);
 * chart.setCategories("Q1", "Q2", "Q3", "Q4");
 * chart.setMultiSeries(Arrays.asList(
 *     new BsChart.Series("2024", BsChart.pointsOfY(10, 14, 18, 22)),
 *     new BsChart.Series("2025", BsChart.pointsOfY(15, 19, 23, 28))
 * ));
 * stage.addActor(chart);
 * }</pre>
 */
public class BsBarChart extends BsChart {

    public enum Orientation { VERTICAL, HORIZONTAL }

    private Orientation orientation = Orientation.VERTICAL;
    private String[] categories;
    private float groupGapRatio = 0.2f;
    private float barGapRatio = 0.1f;
    /** hover 高亮的 [categoryIdx, seriesIdx]。 */
    private int hoverCat = -1, hoverSeries = -1;
    /** 每柱的位置缓存（hover 检测用）：每个柱子记录 {categoryIdx, seriesIdx, x, y, w, h}。 */
    private final List<float[]> barRects = new ArrayList<>();

    public BsBarChart setOrientation(Orientation o) { this.orientation = o; return this; }
    public BsBarChart setCategories(String... names) { this.categories = names; return this; }
    public BsBarChart setGroupGapRatio(float r) { this.groupGapRatio = r; return this; }
    public BsBarChart setBarGapRatio(float r) { this.barGapRatio = r; return this; }

    public BsBarChart() {
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
        if (gridVisible) {
            sr.setColor(gridColor);
            for (int i = 0; i <= 5; i++) {
                float y = baseY + plotH * i / 5f;
                rectLine(sr, padLeft + legendPadLeft, y, w - padRight, y, 1);
            }
        }
        sr.setColor(axisColor);
        rectLine(sr, padLeft + legendPadLeft - 1.5f, baseY, padLeft + legendPadLeft - 1.5f,
                h - padTop - legendPadTop, 1.5f);
        rectLine(sr, padLeft + legendPadLeft, baseY - 1.5f, w - padRight, baseY - 1.5f, 1.5f);
    }

    @Override
    protected void drawChart(ShapeRenderer sr) {
        if (seriesList.isEmpty()) return;
        barRects.clear();   // 重建缓存
        int seriesCount = seriesList.size();
        int catCount = categories != null ? categories.length : seriesList.get(0).points.size();

        float plotW = getWidth() - padLeft - padRight - legendPadLeft - legendPadRight;
        float plotH = getHeight() - padTop - padBottom - legendPadTop - legendPadBottom;
        float baseY = padBottom + legendPadBottom;
        float baseX = padLeft + legendPadLeft;

        if (orientation == Orientation.VERTICAL) {
            // 垂直：组在 X 方向排列，柱向上生长
            float groupW = plotW / Math.max(1, catCount);
            float groupGap = groupW * groupGapRatio;
            float barsAreaW = groupW - groupGap;
            float barW = barsAreaW / seriesCount * (1 - barGapRatio);
            float barGapActual = (barsAreaW / seriesCount) * barGapRatio;

            for (int cat = 0; cat < catCount; cat++) {
                float groupStartX = baseX + cat * groupW + groupGap / 2;
                for (int sIdx = 0; sIdx < seriesCount; sIdx++) {
                    if (hidden.get(sIdx)) continue;
                    Series s = seriesList.get(sIdx);
                    if (cat >= s.points.size()) continue;
                    float v = s.points.get(cat).y;
                    Color c = s.color != null ? s.color : palette(sIdx);
                    boolean isHovered = (hoverCat == cat && hoverSeries == sIdx);
                    float barH = (maxY == 0) ? 0 : (v / maxY) * plotH;
                    float x = groupStartX + sIdx * (barW + barGapActual);
                    sr.setColor(c);
                    sr.rect(x, baseY, barW, barH);
                    barRects.add(new float[]{cat, sIdx, x, baseY, barW, barH});
                    if (isHovered) {
                        sr.setColor(1, 1, 1, 0.5f);
                        sr.rect(x, baseY + Math.max(0, barH - 3), barW, 3);
                    }
                }
            }
        } else {
            // 水平：组在 Y 方向排列，柱向右生长；groupH 基于 plotH（不再混用 plotW）
            float groupH = plotH / Math.max(1, catCount);
            float groupGapH = groupH * groupGapRatio;
            float barsAreaH = groupH - groupGapH;
            float barH = barsAreaH / seriesCount * (1 - barGapRatio);
            float barGapActual = (barsAreaH / seriesCount) * barGapRatio;

            for (int cat = 0; cat < catCount; cat++) {
                // 从下往上画 category；groupStartY 是该组底部
                float groupStartY = baseY + cat * groupH + groupGapH / 2;
                for (int sIdx = 0; sIdx < seriesCount; sIdx++) {
                    if (hidden.get(sIdx)) continue;
                    Series s = seriesList.get(sIdx);
                    if (cat >= s.points.size()) continue;
                    float v = s.points.get(cat).y;
                    Color c = s.color != null ? s.color : palette(sIdx);
                    boolean isHovered = (hoverCat == cat && hoverSeries == sIdx);
                    float barL = (maxY == 0) ? 0 : (v / maxY) * plotW;
                    float y = groupStartY + sIdx * (barH + barGapActual);
                    sr.setColor(c);
                    sr.rect(baseX, y, barL, barH);
                    barRects.add(new float[]{cat, sIdx, baseX, y, barL, barH});
                    if (isHovered) {
                        sr.setColor(1, 1, 1, 0.5f);
                        sr.rect(baseX + Math.max(0, barL - 3), y, 3, barH);
                    }
                }
            }
        }
    }

    @Override
    protected void drawHoverHighlight(ShapeRenderer sr) {
        // 已在 drawChart 中处理（isHovered 标记）
    }

    @Override
    protected void updateHover() {
        if (!hoverEnabled || hoverLocalX < 0) {
            hoverCat = -1; hoverSeries = -1;
            hoverSeriesIdx = -1; hoverPointIdx = -1;
            return;
        }
        hoverCat = -1; hoverSeries = -1;
        for (float[] r : barRects) {
            if (hoverLocalX >= r[2] && hoverLocalX <= r[2] + r[4]
                    && hoverLocalY >= r[3] && hoverLocalY <= r[3] + r[5]) {
                hoverCat = (int) r[0];
                hoverSeries = (int) r[1];
                hoverSeriesIdx = hoverSeries;
                hoverPointIdx = hoverCat;
                return;
            }
        }
        hoverSeriesIdx = -1; hoverPointIdx = -1;
    }

    @Override
    protected void drawTooltip(Batch batch, float parentAlpha) {
        if (hoverCat < 0 || hoverSeries < 0) return;
        Series s = seriesList.get(hoverSeries);
        if (hoverCat >= s.points.size()) return;
        float v = s.points.get(hoverCat).y;
        String catLabel = (categories != null && hoverCat < categories.length)
                ? categories[hoverCat] : ("#" + hoverCat);
        String seriesLabel = (s.label == null ? "系列" + (hoverSeries + 1) : s.label);
        drawTooltipBox(batch, parentAlpha,
                new String[]{seriesLabel, catLabel + " = " + fmt(v)},
                s.color != null ? s.color : Color.GRAY);
    }

    @Override
    protected void drawAxisLabels(Batch batch, float parentAlpha) {
        super.drawAxisLabels(batch, parentAlpha);
        // 额外画 category 标签（X 轴下方的 Q1/Q2/Q3/Q4）
        if (categories == null) return;
        float oldColor = packColor(font.getColor());
        font.setColor(textColor.r, textColor.g, textColor.b, textColor.a * parentAlpha);
        float plotW = getWidth() - padLeft - padRight - legendPadLeft - legendPadRight;
        float groupW = plotW / categories.length;
        for (int i = 0; i < categories.length; i++) {
            float sx = padLeft + legendPadLeft + groupW * (i + 0.5f);
            glyphLayout.setText(font, categories[i]);
            font.draw(batch, categories[i],
                    getX() + sx - glyphLayout.width / 2f,
                    getY() + padBottom + legendPadBottom - 18);
        }
        font.setColor(unpackColor(oldColor));
    }

    /** 图例命中测试复用基类（基于 legendRects）。 */

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
