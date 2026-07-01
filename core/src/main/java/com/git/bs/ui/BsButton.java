package com.git.bs.ui;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * Bootstrap 5 风格按钮。支持 6 色变体 + outline + size + 可选 icon。
 * <p>所需 Skin 资源由 {@link BsSkinFactory} 提供：</p>
 * <ul>
 *   <li>{@code bs-btn-{color}}（实心）</li>
 *   <li>{@code bs-btn-outline-{color}}</li>
 * </ul>
 *
 * <p><b>Icon 用法</b>：用 {@link #setIcon(Drawable)} 在按钮文字前加图标，例如：
 * <pre>{@code
 * BsButton btn = new BsButton("设置", skin, Variant.PRIMARY, Style.SOLID, Size.MD);
 * btn.setIcon(BsIcon.get("gear"));   // 文字前显示齿轮图标
 * }</pre>
 * <p>icon 默认 16×16，可用 {@link #setIconSize(float, float)} 调整。</p>
 */
public class BsButton extends TextButton {

    public enum Variant { PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO, LIGHT, DARK }
    public enum Size { SM, MD, LG }
    public enum Style { SOLID, OUTLINE }

    private Image iconImage;
    private float iconW = 16, iconH = 16;
    private float iconTextGap = 4;
    private boolean iconLeft = true;  // true=icon 在文字左

    public BsButton(String text, Skin skin) {
        this(text, skin, Variant.PRIMARY, Style.SOLID, Size.MD);
    }

    public BsButton(String text, Skin skin, Variant v, Style st, Size sz) {
        super(text, skin, styleName(v, st));
        float padV = sz == Size.SM ? 4 : sz == Size.LG ? 12 : 7;
        float padH = sz == Size.SM ? 12 : sz == Size.LG ? 20 : 14;
        pad(padV, padH, padV, padH);
        // libGDX Button 默认点击会 toggle isChecked（点击后停在 checked 填充态，第二次点击才复原）。
        // 仅 OUTLINE 风格做成 momentary：取消点击触发的 ChangeEvent → setChecked 内部翻转被回滚 → 点击后自动复原。
        // SOLID 等其它风格保留 libGDX 原始 toggle 逻辑（不动）。
        if (st == Style.OUTLINE) {
            // setProgrammaticChangeEvents(false)：让 BsMenuBar / BsLayoutAdmin 等的「程序化 setChecked」
            //   不触发 ChangeEvent，从而不受下面 cancel 影响（它们的选中态照常生效）。
            setProgrammaticChangeEvents(false);
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                    event.cancel();
                }
            });
        }
    }

    private static String styleName(Variant v, Style st) {
        String color = v.name().toLowerCase();
        return st == Style.OUTLINE ? "bs-btn-outline-" + color : "bs-btn-" + color;
    }

    // ========================= Icon 支持 =========================

    /**
     * 在按钮文字前/后加图标。null 则移除图标。
     * <p>实现：scene2d TextButton 内部是 Table，构造后第一行 cell 是 Label，
     * 本方法 clearChildren + 重建为 [icon?] + [Label]。</p>
     */
    public BsButton setIcon(Drawable icon) {
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

    /** 设置 icon 尺寸。 */
    public BsButton setIconSize(float w, float h) {
        this.iconW = w;
        this.iconH = h;
        if (iconImage != null) {
            iconImage.setSize(w, h);
            relayoutWithIcon();
        }
        return this;
    }

    /** icon 与文字之间的间距。 */
    public BsButton setIconTextGap(float gap) {
        this.iconTextGap = gap;
        if (iconImage != null) relayoutWithIcon();
        return this;
    }

    /** icon 位置：true=文字左侧（默认），false=右侧。 */
    public BsButton setIconLeft(boolean left) {
        this.iconLeft = left;
        if (iconImage != null) relayoutWithIcon();
        return this;
    }

    /** 重建内部布局：把 icon + label 放回 Table。 */
    private void relayoutWithIcon() {
        if (iconImage == null) return;
        Label label = getLabel();
        // clearChildren 会清掉 Label cell，需要重新加
        clearChildren();
        if (iconLeft) {
            add(iconImage).size(iconW, iconH).padRight(iconTextGap);
            add(label);
        } else {
            add(label);
            add(iconImage).size(iconW, iconH).padLeft(iconTextGap);
        }
    }
}
