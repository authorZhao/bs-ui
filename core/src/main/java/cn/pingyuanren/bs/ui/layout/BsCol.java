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
package cn.pingyuanren.bs.ui.layout;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.Align;

/**
 * 纵排布局 —— 子节点从上到下纵向排列，{@link VerticalGroup} 的薄封装。
 *
 * <p>定位：bsui 的「四种基础布局」之一（横 / 纵 / 格子 / 流式）。
 * 适合表单项、卡片堆叠、日志/告警条目列表等「一列排开」的场景。</p>
 *
 * <p>结构：</p>
 * <pre>
 *  ┌──────────┐
 *  │  [A]     │   ← 子节点纵向堆叠，gap 控制间距
 *  │  [B]     │
 *  │  [C]     │
 *  │  [D]     │
 *  └──────────┘
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsCol col = new BsCol()
 *         .gap(6)              // 子节点间距 6px
 *         .pad(8)              // 内边距 8px
 *         .align("left")       // 整体左对齐
 *         .fill(true)          // 子节点填满宽度
 *         .add(title).add(body).add(footer);
 * stage.addActor(col);
 * }</pre>
 *
 * <p>实现说明：继承自 {@link VerticalGroup}，本类仅做语义化命名 + builder 风格 API 转发，
 * 不引入额外能力（{@link VerticalGroup#wrap(boolean) wrap} 留给流式场景，本类不暴露）。</p>
 *
 * @see BsRow 横排
 * @see BsGrid 固定列网格
 * @see BsFlow 自适应换行流式
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsCol extends VerticalGroup {

    /** 创建纵排，默认 gap=4、pad=0、左对齐。 */
    public BsCol() {
        space(4);
        align(Align.left);
    }

    /** 子节点之间的间距（px）。 */
    public BsCol gap(float px) { space(px); return this; }

    /** 四向内边距（px）。 */
    @Override
    public BsCol pad(float pad) { super.pad(pad); return this; }

    /** 分向内边距。 */
    @Override
    public BsCol pad(float top, float left, float bottom, float right) {
        super.pad(top, left, bottom, right); return this;
    }

    /**
     * 整体对齐方式（水平方向决定子节点如何贴齐列宽）。
     * @param align "left" / "center" / "right" / "top" / "bottom"
     */
    public BsCol align(String align) {
        super.align(BsRow.alignOf(align));
        return this;
    }

    /** 子节点是否填满布局宽度。 */
    public BsCol fill(boolean fill) { super.fill(fill ? 1f : 0f); return this; }

    /** 子节点是否同时 expand（占满剩余空间）。等价于 fill + expand。 */
    @Override
    public BsCol grow() { super.grow(); return this; }

    /** 反转排列顺序（下到上）。 */
    @Override
    public BsCol reverse(boolean r) { super.reverse(r); return this; }

    /** 追加一个子节点。 */
    public BsCol add(Actor a) { addActor(a); return this; }

    /** 一次性追加多个子节点。 */
    public BsCol addAll(Actor... actors) {
        if (actors != null) for (Actor a : actors) addActor(a);
        return this;
    }
}
