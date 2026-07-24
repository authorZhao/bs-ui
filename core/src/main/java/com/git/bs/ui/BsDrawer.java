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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 风格专用抽屉（Drawer）—— 比 {@link BsOffcanvas} 更结构化的版本，
 * 适合做详情面板、属性面板、编辑面板。
 *
 * <p>固定结构：</p>
 * <pre>
 * ┌─────────────────────────────┐
 * │  标题栏（图标 + 标题 + ×）   │
 * ├─────────────────────────────┤
 * │                             │
 * │      内容（业务方填入）       │
 * │                             │
 * ├─────────────────────────────┤
 * │  底部按钮（取消 / 保存 ...）  │
 * └─────────────────────────────┘
 * </pre>
 *
 * <p>与 BsOffcanvas 区别：</p>
 * <ul>
 *   <li>BsOffcanvas 是简单面板，从边缘滑入；BsDrawer 是带完整框架的复合面板</li>
 *   <li>BsDrawer 默认尺寸更大（400×stage 高度），适合做详情</li>
 *   <li>BsDrawer 自带底部按钮栏 API</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsDrawer drawer = new BsDrawer(skin);
 * drawer.setTitle("编辑用户");
 * drawer.setContent(myForm);
 * drawer.setDrawerWidth(420);
 * drawer.addButton("取消", () -> drawer.close(), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
 * drawer.addButton("保存", () -> { doSave(); drawer.close(); }, BsButton.Variant.PRIMARY);
 * drawer.setOnClose(() -> setStatus("抽屉关闭"));
 * drawer.show(stage);   // 默认从右侧滑入
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsDrawer extends Table {

    public enum Side { LEFT, RIGHT }

    private final Table panel;
    private final Table titleBar;
    private final Container<Actor> contentWrap;
    private final Table buttonBar;
    private Table backdrop;
    private float drawerWidth = 400;
    private Side side = Side.RIGHT;
    private boolean shown = false;
    private Runnable onClose;
    @Setter private boolean closeOnBackdrop = true;

    private static final float ANIM_DURATION = 0.25f;

    public BsDrawer(Skin skin) {
        panel = new Table();
        panel.setBackground(skin.getDrawable("bs-window-bg"));
        panel.setTouchable(Touchable.enabled);
        panel.pad(0);
        panel.top().left();
        panel.defaults().growX();

        // 标题栏
        titleBar = new Table();
        titleBar.setBackground(skin.getDrawable("bs-menu-bar-bg"));
        titleBar.left();
        titleBar.pad(12, 16, 12, 16);
        titleBar.defaults().pad(0).left();
        panel.add(titleBar).growX().row();

        // 标题与内容之间的分隔线
        Table sep = new Table();
        sep.setBackground(BsSkinFactory.drawableOf(BsTheme.bds()));
        panel.add(sep).height(1).growX().row();

        // 内容区
        contentWrap = new Container<>();
        contentWrap.fill();
        contentWrap.top().left();
        contentWrap.pad(16);
        panel.add(contentWrap).grow().row();

        // 按钮栏（默认隐藏）
        buttonBar = new Table();
        buttonBar.right();
        buttonBar.pad(12, 16, 12, 16);
        buttonBar.defaults().pad(0).padLeft(8);
        buttonBar.setVisible(false);
        // 按钮栏上方分隔线
        Table sep2 = new Table();
        sep2.setBackground(BsSkinFactory.drawableOf(BsTheme.bds()));
        panel.add(sep2).height(1).growX().row();
        panel.add(buttonBar).growX();

        // 标题栏 × 关闭按钮（默认放最右）
        Label.LabelStyle xStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        xStyle.font = skin.getFont("font-xl");
        Label x = new Label("×", xStyle);
        x.setColor(BsPalette.SECONDARY.getMain());
        Container<Label> xWrap = new Container<>(x);
        xWrap.setTouchable(Touchable.enabled);
        xWrap.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { close(); }
        });
        titleBar.add(xWrap).expandX().right();
    }

    public BsDrawer setTitle(String title) {
        // 清掉除了 × 之外的标题内容，重新排
        Actor xCell = titleBar.getChildren().size > 0
                ? titleBar.getChildren().get(titleBar.getChildren().size - 1) : null;
        titleBar.clearChildren();
        Skin skin = BsUI.getSkin();
        Label.LabelStyle tStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        tStyle.font = skin.getFont("font-lg");
        Label t = new Label(title == null ? "" : title, tStyle);
        t.setColor(BsTheme.tp());
        titleBar.add(t).left();
        if (xCell != null) titleBar.add(xCell).expandX().right();
        return this;
    }

    public BsDrawer setContent(Actor content) {
        contentWrap.setActor(content);
        return this;
    }

    public BsDrawer setDrawerWidth(float w) { this.drawerWidth = w; return this; }
    public BsDrawer setSide(Side s) { this.side = s; return this; }

    public BsDrawer addButton(String label, Runnable onClick,
                              BsButton.Variant variant, BsButton.Style style) {
        buttonBar.setVisible(true);
        BsButton btn = new BsButton(label, BsUI.getSkin(), variant, style, BsButton.Size.MD);
        if (onClick != null) {
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    try { onClick.run(); } catch (Throwable t) { log.warn("drawer button", t); }
                }
            });
        }
        buttonBar.add(btn);
        return this;
    }

    public BsDrawer addButton(String label, Runnable onClick, BsButton.Variant variant) {
        return addButton(label, onClick, variant, BsButton.Style.SOLID);
    }

    public BsDrawer setOnClose(Runnable r) { this.onClose = r; return this; }
    public boolean isShown() { return shown; }

    public void show(Stage stage) {
        if (stage == null || shown) return;
        shown = true;
        // backdrop
        backdrop = new Table();
        backdrop.setBackground(BsSkinFactory.drawableOf(BsTheme.ov()));
        backdrop.setFillParent(true);
        backdrop.setTouchable(Touchable.enabled);
        backdrop.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (closeOnBackdrop && event.getTarget() == backdrop) close();
            }
        });
        backdrop.setColor(1, 1, 1, 0);
        backdrop.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(ANIM_DURATION, com.badlogic.gdx.math.Interpolation.fade));
        stage.addActor(backdrop);

        // panel
        panel.setSize(drawerWidth, stage.getHeight());
        // 起始位置（屏幕外）
        if (side == Side.RIGHT) {
            panel.setPosition(stage.getWidth(), 0);
        } else {
            panel.setPosition(-drawerWidth, 0);
        }
        stage.addActor(panel);
        panel.toFront();

        float targetX = (side == Side.RIGHT) ? stage.getWidth() - drawerWidth : 0;
        panel.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(
                targetX, 0, ANIM_DURATION, com.badlogic.gdx.math.Interpolation.exp5Out));
    }

    public void close() {
        if (!shown) return;
        shown = false;
        Stage st = panel.getStage() != null ? panel.getStage() : getStage();
        if (st == null) {
            cleanup();
            return;
        }
        float outX = (side == Side.RIGHT) ? st.getWidth() : -drawerWidth;
        panel.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(
                        outX, 0, ANIM_DURATION, com.badlogic.gdx.math.Interpolation.exp5In),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.run(this::cleanup)
        ));
        if (backdrop != null) {
            backdrop.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(ANIM_DURATION, com.badlogic.gdx.math.Interpolation.fade),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor()
            ));
        }
    }

    private void cleanup() {
        if (backdrop != null) {
            try { backdrop.remove(); } catch (Throwable ignored) {}
            backdrop = null;
        }
        try { panel.remove(); } catch (Throwable ignored) {}
        if (onClose != null) {
            try { onClose.run(); } catch (Throwable t) { log.warn("onClose", t); }
        }
    }
}
