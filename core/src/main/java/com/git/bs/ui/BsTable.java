package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Bootstrap 风格表格：表头 + 数据行。
 *
 * <p>两种 cell 模式（{@link #setCellMode}）：</p>
 * <ul>
 *   <li><b>BUTTON</b>（默认）：每个 cell 是 TextButton，原行为兼容老代码</li>
 *   <li><b>LABEL</b>：cell 是纯 Label（不可单独点击），整行作为单位选中/hover，
 *       视觉更接近 Bootstrap 真实表格，性能也更好（少 N×M 个按钮）</li>
 * </ul>
 *
 * <p>LABEL 模式下：</p>
 * <ul>
 *   <li>{@link #setSelectableRows(true)} 开启行选中（多选/单选由 {@link #setMultiSelect} 控制）</li>
 *   <li>{@link #setCheckedColumnVisible(true)} 显示一个勾选列（左起第一列），单独点击切换该行选中</li>
 *   <li>{@link #setOnRowClick} 仍然触发（点击非勾选列的位置时）</li>
 *   <li>{@link #setOnHeaderClick} 排序回调不变</li>
 * </ul>
 *
 * <p>用法（LABEL 模式 + 勾选列）：</p>
 * <pre>{@code
 * BsTable t = new BsTable(skin);
 * t.setCellMode(BsTable.CellMode.LABEL);
 * t.setHeaders("ID", "名称", "状态");
 * t.setSelectableRows(true);
 * t.setMultiSelect(true);
 * t.setCheckedColumnVisible(true);
 * t.setData(rows);
 * t.setOnRowClick(row -> setStatus("点击行: " + row));
 * }</pre>
 */
@Slf4j
public class BsTable extends Table {

    public enum CellMode { BUTTON, LABEL }

    @Getter private List<String> headers = Collections.emptyList();
    @Getter private List<List<String>> data = Collections.emptyList();
    private IntConsumer onRowClick;
    private IntConsumer onHeaderClick;
    /** LABEL 模式下，勾选列点击回调（参数 = 行索引 0..n-1，即当前页内索引）。 */
    private IntConsumer onCheckToggle;
    private float colWidth = 120f;
    /** 当前选中行（单选模式，-1 = 无）。 */
    private int selectedRow = -1;
    /** 多选模式下选中的行集合。 */
    private final BitSet selectedRows = new BitSet();
    /** 点击行切换选中（BUTTON 模式用）。 */
    private boolean toggleOnClick = true;

    private CellMode cellMode = CellMode.BUTTON;
    /** LABEL 模式下，是否允许行选中（默认 true）。 */
    private boolean selectableRows = false;
    /** LABEL 模式下，是否多选（默认 false 单选）。 */
    private boolean multiSelect = false;
    /** LABEL 模式下，是否显示勾选列（最左侧 checkbox 列）。 */
    private boolean checkedColumnVisible = false;
    /** 勾选列宽度。 */
    private float checkColWidth = 36;

    public BsTable(Skin skin) {
        left().top();
        defaults().pad(2);
    }

    // ========================= 配置 =========================

    public void setHeaders(String... headers) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, headers);
        this.headers = list;
        rebuild();
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers != null ? headers : Collections.emptyList();
        rebuild();
    }

    public void setData(List<List<String>> data) {
        this.data = data != null ? data : Collections.emptyList();
        rebuild();
    }

    public void setColWidth(float w) {
        this.colWidth = w;
        rebuild();
    }

    public void setOnRowClick(IntConsumer cb) { this.onRowClick = cb; }
    public void setOnHeaderClick(IntConsumer cb) { this.onHeaderClick = cb; }
    public void setOnCheckToggle(IntConsumer cb) { this.onCheckToggle = cb; }

    public void setSelectedRow(int row) {
        this.selectedRow = row;
        rebuild();
    }

    public int getSelectedRow() { return selectedRow; }

    public void setToggleOnClick(boolean t) { this.toggleOnClick = t; }

    /** 设置 cell 模式（BUTTON / LABEL）。 */
    public BsTable setCellMode(CellMode m) { this.cellMode = m; rebuild(); return this; }
    public CellMode getCellMode() { return cellMode; }

    /** LABEL 模式：是否允许行选中。 */
    public BsTable setSelectableRows(boolean s) { this.selectableRows = s; rebuild(); return this; }

    /** LABEL 模式：单选/多选。 */
    public BsTable setMultiSelect(boolean m) { this.multiSelect = m; rebuild(); return this; }
    public boolean isMultiSelect() { return multiSelect; }

    /** LABEL 模式：是否显示勾选列（默认不显示）。 */
    public BsTable setCheckedColumnVisible(boolean v) { this.checkedColumnVisible = v; rebuild(); return this; }

    /** 多选模式下获取所有选中行（LABEL 模式 + 勾选列）。 */
    public List<Integer> getSelectedRows() {
        List<Integer> r = new ArrayList<>();
        if (multiSelect) {
            for (int i = selectedRows.nextSetBit(0); i >= 0; i = selectedRows.nextSetBit(i + 1)) r.add(i);
        } else if (selectedRow >= 0) {
            r.add(selectedRow);
        }
        return r;
    }

    /** 当前数据行数。 */
    public int getRowCount() { return data.size(); }

    /** 取第 row 行数据。 */
    public List<String> getRow(int row) {
        if (row < 0 || row >= data.size()) return Collections.emptyList();
        return data.get(row);
    }

    // ========================= rebuild =========================

    private void rebuild() {
        clearChildren();
        if (cellMode == CellMode.LABEL) {
            rebuildLabelMode();
        } else {
            rebuildButtonMode();
        }
    }

    /** LABEL 模式渲染。 */
    private void rebuildLabelMode() {
        Skin skin = BsUI.getSkin();
        int realCols = Math.max(headers.size(), firstRowLen());
        int totalCols = realCols + (checkedColumnVisible ? 1 : 0);

        // 表头
        if (!headers.isEmpty()) {
            if (checkedColumnVisible) {
                Label empty = new Label("", skin);
                Container<Label> hw = new Container<>(empty);
                hw.setBackground(skin.getDrawable("bs-menu-bar-bg"));
                hw.fill();
                add(hw).width(checkColWidth).height(28).pad(0);
            }
            for (int c = 0; c < headers.size(); c++) {
                final int col = c;
                Label h = new Label(headers.get(c), skin);
                h.setColor(BsTheme.tp());
                Container<Label> hw = new Container<>(h);
                hw.setBackground(skin.getDrawable("bs-menu-bar-bg"));
                hw.fill();
                hw.pad(0, 8, 0, 8);
                hw.left();
                // 排序点击
                if (onHeaderClick != null) {
                    hw.setTouchable(Touchable.enabled);
                    hw.addListener(new ClickListener() {
                        @Override public void clicked(InputEvent event, float x, float y) {
                            try { onHeaderClick.accept(col); } catch (Throwable t) { log.warn("header click", t); }
                        }
                    });
                }
                add(hw).width(colWidth).height(28).pad(0);
            }
            row();
            // 分隔线
            Container<Label> sep = new Container<>(new Label("", skin));
            sep.background(skin.newDrawable("white", BsTheme.bds()));
            sep.height(1f).fillX();
            add(sep).colspan(totalCols).growX().pad(0);
            row();
        }

        // 数据行
        for (int r = 0; r < data.size(); r++) {
            final int row = r;
            List<String> cells = data.get(r);
            boolean isSelected = multiSelect ? selectedRows.get(r) : (selectedRow == r);

            // 勾选列
            if (checkedColumnVisible) {
                Drawable checkD = isSelected
                        ? skin.getDrawable("bs-check-on")
                        : skin.getDrawable("bs-check-off");
                Image cb = new Image(checkD);
                cb.setScaling(Scaling.fit);
                Container<Image> cbWrap = new Container<>(cb);
                cbWrap.fill();
                cbWrap.setBackground(rowBg(isSelected, false));
                if (selectableRows) {
                    cbWrap.setTouchable(Touchable.enabled);
                    cbWrap.addListener(new ClickListener() {
                        @Override public void clicked(InputEvent event, float x, float y) {
                            toggleRowSelection(row);
                            if (onCheckToggle != null) {
                                try { onCheckToggle.accept(row); } catch (Throwable t) { log.warn("check toggle", t); }
                            }
                        }
                    });
                }
                add(cbWrap).width(checkColWidth).height(28).pad(0);
            }

            // 数据 cell（Label）
            for (int c = 0; c < cells.size(); c++) {
                Label cell = new Label(safe(cells, c), skin);
                cell.setColor(isSelected ? Color.WHITE : BsTheme.tp());
                Container<Label> cw = new Container<>(cell);
                cw.fill();
                cw.pad(0, 8, 0, 8);
                cw.left();
                cw.setBackground(rowBg(isSelected, false));
                add(cw).width(colWidth).height(28).pad(0);
            }
            row();

            // 整行点击监听：用最后添加的 cell 不可，需要给整行 row 容器加 ——
            // scene2d Table 的 row 没有"行容器"概念，简化：给每行的最后一个 cell 加监听
            // 更稳妥：每行所有 cell 各加一份相同 listener
            if (selectableRows && !checkedColumnVisible) {
                // 给本行最后几个 cell 加点击
                int cellsThisRow = getChildren().size;   // 不准，仅占位
            }
        }

        // 整行点击：把每行所有 cell 通过 cell-level listener 触发
        // 上面没加 listener，这里用一个 trick：rebuild 完成后遍历 children 加 listener
        if (selectableRows) {
            attachRowClickListeners();
        }

        if (data.isEmpty() && headers.isEmpty()) {
            add(new Label("(空表)", skin)).pad(8);
        }
    }

    /** 给 LABEL 模式每行的所有 cell（含勾选列除外）绑定点击回调。 */
    private void attachRowClickListeners() {
        Skin skin = BsUI.getSkin();
        // 由于 rebuild 时 cell 是按行顺序加入的，按行分组绑定
        int totalCols = Math.max(headers.size(), firstRowLen()) + (checkedColumnVisible ? 1 : 0);
        // 表头占用一行（如果 headers 非空）+ 分隔线行
        int headerRows = headers.isEmpty() ? 0 : 2;
        // 遍历 children 按 row 分组
        com.badlogic.gdx.utils.Array<Actor> children = getChildren();
        int dataStart = headerRows * totalCols + (headerRows > 0 ? 1 : 0);   // 跳过表头 + 分隔线
        // 实际 children 顺序：表头 cells → 分隔线 → 第 0 行 cells → 第 1 行 cells → ...
        // 简化：用 indexOf 找分隔线后开始
        int sepIdx = -1;
        for (int i = 0; i < children.size; i++) {
            // 分隔线是一个 Container，宽度 colspan，靠它定位
            if (headerRows > 0 && i >= totalCols) {
                sepIdx = i;
                break;
            }
        }
        final int startIdx = sepIdx >= 0 ? sepIdx + 1 : (headerRows > 0 ? totalCols : 0);

        // 遍历每行
        for (int r = 0; r < data.size(); r++) {
            final int row = r;
            int base = startIdx + r * totalCols;
            int startCol = checkedColumnVisible ? 1 : 0;   // 跳过勾选列
            for (int c = startCol; c < totalCols; c++) {
                int idx = base + c;
                if (idx >= children.size) break;
                Actor cellActor = children.get(idx);
                cellActor.setTouchable(Touchable.enabled);
                cellActor.clearListeners();
                cellActor.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        // 勾选列存在时，行点击仅触发 onRowClick，不切换选中
                        if (checkedColumnVisible) {
                            if (onRowClick != null) {
                                try { onRowClick.accept(row); } catch (Throwable t) { log.warn("row click", t); }
                            }
                        } else {
                            if (selectableRows) {
                                toggleRowSelection(row);
                            }
                            if (onRowClick != null) {
                                try { onRowClick.accept(row); } catch (Throwable t) { log.warn("row click", t); }
                            }
                        }
                    }
                    @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        boolean sel = multiSelect ? selectedRows.get(row) : (selectedRow == row);
                        if (!sel && pointer == -1) {
                            ((Container<?>) cellActor).setBackground(skin.getDrawable("bs-menu-title-hover"));
                        }
                    }
                    @Override public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        boolean sel = multiSelect ? selectedRows.get(row) : (selectedRow == row);
                        if (!sel) {
                            ((Container<?>) cellActor).setBackground(rowBg(false, false));
                        }
                    }
                });
            }
        }
    }

    /** 切换某行选中状态（LABEL 模式）。 */
    private void toggleRowSelection(int row) {
        if (multiSelect) {
            selectedRows.flip(row);
        } else {
            selectedRow = (selectedRow == row) ? -1 : row;
        }
        rebuild();
    }

    /** 选中行的背景 / 普通行的背景。 */
    private Drawable rowBg(boolean selected, boolean hovered) {
        if (selected) return BsUI.getSkin().getDrawable("bs-list-selection");
        return (Drawable) null;
    }

    private int firstRowLen() {
        return data.isEmpty() ? 0 : data.get(0).size();
    }

    /** BUTTON 模式渲染（原实现，保持兼容）。 */
    private void rebuildButtonMode() {
        Skin skin = BsUI.getSkin();
        // 表头
        if (!headers.isEmpty()) {
            for (int c = 0; c < headers.size(); c++) {
                final int col = c;
                TextButton h = new TextButton(headers.get(c), skin, "bs-menu-title");
                h.getLabel().setColor(BsTheme.tp());
                h.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        if (onHeaderClick != null) {
                            try { onHeaderClick.accept(col); } catch (Throwable t) { log.warn("onHeaderClick", t); }
                        }
                    }
                });
                add(h).width(colWidth).height(28);
            }
            row();
            Container<Label> sep = new Container<>(new Label("", skin));
            sep.background(skin.newDrawable("white", BsTheme.bds()));
            sep.height(1f).fillX();
            add(sep).colspan(headers.size()).growX();
            row();
        }

        for (int r = 0; r < data.size(); r++) {
            final int row = r;
            List<String> cells = data.get(r);
            boolean isSelected = (row == selectedRow);
            for (int c = 0; c < cells.size(); c++) {
                TextButton cell = new TextButton(safe(cells, c), skin, "bs-menu-item");
                if (isSelected) {
                    com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle selStyle =
                            new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle(
                                    skin.get("bs-menu-item", com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle.class));
                    selStyle.up = skin.getDrawable("bs-list-selection");
                    selStyle.over = skin.getDrawable("bs-list-selection");
                    selStyle.down = skin.getDrawable("bs-list-selection");
                    selStyle.fontColor = Color.WHITE;
                    cell.setStyle(selStyle);
                }
                cell.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        if (toggleOnClick && selectedRow == row) {
                            setSelectedRow(-1);
                        } else {
                            setSelectedRow(row);
                        }
                        if (onRowClick != null) {
                            try { onRowClick.accept(row); } catch (Throwable t) { log.warn("onRowClick", t); }
                        }
                    }
                });
                add(cell).width(colWidth).height(26);
            }
            row();
        }

        if (data.isEmpty() && headers.isEmpty()) {
            add(new Label("(空表)", skin)).pad(8);
        }
    }

    private static String safe(List<String> row, int idx) {
        return (idx < row.size()) ? (row.get(idx) != null ? row.get(idx) : "") : "";
    }
}
