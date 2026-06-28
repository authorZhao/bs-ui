package com.git.bs.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
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
 */
public class BsSlider extends Slider {

    public BsSlider(float min, float max, float step, boolean vertical, Skin skin) {
        super(min, max, step, vertical, skin);
    }

    public BsSlider(float min, float max, float step, boolean vertical, Skin skin, String styleName) {
        super(min, max, step, vertical, skin, styleName);
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
