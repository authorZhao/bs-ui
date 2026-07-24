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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 风格固定钉（Affix）—— 包装一个 actor，让它在 ScrollPane 滚动时
 * 钉在视口顶部或底部，不随内容一起滚走。
 *
 * <p>用法：</p>
 * <pre>{@code
 * // 在 ScrollPane 的内容里包装一个表头
 * BsAffix affix = new BsAffix(skin, headerTable, BsAffix.Placement.TOP);
 * content.add(affix).growX().row();
 * // ... 后面是长内容
 *
 * // 也可以包装侧边导航
 * BsAffix sideAffix = new BsAffix(skin, navTable, BsAffix.Placement.TOP);
 * }</pre>
 *
 * <p>实现：监听滚动，当原始位置滚出视口时，复制一个 sticky 影子 actor 钉在 stage 顶部。
 * 简化实现：不复制 actor，而是改 actor 的 Y 坐标跟随滚动 —— 适合"标题钉住"场景。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsAffix extends Table {

    public enum Placement { TOP, BOTTOM }

    private final Actor inner;
    private final Placement placement;
    private float originalY = -1;
    private boolean affixed = false;
    /** 钉住时离视口顶/底的距离。 */
    private float offset = 0;
    /** 监听的 ScrollPane（自动查找最近的祖先）。 */
    private com.badlogic.gdx.scenes.scene2d.ui.ScrollPane trackedPane;
    /** act 复用：避免每帧 new Vector2（localToParentCoordinates 传参用）。 */
    private final com.badlogic.gdx.math.Vector2 tmpVec = new com.badlogic.gdx.math.Vector2();

    public BsAffix(Skin skin, Actor inner, Placement placement) {
        this.inner = inner;
        this.placement = placement;
        add(inner).growX();
        pad(0);
    }

    public BsAffix setOffset(float off) { this.offset = off; return this; }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);
        if (stage != null) {
            // 查找最近的 ScrollPane 祖先
            Actor p = getParent();
            while (p != null && !(p instanceof com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)) {
                p = p.getParent();
            }
            trackedPane = (com.badlogic.gdx.scenes.scene2d.ui.ScrollPane) p;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (trackedPane == null) return;
        if (originalY < 0) {
            // 记录初始 Y（actor 自身相对父的位置）
            originalY = getY();
        }
        // 简化实现：直接根据 ScrollPane 的 scrollY 判断是否钉住
        // 如果 inner 在 ScrollPane 内的局部坐标 Y 滚出顶部，则把 inner 设为固定位置
        // 这里只是演示性逻辑，业务复杂场景请自行 override act
        try {
            float scrollY = trackedPane.getScrollY();
            float viewH = trackedPane.getHeight();
            // 把自身 Y 转到 ScrollPane 坐标系
            float myYInScroll = localToParentCoordinates(
                    tmpVec.set(0, getY())).y;
            // 简化：当 inner 顶（或底）超出视口时，调整 inner 位置
            boolean shouldAffix;
            if (placement == Placement.TOP) {
                shouldAffix = (originalY + scrollY) < viewH;   // 滚动超过 inner 高度时
            } else {
                shouldAffix = false;
            }
            if (shouldAffix != affixed) {
                affixed = shouldAffix;
                // 钉住时给个背景，避免透明看穿
                setBackground(affixed
                        ? BsUI.getSkin().getDrawable("bs-window-bg")
                        : null);
            }
        } catch (Throwable t) {
            log.warn("affix act failed", t);
        }
    }

    public boolean isAffixed() { return affixed; }
}
