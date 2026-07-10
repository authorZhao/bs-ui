package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;

/**
 * Bootstrap 风格图表基类（Charts）—— 抽象层。
 *
 * <p>提供：</p>
 * <ul>
 *   <li><b>数据模型</b>：{@link Series}（一条数据系列）+ {@link Point}（二维数值点）</li>
 *   <li><b>坐标映射</b>：数据坐标 ⇄ 屏幕坐标；坐标轴刻度线 + 数值标签（用 BitmapFont 渲染）</li>
 *   <li><b>图例 Legend</b>：系列名 + 颜色块，位置可配（TOP / BOTTOM / LEFT / RIGHT / NONE），
 *       点击可切换该系列显隐</li>
 *   <li><b>Hover tooltip</b>：鼠标 hover 时在数据点旁绘制坐标/数值（折线/柱状）或百分比（饼图）</li>
 *   <li><b>系列显隐 + 点击隔离</b>：{@code hidden} BitSet 记录隐藏的系列；
 *       折线/柱状支持单击只显示该系列，Shift 多选做对比</li>
 * </ul>
 *
 * <p><b>draw 流程</b>：因 ShapeRenderer 不能渲染文字，draw 改为：
 * <pre>
 *   1. batch.end() → shape 画图形（轴/线/柱/饼）
 *   2. batch.begin() → 画坐标轴标签 + 图例文字 + tooltip 文字
 * </pre>
 * 子类只重写 {@link #drawChart(ShapeRenderer)} 和 {@link #drawLegendBadge}，
 * 轴/图例框架由基类处理。</p>
 */
@Slf4j
public abstract class BsChart extends Actor {

    // ========================= 数据模型 =========================

    /** 数据点：x/y 二维数值。 */
    public static class Point {
        public final float x, y;
        public Point(float x, float y) { this.x = x; this.y = y; }
        @Override public String toString() { return "(" + fmt(x) + "," + fmt(y) + ")"; }
    }

    /** 一条数据系列：label + 多个点 + 颜色。 */
    public static class Series {
        public String label;
        public List<Point> points;
        public Color color;
        public Series(String label, List<Point> points) {
            this.label = label; this.points = points;
        }
        public Series(String label, List<Point> points, Color color) {
            this.label = label; this.points = points; this.color = color;
        }
    }

    /**
     * 默认 6 色调色板（与 BsPalette 一致）。每次调用从 skin 取当前主题色，
     * 切换主题后新绘制的图表会自动用新色。
     * V2：必须传 skin（颜色存在 skin Color 桶）。
     */
    protected static Color[] defaultPalette(Skin skin) {
        return new Color[]{
                BsPalette.PRIMARY.getMain(),
                BsPalette.DANGER.getMain(),
                BsPalette.SUCCESS.getMain(),
                BsPalette.WARNING.getMain(),
                BsPalette.INFO.getMain(),
                BsPalette.SECONDARY.getMain(),
        };
    }

    /**
     * 兼容字段：类加载时取一次默认主题色（通常是浅色主题）。
     * V2：颜色存在 skin Color 桶，static 字段无法访问 skin，已删除。
     * 请使用 {@link #defaultPalette(Skin)} 每次取最新主题色。
     */

    /** BsChart 自身保存的 skin（V2：颜色从此取），由 setSkinFont 初始化。 */
    protected Skin skin;

    /** 当前 skin 对应的调色板缓存（setSkinFont 时刷新，避免每帧重建）。 */
    protected Color[] paletteCache = new Color[]{Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.GRAY};

    /** 取第 idx 个默认调色板色（已 setSkinFont 后可用）。 */
    protected Color palette(int idx) {
        return paletteCache[idx % paletteCache.length];
    }

    // ========================= 状态字段 =========================

    protected final List<Series> seriesList = new ArrayList<>();
    /** 隐藏的系列索引集合（图例点击切换 + 点击隔离时使用）。 */
    protected final BitSet hidden = new BitSet();

    protected float padLeft = 48;
    protected float padRight = 16;
    protected float padTop = 16;
    protected float padBottom = 32;
    /** 数据坐标系 min/max（recomputeBounds 计算）。 */
    protected float minX, maxX, minY, maxY;
    protected boolean axesVisible = true;
    protected boolean gridVisible = true;
    /** Y/X 轴刻度等分数（默认 5，小图可调小避免密集重叠）。 */
    protected int yTickCount = 5;
    protected int xTickCount = 5;
    /** V2：颜色存放在 skin Color 桶，字段初始化时无法访问 skin，先 null，draw 时按需从 skin 取。 */
    protected Color axisColor;
    protected Color gridColor;
    protected Color textColor;

