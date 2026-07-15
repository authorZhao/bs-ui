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
 * @author authorZhao
 * @since 2026-07-16
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
