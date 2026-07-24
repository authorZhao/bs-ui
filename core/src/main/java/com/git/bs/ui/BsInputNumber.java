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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Bootstrap 5 风格数字步进器（InputNumber / Number Stepper）。
 *
 * <p><b>注意：Bootstrap 的 "Spinner" 是加载转圈（{@link BsSpinner}），
 * 这里的 +/- 数字步进器对应 HTML &lt;input type="number"&gt;，命名为 InputNumber。</b></p>
 *
 * <p>结构：[−] [输入框] [+]，水平排列。支持：</p>
 * <ul>
 *   <li>步长 step（默认 1，可设小数）</li>
 *   <li>范围 min/max（超出会被 clamp）</li>
 *   <li>整数/小数模式</li>
 *   <li>直接输入数字（失焦时校验并格式化）</li>
 *   <li>长按 +/- 按钮连续增减（每 0.1s 触发一次）</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsInputNumber num = new BsInputNumber(skin);
 * num.setRange(0, 100);
 * num.setStep(5);
 * num.setValue(20);
 * num.setOnChange(v -> setStatus("当前值: " + v));
 * stage.addActor(num);
 * }</pre>
 *
 * <p>实现：[−] 与 [+] 用 {@link BsButton}（SM）；中间是 {@link BsTextField}，
 * 失焦/回车时解析文本回填。长按用 ClickListener 的 touchDown/UP 实现，
 * 触发后启动定时器在 act 中重复触发，松手取消。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsInputNumber extends Table {

    private final BsButton minusBtn;
    private final BsButton plusBtn;
    private final BsTextField field;

    private double step = 1;
    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;
    private boolean integerOnly = true;
    private int decimals = 0;
    private double value = 0;
    private Consumer<Double> onChange;

    /** 长按重复触发间隔（秒）。 */
    private static final float REPEAT_INTERVAL = 0.08f;
    /** 长按多少秒后开始重复触发。 */
    private static final float REPEAT_DELAY = 0.4f;
    /** 当前正在长按的方向：+1 = plus, -1 = minus, 0 = 无。 */
    private int holdingDir = 0;
    private float holdTimer = 0;

    public BsInputNumber(Skin skin) {
        minusBtn = new BsButton("-", skin, BsButton.Variant.SECONDARY, BsButton.Style.SOLID, BsButton.Size.SM);
        plusBtn = new BsButton("+", skin, BsButton.Variant.SECONDARY, BsButton.Style.SOLID, BsButton.Size.SM);
        field = new BsTextField("0", skin);
        field.setAlignment(1);  // center
        field.setTextFieldFilter((f, ch) -> Character.isDigit(ch) || ch == '-' || ch == '.');
        field.setTextFieldListener(new com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                if (c == '\n' || c == '\r') {
                    commitText();
                }
            }
        });
        // 失焦时提交（焦点切到其他 actor 时校验文本）
        field.addListener(new ClickListener() {
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer != -1) {
                    commitText();
                }
            }
        });

        attachHold(minusBtn, -1);
        attachHold(plusBtn, +1);

        add(minusBtn).size(32, 32);
        add(field).width(70).height(32).padLeft(-1);   // -1 让按钮和输入框边框贴合
        add(plusBtn).size(32, 32).padLeft(-1);

        setValue(0);
    }

    /** 给按钮挂上长按连续触发逻辑。dir = -1 / +1。 */
    private void attachHold(BsButton btn, int dir) {
        btn.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                applyDelta(dir * step);
                holdingDir = dir;
                holdTimer = 0;
                return true;
            }
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                holdingDir = 0;
            }
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // touchDown 已应用一次增量，click 不再重复
            }
        });
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (holdingDir != 0) {
            holdTimer += delta;
            if (holdTimer >= REPEAT_DELAY) {
                // 进入连续触发阶段：每 REPEAT_INTERVAL 触发一次
                float excess = holdTimer - REPEAT_DELAY;
                int ticks = (int) (excess / REPEAT_INTERVAL) + 1;
                // 但 act 每帧只触发一次（够快），用累积时间控制
                if (holdTimer - REPEAT_DELAY >= REPEAT_INTERVAL) {
                    applyDelta(holdingDir * step);
                    holdTimer -= REPEAT_INTERVAL;
                }
                if (ticks > 100) holdTimer = REPEAT_DELAY;  // 防爆
            }
        }
    }

    /** 应用增量并校验范围。 */
    private void applyDelta(double delta) {
        double v = value + delta;
        if (v < min) v = min;
        if (v > max) v = max;
        setValue(v);
    }

    /** 把输入框文本解析为数字并提交（无效则回滚）。 */
    private void commitText() {
        String t = field.getText();
        try {
            double v = Double.parseDouble(t);
            if (v < min) v = min;
            if (v > max) v = max;
            setValue(v);
        } catch (NumberFormatException ex) {
            // 无效输入，回滚显示
            syncField();
        }
    }

    /** 同步输入框显示。 */
    private void syncField() {
        String txt;
        if (integerOnly || decimals == 0) {
            txt = String.valueOf((long) value);
        } else {
            txt = String.format("%." + decimals + "f", value);
        }
        if (!field.getText().equals(txt)) {
            field.setText(txt);
        }
    }

    // ========================= builder =========================

    public BsInputNumber setStep(double step) { this.step = step; return this; }
    public BsInputNumber setRange(double min, double max) { this.min = min; this.max = max; return this; }
    public BsInputNumber setMin(double min) { this.min = min; return this; }
    public BsInputNumber setMax(double max) { this.max = max; return this; }

    /** 是否只允许整数（默认 true）。设 false 后可输入小数。 */
    public BsInputNumber setIntegerOnly(boolean integerOnly) {
        this.integerOnly = integerOnly;
        if (integerOnly) this.decimals = 0;
        return this;
    }

    /** 小数位数（integerOnly=false 时生效）。 */
    public BsInputNumber setDecimals(int d) {
        this.decimals = Math.max(0, d);
        if (this.decimals > 0) this.integerOnly = false;
        return this;
    }

    public BsInputNumber setValue(double v) {
        if (v < min) v = min;
        if (v > max) v = max;
        boolean changed = Double.compare(v, this.value) != 0;
        this.value = v;
        syncField();
        if (changed && onChange != null) {
            try { onChange.accept(this.value); } catch (Throwable t) { log.warn("onChange", t); }
        }
        return this;
    }

    public double getValue() { return value; }
    public int getIntValue() { return (int) Math.round(value); }
    public long getLongValue() { return Math.round(value); }

    public BsInputNumber setOnChange(Consumer<Double> cb) { this.onChange = cb; return this; }

    /** 获取内部输入框（用于设置宽度/校验等）。 */
    public BsTextField getField() { return field; }
}
