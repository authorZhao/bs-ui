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
