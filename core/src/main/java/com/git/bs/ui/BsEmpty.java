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

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.git.bs.i18n.BsI18n;

/**
 * Bootstrap 风格空状态（Empty State）—— 列表/搜索无结果时显示。
 *
 * <p>结构：[图标] + 标题 + 副描述 + 可选操作按钮。
 * 常用在 {@link BsTable}、{@link BsList}、{@link BsListGroup} 数据为空时替换内容。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsEmpty empty = new BsEmpty(skin)
 *         .icon(BsIcon.get("inbox"))
 *         .title("暂无数据")
 *         .description("点击下方按钮添加第一条记录")
 *         .actionButton("添加", () -> setStatus("点击了添加"));
 * stage.addActor(empty);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsEmpty extends Table {

    public BsEmpty(Skin skin) {
        center();
        defaults().center().padTop(6);
        // 图标占位（用大字 emoji 兜底，无 icon 时显示一个圆角灰底）
        Label iconLabel = new Label("📭", skin);
        iconLabel.setFontScale(2.5f);
        add(iconLabel).padBottom(8).row();

        Label title = new Label(BsI18n.get("core.empty", "暂无数据"), skin);
        title.setColor(BsTheme.tp());
        title.setFontScale(1.1f);
        add(title).row();

        Label desc = new Label(BsI18n.get("core.empty_desc", "没有符合条件的记录"), skin);
        desc.setColor(BsTheme.ts());
        desc.setFontScale(0.95f);
        add(desc).row();
    }

    /** 用图标 drawable 替换默认 emoji。 */
    public BsEmpty icon(Drawable d) {
        if (d == null) return this;
        // 清掉第一个 cell（emoji），改为 Image
        getCells().first().clearActor();
        com.badlogic.gdx.scenes.scene2d.ui.Image img = new com.badlogic.gdx.scenes.scene2d.ui.Image(d);
        img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        getCells().first().setActor(img);
        getCells().first().size(64, 64);
        return this;
    }

    public BsEmpty title(String t) {
        Label l = (Label) getCells().get(1).getActor();
        l.setText(t);
        return this;
    }

    public BsEmpty description(String d) {
        Label l = (Label) getCells().get(2).getActor();
        l.setText(d);
        return this;
    }

    /** 添加一个操作按钮（在描述下方）。 */
    public BsEmpty actionButton(String label, Runnable onClick) {
        BsButton btn = new BsButton(label, BsUI.getSkin(), BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        btn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                if (onClick != null) {
                    try { onClick.run(); } catch (Throwable ignored) {}
                }
                return true;
            }
        });
        add(btn).padTop(14).row();
        return this;
    }
}
