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
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bootstrap 风格属性编辑器（Property Sheet / Inspector）——
 * 像 Unity Inspector / 通用编辑器的属性面板，左 key 右 value，
 * value 类型驱动编辑控件（文本/数字/颜色/下拉/开关）。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsPropertySheet sheet = new BsPropertySheet(skin);
 * sheet.addProperty("name", "John Doe", BsPropertySheet.Type.TEXT);
 * sheet.addProperty("age", 28, BsPropertySheet.Type.NUMBER);
 * sheet.addProperty("color", Color.valueOf("#FFC107"), BsPropertySheet.Type.COLOR);
 * sheet.addProperty("role", "Admin", BsPropertySheet.Type.SELECT, "Admin", "User", "Guest");
 * sheet.addProperty("enabled", true, BsPropertySheet.Type.BOOLEAN);
 * sheet.addSection("基本", sub -> sub.addProperty("id", 1001, Type.NUMBER));
 * sheet.setOnChange((key, value) -> setStatus(key + " → " + value));
 * stage.addActor(sheet);
 * }</pre>
 *
 * <p>实现：纵向 Table，每行 = [Label 名字 | 控件]，
 * section 用分组标题行分隔。控件类型对应：TEXT→BsTextField、NUMBER→BsInputNumber、
 * COLOR→色块按钮点开 BsColorPicker、SELECT→BsSelectBox、BOOLEAN→BsSwitch。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsPropertySheet extends Table {

    public enum Type { TEXT, NUMBER, COLOR, SELECT, BOOLEAN, READONLY }

    private final List<Property> properties = new ArrayList<>();
    private PropertyChangeListener onChange;
    private float labelWidth = 100;
    private float valueWidth = 180;

    public interface PropertyChangeListener {
        void onChange(String key, Object newValue);
    }

    public BsPropertySheet(Skin skin) {
        left().top();
        defaults().growX().left();
        pad(8);
    }

    public BsPropertySheet setLabelWidth(float w) { this.labelWidth = w; return this; }
    public BsPropertySheet setValueWidth(float w) { this.valueWidth = w; return this; }
    public BsPropertySheet setOnChange(PropertyChangeListener cb) { this.onChange = cb; return this; }

    /** 添加一个分组标题（视觉分隔）。 */
    public BsPropertySheet addSection(String title) {
        Label t = new Label(title, BsUI.getSkin());
        t.setColor(BsPalette.SECONDARY.getMain());
        t.setFontScale(0.95f);
        Container<Label> wrap = new Container<>(t);
        wrap.fill();
        wrap.pad(8, 0, 4, 0);
        add(wrap).growX().colspan(2).row();
        return this;
    }

    /** 文本属性。 */
    public Property addProperty(String name, String value) {
        return addProperty(name, value, Type.TEXT);
    }

    /** 整数属性。 */
    public Property addProperty(String name, int value) {
        return addProperty(name, (long) value, Type.NUMBER);
    }

    /** 浮点属性。 */
    public Property addProperty(String name, float value) {
        return addProperty(name, (double) value, Type.NUMBER);
    }

    /** 布尔属性。 */
    public Property addProperty(String name, boolean value) {
        return addProperty(name, value, Type.BOOLEAN);
    }

    /** 颜色属性。 */
    public Property addProperty(String name, Color value) {
        return addProperty(name, value, Type.COLOR);
    }

    /**
     * 添加一个属性（最通用的入口）。
     * @param name 左侧显示名
     * @param value 初始值（String/Number/Boolean/Color）
     * @param type 编辑控件类型
     * @param options 仅 Type.SELECT 时使用，可选项列表
     */
    public Property addProperty(String name, Object value, Type type, String... options) {
        Property p = new Property(BsUI.getSkin(), name, value, type, options);
        p.sheet = this;
        properties.add(p);
        add(p.nameLabel).width(labelWidth).pad(6, 4, 6, 8).left();
        add(p.valueWrap).width(valueWidth).pad(6, 4, 6, 4).left().row();
        return p;
    }

    /** 触发变更回调。 */
    void fireChange(String key, Object newValue) {
        if (onChange != null) {
            try { onChange.onChange(key, newValue); } catch (Throwable t) { log.warn("onChange", t); }
        }
    }

    public Property getProperty(String name) {
        for (Property p : properties) if (p.name.equals(name)) return p;
        return null;
    }

    /** 获取所有属性当前值（key → value）。 */
    public Map<String, Object> collectValues() {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Property p : properties) m.put(p.name, p.getValue());
        return m;
    }

    /** 清空所有属性。 */
    public BsPropertySheet clearProperties() {
        properties.clear();
        clearChildren();
        return this;
    }

    // ========================= Property =========================

    /** 单个属性条目。 */
    public static class Property {
        final String name;
        private final Type type;
        private final String[] options;
        private BsPropertySheet sheet;
        final Label nameLabel;
        final Container<Actor> valueWrap;
        private Actor editor;
        private Object currentValue;

        Property(Skin skin, String name, Object value, Type type, String... options) {
            this.name = name;
            this.type = type;
            this.options = options;
            this.currentValue = value;
            this.nameLabel = new Label(name, skin);
            this.nameLabel.setColor(BsTheme.tp());
            this.valueWrap = new Container<>();
            this.valueWrap.fill();
            this.valueWrap.left();
            buildEditor();
        }

        private void buildEditor() {
            Skin skin = BsUI.getSkin();
            switch (type) {
                case TEXT: {
                    BsTextField tf = new BsTextField(currentValue == null ? "" : currentValue.toString(), skin);
                    tf.setTextFieldListener((f, c) -> {
                        currentValue = f.getText();
                        if (sheet != null) sheet.fireChange(name, currentValue);
                    });
                    editor = tf;
                    break;
                }
                case NUMBER: {
                    BsInputNumber num = new BsInputNumber(skin);
                    num.setRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
                    if (currentValue instanceof Number) num.setValue(((Number) currentValue).doubleValue());
                    num.setOnChange(v -> {
                        currentValue = v;
                        if (sheet != null) sheet.fireChange(name, v);
                    });
                    editor = num;
                    break;
                }
                case BOOLEAN: {
                    BsSwitch sw = new BsSwitch(skin);
                    sw.setChecked(Boolean.TRUE.equals(currentValue));
                    sw.setOnChange(c -> {
                        currentValue = c;
                        if (sheet != null) sheet.fireChange(name, c);
                    });
                    editor = sw;
                    break;
                }
                case COLOR: {
                    Color c = (currentValue instanceof Color) ? (Color) currentValue : Color.WHITE;
                    BsColorPicker cp = new BsColorPicker(skin);
                    cp.setSelectedColor(c);
                    cp.setSize(70, 28);   // 显式尺寸，避免被 Container fill 拉伸
                    cp.setOnChange(col -> {
                        currentValue = col;
                        if (sheet != null) sheet.fireChange(name, col);
                    });
                    editor = cp;
                    break;
                }
                case SELECT: {
                    BsSelectBox<String> sb = new BsSelectBox<>(skin);
                    com.badlogic.gdx.utils.Array<String> items = new com.badlogic.gdx.utils.Array<>();
                    String initial = currentValue == null ? "" : currentValue.toString();
                    int selIdx = 0;
                    if (options != null) {
                        for (int i = 0; i < options.length; i++) {
                            items.add(options[i]);
                            if (options[i].equals(initial)) selIdx = i;
                        }
                    }
                    sb.setItems(items);
                    if (items.size > 0) sb.setSelectedIndex(selIdx);
                    sb.addListener(new ChangeListener() {
                        @Override public void changed(ChangeEvent event, Actor actor) {
                            currentValue = sb.getSelected();
                            if (sheet != null) sheet.fireChange(name, currentValue);
                        }
                    });
                    editor = sb;
                    break;
                }
                case READONLY:
                default: {
                    Label l = new Label(currentValue == null ? "-" : currentValue.toString(), skin);
                    l.setColor(new Color(0.3f, 0.3f, 0.32f, 1f));
                    editor = l;
                    break;
                }
            }
            // COLOR 不希望被 fill 拉伸（保持默认 60×28 尺寸）
            valueWrap.fill(type != Type.COLOR);
            valueWrap.setActor(editor);
        }

        public Object getValue() { return currentValue; }

        public Property setValue(Object v) {
            this.currentValue = v;
            buildEditor();
            return this;
        }

        public String getName() { return name; }
        public Type getType() { return type; }
        public Actor getEditor() { return editor; }
    }
}
