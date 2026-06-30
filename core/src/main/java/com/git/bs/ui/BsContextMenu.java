package com.git.bs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 右键上下文菜单（桌面 GUI 高频能力）：给任意 Actor 挂一个右键菜单，
 * 右键（桌面）或长按（触屏 500ms）时在点击位置弹出。
 *
 * <p><b>用法</b>：</p>
 * <pre>{@code
 * new BsContextMenu()
 *         .add("复制",   () -> copy())
 *         .add("粘贴",   () -> paste())
 *         .addDisabled("重命名（只读）")
 *         .addSeparator()
 *         .add("删除",   () -> delete())
 *         .attach(actor);   // 右键/长按该 actor 即弹出
 *
 * // 也可手动弹出（如点击"菜单"按钮）：
 * contextMenu.show(stage, stageX, stageY);
 * }</pre>
 *
 * <p><b>实现要点</b>：</p>
 * <ul>
 *   <li>沿用 {@link BsMenuPopup} 的 backdrop 方案：透明全屏 actor 捕获"点外部"/Esc 关闭。</li>
 *   <li>item 用 {@link TextButton}（style = {@code bs-menu-item}），事件路径清晰。</li>
 *   <li>定位在点击坐标，做边界纠正（下方/右侧不够则翻转）；弹层延后一帧（postRunnable），
 *       避开"在 touchDown 中 addActor 被同一事件移除"的坑。</li>
 *   <li>触屏无右键 → 用 {@link Timer} 实现长按；移动超过阈值取消。</li>
 * </ul>
 *
 * <p><b>v1 不含</b>：子菜单、键盘上下方向键导航、图标项。可后续扩展。</p>
 */
@Slf4j
public class BsContextMenu {

    /** 单个菜单项。 */
    public static final class Item {
        final String text;
        final Runnable action;
        final boolean disabled;
        final boolean separator;

        private Item(String text, Runnable action, boolean disabled, boolean separator) {
            this.text = text; this.action = action; this.disabled = disabled; this.separator = separator;
        }
        /** 普通可点击项。 */
        public static Item of(String text, Runnable action) { return new Item(text, action, false, false); }
        /** 禁用项（灰显、不触发）。 */
        public static Item disabled(String text) { return new Item(text, null, true, false); }
        /** 分隔线。 */
        public static Item separator() { return new Item(null, null, false, true); }
    }

    private static final float LONG_PRESS_SEC = 0.5f;
    private static final float LONG_PRESS_MOVE_TOL = 16f;
    private static final float ITEM_MIN_W = 140f;

    private final List<Item> items = new ArrayList<>();
    /** 已挂载 actor → listener，用于 detach。 */
    private final Map<Actor, EventListener> attached = new IdentityHashMap<>();

    private Table root;
    private Actor backdrop;
    private boolean open;
    private Runnable onClose;

    public BsContextMenu() { this(BsUI.getSkin()); }

    /** skin 参数保留以对齐其它组件 API；实际渲染始终取当前主题 skin（见 {@link #buildAndShow}），
     *  因为菜单按需弹出，可能在切主题后才打开，不能缓存构造时的 skin。 */
    public BsContextMenu(Skin skin) { }

    /** 关闭时的回调（点 item / 点外部 / Esc / close() 都触发）。 */
    public BsContextMenu setOnClose(Runnable onClose) { this.onClose = onClose; return this; }

    // =================== 构建菜单项（builder，返回 this）===================

    public BsContextMenu add(String text, Runnable action) { items.add(Item.of(text, action)); return this; }
    public BsContextMenu addDisabled(String text) { items.add(Item.disabled(text)); return this; }
    public BsContextMenu addSeparator() { items.add(Item.separator()); return this; }
    public BsContextMenu addItem(Item item) { if (item != null) items.add(item); return this; }
    public BsContextMenu clear() { items.clear(); return this; }

    // =================== 挂载 ===================

    /**
     * 挂到 actor：右键（桌面）/ 长按（触屏）时在点击位置弹出。
     * 会把 actor 置为 {@link Touchable#enabled}（否则接收不到事件）；重复 attach 同一 actor 会先移除旧监听。
     */
    public BsContextMenu attach(Actor actor) {
        if (actor == null) return this;
        detach(actor);
        actor.setTouchable(Touchable.enabled);
        EventListener l = createListener(actor);
        actor.addListener(l);
        attached.put(actor, l);
        return this;
    }

    /** 移除某 actor 上的监听。 */
    public void detach(Actor actor) {
        if (actor == null) return;
        EventListener l = attached.remove(actor);
        if (l != null) actor.removeListener(l);
    }

