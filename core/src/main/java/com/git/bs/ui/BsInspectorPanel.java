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
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * Bootstrap 风格检视面板（Inspector Panel）—— 类似 Unity Inspector，
 * 显示「当前选中节点」的属性，可切换不同对象。
 *
 * <p>结构：</p>
 * <pre>
 * ┌────────────────────────┐
 * │  [节点图标] 节点名称     │  ← 标题栏（图标 + 名称 + 可选类型标签）
 * ├────────────────────────┤
 * │ ─ 基本 ─                │  ← 分组
 * │   name     [TextField] │
 * │   id        1024        │
 * │ ─ 位置 ─                │
 * │   x         100         │
 * │   y         80          │
 * │ ─ 外观 ─                │
 * │   color     [color]     │
 * │   visible   [switch]    │
 * └────────────────────────┘
 * </pre>
 *
 * <p>与 {@link BsPropertySheet} 的区别：</p>
 * <ul>
 *   <li>Inspector 是「单对象 + 标题栏 + 类型徽章」的封装，用于"选中什么就显示什么"</li>
 *   <li>PropertySheet 是纯属性列表，没有标题概念</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsInspectorPanel insp = new BsInspectorPanel(skin);
 * insp.setTarget("Player", "GameObject", someIcon);
 * insp.sheet().addProperty("name", "Hero", Type.TEXT);
 * insp.sheet().addProperty("hp", 100, Type.NUMBER);
 * // 切换选中
 * insp.setTarget("NPC_01", "GameObject", icon);
 * insp.sheet().clearProperties();
 * insp.sheet().addProperty(...);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsInspectorPanel extends Table {

    private final Table titleBar;
    private final com.badlogic.gdx.scenes.scene2d.ui.Image iconImage;
    private final Label titleLabel;
    private final Label typeBadge;
    private final BsPropertySheet sheet;
    private Runnable onClose;
    private Label closeButton;

    public BsInspectorPanel(Skin skin) {
        setBackground(skin.getDrawable("bs-window-bg"));
        left().top();
        defaults().growX().left();
        pad(0);

        // 标题栏：[icon] 名称 [类型徽章] ...... [×]
        titleBar = new Table();
        titleBar.setBackground(skin.getDrawable("bs-menu-bar-bg"));
        titleBar.left();
        titleBar.pad(8, 10, 8, 10);
        titleBar.defaults().pad(0).left();

        iconImage = new com.badlogic.gdx.scenes.scene2d.ui.Image();
        iconImage.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        titleBar.add(iconImage).size(20).padRight(8).left();

        Label.LabelStyle lgStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        lgStyle.font = skin.getFont("font-lg");
        Label.LabelStyle smStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        smStyle.font = skin.getFont("font-sm");
        titleLabel = new Label(BsI18n.get("inspector.unselected", "(未选中)"), lgStyle);
        titleLabel.setColor(BsTheme.tp());
        titleBar.add(titleLabel).left();

        typeBadge = new Label("", smStyle);
        typeBadge.setColor(BsPalette.SECONDARY.getMain());
        Container<Label> badgeWrap = new Container<>(typeBadge);
        badgeWrap.setBackground(skin.getDrawable("bs-menu-title-hover"));
        badgeWrap.pad(2, 8, 2, 8);
        titleBar.add(badgeWrap).padLeft(8).left();

        titleBar.add().growX();   // 弹簧推 × 到右边

        Label.LabelStyle xlStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        xlStyle.font = skin.getFont("font-xl");
        closeButton = new Label("×", xlStyle);
        closeButton.setColor(BsPalette.SECONDARY.getMain());
        Container<Label> closeWrap = new Container<>(closeButton);
        closeWrap.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        closeWrap.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                if (onClose != null) {
                    try { onClose.run(); } catch (Throwable t) { log.warn("onClose", t); }
                }
                return true;
            }
            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                closeButton.setColor(BsTheme.tp());
            }
            @Override
            public void exit(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                closeButton.setColor(BsPalette.SECONDARY.getMain());
            }
        });
        titleBar.add(closeWrap).right();

        add(titleBar).growX().row();

        // 分隔线
        Table sep = new Table();
        sep.setBackground(BsSkinFactory.drawableOf(BsTheme.bds()));
        add(sep).height(1).growX().row();

        // 属性编辑器
        sheet = new BsPropertySheet(skin);
        add(sheet).growX().pad(8).row();
    }

    /** 设置当前检视的目标。 */
    public BsInspectorPanel setTarget(String name, String type,
                                      com.badlogic.gdx.scenes.scene2d.utils.Drawable icon) {
        titleLabel.setText(name == null ? BsI18n.get("inspector.unselected", "(未选中)") : name);
        typeBadge.setText(type == null || type.isEmpty() ? "" : type);
        typeBadge.setVisible(type != null && !type.isEmpty());
        if (icon != null) {
            iconImage.setDrawable(icon);
            iconImage.setVisible(true);
        } else {
            iconImage.setVisible(false);
        }
        return this;
    }

    public BsInspectorPanel setTarget(String name, String type) {
        return setTarget(name, type, null);
    }

    /** 取底层 PropertySheet 配置属性。 */
    public BsPropertySheet sheet() { return sheet; }

    /** 关闭按钮回调（× 点击）。 */
    public BsInspectorPanel setOnClose(Runnable r) { this.onClose = r; return this; }

    /** 清空属性表（切换目标时常用）。 */
    public BsInspectorPanel clearProperties() {
        sheet.clearProperties();
        return this;
    }
}
