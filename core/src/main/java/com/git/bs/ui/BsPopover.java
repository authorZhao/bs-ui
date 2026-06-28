package com.git.bs.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 风格 Popover：点击触发，显示"标题 + 内容"浮层；点外部或 Esc 关闭。
 * 比 Tooltip 更大、内容更丰富（可包含按钮等富内容）。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsPopover popover = new BsPopover(triggerButton, "确认操作", skin)
 *         .placement(BsPopover.Placement.RIGHT)
 *         .content("确定要删除这条记录吗？此操作不可撤销。");
 * popover.addContentActor(deleteButton);  // 加额外 actor
 * popover.attach(stage); // 绑定到 trigger 的 click 事件
 * popover.setOnConfirm(() -> doDelete());
 * }</pre>
 *
 * <p>实现：trigger 的 click 监听切换 popover 显示；显示时加全屏 backdrop 捕获"点外部"
 * 关闭；点 popover 内部不关闭（事件 root 拦截 backdrop）。</p>
 */
@Slf4j
public class BsPopover {

    public enum Placement { TOP, BOTTOM, LEFT, RIGHT }

    // 颜色 token 改为方法形式（V2：颜色存放在 skin Color 桶，需传 skin）
    private static Color bgColor(Skin skin)       { return BsTheme.be(); }       // 弹层背景，纯白
    private static Color headerColor(Skin skin)   { return BsTheme.bhH(); }      // 表头/工具栏背景 0xF8F9FA
    private static Color borderColor(Skin skin)   { return BsTheme.bds(); }      // 强边框 0xDEE2E6
    private static Color textColor(Skin skin)     { return BsTheme.tp(); }       // 深主文字色 0.15
    private static Color headerTextColor(Skin skin){ return BsTheme.tp(); }      // 标题深字色
    private static final float GAP = 8f;

    private final Actor trigger;
    private final String title;
    private Placement placement = Placement.RIGHT;
    private String contentText;
    private Table contentTable;  // 内容区容器（业务方可加额外 actor）
    private Runnable onConfirm;

    private Stage stage;
    private Actor backdrop;
    private Table root;
    private boolean open;
    private boolean attached;

    public BsPopover(Actor trigger, String title, Skin skin) {
        this.trigger = trigger;
        this.title = title;
    }

    public BsPopover placement(Placement p) { this.placement = p; return this; }
    public BsPopover content(String text) { this.contentText = text; return this; }
    public BsPopover onConfirm(Runnable r) { this.onConfirm = r; return this; }

    /** 在内容区追加任意 actor（按钮、文本、图片等）。 */
    public BsPopover addContentActor(Actor a) {
        ensureContentTable();
        contentTable.add(a).padTop(4).row();
        return this;
    }

    private void ensureContentTable() {
        if (contentTable == null) contentTable = new Table();
    }

    /** 绑定到 trigger 的 click 事件（点击切换 popover 显示）。 */
    public void attach(Stage stage) {
        if (attached) return;
        attached = true;
        this.stage = stage;
        trigger.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (open) close();
                else show();
            }
        });
    }

    private void show() {
        if (open) return;
        // backdrop：透明全屏，捕获点击关闭 popover
        backdrop = new Actor();
        backdrop.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        backdrop.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        backdrop.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                close();
                return true;
            }
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) { close(); return true; }
                return false;
            }
        });

        root = buildRoot();
        root.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        // root 拦截事件，不让 backdrop 收到（防点击 popover 内部关闭）
        root.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                return false;
            }
        });
        root.pack();

        Vector2 tp = trigger.localToStageCoordinates(new Vector2(0, 0));
        positionRoot(tp);

        stage.addActor(backdrop);
        stage.addActor(root);
        stage.setKeyboardFocus(backdrop);
        open = true;
    }

    private Table buildRoot() {
        Skin skin = BsUI.getSkin();
        Table table = new Table();
        Drawable bg = skin.newDrawable("bs-window-bg", bgColor(skin));
        table.setBackground(bg);
        table.pad(0);

        // 标题栏（浅灰背景 + 深色字 + 底部边框）
        Label titleLabel = new Label(title, skin);
        titleLabel.setColor(headerTextColor(skin));
        Container<Label> headerWrap = new Container<>(titleLabel);
        headerWrap.background(skin.newDrawable("white", headerColor(skin)));
        headerWrap.pad(8, 12, 8, 12).fillX();
        table.add(headerWrap).growX().row();

        // 标题/内容之间的分隔线
        Container<Label> sep = new Container<>(new Label("", skin));
        sep.background(skin.newDrawable("white", borderColor(skin)));
        sep.height(1f).fillX();
        table.add(sep).growX().row();

        // 内容区：每次 show 新建 contentTable（避免多次开关后 contentLabel 累积叠加）
        Table newContent = new Table();
        if (contentText != null && !contentText.isEmpty()) {
            Label contentLabel = new Label(contentText, skin);
            contentLabel.setWrap(true);
            contentLabel.setColor(textColor(skin));
            newContent.add(contentLabel).growX().row();
        }
        // 业务方通过 addContentActor 添加的 actor 转移到新 contentTable（从老 contentTable 摘出）
        if (contentTable != null) {
            for (Actor a : contentTable.getChildren()) {
                a.remove();
                newContent.add(a).padTop(4).growX().row();
            }
        }
        contentTable = newContent;
        table.add(contentTable).pad(10, 12, 10, 12).growX().row();

        // 操作按钮区（默认"关闭"按钮；如果设了 onConfirm 则加"确认"）
        if (onConfirm != null) {
            Table btnRow = new Table();
            btnRow.right();
            TextButton cancel = new TextButton("取消", skin, "bs-btn-secondary");
            cancel.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { close(); }
            });
            TextButton confirm = new TextButton("确认", skin, "bs-btn-primary");
            confirm.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    try { onConfirm.run(); } catch (Throwable t) { log.warn("onConfirm error", t); }
                    close();
                }
            });
            btnRow.add(cancel).width(70).padRight(6);
            btnRow.add(confirm).width(70);
            table.add(btnRow).pad(4, 12, 10, 12).right().row();
        } else {
            // 没设 onConfirm，加一个"关闭"链接
            BsLink closeLink = new BsLink("关闭", skin);
            closeLink.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { close(); }
            });
            table.add(closeLink).pad(4, 12, 8, 12).right().row();
        }

        return table;
    }

    private void positionRoot(Vector2 tp) {
        float tw = trigger.getWidth();
        float th = trigger.getHeight();
        float myW = root.getWidth();
        float myH = root.getHeight();
        switch (placement) {
            case TOP:
                root.setPosition(tp.x + (tw - myW) / 2f, tp.y + th + GAP);
                break;
            case BOTTOM:
                root.setPosition(tp.x + (tw - myW) / 2f, tp.y - myH - GAP);
                break;
            case LEFT:
                root.setPosition(tp.x - myW - GAP, tp.y + (th - myH) / 2f);
                break;
            case RIGHT:
                root.setPosition(tp.x + tw + GAP, tp.y + (th - myH) / 2f);
                break;
        }
        // 边界纠正
        if (root.getX() < 0) root.setX(0);
        if (root.getY() < 0) root.setY(0);
        if (root.getX() + myW > stage.getWidth()) root.setX(stage.getWidth() - myW);
        if (root.getY() + myH > stage.getHeight()) root.setY(stage.getHeight() - myH);
    }

    public void close() {
        if (!open) return;
        if (backdrop != null) { backdrop.remove(); backdrop = null; }
        if (root != null) { root.remove(); root = null; }
        open = false;
    }

    public boolean isOpen() { return open; }
}
