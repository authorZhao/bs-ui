package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * 带图标的 Label：水平排列 [icon] + [text]。
 *
 * <p>scene2d {@link Label} 本身不支持 icon，本类用 {@link Table} 包装 Image + Label
 * 模拟。常用于菜单项、状态显示、表头等。</p>
 *
 * <pre>{@code
 * BsIconLabel label = new BsIconLabel("3 条新消息", skin)
 *         .icon(BsIcon.get("envelope"))
 *         .iconColor(Color.RED);
 * }</pre>
 */
public class BsIconLabel extends Table {

    private final Image iconImage;
    private final Label label;
    private float iconSize = 16;
    private float gap = 4;

    /** 用默认 LabelStyle 创建。 */
    public BsIconLabel(String text, Skin skin) {
        this.iconImage = new Image();
        iconImage.setVisible(false);
        iconImage.setScaling(Scaling.fit);
        this.label = new Label(text, skin);
        left();
        add(iconImage).size(iconSize, iconSize).padRight(gap);
        add(label);
    }

    /** 用自定义 LabelStyle 创建。 */
    public BsIconLabel(String text, Label.LabelStyle style, Skin skin) {
        this.iconImage = new Image();
        iconImage.setVisible(false);
        iconImage.setScaling(Scaling.fit);
        this.label = new Label(text, style);
        left();
        add(iconImage).size(iconSize, iconSize).padRight(gap);
        add(label);
    }

    /** 设置图标。null 隐藏。 */
    public BsIconLabel icon(Drawable d) {
        if (d == null) {
            iconImage.setVisible(false);
        } else {
            iconImage.setDrawable(d);
            iconImage.setSize(iconSize, iconSize);
            iconImage.setVisible(true);
        }
        return this;
    }

    /** 用图标名从 {@link BsIcon} 加载。 */
    public BsIconLabel iconName(String name) {
        return icon(BsIcon.get(name));
    }

    public BsIconLabel iconSize(float size) {
        this.iconSize = size;
        getCell(iconImage).size(size, size);
        return this;
    }

    public BsIconLabel gap(float g) {
        this.gap = g;
        getCell(iconImage).padRight(g);
        return this;
    }

    /** icon 染色（白底图标 × color = 目标色）。 */
    public BsIconLabel iconColor(Color c) {
        iconImage.setColor(c);
        return this;
    }

    public BsIconLabel textColor(Color c) {
        // Label 用 style.fontColor，setColor 被覆盖，需要新 style
        Label.LabelStyle ls = new Label.LabelStyle(label.getStyle());
        ls.fontColor = c;
        label.setStyle(ls);
        return this;
    }

    public Label getLabel() { return label; }
    public Image getIconImage() { return iconImage; }
}
