/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库
 * Copyright (c) 2026 bs-ui contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package cn.pingyuanren.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

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
/// 实现：自定义 `Actor`，draw 用 ShapeRenderer 三角形带画连续圆环弧（每 ~2° 一段，
/// 无间隙，边缘靠 MSAA 平滑），track 整圈 + progress 弧从顶部(12 点)顺时针。
/// <p>历史：早期版本用 48 颗位图圆点铺圆周（珠串），放大后有间隙/发虚，2026-07-24 改为
/// ShapeRenderer 连续弧，与原 {@link BsRingProgress} 子类统一。BsRingProgress 现为兼容别名。</p>
///
/// 字段/常量/method 多为 protected，便于子类（如 BsRingProgress）复用，对调用者无影响。
/// @author authorZhao
/// @since 2026-07-16
public class BsCircularProgress extends Actor {

    public enum Variant { PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO }

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

    /** 共享 ShapeRenderer（static 单例，所有环形进度共用，避免每实例泄漏 native 资源）。 */
    private static ShapeRenderer sharedSR;
    private static synchronized ShapeRenderer sr() {
        if (sharedSR == null) sharedSR = new ShapeRenderer();
        return sharedSR;
    }
    /** draw 复用：避免每帧 new 2 个 Color（track/progress 各拷贝一份仅为改 alpha）。 */
    private final Color tmpTrack = new Color();
    private final Color tmpProg = new Color();
    /** label 文本缓存：只在 percent 变化时重算 String，避免每帧字符串拼接。 */
    private float cachedPercent = -1f;
    private String cachedLabel = "";

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

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Skin skin = BsUI.getSkin();
        Color trackColor = skin.get("bs-border", Color.class);
        Color progColor = progressColor();
        Color c = getColor();
        float alpha = c.a * parentAlpha;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float size = Math.min(getWidth(), getHeight());
        float thickness = Math.max(4f, size * 0.1f);
        float r = size / 2f - thickness / 2f - 1f;

        // shape 阶段：三角形带画连续圆环弧（平滑无发虚），算法见 BsShapeDraw.drawRingArc
        batch.end();
        try {
            ShapeRenderer s = sr();
            s.setProjectionMatrix(batch.getProjectionMatrix());
            s.setTransformMatrix(batch.getTransformMatrix());
            s.setColor(1, 1, 1, 1);
            s.begin(ShapeType.Filled);
            try {
                s.translate(getX(), getY(), 0);
                tmpTrack.set(trackColor);
                tmpTrack.a *= alpha;
                // track 整圈（底色）
                BsShapeDraw.drawRingArc(s, cx, cy, r, thickness, 90f, 360f, tmpTrack);
                tmpProg.set(progColor);
                tmpProg.a *= alpha;
                if (indeterminate) {
                    BsShapeDraw.drawRingArc(s, cx, cy, r, thickness, 90f - indetAngle, INDET_SWEEP_DEG, tmpProg);
                } else if (percent > 0.001f) {
                    BsShapeDraw.drawRingArc(s, cx, cy, r, thickness, 90f, 360f * percent, tmpProg);
                }
            } finally {
                s.identity();
                s.end();
            }
        } finally {
            batch.begin();
        }

        if (showLabel && !indeterminate) {
            drawCenterLabel(batch, alpha, skin, cx, cy);
        }
        batch.setColor(Color.WHITE);
    }

    /** 中心百分比文字（actor 局部坐标 cx/cy）。protected 供子类复用。 */
    protected void drawCenterLabel(Batch batch, float alpha, Skin skin, float cx, float cy) {
        // 只在 percent 变化时重算文本（避免每帧字符串拼接 GC）
        if (cachedPercent != percent) {
            cachedPercent = percent;
            cachedLabel = Math.round(percent * 100) + "%";
        }
        BitmapFont font = skin.getFont("default");
        Color tp = skin.get("bs-text-primary", Color.class);
        Color oldFont = font.getColor();
        glyph.setText(font, cachedLabel);
        font.setColor(tp.r, tp.g, tp.b, alpha);
        batch.setColor(Color.WHITE);
        font.draw(batch, cachedLabel, getX() + cx - glyph.width / 2f, getY() + cy + glyph.height / 2f);
        font.setColor(oldFont);
    }
}
