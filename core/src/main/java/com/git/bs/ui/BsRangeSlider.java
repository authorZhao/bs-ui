/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库。
 * Copyright (c) 2026 bs-ui contributors
 *
 * 基于 Apache License 2.0 开源，允许商用、修改和再分发。
 * 使用本库的产品须在“关于”界面标注本项目，详见 LICENSE。
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.extern.slf4j.Slf4j;

/// 双滑块区间选择器（RangeSlider）：在 `[min, max]` 内选择 `[low, high]` 区间。
///
/// 弥补 `BsSlider` 只支持单值的不足，用于价格区间、数值过滤等场景。
///
/// 用法：
/// ```java
/// BsRangeSlider rs = new BsRangeSlider(0, 100, 1)
///         .setRange(20, 80)
///         .setMinGap(5)
///         .setOnChange((lo, hi) -> System.out.println("区间: " + lo + " ~ " + hi));
/// rs.setSize(300, 24);
/// stage.addActor(rs);
/// ```
///
/// 实现：自定义 `Actor`，draw 画 track + 区间 fill + 两个 knob；
/// `InputListener` 拖动较近的那个 knob，自动保证 `low ≤ high − minGap`，按 step 对齐。
///
/// v1 不含：垂直方向、键盘操作、禁用态。
/// @author authorZhao
/// @since 2026-07-16
@Slf4j
public class BsRangeSlider extends Actor {

    /// 区间变化回调。
    @FunctionalInterface
    public interface OnRangeChange {
        void changed(float low, float high);
    }

    private final float min;
    private final float max;
    private final float step;
    /// 区间填充 Drawable（primary 色纯色块）。构造时创建一次缓存，
    /// 避免在 draw() 里每帧 new Pixmap+Texture 导致 GPU 内存泄漏。
    /// track/knob 走 skin 单例（skin.getDrawable 不分配），无需缓存。
    private final Drawable fill = BsSkinFactory.drawableOf(BsPalette.PRIMARY.getMain());

    private float low;
    private float high;
    private float minGap = 0f;
    private OnRangeChange onChange;
    /// 正在拖动的 knob：0=low，1=high，-1=无。
    private int dragging = -1;

    public BsRangeSlider(float min, float max) {
        this(min, max, 0f);
    }

    public BsRangeSlider(float min, float max, float step) {
        this.min = min;
        this.max = Math.max(min, max);
        this.step = step > 0 ? step : 0;
        this.low = min;
        this.high = this.max;
        setTouchable(Touchable.enabled);
        setSize(200, 24);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                float xl = valueToX(low);
                float xh = valueToX(high);
                dragging = (Math.abs(x - xl) <= Math.abs(x - xh)) ? 0 : 1;
                moveTo(x);
                return true;  // 拿到 touchFocus，后续拖动/抬起才会回调本监听
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                moveTo(x);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                dragging = -1;
            }
        });
    }

    // =================== API ===================

    public BsRangeSlider setLow(float v) {
        low = clampLow(snap(v));
        return this;
    }

    public BsRangeSlider setHigh(float v) {
        high = clampHigh(snap(v));
        return this;
    }

    /// 同时设区间。
    public BsRangeSlider setRange(float lo, float hi) {
        setLow(lo);
        setHigh(hi);
        return this;
    }

    /// 两 knob 之间的最小间隔（max - minGap 仍允许贴近）。
    public BsRangeSlider setMinGap(float gap) {
        this.minGap = Math.max(0, gap);
        return this;
    }

    public BsRangeSlider setOnChange(OnRangeChange c) {
        this.onChange = c;
        return this;
    }

    public float getLow() { return low; }

    public float getHigh() { return high; }

    // =================== 内部 ===================

    private void moveTo(float x) {
        float v = snap(xToValue(x));
        if (dragging == 0) {
            low = clampLow(v);
        } else if (dragging == 1) {
            high = clampHigh(v);
        }
        if (onChange != null) {
            try {
                onChange.changed(low, high);
            } catch (Throwable t) {
                log.warn("BsRangeSlider onChange error", t);
            }
        }
    }

    private float clampLow(float v) {
        return MathUtils.clamp(v, min, Math.max(min, high - minGap));
    }

    private float clampHigh(float v) {
        return MathUtils.clamp(v, Math.min(max, low + minGap), max);
    }

    private float snap(float v) {
        if (step <= 0) return v;
        return Math.round((v - min) / step) * step + min;
    }

    private float span() {
        return Math.max(1e-6f, max - min);
    }

    private float knobW() {
        Drawable k = BsUI.getSkin().getDrawable("bs-slider-knob");
        float w = k.getMinWidth();
        return w > 0 ? w : getHeight();
    }

    private float valueToX(float v) {
        float knobW = knobW();
        float left = knobW / 2f;
        float right = getWidth() - knobW / 2f;
        return left + (v - min) / span() * (right - left);
    }

    private float xToValue(float x) {
        float knobW = knobW();
        float left = knobW / 2f;
        float right = getWidth() - knobW / 2f;
        float t = (x - left) / Math.max(1e-6f, right - left);
        return MathUtils.clamp(min + t * span(), min, max);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Skin skin = BsUI.getSkin();
        Drawable track = skin.getDrawable("bs-slider-bg");
        Drawable knob = skin.getDrawable("bs-slider-knob");

        float w = getWidth();
        float h = getHeight();
        float knobW = knob.getMinWidth() > 0 ? knob.getMinWidth() : h;
        float knobH = knob.getMinHeight() > 0 ? knob.getMinHeight() : h;
        float trackH = Math.max(4f, knobH * 0.4f);
        float cy = getY() + h / 2f;

        Color c = getColor();
        batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);

        float xl = valueToX(low);
        float xh = valueToX(high);

        // track（整条）
        track.draw(batch, getX() + knobW / 2f, cy - trackH / 2f, w - knobW, trackH);
        // 区间 fill
        fill.draw(batch, getX() + xl, cy - trackH / 2f, Math.max(0, xh - xl), trackH);
        // 两 knob
        knob.draw(batch, getX() + xl - knobW / 2f, cy - knobH / 2f, knobW, knobH);
        knob.draw(batch, getX() + xh - knobW / 2f, cy - knobH / 2f, knobW, knobH);

        batch.setColor(Color.WHITE);
    }
}
