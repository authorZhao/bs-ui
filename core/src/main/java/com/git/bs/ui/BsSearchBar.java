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
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Bootstrap 风格搜索栏（SearchBar）—— 输入 + 搜索按钮 + 清除按钮 + 可选过滤器下拉。
 *
 * <p>结构：</p>
 * <pre>
 * [筛选▾] [🔍 搜索......  ×]  [搜索]
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsSearchBar search = new BsSearchBar(skin);
 * search.setPlaceholder("输入用户名或邮箱...");
 * search.addFilter("全部", "姓名", "邮箱", "手机");
 * search.setOnFilterChange(idx -> setStatus("过滤器: " + idx));
 * search.setOnSearch(text -> doSearch(text));
 * stage.addActor(search);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsSearchBar extends Table {

    private final BsSelectBox<String> filterBox;
    private final BsTextField field;
    private final BsButton clearBtn;
    private final BsButton searchBtn;
    private Consumer<String> onSearch;
    private Consumer<Integer> onFilterChange;

    public BsSearchBar(Skin skin) {
        this(skin, true);
    }

    public BsSearchBar(Skin skin, boolean withFilter) {
        left();
        defaults().pad(0);

        filterBox = new BsSelectBox<>(skin);
        filterBox.setVisible(withFilter);
        if (withFilter) {
            com.badlogic.gdx.utils.Array<String> items = new com.badlogic.gdx.utils.Array<>();
            items.add(BsI18n.get("core.search.all", "全部"));
            filterBox.setItems(items);
            filterBox.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                @Override public void changed(com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    if (onFilterChange != null) {
                        try { onFilterChange.accept(filterBox.getSelectedIndex()); } catch (Throwable t) { log.warn("onFilter", t); }
                    }
                    triggerSearch();
                }
            });
            add(filterBox).width(110).padRight(6);
        }

        field = new BsTextField("", skin);
        field.setMessageText(BsI18n.get("core.search.placeholder", "搜索..."));
        field.setTextFieldListener((f, c) -> {
            updateClearBtn();
            if (c == '\n' || c == '\r') triggerSearch();
        });
        add(field).width(260);

        clearBtn = new BsButton("×", skin, BsButton.Variant.SECONDARY, BsButton.Style.SOLID, BsButton.Size.SM);
        clearBtn.pad(2, 8, 2, 8);
        clearBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                field.setText("");
                updateClearBtn();
            }
        });
        clearBtn.setVisible(false);
        add(clearBtn).padLeft(-1);

        searchBtn = new BsButton(BsI18n.get("core.search.button", "搜索"), skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        searchBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                triggerSearch();
            }
        });
        add(searchBtn).padLeft(6);
    }

    private void updateClearBtn() {
        clearBtn.setVisible(!field.getText().isEmpty());
    }

    private void triggerSearch() {
        if (onSearch != null) {
            try { onSearch.accept(field.getText()); } catch (Throwable t) { log.warn("onSearch", t); }
        }
    }

    public BsSearchBar setPlaceholder(String p) {
        field.setMessageText(p);
        return this;
    }

    public BsSearchBar addFilter(String... options) {
        com.badlogic.gdx.utils.Array<String> items = new com.badlogic.gdx.utils.Array<>(options);
        filterBox.setItems(items);
        filterBox.setVisible(true);
        return this;
    }

    public BsSearchBar setOnSearch(Consumer<String> cb) { this.onSearch = cb; return this; }
    public BsSearchBar setOnFilterChange(Consumer<Integer> cb) { this.onFilterChange = cb; return this; }

    public String getText() { return field.getText(); }
    public int getFilterIndex() { return filterBox.getSelectedIndex(); }
    public String getFilter() { return filterBox.getSelected(); }

    public BsTextField getField() { return field; }
    public BsSelectBox<String> getFilterBox() { return filterBox; }
    public BsButton getSearchButton() { return searchBtn; }
}
