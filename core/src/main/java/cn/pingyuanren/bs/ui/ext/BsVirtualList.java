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
package cn.pingyuanren.bs.ui.ext;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/// 虚拟化长列表：仅实例化**可见区域（+overscan）**的单元格，滚动时回收复用，可承载数万条数据。
///
/// 用法：
/// ```java
/// BsVirtualList<String> list = new BsVirtualList<>(skin, (existing, item, idx) -> {
///     Label l = existing instanceof Label ? (Label) existing : new Label("", skin);
///     l.setText(item);
///     return l;
/// }, 28f);
/// list.setItems(hugeList);
/// list.setOnClick((idx, item) -> setStatus("点 " + idx));
/// ```
///
/// 实现：本类 `extends ScrollPane`，内容是一个 `WidgetGroup` 占位（靠 `getPrefHeight` 返回
/// `条数×行高` 让滚动条正确）。每帧用 **stage 坐标**比较占位顶边与视口顶/底边，
/// 算出可见索引区间（不依赖 `getScrollY` 方向语义）；越界 cell 回收进池、新可见索引从池取并重绑数据。
/// 行 cell 按 `index` 固定在占位内的 y（`totalH - (index+1)*rowH`），滚动由 ScrollPane 整体平移。
///
/// 注意：`CellRenderer` 在 `existing` 非 null 时应**复用**（更新内容）以省分配。
/// @author authorZhao
/// @since 2026-07-16
@Slf4j
public class BsVirtualList<T> extends ScrollPane {

    /// 单元格渲染器：`existing` 非 null 时复用（更新内容），为 null 时新建。返回用于显示的 actor。
    @FunctionalInterface
    public interface CellRenderer<T> {
        Actor render(Actor existing, T item, int index);
    }

    /// 内容占位：靠 `getPrefHeight`/`getMinHeight` 报告总高度，让 ScrollPane 滚动范围正确。
    private static final class Spacer extends WidgetGroup {
        float totalH = 0f;
        @Override public float getPrefWidth() { return 0f; }
        @Override public float getPrefHeight() { return totalH; }
        @Override public float getMinHeight() { return totalH; }
        @Override public void layout() {}
    }

    private final CellRenderer<T> renderer;
    private final float itemH;
    private final Spacer spacer;

    private List<T> items = Collections.emptyList();
    private final Map<Integer, Actor> active = new HashMap<>();   // index → 正在显示的 cell
    private final List<Actor> pool = new ArrayList<>();           // 回收的空闲 cell
    private BiConsumer<Integer, T> onClick;
    private float overscanRows = 1f;
    private float lastSpacerStageY = Float.NaN;                   // 滚动检测缓存
    private final Vector2 tmpA = new Vector2();
    private final Vector2 tmpB = new Vector2();
    /** 回收越界 cell 的临时列表（复用，避免滚动时每帧 new ArrayList）。 */
    private final java.util.List<Integer> dropBuf = new java.util.ArrayList<>();

    private final ClickListener cellClick = new ClickListener() {
        @Override public void clicked(InputEvent event, float x, float y) {
            if (onClick == null) return;
            Object o = event.getListenerActor().getUserObject();
            if (o instanceof Integer) {
                int i = (Integer) o;
                if (i >= 0 && i < items.size()) {
                    try { onClick.accept(i, items.get(i)); } catch (Throwable t) { log.warn("BsVirtualList onClick error", t); }
                }
            }
        }
    };

    public BsVirtualList(Skin skin, CellRenderer<T> renderer, float itemHeight) {
        super(new Spacer(), skin);
        this.spacer = (Spacer) getWidget();
        this.renderer = renderer;
        this.itemH = Math.max(1f, itemHeight);
        setScrollingDisabled(true, false);   // 仅纵向
        setFadeScrollBars(false);
    }

    public BsVirtualList<T> setItems(List<T> items) {
        this.items = items == null ? Collections.emptyList() : items;
        recycleAll();
        spacer.totalH = this.items.size() * itemH;
        invalidate();   // 重新计算滚动范围
        return this;
    }

    public List<T> getItems() { return items; }

    public BsVirtualList<T> setOnClick(BiConsumer<Integer, T> cb) { this.onClick = cb; return this; }

    /// 额外预渲染行数（上下各 `rows`），缓解快速滚动闪白。
    public BsVirtualList<T> setOverscanRows(float rows) { this.overscanRows = rows; return this; }

    @Override
    public void act(float delta) {
        super.act(delta);
        updateVisible();
    }

    @Override
    public void layout() {
        super.layout();
        lastSpacerStageY = Float.NaN;   // 尺寸变化，强制重算
        updateVisible();
    }

    private void updateVisible() {
        if (getStage() == null) return;
        if (items.isEmpty()) {
            recycleAll();
            return;
        }
        float totalH = spacer.totalH;
        float viewH = getHeight();
        float viewW = getWidth();
        if (viewH <= 0) return;

        float spacerStageY = spacer.localToStageCoordinates(tmpA.set(0, 0)).y;
        if (spacerStageY == lastSpacerStageY && !active.isEmpty()) {
            return;   // 未滚动，跳过重算
        }
        lastSpacerStageY = spacerStageY;

        // 可见窗口在占位局部坐标系下的 [visBotC, visTopC]
        float scrollTopStage = localToStageCoordinates(tmpB.set(0, viewH)).y;
        float scrollBotStage = localToStageCoordinates(tmpB.set(0, 0)).y;
        float visTopC = scrollTopStage - spacerStageY;
        float visBotC = scrollBotStage - spacerStageY;

        int n = items.size();
        int first = (int) Math.floor((totalH - visTopC) / itemH) - (int) Math.floor(overscanRows);
        int last = (int) Math.ceil((totalH - visBotC) / itemH) + (int) Math.ceil(overscanRows);
        first = Math.max(0, Math.min(first, n - 1));
        last = Math.max(0, Math.min(last, n - 1));

        // 回收越界（dropBuf 复用，clear 后填充，避免每帧 new ArrayList）
        dropBuf.clear();
        for (Integer idx : active.keySet()) {
            if (idx < first || idx > last) dropBuf.add(idx);
        }
        for (Integer idx : dropBuf) {
            Actor c = active.remove(idx);
            if (c != null) { c.setVisible(false); pool.add(c); }
        }

        // 绑定 [first, last]
        for (int i = first; i <= last; i++) {
            Actor cell = active.get(i);
            if (cell == null) {
                Actor reused = pool.isEmpty() ? null : pool.remove(pool.size() - 1);
                cell = renderer.render(reused, items.get(i), i);
                if (cell == null) continue;
                if (cell.getParent() != spacer) spacer.addActor(cell);
                if (!cell.getListeners().contains(cellClick, true)) cell.addListener(cellClick);
                cell.setUserObject(i);
                cell.setVisible(true);
                active.put(i, cell);
            }
            cell.setBounds(0, totalH - (i + 1) * itemH, viewW, itemH);
        }
    }

    private void recycleAll() {
        for (Actor c : active.values()) {
            c.setVisible(false);
            pool.add(c);
        }
        active.clear();
        lastSpacerStageY = Float.NaN;
    }
}
