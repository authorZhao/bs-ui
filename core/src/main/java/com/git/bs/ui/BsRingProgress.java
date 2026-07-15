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
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * 平滑环形进度 —— {@link BsCircularProgress} 的子类，用 ShapeRenderer 画三角形带连续环弧。
 *
 * <p><b>为何单独子类而非改父类</b>：父类 {@link BsCircularProgress} 用离散位图圆点
 * （`bs-circle`）铺圆周，纯 Batch 渲染、默认场景够用且兼容性好；但放大后位图缩放 +
 * 珠串间隙会"发虚"。本子类改用 ShapeRenderer 画连续三角形带（每 ~2° 一段，无间隙，
 * 边缘靠 MSAA 平滑），适合大屏仪表盘等需要清晰大圆环的场景。父类保持原样，互不影响。</p>
 *
 * <p>API 完全沿用父类（{@link #setPercent}/{@link #setVariant}/{@link #setShowLabel}/
 * {@link #setIndeterminate}），只重写渲染方式。track 整圈 + progress 弧从顶部(12 点)顺时针。</p>
 *
 * <pre>{@code
 * BsRingProgress ring = new BsRingProgress(skin, BsCircularProgress.Variant.SUCCESS)
 *         .setPercent(0.65f).setShowLabel(true);
 * ring.setSize(96, 96);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsRingProgress extends BsCircularProgress {

    private static ShapeRenderer sharedSR;
    private static synchronized ShapeRenderer sr() {
        if (sharedSR == null) sharedSR = new ShapeRenderer();
        return sharedSR;
    }

    public BsRingProgress(Skin skin) {
        super(skin);
    }

    public BsRingProgress(Skin skin, Variant variant) {
        super(skin, variant);
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

        // shape 阶段：三角形带画连续圆环弧（平滑无发虚）
        batch.end();
        try {
            ShapeRenderer sr = sr();
            sr.setProjectionMatrix(batch.getProjectionMatrix());
            sr.setTransformMatrix(batch.getTransformMatrix());
            sr.setColor(1, 1, 1, 1);
            sr.begin(ShapeType.Filled);
            try {
                sr.translate(getX(), getY(), 0);
                Color tc = new Color(trackColor);
                tc.a *= alpha;
                // track 整圈（底色）
                drawRingArc(sr, cx, cy, r, thickness, 90f, 360f, tc);
                Color pc = new Color(progColor);
                pc.a *= alpha;
                if (indeterminate) {
                    drawRingArc(sr, cx, cy, r, thickness, 90f - indetAngle, INDET_SWEEP_DEG, pc);
                } else if (percent > 0.001f) {
                    drawRingArc(sr, cx, cy, r, thickness, 90f, 360f * percent, pc);
                }
            } finally {
                sr.identity();
                sr.end();
            }
        } finally {
            batch.begin();
        }

        // label（batch 阶段，复用父类逻辑）
        if (showLabel && !indeterminate) {
            drawCenterLabel(batch, alpha, skin, cx, cy);
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * 画圆环弧（三角形带）。从 startDeg（90=顶部 12 点）顺时针扫 sweepDeg 度，
     * 每 ~2° 一段，连续无间隙。
     */
    private void drawRingArc(ShapeRenderer sr, float cx, float cy, float r, float thickness,
                             float startDeg, float sweepDeg, Color color) {
        if (sweepDeg <= 0) return;
        sr.setColor(color);
        float inner = r - thickness / 2f;
        float outer = r + thickness / 2f;
        int segs = Math.max(2, (int) Math.ceil(sweepDeg / 2f));
        double a0 = Math.toRadians(startDeg);
        float ix0 = cx + (float) Math.cos(a0) * inner, iy0 = cy + (float) Math.sin(a0) * inner;
        float ox0 = cx + (float) Math.cos(a0) * outer, oy0 = cy + (float) Math.sin(a0) * outer;
        for (int i = 1; i <= segs; i++) {
            double a = Math.toRadians(startDeg - sweepDeg * i / segs);   // 递减 = 顺时针
            float ix = cx + (float) Math.cos(a) * inner;
            float iy = cy + (float) Math.sin(a) * inner;
            float ox = cx + (float) Math.cos(a) * outer;
            float oy = cy + (float) Math.sin(a) * outer;
            sr.triangle(ix0, iy0, ox0, oy0, ox, oy);
            sr.triangle(ix0, iy0, ox, oy, ix, iy);
            ix0 = ix; iy0 = iy; ox0 = ox; oy0 = oy;
        }
    }
}
