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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.extern.slf4j.Slf4j;

/**
 * 带徽标的按钮：Bootstrap 风格按钮 + 右上角 Badge（消息数/数量红点）。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsBadgeButton btn = new BsBadgeButton("消息", skin, BsBadgeButton.Variant.PRIMARY);
 * btn.setBadge(5);
 * btn.setOnClick(() -> openInbox());
 * stage.addActor(btn);
 * btn.pack();
 * }</pre>
 *
 * <p>实现：用 {@link Group} 包装 BsButton + BsBadge。group 尺寸 = 按钮尺寸 +
 * badge 溢出空间（badge 右上角半圆溢出 ~8px），避免 badge 被 button 边界裁剪。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsBadgeButton extends Group {

    private final BsButton button;
    private BsBadge badge;
    private boolean badgeVisible = false;
    /** badge 溢出按钮边缘的距离（半个 badge 宽度）。 */
    private static final float BADGE_OVERFLOW = 10f;

    public BsBadgeButton(String text, Skin skin) {
        this(text, skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
    }

    public BsBadgeButton(String text, Skin skin, BsButton.Variant variant) {
        this(text, skin, variant, BsButton.Style.SOLID, BsButton.Size.MD);
    }

    public BsBadgeButton(String text, Skin skin, BsButton.Variant variant,
                         BsButton.Style style, BsButton.Size size) {
        button = new BsButton(text, skin, variant, style, size);
        addActor(button);
        // 默认创建一个红色 badge，但隐藏
        badge = new BsBadge("0", skin, BsBadge.Variant.DANGER);
        badge.setVisible(false);
        addActor(badge);
        // 第一次 pack 确保尺寸
        button.pack();
    }

    /** 设置 badge 数字。0 或负数 → 隐藏；正数 → 显示。 */
    public BsBadgeButton setBadge(int count) {
        if (count <= 0) {
            badgeVisible = false;
            badge.setVisible(false);
            relayout();
        } else {
            badgeVisible = true;
            badge.setText(String.valueOf(count));
            badge.setVisible(true);
            badge.toFront();
            relayout();
        }
        return this;
    }

    /** 设置 badge 文本（如 "99+"、"new"）。null/空 → 隐藏。 */
    public BsBadgeButton setBadge(String text) {
        if (text == null || text.isEmpty()) {
            badgeVisible = false;
            badge.setVisible(false);
            relayout();
        } else {
            badgeVisible = true;
            badge.setText(text);
            badge.setVisible(true);
            badge.toFront();
            relayout();
        }
        return this;
    }

    /** 设置 badge 配色（默认 DANGER 红）。 */
    public BsBadgeButton setBadgeVariant(BsBadge.Variant v) {
        Skin skin = button.getSkin();
        if (skin != null) {
            String text = badge.getText();
            boolean vis = badge.isVisible();
            badge.remove();
            badge = new BsBadge(text, skin, v);
            badge.setVisible(vis);
            addActor(badge);
            badge.toFront();
            relayout();
        }
        return this;
    }

    /** 点击回调。 */
    public BsBadgeButton setOnClick(Runnable r) {
        button.clearListeners();
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (r != null) {
                    try { r.run(); } catch (Throwable t) { log.warn("onClick", t); }
                }
            }
        });
        return this;
    }

    public BsButton getButton() { return button; }
    public BsBadge getBadge() { return badge; }

    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
        button.setSize(width, height);
        positionBadge();
    }

    /** 重算 layout：badge.pack → group 尺寸 = button + badge 溢出 → badge 定位右上角。 */
    private void relayout() {
        button.pack();
        badge.pack();
        // group 尺寸：按钮 + 溢出（badge 半个在按钮外）
        float gw = button.getWidth() + (badgeVisible ? BADGE_OVERFLOW : 0);
        float gh = button.getHeight() + (badgeVisible ? BADGE_OVERFLOW : 0);
        super.setSize(gw, gh);
        // button 占据 group 左下（让 badge 溢出到右上）
        button.setPosition(0, 0);
        button.setSize(button.getWidth(), button.getHeight());
        positionBadge();
    }

    /** badge 定位：中心在 button 右上角，向外偏移 BADGE_OVERFLOW/2。 */
    private void positionBadge() {
        if (badge == null || !badge.isVisible()) return;
        badge.pack();
        float bw = badge.getWidth();
        float bh = badge.getHeight();
        // button 右上角 (buttonW, buttonH)，badge 中心放在该点偏外
        float cx = button.getWidth() + BADGE_OVERFLOW / 2f;
        float cy = button.getHeight() + BADGE_OVERFLOW / 2f;
        badge.setPosition(cx - bw / 2f, cy - bh / 2f);
        badge.toFront();
    }

    /** pack：先 pack 按钮，relayout（含 badge 尺寸/位置）。
     * libgdx Actor.pack() 在某些版本是 final，所以不写 @Override。 */
    public void pack() {
        relayout();
    }

    @SuppressWarnings("unused")
    private void touchActor(Actor a) {}
}
