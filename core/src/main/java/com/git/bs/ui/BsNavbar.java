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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Bootstrap 5 风格导航栏（Navbar）—— 应用顶部完整导航框架。
 *
 * <p>结构：[logo] [品牌名] [主菜单区 {@link BsMenuBar}] ...... [搜索/操作按钮区]</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsNavbar navbar = new BsNavbar(skin);
 * navbar.setBrand("MyApp");
 * navbar.addMenuItem("文件", menu -> {
 *     menu.addItem("新建", () -> ...);
 *     menu.addItem("打开", () -> ...);
 * });
 * navbar.addAction("设置", () -> ...);
 * navbar.addSearchField("搜索...");
 * stage.addActor(navbar);
 * }</pre>
 *
 * <p>实现：横向 Table，左侧 [logo + brand + menubar]，右侧 [action 区]，
 * 中间用 fill 空间推右。背景用 {@code bs-menu-bar-bg}（浅灰白圆角），
 * 底部加一条 1px 横线（深色 drawable）模拟 navbar 底部 border。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsNavbar extends Table {

    private final Table leftZone;
    private final Table rightZone;
    private final BsMenuBar menuBar;
    private Image logoImage;
    private Label brandLabel;

    public BsNavbar(Skin skin) {
        setBackground(skin.getDrawable("bs-menu-bar-bg"));
        pad(6, 14, 6, 14);
        left().top();

        leftZone = new Table();
        leftZone.left();
        rightZone = new Table();
        rightZone.right();
        menuBar = new BsMenuBar(skin);

        add(leftZone).growX().left();
        add(rightZone).right();
    }

    /** 设置品牌名（左侧文字）。 */
    public BsNavbar setBrand(String name) {
        if (brandLabel == null) {
            brandLabel = new Label(name, BsUI.getSkin());
            brandLabel.setColor(BsTheme.tp());
            brandLabel.setFontScale(1.2f);
        } else {
            brandLabel.setText(name);
        }
        relayoutLeft();
        return this;
    }

    /** 设置左侧 logo 图标。 */
    public BsNavbar setLogo(Drawable logo) {
        if (logoImage == null) {
            logoImage = new Image(logo);
            logoImage.setScaling(Scaling.fit);
            logoImage.setSize(28, 28);
        } else {
            logoImage.setDrawable(logo);
        }
        relayoutLeft();
        return this;
    }

    /** 添加下拉菜单（通过 BsMenuBar.BsMenu 添加 item）。 */
    public BsNavbar addMenuItem(String title, Consumer<BsMenuBar.BsMenu> config) {
        BsMenuBar.BsMenu menu = menuBar.addMenu(title);
        if (config != null) {
            try { config.accept(menu); } catch (Throwable t) { log.warn("menu config", t); }
        }
        relayoutLeft();
        return this;
    }

    /** 右侧操作区加按钮。 */
    public BsNavbar addAction(String label, Runnable onClick) {
        return addAction(label, onClick, BsButton.Variant.PRIMARY, BsButton.Style.OUTLINE);
    }

    public BsNavbar addAction(String label, Runnable onClick,
                              BsButton.Variant variant, BsButton.Style style) {
        BsButton b = new BsButton(label, BsUI.getSkin(), variant, style, BsButton.Size.SM);
        if (onClick != null) {
            b.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override
                public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                    try { onClick.run(); } catch (Throwable t) { log.warn("action", t); }
                    return true;
                }
            });
        }
        rightZone.add(b).padLeft(6);
        return this;
    }

    /** 右侧加搜索框（ BsTextField）。 */
    public BsNavbar addSearchField(String placeholder) {
        BsTextField f = new BsTextField("", BsUI.getSkin());
        f.setMessageText(placeholder == null ? "搜索..." : placeholder);
        rightZone.add(f).width(180).padLeft(6);
        return this;
    }

    /** 右侧加自定义 actor。 */
    public BsNavbar addCustomAction(Actor actor) {
        rightZone.add(actor).padLeft(6);
        return this;
    }

    /** 重建左侧布局（logo + brand + menubar）。 */
    private void relayoutLeft() {
        leftZone.clearChildren();
        if (logoImage != null) {
            leftZone.add(logoImage).size(28, 28).padRight(8);
        }
        if (brandLabel != null) {
            leftZone.add(brandLabel).padRight(14);
        }
        leftZone.add(menuBar);
    }

    /** 底部加 1px 深色横线（模拟 navbar 底部 border）。 */
    public BsNavbar showBottomBorder(boolean show) {
        if (show) {
            Table line = new Table();
            line.setBackground(BsUI.getSkin().newDrawable("white", BsTheme.bds()));
            row();
            add(line).growX().height(1).colspan(2);
        }
        return this;
    }
}
