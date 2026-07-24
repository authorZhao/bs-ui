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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * 确认对话框（基于 {@link BsModal}）：问号图标 + 问题文本 + 是/否按钮。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsConfirmDialog.show(stage, skin, "确认删除？", "此操作不可撤销，是否继续？", ok -> {
 *     if (ok) doDelete();
 * });
 * }</pre>
 *
 * <p>"是"按钮用 PRIMARY 蓝；"否"按钮用 SECONDARY 描边。
 * 默认带分隔线 + 标题图标（问号色块）+ 淡入动画。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsConfirmDialog extends BsModal {

    private final Consumer<Boolean> onResult;

    public BsConfirmDialog(String title, String message, Skin skin, Consumer<Boolean> onResult) {
        super(title == null ? BsI18n.get("dialog.confirm_title", "请确认") : title, skin);
        this.onResult = onResult;

        // 标题前问号图标（蓝色，象征询问）——优先用 atlas 图标，回退色块
        Drawable qIcon = BsIcon.get("question-circle", BsPalette.PRIMARY.getMain());
        setTitleIcon(qIcon != null ? qIcon : BsUI.getSkin().newDrawable("white", BsPalette.PRIMARY.getMain()));

        // 内容
        Label msg = new Label(message == null ? "" : message, skin);
        msg.setColor(BsTheme.tp());
        msg.setWrap(true);
        content(msg).contentWidth(380);

        separator(true);

        // 是 / 否
        addButton(BsI18n.get("btn.no", "否"), () -> reply(false), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        addButton(BsI18n.get("btn.yes", "是"), () -> reply(true), BsButton.Variant.PRIMARY, BsButton.Style.SOLID);

        closeOnBackdrop(false);  // 确认框点背景不关，强制用户选择
        setEnterAnimation(m -> BsAnimations.fadeIn(m, 0.22f));
        setExitAnimation((m, done) -> BsAnimations.fadeOut(m, 0.18f, done));
    }

    private void reply(boolean ok) {
        if (onResult != null) {
            try { onResult.accept(ok); } catch (Throwable t) { log.warn("onResult error", t); }
        }
    }

    /** 静态便捷入口。 */
    public static BsConfirmDialog show(Stage stage, Skin skin, String title, String message,
                                       Consumer<Boolean> onResult) {
        BsConfirmDialog d = new BsConfirmDialog(title, message, skin, onResult);
        d.showModal(stage);
        return d;
    }
}
