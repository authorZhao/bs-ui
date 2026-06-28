package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import lombok.extern.slf4j.Slf4j;

import java.util.function.IntConsumer;

/**
 * Bootstrap 风格星级评分（Rating）—— 5 颗星，支持半星、只读模式。
 *
 * <p><b>实现说明</b>：星星是<b>自绘 5 角星</b>（不依赖字体的 ★/☆ 字符 ——
 * 中文字体可能不含这些字符导致空白），用 ShapeRenderer 直接画填充星和描边星。
 * 已选 = warning 黄填充，未选 = 灰描边。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsRating rating = new BsRating(skin);
 * rating.setValue(3);
 * rating.setOnChange(v -> setStatus("评分: " + v));
 *
 * // 半星模式
 * rating.setHalfStars(true);
 * rating.setValue(3.5f);
 * }</pre>
 */
@Slf4j
public class BsRating extends Table {

    private float value = 0;
    private boolean readOnly = false;
    private boolean halfStars = false;
    private IntConsumer onChange;
    /** V2：颜色存放在 skin，字段初始化时无法访问 skin，先 null，构造中赋值。 */
    private Color starColorOverride = null;
    private Color starColor; // 在构造中从 skin 取（除非用户覆盖）
    private Color emptyColor = new Color(0x8B / 255f, 0x90 / 255f, 0x98 / 255f, 1f);
    private float starSize = 24;
    private float gap = 4;

    private final StarActor[] stars = new StarActor[5];
    private static ShapeRenderer sharedSR;

    public BsRating(Skin skin) {
        this.starColor = BsPalette.WARNING.getMain();  // V2：从 skin 取
        left();
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            StarActor sa = new StarActor();
            sa.setSize(starSize, starSize);
            sa.setTouchable(Touchable.enabled);
            sa.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if (readOnly) return false;
                    float w = sa.getWidth();
                    float newV;
                    if (halfStars) {
                        newV = (x < w / 2f) ? idx + 0.5f : idx + 1;
                    } else {
                        newV = idx + 1;
                    }
                    setValue(newV);
                    return true;
                }
            });
            stars[i] = sa;
            add(sa).size(starSize, starSize).padRight(gap);
        }
        updateStars();
    }

    public BsRating setValue(float v) {
        v = Math.max(0, Math.min(5, v));
        if (!halfStars) v = Math.round(v);
        boolean changed = Math.abs(v - value) > 0.001f;
        this.value = v;
        updateStars();
        if (changed && onChange != null) {
            try { onChange.accept(Math.round(value * 2)); } catch (Throwable t) { log.warn("onChange", t); }
        }
        return this;
    }

    public float getValue() { return value; }

    public BsRating setReadOnly(boolean r) {
        this.readOnly = r;
        setTouchable(r ? Touchable.disabled : Touchable.enabled);
        return this;
    }

    public BsRating setHalfStars(boolean h) { this.halfStars = h; updateStars(); return this; }

    public BsRating setOnChange(IntConsumer cb) { this.onChange = cb; return this; }

    public BsRating setStarColor(Color c) { this.starColorOverride = c; this.starColor = c; updateStars(); return this; }

    public BsRating setEmptyColor(Color c) { this.emptyColor = c; updateStars(); return this; }

    public BsRating setStarSize(float s) {
        this.starSize = s;
        for (StarActor a : stars) a.setSize(s, s);
        invalidateHierarchy();
        return this;
    }

    private void updateStars() {
        for (int i = 0; i < 5; i++) {
            float starValue = i + 1;
            StarActor sa = stars[i];
            if (value >= starValue) {
                sa.fill = 1f;       // 整星
            } else if (value >= starValue - 0.5f && halfStars) {
                sa.fill = 0.5f;     // 半星
            } else {
                sa.fill = 0f;       // 空
            }
        }
    }

    /** 单个星星自绘 Actor。fill=0 描边，fill=1 填充，fill=0.5 半填充。 */
    private class StarActor extends Actor {
        float fill = 0f;

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (sharedSR == null) sharedSR = new ShapeRenderer();
            batch.end();
            try {
                sharedSR.setProjectionMatrix(batch.getProjectionMatrix());
                sharedSR.setTransformMatrix(batch.getTransformMatrix());
                sharedSR.begin(ShapeRenderer.ShapeType.Filled);
                try {
                    sharedSR.translate(getX(), getY(), 0);
                    drawStar(sharedSR, getWidth(), getHeight(), fill, parentAlpha);
                } finally {
                    sharedSR.identity();
                    sharedSR.end();
                }
            } finally {
                batch.begin();
            }
        }

        private void drawStar(ShapeRenderer sr, float w, float h, float fill, float alpha) {
            // 计算 5 角星的 10 个顶点（外 5 + 内 5）
            float cx = w / 2f, cy = h / 2f;
            float rOuter = Math.min(w, h) / 2f;
            float rInner = rOuter * 0.4f;
            // 自下而上画（libgdx Y 朝上）
            // 5 角星尖角朝上的标准起始角度 = 90°（顶部）
            float[] xs = new float[10];
            float[] ys = new float[10];
            for (int i = 0; i < 10; i++) {
                float angle = (float) Math.toRadians(90 + i * 36);   // 每 36° 一个顶点
                float r = (i % 2 == 0) ? rOuter : rInner;
                xs[i] = cx + (float) Math.cos(angle) * r;
                ys[i] = cy + (float) Math.sin(angle) * r;
            }
            // 画填充星（整星 = 完全填充，半星 = 只画左半部分填充）
            if (fill > 0) {
                sr.setColor(starColor.r, starColor.g, starColor.b, starColor.a * alpha);
                if (fill >= 1f) {
                    // 整星：用三角形扇
                    for (int i = 1; i < 9; i++) {
                        sr.triangle(xs[0], ys[0], xs[i], ys[i], xs[i + 1], ys[i + 1]);
                    }
                } else {
                    // 半星：把星星按 x < cx 切，左半边填充
                    // 简化：用 clip 思路，只画 x ≤ cx 的三角形部分（用 cx 当分割）
                    // 实现：把所有顶点 cx 右侧的 clamp 到 cx
                    float[] hx = new float[10];
                    float[] hy = new float[10];
                    for (int i = 0; i < 10; i++) {
                        hx[i] = Math.min(xs[i], cx);
                        hy[i] = ys[i];
                    }
                    for (int i = 1; i < 9; i++) {
                        sr.triangle(hx[0], hy[0], hx[i], hy[i], hx[i + 1], hy[i + 1]);
                    }
                }
            }
            // 画描边（未选时）
            if (fill == 0) {
                sr.setColor(emptyColor.r, emptyColor.g, emptyColor.b, emptyColor.a * alpha);
                // 用三角形扇画实心灰色星
                for (int i = 1; i < 9; i++) {
                    sr.triangle(xs[0], ys[0], xs[i], ys[i], xs[i + 1], ys[i + 1]);
                }
                // 中间画一个小白星"挖空"
                sr.setColor(1, 1, 1, alpha);
                float scale = 0.7f;
                float[] ix = new float[10];
                float[] iy = new float[10];
                for (int i = 0; i < 10; i++) {
                    float angle = (float) Math.toRadians(90 + i * 36);
                    float r = ((i % 2 == 0) ? rOuter : rInner) * scale;
                    ix[i] = cx + (float) Math.cos(angle) * r;
                    iy[i] = cy + (float) Math.sin(angle) * r;
                }
                for (int i = 1; i < 9; i++) {
                    sr.triangle(ix[0], iy[0], ix[i], iy[i], ix[i + 1], iy[i + 1]);
                }
            }
        }
    }
}
