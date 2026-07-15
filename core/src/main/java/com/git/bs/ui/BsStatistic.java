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
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Bootstrap 风格数字统计卡片（Statistic）—— 管理后台首页标配。
 *
 * <p>结构：</p>
 * <pre>
 * ┌──────────────────┐
 * │  [icon]  今日营收 │  ← 标题行（可选 icon）
 * │          ¥12,345 │  ← 大数字
 * │          ↑ 12.5% │  ← 趋势（绿色 ↑ / 红色 ↓）
 * └──────────────────┘
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsStatistic stat = new BsStatistic(skin)
 *         .title("今日营收")
 *         .icon(BsIcon.get("currency-yen"))
 *         .value("¥12,345")
 *         .trend(12.5f);   // 正 = 绿↑，负 = 红↓
 * stage.addActor(stat);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsStatistic extends Table {

    private Label titleLabel;
    private Label valueLabel;
    private Label trendLabel;
    private Table titleRow;
    private Drawable iconDrawable;

    public BsStatistic(Skin skin) {
        setBackground(skin.getDrawable("bs-window-bg"));
        pad(16, 20, 16, 20);
        left().top();
        defaults().growX().left();

        titleRow = new Table();
        titleRow.left();
        titleLabel = new Label("", skin);
        titleLabel.setColor(BsTheme.ts());
        titleLabel.setFontScale(0.95f);

        valueLabel = new Label("", skin);
        valueLabel.setColor(BsTheme.tp());
        valueLabel.setFontScale(1.8f);

        trendLabel = new Label("", skin);
        trendLabel.setFontScale(0.95f);

        rebuild();
    }

    private void rebuild() {
        clearChildren();
        // 标题行
        titleRow.clearChildren();
        titleRow.left();
        if (iconDrawable != null) {
            com.badlogic.gdx.scenes.scene2d.ui.Image img = new com.badlogic.gdx.scenes.scene2d.ui.Image(iconDrawable);
            img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            titleRow.add(img).size(20).padRight(8).left();
        }
        titleRow.add(titleLabel).left();
        add(titleRow).growX().left().row();
        // 数值
        add(valueLabel).growX().left().padTop(6).row();
        // 趋势
        add(trendLabel).growX().left().padTop(4);
    }

    public BsStatistic title(String t) {
        titleLabel.setText(t);
        return this;
    }

    public BsStatistic icon(Drawable d) {
        this.iconDrawable = d;
        rebuild();
        return this;
    }

    public BsStatistic value(String v) {
        valueLabel.setText(v);
        return this;
    }

    /**
     * 自定义数值字体（如大屏的运行时大数字 TTF），不传或 null 则沿用 skin 默认。
     * 用于 KPI 大字号场景 —— app 层用 FreeType 生成纯 ASCII 大字体注入，避免为 CJK 大字号付内存代价。
     */
    public BsStatistic valueFont(BitmapFont f) {
        if (f == null) return this;
        Label.LabelStyle ls = new Label.LabelStyle(valueLabel.getStyle());
        ls.font = f;
        valueLabel.setStyle(ls);
        return this;
    }

    /**
     * 趋势百分比。正值显示绿色 ↑，负值显示红色 ↓，0 不显示趋势。
     */
    public BsStatistic trend(float percent) {
        if (Math.abs(percent) < 0.01f) {
            trendLabel.setText("");
            return this;
        }
        String arrow = percent > 0 ? "↑" : "↓";
        String sign = percent > 0 ? "+" : "";
        trendLabel.setText(arrow + " " + sign + String.format("%.1f", percent) + "%");
        trendLabel.setColor(percent > 0
                ? BsPalette.SUCCESS.getMain()
                : BsPalette.DANGER.getMain());
        return this;
    }

    /** 趋势用自定义文字（不通过百分比），如 "活跃 5分钟前"。 */
    public BsStatistic trendText(String text, Color color) {
        trendLabel.setText(text);
        trendLabel.setColor(color);
        return this;
    }
}
