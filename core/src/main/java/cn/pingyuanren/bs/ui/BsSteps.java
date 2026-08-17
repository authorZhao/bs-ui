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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Bootstrap 风格步骤条（Steps / Stepper）—— 向导式分步表单/流程指示。
 *
 * <p>布局（一行均匀排列：圆心同高，标题在圆下方居中）：</p>
 * <pre>
 *       ①━━━━━━━②━━━━━━━③───────④
 *      资料     邮箱     安全     完成
 * </pre>
 *
 * <p>状态：</p>
 * <ul>
 *   <li><b>DONE</b>：实心圆 + ✓ + 完成色，<b>通往下一段的线 = 完成色</b></li>
 *   <li><b>CURRENT</b>：实心圆 + 数字 + 主色 + 半透明 ring 强调</li>
 *   <li><b>WAIT</b>：空心圆（灰描边）+ 数字，通向线 = 灰色细线</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsSteps steps = new BsSteps(skin);
 * steps.addSteps("填写资料", "验证邮箱", "设置密码", "完成");
 * steps.setCurrent(1);
 * steps.setOnStepClick(idx -> setStatus("切到第 " + idx + " 步"));
 *
 * // 自定义颜色
 * steps.setDoneColor(new Color(0.1f, 0.7f, 0.4f, 1f));
 * steps.setCurrentColor(Color.valueOf("#FD7E14"));
 * steps.setWaitColor(Color.GRAY);
 * steps.setLineHeight(4);
 * }</pre>
 *
 * <p>实现：父 Table = 一行（节点 + 线 + 节点 + 线 + ...），
 * 所有 cell 用 {@code .top()} 垂直对齐，cell 高度 = {@code circleSize + titleGap + titleHeight}，
 * 这样圆心严格在同一水平线（顶部 circleSize/2 处）。线条自绘 Actor 占据自己的 cell 宽度 × cell 高度，
 * draw 时在圆心位置 y 画水平线。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsSteps extends Table {

    /** V2：颜色存在 skin，static 字段初始化时无法访问 skin。已改为接收 skin 参数。 */
    public static Color DONE_DEFAULT(Skin skin) { return BsPalette.SUCCESS.getMain(); }
    public static Color CURRENT_DEFAULT(Skin skin) { return BsPalette.PRIMARY.getMain(); }
    public static Color WAIT_DEFAULT(Skin skin) { return BsTheme.tm(); }

    private final List<String> titles = new ArrayList<>();
    private int current = 0;
    private IntConsumer onStepClick;
    private float circleSize = 32;
    private float titleGap = 6;     // 圆底到标题顶的距离
    private float titleHeight = 22;
    private float lineLength = 60;
    private float lineHeight = 3f;

    /** V2：颜色在构造器中初始化（字段初始化时 skin 还没赋值）。 */
    private Color doneColor;
    private Color currentColor;
    private Color waitColor;
    private Function<Boolean, Color> lineColorFn = done -> done ? doneColor : waitColor;

    private static ShapeRenderer sharedSR;

    public BsSteps(Skin skin) {
        this.doneColor = DONE_DEFAULT(skin);
        this.currentColor = CURRENT_DEFAULT(skin);
        this.waitColor = WAIT_DEFAULT(skin);
        left().top();
        defaults().top().pad(0);
    }

    public BsSteps addStep(String title) { titles.add(title); rebuild(); return this; }

    public BsSteps addSteps(String... ts) {
        for (String t : ts) titles.add(t);
        rebuild();
        return this;
    }

    public BsSteps setCurrent(int idx) {
        if (idx < 0 || idx >= titles.size()) return this;
        this.current = idx;
        rebuild();
        if (onStepClick != null) {
            try { onStepClick.accept(idx); } catch (Throwable t) { log.warn("onStepClick", t); }
        }
        return this;
    }

    public int getCurrent() { return current; }
    public int getStepCount() { return titles.size(); }
    public BsSteps next() { return setCurrent(Math.min(current + 1, titles.size() - 1)); }
    public BsSteps prev() { return setCurrent(Math.max(current - 1, 0)); }

    public BsSteps setOnStepClick(IntConsumer cb) { this.onStepClick = cb; return this; }
    public BsSteps setCircleSize(float s) { this.circleSize = s; rebuild(); return this; }
    public BsSteps setLineLength(float l) { this.lineLength = l; rebuild(); return this; }
    public BsSteps setLineHeight(float h) { this.lineHeight = h; rebuild(); return this; }
    public BsSteps setDoneColor(Color c) { this.doneColor = c; rebuild(); return this; }
    public BsSteps setCurrentColor(Color c) { this.currentColor = c; rebuild(); return this; }
    public BsSteps setWaitColor(Color c) { this.waitColor = c; rebuild(); return this; }
    public BsSteps setLineColor(Function<Boolean, Color> fn) { this.lineColorFn = fn; rebuild(); return this; }

    /** 整行总高度 = 圆 + 标题间距 + 标题。 */
    private float rowHeight() { return circleSize + titleGap + titleHeight; }

    private void rebuild() {
        clearChildren();
        for (int i = 0; i < titles.size(); i++) {
            final int idx = i;
            // 状态判定
            Color circleColor, ringColor = null, labelColor;
            String circleLabel;
            boolean outlined;
            if (i < current) {
                circleColor = doneColor;
                circleLabel = "✓";
                labelColor = BsTheme.tp();
                outlined = false;
            } else if (i == current) {
                circleColor = currentColor;
                ringColor = currentColor;
                circleLabel = String.valueOf(i + 1);
                labelColor = currentColor;
                outlined = false;
            } else {
                circleColor = null;
                ringColor = waitColor;
                circleLabel = String.valueOf(i + 1);
                labelColor = waitColor;
                outlined = true;
            }

            NodeActor node = new NodeActor(BsUI.getSkin(), circleColor, ringColor, outlined,
                    circleLabel, titles.get(i), labelColor,
                    circleSize, titleGap, titleHeight,
                    ringColor == currentColor);
            node.setSize(circleSize + 10, rowHeight());
            node.setTouchable(Touchable.enabled);
            node.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    setCurrent(idx);
                    return true;
                }
            });
            // 节点 cell：固定宽度 = circleSize+10，高度 = rowHeight，顶部对齐
            add(node).width(circleSize + 10).height(rowHeight()).top().padRight(0);

            // 连接线（除最后一个节点外）
            if (i < titles.size() - 1) {
                boolean segmentDone = (i < current);
                Color segColor = lineColorFn.apply(segmentDone);
                LineActor line = new LineActor(segColor, lineHeight,
                        circleSize / 2f,   // 线的 Y 偏移 = 圆心
                        lineLength, rowHeight(), segmentDone);
                line.setSize(lineLength, rowHeight());
                add(line).width(lineLength).height(rowHeight()).top().padRight(0);
            }
        }
    }

    /** 节点自绘 Actor：圆 + 圆内数字/勾 + 圆下方标题。 */
    private static class NodeActor extends Actor {
        private final Skin skin;
        private final Color fillColor;
        private final Color ringColor;
        private final boolean outlined;
        private final String circleLabel;
        private final String title;
        private final Color labelColor;
        private final float circleSize;
        private final float titleGap;
        private final float titleHeight;
        private final boolean hasRing;
        // Label 复用：构造期 new 一次，draw 里只更新属性，避免每帧 new 4 个对象（2 LabelStyle + 2 Label）
        private final Label circleText;
        private final Label titleText;

        NodeActor(Skin skin, Color fillColor, Color ringColor, boolean outlined,
                  String circleLabel, String title, Color labelColor,
                  float circleSize, float titleGap, float titleHeight, boolean hasRing) {
            this.skin = skin;
            this.fillColor = fillColor;
            this.ringColor = ringColor;
            this.outlined = outlined;
            this.circleLabel = circleLabel;
            this.title = title;
            this.labelColor = labelColor;
            this.circleSize = circleSize;
            this.titleGap = titleGap;
            this.titleHeight = titleHeight;
            this.hasRing = hasRing;
            // 预创建 Label + Style，draw 阶段只更新位置/颜色
            Label.LabelStyle clStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
            clStyle.font = skin.getFont("font-sm");
            circleText = new Label(circleLabel, clStyle);
            circleText.setAlignment(com.badlogic.gdx.utils.Align.center);
            circleText.setSize(circleSize, circleSize);

            Label.LabelStyle tlStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
            tlStyle.font = skin.getFont("font-sm");
            titleText = new Label(title, tlStyle);
            titleText.setAlignment(com.badlogic.gdx.utils.Align.center);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float w = getWidth();
            float cx = w / 2f;          // 整个节点宽度内居中（圆心也在 cx）
            float cy = circleSize / 2f; // 圆心 Y = 顶部 circleSize/2

            // === shape 阶段：画圆 ===
            if (sharedSR == null) sharedSR = new ShapeRenderer();
            batch.end();
            try {
                sharedSR.setProjectionMatrix(batch.getProjectionMatrix());
                sharedSR.setTransformMatrix(batch.getTransformMatrix());
                sharedSR.begin(ShapeRenderer.ShapeType.Filled);
                try {
                    sharedSR.translate(getX(), getY(), 0);
                    if (outlined) {
                        // WAIT：灰描边 + 白心（自绘两圈圆）
                        float r = circleSize / 2f;
                        sharedSR.setColor(ringColor.r, ringColor.g, ringColor.b, ringColor.a * parentAlpha);
                        sharedSR.circle(cx, cy, r);
                        sharedSR.setColor(1, 1, 1, parentAlpha);
                        sharedSR.circle(cx, cy, r - 2);
                    } else {
                        // CURRENT 的 ring（半透明同色背景）
                        if (hasRing) {
                            sharedSR.setColor(ringColor.r, ringColor.g, ringColor.b, 0.3f * parentAlpha);
                            sharedSR.circle(cx, cy, circleSize / 2f + 5);
                        }
                        // 实心圆
                        sharedSR.setColor(fillColor.r, fillColor.g, fillColor.b, fillColor.a * parentAlpha);
                        sharedSR.circle(cx, cy, circleSize / 2f);
                    }
                } finally {
                    sharedSR.identity();
                    sharedSR.end();
                }
            } finally {
                batch.begin();
            }

            // === batch 阶段：画数字/勾 + 标题（复用预创建 Label） ===
            // 圆内的数字/勾
            circleText.setColor(outlined ? labelColor : Color.WHITE);
            circleText.setBounds(getX() + cx - circleSize / 2f,
                    getY() + cy - circleSize / 2f,
                    circleSize, circleSize);
            circleText.draw(batch, parentAlpha);

            // 圆下方的标题
            titleText.setColor(labelColor);
            float tWidth = circleSize + 30;
            titleText.setSize(tWidth, titleHeight);
            titleText.setBounds(getX() + cx - tWidth / 2f,
                    getY() + circleSize + titleGap,
                    tWidth, titleHeight);
            titleText.draw(batch, parentAlpha);
        }
    }

    /** 一根水平线自绘 Actor。固定在 cell 高度的 centerY 处画。 */
    private static class LineActor extends Actor {
        private final Color color;
        private final float lineH;
        private final float centerY;       // 线的 Y 偏移（对应圆心位置）
        private final boolean done;

        LineActor(Color color, float lineH, float centerY, float width, float totalH, boolean done) {
            this.color = color;
            this.lineH = lineH;
            this.centerY = centerY;
            this.done = done;
        }

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
                    // 线段：垂直居中于 centerY，左右各延伸到 cell 边界（贴节点）
                    // cell 高度 = rowHeight，lineY 是相对 actor 原点
                    float y = centerY - lineH / 2f;
                    float alpha = (done ? 1f : 0.7f) * parentAlpha;
                    sharedSR.setColor(color.r, color.g, color.b, color.a * alpha);
                    sharedSR.rect(0, y, getWidth(), lineH);
                } finally {
                    sharedSR.identity();
                    sharedSR.end();
                }
            } finally {
                batch.begin();
            }
        }
    }
}