    /** 移除所有已挂载的监听。 */
    public void detachAll() {
        for (Map.Entry<Actor, EventListener> e : new ArrayList<>(attached.entrySet())) {
            e.getKey().removeListener(e.getValue());
        }
        attached.clear();
    }

    // =================== 显隐 ===================

    /** 在 stage 坐标 (x,y) 弹出菜单（左下原点）。可在任何时机直接调用。 */
    public void show(Stage stage, float x, float y) {
        if (stage == null || items.isEmpty()) return;
        if (open) close();
        // postRunnable 延迟一帧：避开"在 touchDown 中 addActor 被同一事件移除"的坑
        Gdx.app.postRunnable(() -> {
            if (open) close();  // 防重入
            buildAndShow(stage, x, y);
        });
    }

    public void close() {
        if (!open) return;
        if (backdrop != null) { backdrop.remove(); backdrop = null; }
        if (root != null) { root.remove(); root = null; }
        open = false;
        if (onClose != null) {
            try { onClose.run(); } catch (Throwable t) { log.warn("BsContextMenu onClose error", t); }
        }
    }

    public boolean isOpen() { return open; }

    // =================== 内部 ===================

    private EventListener createListener(Actor actor) {
        return new InputListener() {
            float downX, downY;
            Timer.Task longPressTask;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // 桌面右键：直接弹
                if (button == Input.Buttons.RIGHT) {
                    Vector2 p = actor.localToStageCoordinates(new Vector2(x, y));
                    show(actor.getStage(), p.x, p.y);
                    return true;
                }
                // 触屏长按（pointer 0，触屏上即手指）
                if (pointer == 0 && button == Input.Buttons.LEFT) {
                    downX = x;
                    downY = y;
                    cancelLongPress();
                    final float lx = x, ly = y;
                    longPressTask = Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            Vector2 p = actor.localToStageCoordinates(new Vector2(lx, ly));
                            Stage s = actor.getStage();
                            if (s != null) show(s, p.x, p.y);
                        }
                    }, LONG_PRESS_SEC);
                }
                return false;  // 左键不拦截，保证 actor 自身点击仍可用
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                cancelLongPress();
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (longPressTask != null
                        && (Math.abs(x - downX) > LONG_PRESS_MOVE_TOL || Math.abs(y - downY) > LONG_PRESS_MOVE_TOL)) {
                    cancelLongPress();  // 移动过多，判定为拖拽而非长按
                }
            }

            private void cancelLongPress() {
                if (longPressTask != null) { longPressTask.cancel(); longPressTask = null; }
            }
        };
    }

    private void buildAndShow(Stage stage, float x, float y) {
        // 始终取当前主题 skin：菜单按需弹出，可能在切主题后才打开
        Skin skin = BsUI.getSkin();
        // backdrop：透明全屏，捕获"点外部"→关闭，拦截 Esc
        backdrop = new Actor();
        backdrop.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        backdrop.setTouchable(Touchable.enabled);
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
        root.setTouchable(Touchable.enabled);

        float fontH = skin.getFont("default").getLineHeight();
        Color border = skin.get("bs-border", Color.class);

        for (Item it : items) {
            if (it.separator) {
                Table sep = new Table();
                sep.setBackground(BsUI.drawableOf(border));
                root.add(sep).growX().height(1f).padTop(4).padBottom(4).row();
                continue;
            }
            TextButton btn = new TextButton(it.text == null ? "" : it.text, skin, "bs-menu-item");
            btn.setDisabled(it.disabled);
            final Runnable action = it.action;
            final boolean disabled = it.disabled;
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (disabled) return;
                    try {
                        if (action != null) action.run();
                    } catch (Throwable t) {
                        log.warn("BsContextMenu item action error", t);
                    }
                    close();
                }
            });
            root.add(btn).left().growX().pad(2).minWidth(ITEM_MIN_W).height(fontH + 10).row();
        }

        root.pack();

        // item 过多时按 stage 高度封顶（v1 不滚动）
        float maxH = stage.getHeight() * 0.7f;
        if (root.getHeight() > maxH) root.setHeight(maxH);

        // 边界纠正（左下原点）：下方不够翻上方，右侧越界左移
        float px = x;
        float py = y - root.getHeight();
        if (py < 0) py = y;
        if (px + root.getWidth() > stage.getWidth()) px = stage.getWidth() - root.getWidth();
        if (px < 0) px = 0;
        root.setPosition(px, py);

        stage.addActor(backdrop);
        stage.addActor(root);
        stage.setKeyboardFocus(backdrop);
        root.toFront();
        open = true;
    }
}
