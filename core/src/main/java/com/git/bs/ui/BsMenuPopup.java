package com.git.bs.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单弹出浮层：点击 {@link BsMenuBar} 的菜单按钮时显示，列出该菜单下的所有 item
 * 供用户选择；点 item 触发回调并关闭，点浮层外或按 Esc 也关闭。
 *
 * <p>实现要点：</p>
 * <ul>
 *   <li>用一个透明全屏 backdrop 捕获"点外部"事件以关闭浮层（拦截事件防止穿透到下层 UI）。</li>
 *   <li>浮层内每个 item 是一个独立的 {@link TextButton}（带 hover 视觉），
 *       避免使用 scene2d List+ScrollPane 在 backdrop 场景下事件时序不稳的问题。</li>
 *   <li>定位：根据触发按钮的屏幕坐标显示在按钮下方；下方空间不够则翻到上方。</li>
 * </ul>
 */
@Slf4j
public class BsMenuPopup {

    private Table root;
    private Actor backdrop;
    private boolean open;
    private Runnable onClose;

    public BsMenuPopup(Skin skin) {
    }

    /** 设置 popup 关闭时的回调（任何路径关闭都会触发：点 item、点外部、按 Esc、close()）。 */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * 显示浮层，浮层左上角紧贴 anchor 按钮的左下角；下方空间不够则翻到按钮上方。
     *
     * @param labels  可点击项的文本（按显示顺序）
     * @param actions 对应位置的回调（点 item 时执行并关闭浮层）
     */
    public void show(Stage stage, Actor anchor, List<String> labels, List<Runnable> actions) {
        Skin skin = BsUI.getSkin();
        if (open) close();

        Vector2 btnPos = anchor.localToStageCoordinates(new Vector2(0, 0));
        float btnBottom = btnPos.y;
        float btnTop = btnPos.y + anchor.getHeight();

        // backdrop：透明全屏 actor，捕获点击 → 关闭浮层；拦截 Esc
        // 注意：必须先 add，root 后 add → root 在 backdrop 上层，事件能正确路由到 root 内的 item 按钮。
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

        root = new Table();
        root.setBackground(skin.getDrawable("bs-window-bg"));
        root.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled); // 让 root 自己接收点击但不吞（透到 backdrop 之外）

        // 每个 item 用 TextButton（独立 actor，事件路径清晰，避开 scene2d List 在
        // ScrollPane+backdrop 场景下 ChangeListener 时序不稳的问题）
        float fontH = skin.getFont("default").getLineHeight();
        float itemMinW = 120f;
        for (int i = 0; i < labels.size(); i++) {
            final int idx = i;
            final Runnable action = actions.get(i);
            // 用 bs-menu-item 扁平 style（透明背景 + hover/active 高亮）
            TextButton itemBtn = new TextButton(labels.get(i), skin, "bs-menu-item");
            itemBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    log.info("BsMenuPopup item clicked, idx={}", idx);
                    try {
                        if (action != null) action.run();
                    } catch (Throwable t) {
                        log.warn("BsMenuPopup item action error", t);
                    }
                    close();
                }
            });
            root.add(itemBtn).left().growX().pad(2).minWidth(itemMinW).height(fontH + 10).row();
        }

        root.pack();

        // 限制 root 最大高度（item 过多时靠 stage 高度封顶，超出本版暂不支持滚动）
        float maxH = stage.getHeight() * 0.7f;
        if (root.getHeight() > maxH) {
            root.setHeight(maxH);
        }

        // 定位：默认放按钮下方；下方不够则翻到按钮上方
        float x = btnPos.x;
        float y = btnBottom - root.getHeight();
        if (y < 0) {
            y = btnTop;
        }
        if (x + root.getWidth() > stage.getWidth()) {
            x = stage.getWidth() - root.getWidth();
        }
        if (x < 0) x = 0;
        root.setPosition(x, y);

        stage.addActor(backdrop);
        stage.addActor(root);
        stage.setKeyboardFocus(backdrop);
        open = true;
    }

    /**
     * 按屏幕坐标显示浮层（不依赖 anchor actor，用于右键菜单等"凭空弹出"场景）。
     * <p><b>兼容性</b>：保留 {@link EditorPopupMenu} 的 postRunnable 延迟一帧行为
     * （避免在 touchDown 事件中立即 addActor 被同一事件移除）。</p>
     *
     * @param stage 目标 stage
     * @param x 屏幕坐标 X（左下原点）
     * @param y 屏幕坐标 Y
     * @param labels 显示项文本
     * @param actions 对应位置回调
     */
    public void showAt(Stage stage, float x, float y, List<String> labels, List<Runnable> actions) {
        if (open) close();
        // postRunnable 延迟一帧（坑 8：showMenu 在 touchDown 中调用导致同一事件移除）
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (open) close();  // 防重入
            buildAndShowAt(stage, x, y, labels, actions);
        });
    }

    private void buildAndShowAt(Stage stage, float x, float y, List<String> labels, List<Runnable> actions) {
        Skin skin = BsUI.getSkin();
        // backdrop
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

        // 每个 item 用 TextButton（独立 actor，事件路径清晰）
        float fontH = skin.getFont("default").getLineHeight();
        for (int i = 0; i < labels.size(); i++) {
            final int idx = i;
            final Runnable action = actions.get(i);
            TextButton itemBtn = new TextButton(labels.get(i), skin, "bs-menu-item");
            itemBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    log.info("BsMenuPopup item clicked, idx={}", idx);
                    try {
                        if (action != null) action.run();
                    } catch (Throwable t) {
                        log.warn("BsMenuPopup item action error", t);
                    }
                    close();
                }
            });
            root.add(itemBtn).left().growX().pad(2).minWidth(140).height(fontH + 10).row();
        }

        root.pack();

        // 边界纠正（默认左下原点定位，超出 stage 则向左/下调整）
        float px = x;
        float py = y - root.getHeight();
        if (py < 0) py = y;  // 上方空间不够，往上弹
        if (px + root.getWidth() > stage.getWidth()) px = stage.getWidth() - root.getWidth();
        if (px < 0) px = 0;
        root.setPosition(px, py);

        stage.addActor(backdrop);
        stage.addActor(root);
        stage.setKeyboardFocus(backdrop);
        root.toFront();
        open = true;
    }

    /**
     * 兼容 scene2d Actor.remove() API，跟 {@link #close()} 一样。
     * 业务方拿到 BsMenuPopup 后可以直接 activeMenu.remove() 关闭。
     */
    public boolean remove() {
        close();
        return true;
    }

    public void close() {
        if (!open) return;
        if (backdrop != null) { backdrop.remove(); backdrop = null; }
        if (root != null) { root.remove(); root = null; }
        open = false;
        if (onClose != null) {
            try { onClose.run(); } catch (Throwable t) { log.warn("BsMenuPopup onClose error", t); }
            onClose = null;
        }
    }

    public boolean isOpen() { return open; }

    @SuppressWarnings("unused")
    private static List<Runnable> empty() { return new ArrayList<>(); }
}
