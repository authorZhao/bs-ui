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
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.utils.Align;

/**
 * 横排布局 —— 子节点从左到右横向排列，{@link HorizontalGroup} 的薄封装。
 *
 * <p>定位：bsui 的「四种基础布局」之一（横 / 纵 / 格子 / 流式）。
 * 适合工具栏、按钮组、KPI 卡片行等「一行排开」的场景。
 * 需要按容器宽度自动换行请用 {@link BsFlow}；固定列数自动换行请用 {@link BsGrid}。</p>
 *
 * <p>结构：</p>
 * <pre>
 *  ┌──────────────────────────────┐
 *  │  [A]  [B]  [C]  [D]  [E]     │   ← 子节点横向堆叠，gap 控制间距
 *  └──────────────────────────────┘
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsRow row = new BsRow()
 *         .gap(8)              // 子节点间距 8px
 *         .pad(10)             // 内边距 10px
 *         .align("center")     // 整体垂直居中
 *         .fill(true)          // 子节点填满高度
 *         .add(btn1).add(btn2).add(btn3);
 * stage.addActor(row);
 *
 * // 等价于
 * HorizontalGroup g = new HorizontalGroup();
 * g.space(8).pad(10).align(Align.center).fill();
 * g.addActor(btn1); g.addActor(btn2); g.addActor(btn3);
 * }</pre>
 *
 * <p>实现说明：继承自 {@link HorizontalGroup}，本类仅做语义化命名 + builder 风格 API 转发，
 * 不引入额外能力（{@link HorizontalGroup#wrap(boolean) wrap} 留给 {@link BsFlow}）。</p>
 *
 * @see BsCol 纵排
 * @see BsGrid 固定列网格
 * @see BsFlow 自适应换行流式
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsRow extends HorizontalGroup {

    /** 创建横排，默认 gap=4、pad=0、垂直居中对齐。 */
    public BsRow() {
        space(4);
        align(Align.center);
    }

    /** 子节点之间的间距（px）。 */
    public BsRow gap(float px) { space(px); return this; }

    /** 四向内边距（px）。 */
    @Override
    public BsRow pad(float pad) { super.pad(pad); return this; }

    /** 分向内边距。 */
    @Override
    public BsRow pad(float top, float left, float bottom, float right) {
        super.pad(top, left, bottom, right); return this;
    }

    /**
     * 整体对齐方式（垂直方向决定子节点如何贴齐行高）。
     * @param align "top" / "center" / "bottom" / "left" / "right"
     */
    public BsRow align(String align) {
        super.align(alignOf(align));
        return this;
    }

    /** 子节点是否填满布局高度。 */
    public BsRow fill(boolean fill) { super.fill(fill ? 1f : 0f); return this; }

    /** 子节点是否同时 expand（占满剩余空间）。等价于 fill + expand。 */
    @Override
    public BsRow grow() { super.grow(); return this; }

    /** 反转排列顺序（右到左）。 */
    @Override
    public BsRow reverse(boolean r) { super.reverse(r); return this; }

    /** 追加一个子节点。 */
    public BsRow add(Actor a) { addActor(a); return this; }

    /** 一次性追加多个子节点。 */
    public BsRow addAll(Actor... actors) {
        if (actors != null) for (Actor a : actors) addActor(a);
        return this;
    }

    /** 字符串 → libgdx Align 常量。 */
    static int alignOf(String align) {
        if (align == null) return Align.center;
        switch (align.toLowerCase()) {
            case "top":         return Align.top;
            case "bottom":      return Align.bottom;
            case "left":        return Align.left;
            case "right":       return Align.right;
            case "topleft":
            case "top-left":    return Align.topLeft;
            case "topright":
            case "top-right":   return Align.topRight;
            case "bottomleft":
            case "bottom-left": return Align.bottomLeft;
            case "bottomright":
            case "bottom-right":return Align.bottomRight;
            default:            return Align.center;
        }
    }
}
