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

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bootstrap 风格穿梭框（Transfer）—— 左右双列，可相互移动选中项。
 * 常用于权限分配、角色绑定、字段选择等场景。
 *
 * <p>结构：</p>
 * <pre>
 * ┌─────────┐  [→][→]  ┌─────────┐
 * │ 可选列表 │  [←][←]  │ 已选列表 │
 * │  □ A    │          │  □ X    │
 * │  □ B    │          │  □ Y    │
 * └─────────┘          └─────────┘
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsTransfer t = new BsTransfer(skin);
 * t.setLeftTitle("可选权限");
 * t.setRightTitle("已授权");
 * t.setOptions("read", "write", "delete", "admin", "audit");
 * t.setSelected(java.util.Arrays.asList("read", "write"));
 * t.setOnChange(selected -> setStatus("已选: " + selected));
 * stage.addActor(t);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsTransfer extends Table {

    private final Table leftPanel;
    private final Table rightPanel;
    private final BsScrollPane leftScroll;
    private final BsScrollPane rightScroll;
    private final Label leftCount;
    private final Label rightCount;

    private List<String> options = new ArrayList<>();
    /** 选项是否在「已选」侧的标记。 */
    private final BitSet selected = new BitSet();
    /** 左侧列表项的选中（待移动）标记。 */
    private final BitSet leftMarked = new BitSet();
    /** 右侧列表项的选中（待移动）标记。 */
    private final BitSet rightMarked = new BitSet();

    private Consumer<List<String>> onChange;

    public BsTransfer(Skin skin) {
        left().top();
        defaults().top();

        // 左面板（可选）
        leftPanel = makePanel();
        leftScroll = new BsScrollPane(leftPanel, skin);
        leftScroll.setFadeScrollBars(false);
        leftScroll.setScrollingDisabled(true, false);
        Table leftWrap = makeColumn(BsI18n.get("core.transfer.available", "可选"), leftScroll, leftCount = makeCountLabel());

        // 中间按钮
        Table middle = makeMiddleButtons();

        // 右面板（已选）
        rightPanel = makePanel();
        rightScroll = new BsScrollPane(rightPanel, skin);
        rightScroll.setFadeScrollBars(false);
        rightScroll.setScrollingDisabled(true, false);
        Table rightWrap = makeColumn(BsI18n.get("core.transfer.selected", "已选"), rightScroll, rightCount = makeCountLabel());

        add(leftWrap).width(180).height(220);
        add(middle).pad(8);
        add(rightWrap).width(180).height(220);
    }

    private Table makePanel() {
        Table t = new Table();
        t.left().top();
        t.defaults().growX().left();
        return t;
    }

    private Label makeCountLabel() {
        Label l = new Label("(0)", BsUI.getSkin());
        l.setColor(BsPalette.SECONDARY.getMain());
        l.setFontScale(0.85f);
        return l;
    }

    private Table makeColumn(String title, com.badlogic.gdx.scenes.scene2d.Actor body, Label count) {
        Skin skin = BsUI.getSkin();
        Table wrap = new Table();
        wrap.top().left();
        wrap.defaults().growX().left();
        // 标题行
        Table titleRow = new Table();
        titleRow.left();
        titleRow.setBackground(skin.getDrawable("bs-menu-bar-bg"));
        titleRow.pad(6, 8, 6, 8);
        Label t = new Label(title, skin);
        t.setColor(BsTheme.tp());
        t.setFontScale(0.95f);
        titleRow.add(t).left();
        titleRow.add(count).right().padLeft(8);
        wrap.add(titleRow).growX().row();
        // body
        wrap.add(body).grow().row();
        return wrap;
    }

    private Table makeMiddleButtons() {
        Skin skin = BsUI.getSkin();
        Table m = new Table();
        m.center();
        m.defaults().pad(2);
        BsButton toRight = new BsButton("添加 →", skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.SM);
        toRight.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { moveToRight(); }
        });
        BsButton toLeft = new BsButton("← 移除", skin, BsButton.Variant.SECONDARY, BsButton.Style.SOLID, BsButton.Size.SM);
        toLeft.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { moveToLeft(); }
        });
        m.add(toRight).row();
        m.add(toLeft);
        return m;
    }

    // ========================= 数据 =========================

    public BsTransfer setOptions(List<String> opts) {
        this.options = new ArrayList<>(opts);
        selected.clear();
        leftMarked.clear();
        rightMarked.clear();
        rebuild();
        return this;
    }

    public BsTransfer setOptions(String... opts) {
        return setOptions(java.util.Arrays.asList(opts));
    }

    public BsTransfer setSelected(List<String> sel) {
        selected.clear();
        for (String s : sel) {
            int idx = options.indexOf(s);
            if (idx >= 0) selected.set(idx);
        }
        rebuild();
        notifyChange();
        return this;
    }

    public List<String> getSelected() {
        List<String> r = new ArrayList<>();
        for (int i = selected.nextSetBit(0); i >= 0; i = selected.nextSetBit(i + 1)) {
            if (i < options.size()) r.add(options.get(i));
        }
        return r;
    }

    public BsTransfer setOnChange(Consumer<List<String>> cb) { this.onChange = cb; return this; }

    // ========================= 移动 =========================

    private void moveToRight() {
        for (int i = leftMarked.nextSetBit(0); i >= 0; i = leftMarked.nextSetBit(i + 1)) {
            selected.set(i);
        }
        leftMarked.clear();
        rebuild();
        notifyChange();
    }

    private void moveToLeft() {
        for (int i = rightMarked.nextSetBit(0); i >= 0; i = rightMarked.nextSetBit(i + 1)) {
            selected.clear(i);
        }
        rightMarked.clear();
        rebuild();
        notifyChange();
    }

    private void notifyChange() {
        if (onChange != null) {
            try { onChange.accept(getSelected()); } catch (Throwable t) { log.warn("onChange", t); }
        }
    }

    // ========================= 重建 =========================

    private void rebuild() {
        // 左侧（未选）
        leftPanel.clearChildren();
        int leftCount = 0;
        for (int i = 0; i < options.size(); i++) {
            if (selected.get(i)) continue;
            addRow(leftPanel, options.get(i), i, leftMarked);
            leftCount++;
        }
        this.leftCount.setText("(" + leftCount + ")");

        // 右侧（已选）
        rightPanel.clearChildren();
        int rightCount = 0;
        for (int i = 0; i < options.size(); i++) {
            if (!selected.get(i)) continue;
            addRow(rightPanel, options.get(i), i, rightMarked);
            rightCount++;
        }
        this.rightCount.setText("(" + rightCount + ")");
    }

    private void addRow(Table panel, String text, int idx, BitSet markSet) {
        Skin skin = BsUI.getSkin();
        final int i = idx;
        final BitSet marks = markSet;
        // 用 Image + bs-check-off / bs-check-on drawable 替代字体字符
        com.badlogic.gdx.scenes.scene2d.ui.Image box = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                skin.getDrawable("bs-check-off"));
        box.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        Label l = new Label(text, skin);
        l.setColor(BsTheme.tp());
        Table row = new Table();
        row.left();
        row.defaults().pad(2).left();
        row.add(box).size(18).padRight(6);
        row.add(l).growX().left();
        row.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        row.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                boolean m = marks.get(i);
                marks.set(i, !m);
                // 切换 drawable
                box.setDrawable(skin.getDrawable(m ? "bs-check-off" : "bs-check-on"));
                // 整行背景也跟着变（选中态高亮）
                row.setBackground(m ? null : skin.getDrawable("bs-menu-item-hover"));
            }
            @Override public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (!marks.get(i)) row.setBackground(skin.getDrawable("bs-menu-title-hover"));
            }
            @Override public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                if (!marks.get(i)) row.setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);
            }
        });
        panel.add(row).growX().pad(2, 4, 2, 4).row();
    }
}
