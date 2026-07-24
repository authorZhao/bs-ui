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

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * 文本输入对话框（基于 {@link BsModal}）：提示文本 + 单行输入框 + 确定/取消。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsPromptDialog.show(stage, skin, "新建项目", "请输入项目名：", "my-project", text -> {
 *     if (text != null) {  // null 表示用户取消
 *         createProject(text);
 *     }
 * });
 * }</pre>
 *
 * <p>"确定"按钮自动 trim 输入并校验非空（空字符串也会作为 null 回调）。
 * 取消（或点 ×）回调 null。带分隔线 + 缩放进入动画。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsPromptDialog extends BsModal {

    private final BsTextField textField;
    private final Consumer<String> onResult;
    private final Label errorLabel;

    public BsPromptDialog(String title, String message, String initialText, Skin skin,
                          Consumer<String> onResult) {
        super(title == null ? BsI18n.get("dialog.input_title", "请输入") : title, skin);
        this.onResult = onResult;

        Drawable pIcon = BsIcon.get("keyboard", BsPalette.SUCCESS.getMain());
        setTitleIcon(pIcon != null ? pIcon : skin.newDrawable("white", BsPalette.SUCCESS.getMain()));

        // 内容：消息 + 输入框 + 错误提示
        com.badlogic.gdx.scenes.scene2d.ui.Table content = new com.badlogic.gdx.scenes.scene2d.ui.Table();
        content.left().top();
        content.defaults().left().padBottom(6);

        if (message != null && !message.isEmpty()) {
            Label msg = new Label(message, skin);
            msg.setColor(BsTheme.tp());
            msg.setWrap(true);
            content.add(msg).growX().row();
        }

        textField = new BsTextField(initialText == null ? "" : initialText, skin);
        content.add(textField).growX().width(360).row();

        errorLabel = new Label("", skin);
        errorLabel.setColor(BsPalette.DANGER.getMain());
        content.add(errorLabel).growX().left();

        content(content).contentWidth(400);
        separator(true);

        addButton(BsI18n.get("btn.cancel", "取消"), () -> reply(null), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        addButton(BsI18n.get("btn.ok", "确定"), this::confirm, BsButton.Variant.PRIMARY, BsButton.Style.SOLID);

        closeOnBackdrop(false);
        setEnterAnimation(m -> BsAnimations.scaleIn(m, 0.24f));
        setExitAnimation((m, done) -> BsAnimations.fadeOut(m, 0.18f, done));
    }

    private void confirm() {
        String text = textField.getText();
        text = text == null ? "" : text.trim();
        if (text.isEmpty()) {
            errorLabel.setText(BsI18n.get("prompt.empty_error", "输入不能为空"));
            return;
        }
        reply(text);
    }

    private void reply(String result) {
        if (onResult != null) {
            try { onResult.accept(result); } catch (Throwable t) { log.warn("onResult error", t); }
        }
    }

    /** 静态便捷入口。 */
    public static BsPromptDialog show(Stage stage, Skin skin, String title, String message,
                                      String initialText, Consumer<String> onResult) {
        BsPromptDialog d = new BsPromptDialog(title, message, initialText, skin, onResult);
        d.showModal(stage);
        // 弹出后自动聚焦输入框
        stage.setKeyboardFocus(d.textField);
        return d;
    }
}
