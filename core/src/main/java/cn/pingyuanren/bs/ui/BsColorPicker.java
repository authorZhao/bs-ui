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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Bootstrap 风格颜色选择器：色块按钮，点击弹出 {@link BsColorPickerPopup}。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsColorPicker picker = new BsColorPicker(skin);
 * picker.setColor(Color.valueOf("#0D6EFD"));
 * picker.setOnChange(c -> setStatus("R=" + (int)(c.r*255)));
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsColorPicker extends TextButton {

    private Color value = new Color(Color.WHITE);
    private final Skin skin;
    private Consumer<Color> onChange;
    private BsColorPickerPopup popup;

    public BsColorPicker(Skin skin) {
        super("", resolveStyle(skin));
        this.skin = skin;
        setSize(60, 28);
        // 默认白色（业务方调 setSelectedColor 覆盖）
        setSelectedColor(Color.WHITE);

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openPopup();
            }
        });
    }

    /**
     * 优先取 skin 桶里注册的 "bs-color-picker" style（BsSkinFactory 注册、可导出）；
     * 取不到时尝试从 bs-color-swatch-up 派生；再不行用 default TextButtonStyle 兜底。
     */
    private static TextButtonStyle resolveStyle(Skin skin) {
        try {
            if (skin.has("bs-color-picker", TextButtonStyle.class)) {
                // 复制一份，避免污染 skin 桶里的原 style（运行时改 style.up 不影响别的实例）
                TextButtonStyle src = skin.get("bs-color-picker", TextButtonStyle.class);
                TextButtonStyle copy = new TextButtonStyle(src);
                return copy;
            }
        } catch (Throwable ignored) {}
        // 兜底：尝试派生 swatch drawable 构造临时 style
        TextButtonStyle s = new TextButtonStyle();
        try {
            Drawable swatch = skin.getDrawable("bs-color-swatch-up");
            s.up = swatch;
            s.over = swatch;
            s.down = swatch;
        } catch (Throwable ignored) {}
        s.font = skin.has("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class)
                ? skin.getFont("default-font") : skin.getFont("default");
        return s;
    }

    /**
     * 设置当前色，同时刷新色块视觉。
     * <p>实现：从 skin 的 "bs-color-swatch-up"（白底圆角 NinePatchDrawable）派生 tint 后的副本，
     * 设置到自己的 style.up/over/down，让 TextButton 渲染时显示这个颜色。</p>
     * <p>注意：不能用 Actor.setColor，libgdx 的 TextButton 渲染时 actor.color 不会乘到 style.up drawable 上。</p>
     */
    public void setSelectedColor(Color c) {
        this.value = new Color(c);
        try {
            Drawable tinted = skin.newDrawable("bs-color-swatch-up", new Color(c));
            TextButtonStyle st = getStyle();
            st.up = tinted;
            st.over = tinted;
            st.down = tinted;
        } catch (Throwable t) {
            // 派生失败（极端兜底）退回 actor.color，至少色块对
            super.setColor(new Color(c));
        }
    }

    public Color getSelectedColor() { return new Color(value); }

    public void setOnChange(Consumer<Color> onChange) {
        this.onChange = onChange;
    }

    private void openPopup() {
        if (popup != null && popup.isOpen()) { popup.close(); return; }
        if (getStage() == null) return;
        popup = new BsColorPickerPopup(BsUI.getSkin());
        popup.setOnPick(c -> {
            setSelectedColor(c);
            if (onChange != null) {
                try { onChange.accept(new Color(c)); } catch (Throwable t) { log.warn("onChange error", t); }
            }
        });
        popup.show(getStage(), this, value);
    }
}
