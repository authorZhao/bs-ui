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

/**
 * Bootstrap 风格段落正文 —— 默认跟随 skin 当前字体（{@link Size#DEFAULT}）、textPrimary 色。
 *
 * <p>位图字体的行高由字体本身决定，无法独立调整 line-height；
 * 段落之间的纵向间距建议用 {@link #MARGIN_BOTTOM}（外层布局 pad bottom）。</p>
 *
 * <pre>{@code
 * root.add(new BsParagraph("这是一段说明文字……")).growX()
 *     .padBottom(BsParagraph.MARGIN_BOTTOM).row();
 *
 * // 弱化/警示段落
 * new BsParagraph("该操作不可逆", Variant.DANGER);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsParagraph extends BsText {

    /** 段落建议下间距（px），复现 Bootstrap {@code <p>} 的 margin-bottom 节奏。 */
    public static final float MARGIN_BOTTOM = 12f;

    public BsParagraph(CharSequence text) {
        super(text);   // DEFAULT size，跟随 skin
    }

    /** 指定颜色变体（字号仍跟随 skin 默认）。 */
    public BsParagraph(CharSequence text, Variant variant) {
        super(text, Size.DEFAULT, variant);
    }

    /** 指定字号档（颜色为默认 textPrimary）。 */
    public BsParagraph(CharSequence text, Size size) {
        super(text, size, Variant.DEFAULT);
    }
}
