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
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

    // ---- 抗锯齿（超采样纹理缓存） ----
    /** true = 超采样 Pixmap 缓存纹理渲染（边缘平滑）；false = ShapeRenderer 直绘（旧路径）。 */
    private boolean antialias = true;
    /** 纵向图例的数值后缀（如耗时 "12.3s" / 步数 "38 步"）；null = 只显示百分比。 */
    private Function<Float, String> legendValueFormatter;
    private Texture pieTexture;
    private int pieTexW, pieTexH;
    private String pieCacheKey;

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

    /**
     * 抗锯齿开关（默认开）：开 = 饼图主体画进 2~3 倍超采样 Pixmap 缓存纹理，
     * 以 Linear 采样缩回显示，扇形/圆边缘平滑无锯齿；关 = ShapeRenderer 直绘（旧行为）。
     * 纹理按（尺寸/数据/hover/显隐）缓存，静止时零重绘开销。
     */
    public BsPieChart setAntialias(boolean v) {
        this.antialias = v;
        this.pieCacheKey = null;
        return this;
    }

    /** 纵向（LEFT/RIGHT）图例的数值后缀格式器：入参 slice 值，返回展示文本（如 "12.3s"）。 */
    public BsPieChart setLegendValueFormatter(Function<Float, String> formatter) {
        this.legendValueFormatter = formatter;
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
            float a1 = startAngleDeg;   // 本 slice 覆盖 [a1-sweep, a1]（CCW）
            startAngleDeg -= sweep;
            if (antialias) continue;   // AA 路径：几何进缓存纹理，batch 阶段绘制
            // hover 时扇形稍微外推（不影响 sliceRects 的稳定参数）
            boolean isHovered = (hoverSeriesIdx == i);
            float ox = 0, oy = 0;
            if (isHovered) {
                float midDeg = (float) Math.toRadians(a1 - sweep / 2f);
                ox = (float) Math.cos(midDeg) * 6;
                oy = (float) Math.sin(midDeg) * 6;
            }
            sr.setColor(c);
            sr.arc(cx + ox, cy + oy, r, a1 - sweep, sweep, Math.max(6, (int) (sweep / 4)));
        }
        if (antialias) {
            ensurePieTexture(total, size, cx, cy, r);
            return;
        }

        // 环形：挖空中心
        if (donutHole > 0) {
            sr.setColor(BsTheme.be());
            sr.circle(cx, cy, r * donutHole);
        }
    }

    // ========================= 抗锯齿：超采样纹理缓存 =========================

    /**
     * 饼图主体画进超采样 Pixmap（2~3×）→ Linear 纹理缓存 → drawTextOverlay 阶段整幅贴回。
     * ShapeRenderer 直绘无边缘 AA，小尺寸扇形锯齿明显；超采样 + 双线性缩回后边缘平滑。
     * 缓存键 = 尺寸/hover/显隐/数据（值+色），静止时零重绘；绘制在 GL 线程（draw 回调内）。
     */
    private void ensurePieTexture(float total, float size, float cx, float cy, float r) {
        String key = pieCacheKey();
        if (pieTexture != null && key.equals(pieCacheKey)) return;
        pieCacheKey = key;
        int w = Math.max(8, (int) Math.ceil(getWidth()));
        int h = Math.max(8, (int) Math.ceil(getHeight()));
        int ss = supersample(w, h);
        if (pieTexture != null && (pieTexW != w || pieTexH != h)) {
            pieTexture.dispose();
            pieTexture = null;
        }
        pieTexW = w;
        pieTexH = h;
        Pixmap pm = new Pixmap(w * ss, h * ss, Pixmap.Format.RGBA8888);
        // Pixmap y 轴向下：actor 局部 y 上向 → 纹理行翻转（py = pmH - localY*ss），角度取 -sin
        float pmCx = cx * ss, pmCy = h * ss - cy * ss;
        float startAngleDeg = 90f;
        for (int i = 0; i < slices.size(); i++) {
            if (hidden.get(i)) continue;
            Slice s = slices.get(i);
            float sweep = s.value / total * 360f;
            float a1 = startAngleDeg;
            startAngleDeg -= sweep;
            float ox = 0, oy = 0;
            if (hoverSeriesIdx == i) {   // hover 外推 6px（与直绘路径一致）
                float midDeg = (float) Math.toRadians(a1 - sweep / 2f);
                ox = (float) Math.cos(midDeg) * 6 * ss;
                oy = -(float) Math.sin(midDeg) * 6 * ss;   // y 翻转
            }
            pm.setColor(s.color != null ? s.color : Color.GRAY);
            fillWedge(pm, pmCx + ox, pmCy + oy, r * ss, a1 - sweep, a1);
        }
        if (donutHole > 0) {   // 环形：中心抠透明（背景透出，等价旧路径填底色）
            pm.setBlending(Pixmap.Blending.None);
            pm.setColor(0, 0, 0, 0);
            pm.fillCircle(Math.round(pmCx), Math.round(pmCy), Math.max(1, Math.round(r * donutHole * ss)));
            pm.setBlending(Pixmap.Blending.SourceOver);
        }
        if (pieTexture != null) pieTexture.dispose();
        pieTexture = new Texture(pm);
        pieTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
    }

    /** 缓存键：影响像素的全部输入（尺寸/数据/色/hover/显隐/环形）。 */
    private String pieCacheKey() {
        StringBuilder sb = new StringBuilder(64);
        sb.append((int) getWidth()).append('x').append((int) getHeight())
                .append('#').append(hoverSeriesIdx)
                .append('/').append(Float.floatToIntBits(donutHole))
                .append('/').append(hidden.hashCode());
        for (Slice s : slices) {
            sb.append('|').append(Float.floatToIntBits(s.value))
                    .append(',').append(s.color != null ? s.color.toIntBits() : 0);
        }
        return sb.toString();
    }

    /** 超采样倍率：小图 3×、大图 2×（总像素 ≈ ≤1.5M 封顶，GWT 堆友好）。 */
    private static int supersample(int w, int h) {
        int ss = 3;
        while (ss > 1 && (long) w * h * ss * ss > 1_500_000) ss--;
        return ss;
    }

    /** 2° 步进三角扇填充扇形（数学角 CCW，y 已翻转）。 */
    private static void fillWedge(Pixmap pm, float cx, float cy, float r, float a0, float a1) {
        for (float a = a0; a < a1; a += 2f) {
            float b = Math.min(a1, a + 2f);
            pm.fillTriangle(Math.round(cx), Math.round(cy),
                    Math.round(cx + r * cosDeg(a)), Math.round(cy - r * sinDeg(a)),
                    Math.round(cx + r * cosDeg(b)), Math.round(cy - r * sinDeg(b)));
        }
    }

    private static float cosDeg(float deg) {
        return (float) Math.cos(Math.toRadians(deg));
    }

    private static float sinDeg(float deg) {
        return (float) Math.sin(Math.toRadians(deg));
    }

    /** AA 路径：饼图纹理在文字层之前绘制（饼在图例/tooltip 之下）。 */
    @Override
    protected void drawTextOverlay(Batch batch, float parentAlpha) {
        if (antialias && pieTexture != null) {
            batch.setColor(1, 1, 1, parentAlpha);
            batch.draw(pieTexture, getX(), getY(), getWidth(), getHeight());
            batch.setColor(1, 1, 1, 1);
        }
        super.drawTextOverlay(batch, parentAlpha);
    }

    /** 从舞台移除（含 stage.clear/Stage.dispose 走 Group.clear → remove）时释放缓存纹理。 */
    @Override
    public boolean remove() {
        disposePieTexture();
        return super.remove();
    }

    /** 显式释放缓存纹理（remove 已覆盖常规路径，此方法兜底手动管理场景）。 */
    public void disposePieTexture() {
        if (pieTexture != null) {
            pieTexture.dispose();
            pieTexture = null;
        }
        pieCacheKey = null;
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
        // 值文本与图例同口径：有 legendValueFormatter 用它（如 3754ms → "3.754s"），否则裸数值
        String valTxt = legendValueFormatter != null
                ? legendValueFormatter.apply(s.value) : fmt(s.value);
        drawTooltipBox(batch, parentAlpha,
                new String[]{s.label, valTxt + "  (" + fmt(pct) + "%)"},
                s.color != null ? s.color : Color.GRAY);
    }

    /**
     * 纵向图例列宽：与 {@link #drawLegend} 的 labelProvider 同口径测量
     * （label + 百分比 + {@code legendValueFormatter} 后缀）。
     * 基类只量 label 会把图例估窄（"F2L" vs "F2L 40.7%  10.324s"），
     * 饼图按窄图例算直径 → 实际图例画出 actor 右边界外（0.3.10 修复）。
     */
    @Override
    protected float measureLegendColWidth() {
        float total = 0;
        for (int i = 0; i < slices.size(); i++) {
            if (!hidden.get(i)) total += slices.get(i).value;
        }
        total = Math.max(0.0001f, total);
        float max = 0;
        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            String txt = s.label + " " + fmt(s.value / total * 100) + "%";
            if (legendValueFormatter != null) txt += "  " + legendValueFormatter.apply(s.value);
            glyphLayout.setText(font, txt);
            if (glyphLayout.width > max) max = glyphLayout.width;
        }
        return max + measureBlockWidth() + 10;
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
            String txt = s.label + " " + fmt(s.value / totalFinal * 100) + "%";
            if (legendValueFormatter != null) txt += "  " + legendValueFormatter.apply(s.value);
            return txt;
        });
    }

    public List<Slice> getSlices() { return slices; }
}
