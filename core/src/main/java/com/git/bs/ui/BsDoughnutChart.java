package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Bootstrap 风格环形图（DoughnutChart）—— 中间空，可在中心显示总值/标签。
 *
 * <p>本质上是 {@link BsPieChart} 的强化版：默认 {@code donutHole=0.6}，
 * 中心绘制一个标签（如总数值、单位）。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsDoughnutChart chart = new BsDoughnutChart();
 * chart.setSize(360, 320);
 * chart.setSkinFont(skin);
 * chart.setCenterLabel("总计", "2460");
 * chart.setSlices(
 *     "前端", 40,
 *     "后端", 35,
 *     "运维", 15,
 *     "测试", 10
 * );
 * stage.addActor(chart);
 * }</pre>
 */
public class BsDoughnutChart extends BsPieChart {

    private String centerTitle;
    private String centerValue;

    public BsDoughnutChart() {
        super();
        setDonutHole(0.6f);   // 默认环形
    }

    /** 中心文字：title（小字） + value（大字）。 */
    public BsDoughnutChart setCenterLabel(String title, String value) {
        this.centerTitle = title;
        this.centerValue = value;
        return this;
    }

    public BsDoughnutChart setCenterTitle(String t) { this.centerTitle = t; return this; }
    public BsDoughnutChart setCenterValue(String v) { this.centerValue = v; return this; }

    @Override
    protected void drawAxisLabels(Batch batch, float parentAlpha) {
        super.drawAxisLabels(batch, parentAlpha);
        // 在中心空洞处绘制标签
        if (centerTitle == null && centerValue == null) return;
        float cx = lastPieCx;
        float cy = lastPieCy;
        float oldColor = packColor(font.getColor());
        // value（大字）
        if (centerValue != null) {
            font.setColor(textColor.r, textColor.g, textColor.b, parentAlpha);
            GlyphLayout glv = new GlyphLayout();
            glv.setText(font, centerValue);
            float yOffset = (centerTitle != null) ? -2 : glv.height / 2f;
            font.draw(batch, centerValue,
                    getX() + cx - glv.width / 2f,
                    getY() + cy + yOffset + glv.height / 2f);
        }
        // title（小字，灰色）
        if (centerTitle != null) {
            font.setColor(0x6C / 255f, 0x75 / 255f, 0x7D / 255f, parentAlpha);
            float oldScaleX = font.getScaleX();
            float oldScaleY = font.getScaleY();
            font.getData().setScale(0.85f);
            GlyphLayout glt = new GlyphLayout();
            glt.setText(font, centerTitle);
            float yOffset = (centerValue != null) ? 6 : 0;
            font.draw(batch, centerTitle,
                    getX() + cx - glt.width / 2f,
                    getY() + cy + yOffset);
            font.getData().setScale(oldScaleX, oldScaleY);   // 恢复
        }
        font.setColor(unpackColor(oldColor));
    }
}
