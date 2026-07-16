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
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bootstrap 风格标签输入（Tag Input / Chips）——
 * 按回车把输入变成"胶囊"chip，每个 chip 可点 × 删除。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsTagInput tags = new BsTagInput(skin);
 * tags.setPlaceholder("输入标签后回车");
 * tags.addTag("Java");
 * tags.addTag("libgdx");
 * tags.setOnChange(list -> setStatus("当前标签: " + list));
 * stage.addActor(tags);
 * }</pre>
 *
 * <p>实现：横向 wrap Table，每行 = [chip][chip][chip] [输入框]。
 * 输入回车时把文本变成 chip 插入，清空输入框。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsTagInput extends Table {

    private final BsTextField field;
    private final Table chipsWrap;
    private final List<String> tags = new ArrayList<>();
    private Consumer<List<String>> onChange;
    private float fieldWidth = 120;
    /** V2：颜色存放在 skin，字段初始化时无法访问 skin，先 null，构造中赋值。 */
    private Color chipColor;

    public BsTagInput(Skin skin) {
        this.chipColor = BsPalette.PRIMARY.getMain();
        setBackground(skin.getDrawable("bs-text-field-bg"));
        pad(6, 8, 6, 8);
        left().top();
        chipsWrap = new Table();
        chipsWrap.left();
        add(chipsWrap).left();
        field = new BsTextField("", skin);
        field.setMessageText(BsI18n.get("core.tag.placeholder", "输入后回车"));
        // 让 TextField 不带自己的边框背景
        field.setStyle(skin.get(com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle.class));
        add(field).width(fieldWidth);

        field.setTextFieldListener(new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                if (c == '\n' || c == '\r') {
                    String t = textField.getText().trim();
                    if (!t.isEmpty() && !tags.contains(t)) {
                        addTag(t);
                    }
                    textField.setText("");
                }
            }
        });
    }

    /** 添加一个 chip。 */
    public BsTagInput addTag(String t) {
        if (t == null || t.isEmpty() || tags.contains(t)) return this;
        tags.add(t);
        rebuildChips();
        notifyChange();
        return this;
    }

    public BsTagInput addTags(List<String> ts) {
        for (String t : ts) {
            if (t != null && !t.isEmpty() && !tags.contains(t)) tags.add(t);
        }
        rebuildChips();
        notifyChange();
        return this;
    }

    /** 移除一个 chip。 */
    public BsTagInput removeTag(String t) {
        if (tags.remove(t)) {
            rebuildChips();
            notifyChange();
        }
        return this;
    }

    public List<String> getTags() { return tags; }

    public BsTagInput setPlaceholder(String p) {
        field.setMessageText(p);
        return this;
    }

    public BsTagInput setOnChange(Consumer<List<String>> cb) { this.onChange = cb; return this; }

    public BsTagInput setChipColor(Color c) { this.chipColor = c; rebuildChips(); return this; }

    public BsTagInput setFieldWidth(float w) {
        this.fieldWidth = w;
        getCells().get(1).width(w);
        return this;
    }

    private void rebuildChips() {
        Skin skin = BsUI.getSkin();
        chipsWrap.clearChildren();
        for (String t : tags) {
            // chip = [文字] [×]
            Table chip = new Table();
            chip.setBackground(skin.newDrawable("white", chipColor));
            chip.pad(2, 6, 2, 6);
            Label.LabelStyle cs = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
            cs.font = skin.getFont("font-sm");
            Label l = new Label(t, cs);
            l.setColor(Color.WHITE);
            chip.add(l).padRight(4);
            // × 关闭按钮
            Label x = new Label("×", cs);
            x.setColor(Color.WHITE);
            Container<Label> xWrap = new Container<>(x);
            xWrap.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            final String tag = t;
            xWrap.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    removeTag(tag);
                }
            });
            chip.add(xWrap);
            chipsWrap.add(chip).pad(2).row();
        }
    }

    private void notifyChange() {
        if (onChange != null) {
            try { onChange.accept(new ArrayList<>(tags)); } catch (Throwable t) { log.warn("onChange", t); }
        }
    }
}
