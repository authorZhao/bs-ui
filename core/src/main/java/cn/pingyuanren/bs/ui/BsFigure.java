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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * Bootstrap 5 风格图文（Figure）—— 图片 + 下方说明文字的组合。
 *
 * <p>结构：</p>
 * <pre>
 * ┌─────────────────────┐
 * │                     │
 * │       [图片]        │   ← 主图（可设圆角/缩放）
 * │                     │
 * ├─────────────────────┤
 * │  说明文字 caption   │   ← 图注（灰色小字）
 * └─────────────────────┘
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsFigure fig = new BsFigure(skin)
 *         .image(myDrawable)
 *         .imageSize(300, 200)
 *         .caption("图 1：这是产品示意图");
 * stage.addActor(fig);
 * }</pre>
 *
 * <p>实现：垂直 Table = [Image（fit 缩放）] + [Label（居中、wrap）]。
 * 用于插图展示、文档配图、相册缩略图等场景。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsFigure extends Table {

    private final Image image;
    private final Label captionLabel;
    private final Table imgWrap;

    public BsFigure(Skin skin) {
        defaults().growX();
        left().top();

        // 图片容器（带圆角白底，让无图时不空）
        imgWrap = new Table();
        imgWrap.setBackground(skin.getDrawable("bs-window-bg"));
        image = new Image();
        image.setScaling(Scaling.fit);
        imgWrap.add(image).center();
        add(imgWrap).growX().row();

        // 图注
        captionLabel = new Label("", skin);
        captionLabel.setColor(BsTheme.ts());
        captionLabel.setWrap(true);
        captionLabel.setAlignment(1);  // center
        add(captionLabel).growX().padTop(6).center();
    }

    /** 设置图片。null 则清空。 */
    public BsFigure image(Drawable d) {
        image.setDrawable(d);
        return this;
    }

    /** 设置图片尺寸（同时也是整个 figure 的宽度）。 */
    public BsFigure imageSize(float w, float h) {
        imgWrap.getCell(image).size(w, h);
        return this;
    }

    /** 设置图注文字。 */
    public BsFigure caption(String text) {
        captionLabel.setText(text == null ? "" : text);
        return this;
    }

    /** 图注颜色（默认灰色）。 */
    public BsFigure captionColor(Color c) {
        captionLabel.setColor(c);
        return this;
    }

    /** 图注字号（传烘焙字号 key，如 "font-sm"/"font-lg"/"font-xl"，不缩放避免发虚）。 */
    public BsFigure captionFont(String fontKey) {
        Label.LabelStyle s = new Label.LabelStyle(captionLabel.getStyle());
        s.font = BsUI.getSkin().getFont(fontKey);
        captionLabel.setStyle(s);
        return this;
    }

    /** 是否显示图片边框（默认有圆角白底）。 */
    public BsFigure bordered(boolean show) {
        imgWrap.setBackground(show ? BsUI.getSkin().getDrawable("bs-window-bg") : null);
        return this;
    }
}