    /** 字体（用于轴刻度 + 图例 + tooltip）。 */
    protected BitmapFont font;
    /** 子类可访问的 GlyphLayout（避免重复 new）。 */
    protected final GlyphLayout glyphLayout = new GlyphLayout();

    /** 图例位置。 */
    public enum LegendPlacement { TOP, BOTTOM, LEFT, RIGHT, NONE }
    protected LegendPlacement legendPlacement = LegendPlacement.TOP;
    protected boolean legendVisible = true;
    /** 图例区域（相对 actor 局部坐标），子类绘制时让出。 */
    protected float legendPadTop = 0, legendPadBottom = 0, legendPadLeft = 0, legendPadRight = 0;
    /**
     * 图例项命中矩形缓存（局部坐标）：每个 entry = {seriesIdx, x, y, w, h}。
     * drawLegend 填充，hitTestLegend 消费，避免估算偏差导致点击不准。
     */
    protected final List<float[]> legendRects = new ArrayList<>();

    /** Hover tooltip 开关。 */
    protected boolean hoverEnabled = true;
    /** 当前 hover 的系列索引与点索引（-1 = 无 hover）。 */
    protected int hoverSeriesIdx = -1;
    protected int hoverPointIdx = -1;
    /** 鼠标在 actor 局部坐标系下的位置。 */
    protected float hoverLocalX = -1, hoverLocalY = -1;

    /** 点击隔离是否启用（折线/柱状）：true 时单击只显示该系列，Shift 多选对比。 */
    protected boolean clickToIsolate = false;

    /** 本实例 ShapeRenderer（构造时取 {@link BsUI#shapeRenderer()} 全局，draw 用字段避免每帧方法调用；可用 {@link #setShapeRenderer} 自定义）。 */
    protected ShapeRenderer renderer;

    /** 全局 ShapeRenderer 访问（兼容旧调用，返回 {@link BsUI#shapeRenderer()}）。 */
    protected static ShapeRenderer sr() {
        return BsUI.shapeRenderer();
    }

    /** 自定义 ShapeRenderer（覆盖默认全局实例）。 */
    public BsChart setShapeRenderer(ShapeRenderer custom) {
        this.renderer = custom;
        return this;
    }

    /** 自定义字体（覆盖构造时从 skin 取的 default）。 */
    public BsChart setFont(BitmapFont f) {
        if (f != null) this.font = f;
        return this;
    }

    // ========================= 构造 =========================

