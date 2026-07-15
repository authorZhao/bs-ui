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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 风格 Tooltip：hover 时弹出深色背景+白字提示，支持 4 方向 placement。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsTooltip tip = new BsTooltip(targetButton, "保存修改", skin, Placement.TOP);
 * tip.attach(stage);
 * tip.setShowDelay(0.5f); // hover 0.5s 后显示
 * }</pre>
 *
 * <p>实现：tooltip 本身是 {@link Table}，加入 stage 顶层；监听 target 的 enter/exit；
 * enter 后等待 showDelay 才显示（避免快速划过）；相对 target 的边界计算 placement 位置。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsTooltip extends Table {

    public enum Placement { TOP, BOTTOM, LEFT, RIGHT }

    private static final Color BG_COLOR = new Color(0, 0, 0, 0.9f);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final float GAP = 6f;
    private static final float DEFAULT_SHOW_DELAY = 0.3f;

    private final Actor target;
    private final Placement placement;
    private final Label label;
    private Stage stage;
    private boolean attached;
    private float showDelay = DEFAULT_SHOW_DELAY;
    private float hoverTime;  // 鼠标 hover 累计时间，<0 表示未 hover
    private boolean showing;
    // stage 级别 mouseMoved 监听（用于鼠标离开 target 时可靠 hide）
    private com.badlogic.gdx.scenes.scene2d.InputListener stageMouseTracker;

    public BsTooltip(Actor target, String text, Skin skin) {
        this(target, text, skin, Placement.TOP);
    }

    public BsTooltip(Actor target, String text, Skin skin, Placement placement) {
        this.target = target;
        this.placement = placement;
        // 新建独立 LabelStyle 设 fontColor=白（scene2d Label 渲染时实际用 style.fontColor，
        // 不是 Actor.color，setColor 设白色会被 style 的深色 fontColor 覆盖 → 黑底黑字）
        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle whiteStyle =
                new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(skin.get(
                        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle.class));
        whiteStyle.fontColor = TEXT_COLOR;
        this.label = new Label(text, whiteStyle);
        setBackground(makeBg(skin));
        pad(6, 10, 6, 10);
        add(label);
        setVisible(false);
        setTransform(true);
        // 自身不可点击（tooltip 不应该阻挡鼠标事件，否则鼠标移到 tooltip 上时
        // target 收不到 exit，造成"关不掉"）
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    }

    public void setShowDelay(float seconds) { this.showDelay = seconds; }

    public void setText(String text) { label.setText(text); }

    /** 加入 stage，并绑定 target + stage 的 hover 监听。 */
    public void attach(Stage stage) {
        if (attached) return;
        attached = true;
        this.stage = stage;
        stage.addActor(this);
        // target 的 enter 触发"开始 hover"
        target.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer != -1) return; // 只响应鼠标
                hoverTime = 0;
            }
        });
        // stage 级 mouseMoved：鼠标移动到非 target 区域时可靠地 hide
        // （scene2d 的 exit 事件在快速移动或事件路径复杂时可能丢失）
        stageMouseTracker = new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                onStageMouseMoved(x, y);
                return false;
            }
        };
        stage.addListener(stageMouseTracker);
    }

    /** 鼠标移动时检查是否还在 target 边界内，不在则 hide。 */
    private void onStageMouseMoved(float stageX, float stageY) {
        if (target.getStage() == null) {
            hide();
            return;
        }
        Vector2 tp = target.localToStageCoordinates(new Vector2(0, 0));
        boolean inside = stageX >= tp.x && stageX <= tp.x + target.getWidth()
                && stageY >= tp.y && stageY <= tp.y + target.getHeight();
        if (!inside) {
            hoverTime = -1;
            hide();
        } else if (hoverTime < 0) {
            // 鼠标重新进入 target（兜底，防止 enter 未触发）
            hoverTime = 0;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!attached) return;
        // target 已不在 stage（被 remove/切屏）→ 同步移除自己 + 监听器，避免残留 tooltip
        if (target.getStage() == null) {
            detach();
            return;
        }
        if (hoverTime >= 0 && !showing) {
            hoverTime += delta;
            if (hoverTime >= showDelay) {
                show();
            }
        }
    }

    /** 从 stage 摘除自己 + 清 stage 监听器（target 销毁或主动 detach 时调用）。 */
    public void detach() {
        if (stage != null && stageMouseTracker != null) {
            stage.removeListener(stageMouseTracker);
            stageMouseTracker = null;
        }
        remove();
        attached = false;
    }

    private void show() {
        showing = true;
        setVisible(true);
        toFront();
        pack();
        positionNearTarget();
    }

    private void hide() {
        showing = false;
        setVisible(false);
    }

    private void positionNearTarget() {
        if (target.getStage() == null) return;
        Vector2 targetPos = target.localToStageCoordinates(new Vector2(0, 0));
        float tw = target.getWidth();
        float th = target.getHeight();
        float myW = getWidth();
        float myH = getHeight();

        switch (placement) {
            case TOP:
                setPosition(targetPos.x + (tw - myW) / 2f, targetPos.y + th + GAP);
                break;
            case BOTTOM:
                setPosition(targetPos.x + (tw - myW) / 2f, targetPos.y - myH - GAP);
                break;
            case LEFT:
                setPosition(targetPos.x - myW - GAP, targetPos.y + (th - myH) / 2f);
                break;
            case RIGHT:
                setPosition(targetPos.x + tw + GAP, targetPos.y + (th - myH) / 2f);
                break;
        }
        // 边界纠正（防止超出 stage）
        if (getX() < 0) setX(0);
        if (getY() < 0) setY(0);
        if (getX() + myW > target.getStage().getWidth()) {
            setX(target.getStage().getWidth() - myW);
        }
        if (getY() + myH > target.getStage().getHeight()) {
            setY(target.getStage().getHeight() - myH);
        }
    }

    /** 生成 tooltip 黑底 drawable。 */
    private static Drawable makeBg(Skin skin) {
        return skin.newDrawable("bs-tooltip-bg", BG_COLOR);
    }
}
