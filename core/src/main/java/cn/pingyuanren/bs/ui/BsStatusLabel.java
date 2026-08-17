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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * Bootstrap 风格状态标签：支持 primary/success/danger/warning 等 variant。
 *
 * <p><b>实现说明</b>：scene2d {@link Label} 渲染时实际颜色由 {@code LabelStyle.fontColor}
 * 决定（不是 Actor.getColor）。本类通过给每个实例创建独立的 LabelStyle（fontColor = variant 色）
 * 来确保颜色生效，而不是依赖 setColor（会被 style.fontColor 覆盖）。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsStatusLabel extends Label {

    public enum Variant { PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO }

    private Variant variant;

    public BsStatusLabel(CharSequence text, Skin skin) {
        this(text, skin, Variant.SECONDARY);
    }

    public BsStatusLabel(CharSequence text, Skin skin, Variant v) {
        super(text, new LabelStyle(skin.get(LabelStyle.class)));
        setVariant(v);
    }

    public void setVariant(Variant v) {
        this.variant = v;
        LabelStyle style = new LabelStyle(getStyle());
        style.fontColor = variantColor(BsUI.getSkin(), v);
        setStyle(style);
    }

    /** V2：variant 色从 skin Color 桶取。 */
    public static Color variantColor(Skin skin, Variant v) {
        switch (v) {
            case PRIMARY:   return BsPalette.PRIMARY.getMain();
            case SUCCESS:   return BsPalette.SUCCESS.getMain();
            case DANGER:    return BsPalette.DANGER.getMain();
            case WARNING:   return BsPalette.WARNING.getMain();
            case INFO:      return BsPalette.INFO.getMain();
            case SECONDARY:
            default:        return BsPalette.SECONDARY.getMain();
        }
    }

    public Variant getVariant() { return variant; }
}
