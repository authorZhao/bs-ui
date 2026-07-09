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
 * Bootstrap 5 风格按钮。支持 6 色变体 + solid/outline/ghost 三种样式 + size + 可选 icon。
 * <p>所需 Skin 资源由 {@link BsSkinFactory} 提供：</p>
 * <ul>
 *   <li>{@code bs-btn-{color}}（实心 SOLID）</li>
 *   <li>{@code bs-btn-outline-{color}}（OUTLINE 描边）</li>
 *   <li><b>{@code bs-btn-ghost-{color}}（GHOST 无边框）</b>：透明背景无边框，hover 微亮、按下选中色。
 *       按需运行时派生（{@link #ensureGhostStyle}），烘焙 skin 无需预生成 —— 适合导航/工具栏等
 *       「平时无边框、悬浮才显现」的场景（Win11 风格）。</li>
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
    public enum Style { SOLID, OUTLINE, GHOST }

    private Image iconImage;
    private float iconW = 16, iconH = 16;
    private float iconTextGap = 4;
    private boolean iconLeft = true;  // true=icon 在文字左

    public BsButton(String text, Skin skin) {
        this(text, skin, Variant.PRIMARY, Style.SOLID, Size.MD);
    }

    public BsButton(String text, Skin skin, Variant v, Style st, Size sz) {
        super(text, skin, resolveStyleName(skin, v, st));
        float padV = sz == Size.SM ? 4 : sz == Size.LG ? 12 : 7;
        float padH = sz == Size.SM ? 12 : sz == Size.LG ? 20 : 14;
        pad(padV, padH, padV, padH);
        // libGDX Button 默认点击会 toggle isChecked（点击后停在 checked 填充态，第二次点击才复原）。
        // OUTLINE / GHOST 做成 momentary：取消点击触发的 ChangeEvent → setChecked 内部翻转被回滚 → 点击后自动复原。
        // SOLID 保留 libGDX 原始 toggle 逻辑。
        if (st == Style.OUTLINE || st == Style.GHOST) {
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

    private static String resolveStyleName(Skin skin, Variant v, Style st) {
        if (st == Style.GHOST) {
            String name = "bs-btn-ghost-" + v.name().toLowerCase();
            ensureGhostStyle(skin, v, name);
            return name;
        }
        return styleName(v, st);
    }

    private static String styleName(Variant v, Style st) {
        String color = v.name().toLowerCase();
        return st == Style.OUTLINE ? "bs-btn-outline-" + color : "bs-btn-" + color;
    }

    /**
     * 运行时派生 GHOST 样式并注册进 skin（首次使用时，主题切换 skin 重建后会重新派生）。
     *
     * <p><b>和 OUTLINE 完全一致，仅 up 去掉边框线</b>：复用 OUTLINE 的 over/down/fontColor 等
     * 全部字段（hover/按下表现、文字色都和 OUTLINE 一样），只把 up 的描边 drawable 置 null。
     * 适合「平时无边框、hover/按下才显现」的场景（Win11 导航风格）。</p>
     */
    private static void ensureGhostStyle(Skin skin, Variant v, String name) {
        if (skin.has(name, TextButtonStyle.class)) return;
        String outlineName = "bs-btn-outline-" + v.name().toLowerCase();
        TextButtonStyle base;
        try {
            base = skin.get(outlineName, TextButtonStyle.class);
        } catch (Throwable t) {
            base = skin.get("default", TextButtonStyle.class);   // 兜底
        }
        TextButtonStyle ghost = new TextButtonStyle(base);
        ghost.up = null;   // 唯一区别：up 无边框透明；over/down/fontColor 等均与 OUTLINE 一致
        skin.add(name, ghost);
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
