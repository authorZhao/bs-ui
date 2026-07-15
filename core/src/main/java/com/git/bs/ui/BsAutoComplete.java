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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bootstrap 风格自动补全输入框（AutoComplete）—— 输入时实时过滤候选词并下拉显示。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsAutoComplete ac = new BsAutoComplete(skin);
 * ac.setCandidates(java.util.Arrays.asList(
 *     "Apple", "Banana", "Cherry", "Grape", "Orange", "Peach", "Pear"));
 * ac.setOnSelect(text -> setStatus("选了: " + text));
 * stage.addActor(ac);
 * }</pre>
 *
 * <p>实现：内部 = BsTextField + 下拉 popup（Table）。输入时 keyTyped 触发过滤，
 * 弹出 popup 显示匹配项，点击 popup 项填入输入框并关闭 popup。
 * 失焦（点外部）时关闭 popup。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsAutoComplete extends Table {

    private final BsTextField field;
    private final Table popup;
    private List<String> candidates = new ArrayList<>();
    private Consumer<String> onSelect;
    private int maxShown = 8;
    private float popupWidth = 240;

    public BsAutoComplete(Skin skin) {
        field = new BsTextField("", skin);
        add(field).width(popupWidth).row();
        // 弹出层（默认隐藏，加到 stage）
        popup = new Table(skin);
        popup.setBackground(skin.getDrawable("bs-window-bg"));
        popup.setVisible(false);
        popup.pad(4);
        popup.left().top();

        field.setTextFieldListener(new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                refreshPopup();
            }
        });

        // 失焦关闭：监听 stage touchDown（点在 field 外）
        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                // 暂留：可加聚焦样式
            }
        });
    }

    /** 候选词列表（全量）。 */
    public BsAutoComplete setCandidates(List<String> c) {
        this.candidates = c;
        return this;
    }

    public BsAutoComplete setOnSelect(Consumer<String> cb) { this.onSelect = cb; return this; }

    public BsAutoComplete setMaxShown(int n) { this.maxShown = n; return this; }

    public BsAutoComplete setPopupWidth(float w) {
        this.popupWidth = w;
        getCells().first().width(w);
        return this;
    }

    public BsTextField getField() { return field; }

    public String getText() { return field.getText(); }

    @Override
    protected void setStage(com.badlogic.gdx.scenes.scene2d.Stage stage) {
        super.setStage(stage);
        if (stage != null) {
            // 把 popup 加到 stage（位置在 field 下方）
            stage.addActor(popup);
            popup.toFront();
            // stage 级别的 touchDown 监听（点 field 外关闭 popup）
            stage.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if (!popup.isVisible()) return false;
                    // 检查点击是否在 field 或 popup 内
                    float fx = field.getX(), fy = field.getY();
                    float fw = field.getWidth(), fh = field.getHeight();
                    boolean inField = (x >= fx && x <= fx + fw && y >= fy && y <= fy + fh);
                    float px = popup.getX(), py = popup.getY();
                    float pw = popup.getWidth(), ph = popup.getHeight();
                    boolean inPopup = (x >= px && x <= px + pw && y >= py && y <= py + ph);
                    if (!inField && !inPopup) {
                        popup.setVisible(false);
                    }
                    return false;
                }
            });
        }
    }

    /** 根据当前输入刷新候选词并显示/隐藏 popup。 */
    private void refreshPopup() {
        Skin skin = BsUI.getSkin();
        String text = field.getText().trim().toLowerCase();
        popup.clearChildren();
        if (text.isEmpty()) {
            popup.setVisible(false);
            return;
        }
        List<String> matched = new ArrayList<>();
        for (String c : candidates) {
            if (matched.size() >= maxShown) break;
            if (c.toLowerCase().contains(text)) matched.add(c);
        }
        if (matched.isEmpty()) {
            popup.setVisible(false);
            return;
        }
        // 定位 popup（field 局部坐标转 stage 坐标）
        float x = field.getX();
        float y = field.getY();
        // 转换到 stage 坐标
        Actor p = field.getParent();
        while (p != null) {
            x += p.getX();
            y += p.getY();
            p = p.getParent();
        }
        popup.setPosition(x, y - 4);   // field 下方 4px
        for (String c : matched) {
            Label item = new Label(c, skin);
            item.setColor(BsTheme.tp());
            Container<Label> wrap = new Container<>(item);
            wrap.fill();
            wrap.pad(4, 8, 4, 8);
            wrap.setBackground(skin.getDrawable("bs-menu-item-up"));
            wrap.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            wrap.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    field.setText(c);
                    popup.setVisible(false);
                    if (onSelect != null) {
                        try { onSelect.accept(c); } catch (Throwable t) { log.warn("onSelect", t); }
                    }
                    return true;
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    wrap.setBackground(skin.getDrawable("bs-menu-item-hover"));
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    wrap.setBackground(skin.getDrawable("bs-menu-item-up"));
                }
            });
            popup.add(wrap).growX().pad(1).row();
        }
        popup.setSize(popupWidth, matched.size() * 28 + 8);
        popup.setVisible(true);
        popup.toFront();
    }

    @Override
    public boolean remove() {
        if (popup != null) popup.remove();
        return super.remove();
    }
}
