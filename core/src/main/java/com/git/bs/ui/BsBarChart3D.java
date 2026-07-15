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
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 真 3D 柱状图 —— 基于 {@link BsChart}，使用等距投影 + ShapeRenderer。
 *
 * <p>每根柱子拆成 3 个可见面（顶 / 正 / 侧），按深度排序后绘制，
 * 三面明暗系数（顶 1.0 / 正 0.8 / 侧 0.6）模拟立体光照。
 * 暴露 {@link #setYawDegrees(float)} 支持拖拽旋转视角（默认 30° 等距）。</p>
 *
 * <p>零新依赖：复用 {@link BsChart#sr()} 单例、scene2d 的 draw 编排，
 * TeaVM/WebGL 兼容。3D 坐标 → 投影 → 2D 屏幕是真正的 3D 数学，
 * 只是渲染走 ShapeRenderer 而非 OpenGL 着色器管线。</p>
 *
 * <p><b>渲染热路径优化</b>：{@code drawChart} 每帧调用，已剔除所有 per-frame
 * 分配（trig 常量每帧只算一次，顶点投影写入复用缓冲，颜色明暗走标量乘法）。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsBarChart3D chart = new BsBarChart3D();
 * chart.setSize(640, 260);
 * chart.setSkinFont(skin);
 * chart.setCategories("Q1", "Q2", "Q3", "Q4");
 * chart.setMultiSeries(Arrays.asList(
 *     new BsChart.Series("2024", BsChart.pointsOfY(35, 48, 60, 72)),
 *     new BsChart.Series("2025", BsChart.pointsOfY(45, 55, 68, 88))
 * ));
 * stage.addActor(chart);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsBarChart3D extends BsChart {

    /** 视角偏航角（度）：0 = 正对正面，30 = 默认等距。可拖拽改。 */
    private float yawDeg = 30f;
    /** 俯仰角（度）：0 = 平视，默认 20（轻微俯视以看到顶面）。 */
    private float pitchDeg = 20f;
    /** 柱子深度（决定 Z 方向厚度，屏幕像素）。 */
    private float barDepth = 32f;
    /** 间距比例（沿用 2D 柱状图风格）。 */
    private float groupGapRatio = 0.22f;
    private float barGapRatio = 0.10f;

    private String[] categories;

    /** hover 高亮的 [categoryIdx, seriesIdx]。 */
    private int hoverCat = -1, hoverSeries = -1;
    /** hover 命中柱子的顶面屏幕 y（局部坐标），用于 tooltip 锚定到柱顶上方。 */
    private float hoverBarTopY = -1;

    /**
     * 每根柱子的命中缓存：{categoryIdx, seriesIdx, minX, minY, w, h}。
     * 每帧重建值（复用 float[] 实例避免分配）。
     */
    private final List<float[]> barRects = new ArrayList<>();

    /** 柱子几何缓冲（drawChart 每帧复用，避免 new）。 */
    private final List<Bar3D> bars = new ArrayList<>();

    /** 8 个顶点投影缓冲（drawBar 每帧复用）：[8][2]。索引见 {@link #V_*}。 */
    private final float[][] projBuf = new float[8][2];

    // 顶点投影缓冲索引（顺序：fbl, fbr, ftl, ftr, bbl, bbr, btl, btr）
    private static final int V_FBL = 0, V_FBR = 1, V_FTL = 2, V_FTR = 3;
    private static final int V_BBL = 4, V_BBR = 5, V_BTL = 6, V_BTR = 7;

    /** 每帧只算一次的投影常量（drawChart 入口赋值）。 */
    private float cosYaw, sinPitch;

    /**
     * 面/三角形外扩像素：封住 ShapeRenderer Filled 模式下相邻面共享边的光栅化缝隙。
     * 0.75px 足以覆盖 1px 漏风，又不至于让柱子轮廓明显变大。
     */
    private static final float SEAM_INFLATE = 0.75f;

    public BsBarChart3D setYawDegrees(float deg) { this.yawDeg = deg; return this; }
    public BsBarChart3D setPitchDegrees(float deg) { this.pitchDeg = deg; return this; }
    public BsBarChart3D setBarDepth(float d) { this.barDepth = d; return this; }
    public BsBarChart3D setGroupGapRatio(float r) { this.groupGapRatio = r; return this; }
    public BsBarChart3D setBarGapRatio(float r) { this.barGapRatio = r; return this; }
    public BsBarChart3D setCategories(String... names) { this.categories = names; return this; }

    public float getYawDegrees() { return yawDeg; }

    public BsBarChart3D() {
        super();
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                int legendIdx = hitTestLegend(x, y);
                if (legendIdx >= 0) {
                    toggleSeries(legendIdx);
                    return true;
                }
                return false;
            }
        });
    }

    // ========================= 3D 投影核心 =========================

    /**
     * 把柱子的 3D 局部坐标 (x, y, z) 投影写入 {@code out}（避免分配）。
     * <p><b>等距投影</b>（oblique）：背面（z&gt;0）相对正面往右上偏移，
     * 模拟"俯视"视角——顶面可见、背面抬升，立方体向后上方延伸。</p>
     * <pre>
     *   sx = x + z·cosYaw     // 背面横向右移（yaw 控制左右偏）
     *   sy = y + z·sinPitch   // 背面向上抬（pitch 控制俯视，scene2d y 轴向上）
     * </pre>
     * 依赖 {@link #cosYaw}/{@link #sinPitch}（drawChart 入口已算好）。
     * <p>注：正面 z=0 时投影即原坐标，hover AABB 命中精确。</p>
     */
    private void project(float x, float y, float z, float[] out) {
        out[0] = x + z * cosYaw;
        out[1] = y + z * sinPitch;
    }

    // ========================= 绘制 =========================

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
        barRects.clear();

        // 每帧只算一次投影常量（避免 project/depthKey 里重复 toRadians + cos/sin）
        float yawRad = (float) Math.toRadians(yawDeg);
        cosYaw = (float) Math.cos(yawRad);
        sinPitch = (float) Math.sin(Math.toRadians(pitchDeg));
        boolean yawPositive = cosYaw >= 0;

        int seriesCount = seriesList.size();
        int catCount = categories != null ? categories.length
                : seriesList.get(0).points.size();

        float plotW = getWidth() - padLeft - padRight - legendPadLeft - legendPadRight;
        float plotH = getHeight() - padTop - padBottom - legendPadTop - legendPadBottom;
        float baseY = padBottom + legendPadBottom;
        float baseX = padLeft + legendPadLeft;

        float groupW = plotW / Math.max(1, catCount);
        float groupGap = groupW * groupGapRatio;
        float barsAreaW = groupW - groupGap;
        float barW = barsAreaW / seriesCount * (1 - barGapRatio);
        float barGapActual = (barsAreaW / seriesCount) * barGapRatio;

        // 1. 收集每根柱子的几何 + 颜色 + 深度键（复用 Bar3D 池）
        int n = 0;
        for (int cat = 0; cat < catCount; cat++) {
            float groupStartX = baseX + cat * groupW + groupGap / 2;
            for (int sIdx = 0; sIdx < seriesCount; sIdx++) {
                if (hidden.get(sIdx)) continue;
                Series s = seriesList.get(sIdx);
                if (cat >= s.points.size()) continue;
                float v = s.points.get(cat).y;
                Color c = s.color != null ? s.color : palette(sIdx);
                float frontX = groupStartX + sIdx * (barW + barGapActual);

                Bar3D bar = obtainBar(n++);
                bar.cat = cat;
                bar.series = sIdx;
                bar.color = c;
                bar.hovered = (hoverCat == cat && hoverSeries == sIdx);
                bar.frontX = frontX;
                bar.barW = barW;
                bar.barH = (maxY == 0) ? 0 : (v / maxY) * plotH;
                // 深度键：投影后横向越小越靠后（先画）。
                // sortKey = -(centerSx)，降序排序即"远先画"——painter's algorithm
                bar.sortKey = -((frontX + barW / 2f) * cosYaw);
            }
        }
        // 截断 bars 到本次实际数量（list 复用，多余旧实例不影响）
        while (bars.size() > n) bars.remove(bars.size() - 1);

        // 2. 按 sortKey 降序排序（远先画）
        bars.sort((a, b) -> Float.compare(b.sortKey, a.sortKey));

        // 3. 逐根绘制 3 个可见面 + 命中缓存
        int rectIdx = 0;
        for (Bar3D bar : bars) {
            drawBar(sr, bar, baseY, yawPositive);
            // 命中检测缓存：用正面矩形（z=0 时投影即原坐标，AABB 精确）
            float minX = bar.frontX;
            float minY = baseY;
            float maxX = bar.frontX + bar.barW;
            float maxY2 = baseY + bar.barH;
            float[] r = obtainRect(rectIdx++);
            r[0] = bar.cat;
            r[1] = bar.series;
            r[2] = Math.min(minX, maxX);
            r[3] = Math.min(minY, maxY2);
            r[4] = Math.abs(maxX - minX);
            r[5] = Math.abs(maxY2 - minY);
        }
        while (barRects.size() > rectIdx) barRects.remove(barRects.size() - 1);
    }

    /** 从 Bar3D 池取一个（复用避免 per-frame new）。 */
    private Bar3D obtainBar(int idx) {
        if (idx < bars.size()) return bars.get(idx);
        Bar3D bar = new Bar3D();
        bars.add(bar);
        return bar;
    }

    /** 从 barRects 池取一个 float[6]（复用避免 per-frame new）。 */
    private float[] obtainRect(int idx) {
        if (idx < barRects.size()) return barRects.get(idx);
        float[] r = new float[6];
        barRects.add(r);
        return r;
    }

    /**
     * 单根 3D 柱子的 3 个可见面。yawPositive=true 时右侧面可见，否则左侧面可见。
     * 投影写入 {@link #projBuf}（每帧复用，零分配）。
     * <p>每面调用 fillQuad 时传入 quad 的几何中心，用于把 4 顶点向外扩张 ~0.5px，
     * 覆盖 ShapeRenderer Filled 模式下相邻三角形/面共享边的光栅化缝隙（"漏风"）。</p>
     */
    private void drawBar(ShapeRenderer sr, Bar3D bar, float baseY, boolean yawPositive) {
        float x0 = bar.frontX;
        float x1 = bar.frontX + bar.barW;
        float y0 = baseY;
        float y1 = baseY + bar.barH;
        float z1 = barDepth;

        // 8 顶点投影（写入复用缓冲）
        project(x0, y0, 0,  projBuf[V_FBL]);
        project(x1, y0, 0,  projBuf[V_FBR]);
        project(x0, y1, 0,  projBuf[V_FTL]);
        project(x1, y1, 0,  projBuf[V_FTR]);
        project(x0, y0, z1, projBuf[V_BBL]);
        project(x1, y0, z1, projBuf[V_BBR]);
        project(x0, y1, z1, projBuf[V_BTL]);
        project(x1, y1, z1, projBuf[V_BTR]);

        Color c = bar.color;
        // 顶面（恒可见，最亮 ×1.0）：ftl, ftr, btr, btl
        fillQuad(sr, c, 1.0f, V_FTL, V_FTR, V_BTR, V_BTL);
        // 正面（朝向观察者 ×0.8）：fbl, fbr, ftr, ftl
        fillQuad(sr, c, 0.8f, V_FBL, V_FBR, V_FTR, V_FTL);
        // 侧面 ×0.6：yawPositive → 右侧（x1）可见 fbr, bbr, btr, ftr
        //          否则 → 左侧（x0）可见 bbl, fbl, ftl, btl
        if (yawPositive) {
            fillQuad(sr, c, 0.6f, V_FBR, V_BBR, V_BTR, V_FTR);
        } else {
            fillQuad(sr, c, 0.6f, V_BBL, V_FBL, V_FTL, V_BTL);
        }

        // hover 高亮：正面顶边亮线
        if (bar.hovered) {
            sr.setColor(1, 1, 1, 0.85f);
            float[] ftl = projBuf[V_FTL], ftr = projBuf[V_FTR];
            rectLine(sr, ftl[0], ftl[1], ftr[0], ftr[1], 3);
        }
    }

    /**
     * 用 2 个三角形填充四边形，颜色 × 亮度系数。
     * <p>把 4 顶点沿"从 quad 中心指向自身"的方向外扩 {@link #SEAM_INFLATE}px 后再画，
     * 让相邻面共享边有 ~1px 重叠，封住 ShapeRenderer Filled 模式下的光栅化缝隙
     * （即"漏风"现象：相邻三角形/面的共享边因浮点光栅化出现 1px 透明缝）。</p>
     */
    private void fillQuad(ShapeRenderer sr, Color base, float shadeK,
                          int i0, int i1, int i2, int i3) {
        float r = Math.min(1, base.r * shadeK);
        float g = Math.min(1, base.g * shadeK);
        float b = Math.min(1, base.b * shadeK);
        sr.setColor(r, g, b, base.a);

        float[] v0 = projBuf[i0], v1 = projBuf[i1], v2 = projBuf[i2], v3 = projBuf[i3];
        // quad 中心
        float cx = (v0[0] + v1[0] + v2[0] + v3[0]) * 0.25f;
        float cy = (v0[1] + v1[1] + v2[1] + v3[1]) * 0.25f;
        // 每顶点沿 (顶点-中心) 单位向量外扩 SEAM_INFLATE
        float a0x, a0y, a1x, a1y, a2x, a2y, a3x, a3y;
        float dx, dy, len;
        dx = v0[0] - cx; dy = v0[1] - cy;
        len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) { a0x = v0[0]; a0y = v0[1]; } else {
            a0x = v0[0] + dx / len * SEAM_INFLATE; a0y = v0[1] + dy / len * SEAM_INFLATE; }
        dx = v1[0] - cx; dy = v1[1] - cy;
        len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) { a1x = v1[0]; a1y = v1[1]; } else {
            a1x = v1[0] + dx / len * SEAM_INFLATE; a1y = v1[1] + dy / len * SEAM_INFLATE; }
        dx = v2[0] - cx; dy = v2[1] - cy;
        len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) { a2x = v2[0]; a2y = v2[1]; } else {
            a2x = v2[0] + dx / len * SEAM_INFLATE; a2y = v2[1] + dy / len * SEAM_INFLATE; }
        dx = v3[0] - cx; dy = v3[1] - cy;
        len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) { a3x = v3[0]; a3y = v3[1]; } else {
            a3x = v3[0] + dx / len * SEAM_INFLATE; a3y = v3[1] + dy / len * SEAM_INFLATE; }

        sr.triangle(a0x, a0y, a1x, a1y, a2x, a2y);
        sr.triangle(a0x, a0y, a2x, a2y, a3x, a3y);
    }

    @Override
    protected void drawHoverHighlight(ShapeRenderer sr) {
        // 已在 drawBar 中处理
    }

    @Override
    protected void updateHover() {
        if (!hoverEnabled || hoverLocalX < 0) {
            hoverCat = -1; hoverSeries = -1;
            hoverBarTopY = -1;
            hoverSeriesIdx = -1; hoverPointIdx = -1;
            return;
        }
        hoverCat = -1; hoverSeries = -1;
        hoverBarTopY = -1;
        for (float[] r : barRects) {
            if (hoverLocalX >= r[2] && hoverLocalX <= r[2] + r[4]
                    && hoverLocalY >= r[3] && hoverLocalY <= r[3] + r[5]) {
                hoverCat = (int) r[0];
                hoverSeries = (int) r[1];
                hoverSeriesIdx = hoverSeries;
                hoverPointIdx = hoverCat;
                // 柱顶 sy（正面矩形顶边）；tooltip 会锚定到此处上方
                hoverBarTopY = r[3] + r[5];
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
        String[] lines = new String[]{seriesLabel, catLabel + " = " + fmt(v)};
        Color accent = s.color != null ? s.color : Color.GRAY;

        // 测量 tooltip 框尺寸（同基类 drawTooltipBox）
        float maxW = 0;
        for (String l : lines) {
            glyphLayout.setText(font, l);
            if (glyphLayout.width > maxW) maxW = glyphLayout.width;
        }
        float boxW = maxW + 16;
        float boxH = lines.length * 18 + 10;

        // 锚点：优先用柱顶 sy，tooltip 底边贴柱顶上方 4px（不压柱子颜色）；
        // 退回用鼠标位置（hoverBarTopY 无效时）。
        float anchorY = (hoverBarTopY > 0) ? hoverBarTopY : hoverLocalY;
        float ty = anchorY + boxH + 4;   // box 上沿 = 柱顶 + 4 + boxH
        // 上方空间不够 → 翻到柱顶下方
        if (ty > getHeight() - padTop - legendPadTop) {
            ty = anchorY - 4;   // box 上沿贴柱顶下方 4px
        }
        float tx = hoverLocalX + 14;
        if (tx + boxW > getWidth() - padRight) tx = hoverLocalX - boxW - 8;
        if (tx < padLeft + legendPadLeft) tx = padLeft + legendPadLeft;

        float oldColor = packColor(font.getColor());
        for (int i = 0; i < lines.length; i++) {
            if (i == 0) {
                font.setColor(accent.r, accent.g, accent.b, parentAlpha);
            } else {
                font.setColor(textColor.r, textColor.g, textColor.b, parentAlpha);
            }
            font.draw(batch, lines[i],
                    getX() + tx + 8,
                    getY() + ty - 9 - i * 18);
        }
        font.setColor(unpackColor(oldColor));
    }

    @Override
    protected void drawAxisLabels(Batch batch, float parentAlpha) {
        super.drawAxisLabels(batch, parentAlpha);
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

    // ========================= 工具 =========================

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

    /** 单根 3D 柱子的几何缓存（内部用，池化复用）。 */
    private static final class Bar3D {
        int cat;
        int series;
        Color color;
        boolean hovered;
        float frontX;
        float barW;
        float barH;
        float sortKey;
    }
}
