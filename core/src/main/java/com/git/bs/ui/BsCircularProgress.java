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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/// 环形进度（circular progress / ring）：determinate 百分比环 或 indeterminate 旋转弧。
///
/// 与 `BsProgress`（横向条）互补：仪表盘、上传/下载百分比、加载圆环等场景。
///
/// 用法：
/// ```java
/// BsCircularProgress ring = new BsCircularProgress(skin)
///         .setVariant(BsCircularProgress.Variant.SUCCESS)
///         .setPercent(0.65f)
///         .setShowLabel(true);
/// ring.setSize(80, 80);
/// stage.addActor(ring);
///
/// // 不确定态（加载中）：
/// new BsCircularProgress(skin).setIndeterminate(true).setSize(48, 48);
/// ```
///
/// 实现：自定义 `Actor`，draw 用一个白色圆点沿圆周铺 SEGMENTS 个点，
/// 按 percent 决定每点是 progress 色还是 track 色（纯 Batch，不依赖 ShapeRenderer）。
/// **圆点优先取 skin 的 `bs-circle`**（由 `BsSkinFactory.circleDrawable` 统一生成、
/// 可随 skin 导出/换主题）；skin 缺失时才用同一生成器兜底（兜底 Texture 由本组件 dispose）。
///
/// <p><b>扩展</b>：需要更平滑的圆环（大屏仪表盘等场景，离散圆点会"发虚"）用子类
/// {@link BsRingProgress} —— 它用 ShapeRenderer 画三角形带连续环弧。本类保持默认行为不变。</p>
///
/// 字段/常量/method 多为 protected，便于子类（如 BsRingProgress）复用，对调用者无影响。
/// @author authorZhao
/// @since 2026-07-16
public class BsCircularProgress extends Actor {

    public enum Variant { PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO }

    private static final int SEGMENTS = 48;
    /** 供子类（BsRingProgress）复用。 */
    protected static final float INDET_SPEED_DEG = 270f;
    /** 供子类复用。 */
    protected static final float INDET_SWEEP_DEG = 90f;

    /** 供子类绘制 label 时复用。 */
    protected final GlyphLayout glyph = new GlyphLayout();

    protected float percent = 0f;
    protected boolean indeterminate = false;
    protected float indetAngle = 0f;
    protected boolean showLabel = false;
    protected Variant variant = Variant.PRIMARY;

    /// skin 无 `bs-circle` 时的兜底圆点（由本组件自管 dispose）。
    private Drawable dotFallback;

    public BsCircularProgress(Skin skin) {
        this(skin, Variant.PRIMARY);
    }

    public BsCircularProgress(Skin skin, Variant variant) {
        this.variant = variant;
        setSize(64, 64);
    }

    // =================== API ===================

    /// determinate 进度（0~1，会被 clamp）。indeterminate 模式下无效。
    public BsCircularProgress setPercent(float p) {
        this.percent = MathUtils.clamp(p, 0f, 1f);
        return this;
    }

    public float getPercent() { return percent; }

    /// 旋转弧加载态（true 时忽略 percent）。
    public BsCircularProgress setIndeterminate(boolean b) {
        this.indeterminate = b;
        return this;
    }

    /// 中心显示百分比文字（仅 determinate）。
    public BsCircularProgress setShowLabel(boolean b) {
        this.showLabel = b;
        return this;
    }

    public BsCircularProgress setVariant(Variant v) {
        this.variant = v;
        return this;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (indeterminate) {
            indetAngle = (indetAngle + INDET_SPEED_DEG * delta) % 360f;
        }
    }

    @Override
    public boolean remove() {
        disposeDotFallback();
        return super.remove();
    }

    // =================== 内部 ===================

    /** variant → 主色（protected 供子类复用）。 */
    protected Color progressColor() {
        switch (variant) {
            case PRIMARY:   return BsPalette.PRIMARY.getMain();
            case SECONDARY: return BsPalette.SECONDARY.getMain();
            case SUCCESS:   return BsPalette.SUCCESS.getMain();
            case DANGER:    return BsPalette.DANGER.getMain();
            case WARNING:   return BsPalette.WARNING.getMain();
            case INFO:      return BsPalette.INFO.getMain();
        }
        return BsPalette.PRIMARY.getMain();
    }

    /// 圆点：优先 `skin.bs-circle`；缺失则用 `BsSkinFactory.circleDrawable` 统一生成的兜底。
    private Drawable resolveDot(Skin skin) {
        if (skin.has("bs-circle", Drawable.class)) return skin.getDrawable("bs-circle");
        if (dotFallback == null) dotFallback = BsSkinFactory.circleDrawable(16);
        return dotFallback;
    }

    private void disposeDotFallback() {
        if (dotFallback instanceof TextureRegionDrawable) {
            Texture t = ((TextureRegionDrawable) dotFallback).getRegion().getTexture();
            if (t != null) {
                try { t.dispose(); } catch (Throwable ignored) {}
            }
        }
        dotFallback = null;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Skin skin = BsUI.getSkin();
        Drawable dot = resolveDot(skin);
        Color trackColor = skin.get("bs-border", Color.class);
        Color progColor = progressColor();
        Color c = getColor();
        float alpha = c.a * parentAlpha;

        float cx = getX() + getWidth() / 2f;
        float cy = getY() + getHeight() / 2f;
        float size = Math.min(getWidth(), getHeight());
        float thickness = Math.max(4f, size * 0.1f);
        float r = size / 2f - thickness / 2f;
        float dotSize = thickness;

        for (int i = 0; i < SEGMENTS; i++) {
            float frac = i / (float) SEGMENTS;
            boolean on;
            if (indeterminate) {
                float local = ((frac * 360f) - indetAngle + 360f) % 360f;
                on = local < INDET_SWEEP_DEG;
            } else {
                on = frac <= percent;
            }
            Color col = on ? progColor : trackColor;

            float ang = frac * 360f - 90f;  // 从顶部开始
            float rad = (float) Math.toRadians(ang);
            float px = cx + (float) Math.cos(rad) * r;
            float py = cy + (float) Math.sin(rad) * r;
            batch.setColor(col.r, col.g, col.b, alpha);
            dot.draw(batch, px - dotSize / 2f, py - dotSize / 2f, dotSize, dotSize);
        }

        if (showLabel && !indeterminate) {
            drawCenterLabel(batch, alpha, skin, cx - getX(), cy - getY());
        }

        batch.setColor(Color.WHITE);
    }

    /** 中心百分比文字（actor 局部坐标 cx/cy）。protected 供子类复用。 */
    protected void drawCenterLabel(Batch batch, float alpha, Skin skin, float cx, float cy) {
        String text = Math.round(percent * 100) + "%";
        BitmapFont font = skin.getFont("default");
        Color tp = skin.get("bs-text-primary", Color.class);
        Color oldFont = font.getColor();
        glyph.setText(font, text);
        font.setColor(tp.r, tp.g, tp.b, alpha);
        batch.setColor(Color.WHITE);
        font.draw(batch, text, getX() + cx - glyph.width / 2f, getY() + cy + glyph.height / 2f);
        font.setColor(oldFont);
    }
}
