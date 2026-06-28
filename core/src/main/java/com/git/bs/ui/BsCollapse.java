package com.git.bs.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 5 风格折叠面板（Collapse）。
 *
 * <p>可展开/收起的容器：标题行（点击切换）+ 内容（展开时显示，收起时隐藏）。
 * 也是 {@link BsAccordion}（手风琴）的基础单元。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsCollapse collapse = new BsCollapse(skin);
 * collapse.setTitle("用户信息");
 * collapse.setContent(new Label("这里放任意 actor"));
 * collapse.setExpanded(true);          // 默认展开
 * collapse.setOnToggle(e -> ...);       // 切换回调
 * stage.addActor(collapse);
 * }</pre>
 *
 * <p>实现：标题行（Button 风格）+ 内容容器。展开/收起用 Actions 控制内容容器
 * 的高度 + alpha，做平滑过渡（不是瞬时显隐）。</p>
 */
@Slf4j
public class BsCollapse extends Table {

    /** 切换事件。 */
    public interface ToggleListener {
        void onToggle(BsCollapse source, boolean expanded);
    }

    private final Table headerRow;
    private final Container<Actor> contentWrap;
    private Actor content;       // 业务方提供的内容
    private boolean expanded = false;
    private ToggleListener listener;
    private float animDuration = 0.18f;
    private float cachedContentHeight = 0f;

    public BsCollapse(Skin skin) {
        setBackground(skin.getDrawable("bs-window-bg"));
        top().left();
        defaults().growX();

        headerRow = new Table();
        headerRow.left();
        headerRow.pad(8, 12, 8, 12);
        headerRow.setBackground(skin.getDrawable("bs-menu-title-up"));
        headerRow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setExpanded(!expanded);
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                headerRow.setBackground(BsUI.getSkin().getDrawable("bs-menu-title-hover"));
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                headerRow.setBackground(BsUI.getSkin().getDrawable("bs-menu-title-up"));
            }
        });
        add(headerRow).growX().row();

        contentWrap = new Container<>();
        contentWrap.fill();
        contentWrap.top().left();
        add(contentWrap).growX();

        // 初始收起
        applyCollapsedVisual();
    }

    public BsCollapse setTitle(String title) {
        headerRow.clearChildren();
        Label arrow = new Label(expanded ? "▾" : "▸", BsUI.getSkin());
        arrow.setColor(BsTheme.tm());
        headerRow.add(arrow).padRight(8).left();
        Label label = new Label(title == null ? "" : title, BsUI.getSkin());
        label.setColor(BsTheme.tp());
        headerRow.add(label).growX().left();
        return this;
    }

    public BsCollapse setContent(Actor content) {
        this.content = content;
        contentWrap.setActor(content);
        // 测量内容自然高度（Actor 没有 pack，用 Widget/Layout 的 prefHeight 兜底）
        if (content != null) {
            if (content instanceof com.badlogic.gdx.scenes.scene2d.ui.Widget) {
                ((com.badlogic.gdx.scenes.scene2d.ui.Widget) content).pack();
            }
            cachedContentHeight = content.getHeight() > 0
                    ? content.getHeight()
                    : content instanceof com.badlogic.gdx.scenes.scene2d.utils.Layout
                            ? ((com.badlogic.gdx.scenes.scene2d.utils.Layout) content).getPrefHeight()
                            : 80f;
        }
        applyCollapsedVisual();
        return this;
    }

    public Actor getContent() { return content; }

    public boolean isExpanded() { return expanded; }

    /** 展开/收起（带动画）。 */
    public BsCollapse setExpanded(boolean expanded) {
        if (this.expanded == expanded) return this;
        this.expanded = expanded;
        if (expanded) {
            applyExpandedVisual();
        } else {
            applyCollapsedVisual();
        }
        // 重建标题（更新箭头方向）
        refreshHeaderArrow();
        if (listener != null) {
            try { listener.onToggle(this, expanded); } catch (Throwable t) { log.warn("onToggle", t); }
        }
        return this;
    }

    /** 立即同步状态（不动画），用于初始化/批量切换。 */
    public BsCollapse setExpandedImmediate(boolean expanded) {
        float oldAnim = this.animDuration;
        this.animDuration = 0f;
        setExpanded(expanded);
        this.animDuration = oldAnim;
        return this;
    }

    public BsCollapse setOnToggle(ToggleListener l) { this.listener = l; return this; }

    private void applyExpandedVisual() {
        if (content == null) return;
        content.setVisible(true);
        contentWrap.setVisible(true);
        contentWrap.height(cachedContentHeight);
        if (animDuration > 0) {
            contentWrap.addAction(Actions.sequence(
                    Actions.alpha(0),
                    Actions.parallel(
                            Actions.fadeIn(animDuration, Interpolation.fade),
                            Actions.sizeTo(contentWrap.getWidth(), cachedContentHeight, animDuration)
                    )
            ));
        } else {
            contentWrap.setHeight(cachedContentHeight);
            contentWrap.setColor(1, 1, 1, 1);
        }
    }

    private void applyCollapsedVisual() {
        if (content == null) {
            contentWrap.setVisible(false);
            contentWrap.height(0);
            return;
        }
        if (animDuration > 0 && expanded) {
            // 从展开态收起：动画到 0 后再隐藏
            contentWrap.addAction(Actions.sequence(
                    Actions.parallel(
                            Actions.fadeOut(animDuration, Interpolation.fade),
                            Actions.sizeTo(contentWrap.getWidth(), 0, animDuration)
                    ),
                    Actions.run(() -> {
                        contentWrap.setVisible(false);
                        content.setVisible(false);
                    })
            ));
        } else {
            contentWrap.setHeight(0);
            contentWrap.setVisible(false);
            content.setVisible(false);
            contentWrap.setColor(1, 1, 1, 0);
        }
    }

    private void refreshHeaderArrow() {
        // 简化：把 header 全清重建（性能足够）
        if (headerRow.getChildren().size >= 2) {
            Actor arrow = headerRow.getChildren().first();
            if (arrow instanceof Label) {
                ((Label) arrow).setText(expanded ? "▾" : "▸");
            }
        }
    }

    @Override
    public void layout() {
        super.layout();
        // 内容宽度跟随容器
        if (content != null) {
            content.setWidth(getWidth() - 4);
        }
    }
}