    protected BsChart() {
        // 默认从全局 skin 取 default 字体（切主题是整体重建，构造时 skin 必已就绪），
        // 不 new BitmapFont 避免每个图表实例泄漏 native 字体内存；后续 setSkinFont 可换。
        try { this.font = BsUI.getSkin().getFont("default"); } catch (Throwable ignored) {}
        this.renderer = BsUI.shapeRenderer();   // 全局共享 SR（不每组件 static 单例）
        // hover 监听
        addListener(new InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                hoverLocalX = x;
                hoverLocalY = y;
                updateHover();
                return false;
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                hoverLocalX = x; hoverLocalY = y;
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                hoverLocalX = -1; hoverLocalY = -1;
                hoverSeriesIdx = -1; hoverPointIdx = -1;
            }
        });
    }

    /** 从 skin 设置字体（建议调用）。V2：同时缓存 skin 用于颜色查询。 */
    public BsChart setSkinFont(Skin skin) {
        this.skin = skin;
        try {
            if (skin.has("default", BitmapFont.class)) {
                this.font = skin.getFont("default");
            }
        } catch (Throwable ignored) {}
        // 刷新颜色字段（主题切换后可再次调用此方法刷新）
        if (skin != null) {
            axisColor = BsTheme.tm();
            gridColor = BsTheme.bh();
            textColor = BsTheme.ts();
            paletteCache = defaultPalette(skin);
        }
        return this;
    }

    // ========================= 数据设置 =========================

    public BsChart setData(List<Point> points) {
        seriesList.clear();
        seriesList.add(new Series(null, points, defaultPalette(skin)[0]));
        hidden.clear();
        recomputeBounds();
        return this;
    }

    public BsChart setMultiSeries(List<Series> series) {
        seriesList.clear();
        Color[] palette = defaultPalette(skin);
        for (int i = 0; i < series.size(); i++) {
            Series s = series.get(i);
            if (s.color == null) s.color = palette[i % palette.length];
            seriesList.add(s);
        }
        hidden.clear();
        recomputeBounds();
        return this;
    }

    /** 子类可重写：饼图不需要坐标映射。 */
    protected void recomputeBounds() {
        minX = Float.POSITIVE_INFINITY; maxX = Float.NEGATIVE_INFINITY;
        minY = Float.POSITIVE_INFINITY; maxY = Float.NEGATIVE_INFINITY;
        for (Series s : seriesList) {
            for (Point p : s.points) {
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }
        }
        if (minX == Float.POSITIVE_INFINITY) { minX = 0; maxX = 1; }
        if (minY == Float.POSITIVE_INFINITY) { minY = 0; maxY = 1; }
        if (minY > 0) minY = 0;
        if (maxY == minY) maxY = minY + 1;
        maxY += (maxY - minY) * 0.1f;
    }

    // ========================= 坐标映射 =========================

    protected final float toScreenX(float x) {
        float plotW = getWidth() - padLeft - padRight - legendPadLeft - legendPadRight;
        if (maxX == minX) return padLeft + legendPadLeft;
        return padLeft + legendPadLeft + (x - minX) / (maxX - minX) * plotW;
    }

    protected final float toScreenY(float y) {
        float plotH = getHeight() - padTop - padBottom - legendPadTop - legendPadBottom;
        if (maxY == minY) return padBottom + legendPadBottom;
        return padBottom + legendPadBottom + (y - minY) / (maxY - minY) * plotH;
    }

    // ========================= 配置 =========================

    public BsChart setPadding(float left, float right, float top, float bottom) {
        this.padLeft = left; this.padRight = right; this.padTop = top; this.padBottom = bottom;
        return this;
    }
    public BsChart setAxesVisible(boolean v) { this.axesVisible = v; return this; }
    public BsChart setGridVisible(boolean v) { this.gridVisible = v; return this; }
    /** Y 轴刻度等分数（默认 5，小图设 2 避免密集重叠看不清）。 */
    public BsChart setYTickCount(int n) { this.yTickCount = Math.max(1, n); return this; }
    /** X 轴刻度等分数。 */
    public BsChart setXTickCount(int n) { this.xTickCount = Math.max(1, n); return this; }
    public BsChart setAxisColor(Color c) { this.axisColor = c; return this; }
    public BsChart setGridColor(Color c) { this.gridColor = c; return this; }
    public BsChart setTextColor(Color c) { this.textColor = c; return this; }

    public BsChart setLegendPlacement(LegendPlacement p) { this.legendPlacement = p; return this; }
    public BsChart setLegendVisible(boolean v) { this.legendVisible = v; return this; }

    public BsChart setHoverEnabled(boolean v) { this.hoverEnabled = v; return this; }

    /** 折线/柱状：开启后单击系列只显示该系列，Shift+click 多选对比。 */
    public BsChart setClickToIsolate(boolean v) { this.clickToIsolate = v; return this; }

    /** 切换系列显隐（图例点击）。 */
    public void toggleSeries(int idx) {
        if (idx < 0 || idx >= seriesList.size()) return;
        hidden.flip(idx);
    }

    /** 单击隔离模式：把所有非选中系列设为隐藏。Shift 时多选保留。 */
    public void isolateSeries(int idx, boolean additive) {
        if (idx < 0 || idx >= seriesList.size()) return;
        if (!additive) hidden.set(0, seriesList.size());   // 先全隐藏
        hidden.flip(idx);   // 当前 idx 设为可见
    }

    public boolean isSeriesHidden(int idx) { return hidden.get(idx); }

    public List<Series> getSeriesList() { return seriesList; }

    // ========================= draw 流程 =========================

    @Override
    public final void draw(Batch batch, float parentAlpha) {
        // 1. 计算图例预留空间
        computeLegendPadding();

        // 2. shape 阶段：画图形（轴、网格、柱、线、饼）
        batch.end();
        try {
            ShapeRenderer sr = this.renderer;   // 用字段，不每帧调 sr()
            sr.setProjectionMatrix(batch.getProjectionMatrix());
            sr.setTransformMatrix(batch.getTransformMatrix());
            sr.setColor(1, 1, 1, parentAlpha);
            sr.begin(ShapeType.Filled);
            try {
                sr.translate(getX(), getY(), 0);
                drawAxesAndGrid(sr);
                drawChart(sr);
                // shape 阶段也画 hover 高亮（数据点外圈、饼图扇形高亮）
                if (hoverEnabled) drawHoverHighlight(sr);
            } finally {
                sr.identity();
                sr.end();
            }
        } finally {
            batch.begin();
        }

        // 3. batch 阶段：画文字（轴刻度、图例、tooltip）
        drawTextOverlay(batch, parentAlpha);
    }

    /** 计算图例占用的空间（legendPadTop/Bottom/Left/Right）。 */
    protected void computeLegendPadding() {
        legendPadTop = legendPadBottom = legendPadLeft = legendPadRight = 0;
        if (!legendVisible || legendPlacement == LegendPlacement.NONE) return;
        if (seriesList.isEmpty()) return;
        float legendSize = 24f;   // 图例区域估算大小
        switch (legendPlacement) {
            case TOP:    legendPadTop = legendSize; break;
            case BOTTOM: legendPadBottom = legendSize; break;
            case LEFT:   legendPadLeft = legendSize + 60; break;
            case RIGHT:  legendPadRight = legendSize + 60; break;
        }
    }

    /** 画坐标轴 + 网格（基类提供水平网格 + 主轴默认实现，子类可重写补充）。 */
    protected void drawAxesAndGrid(ShapeRenderer sr) {
        if (!axesVisible) return;
        // 默认不画，由子类（折线/柱状）按需重写
    }

    /** 子类实现：画图表主体（线、柱、饼）。 */
    protected abstract void drawChart(ShapeRenderer sr);

    /** 子类实现：在 hover 数据点画高亮（外圈/加粗）。 */
    protected void drawHoverHighlight(ShapeRenderer sr) {
        // 默认无操作，子类按需重写
    }

    /** 画文字（轴标签 + 图例 + tooltip），子类可扩展。 */
    protected void drawTextOverlay(Batch batch, float parentAlpha) {
        // 轴标签
        if (axesVisible) drawAxisLabels(batch, parentAlpha);
        // 图例
        if (legendVisible && legendPlacement != LegendPlacement.NONE) drawLegend(batch, parentAlpha);
        // tooltip
        if (hoverEnabled && hoverSeriesIdx >= 0) drawTooltip(batch, parentAlpha);
    }

    /** 画 X/Y 轴刻度文字（折线/柱状用）。饼图重写为空。 */
    protected void drawAxisLabels(Batch batch, float parentAlpha) {
        if (!axesVisible) return;
        float oldColor = packColor(font.getColor());
        font.setColor(textColor.r, textColor.g, textColor.b, textColor.a * parentAlpha);

        float plotH = getHeight() - padTop - padBottom - legendPadTop - legendPadBottom;
        float baseY = padBottom + legendPadBottom;
        // Y 轴：yTickCount 等分刻度，标签靠左
        for (int i = 0; i <= yTickCount; i++) {
            float v = minY + (maxY - minY) * i / (float) yTickCount;
            float sy = baseY + plotH * i / (float) yTickCount;
            String text = fmt(v);
            glyphLayout.setText(font, text);
            font.draw(batch, text,
                    getX() + padLeft + legendPadLeft - glyphLayout.width - 6,
                    getY() + sy + glyphLayout.height / 2f);
        }
        // X 轴：xTickCount 等分刻度，标签靠下
        float plotW = getWidth() - padLeft - padRight - legendPadLeft - legendPadRight;
        for (int i = 0; i <= xTickCount; i++) {
            float v = minX + (maxX - minX) * i / (float) xTickCount;
            float sx = padLeft + legendPadLeft + plotW * i / (float) xTickCount;
            String text = fmt(v);
            glyphLayout.setText(font, text);
            font.draw(batch, text,
                    getX() + sx - glyphLayout.width / 2f,
                    getY() + baseY - 6);
        }
        font.setColor(unpackColor(oldColor));
    }

    /**
     * 画图例（颜色块 + 标签，<b>标签文字用系列颜色</b>让线条/扇形与文字一一对应）。
     * 同时填充 {@link #legendRects} 缓存供 {@link #hitTestLegend} 精确命中。
     * 饼图可重写以附加百分比。
     */
    protected void drawLegend(Batch batch, float parentAlpha) {
        drawLegend(batch, parentAlpha, seriesIdx -> {
            Series s = seriesList.get(seriesIdx);
            return (s.label == null ? "系列" + (seriesIdx + 1) : s.label);
        });
    }

    /**
     * 通用图例绘制（子类可自定义 label 提供器，例如饼图追加百分比）。
     * @param labelProvider 输入 seriesIdx，返回该行显示的文本
     */
    protected void drawLegend(Batch batch, float parentAlpha, java.util.function.Function<Integer, String> labelProvider) {
        if (seriesList.isEmpty()) return;
        legendRects.clear();
        // 1. 真实测量每条目宽度（用 glyphLayout，不是估算）
        float gap = 18f;
        int n = seriesList.size();
        float[] widths = new float[n];
        float blockW = measureBlockWidth();
        float totalW = 0;
        for (int i = 0; i < n; i++) {
            glyphLayout.setText(font, labelProvider.apply(i));
            widths[i] = glyphLayout.width;
            totalW += widths[i] + blockW + 4 /*block-text gap*/ + gap;
        }
        totalW -= gap;

        boolean horizontal = (legendPlacement == LegendPlacement.TOP || legendPlacement == LegendPlacement.BOTTOM);
        float cx = getWidth() / 2f;
        // 横向：起始 x 居中；纵向：起始 x 靠边
        float startX = horizontal ? cx - totalW / 2f
                : (legendPlacement == LegendPlacement.LEFT ? 6 : getWidth() - measureLegendColWidth() - 6);
        // 横向：固定 y 顶部/底部；纵向：从顶到底每行 18px
        float startY = (legendPlacement == LegendPlacement.TOP) ? getHeight() - 8
                : (legendPlacement == LegendPlacement.BOTTOM) ? 12
                : getHeight() - 16;

        float oldColor = packColor(font.getColor());
        float x = startX;
        float y = startY;
        for (int i = 0; i < n; i++) {
            Series s = seriesList.get(i);
            Color c = s.color != null ? s.color : Color.GRAY;
            boolean isHidden = hidden.get(i);
            float alpha = (isHidden ? 0.35f : 1f) * parentAlpha;
            String label = labelProvider.apply(i);

            // ■ 色块（用系列色）
            String block = "■";
            font.setColor(c.r, c.g, c.b, alpha);
            glyphLayout.setText(font, block);
            float blockDrawW = glyphLayout.width;
            font.draw(batch, block, getX() + x, getY() + y);

            // 文字（也用系列色，让"红线条"对应"红色标签"）
            font.setColor(c.r, c.g, c.b, alpha);
            glyphLayout.setText(font, label);
            float textX = x + blockDrawW + 4;
            font.draw(batch, label, getX() + textX, getY() + y);

            // 记录命中矩形：[seriesIdx, x, y - textHeight, totalItemW, lineHeight]
            // libgdx 字体 y 是 baseline，文字大致占 lineHeight 高度（向上延伸）
            float itemW = blockDrawW + 4 + widths[i];
            float lineHeight = font.getLineHeight();
            legendRects.add(new float[]{i, x, y - 4, itemW, lineHeight + 4});

            if (horizontal) {
                x += itemW + gap;
            } else {
                y -= 18;
                x = startX;   // 纵向每行都从 startX 开始
            }
        }
        font.setColor(unpackColor(oldColor));
    }

    /** 测量 "■" 色块的宽度（依赖字体，可能因字号不同而变化）。 */
    private float measureBlockWidth() {
        glyphLayout.setText(font, "■");
        return glyphLayout.width;
    }

    /** 纵向布局（LEFT/RIGHT）时图例列的宽度估算（最长标签 + block）。 */
    protected float measureLegendColWidth() {
        float max = 0;
        for (int i = 0; i < seriesList.size(); i++) {
            String label = seriesList.get(i).label;
            if (label == null) label = "系列" + (i + 1);
            glyphLayout.setText(font, label);
            if (glyphLayout.width > max) max = glyphLayout.width;
        }
        return max + measureBlockWidth() + 10;
    }

    /**
     * 图例点击命中测试（基于 {@link #legendRects} 精确匹配，避免估算偏差）。
     * 所有子类共用此方法，无需各自实现。
     * @return 命中的 seriesIdx，未命中返回 -1
     */
    protected int hitTestLegend(float localX, float localY) {
        if (!legendVisible || legendPlacement == LegendPlacement.NONE) return -1;
        for (float[] r : legendRects) {
            float rx = r[1], ry = r[2], rw = r[3], rh = r[4];
            if (localX >= rx && localX <= rx + rw && localY >= ry && localY <= ry + rh) {
                return (int) r[0];
            }
        }
        return -1;
    }


    /** 子类重写：画 hover tooltip（坐标 / 数值 / 百分比）。 */
    protected void drawTooltip(Batch batch, float parentAlpha) {
        if (hoverSeriesIdx < 0 || hoverPointIdx < 0) return;
        if (hoverSeriesIdx >= seriesList.size()) return;
        Series s = seriesList.get(hoverSeriesIdx);
        if (hoverPointIdx >= s.points.size()) return;
        Point p = s.points.get(hoverPointIdx);
        String line1 = (s.label == null ? "系列" + (hoverSeriesIdx + 1) : s.label);
        String line2 = "x=" + fmt(p.x) + "  y=" + fmt(p.y);
        drawTooltipBox(batch, parentAlpha,
                new String[]{line1, line2},
                s.color != null ? s.color : Color.GRAY);
    }

    /** 绘制 tooltip 框（白底 + 边框 + 文字）。 */
    protected void drawTooltipBox(Batch batch, float parentAlpha, String[] lines, Color accent) {
        // 计算最大宽度
        float maxW = 0;
        for (String l : lines) {
            glyphLayout.setText(font, l);
            if (glyphLayout.width > maxW) maxW = glyphLayout.width;
        }
        float boxW = maxW + 16;
        float boxH = lines.length * 18 + 10;

        // tooltip 位置（hoverLocalX/Y + offset）
        float tx = hoverLocalX + 14;
        float ty = hoverLocalY + 14;
        // 防止超出右边/上边
        if (tx + boxW > getWidth()) tx = hoverLocalX - boxW - 8;
        if (ty + boxH > getHeight()) ty = hoverLocalY - boxH - 8;

        // 文字（背景框在 shape 阶段画了；这里直接画文字）
        // 改为：在 batch 中无法画框，简化用文字 + 边距
        float oldColor = packColor(font.getColor());
        for (int i = 0; i < lines.length; i++) {
            if (i == 0) {
                font.setColor(accent.r, accent.g, accent.b, parentAlpha);
            } else {
                font.setColor(textColor.r, textColor.g, textColor.b, parentAlpha);
            }
            font.draw(batch, lines[i],
                    getX() + tx + 8,
                    getY() + ty + boxH - 9 - i * 18);
        }
        font.setColor(unpackColor(oldColor));
    }

    /** 在 shape 阶段画 tooltip 背景框（默认实现：白底 + accent 边）。 */
    protected void drawTooltipBackground(ShapeRenderer sr) {
        if (hoverSeriesIdx < 0) return;
        // 默认不画，子类可调用
    }

    // ========================= hover 检测 =========================

    /** 子类重写：根据 hoverLocalX/Y 更新 hoverSeriesIdx/hoverPointIdx。 */
    protected void updateHover() {
        // 默认：无操作，子类按需重写
    }

    // ========================= 工具 =========================

    /** 数字格式化：整数显示整数，小数保留 1~2 位。 */
    protected static String fmt(float v) {
        if (Math.abs(v - Math.round(v)) < 0.001f) return String.valueOf(Math.round(v));
        if (Math.abs(v) >= 100) return String.format("%.1f", v);
        return String.format("%.2f", v);
    }

    public static List<Point> points(float... xyPairs) {
        if (xyPairs.length % 2 != 0) throw new IllegalArgumentException("必须 x,y 成对");
        List<Point> list = new ArrayList<>(xyPairs.length / 2);
        for (int i = 0; i < xyPairs.length; i += 2) {
            list.add(new Point(xyPairs[i], xyPairs[i + 1]));
        }
        return list;
    }

    public static List<Point> pointsOfY(float... ys) {
        List<Point> list = new ArrayList<>(ys.length);
        for (int i = 0; i < ys.length; i++) list.add(new Point(i, ys[i]));
        return list;
    }

    /** 把 Color 压成 float（保存/恢复用）。 */
    protected static float packColor(Color c) {
        return Float.intBitsToFloat(
                ((int) (c.a * 255) << 24) | ((int) (c.r * 255) << 16)
                        | ((int) (c.g * 255) << 8) | (int) (c.b * 255));
    }
    protected static Color unpackColor(float packed) {
        int i = Float.floatToRawIntBits(packed);
        return new Color(((i >> 16) & 0xFF) / 255f, ((i >> 8) & 0xFF) / 255f,
                (i & 0xFF) / 255f, ((i >> 24) & 0xFF) / 255f);
    }
}
