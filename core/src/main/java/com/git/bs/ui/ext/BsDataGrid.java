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
package com.git.bs.ui.ext;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.git.bs.ui.BsSkinFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/// 虚拟化数据表格（DataTable 增强，独立新类，不改既有 `BsDataTable`）。
///
/// 固定表头 + 虚拟化行（复用 {@link BsVirtualList}），可承载大数据量。
/// 列由 `addColumn(标题, 取值器, 列宽)` 声明；行点击、斑马纹开箱即用。
///
/// 用法：
/// ```java
/// BsDataGrid<User> grid = new BsDataGrid<User>(skin)
///         .addColumn("姓名", u -> u.name, 120)
///         .addColumn("年龄", u -> String.valueOf(u.age), 80)
///         .addColumn("邮箱", u -> u.email, 200)
///         .setItems(users)
///         .setOnRowClick((idx, u) -> setStatus("点 " + u.name));
/// grid.setSize(600, 400);
/// ```
///
/// 实现：Table = 固定表头行 + [行容器](BsVirtualList)。
/// 行 cell = 横向 `Table`（每列一个 Label，宽度对齐表头）。行高固定（默认 32）。
///
/// v1 不含：横向滚动、列排序/拖拽/冻结/显隐、单元格富内容、可编辑、行多选。
/// @author authorZhao
/// @since 2026-07-16
@Slf4j
public class BsDataGrid<T> extends Table {

    /// 列定义。
    public static final class Column<T> {
        public final String title;
        public final Function<T, String> getter;
        public final float width;
        public Column(String title, Function<T, String> getter, float width) {
            this.title = title; this.getter = getter; this.width = width;
        }
    }

    private final Skin skin;
    private final List<Column<T>> columns = new ArrayList<>();
    private final Table header;
    private final BsVirtualList<T> list;
    private float rowH = 32f;
    private boolean zebra = true;

    public BsDataGrid(Skin skin) {
        this.skin = skin;
        header = new Table();
        list = new BsVirtualList<>(skin, this::renderRow, rowH);
        list.setOverscanRows(1);
        defaults().growX();
        add(header).growX().row();
        add(list).grow().row();
    }

    /// 新增一列。须在 `setItems` 前定义好列。
    public BsDataGrid<T> addColumn(String title, Function<T, String> getter, float width) {
        columns.add(new Column<>(title, getter, width));
        rebuildHeader();
        return this;
    }

    public BsDataGrid<T> setRowHeight(float h) {
        this.rowH = Math.max(12f, h);
        return this;
    }

    /// 是否启用斑马纹（默认开）。
    public BsDataGrid<T> setZebra(boolean zebra) {
        this.zebra = zebra;
        return this;
    }

    public BsDataGrid<T> setItems(List<T> items) {
        list.setItems(items);
        return this;
    }

    public BsDataGrid<T> setOnRowClick(BiConsumer<Integer, T> cb) {
        list.setOnClick(cb);
        return this;
    }

    // =================== 内部 ===================

    private void rebuildHeader() {
        header.clearChildren();
        header.setBackground(skin.getDrawable("bs-menu-bar-bg"));
        for (Column<T> c : columns) {
            Label h = new Label(c.title == null ? "" : c.title, skin);
            h.setColor(skin.get("bs-text-primary", Color.class));
            header.add(h).width(c.width).left().padLeft(8).padRight(4);
        }
    }

    private Actor renderRow(Actor existing, T item, int index) {
        Table row;
        if (existing instanceof Table) {
            row = (Table) existing;
            row.clearChildren();
        } else {
            row = new Table();
        }
        // 斑马纹
        if (zebra && index % 2 == 1) {
            row.setBackground(BsSkinFactory.drawableOf(skin.get("bs-bg-hover", Color.class)));
        } else {
            row.setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);
        }
        Color tp = skin.get("bs-text-primary", Color.class);
        for (Column<T> c : columns) {
            String text;
            try {
                text = item == null ? "" : c.getter.apply(item);
            } catch (Throwable t) {
                log.warn("BsDataGrid getter error at col {}: {}", c.title, t.toString());
                text = "";
            }
            Label l = new Label(text == null ? "" : text, skin);
            l.setColor(tp);
            row.add(l).width(c.width).left().padLeft(8).padRight(4);
        }
        return row;
    }
}
