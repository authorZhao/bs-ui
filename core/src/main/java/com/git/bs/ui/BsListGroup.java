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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bootstrap 5 风格列表组（List Group）—— 带样式的纵向条目列表。
 *
 * <p>与 {@link BsList}（纯选择列表）的区别：List Group 每项可自定义内容，
 * 支持图标/角标/副标题/禁用/点击操作/选中态，更接近 Bootstrap list-group-item 的语义。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsListGroup group = new BsListGroup(skin);
 * group.addItem(item -> item
 *         .icon(BsIcon.get("envelope"))
 *         .title("收件箱")
 *         .subtitle("12 条未读")
 *         .badge("12")
 *         .onClick(() -> openInbox()));
 * group.addItem(item -> item
 *         .title("已发送")
 *         .disabled(true));
 * stage.addActor(group);
 * }</pre>
 *
 * <p>实现：每个 item 是一个 Table 横向布局 [图标?] [标题/副标题] [badge?]。
 * hover 浅蓝底，active 蓝底白字，disabled 灰字不可点。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsListGroup extends Table {

    private final List<Item> items = new ArrayList<>();
    private int selectedIdx = -1;
    private Consumer<Integer> onSelect;
    private float itemHeight = 44;
    private boolean flush = false;   // flush 样式：无圆角无间距

    public BsListGroup(Skin skin) {
        left().top();
        defaults().growX();
    }

    /** 添加一项，通过 consumer 配置内容。 */
    public BsListGroup addItem(Consumer<Item> config) {
        Item item = new Item(BsUI.getSkin(), items.size());
        if (config != null) config.accept(item);
        items.add(item);
        item.build();
        // 选中/点击监听
        item.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (item.disabled) return;
                select(item.index);
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (!item.disabled && pointer == -1) item.applyHoverStyle();
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (!item.disabled) item.applyIdleStyle(selectedIdx == item.index);
            }
        });
        add(item).growX().height(itemHeight).padBottom(flush ? 0 : 2).row();
        return this;
    }

    /** 选中指定索引（高亮 + 触发回调）。 */
    public BsListGroup select(int idx) {
        if (idx < 0 || idx >= items.size()) return this;
        int old = selectedIdx;
        if (old >= 0 && old < items.size()) items.get(old).applyIdleStyle(false);
        selectedIdx = idx;
        items.get(idx).applyActiveStyle();
        if (onSelect != null) {
            try { onSelect.accept(idx); } catch (Throwable t) { log.warn("onSelect", t); }
        }
        return this;
    }

    public int getSelectedIndex() { return selectedIdx; }

    public BsListGroup setOnSelect(Consumer<Integer> cb) {
        this.onSelect = cb;
        return this;
    }

    public BsListGroup setItemHeight(float h) {
        this.itemHeight = h;
        return this;
    }

    /** flush 样式：项之间无间距、无圆角（贴在一起）。 */
    public BsListGroup setFlush(boolean f) {
        this.flush = f;
        return this;
    }

    public List<Item> getItems() { return items; }

    public Item getItem(int idx) {
        if (idx < 0 || idx >= items.size()) return null;
        return items.get(idx);
    }

    // ========================= Item =========================

    /** 单个列表项。 */
    public static class Item extends Table {
        private final int index;
        private Drawable icon;
        private String title;
        private String subtitle;
        private String badge;
        private Color badgeColor;
        private boolean disabled = false;
        private Runnable onClick;
        private final Table contentRow = new Table();

        public Item(Skin skin, int index) {
            this.index = index;
            left();
            pad(8, 12, 8, 12);
            add(contentRow).growX();
        }

        public Item icon(Drawable d) { this.icon = d; return this; }
        public Item title(String t) { this.title = t; return this; }
        public Item subtitle(String s) { this.subtitle = s; return this; }
        public Item badge(String b) { this.badge = b; return this; }
        public Item badgeColor(Color c) { this.badgeColor = c; return this; }
        public Item disabled(boolean d) { this.disabled = d; setTouchable(d ? Touchable.disabled : Touchable.enabled); return this; }
        public Item onClick(Runnable r) { this.onClick = r; return this; }

        /** 构造内部内容（标题/副标题/图标/badge）。 */
        void build() {
            Skin skin = BsUI.getSkin();
            contentRow.clearChildren();
            contentRow.left();
            // 图标
            if (icon != null) {
                Image img = new Image(icon);
                img.setScaling(Scaling.fit);
                contentRow.add(img).size(20).padRight(10).left();
            }
            // 标题 + 副标题
            Table textCol = new Table();
            textCol.left();
            if (title != null) {
                Label t = new Label(title, skin);
                t.setColor(disabled ? BsTheme.tm() : BsTheme.tp());
                textCol.add(t).left().row();
            }
            if (subtitle != null) {
                Label s = new Label(subtitle, skin);
                s.setColor(BsTheme.ts());
                s.setFontScale(0.9f);
                textCol.add(s).left();
            }
            contentRow.add(textCol).growX().left();

            // badge
            if (badge != null && !badge.isEmpty()) {
                Container<Label> badgeWrap = new Container<>();
                badgeWrap.setBackground(skin.newDrawable("white",
                        badgeColor != null ? badgeColor : BsPalette.DANGER.getMain()));
                badgeWrap.pad(2, 8, 2, 8);
                Label bl = new Label(badge, skin);
                bl.setColor(Color.WHITE);
                bl.setFontScale(0.85f);
                badgeWrap.setActor(bl);
                contentRow.add(badgeWrap).padLeft(8).right();
            }

            applyIdleStyle(false);
        }

        void applyIdleStyle(boolean active) {
            if (active) {
                applyActiveStyle();
            } else {
                setBackground(BsUI.getSkin().getDrawable("bs-window-bg"));
            }
        }

        void applyHoverStyle() {
            setBackground(BsUI.getSkin().getDrawable("bs-menu-title-hover"));
        }

        void applyActiveStyle() {
            setBackground(BsUI.getSkin().getDrawable("bs-list-selection"));
        }
    }
}
