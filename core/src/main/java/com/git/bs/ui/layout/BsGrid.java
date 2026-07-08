package com.git.bs.ui.layout;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

/**
 * 格子布局 —— 固定列数自动换行的网格，基于 {@link Table}。
 *
 * <p>定位：bsui 的「四种基础布局」之一（横 / 纵 / 格子 / 流式）。
 * 适合 KPI 卡片墙、缩略图墙、表单字段网格等「固定 N 列、多了自动进下一行」的场景。
 * 需要按容器宽度动态列数（CSS-grid 风格）请用 {@link BsFlow}。</p>
 *
 * <p>结构（columns=4）：</p>
 * <pre>
 *  ┌──────┬──────┬──────┬──────┐
 *  │ [A]  │ [B]  │ [C]  │ [D]  │   ← 第 1 行（4 个格子）
 *  ├──────┼──────┼──────┼──────┤
 *  │ [E]  │ [F]  │ [G]  │      │   ← 第 2 行（不满 4 个也自动成行）
 *  └──────┴──────┴──────┴──────┘
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsGrid grid = new BsGrid(4)        // 4 列
 *         .gap(8)                     // 格子间距 8px（同时影响行列间距）
 *         .pad(10)
 *         .fillX()                    // 每格横向填满
 *         .add(card1).add(card2).add(card3).add(card4)
 *         .add(card5).add(card6);     // 第 5、6 个自动进入第 2 行
 * stage.addActor(grid);
 * }</pre>
 *
 * <p>实现说明：{@link Table} 是行式布局（需手动 {@link Table#row()}），本类维护一个列计数器，
 * 每追加 {@link #columns} 个子节点自动调一次 {@code row()}，把「数行号」这个易错的样板代码封装掉。
 * 单元格的 {@link Cell} 配置通过 {@link #cellConfig(java.util.function.Consumer)} 统一施加。</p>
 *
 * @see BsRow 横排
 * @see BsCol 纵排
 * @see BsFlow 自适应换行流式
 */
public class BsGrid extends Table {

    private final int columns;
    private int cursor = 0;          // 当前行已填充的格子数
    private float gapX = 4, gapY = 4;
    private boolean fillX, fillY;
    private boolean growX, growY;

    /**
     * 创建网格。
     * @param columns 列数（&gt;=1）
     */
    public BsGrid(int columns) {
        if (columns < 1) throw new IllegalArgumentException("columns must be >= 1, got " + columns);
        this.columns = columns;
        top().left();
        defaults().top().left();
    }

    /** 列数。 */
    public int columns() { return columns; }

    /** 设置行列间距（px），同值作用于横向与纵向。 */
    public BsGrid gap(float px) { return gap(px, px); }

    /** 分别设置列间距、行间距。 */
    public BsGrid gap(float x, float y) {
        this.gapX = x; this.gapY = y;
        defaults().spaceLeft(x / 2f).spaceRight(x / 2f)
                .spaceTop(y / 2f).spaceBottom(y / 2f);
        return this;
    }

    /** 四向内边距。 */
    public BsGrid pad(float pad) { super.pad(pad); return this; }

    /** 分向内边距。 */
    public BsGrid pad(float top, float left, float bottom, float right) {
        super.pad(top, left, bottom, right); return this;
    }

    /** 整体对齐方式（"topleft" / "center" 等）。 */
    public BsGrid align(String align) {
        int a = BsRow.alignOf(align);
        super.align(a);
        return this;
    }

    /** 每个格子横向填满列宽。 */
    public BsGrid fillX() { this.fillX = true; defaults().fillX(); return this; }

    /** 每个格子纵向填满行高。 */
    public BsGrid fillY() { this.fillY = true; defaults().fillY(); return this; }

    /** 每个格子双向填满。 */
    public BsGrid fill() { return fillX().fillY(); }

    /** 每个格子横向 grow（占满剩余空间）。 */
    public BsGrid growX() { this.growX = true; defaults().growX(); return this; }

    /** 每个格子纵向 grow。 */
    public BsGrid growY() { this.growY = true; defaults().growY(); return this; }

    /** 每个格子双向 grow。 */
    public BsGrid grow() { return growX().growY(); }

    /**
     * 对每个新追加的格子施加额外配置（如 minWidth / uniform 等）。
     * <pre>{@code
     * new BsGrid(3).cellConfig(c -> c.minWidth(120).uniform())
     * }</pre>
     */
    public BsGrid cellConfig(java.util.function.Consumer<Cell<?>> config) {
        // 在 add() 里对每个 cell 调用 config
        this.cellConfig = config;
        return this;
    }

    private java.util.function.Consumer<Cell<?>> cellConfig = null;

    /**
     * 追加一个子节点。满 columns 个自动 {@code row()}。
     * @return this（链式）
     */
    @Override
    public <T extends Actor> Cell<T> add(T actor) {
        Cell<T> cell = super.add(actor);
        if (cellConfig != null) {
            try { cellConfig.accept(cell); } catch (Throwable ignored) {}
        }
        cursor++;
        if (cursor >= columns) {
            super.row();
            cursor = 0;
        }
        return cell;
    }

    /** 显式换行（极少用，因为 add 会自动换）。 */
    public BsGrid nextRow() {
        if (cursor != 0) { super.row(); cursor = 0; }
        return this;
    }

    /**
     * 链式追加多个子节点（builder 风格），满 columns 自动换行。
     * <p>与 {@link #add(Actor)} 区别：{@code add} 返回 {@link Cell}（可配 growX/height 等），
     * 本方法返回 {@code BsGrid} 自身，方便连续追加：</p>
     * <pre>{@code
     * new BsGrid(3).append(a, b, c).append(d, e, f);
     * // 等价于
     * grid.add(a); grid.add(b); grid.add(c); grid.add(d); grid.add(e); grid.add(f);
     * }</pre>
     */
    public BsGrid append(Actor... actors) {
        if (actors != null) for (Actor a : actors) super.add(a);
        return this;
    }

    /** 单个链式追加（不需配 Cell 时用），返回 this。 */
    public BsGrid append(Actor a) {
        super.add(a);
        return this;
    }
}
