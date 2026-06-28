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
