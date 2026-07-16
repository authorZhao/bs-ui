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

import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bootstrap 风格增强数据表格（DataTable）——
 * 把 {@link BsTable} + {@link BsPagination} + 排序 + 行选择 + 空状态({@link BsEmpty}) 整合成开箱即用的组件。
 *
 * <p>功能：</p>
 * <ul>
 *   <li><b>分页</b>：内置 Pagination，自动按 pageSize 切页</li>
 *   <li><b>排序</b>：点表头列触发排序（业务方可提供 Comparator，或用默认字符串比较）</li>
 *   <li><b>行选择</b>：单选/多选，{@link #setSelected} / {@link #getSelectedIndices}</li>
 *   <li><b>空状态</b>：data 为空时自动显示 BsEmpty，避免空白</li>
 *   <li><b>加载态</b>：可选 LoadingOverlay 叠加（业务方控制显示/隐藏）</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsDataTable dt = new BsDataTable(skin);
 * dt.setHeaders("ID", "姓名", "年龄", "状态");
 * dt.setPageSize(10);
 * dt.setData(rows);   // List<List<String>>
 * dt.setMultiSelect(false);
 * dt.setOnRowSelect(idx -> setStatus("选中: " + dt.getRow(idx)));
 * dt.setSortable(true);   // 启用点击表头排序
 * dt.setOnSort((colIdx, ascending) -> sortData(colIdx, ascending));
 * stage.addActor(dt);
 * }</pre>
 *
 * <p>实现：竖向 Table = [工具栏（可选）] + [BsTable 滚动区] + [分页栏 + 信息文字]。
 * 排序时重排 data 后调 {@link #refreshPage()}。
 * 空状态：data 为空时用 BsEmpty 替换表格内容。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsDataTable extends Table {

    /** 排序回调：列索引 + 是否升序。 */
    public interface SortCallback {
        void onSort(int colIdx, boolean ascending);
    }

    private final BsTable table;
    private final BsPagination pagination;
    private final BsScrollPane tableScroll;
    private final Container<com.badlogic.gdx.scenes.scene2d.Actor> bodyWrap;
    private final Label infoLabel;
    private final Table footerRow;

    private List<List<String>> allData = new ArrayList<>();
    private List<String> headers;
    private int pageSize = 10;
    private int currentPage = 1;
    private boolean multiSelect = false;
    /** 启用 LABEL 模式 + 勾选列（行点击不切换选中，仅勾选列切换）。 */
    private boolean useLabelModeWithCheck = false;
    private boolean sortable = false;
    private int sortCol = -1;
    private boolean sortAscending = true;
    private SortCallback onSort;
    private Consumer<Integer> onRowSelect;
    private Comparator<List<String>> colComparator;
    private final BitSet selected = new BitSet();

    public BsDataTable(Skin skin) {
        left().top();
        defaults().growX().left();

        // 表格本体（用 Container 包装方便切换 Empty）
        table = new BsTable(skin);
        table.setOnRowClick(this::handleRowClick);
        table.setOnHeaderClick(this::handleHeaderClick);
        table.setOnCheckToggle(this::handleCheckToggle);
        tableScroll = new BsScrollPane(table, skin);
        tableScroll.setFadeScrollBars(false);
        tableScroll.setScrollingDisabled(true, false);

        // body 容器：装表格或空状态
        bodyWrap = new Container<>();
        bodyWrap.fill();
        bodyWrap.setActor(tableScroll);
        add(bodyWrap).growX().height(280).row();

        // 底部：分页 + 信息
        footerRow = new Table();
        footerRow.left();
        footerRow.defaults().pad(4);
        Label.LabelStyle infoStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        infoStyle.font = skin.getFont("font-sm");
        infoLabel = new Label(BsI18n.get("table.no_data", "(无数据)"), infoStyle);
        infoLabel.setColor(BsPalette.SECONDARY.getMain());
        pagination = new BsPagination(skin);
        pagination.setOnChange(page -> {
            currentPage = page;
            refreshPage();
        });
        footerRow.add(infoLabel).growX().left();
        footerRow.add(pagination).right();
        add(footerRow).growX().padTop(4).row();
    }

    // ========================= 数据 =========================

    public BsDataTable setHeaders(String... hs) {
        this.headers = new ArrayList<>(java.util.Arrays.asList(hs));
        table.setHeaders(headers);
        return this;
    }

    public BsDataTable setHeaders(List<String> hs) {
        this.headers = new ArrayList<>(hs);
        table.setHeaders(headers);
        return this;
    }

    /**
     * 设置全量数据（会自动算分页、刷新当前页）。
     * @param rows 每行 = List<String>（每列一个值）
     */
    public BsDataTable setData(List<List<String>> rows) {
        this.allData = new ArrayList<>(rows);
        this.selected.clear();
        if (pageSize <= 0) pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil(allData.size() * 1f / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;
        pagination.setTotalPages(totalPages);
        pagination.setCurrentPage(currentPage);
        refreshPage();
        return this;
    }

    /** 重新刷新当前页数据 + 信息文字 + 空状态切换。 */
    public void refreshPage() {
        // 空状态
        if (allData.isEmpty()) {
            bodyWrap.setActor(new BsEmpty(BsUI.getSkin())
                    .title(BsI18n.get("table.empty", "暂无数据"))
                    .description(BsI18n.get("table.empty_hint", "当前列表为空")));
            infoLabel.setText(BsI18n.get("table.zero_count", "(共 0 条)"));
            return;
        }
        if (!(bodyWrap.getActor() instanceof BsScrollPane)) {
            bodyWrap.setActor(tableScroll);
        }
        int from = (currentPage - 1) * pageSize;
        int to = Math.min(from + pageSize, allData.size());
        List<List<String>> page = new ArrayList<>(allData.subList(from, to));
        table.setData(page);
        infoLabel.setText(BsI18n.get("table.page_info", "共 {0} 条，第 {1}/{2} 页，本页 {3} 条",
                allData.size(), currentPage, pagination.getTotalPages(), page.size()));
    }

    public int getRowCount() { return allData.size(); }
    public List<String> getRow(int idx) {
        if (idx < 0 || idx >= allData.size()) return new ArrayList<>();
        return allData.get(idx);
    }
    public List<List<String>> getData() { return allData; }

    public BsDataTable setPageSize(int n) { this.pageSize = Math.max(1, n); return this; }
    public int getPageSize() { return pageSize; }
    public int getCurrentPage() { return currentPage; }

    // ========================= 选择 =========================

    public BsDataTable setMultiSelect(boolean m) { this.multiSelect = m; return this; }
    public boolean isMultiSelect() { return multiSelect; }

    /**
     * 启用「LABEL 模式 + 勾选列」：cell 是纯 Label，最左侧出现勾选列单独切换选中，
     * 行点击不再自动 toggle 选中（只触发 onRowSelect 回调）。
     * 勾选列点击会触发 onRowSelect。
     * @param multi 是否多选（true 多选 / false 单选）
     */
    public BsDataTable setLabelModeWithCheckColumn(boolean multi) {
        this.useLabelModeWithCheck = true;
        this.multiSelect = multi;
        table.setCellMode(BsTable.CellMode.LABEL);
        table.setSelectableRows(true);
        table.setMultiSelect(multi);
        table.setCheckedColumnVisible(true);
        return this;
    }

    /** 勾选列点击 → 切换 BitSet 并通知回调。 */
    private void handleCheckToggle(int rowInCurrentPage) {
        int absIdx = (currentPage - 1) * pageSize + rowInCurrentPage;
        boolean wasSelected = selected.get(absIdx);
        if (!multiSelect) selected.clear();
        selected.set(absIdx, !wasSelected);
        if (onRowSelect != null) {
            try { onRowSelect.accept(absIdx); } catch (Throwable t) { log.warn("onRowSelect", t); }
        }
    }

    public BsDataTable setSelected(int rowIdx, boolean sel) {
        if (rowIdx < 0 || rowIdx >= allData.size()) return this;
        if (!multiSelect && sel) selected.clear();
        selected.set(rowIdx, sel);
        // 同步视觉到 BsTable：单选模式下把绝对 idx 映射到当前页 idx
        int pageRowIdx = rowIdx - (currentPage - 1) * pageSize;
        if (pageRowIdx >= 0 && pageRowIdx < pageSize) {
            table.setSelectedRow(sel ? pageRowIdx : -1);
        }
        if (onRowSelect != null) {
            try { onRowSelect.accept(rowIdx); } catch (Throwable t) { log.warn("onRowSelect", t); }
        }
        return this;
    }

    public List<Integer> getSelectedIndices() {
        List<Integer> r = new ArrayList<>();
        for (int i = selected.nextSetBit(0); i >= 0; i = selected.nextSetBit(i + 1)) r.add(i);
        return r;
    }

    public List<List<String>> getSelectedRows() {
        List<List<String>> r = new ArrayList<>();
        for (int i = selected.nextSetBit(0); i >= 0; i = selected.nextSetBit(i + 1)) {
            if (i < allData.size()) r.add(allData.get(i));
        }
        return r;
    }

    public BsDataTable clearSelection() { selected.clear(); return this; }

    public BsDataTable setOnRowSelect(Consumer<Integer> cb) { this.onRowSelect = cb; return this; }

    private void handleRowClick(int rowInCurrentPage) {
        // 转成全量 data 的索引
        int absIdx = (currentPage - 1) * pageSize + rowInCurrentPage;
        // BUTTON 模式 / LABEL 模式无勾选列：行点击切换选中
        // LABEL 模式 + 勾选列：行点击仅触发回调，不切换选中（让勾选列单独管）
        if (!useLabelModeWithCheck) {
            boolean wasSelected = selected.get(absIdx);
            if (!multiSelect) selected.clear();
            selected.set(absIdx, !wasSelected);
        }
        if (onRowSelect != null) {
            try { onRowSelect.accept(absIdx); } catch (Throwable t) { log.warn("onRowSelect", t); }
        }
    }

    // ========================= 排序 =========================

    public BsDataTable setSortable(boolean s) { this.sortable = s; return this; }
    public boolean isSortable() { return sortable; }

    /** 提供自定义列比较器（默认按字符串比较）。 */
    public BsDataTable setColumnComparator(Comparator<List<String>> c) {
        this.colComparator = c;
        return this;
    }

    public BsDataTable setOnSort(SortCallback cb) { this.onSort = cb; return this; }

    private void handleHeaderClick(int colIdx) {
        if (!sortable) return;
        if (sortCol == colIdx) {
            sortAscending = !sortAscending;
        } else {
            sortCol = colIdx;
            sortAscending = true;
        }
        doSort(colIdx, sortAscending);
        if (onSort != null) {
            try { onSort.onSort(colIdx, sortAscending); } catch (Throwable t) { log.warn("onSort", t); }
        }
        currentPage = 1;
        pagination.setCurrentPage(1);
        refreshPage();
    }

    private void doSort(int colIdx, boolean ascending) {
        Comparator<List<String>> cmp = colComparator != null ? colComparator
                : (a, b) -> {
                    String va = colIdx < a.size() ? a.get(colIdx) : "";
                    String vb = colIdx < b.size() ? b.get(colIdx) : "";
                    // 数字优先按数字比
                    try {
                        return Double.compare(Double.parseDouble(va), Double.parseDouble(vb));
                    } catch (NumberFormatException ignored) {
                        return va.compareToIgnoreCase(vb);
                    }
                };
        if (!ascending) cmp = cmp.reversed();
        try {
            allData.sort(cmp);
        } catch (Throwable t) {
            log.warn("sort failed", t);
        }
    }

    public int getSortColumn() { return sortCol; }
    public boolean isSortAscending() { return sortAscending; }

    // ========================= 杂项 =========================

    /** 业务方可注入"加载更多"按钮到工具栏（未来扩展）。 */
    public BsDataTable setOnRefresh(Supplier<List<List<String>>> dataProvider) {
        // 简化：调用方主动调 setData 即可，这里仅留接口
        return this;
    }

    public BsTable getInnerTable() { return table; }
    public BsPagination getPagination() { return pagination; }
}
