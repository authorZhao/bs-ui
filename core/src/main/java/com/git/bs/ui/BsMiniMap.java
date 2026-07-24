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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bootstrap 风格小地图（MiniMap）—— 节点画布的缩略图，可点击跳转。
 *
 * <p>结构：自绘 Actor，画布上的节点用小方块表示，连线用细线，
 * 当前视口范围用一个矩形框标出。点击小地图 → 触发「跳转到此坐标」回调。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsMiniMap mm = new BsMiniMap();
 * mm.setSize(180, 140);
 * mm.setCanvasBounds(0, 0, 2000, 1600);   // 画布全尺寸
 * mm.setNodes(nodes);   // List<Node>(x, y, color)
 * mm.setViewport(400, 300, 600, 400);   // 当前可见区域
 * mm.setOnClick((canvasX, canvasY) -> scrollTo(canvasX, canvasY));
 * stage.addActor(mm);
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsMiniMap extends Table {

    /** 画布上的节点（坐标 + 颜色）。 */
    public static class Node {
        public float x, y;
        public Color color;
        public Node(float x, float y, Color color) { this.x = x; this.y = y; this.color = color; }
    }

    private final MiniMapActor actor;
    private Consumer<float[]> onClick;   // 接收 [canvasX, canvasY]
    /** V2：颜色从此 skin 取。由 setSkin 注入；未注入时 draw 阶段尝试从 stage 取。 */
    private Skin skin;

    /** 注入 skin（V2：颜色从 skin Color 桶取）。建议在 add 到 stage 之前调用。
     *  注意：libgdx Table 已有 final setSkin，故改名 themeSkin。 */
    public BsMiniMap themeSkin(Skin skin) {
        this.skin = skin;
        return this;
    }

    /** 取当前 skin（优先字段，回退到 stage 关联）。 */
    private Skin skinOrStage() {
        if (skin != null) return skin;
        if (getStage() != null) {
            // libgdx Stage 本身不直接持有 skin；尝试用 userObject 约定或回退到 BsSkinFactory.current()
            // 这里保守策略：如果没传 skin，draw 时把 skin 字段从测试台等已知入口注入
            // 退而求其次：直接 throw，提示用户必须 setSkin
            throw new IllegalStateException("BsMiniMap: V2 API 必须先 setSkin()");
        }
        throw new IllegalStateException("BsMiniMap: 未注入 skin 且未加入 stage");
    }

    public BsMiniMap() {
        skin = BsUI.getSkin();
        actor = new MiniMapActor();
        add(actor).grow();
    }

    public BsMiniMap setCanvasBounds(float x, float y, float w, float h) {
        actor.canvasX = x; actor.canvasY = y;
        actor.canvasW = w; actor.canvasH = h;
        return this;
    }

    public BsMiniMap setNodes(List<Node> nodes) {
        actor.nodes = new ArrayList<>(nodes);
        return this;
    }

    public BsMiniMap setConnections(List<float[]> conns) {
        actor.connections = new ArrayList<>(conns);   // 每个 = [x1, y1, x2, y2]
        return this;
    }

    /** 设置当前可见视口（画布坐标）。 */
    public BsMiniMap setViewport(float x, float y, float w, float h) {
        actor.viewX = x; actor.viewY = y; actor.viewW = w; actor.viewH = h;
        return this;
    }

    public BsMiniMap setOnClick(Consumer<float[]> cb) { this.onClick = cb; return this; }

    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
        actor.setSize(width, height);
        return;
    }

    private class MiniMapActor extends Actor {
        float canvasX = 0, canvasY = 0, canvasW = 1000, canvasH = 800;
        float viewX = 0, viewY = 0, viewW = 200, viewH = 150;
        List<Node> nodes = new ArrayList<>();
        List<float[]> connections = new ArrayList<>();
        ShapeRenderer sr;
        // draw 阶段复用：避免每帧 new Color / float[]（节点多时数组爆炸）
        private final float[] tmpA = new float[2];
        private final float[] tmpB = new float[2];
        private final Color cBgHeader = new Color();
        private final Color cBds = new Color();
        private final Color cTm = new Color();
        private final Color cPrimarySoft = new Color();
        private final Color cPrimaryStroke = new Color();

        MiniMapActor() {
            setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    float[] canvasXY = miniToCanvas(x, y);
                    if (onClick != null) {
                        try { onClick.accept(canvasXY); } catch (Throwable t) { log.warn("onClick", t); }
                    }
                    return true;
                }
            });
        }

        private float[] miniToCanvas(float mx, float my) {
            // mx, my 是 actor 局部坐标
            float cx = canvasX + (mx / getWidth()) * canvasW;
            float cy = canvasY + (1 - my / getHeight()) * canvasH;   // Y 翻转
            return new float[]{cx, cy};
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (sr == null) sr = new ShapeRenderer();
            Skin skin0 = skinOrStage();
            batch.end();
            try {
                sr.setProjectionMatrix(batch.getProjectionMatrix());
                sr.setTransformMatrix(batch.getTransformMatrix());
                sr.begin(ShapeRenderer.ShapeType.Filled);
                try {
                    sr.translate(getX(), getY(), 0);
                    // 背景
                    cBgHeader.set(BsTheme.bhH());
                    cBgHeader.a = parentAlpha;
                    sr.setColor(cBgHeader);
                    sr.rect(0, 0, getWidth(), getHeight());
                    // 边框
                    cBds.set(BsTheme.bds());
                    cBds.a = parentAlpha;
                    sr.setColor(cBds);
                    float bw = 1.5f;
                    sr.rect(0, 0, getWidth(), bw);
                    sr.rect(0, getHeight() - bw, getWidth(), bw);
                    sr.rect(0, 0, bw, getHeight());
                    sr.rect(getWidth() - bw, 0, bw, getHeight());

                    // 连线
                    cTm.set(BsTheme.tm());
                    cTm.a = parentAlpha;
                    sr.setColor(cTm);
                    for (float[] c : connections) {
                        canvasToMini(c[0], c[1], tmpA);
                        canvasToMini(c[2], c[3], tmpB);
                        rectLine(sr, tmpA[0], tmpA[1], tmpB[0], tmpB[1], 1);
                    }

                    // 节点
                    for (Node n : nodes) {
                        canvasToMini(n.x, n.y, tmpA);
                        Color c = n.color != null ? n.color : BsPalette.PRIMARY.getMain();
                        sr.setColor(c.r, c.g, c.b, c.a * parentAlpha);
                        sr.rect(tmpA[0] - 2, tmpA[1] - 2, 4, 4);
                    }

                    // 视口矩形
                    canvasToMini(viewX, viewY, tmpA);
                    canvasToMini(viewX + viewW, viewY + viewH, tmpB);
                    float vx = Math.min(tmpA[0], tmpB[0]);
                    float vy = Math.min(tmpA[1], tmpB[1]);
                    float vw = Math.abs(tmpB[0] - tmpA[0]);
                    float vh = Math.abs(tmpB[1] - tmpA[1]);
                    cPrimarySoft.set(BsPalette.PRIMARY.getMain());
                    cPrimarySoft.a = 0.3f * parentAlpha;
                    sr.setColor(cPrimarySoft);
                    sr.rect(vx, vy, vw, vh);
                    // 视口描边
                    cPrimaryStroke.set(BsPalette.PRIMARY.getMain());
                    cPrimaryStroke.a = parentAlpha;
                    sr.setColor(cPrimaryStroke);
                    rectLine(sr, vx, vy, vx + vw, vy, 1);
                    rectLine(sr, vx, vy + vh, vx + vw, vy + vh, 1);
                    rectLine(sr, vx, vy, vx, vy + vh, 1);
                    rectLine(sr, vx + vw, vy, vx + vw, vy + vh, 1);
                } finally {
                    sr.identity();
                    sr.end();
                }
            } finally {
                batch.begin();
            }
        }

        /** canvas 坐标 → mini 坐标，结果写入 out（避免每帧 new float[]）。 */
        private void canvasToMini(float cx, float cy, float[] out) {
            out[0] = (cx - canvasX) / canvasW * getWidth();
            out[1] = (1 - (cy - canvasY) / canvasH) * getHeight();   // Y 翻转
        }

        private void rectLine(ShapeRenderer sr, float x1, float y1, float x2, float y2, float w) {
            float dx = x2 - x1, dy = y2 - y1;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.5f) return;
            float nx = -dy / length, ny = dx / length;
            float half = w / 2f;
            float ax = x1 + nx * half, ay = y1 + ny * half;
            float bx = x2 + nx * half, by = y2 + ny * half;
            float cx = x2 - nx * half, cy = y2 - ny * half;
            float dx_ = x1 - nx * half, dy_ = y1 - ny * half;
            sr.triangle(ax, ay, bx, by, cx, cy);
            sr.triangle(ax, ay, cx, cy, dx_, dy_);
        }
    }
}
