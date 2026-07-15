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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.git.bs.i18n.BsI18n;

/**
 * Bootstrap 风格结果页（Result）—— 整页的成功/失败/警告，带图标+标题+描述+操作按钮。
 *
 * <p>结构：</p>
 * <pre>
 *       [✓ 大图标]
 *      成功提交
 *   您的申请已成功提交
 *   [返回首页] [查看详情]
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsResult result = new BsResult(skin, BsResult.Type.SUCCESS)
 *         .title("提交成功")
 *         .description("您的申请已成功提交，我们将在 3 个工作日内审核")
 *         .primaryButton("返回首页", () -> goHome())
 *         .secondaryButton("查看详情", () -> viewDetail());
 * stage.addActor(result);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsResult extends Table {

    public enum Type { SUCCESS, WARNING, ERROR, INFO }

    private final Label iconLabel;
    private final Label titleLabel;
    private final Label descLabel;
    private final Table buttonRow;

    public BsResult(Skin skin) {
        this(skin, Type.INFO);
    }

    public BsResult(Skin skin, Type type) {
        center();
        defaults().center().padTop(6);

        // 大图标
        iconLabel = new Label(iconChar(type), skin);
        iconLabel.setColor(colorFor(skin, type));
        iconLabel.setFontScale(4f);
        add(iconLabel).padBottom(10).row();

        // 标题
        titleLabel = new Label(defaultTitle(type), skin);
        titleLabel.setColor(BsTheme.tp());
        titleLabel.setFontScale(1.4f);
        add(titleLabel).padTop(8).row();

        // 描述
        descLabel = new Label("", skin);
        descLabel.setColor(BsPalette.SECONDARY.getMain());
        descLabel.setWrap(true);
        descLabel.setAlignment(1);   // center
        add(descLabel).growX().padTop(8).width(420).row();

        // 按钮行
        buttonRow = new Table();
        buttonRow.defaults().pad(6);
        add(buttonRow).padTop(18).row();
    }

    public BsResult setType(Type t) {
        iconLabel.setText(iconChar(t));
        iconLabel.setColor(colorFor(BsUI.getSkin(), t));
        if (titleLabel.getText().toString().isEmpty()
                || isDefaultTitle(titleLabel.getText().toString())) {
            titleLabel.setText(defaultTitle(t));
        }
        return this;
    }

    public BsResult title(String t) {
        titleLabel.setText(t);
        return this;
    }

    public BsResult description(String d) {
        descLabel.setText(d == null ? "" : d);
        return this;
    }

    public BsResult primaryButton(String label, Runnable onClick) {
        BsButton btn = new BsButton(label, BsUI.getSkin(), BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        attachClick(btn, onClick);
        buttonRow.add(btn);
        return this;
    }

    public BsResult secondaryButton(String label, Runnable onClick) {
        return secondaryButton(label, onClick, BsButton.Variant.SECONDARY);
    }

    public BsResult secondaryButton(String label, Runnable onClick, BsButton.Variant variant) {
        BsButton btn = new BsButton(label, BsUI.getSkin(), variant, BsButton.Style.OUTLINE, BsButton.Size.MD);
        attachClick(btn, onClick);
        buttonRow.add(btn);
        return this;
    }

    private void attachClick(BsButton btn, Runnable onClick) {
        btn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                if (onClick != null) {
                    try { onClick.run(); } catch (Throwable ignored) {}
                }
                return true;
            }
        });
    }

    /** 图标字符（emoji 兜底）。 */
    private static String iconChar(Type t) {
        switch (t) {
            case SUCCESS: return "✓";
            case WARNING: return "!";
            case ERROR:   return "×";
            case INFO:    return "i";
        }
        return "?";
    }

    public static Color colorFor(Skin skin, Type t) {
        switch (t) {
            case SUCCESS: return BsPalette.SUCCESS.getMain();
            case WARNING: return BsPalette.WARNING.getMain();
            case ERROR:   return BsPalette.DANGER.getMain();
            case INFO:    return BsPalette.PRIMARY.getMain();
        }
        return Color.GRAY;
    }

    private static String defaultTitle(Type t) {
        switch (t) {
            case SUCCESS: return BsI18n.get("core.result.success", "操作成功");
            case WARNING: return BsI18n.get("core.result.warning", "请注意");
            case ERROR:   return BsI18n.get("core.result.error", "操作失败");
            case INFO:    return BsI18n.get("core.result.info", "信息");
        }
        return "";
    }

    private static boolean isDefaultTitle(String t) {
        return BsI18n.get("core.result.success", "操作成功").equals(t)
                || BsI18n.get("core.result.warning", "请注意").equals(t)
                || BsI18n.get("core.result.error", "操作失败").equals(t)
                || BsI18n.get("core.result.info", "信息").equals(t);
    }
}
