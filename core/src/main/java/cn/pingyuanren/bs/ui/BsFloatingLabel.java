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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

/**
 * Bootstrap 5 风格浮动标签输入（Floating Label）——
 * 输入框为空时占位提示在中间；输入内容或聚焦后，占位浮到顶部变成小标签。
 *
 * <p>结构：</p>
 * <pre>
 * ┌─────────────────────────┐    ┌─────────────────────────┐
 * │                         │    │ 用户名                  │  ← 浮到顶部的小标签
 * │   用户名                │ →  │ authorZhao              │  ← 实际输入
 * │                         │    │                         │
 * └─────────────────────────┘    └─────────────────────────┘
 *      (空、未聚焦)                   (有内容/聚焦)
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsFloatingLabel f = new BsFloatingLabel(skin, "用户名");
 * stage.addActor(f);
 *
 * // 取内部 TextField 设置监听
 * f.getField().setTextFieldListener(...);
 * }</pre>
 *
 * <p>实现：Table = [浮起小标签（默认隐藏）] + [TextField]。
 * 监听 TextField 的焦点和内容变化：有内容/聚焦时显示标签并缩放 0.7，否则隐藏。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsFloatingLabel extends Table {

    private final BsTextField field;
    private final Label floatingLabel;
    private final Container<Label> labelWrap;
    private final String placeholder;
    private boolean floated = false;

    public BsFloatingLabel(Skin skin, String placeholder) {
        this.placeholder = placeholder;

        top().left();
        defaults().growX();

        // 浮起的标签（默认高度 0，浮起时显示）
        Label.LabelStyle flStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        flStyle.font = skin.getFont("font-sm");
        floatingLabel = new Label(placeholder, flStyle);
        floatingLabel.setColor(BsTheme.ts());
        labelWrap = new Container<>(floatingLabel);
        labelWrap.pad(0, 4, 0, 4);
        labelWrap.fillX();
        labelWrap.height(0);   // 默认高度 0（隐藏）
        add(labelWrap).growX().row();

        // 输入框（占位文字居中显示）
        field = new BsTextField("", skin);
        field.setMessageText(placeholder);
        add(field).growX();

        // 监听焦点 + 文本变化
        field.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, Actor fromActor) {
                updateFloating(true);
            }
            @Override
            public void exit(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, Actor toActor) {
                updateFloating(false);
            }
        });
        field.setTextFieldListener(new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                boolean focused = textField.getStage() != null
                        && textField.getStage().getKeyboardFocus() == textField;
                updateFloating(focused || !textField.getText().isEmpty());
            }
        });

        updateFloating(false);
    }

    /** 根据是否浮起切换标签显示。 */
    private void updateFloating(boolean hasFocusOrContent) {
        boolean shouldFloat = hasFocusOrContent || !field.getText().isEmpty();
        if (shouldFloat == floated) return;
        floated = shouldFloat;
        if (shouldFloat) {
            labelWrap.height(14);   // 给标签留位置
            // 输入框空时仍显示居中占位；非空时移到顶部后 placeholder 应淡化
            if (!field.getText().isEmpty()) {
                field.setMessageText("");
            }
        } else {
            labelWrap.height(0);
            field.setMessageText(placeholder);
        }
        invalidateHierarchy();
    }

    public BsTextField getField() { return field; }

    public BsFloatingLabel setLabelColor(Color c) {
        floatingLabel.setColor(c);
        return this;
    }

    /** 整体宽度。 */
    public BsFloatingLabel setPrefWidth(float w) {
        field.setWidth(w);
        return this;
    }
}
