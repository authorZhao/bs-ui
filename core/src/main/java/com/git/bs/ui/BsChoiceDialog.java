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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

/**
 * 多选一对话框（基于 {@link BsModal}）：列出若干选项，用户点选后回调索引。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsChoiceDialog.show(stage, skin, "选择难度",
 *         java.util.Arrays.asList("简单", "普通", "困难"),
 *         index -> setStatus("选了第 " + index + " 项"));
 * }</pre>
 *
 * <p>每个选项是一个 {@link TextButton}（扁平 {@code bs-menu-item} style），
 * 点选后回调索引（0-based）并关闭弹窗。带分隔线 + 从上滑入动画。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsChoiceDialog extends BsModal {

    private final Consumer<Integer> onResult;

    public BsChoiceDialog(String title, String message, List<String> options, Skin skin,
                          Consumer<Integer> onResult) {
        super(title == null ? BsI18n.get("core.choose", "请选择") : title, skin);
        this.onResult = onResult;

        setTitleIcon(skin.newDrawable("white", new Color(0x66 / 255f, 0x10 / 255f, 0xF2 / 255f, 1f)));

        // 内容
        Table content = new Table();
        content.left().top();
        content.defaults().left().growX().padBottom(4);

        if (message != null && !message.isEmpty()) {
            Label msg = new Label(message, skin);
            msg.setColor(BsTheme.ts());
            msg.setWrap(true);
            content.add(msg).padBottom(10).row();
        }

        // 选项列表
        for (int i = 0; i < options.size(); i++) {
            final int idx = i;
            TextButton item = new TextButton(options.get(i), skin, "bs-menu-item");
            item.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    reply(idx);
                    close();
                }
            });
            content.add(item).height(32).row();
        }

        content(content).contentWidth(320);
        separator(true);

        // 底部"取消"按钮
        addButton(BsI18n.get("btn.cancel", "取消"), () -> reply(-1), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);

        closeOnBackdrop(true);
        setEnterAnimation(m -> BsAnimations.slideInDown(m, 0.28f));
        setExitAnimation((m, done) -> BsAnimations.fadeOut(m, 0.18f, done));
    }

    private void reply(int index) {
        if (onResult != null) {
            try { onResult.accept(index); } catch (Throwable t) { log.warn("onResult error", t); }
        }
    }

    /** 静态便捷入口。 */
    public static BsChoiceDialog show(Stage stage, Skin skin, String title,
                                      List<String> options, Consumer<Integer> onResult) {
        return show(stage, skin, title, null, options, onResult);
    }

    public static BsChoiceDialog show(Stage stage, Skin skin, String title, String message,
                                      List<String> options, Consumer<Integer> onResult) {
        BsChoiceDialog d = new BsChoiceDialog(title, message, options, skin, onResult);
        d.showModal(stage);
        return d;
    }
}
