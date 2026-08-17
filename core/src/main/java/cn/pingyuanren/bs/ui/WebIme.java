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

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * web 端输入框中文输入兜底（方案 0）。
 *
 * <p>libGDX web 后端（gdx-teavm）的 canvas TextField 收不到 IME 组字事件，中文输不进去
 *（见 {@code docs/} 的输入 IME 分析）。这里在 web 端点 TextField 时弹
 * {@code Gdx.input.getTextInput} 模态框——里面是真 HTML {@code <input>}，浏览器原生 IME、
 * 候选窗、移动端虚拟键盘全都正常。</p>
 *
 * <p><b>只影响 web</b>：仅当 {@code Gdx.app.getType() == WebGL} 才触发；桌面/其它平台 clicked()
 * 直接 return，libGDX 原生内联输入完全不变（只是多一个空跑的 ClickListener）。</p>
 *
 * <p><b>待 web 实测的关键风险</b>：gdx-teavm 的 getTextInput 内部用了 {@code Window.setTimeout}
 *（自动 focus 输入框、抖动动画清理），而本项目在 wasm-gc 下已知 TeaVM 的 setTimeout 会崩
 *（见 {@code TeaVmPlatform} 注释，已改用 libGDX Timer 规避）。若点击字段时崩，方案 0 不可用，
 * 需自写 dialog 或上方案 B（内联 HTML input overlay）。</p>
 *
 * @author authorZhao
 * @since 2026-07-21
 */
final class WebIme {

    private WebIme() {}

    /** 给 TextField 挂上「web 端点击 → 弹输入框」；桌面无副作用。 */
    static void attach(TextField field) {
        field.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (Gdx.app.getType() != ApplicationType.WebGL) return; // 桌面/其它：内联不变
                if (field.isDisabled()) return;
                openDialog(field);
            }
        });
    }

    private static void openDialog(TextField field) {
        // 先摘掉 canvas 字段的键盘焦点：对话框打开期间，document 的 keydown（Backspace 等）会
        // 冒泡到 libGDX，若 canvas 字段还持焦会串键（在 dialog 里按 Backspace 会同时删 canvas 字段）。
        // postRunnable 等当前点击事件处理完再清，避开监听器先后顺序问题。
        Gdx.app.postRunnable(() -> {
            if (field.getStage() != null) field.getStage().setKeyboardFocus(null);
        });
        Gdx.input.getTextInput(new TextInputListener() {
            @Override public void input(String text) {
                setBypassFilter(field, text);
            }
            @Override public void canceled() {}
        }, "", field.getText(), "");
        // 注：gdx-teavm 的 dialog 把 text 参数显示成一行提示（input 不预填），故这里传当前值让用户看到。
    }

    /** 绕过 TextFieldFilter 设值（只读 filter 也能程序化设；逻辑同 BsTextField.setTextProgrammatic）。 */
    private static void setBypassFilter(TextField field, String text) {
        TextFieldFilter old = field.getTextFieldFilter();
        field.setTextFieldFilter(null);
        try {
            field.setText(text);
        } finally {
            field.setTextFieldFilter(old);
        }
    }
}
