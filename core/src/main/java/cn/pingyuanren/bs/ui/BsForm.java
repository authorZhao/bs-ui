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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Bootstrap 风格通用表单：纵向布局，每行 = Label + 控件 + 错误提示。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsForm form = new BsForm(skin);
 * form.addField("用户名", new BsTextField("", skin),
 *         v -> v.isEmpty() ? "必填" : null);
 * form.addField("邮箱", new BsTextField("", skin),
 *         v -> v.contains("@") ? null : "邮箱格式错误");
 * form.addSubmitBar("保存", () -> setStatus("提交: " + form.collectValues()),
 *                   "取消", () -> setStatus("取消"));
 * }</pre>
 *
 * <p>设计：</p>
 * <ul>
 *   <li>每行用独立 {@link Table} 容纳 Label / 控件 / 错误 Label（三列）。</li>
 *   <li>校验在两个时机触发：①控件 change 事件（失焦或输入）②调用 {@link #validateAll()}。</li>
 *   <li>错误 Label 默认隐藏，校验失败时显示红色文字。</li>
 *   <li>submit/cancel 用 {@link BsButton}（Variant.PRIMARY/SECONDARY），submit 前会自动跑校验。</li>
 * </ul>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsForm extends Table {

    /** 单个字段：标签 + 控件 + 校验函数 + 错误 Label。 */
    public static class Field {
        public final String label;
        @Getter public final Actor editor;
        public final Function<String, String> validator; // 返回错误消息，null 表示通过
        public final Label errorLabel;
        public Field(String label, Actor editor, Function<String, String> validator, Label errorLabel) {
            this.label = label; this.editor = editor;
            this.validator = validator; this.errorLabel = errorLabel;
        }
        /** 取控件当前文本（仅支持 TextField 系）。 */
        public String getText() {
            if (editor instanceof TextField) return ((TextField) editor).getText();
            return "";
        }
    }

    @Getter
    private final List<Field> fields = new ArrayList<>();
    private final float labelWidth;
    private final float editorWidth;
    private final float errorWidth;

    public BsForm(Skin skin) {
        this(skin, 100, 220, 180);
    }

    public BsForm(Skin skin, float labelWidth, float editorWidth, float errorWidth) {
        this.labelWidth = labelWidth;
        this.editorWidth = editorWidth;
        this.errorWidth = errorWidth;
        left();
        top();
        defaults().pad(4);
    }

    /** 添加一个带校验的 TextField 字段。 */
    public BsForm addField(String label, Actor editor, Function<String, String> validator) {
        Skin skin = BsUI.getSkin();
        Label labelL = new Label(label, skin);
        labelL.setColor(BsTheme.tp());

        // 错误 label：默认空 + 红色，靠左对齐
        Label error = new Label("", skin);
        error.setColor(BsPalette.DANGER.getMain());
        error.setAlignment(Align.left);

        Field f = new Field(label, editor, validator, error);
        fields.add(f);

        // 实时校验：TextField change 事件
        if (editor instanceof TextField) {
            ((TextField) editor).addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    validateField(f);
                }
            });
        }

        add(labelL).width(labelWidth).top().padTop(8);
        add(editor).width(editorWidth).left();
        add(error).width(errorWidth).left();
        row();
        return this;
    }

    /** 不带校验的字段。 */
    public BsForm addField(String label, Actor editor) {
        return addField(label, editor, null);
    }

    /** 校验单个字段，更新错误 label。返回 null=通过，非 null=错误消息。 */
    public String validateField(Field f) {
        String err = null;
        if (f.validator != null) {
            try { err = f.validator.apply(f.getText()); }
            catch (Throwable t) { err = "校验异常: " + t.getMessage(); }
        }
        f.errorLabel.setText(err == null ? "" : err);
        return err;
    }

    /** 校验所有字段，全部通过返回 true。 */
    public boolean validateAll() {
        boolean allOk = true;
        for (Field f : fields) {
            if (validateField(f) != null) allOk = false;
        }
        return allOk;
    }

    /** 收集所有字段值（按添加顺序）。 */
    public java.util.List<String> collectValues() {
        java.util.List<String> vals = new java.util.ArrayList<>();
        for (Field f : fields) vals.add(f.getText());
        return vals;
    }

    /** 添加提交/取消按钮栏。submitBefore 自动跑 {@link #validateAll()}。 */
    public BsForm addSubmitBar(String submitText, Runnable onSubmit,
                               String cancelText, Runnable onCancel) {
        Table bar = new Table();
        bar.defaults().pad(4);
        BsButton submit = new BsButton(submitText, BsUI.getSkin(),
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        submit.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (validateAll()) {
                    try { onSubmit.run(); } catch (Throwable t) { /* log */ }
                }
            }
        });
        BsButton cancel = new BsButton(cancelText, BsUI.getSkin(),
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.MD);
        cancel.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                try { onCancel.run(); } catch (Throwable t) { /* log */ }
            }
        });
        bar.add(submit);
        bar.add(cancel);
        // label 列留空
        add().width(labelWidth);
        add(bar).colspan(2).left();
        row();
        return this;
    }
}
