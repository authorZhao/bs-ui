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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bootstrap 风格树状列表：节点可展开/折叠，缩进表示层级。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsTree tree = new BsTree(skin);
 * BsTree.Node root = tree.root("项目");
 * BsTree.Node src = root.addChild("src");
 * src.addChild("Main.java");
 * src.addChild("Utils.java");
 * root.addChild("README.md");
 * tree.refresh();
 * tree.setOnNodeClick(n -> setStatus("点击: " + n.getText()));
 * }</pre>
 *
 * <p>实现：纵向 {@link Table}，递归遍历可见节点（父折叠则子不渲染），
 * 每行 = 缩进（depth × indentPx）+ 展开/折叠箭头（▸/▾）+ 文字。
 * 箭头和文字都是 {@link TextButton}（事件路径清晰）。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsTree extends Table {

    private static final float INDENT_PER_DEPTH = 32f;
    private static final float ROW_PAD = 3f;
    private static final float ROW_HEIGHT = 26f;

    /** 各层级字色：depth=0 一级节点用 textPrimary（最醒目），层级越深越浅。
     *  V2：颜色从 skin 取，必须传 skin。 */
    private static Color[] depthColors(Skin skin) {
        return new Color[] {
                BsTheme.tp(),  // depth 0 (一级) textPrimary
                BsTheme.ts(),  // depth 1 (二级) textSecondary
                BsTheme.tm(),  // depth 2 (三级) textMuted
                BsTheme.td(),  // depth 3+ 浅灰 textDisabled
        };
    }

    /** 树节点数据模型。 */
    public static class Node {
        @Getter @Setter private String text;
        @Getter private final List<Node> children = new ArrayList<>();
        @Getter @Setter private Node parent;
        @Getter @Setter private boolean expanded;
        @Getter @Setter private Object userData; // 业务方挂任意数据

        public Node(String text) { this.text = text; }

        public Node addChild(Node child) {
            child.parent = this;
            children.add(child);
            return child;
        }

        public Node addChild(String text) {
            return addChild(new Node(text));
        }

        public boolean isLeaf() { return children.isEmpty(); }

        /** 递归收集所有可见节点（父折叠则子不进入）。 */
        public void collectVisible(List<Node> out, int depth) {
            out.add(this);
            if (expanded && !children.isEmpty()) {
                for (Node c : children) c.collectVisible(out, depth + 1);
            }
        }
    }

    private final Node root;
    private Consumer<Node> onNodeClick;
    private Consumer<Node> onNodeExpandToggle;

    public BsTree(Skin skin) {
        this(skin, "Root");
    }

    public BsTree(Skin skin, String rootText) {
        this.root = new Node(rootText);
        this.root.setExpanded(true);
        left().top();
        defaults().pad(ROW_PAD).left();
    }

    /** 获取根节点（用于构建树形数据）。构建完后调用 {@link #refresh()} 渲染。 */
    public Node root(String rootText) {
        root.setText(rootText);
        return root;
    }

    public Node getRoot() { return root; }

    public void setOnNodeClick(Consumer<Node> cb) { this.onNodeClick = cb; }
    public void setOnNodeExpandToggle(Consumer<Node> cb) { this.onNodeExpandToggle = cb; }

    /** 重新渲染整棵树（数据变更后必须调用）。 */
    public void refresh() {
        clearChildren();
        Skin skin = BsUI.getSkin();
        List<Node> visible = new ArrayList<>();
        // root 自己不显示（业务方一般用 root 装一级节点），渲染 root 的子节点
        for (Node c : root.getChildren()) c.collectVisible(visible, 0);

        for (Node n : visible) {
            int depth = depthOf(n);
            Table row = new Table();
            // 缩进 + 竖线连接器：每个深度位置一条 1px 淡灰竖线
            for (int i = 0; i < depth; i++) {
                // 用 1px 宽的灰色 drawable 作为竖线（在 INDENT_PER_DEPTH 宽度的 cell 内居中）
                com.badlogic.gdx.scenes.scene2d.utils.Drawable vline =
                        BsSkinFactory.drawableOf(BsTheme.bd());
                com.badlogic.gdx.scenes.scene2d.ui.Container<Actor> vlineWrap =
                        new com.badlogic.gdx.scenes.scene2d.ui.Container<>();
                com.badlogic.gdx.scenes.scene2d.ui.Image lineImg = new com.badlogic.gdx.scenes.scene2d.ui.Image(vline);
                vlineWrap.setActor(lineImg);
                vlineWrap.width(1f).height(ROW_HEIGHT);
                // 竖线放在缩进 cell 的左侧（实际 cell 宽度=INDENT_PER_DEPTH，竖线占 1px 居左偏中）
                row.add(vlineWrap).width(INDENT_PER_DEPTH).padLeft((INDENT_PER_DEPTH - 1f) / 2f);
            }
            // 箭头：▸（折叠）/ ▾（展开）/ 空（叶子）
            String arrow = n.isLeaf() ? "  " : (n.isExpanded() ? "▾" : "▸");
            // 按深度调箭头字色（独立 style，避免 setColor 无效）
            Color arrowColor = n.isLeaf()
                    ? BsTheme.td()
                    : depthColors(skin)[Math.min(depth, depthColors(skin).length - 1)];
            TextButtonStyle arrowStyle = new TextButtonStyle(skin.get("bs-menu-title", TextButtonStyle.class));
            arrowStyle.fontColor = arrowColor;
            TextButton arrowBtn = new TextButton(arrow, arrowStyle);
            arrowBtn.setProgrammaticChangeEvents(false);
            if (!n.isLeaf()) {
                arrowBtn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        n.setExpanded(!n.isExpanded());
                        if (onNodeExpandToggle != null) {
                            try { onNodeExpandToggle.accept(n); } catch (Throwable t) { /* ignore */ }
                        }
                        refresh();
                    }
                });
            }
            row.add(arrowBtn).size(20, ROW_HEIGHT).padRight(2);
            // 文字：每个节点独立 style（按深度设 fontColor），
            // 因为 TextButton 渲染用 TextButtonStyle.fontColor，Label.setColor 会被覆盖无效
            Color textColor = depthColors(skin)[Math.min(depth, depthColors(skin).length - 1)];
            TextButtonStyle nodeStyle = new TextButtonStyle(skin.get("bs-menu-title", TextButtonStyle.class));
            nodeStyle.fontColor = textColor;
            // hover 时仍用蓝色 overFontColor（保持导航感）—— 走 linkColor token
            nodeStyle.overFontColor = BsPalette.PRIMARY.getHover();
            nodeStyle.downFontColor = BsPalette.PRIMARY.getHover();
            TextButton textBtn = new TextButton(n.getText(), nodeStyle);
            textBtn.setProgrammaticChangeEvents(false);
            textBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (onNodeClick != null) {
                        try { onNodeClick.accept(n); } catch (Throwable t) { /* ignore */ }
                    }
                    // 点击文字也展开/折叠（如果有子节点）
                    if (!n.isLeaf()) {
                        n.setExpanded(!n.isExpanded());
                        refresh();
                    }
                }
            });
            row.add(textBtn).growX().height(ROW_HEIGHT);
            add(row).growX().row();
        }
    }

    /** 计算节点深度（root 的子节点深度=0）。 */
    private int depthOf(Node n) {
        int d = 0;
        Node p = n.parent;
        while (p != null && p != root) {
            d++;
            p = p.parent;
        }
        return d;
    }
}
