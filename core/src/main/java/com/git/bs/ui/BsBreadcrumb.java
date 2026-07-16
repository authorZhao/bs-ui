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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap 面包屑导航：Home › Users › 张三。每段是 {@link BsLink}（可点击），
 * 用 › 分隔。最后一段通常不点击（当前位置）。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsBreadcrumb bc = new BsBreadcrumb(skin)
 *         .addItem("首页", () -> goHome())
 *         .addItem("用户列表", () -> goUsers())
 *         .addCurrent("张三");   // 当前页（不可点）
 * stage.addActor(bc);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsBreadcrumb extends Table {

    /** 一条面包屑项：文本 + 点击回调 + 是否当前页。 */
    public static class Item {
        public final String text;
        public final Runnable onClick;
        public final boolean current;
        public Item(String t, Runnable r, boolean c) { text = t; onClick = r; current = c; }
    }

    private final List<Item> items = new ArrayList<>();
    /** 分隔符颜色（淡灰）。每次取最新主题的 textMuted。 */
    private Color sepColor() { return BsTheme.tm(); }
    /** 当前页文字颜色（深色）。每次取最新主题的 textPrimary。 */
    private Color currentColor() { return BsTheme.tp(); }

    public BsBreadcrumb(Skin skin) {
        left();
        defaults().pad(0, 4, 0, 4).center();
    }

    /** 加一段（可点击的中间项）。 */
    public BsBreadcrumb addItem(String text, Runnable onClick) {
        items.add(new Item(text, onClick, false));
        rebuild();
        return this;
    }

    /** 加第一段（通常是首页，可点击）。 */
    public BsBreadcrumb addRoot(String text, Runnable onClick) {
        return addItem(text, onClick);
    }

    /** 加当前段（不可点击，深色文字）。 */
    public BsBreadcrumb addCurrent(String text) {
        items.add(new Item(text, null, true));
        rebuild();
        return this;
    }

    /** 清空所有项。 */
    public BsBreadcrumb clearItems() {
        items.clear();
        rebuild();
        return this;
    }

    /** 全量替换。 */
    public BsBreadcrumb setItems(List<Item> newItems) {
        items.clear();
        items.addAll(newItems);
        rebuild();
        return this;
    }

    private void rebuild() {
        clearChildren();
        Skin skin = BsUI.getSkin();
        for (int i = 0; i < items.size(); i++) {
            final Item it = items.get(i);
            if (i > 0) {
                // 分隔符 ›
                Label sep = makeLabel("›", sepColor(), "default");
                add(sep).padLeft(4).padRight(4);
            }
            if (it.current) {
                // 当前页：深色不可点
                Label l = makeLabel(it.text, currentColor(), "default");
                add(l);
            } else {
                // 链接
                BsLink link = new BsLink(it.text, skin);
                link.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        try { if (it.onClick != null) it.onClick.run(); } catch (Throwable t) { log.warn("bc onClick", t); }
                    }
                });
                add(link);
            }
        }
    }

    private Label makeLabel(String text, Color color, String fontKey) {
        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = BsUI.getSkin().getFont(fontKey);
        ls.fontColor = color;
        Label l = new Label(text, ls);
        l.setColor(Color.WHITE);
        return l;
    }
}
