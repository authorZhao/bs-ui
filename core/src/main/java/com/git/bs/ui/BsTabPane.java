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

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.Actor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap 风格 Tab 面板（手动实现，因为 Scene2d 无内置 TabPane）。
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsTabPane extends Table {

    public static class BsTab {
        public final String title;
        public final Table content;
        public final TextButton button;
        public BsTab(String title, Table content, TextButton button) {
            this.title = title; this.content = content; this.button = button;
        }
    }

    @Getter
    private final List<BsTab> tabs = new ArrayList<>();
    private int active = 0;
    private final Table tabBar = new Table();
    private final Table contentContainer = new Table();

    public BsTabPane(Skin skin) {
        add(tabBar).left().growX().row();
        add(contentContainer).grow().row();
    }

    public BsTab addTab(String title, Table content) {
        TextButton btn = new TextButton(title, BsUI.getSkin(), "bs-btn-secondary");
        btn.setProgrammaticChangeEvents(false);
        BsTab tab = new BsTab(title, content, btn);
        final int index = tabs.size();
        tabs.add(tab);
        btn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                select(index);
            }
        });
        rebuildBar();
        return tab;
    }

    public void select(int index) {
        if (index < 0 || index >= tabs.size()) return;
        active = index;
        rebuildBar();
        contentContainer.clearChildren();
        contentContainer.add(tabs.get(index).content).grow();
    }

    private void rebuildBar() {
        tabBar.clearChildren();
        for (int i = 0; i < tabs.size(); i++) {
            BsTab t = tabs.get(i);
            String style = i == active ? "bs-btn-primary" : "bs-btn-outline-secondary";
            t.button.setStyle(BsUI.getSkin().get(style, TextButton.TextButtonStyle.class));
            tabBar.add(t.button).left().padRight(2);
        }
    }
}
