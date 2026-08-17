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
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * Bootstrap 风格链接按钮：透明背景，主色（蓝）文字，hover 时字色加深。
 *
 * <p>支持可选 icon（用 {@link #setIcon(Drawable)}），常用于导航/侧边栏菜单项。</p>
 *
 * <pre>{@code
 * BsLink link = new BsLink("忘记密码？", skin);
 * link.setIcon(BsIcon.get("person"));
 * link.addListener(new ClickListener() { ... });
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsLink extends TextButton {

    private Image iconImage;
    private float iconW = 16, iconH = 16;
    private float iconTextGap = 4;

    public BsLink(String text, Skin skin) {
        super(text, skin, "bs-link");
    }

    /** 自定义链接颜色（覆盖 style 的 fontColor）。 */
    public void setColor(Color color) {
        TextButtonStyle style = new TextButtonStyle(getStyle());
        style.fontColor = new Color(color);
        Color hover = darker(color, 0.1f);
        style.overFontColor = hover;
        style.downFontColor = hover;
        setStyle(style);
    }

    /** 在文字前加图标。null 移除。 */
    public BsLink setIcon(Drawable icon) {
        if (icon == null) {
            if (iconImage != null) {
                iconImage.remove();
                iconImage = null;
            }
            return this;
        }
        if (iconImage == null) {
            iconImage = new Image(icon);
            iconImage.setScaling(Scaling.fit);
        } else {
            iconImage.setDrawable(icon);
        }
        iconImage.setSize(iconW, iconH);
        relayoutWithIcon();
        return this;
    }

    public BsLink setIconSize(float w, float h) {
        this.iconW = w;
        this.iconH = h;
        if (iconImage != null) {
            iconImage.setSize(w, h);
            relayoutWithIcon();
        }
        return this;
    }

    private void relayoutWithIcon() {
        if (iconImage == null) return;
        Label label = getLabel();
        clearChildren();
        add(iconImage).size(iconW, iconH).padRight(iconTextGap);
        add(label);
    }

    private static Color darker(Color c, float amount) {
        return new Color(
                Math.max(0, c.r - amount),
                Math.max(0, c.g - amount),
                Math.max(0, c.b - amount),
                c.a);
    }
}
