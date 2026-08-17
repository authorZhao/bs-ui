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

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Bootstrap 风格 Slider：重写 draw 以消除 scene2d 默认 NinePatch padding 导致的
 * "knob 到端点有距离" 视觉问题，同时让 track 比 knob 细（视觉更柔和）。
 *
 * <p>本类的画法：knob 中心从 knob 半宽 移动到 width - knob 半宽 —— value=min 时
 * knob 左缘对齐 track 左端，value=max 时 knob 右缘对齐 track 右端，视觉上 knob
 * 真正"到底/到顶"。track 视觉宽度 = knob 的 40%（比 knob 细，看起来像 Bootstrap 滑槽）。</p>
 *
 * <p>垂直方向同理：knob 顶部对齐 track 顶端，底部对齐 track 底端。</p>
 *
 * <p><b>滚动容器内拖动防抖</b>：Slider 放在 ScrollPane 里时，ScrollPane 的 flickScroll
 * 手势检测（ActorGestureListener）会在拖动时触发页面平移，导致滑块拖动不流畅 + 页面跟着浮动。
 * 解法：touchDown 时临时禁用最近 ScrollPane 祖先的滚动（{@link ScrollPane#setScrollingDisabled}），
 * touchUp 时恢复。同时 touchDown 返回 true 消费事件阻止冒泡（双重保险）。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsSlider extends Slider {

    /** touchDown 时找到的 ScrollPane 祖先（null = 不在滚动容器内，无需处理）。 */
    private ScrollPane parentScroll;
    /** 记录原滚动禁用状态，touchUp 时恢复。 */
    private boolean savedScrollX, savedScrollY;

    public BsSlider(float min, float max, float step, boolean vertical, Skin skin) {
        super(min, max, step, vertical, skin);
        installScrollGuard();
    }

    public BsSlider(float min, float max, float step, boolean vertical, Skin skin, String styleName) {
        super(min, max, step, vertical, skin, styleName);
        installScrollGuard();
    }

    /**
     * 安装滚动防抖监听。两道防线：
     * <ol>
     *   <li>touchDown 时禁用父 ScrollPane 滚动（对付 flickScroll 手势检测）</li>
     *   <li>touchDown 返回 true 消费事件（阻止 touchDragged 冒泡）</li>
     * </ol>
     * touchUp 时恢复 ScrollPane 状态。
     */
    private void installScrollGuard() {
        addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // 禁用父 ScrollPane 滚动（flickScroll 的手势检测靠这个）
                parentScroll = findScrollPaneAncestor();
                if (parentScroll != null) {
                    savedScrollX = parentScroll.isScrollingDisabledX();
                    savedScrollY = parentScroll.isScrollingDisabledY();
                    parentScroll.setScrollingDisabled(true, true);
                }
                // 消费事件，阻止 touchDragged 冒泡到外层容器
                return true;
            }

            @Override public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                restoreScroll();
            }
        });
    }

    /** 沿 parent 链向上找最近的 {@link ScrollPane}。 */
    private ScrollPane findScrollPaneAncestor() {
        Actor p = getParent();
        while (p != null) {
            if (p instanceof ScrollPane) return (ScrollPane) p;
            p = p.getParent();
        }
        return null;
    }

    /** 恢复 touchDown 时禁用的滚动状态（幂等）。 */
    private void restoreScroll() {
        if (parentScroll != null) {
            try { parentScroll.setScrollingDisabled(savedScrollX, savedScrollY); }
            catch (Throwable ignored) {}
            parentScroll = null;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        SliderStyle style = getStyle();
        Drawable bg = style.background;
        Drawable knob = style.knob;
        if (bg == null || knob == null) {
            super.draw(batch, parentAlpha);
            return;
        }

        float w = getWidth();
        float h = getHeight();
        float percent = MathUtils.clamp(getVisualPercent(), 0f, 1f);

        if (isVertical()) {
            // 垂直：track 顶到底，knob 半高留边
            float knobH = knob.getMinHeight();
            if (knobH <= 0) knobH = w * 0.8f;
            float knobW = knob.getMinWidth();
            if (knobW <= 0) knobW = w;
            // track 占用（knob 中心运动范围）：扣除 knob 半高
            float trackBottom = knobH / 2f;
            float trackTop = h - knobH / 2f;
            float trackH = trackTop - trackBottom;
            // bg 宽度 = track 视觉宽度（knob 的 40%，比 knob 细，看起来更像 Bootstrap 滑槽）
            float bgW = Math.max(4, knobW * 0.4f);
            bg.draw(batch, getX() + (w - bgW) / 2f, getY() + trackBottom,
                    bgW, trackH);
            // knob 中心 y = trackBottom + percent * trackH
            float knobY = getY() + trackBottom + percent * trackH - knobH / 2f;
            float knobX = getX() + (w - knobW) / 2f;
            knob.draw(batch, knobX, knobY, knobW, knobH);
        } else {
            // 水平：track 左到右，knob 半宽留边
            float knobW = knob.getMinWidth();
            if (knobW <= 0) knobW = h;
            float knobH = knob.getMinHeight();
            if (knobH <= 0) knobH = h;
            float trackLeft = knobW / 2f;
            float trackRight = w - knobW / 2f;
            float trackW = trackRight - trackLeft;
            // bg 高度 = track 视觉高度（knob 的 40%，比 knob 细）
            float bgH = Math.max(4, knobH * 0.4f);
            bg.draw(batch, getX() + trackLeft, getY() + (h - bgH) / 2f,
                    trackW, bgH);
            // knob 中心 x = trackLeft + percent * trackW
            float knobX = getX() + trackLeft + percent * trackW - knobW / 2f;
            float knobY = getY() + (h - knobH) / 2f;
            knob.draw(batch, knobX, knobY, knobW, knobH);
        }
    }
}
