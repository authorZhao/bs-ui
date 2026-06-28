package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Bootstrap 风格节点选择面板（Node Palette）——
 * 编辑器左侧的节点选择列表，按分类组织 + 搜索过滤，
 * 点击/双击触发回调（业务方可"拖到画布"或"添加到当前节点"）。
 *
 * <p>结构：</p>
 * <pre>
 * ┌──────────────────────┐
 * │ [🔍 搜索...]          │
 * ├──────────────────────┤
 * │ ▾ 流程控制             │  ← 分组（可折叠）
 * │   • Start             │
 * │   • Branch            │
 * │   • Loop              │
 * │ ▾ 对话                 │
 * │   • Say               │
 * │   • Choice            │
 * │ ▸ 事件                 │  ← 折叠状态
 * └──────────────────────┘
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsNodePalette palette = new BsNodePalette(skin);
 * palette.addCategory("流程控制", cat -> cat
 *         .node("Start", "start_icon")
 *         .node("Branch", "branch_icon")
 *         .node("Loop", "loop_icon"));
 * palette.addCategory("对话", cat -> cat
 *         .node("Say")
 *         .node("Choice"));
 * palette.setOnNodeClick((category, name) -> addNodeToCanvas(category, name));
 * stage.addActor(palette);
 * }</pre>
 */
@Slf4j
public class BsNodePalette extends Table {

    private final BsTextField searchField;
    private final Table listArea;
    private final BsScrollPane listScroll;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private BiConsumer<String, String> onNodeClick;   // (category, nodeName)

    public BsNodePalette(Skin skin) {
        left().top();
        defaults().growX().left();
        pad(8);

        // 搜索框
        searchField = new BsTextField("", skin);
        searchField.setMessageText("搜索节点...");
        searchField.setTextFieldListener((f, c) -> refreshList());
        add(searchField).growX().padBottom(8).row();

        // 列表（滚动）
        listArea = new Table();
        listArea.left().top();
        listArea.defaults().growX().left();
        listScroll = new BsScrollPane(listArea, skin);
        listScroll.setFadeScrollBars(false);
        listScroll.setScrollingDisabled(true, false);
        add(listScroll).grow().row();
    }

    /** 添加一个分类。 */
    public BsNodePalette addCategory(String name, java.util.function.Consumer<Category> config) {
        Category cat = new Category(BsUI.getSkin(), name);
        if (config != null) {
            try { config.accept(cat); } catch (Throwable t) { log.warn("category config", t); }
        }
        categories.put(name, cat);
        refreshList();
        return this;
    }

    public BsNodePalette setOnNodeClick(BiConsumer<String, String> cb) { this.onNodeClick = cb; return this; }

    /** 根据搜索关键字重建列表。 */
    private void refreshList() {
        listArea.clearChildren();
        Skin skin = BsUI.getSkin();
        String filter = searchField.getText().trim().toLowerCase();
        for (Map.Entry<String, Category> e : categories.entrySet()) {
            Category cat = e.getValue();
            // 过滤该分类下的节点
            List<NodeEntry> matched = new ArrayList<>();
            for (NodeEntry n : cat.nodes) {
                if (filter.isEmpty() || n.name.toLowerCase().contains(filter)) {
                    matched.add(n);
                }
            }
            // 搜索时：分类下无匹配节点则不显示分类标题（除非 filter 为空）
            if (!filter.isEmpty() && matched.isEmpty()) continue;
            // 分类标题
            listArea.add(cat.makeTitleHeader()).growX().padTop(8).padBottom(2).row();
            // 节点行
            for (NodeEntry n : matched) {
                listArea.add(makeNodeRow(e.getKey(), n)).growX().pad(2, 12, 2, 4).row();
            }
        }
        if (filter.isEmpty() && categories.isEmpty()) {
            Label empty = new Label("(无分类，请用 addCategory 添加)", skin);
            empty.setColor(Color.GRAY);
            listArea.add(empty).padTop(20).row();
        } else if (!filter.isEmpty()) {
            boolean any = categories.values().stream().anyMatch(c -> c.nodes.stream().anyMatch(n -> n.name.toLowerCase().contains(filter)));
            if (!any) {
                Label empty = new Label("(无匹配)", skin);
                empty.setColor(Color.GRAY);
                listArea.add(empty).padTop(20).row();
            }
        }
    }

    private Table makeNodeRow(String category, NodeEntry entry) {
        Table row = new Table();
        row.left();
        row.defaults().pad(2).left();
        Skin skin = BsUI.getSkin();
        // 节点图标
        if (entry.icon != null) {
            com.badlogic.gdx.scenes.scene2d.ui.Image img = new com.badlogic.gdx.scenes.scene2d.ui.Image(entry.icon);
            img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            row.add(img).size(16).padRight(8).left();
        } else {
            row.add(new Label("•", skin)).padRight(8).left();
        }
        // 节点名
        Label l = new Label(entry.name, skin);
        l.setColor(BsTheme.tp());
        l.setFontScale(0.95f);
        row.add(l).growX().left();
        row.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        row.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (onNodeClick != null) {
                    try { onNodeClick.accept(category, entry.name); } catch (Throwable t) { log.warn("onNodeClick", t); }
                }
            }
            @Override public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                row.setBackground(skin.getDrawable("bs-menu-item-hover"));
            }
            @Override public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                row.setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);
            }
        });
        return row;
    }

    // ========================= Category =========================

    public static class Category {
        private final Skin skin;
        final String name;
        final List<NodeEntry> nodes = new ArrayList<>();
        boolean collapsed = false;

        Category(Skin skin, String name) {
            this.skin = skin;
            this.name = name;
        }

        public Category node(String name) {
            nodes.add(new NodeEntry(name, null));
            return this;
        }

        public Category node(String name, Drawable icon) {
            nodes.add(new NodeEntry(name, icon));
            return this;
        }

        Table makeTitleHeader() {
            Table header = new Table();
            header.left();
            header.defaults().pad(0).left();
            Label arrow = new Label(collapsed ? "▸" : "▾", skin);
            arrow.setColor(BsPalette.SECONDARY.getMain());
            Label title = new Label(name, skin);
            title.setColor(BsTheme.ts());
            title.setFontScale(0.95f);
            header.add(arrow).padRight(6).left();
            header.add(title).left();
            header.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            header.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    collapsed = !collapsed;
                }
            });
            return header;
        }
    }

    public static class NodeEntry {
        final String name;
        final Drawable icon;
        NodeEntry(String name, Drawable icon) { this.name = name; this.icon = icon; }
    }
}
