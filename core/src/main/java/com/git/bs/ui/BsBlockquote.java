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
        Image border = new Image(BsUI.getSkin().newDrawable("white", BsTheme.colorOf("primary")));
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
