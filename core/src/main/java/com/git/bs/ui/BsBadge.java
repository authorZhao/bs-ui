package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * Bootstrap 风格 Badge 徽标：小圆角色块 + 数字/文字。
 *
 * <p>6 种 Variant 配色（primary/success/danger/warning/info/secondary），
 * 用于显示消息数、道具数、状态标签等。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * // 独立使用
 * BsBadge badge = new BsBadge("3", skin, BsBadge.Variant.DANGER);
 *
 * // 配合按钮：用 BsBadgeButton（按钮右上角红点）
 * BsBadgeButton btn = new BsBadgeButton("消息", skin, () -> openInbox());
 * btn.setBadge(5);
 * }</pre>
 */
public class BsBadge extends Table {

    public enum Variant {
        PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO;

        /** 通过枚举取 BsPalette 引用（主题切换后下次调用自动新色）。 */
        public BsPalette palette() {
            switch (this) {
                case PRIMARY:   return BsPalette.PRIMARY;
                case SECONDARY: return BsPalette.SECONDARY;
                case SUCCESS:   return BsPalette.SUCCESS;
                case DANGER:    return BsPalette.DANGER;
                case WARNING:   return BsPalette.WARNING;
                case INFO:      return BsPalette.INFO;
            }
            return BsPalette.SECONDARY;
        }
    }

    private final Label label;

    public BsBadge(String text, Skin skin) {
        this(text, skin, Variant.SECONDARY);
    }

    public BsBadge(String text, Skin skin, Variant variant) {
        // 圆角纯色背景（newDrawable("white", color) 让 1×1 white drawable 染色）
        setBackground(skin.newDrawable("white", variant.palette().getMain()));
        pad(2, 6, 2, 6);

        // 独立 LabelStyle：白字 + 正常字号（确保数字清晰可读）
        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = skin.getFont("default");
        ls.fontColor = Color.WHITE;
        label = new Label(text == null ? "" : text, ls);
        label.setColor(Color.WHITE);
        // 固定最小尺寸（保证单字符也是圆形/圆角方块）
        float minSize = skin.getFont("default").getLineHeight();
        add(label).minWidth(minSize).minHeight(minSize).pad(0, 4, 0, 4).center();
    }

    public void setText(String t) { label.setText(t); }
    public String getText() { return label.getText().toString(); }
}
