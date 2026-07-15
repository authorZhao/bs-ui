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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Bootstrap 风格开关（Switch / Toggle）—— iOS 风格的滑动开关。
 *
 * <p>与 {@link BsCheckBox}（方框打勾）的视觉区别：Switch 是椭圆形轨道 + 圆形滑块，
 * 状态切换时滑块滑动 + 轨道变色，UI 更现代，常用于设置项开关。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsSwitch sw = new BsSwitch(skin);
 * sw.setLabel("启用通知");
 * sw.setChecked(true);
 * sw.setOnChange(checked -> setStatus("通知: " + (checked ? "开" : "关")));
 * stage.addActor(sw);
 * }</pre>
 *
 * <p>实现：自绘 Actor（轨道）+ 一个圆形滑块（用 accent 色填充）。
 * 切换时用 Actions.moveTo 平滑滑动滑块。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsSwitch extends Table {

    public enum Size { SM, MD, LG }

    private final Skin skin;
    private final Track track;
    private boolean checked = false;
    private boolean disabled = false;
    private Consumer<Boolean> onChange;
    private Size size = Size.MD;
    private Label label;

    public BsSwitch(Skin skin) {
        this(skin, Size.MD);
    }

    public BsSwitch(Skin skin, Size size) {
        this.skin = skin;
        this.size = size;
        track = new Track();
        add(track);
        left();
        // 点击切换（点击轨道或标签均可）
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (disabled) return false;
                setChecked(!checked);
                return true;
            }
        });
    }

    /** 添加右侧文字标签。 */
    public BsSwitch setLabel(String text) {
        if (label == null) {
            label = new Label(text, skin);
            add(label).padLeft(8);
        } else {
            label.setText(text);
        }
        return this;
    }

    public boolean isChecked() { return checked; }

    public BsSwitch setChecked(boolean c) {
        if (this.checked == c) return this;
        this.checked = c;
        track.slideTo(c);
        if (onChange != null) {
            try { onChange.accept(c); } catch (Throwable t) { log.warn("onChange", t); }
        }
        return this;
    }

    public BsSwitch setDisabled(boolean d) {
        this.disabled = d;
        track.setDisabled(d);
        if (label != null) label.setColor(d ? BsTheme.td() : BsTheme.tp());
        return this;
    }

    public BsSwitch setOnChange(Consumer<Boolean> cb) { this.onChange = cb; return this; }

    /** 内部 Track 自绘 actor。 */
    private class Track extends Actor {
        private float knobX;
        private float targetKnobX;
        private boolean disabledLocal = false;
        private static final ShapeRenderer SR = new ShapeRenderer();   // 共享单例，避免每实例 new 泄漏 native OpenGL 资源

        Track() {
            float[] wh = trackSize();
            setSize(wh[0], wh[1]);
            knobX = knobRestX(false);
            targetKnobX = knobX;
        }

        private float[] trackSize() {
            switch (size) {
                case SM: return new float[]{36, 20};
                case LG: return new float[]{60, 32};
                default: return new float[]{46, 26};
            }
        }

        private float knobRadius() {
            switch (size) {
                case SM: return 7;
                case LG: return 12;
                default: return 9;
            }
        }

        /** 滑块在指定状态下的 X 坐标（左侧 = unchecked，右侧 = checked）。 */
        private float knobRestX(boolean checkedState) {
            float r = knobRadius();
            return checkedState ? getWidth() - r - 4 : r + 4;
        }

        void slideTo(boolean c) {
            targetKnobX = knobRestX(c);
            float dur = 0.18f;
            clearActions();
            addAction(Actions.sequence(
                    Actions.run(() -> {}),
                    Actions.moveBy(0, 0, dur),   // 占位让 sequence 有时长
                    Actions.run(() -> knobX = targetKnobX)
            ));
            // 直接用并行 action 平滑移动 knobX（用 FloatValue 太重，这里每帧 lerp）
            // 简化：每帧 act 中 lerp
        }

        void setDisabled(boolean d) { disabledLocal = d; }

        @Override
        public void act(float delta) {
            super.act(delta);
            // lerp knobX 向 targetKnobX
            float speed = 12f;   // 越大越快
            knobX += (targetKnobX - knobX) * Math.min(1, delta * speed);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.end();
            try {
                SR.setProjectionMatrix(batch.getProjectionMatrix());
                SR.setTransformMatrix(batch.getTransformMatrix());
                SR.setColor(1, 1, 1, parentAlpha);
                SR.begin(ShapeRenderer.ShapeType.Filled);
                try {
                    SR.translate(getX(), getY(), 0);
                    // 轨道（圆角矩形 = 圆角胶囊）
                    Color trackColor = disabledLocal
                            ? BsTheme.td()
                            : checked
                                    ? BsPalette.PRIMARY.getMain()
                                    : BsTheme.tm();
                    SR.setColor(trackColor);
                    float r = getHeight() / 2f;
                    roundRect(SR, 0, 0, getWidth(), getHeight(), r);
                    // 滑块（白色实心圆）
                    SR.setColor(Color.WHITE);
                    SR.circle(knobX, getHeight() / 2f, knobRadius());
                } finally {
                    SR.identity();
                    SR.end();
                }
            } finally {
                batch.begin();
            }
        }

        /** 圆角矩形（Filled 模式，用矩形 + 两端 fillCircle 近似胶囊）。 */
        private void roundRect(ShapeRenderer sr, float x, float y, float w, float h, float r) {
            SR.rect(x + r, y, w - 2 * r, h);
            SR.rect(x, y + r, r, h - 2 * r);
            SR.rect(x + w - r, y + r, r, h - 2 * r);
            SR.circle(x + r, y + r, r);
            SR.circle(x + w - r, y + r, r);
            SR.circle(x + r, y + h - r, r);
            SR.circle(x + w - r, y + h - r, r);
        }
    }
}
