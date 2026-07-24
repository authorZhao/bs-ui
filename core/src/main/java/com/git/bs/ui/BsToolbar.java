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
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Bootstrap 风格工具栏（Toolbar）—— 横向排列的按钮组 + 分隔线 + 可选下拉菜单按钮。
 * 常用于编辑器顶部、表格上方操作区。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsToolbar tb = new BsToolbar(skin);
 * tb.addButton("新建", () -> newFile(), BsButton.Variant.PRIMARY);
 * tb.addButton("打开", () -> openFile());
 * tb.addSeparator();
 * tb.addIconButton(BsIcon.get("trash"), () -> delete(), BsButton.Variant.DANGER);
 * tb.addButtonWithMenu("导出", menu -> {
 *     menu.addItem("PDF", () -> exportPdf());
 *     menu.addItem("PNG", () -> exportPng());
 * });
 * stage.addActor(tb);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsToolbar extends Table {

    public BsToolbar(Skin skin) {
        left().center();
        defaults().pad(0).center();
        pad(4, 6, 4, 6);
    }

    /** 添加一个文字按钮。 */
    public BsToolbar addButton(String label, Runnable onClick) {
        return addButton(label, onClick, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
    }

    /** 添加一个文字按钮（指定颜色，默认 OUTLINE）。 */
    public BsToolbar addButton(String label, Runnable onClick, BsButton.Variant variant) {
        return addButton(label, onClick, variant, BsButton.Style.OUTLINE);
    }

    public BsToolbar addButton(String label, Runnable onClick,
                               BsButton.Variant variant, BsButton.Style style) {
        BsButton btn = new BsButton(label, BsUI.getSkin(), variant, style, BsButton.Size.SM);
        attachClick(btn, onClick);
        add(btn).padRight(2);
        return this;
    }

    /** 添加一个图标按钮（无文字）。 */
    public BsToolbar addIconButton(Drawable icon, Runnable onClick) {
        return addIconButton(icon, onClick, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
    }

    /** 添加一个图标按钮（指定颜色，默认 OUTLINE）。 */
    public BsToolbar addIconButton(Drawable icon, Runnable onClick, BsButton.Variant variant) {
        return addIconButton(icon, onClick, variant, BsButton.Style.OUTLINE);
    }

    public BsToolbar addIconButton(Drawable icon, Runnable onClick,
                                   BsButton.Variant variant, BsButton.Style style) {
        BsButton btn = new BsButton("", BsUI.getSkin(), variant, style, BsButton.Size.SM);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconSize(16, 16);
            btn.pad(4, 8, 4, 8);
        }
        attachClick(btn, onClick);
        add(btn).padRight(2);
        return this;
    }

    /** 添加一个带下拉菜单的按钮。 */
    public BsToolbar addButtonWithMenu(String label, Consumer<BsMenuBar.BsMenu> menuConfig) {
        BsMenuBar bar = new BsMenuBar(BsUI.getSkin());
        BsMenuBar.BsMenu menu = bar.addMenu(label);
        if (menuConfig != null) {
            try { menuConfig.accept(menu); } catch (Throwable t) { log.warn("menuConfig", t); }
        }
        add(bar).padRight(2);
        return this;
    }

    /** 添加一个带下拉菜单的图标按钮。 */
    public BsToolbar addIconButtonWithMenu(Drawable icon, String label,
                                           Consumer<BsMenuBar.BsMenu> menuConfig) {
        BsMenuBar bar = new BsMenuBar(BsUI.getSkin());
        BsMenuBar.BsMenu menu = bar.addMenu(label, icon);
        if (menuConfig != null) {
            try { menuConfig.accept(menu); } catch (Throwable t) { log.warn("menuConfig", t); }
        }
        add(bar).padRight(2);
        return this;
    }

    /** 添加一条垂直分隔线。 */
    public BsToolbar addSeparator() {
        Container<Actor> sep = new Container<>();
        sep.setBackground(BsSkinFactory.drawableOf(BsTheme.bds()));
        sep.size(1, 22);
        add(sep).size(1, 22).padLeft(8).padRight(8);
        return this;
    }

    /** 添加弹簧（把后续按钮推到右边）。 */
    public BsToolbar addSpring() {
        add().growX();
        return this;
    }

    /** 末尾添加任意 actor。 */
    public BsToolbar addCustom(Actor actor) {
        add(actor).padRight(2);
        return this;
    }

    private void attachClick(BsButton btn, Runnable onClick) {
        if (onClick == null) return;
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                try { onClick.run(); } catch (Throwable t) { log.warn("toolbar click", t); }
            }
        });
    }
}
