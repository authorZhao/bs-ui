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
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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
 * 每行 = 缩进（depth × indentPx）+ 展开/折叠箭头（&gt;/v）/ 叶子文档图标 + 文字。
 * 箭头和文字都是 {@link TextButton}（事件路径清晰）；展开/折叠走箭头，
 * 点非叶子文字同样展开/折叠（并触发 onNodeClick），点叶子文字仅 onNodeClick。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsTree extends Table {

    private static final float INDENT_PER_DEPTH = 32f;
    private static final float ROW_PAD = 3f;
    private static final float ROW_HEIGHT = 26f;

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
            // 箭头：>（折叠）/ v（展开）；叶子用生成的文档图标占位（否则左侧空一截突兀）。
            // 不用 ▸/▾（U+25B8/U+25BE）：预烘焙 .fnt 字形表缺这两个码点，渲染为空白
            if (n.isLeaf()) {
                Image leaf = new Image(leafDrawable());
                leaf.setColor(BsTheme.tm());  // 白色形状 + tint = 跟随主题的 muted 灰
                row.add(leaf).size(13f).padRight(2 + (20f - 13f) / 2f);
            } else {
                TextButtonStyle arrowStyle = new TextButtonStyle(skin.get("bs-menu-title", TextButtonStyle.class));
                arrowStyle.fontColor = BsTheme.tp();
                TextButton arrowBtn = new TextButton(n.isExpanded() ? "v" : ">", arrowStyle);
                arrowBtn.setProgrammaticChangeEvents(false);
                arrowBtn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        toggle(n);
                    }
                });
                row.add(arrowBtn).size(20, ROW_HEIGHT).padRight(2);
            }
            // 文字：节点统一 textPrimary 单色（IDEA 目录树风格，层级感只靠缩进+竖线），
            // TextButton 渲染用 TextButtonStyle.fontColor（Label.setColor 会被覆盖无效）
            TextButtonStyle nodeStyle = new TextButtonStyle(skin.get("bs-menu-title", TextButtonStyle.class));
            nodeStyle.fontColor = BsTheme.tp();
            // hover 时仍用蓝色 overFontColor（保持导航感）—— 走 linkColor token
            nodeStyle.overFontColor = BsPalette.PRIMARY.getHover();
            nodeStyle.downFontColor = BsPalette.PRIMARY.getHover();
            TextButton textBtn = new TextButton(n.getText(), nodeStyle);
            textBtn.setProgrammaticChangeEvents(false);
            // 文字锚左：TextButton 的 Label 默认居中，而行宽随树内最宽行变化（展开出
            // 更深/更长的节点时整棵树变宽），居中会让所有已见节点跟着左右漂移；
            // 锚左后文字 x 只取决于自身缩进深度，展开/折叠不再牵动前面的父节点
            textBtn.getLabel().setAlignment(Align.left);
            textBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    // 叶子=选中回调；非叶子=点击文字同样展开/折叠（并触发选中回调，
                    // 业务方按 isLeaf/expanded 自行取用）
                    if (!n.isLeaf()) {
                        toggle(n);
                    }
                    if (onNodeClick != null) {
                        try { onNodeClick.accept(n); } catch (Throwable t) { /* ignore */ }
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

    /** 展开/折叠切换（箭头与文字点击共用）：更新状态 + 回调 + 重渲染。 */
    private void toggle(Node n) {
        n.setExpanded(!n.isExpanded());
        if (onNodeExpandToggle != null) {
            try { onNodeExpandToggle.accept(n); } catch (Throwable t) { /* ignore */ }
        }
        refresh();
    }

    // ==================== 叶子文档图标（代码生成，无外部资源依赖） ====================

    /** 白色文档形纹理（懒加载静态复用，16×16 仅 1KB，不释放）；
     *  Image.setColor 染主题色，主题切换即时生效。 */
    private static volatile Texture leafTex;

    private static Drawable leafDrawable() {
        Texture tex = leafTex;
        if (tex == null) {
            synchronized (BsTree.class) {
                if (leafTex == null) {
                    Pixmap pm = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
                    pm.setColor(Color.WHITE);
                    // 轮廓：左边/底边/顶边(左段) + 右上折角
                    pm.drawLine(4, 1, 9, 1);
                    pm.drawLine(9, 1, 12, 4);
                    pm.drawLine(12, 4, 12, 14);
                    pm.drawLine(4, 14, 12, 14);
                    pm.drawLine(4, 1, 4, 14);
                    // 内容横线（文本感）
                    pm.drawLine(6, 6, 10, 6);
                    pm.drawLine(6, 9, 10, 9);
                    pm.drawLine(6, 12, 8, 12);
                    leafTex = new Texture(pm);
                    pm.dispose();
                }
                tex = leafTex;
            }
        }
        return new TextureRegionDrawable(new TextureRegion(tex));
    }
}
