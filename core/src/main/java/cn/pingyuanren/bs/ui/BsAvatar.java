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
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * Bootstrap 风格头像（Avatar）—— 独立的头像组件。
 *
 * <p>支持 CIRCLE / ROUNDED / SQUARE 三种形状，可选右下角在线状态点或自定义徽章。
 * 与 {@link BsProfileCard} 内嵌头像的区别：BsAvatar 是原子组件，
 * 单独用作列表项/导航栏/卡片角落的小头像。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsAvatar avatar = new BsAvatar(skin)
 *         .image(myDrawable)
 *         .size(48)
 *         .shape(BsAvatar.Shape.CIRCLE)
 *         .online(true);
 * stage.addActor(avatar);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsAvatar extends Table {

    public enum Shape { CIRCLE, ROUNDED, SQUARE }

    private final Image image;
    private final Container<Image> imgWrap;
    private final Table badgeLayer;
    private float avatarSize = 40;
    private Shape shape = Shape.CIRCLE;
    private Color bgColorOverride = null;
    /** V2：颜色存放在 skin，字段初始化时无法访问 skin。 */
    private Color bgColor() { return bgColorOverride != null ? bgColorOverride : BsPalette.SECONDARY.getMain(); }

    public BsAvatar(Skin skin) {
        image = new Image();
        image.setScaling(Scaling.fit);
        imgWrap = new Container<>(image);
        imgWrap.fill();
        imgWrap.size(avatarSize);
        badgeLayer = new Table();
        badgeLayer.setVisible(false);

        // 用 Stack 把头像和徽章层叠
        Stack stack = new Stack();
        stack.add(imgWrap);
        stack.add(badgeLayer);
        add(stack).size(avatarSize, avatarSize);
    }

    /** 设置头像图片。null 则保留占位。 */
    public BsAvatar image(Drawable d) {
        if (d == null) {
            image.setDrawable(BsSkinFactory.drawableOf(bgColor()));
            return this;
        }
        Drawable view = (shape == Shape.CIRCLE)
                ? BsSkinFactory.makeRoundDrawable(d, (int) avatarSize)
                : d;
        image.setDrawable(view);
        return this;
    }

    /** 头像尺寸（正方形边长）。 */
    public BsAvatar size(float s) {
        this.avatarSize = s;
        imgWrap.size(s);
        getCells().first().size(s, s);
        invalidateHierarchy();
        return this;
    }

    public BsAvatar shape(Shape s) { this.shape = s; return this; }

    public BsAvatar bgColor(Color c) { this.bgColorOverride = c; return this; }

    /** 右下角在线状态点（true = 绿点 online，false = 隐藏）。 */
    public BsAvatar online(boolean isOnline) {
        badgeLayer.clearChildren();
        badgeLayer.setVisible(isOnline);
        if (!isOnline) return this;
        Skin skin = BsUI.getSkin();
        // 用背景色实心圆近似（■ 染色 + 白色描边）
        com.badlogic.gdx.scenes.scene2d.ui.Label dot = new com.badlogic.gdx.scenes.scene2d.ui.Label("●", skin);
        dot.setColor(BsPalette.SUCCESS.getMain());   // success 绿
        Container<com.badlogic.gdx.scenes.scene2d.ui.Label> dotWrap = new Container<>(dot);
        // 白色边框底色
        dotWrap.setBackground(BsSkinFactory.drawableOf(Color.WHITE));
        dotWrap.pad(1, 2, 1, 2);
        float dotSize = Math.max(10, avatarSize * 0.28f);
        badgeLayer.add(dotWrap).size(dotSize, dotSize).expand().bottom().right();
        return this;
    }

    /** 自定义右下角徽章（任意 drawable，如未读数）。 */
    public BsAvatar badge(Drawable d) {
        badgeLayer.clearChildren();
        badgeLayer.setVisible(true);
        Image bi = new Image(d);
        bi.setScaling(Scaling.fit);
        Container<Image> wrap = new Container<>(bi);
        float s = Math.max(12, avatarSize * 0.32f);
        wrap.size(s, s);
        badgeLayer.add(wrap).expand().bottom().right();
        return this;
    }
}
