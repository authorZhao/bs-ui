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

import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bootstrap 风格时间轴（Timeline）—— 垂直列表 + 左侧时间节点，
 * 用于任务进度、消息记录、操作日志。
 *
 * <p>结构：</p>
 * <pre>
 *  ●─── 09:00  创建了任务
 *  │
 *  ●─── 10:30  分配给张三
 *  │
 *  ●─── 14:00  开始处理
 *  │
 *  ●─── 16:00  完成 ✓
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsTimeline tl = new BsTimeline(skin);
 * tl.addItem("09:00", "创建了任务", BsTimeline.Color.PRIMARY);
 * tl.addItem("10:30", "分配给张三", BsTimeline.Color.INFO);
 * tl.addItem("14:00", "开始处理", BsTimeline.Color.WARNING);
 * tl.addItem("16:00", "完成", BsTimeline.Color.SUCCESS);
 * stage.addActor(tl);
 *
 * // 自定义节点内容
 * tl.addItem(item -> item
 *         .time("now")
 *         .color(BsTimeline.Color.DANGER)
 *         .title("紧急修复")
 *         .subtitle("hotfix-branch"));
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsTimeline extends Table {

    public enum Color { PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO }

    private final List<Item> items = new ArrayList<>();
    private Consumer<Item> onClick;
    private float lineWidth = 2f;
    private float dotSize = 12f;

    public BsTimeline(Skin skin) {
        left().top();
        defaults().growX().left();
    }

    /** 添加一条时间项（简化版：时间 + 标题）。 */
    public BsTimeline addItem(String time, String title, Color color) {
        Item item = new Item(BsUI.getSkin());
        item.time(time).title(title).color(color);
        return addItem(item);
    }

    /** 添加一条时间项（builder 风格）。 */
    public BsTimeline addItem(Consumer<Item> config) {
        Item item = new Item(BsUI.getSkin());
        if (config != null) config.accept(item);
        return addItem(item);
    }

    private BsTimeline addItem(Item item) {
        items.add(item);
        item.build(dotSize, lineWidth);
        item.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                if (onClick != null) {
                    try { onClick.accept(item); } catch (Throwable t) { log.warn("onClick", t); }
                }
                return true;
            }
        });
        add(item).growX().padBottom(8).row();
        return this;
    }

    public BsTimeline setOnClick(Consumer<Item> cb) { this.onClick = cb; return this; }

    public BsTimeline setDotSize(float s) { this.dotSize = s; return this; }

    public BsTimeline setLineWidth(float w) { this.lineWidth = w; return this; }

    public List<Item> getItems() { return items; }

    // ========================= Item =========================

    /** 单条时间项。 */
    public static class Item extends Table {
        private String time;
        private String title;
        private String subtitle;
        private Color color = Color.PRIMARY;
        private Drawable iconOverride;
        private boolean last = false;

        public Item(Skin skin) { }

        public Item time(String t) { this.time = t; return this; }
        public Item title(String t) { this.title = t; return this; }
        public Item subtitle(String s) { this.subtitle = s; return this; }
        public Item color(Color c) { this.color = c; return this; }
        public Item icon(Drawable d) { this.iconOverride = d; return this; }

        public String getTitle() { return title; }
        public String getTime() { return time; }

        /** 构建：左侧（节点 + 竖线）+ 右侧（时间 + 标题）。 */
        void build(float dotSize, float lineWidth) {
            clearChildren();
            left().top();
            Skin skin = BsUI.getSkin();

            // 左侧列：圆点 + 竖线
            Table leftCol = new Table();
            leftCol.top().center();
            leftCol.defaults().growX();
            // 圆点
            com.badlogic.gdx.scenes.scene2d.ui.Label dotLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label("●", skin);
            dotLabel.setColor(toGdx(color));
            Container<com.badlogic.gdx.scenes.scene2d.ui.Label> dot = new Container<>(dotLabel);
            dot.size(dotSize);
            dot.setBackground(BsSkinFactory.drawableOf(toGdx(color)));
            leftCol.add(dot).size(dotSize).padTop(2).row();
            // 竖线（灰色细条）
            if (!last) {
                Container<?> line = new Container<>();
                line.setBackground(BsSkinFactory.drawableOf(new com.badlogic.gdx.graphics.Color(0xDE / 255f, 0xE2 / 255f, 0xE6 / 255f, 1f)));
                line.size(lineWidth, 30);
                leftCol.add(line).size(lineWidth, 30).padTop(2);
            }
            add(leftCol).width(dotSize + 8).top().padRight(8);

            // 右侧：时间 + 标题 + 副标题
            Table textCol = new Table();
            textCol.left().top();
            textCol.defaults().growX().left();
            Label.LabelStyle sm = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
            sm.font = skin.getFont("font-sm");
            if (time != null) {
                Label tl = new Label(time, sm);
                tl.setColor(new com.badlogic.gdx.graphics.Color(0x6C / 255f, 0x75 / 255f, 0x7D / 255f, 1f));
                textCol.add(tl).left().row();
            }
            if (title != null) {
                Label t = new Label(title, skin);
                t.setColor(new com.badlogic.gdx.graphics.Color(0.1f, 0.1f, 0.12f, 1f));
                textCol.add(t).left().row();
            }
            if (subtitle != null) {
                Label s = new Label(subtitle, sm);
                s.setColor(new com.badlogic.gdx.graphics.Color(0x49 / 255f, 0x50 / 255f, 0x57 / 255f, 1f));
                s.setWrap(true);
                textCol.add(s).growX().left().padTop(2);
            }
            add(textCol).growX().left();
        }
    }

    /** Timeline.Color → libgdx Color。 */
    private static com.badlogic.gdx.graphics.Color toGdx(Color c) {
        switch (c) {
            case PRIMARY:   return new com.badlogic.gdx.graphics.Color(0x0D / 255f, 0x6E / 255f, 0xFD / 255f, 1f);
            case SECONDARY: return new com.badlogic.gdx.graphics.Color(0x6C / 255f, 0x75 / 255f, 0x7D / 255f, 1f);
            case SUCCESS:   return new com.badlogic.gdx.graphics.Color(0x19 / 255f, 0x87 / 255f, 0x54 / 255f, 1f);
            case DANGER:    return new com.badlogic.gdx.graphics.Color(0xDC / 255f, 0x35 / 255f, 0x45 / 255f, 1f);
            case WARNING:   return new com.badlogic.gdx.graphics.Color(0xFF / 255f, 0xC1 / 255f, 0x07 / 255f, 1f);
            case INFO:      return new com.badlogic.gdx.graphics.Color(0x0D / 255f, 0xCA / 255f, 0xF0 / 255f, 1f);
        }
        return com.badlogic.gdx.graphics.Color.GRAY;
    }
}
