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

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 5 风格侧滑抽屉（Offcanvas）—— 从屏幕边缘滑入的面板。
 *
 * <p>支持 4 个方向：左/右/上/下。常用于：</p>
 * <ul>
 *   <li>侧边筛选条件面板（左/右滑入）</li>
 *   <li>移动端汉堡菜单（左滑入）</li>
 *   <li>顶部通知条 / 底部确认条（上/下滑入）</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsOffcanvas off = new BsOffcanvas(skin, BsOffcanvas.Placement.RIGHT);
 * off.setTitle("筛选");
 * off.setContent(filterTable);
 * off.setWidth(320);
 * off.setOnClose(() -> setStatus("抽屉关闭"));
 * off.show(stage);   // 从右侧滑入；点遮罩或 × 关闭
 * }</pre>
 *
 * <p>实现：panel（圆角白底）+ backdrop（半透明黑色遮罩）。
 * 入场：从边缘滑入；出场：滑出 + 淡出。panel 的位置由 placement 决定。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsOffcanvas extends Table {

    public enum Placement { LEFT, RIGHT, TOP, BOTTOM }

    private final Placement placement;
    private final Table panel;          // 实际面板容器
    private final Table titleRow;       // 标题行
    private final Container<Actor> contentWrap;  // 内容容器
    private Table backdrop;
    private float drawerWidth = 320;
    private float drawerHeight = 320;
    private boolean shown = false;
    private Runnable onClose;
    /** backdrop 点击是否关闭（默认 true）。 */
    @Setter private boolean closeOnBackdrop = true;

    private static final float ANIM_DURATION = 0.25f;

    public BsOffcanvas(Skin skin, Placement placement) {
        this.placement = placement;
        // 外层 Table 仅作布局占位，实际显示靠 panel + backdrop 各自加到 stage
        panel = new Table();
        panel.setBackground(skin.getDrawable("bs-window-bg"));
        panel.setTouchable(Touchable.enabled);
        panel.pad(0);
        panel.top().left();
        panel.defaults().growX();

        titleRow = new Table();
        titleRow.pad(12, 16, 12, 16);
        titleRow.left();
        panel.add(titleRow).growX().row();

        // 标题与内容之间的分隔线
        Table sep = new Table();
        sep.setBackground(BsSkinFactory.drawableOf(BsTheme.bds()));
        sep.setHeight(1);
        panel.add(sep).height(1).growX().row();

        contentWrap = new Container<>();
        contentWrap.fill();
        contentWrap.top().left();
        contentWrap.pad(12, 16, 12, 16);
        panel.add(contentWrap).grow();

        // 默认 × 关闭按钮
        addCloseButtonToTitle();
    }

    /** 在标题行右侧加 × 关闭按钮。 */
    private void addCloseButtonToTitle() {
        Skin skin = BsUI.getSkin();
        Label.LabelStyle xStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        xStyle.font = skin.getFont("font-xl");
        Label x = new Label("×", xStyle);
        x.setColor(BsTheme.tm());
        Container<Label> xWrap = new Container<>(x);
        xWrap.setTouchable(Touchable.enabled);
        xWrap.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { close(); }
        });
        titleRow.add(xWrap).expandX().right();
    }

    /** 设置标题（替换 × 之前的标题文字）。 */
    public BsOffcanvas setTitle(String title) {
        // 清掉除了 × 之外的内容，重排
        Actor xCell = titleRow.getChildren().size > 0
                ? titleRow.getChildren().get(titleRow.getChildren().size - 1) : null;
        titleRow.clearChildren();
        Skin skin = BsUI.getSkin();
        Label.LabelStyle tStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        tStyle.font = skin.getFont("font-lg");
        Label t = new Label(title == null ? "" : title, tStyle);
        t.setColor(BsTheme.tp());
        titleRow.add(t).left();
        if (xCell != null) titleRow.add(xCell).expandX().right();
        return this;
    }

    public BsOffcanvas setContent(Actor content) {
        contentWrap.setActor(content);
        return this;
    }

    /** 设置抽屉宽度（LEFT/RIGHT 生效）。 */
    public BsOffcanvas setDrawerWidth(float w) { this.drawerWidth = w; return this; }

    /** 设置抽屉高度（TOP/BOTTOM 生效）。 */
    public BsOffcanvas setDrawerHeight(float h) { this.drawerHeight = h; return this; }

    public BsOffcanvas setOnClose(Runnable r) { this.onClose = r; return this; }

    public boolean isShown() { return shown; }

    // ========================= 显示 / 关闭 =========================

    /** 显示：添加 backdrop + panel，按 placement 定位，从边缘滑入。 */
    public void show(Stage stage) {
        if (stage == null || shown) return;
        shown = true;

        // backdrop：半透明黑色遮罩，覆盖全屏
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
        backdrop.addAction(Actions.fadeIn(ANIM_DURATION, Interpolation.fade));
        stage.addActor(backdrop);

        // panel 尺寸
        boolean vertical = (placement == Placement.LEFT || placement == Placement.RIGHT);
        float pw = vertical ? drawerWidth : stage.getWidth();
        float ph = vertical ? stage.getHeight() : drawerHeight;
        panel.setSize(pw, ph);
        positionOffscreen(stage);   // 起始位置在屏幕外
        stage.addActor(panel);
        panel.toFront();

        // 滑入到目标位置
        float targetX = targetX(stage, pw);
        float targetY = targetY(stage, ph);
        panel.addAction(Actions.moveTo(targetX, targetY, ANIM_DURATION, Interpolation.exp5Out));
    }

    /** 关闭：panel 滑出 + backdrop 淡出，然后移除。 */
    public void close() {
        if (!shown) return;
        shown = false;
        Stage st = getStageOrPanelStage();
        if (st == null) {
            cleanupActors();
            return;
        }
        float w = panel.getWidth();
        float h = panel.getHeight();
        // 滑出目标位置
        float outX = panel.getX();
        float outY = panel.getY();
        switch (placement) {
            case LEFT:   outX = -w; break;
            case RIGHT:  outX = st.getWidth(); break;
            case TOP:    outY = st.getHeight(); break;
            case BOTTOM: outY = -h; break;
        }
        panel.addAction(Actions.sequence(
                Actions.moveTo(outX, outY, ANIM_DURATION, Interpolation.exp5In),
                Actions.run(this::cleanupActors)
        ));
        if (backdrop != null) {
            backdrop.addAction(Actions.sequence(
                    Actions.fadeOut(ANIM_DURATION, Interpolation.fade),
                    Actions.removeActor()
            ));
        }
    }

    private Stage getStageOrPanelStage() {
        return panel.getStage() != null ? panel.getStage() : getStage();
    }

    /** 移除所有 actor 并触发 onClose。 */
    private void cleanupActors() {
        if (backdrop != null) {
            try { backdrop.remove(); } catch (Throwable ignored) {}
            backdrop = null;
        }
        try { panel.remove(); } catch (Throwable ignored) {}
        if (onClose != null) {
            try { onClose.run(); } catch (Throwable t) { log.warn("onClose", t); }
        }
    }

    /** panel 起始位置（屏幕外）。 */
    private void positionOffscreen(Stage stage) {
        switch (placement) {
            case LEFT:   panel.setPosition(-drawerWidth, 0); break;
            case RIGHT:  panel.setPosition(stage.getWidth(), 0); break;
            case TOP:    panel.setPosition(0, stage.getHeight()); break;
            case BOTTOM: panel.setPosition(0, -drawerHeight); break;
        }
    }

    /** panel 显示后的目标位置。 */
    private float targetX(Stage stage, float pw) {
        switch (placement) {
            case LEFT:   return 0;
            case RIGHT:  return stage.getWidth() - pw;
            default:     return 0;
        }
    }

    private float targetY(Stage stage, float ph) {
        switch (placement) {
            case TOP:    return stage.getHeight() - ph;
            case BOTTOM: return 0;
            default:     return 0;
        }
    }
}
