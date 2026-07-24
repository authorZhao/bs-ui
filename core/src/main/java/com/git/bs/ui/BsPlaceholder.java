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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Bootstrap 5 风格骨架屏（Placeholder）—— 加载时显示的灰色块状骨架，
 * 模拟即将出现的内容布局，比 spinner 更有"内容感"。
 *
 * <p>典型用法：卡片骨架（图 + 标题 + 2 行文字 + 按钮），列表骨架（图标 + 文字行 ×N）。
 * 配合 {@link #setPulsing(boolean)} 开启呼吸动画。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsPlaceholder card = BsPlaceholder.card(skin)   // 一键生成卡片骨架
 *         .pulsing(true);
 * stage.addActor(card);
 *
 * // 自定义
 * BsPlaceholder custom = new BsPlaceholder(skin);
 * custom.row().col(120, 16);   // 一行 120×16 的块
 * custom.row().col(80, 12);
 * custom.row().cols(0.5f, 0.5f, 0, 12);   // 两列各占一半宽度，高 12
 * custom.pulsing(true);
 * }</pre>
 *
 * <p>实现：每个块是一个 Container + 灰色 drawable 背景。
 * pulsing 用 Actions.sequence(fadeOut, fadeIn) forever 实现呼吸效果。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsPlaceholder extends Table {

    private boolean pulsing = false;
    private float pulseSpeed = 0.8f;
    /** V2：颜色存放在 skin，字段初始化时取不到，改成方法形式。 */
    private Color baseColorOverride = null;
    private Color baseColor(Skin skin) {
        return baseColorOverride != null ? baseColorOverride : BsTheme.bh();
    }

    public BsPlaceholder(Skin skin) {
        left().top();
        defaults().pad(4).left();
    }

    /** 进入新一行（不覆盖 Table.row()，使用独立方法名以便链式）。 */
    public BsPlaceholder newRow() {
        super.row();
        return this;
    }

    /** 加一个固定尺寸的灰色块。 */
    public BsPlaceholder col(float width, float height) {
        add(makeBlock(width, height)).width(width).height(height);
        return this;
    }

    /**
     * 一行多个块，按比例分配宽度。
     * @param widthRatios 各块占容器宽度的比例（0~1），最后一个可以用 0 表示"剩余宽度"
     * @param height 所有块的高度
     */
    public BsPlaceholder cols(float height, float... widthRatios) {
        for (float r : widthRatios) {
            Container<?> c = makeBlock(0, height);
            if (r > 0) {
                add(c).height(height).growX().width(r * 100);   // 用 prefWidth 暗示比例
            } else {
                add(c).height(height).growX();
            }
        }
        return this;
    }

    /** 一行两块：前 width1 占比 + 间隔 + 后 width2 占比，高度 height。 */
    public BsPlaceholder twoCols(float widthRatio1, float widthRatio2, float height) {
        Container<?> c1 = makeBlock(0, height);
        Container<?> c2 = makeBlock(0, height);
        add(c1).height(height).growX();
        add(c2).height(height).growX().width(widthRatio2 / widthRatio1 * 100);
        return this;
    }

    /** 灰色圆角块（Container + 染色 drawable）。 */
    private Container<?> makeBlock(float w, float h) {
        Container<?> c = new Container<>();
        Color col = baseColor(getSkin());
        Drawable d = getSkin() != null
                ? getSkin().newDrawable("bs-progress-track", col)
                : null;
        if (d == null && getSkin() != null) {
            d = BsSkinFactory.drawableOf(col);
        }
        if (d != null) c.setBackground(d);
        c.fill();
        c.size(w, h);
        return c;
    }

    /** 开启/关闭呼吸动画。 */
    public BsPlaceholder pulsing(boolean on) {
        this.pulsing = on;
        if (on) {
            // 当前 actor 上加 forever action
            clearActions();
            float half = pulseSpeed / 2f;
            addAction(Actions.forever(Actions.sequence(
                    Actions.alpha(0.4f, half, Interpolation.fade),
                    Actions.alpha(1f, half, Interpolation.fade)
            )));
        } else {
            clearActions();
            addAction(Actions.alpha(1f));
        }
        return this;
    }

    public BsPlaceholder setPulseSpeed(float sec) {
        this.pulseSpeed = sec;
        if (pulsing) pulsing(true);
        return this;
    }

    public BsPlaceholder setBlockColor(Color c) {
        this.baseColorOverride = c;
        // 重建所有块（简单方案：清掉重建）
        return this;
    }

    // ========================= 预设模板 =========================

    /** 一键生成卡片骨架（图 + 标题 + 2 行文字 + 按钮占位）。 */
    public static BsPlaceholder card(Skin skin) {
        BsPlaceholder p = new BsPlaceholder(skin);
        // 图片占位（满宽 × 120）
        p.col(360, 120);
        p.row();
        // 标题（半宽 × 18）
        p.col(180, 18);
        p.row();
        // 正文 2 行
        p.col(320, 12);
        p.row();
        p.col(280, 12);
        p.row();
        // 按钮（80 × 28）
        p.col(80, 28);
        return p;
    }

    /** 一键生成列表项骨架（方形图标 + 主标题 + 副标题）。 */
    public static BsPlaceholder listItem(Skin skin) {
        BsPlaceholder p = new BsPlaceholder(skin);
        // 32×32 图标 + 文字列
        p.col(32, 32);
        Table textCol = new Table();
        textCol.left().top();
        textCol.defaults().pad(2).left();
        BsPlaceholder t1 = new BsPlaceholder(skin);
        t1.col(120, 14);
        BsPlaceholder t2 = new BsPlaceholder(skin);
        t2.col(80, 10);
        textCol.add(t1).left().row();
        textCol.add(t2).left();
        p.add(textCol).padLeft(8).growX();
        return p;
    }
}
