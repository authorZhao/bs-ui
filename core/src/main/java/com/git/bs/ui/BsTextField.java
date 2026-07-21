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

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter;

import java.lang.reflect.Field;

/// @author authorZhao
/// @since 2026-07-16
public class BsTextField extends TextField {
    public BsTextField(String text, Skin skin) {
        super(text, skin);
        WebIme.attach(this);
    }

    public BsTextField(String text, Skin skin, String styleName) {
        super(text, skin, styleName);
        WebIme.attach(this);
    }

    /// 程序化设值：绕过 TextFieldFilter。
    ///
    /// libGDX 1.14.x 的 `setText` 内部走 `paste`，会逐字符过 `filter.acceptChar`，
    /// 若 filter 拒绝字符（如只读选择器的 `(f, c) -> false`），setText 设的值会被全部丢弃，
    /// 表现为「输入框永远空白」。这里在设值前临时摘掉 filter，设完恢复。
    public void setTextProgrammatic(String str) {
        TextFieldFilter old = getTextFieldFilter();
        setTextFieldFilter(null);
        try {
            setText(str);
        } finally {
            setTextFieldFilter(old);
        }
    }

    // =================== 光标闪烁频率控制 ===================

    /**
     * libgdx TextField 内部光标闪烁计时字段（反射获取，不同版本字段名/逻辑可能不同；
     * 取不到则本机制失效，退回 libgdx 默认闪烁频率）。
     */
    private static final Field CURSOR_BLINK;
    static {
        Field f = null;
        try {
            f = TextField.class.getDeclaredField("cursorBlink");
            f.setAccessible(true);
        } catch (Throwable ignored) {}
        CURSOR_BLINK = f;
    }

    /** 自定义光标闪烁周期（秒）。默认 1.06f（≈Win11 0.53s 切换一次），比 libgdx 默认慢、更柔和。 */
    private float blinkPeriod = 1.06f;
    private float blinkAccum;

    /** 设置光标闪烁周期（秒）。值越大闪烁越慢；过小会 clamp 到 0.2f。 */
    public BsTextField setBlinkPeriod(float seconds) {
        this.blinkPeriod = Math.max(0.2f, seconds);
        return this;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (CURSOR_BLINK == null) return;   // 反射取不到字段，退回默认
        // libgdx super.act 会把 cursorBlink 减 delta；这里按自定义周期覆盖 cursorBlink 值，
        // 让 draw 的光标可见判断按慢节奏切换（前半周期可见、后半隐藏）。
        blinkAccum += delta;
        if (blinkAccum >= blinkPeriod) blinkAccum -= blinkPeriod;
        try {
            float phase = blinkAccum / blinkPeriod;  // 0~1
            // libgdx draw 通常：cursorBlink 较小（< 0.5）画光标，较大不画。
            // 前半周期设 0.2（画）、后半设 0.8（不画）→ 慢闪烁。
            CURSOR_BLINK.set(this, phase < 0.5f ? 0.2f : 0.8f);
        } catch (Throwable ignored) {}
    }
}
