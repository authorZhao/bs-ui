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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Align;

/**
 * Bootstrap 风格旋转加载器。
 *
 * <p>两种样式：</p>
 * <ul>
 *   <li>{@link Style#BORDER} —— 圆环旋转（spinner-border）：3/4 圆弧（270°）绕中心转。
 *       视觉接近 Bootstrap 默认 spinner-border。</li>
 *   <li>{@link Style#GROW} —— 脉冲缩放（spinner-grow）：实心圆周期性缩放。</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsSpinner spinner = new BsSpinner(skin, Style.BORDER, Color.NAVY);
 * spinner.setSize(32, 32);
 * stage.addActor(spinner);
 * // act 由 stage 自动驱动；想停止：spinner.setSpinning(false)
 * }</pre>
 *
 * <p>实现：用 {@link ShapeRenderer} 自绘（和 BsRingProgress 一致）——
 * BORDER 用三角形带画 270° 粗弧（{@link BsShapeDraw#drawRingArc}），
 * GROW 用实心圆 + scale。不再用 Pixmap 像素拼凑（旧实现擦除逻辑错误，
 * 只画出"一个小钩"，已废弃）。ShapeRenderer 为 static 共享单例，避免每实例泄漏 OpenGL 资源。</p>
 *
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsSpinner extends Actor {

    public enum Style { BORDER, GROW }

    private static final float BORDER_SPEED_DEG = 270f;  // 每秒转 270°（Bootstrap 默认 0.75s/圈）
    private static final float GROW_PERIOD = 1.0f;       // 缩放周期 1s
    /** BORDER 弧的扫过角度（Bootstrap spinner-border 标准是 270°，剩 90° 透明缺口）。 */
    private static final float BORDER_SWEEP_DEG = 270f;

    /** 共享 ShapeRenderer（static 单例，所有 spinner 共用，避免每实例 new 泄漏 native 资源）。
     *  libgdx UI 都在 render 线程，单例无并发问题。 */
    private static ShapeRenderer sharedSR;

    private final Style style;
    /** draw 复用：避免每帧 new Color（alpha 合成用）。 */
    private final Color tmpColor = new Color();
    private float angle;
    private float scaleTime;
    private boolean spinning = true;

    public BsSpinner(com.badlogic.gdx.scenes.scene2d.ui.Skin skin, Style style) {
        this(skin, style, BsPalette.PRIMARY.getMain()); // 主色 #0D6EFD
    }

    public BsSpinner(com.badlogic.gdx.scenes.scene2d.ui.Skin skin, Style style, Color color) {
        this.style = style;
        setColor(color);   // 用 Actor.setColor（scene2d 惯例，draw 里 getColor() 取色）
        setSize(32, 32);
        setOrigin(Align.center);
    }

    public void setSpinning(boolean spinning) {
        this.spinning = spinning;
    }

    public boolean isSpinning() { return spinning; }

    private static synchronized ShapeRenderer sr() {
        if (sharedSR == null) sharedSR = new ShapeRenderer();
        return sharedSR;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!spinning) return;
        if (style == Style.BORDER) {
            angle = (angle + BORDER_SPEED_DEG * delta) % 360f;
        } else {
            scaleTime = (scaleTime + delta) % GROW_PERIOD;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // cx/cy 用 actor 局部坐标（不含 getX()/getY()），配合下方 translate(getX(), getY()) 定位
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float size = Math.min(getWidth(), getHeight());
        Color c = getColor();
        float alpha = c.a * parentAlpha;

        // 切到 ShapeRenderer 渲染（和 BsRingProgress/BsRating 同模式）
        batch.end();
        try {
            ShapeRenderer s = sr();
            s.setProjectionMatrix(batch.getProjectionMatrix());
            s.setTransformMatrix(batch.getTransformMatrix());
            s.setColor(1, 1, 1, 1);
            s.begin(ShapeRenderer.ShapeType.Filled);
            try {
                s.translate(getX(), getY(), 0);

                if (style == Style.BORDER) {
                    // 3/4 圆弧（270°）旋转。thickness 约为 size 的 1/8（Bootstrap border-width 0.25em）
                    float r = size / 2f;
                    float thickness = Math.max(3f, size / 8f);
                    Color base = getColor();
                    tmpColor.set(base.r, base.g, base.b, alpha);
                    BsShapeDraw.drawRingArc(s, cx, cy, r - thickness / 2f, thickness,
                            90f + angle, BORDER_SWEEP_DEG, tmpColor);
                } else {
                    // GROW: 实心圆脉冲缩放（0 → 1，用 sin 平滑）
                    float t = scaleTime / GROW_PERIOD;  // 0~1
                    float scale = 0.5f + 0.5f * (float) Math.sin(t * Math.PI * 2 - Math.PI / 2); // 0~1
                    scale = Math.max(0.1f, scale);
                    float radius = (size / 2f) * scale;
                    Color base = getColor();
                    s.setColor(base.r, base.g, base.b, alpha);
                    s.circle(cx, cy, radius);
                }
            } finally {
                s.identity();
                s.end();
            }
        } finally {
            batch.begin();
        }
    }
}
