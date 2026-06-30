package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/// Anchor / TOC 锚点导航：一组标题链接，点击滚动到目标位置，滚动时自动高亮"当前所在区域"。
///
/// 用法：
/// ```java
/// BsScrollPane scroll = ...;   // 放长文档的滚动容器
/// BsAnchor anchor = new BsAnchor(skin, scroll)
///         .setOnAnchorChange(i -> setStatus("当前节: " + i))
///         .add("概述",   heading0)
///         .add("安装",   heading1)
///         .add("用法",   heading2);
/// // headingN 是文档里对应标题的 actor，须按文档顺序添加
/// ```
///
/// 实现：`Table`（竖排链接，`bs-link` style）。高亮判定用 **stage 坐标**比较各标题顶边
/// 与滚动视口顶边——不依赖 `ScrollPane.getScrollY()` 的方向语义，避免踩坑。
/// 点击调 `scroll.scrollTo(...)` 滚到目标（内容坐标由沿父级累加得到）。
///
/// v1 不含：平滑滚动动画、点击把标题精确贴顶（scrollTo 只保证"可见"）、二级缩进。
@Slf4j
public class BsAnchor extends Table {

    /** 单个锚点：标题文本 + 目标 actor。 */
    public static final class Item {
        public final String title;
        public final Actor target;
        public Item(String title, Actor target) {
            this.title = title;
            this.target = target;
        }
    }

    private final ScrollPane scroll;
    private final List<Item> items = new ArrayList<>();
    private final List<TextButton> links = new ArrayList<>();

    private int active = -1;
    private Consumer<Integer> onAnchorChange;
    private float topMargin = 0f;
    private final Vector2 tmp = new Vector2();

    public BsAnchor(Skin skin, ScrollPane scroll) {
        this.scroll = scroll;
        defaults().left().pad(2);
        left().top();
    }

    /** 添加一个锚点（须按文档从上到下顺序添加）。 */
    public BsAnchor add(String title, Actor target) {
        items.add(new Item(title, target));
        final int idx = items.size() - 1;
        TextButton link = new TextButton(title == null ? "" : title, BsUI.getSkin(), "bs-link");
        link.getLabel().setColor(BsTheme.ts());
        link.left();
        link.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { scrollTo(idx); }
        });
        links.add(link);
        add(link).growX().row();
        updateHighlight();
        return this;
    }

    /// 高亮判定的上偏移（>0 时，标题进入视口顶部下方该距离才算激活，缓解顶部抖动）。
    public BsAnchor setTopMargin(float margin) {
        this.topMargin = margin;
        return this;
    }

    public BsAnchor setOnAnchorChange(Consumer<Integer> c) {
        this.onAnchorChange = c;
        return this;
    }

    public int getActive() { return active; }

    // =================== 内部 ===================

    private void scrollTo(int idx) {
        if (idx < 0 || idx >= items.size()) return;
        Actor t = items.get(idx).target;
        if (scroll == null || t == null || t.getStage() == null) return;
        float[] b = contentBounds(scroll, t);
        scroll.scrollTo(b[0], b[1], b[2], b[3], false, false);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (scroll == null || scroll.getStage() == null || items.isEmpty()) return;
        int newActive = computeActive();
        if (newActive != active) {
            active = newActive;
            updateHighlight();
            if (onAnchorChange != null) {
                try { onAnchorChange.accept(active); } catch (Throwable t) { log.warn("BsAnchor onAnchorChange error", t); }
            }
        }
    }

    /// 当前激活项 = 第一个顶边已滚到视口顶部（含 margin）及以下的标题；若全部还在上方，则是末项。
    private int computeActive() {
        float vpTop = scroll.localToStageCoordinates(tmp.set(0, scroll.getHeight())).y - topMargin;
        for (int i = 0; i < items.size(); i++) {
            Actor t = items.get(i).target;
            if (t == null || t.getStage() == null) continue;
            float tTop = t.localToStageCoordinates(tmp.set(0, t.getHeight())).y;
            if (tTop <= vpTop) {
                return i;
            }
        }
        return items.size() - 1;
    }

    private void updateHighlight() {
        Skin skin = BsUI.getSkin();
        Color primary = skin.get("bs-primary", Color.class);
        Color secondary = skin.get("bs-text-secondary", Color.class);
        for (int i = 0; i < links.size(); i++) {
            links.get(i).getLabel().setColor(i == active ? primary : secondary);
        }
    }

    /** 计算 target 在 ScrollPane 内容坐标系下的 {x, y, w, h}（沿父级累加到内容根为止）。 */
    private static float[] contentBounds(ScrollPane scroll, Actor target) {
        float x = 0, y = 0;
        Actor a = target;
        while (a.getParent() != null && a.getParent() != scroll) {
            x += a.getX();
            y += a.getY();
            a = a.getParent();
        }
        return new float[] { x, y, target.getWidth(), target.getHeight() };
    }
}
