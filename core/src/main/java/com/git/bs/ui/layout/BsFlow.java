package com.git.bs.ui.layout;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.utils.Align;

/**
 * 流式自适应布局 —— 子节点横向排列，宽度不够自动换行，{@link HorizontalGroup} 的 {@code wrap(true)} 封装。
 *
 * <p>定位：bsui 的「四种基础布局」之一（横 / 纵 / 格子 / 流式）。
 * 适合标签云、徽章集合、搜索建议、不固定个数的卡片集合等「容器宽度可变、子节点个数不定」的场景。
 * 固定列数请用 {@link BsGrid}；单行不换行用 {@link BsRow}。</p>
 *
 * <p>结构（宽度变化时自动重排）：</p>
 * <pre>
 *  宽度足够：  [A] [B] [C] [D] [E] [F] [G]
 *
 *  宽度变窄：  [A] [B] [C] [D]
 *              [E] [F] [G]
 *
 *  更窄：      [A] [B]
 *              [C] [D]
 *              [E] [F]
 *              [G]
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsFlow flow = new BsFlow()
 *         .gap(8)                  // 同行内子节点间距
 *         .rowGap(6)               // 换行后的行间距
 *         .pad(10)
 *         .align("left")           // 首行起始对齐
 *         .rowAlign("center")      // 每行内子节点垂直对齐
 *         .add(tag1).add(tag2).add(tag3);
 * stage.addActor(flow);
 *
 * // 流式必须放在「会施加宽度」的父容器里（如 Table 的 growX 单元格），否则没有可换行的边界。
 * }</pre>
 *
 * <p><b>关键注意</b>：流式布局的换行依赖「外部施加的宽度」。如果把它直接 {@code stage.addActor(flow)}
 * 而不设宽，它的 {@code getPrefWidth()} 会是全部子节点之和（不会换行）。
 * 通常的用法是放进 {@code Table} 的 {@code growX()} 单元格，或自己 {@code setWidth(...)} + {@code validate()}。</p>
 *
 * @see BsRow 横排（不换行）
 * @see BsGrid 固定列网格
 */
public class BsFlow extends HorizontalGroup {

    /** 创建流式布局，默认 gap=4、rowGap=4、wrap=true、左对齐。 */
    public BsFlow() {
        space(4);
        wrapSpace(4);
        wrap(true);
        align(Align.topLeft);
        rowAlign(Align.center);
    }

    /** 同行内子节点之间的间距（px）。 */
    public BsFlow gap(float px) { space(px); return this; }

    /** 换行后行与行之间的间距（px）。 */
    public BsFlow rowGap(float px) { wrapSpace(px); return this; }

    /** 四向内边距。 */
    @Override
    public BsFlow pad(float pad) { super.pad(pad); return this; }

    /** 分向内边距。 */
    @Override
    public BsFlow pad(float top, float left, float bottom, float right) {
        super.pad(top, left, bottom, right); return this;
    }

    /**
     * 整体对齐方式（容器空间多余时，整组流贴向哪边）。
     * @param align "topleft" / "center" / "topright" / ...
     */
    public BsFlow align(String align) {
        super.align(BsRow.alignOf(align));
        return this;
    }

    /**
     * 每一行内子节点的垂直对齐方式。
     * @param align "top" / "center" / "bottom"
     */
    public BsFlow rowAlign(String align) {
        super.rowAlign(BsRow.alignOf(align));
        return this;
    }

    /** 反转排列顺序。 */
    @Override
    public BsFlow reverse(boolean r) { super.reverse(r); return this; }

    /** 反转换行方向（默认往下换，true 则往上换）。 */
    @Override
    public BsFlow wrapReverse(boolean r) { super.wrapReverse(r); return this; }

    /** 追加一个子节点。 */
    public BsFlow add(Actor a) { addActor(a); return this; }

    /** 一次性追加多个子节点。 */
    public BsFlow addAll(Actor... actors) {
        if (actors != null) for (Actor a : actors) addActor(a);
        return this;
    }

    /** 返回布局后实际换出的行数（{@code validate()} 后有效）。 */
    public int rows() { return getRows(); }
}
