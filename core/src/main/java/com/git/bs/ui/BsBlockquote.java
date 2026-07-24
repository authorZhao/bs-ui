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

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;

/**
 * Bootstrap 风格引用块 —— 左侧 4px primary 色边框 + 左缩进 + 弱化（textSecondary）文字。
 *
 * <p>因位图字体无斜体变体，弱化靠 textSecondary 色 + 左边框表达，
 * 而不是 Bootstrap 原生的 italic；需要斜体可对内部文字调 {@link BsText#italic()}。</p>
 *
 * <pre>{@code
 * root.add(new BsBlockquote("这就是传说中的名言。")).growX().left().row();
 * // 大号引用（lg 字号）
 * root.add(new BsBlockquote("重要的话要大声说。", true)).growX().left().row();
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsBlockquote extends Table {

    private final BsText text;

    public BsBlockquote(CharSequence content) {
        this(content, false);
    }

    /** @param large true 用 lg 字号（更接近 Bootstrap lead 风格），false 跟随 skin 默认。 */
    public BsBlockquote(CharSequence content, boolean large) {
        left().top();
        text = new BsText(content, large ? BsText.Size.LG : BsText.Size.DEFAULT, BsText.Variant.SECONDARY);

        // 左侧 4px primary 色条（白底染色，stretch 拉满 cell 高度 = 文字高度）
        Image border = new Image(BsSkinFactory.drawableOf(BsTheme.colorOf("primary")));
        border.setScaling(Scaling.stretch);
        add(border).width(4f).fillY();

        add(text).padLeft(12f).padRight(8f).growX().left();
    }

    public BsBlockquote setContent(CharSequence c) {
        text.setText(c);
        return this;
    }

    public BsText getText() { return text; }
}
