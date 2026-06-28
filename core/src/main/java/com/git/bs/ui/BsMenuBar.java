package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap 风格菜单栏：横向排列的菜单按钮，每个对应一个下拉子菜单。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsMenuBar bar = new BsMenuBar(skin);
 * BsMenuBar.BsMenu file = bar.addMenu("File");
 * file.addItem("New", () -> ...);
 * file.addItem("Open", () -> ...);
 * file.addSeparator();           // 占位的不可点项（视觉分隔）
 * file.addItem("Exit", () -> ...);
 * }</pre>
 *
 * <p>点击菜单按钮会在按钮下方弹出一个浮层（{@link BsMenuPopup}）显示所有 item；
 * 点 item 触发回调并关闭浮层，点外部或按 Esc 也关闭。</p>
 */
public class BsMenuBar extends Table {

    /** 单个菜单项：label + 触发动作。 */
    public static class BsMenuItem {
        public final String label;
        public final Runnable action;
        public final boolean separator;
        public BsMenuItem(String label, Runnable action) {
            this.label = label; this.action = action; this.separator = false;
        }
        /** separator 占位项：label 传 null，不可点击。 */
        public BsMenuItem() { this.label = null; this.action = null; this.separator = true; }
    }

    /** 一个菜单（标题 + 触发按钮 + items 列表）。 */
    public static class BsMenu {
        public final String title;
        @Getter
        public final List<BsMenuItem> items = new ArrayList<>();
        public final TextButton button;
        private BsMenuPopup popup;

        public BsMenu(String title, TextButton button, Skin skin) {
            this.title = title; this.button = button;
        }

        /** 添加一个可点击项。 */
        public BsMenu addItem(String label, Runnable action) {
            items.add(new BsMenuItem(label, action));
            return this;
        }

        /** 添加视觉分隔符（占位、不可点击）。 */
        public BsMenu addSeparator() {
            items.add(new BsMenuItem());
            return this;
        }

        /** 在 anchor 按钮下方弹出 items 浮层。 */
        public void showPopup() {
            // 把 separator 过滤掉（ BsList 不支持分隔），仅显示可点击项
            List<String> labels = new ArrayList<>();
            List<Runnable> actions = new ArrayList<>();
            for (BsMenuItem it : items) {
                if (it.separator) continue;
                labels.add(it.label);
                actions.add(it.action);
            }
            if (labels.isEmpty()) return;
            if (popup != null && popup.isOpen()) { popup.close(); return; }
            popup = new BsMenuPopup(BsUI.getSkin());
            // popup 关闭时（任何路径：点item/外部/Esc）都通知 onClose，让 menu bar 清按钮 checked
            popup.setOnClose(() -> {
                if (button.isChecked()) button.setChecked(false);
            });
            popup.show(button.getStage(), button, labels, actions);
        }

        public boolean isPopupOpen() {
            return popup != null && popup.isOpen();
        }

        public void closePopup() {
            if (popup != null) popup.close();
        }
    }

    @Getter
    private final List<BsMenu> menus = new ArrayList<>();

    public BsMenuBar(Skin skin) {
        setBackground(skin.newDrawable("bs-menu-bar-bg"));
        pad(2);
        left();
    }

    /**
     * 添加一个菜单标题按钮（不含 items，items 通过返回的 BsMenu.addItem 继续追加）。
     */
    public BsMenu addMenu(String title) {
        return addMenu(title, null);
    }

    /**
     * 添加一个带图标的菜单标题按钮。
     * @param title 标题
     * @param icon 图标 Drawable（null=无图标）；可用 {@link BsIcon#get(String)} 加载
     */
    public BsMenu addMenu(String title, Drawable icon) {
        TextButton btn = new TextButton(title, BsUI.getSkin(), "bs-menu-title");
        btn.setProgrammaticChangeEvents(false);
        // 加图标（如果有）：直接重排 cell 加 Image
        if (icon != null) {
            com.badlogic.gdx.scenes.scene2d.ui.Image img = new com.badlogic.gdx.scenes.scene2d.ui.Image(icon);
            img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            com.badlogic.gdx.scenes.scene2d.ui.Label label = btn.getLabel();
            btn.clearChildren();
            btn.add(img).size(16, 16).padRight(4);
            btn.add(label);
        }
        BsMenu menu = new BsMenu(title, btn, BsUI.getSkin());
        menus.add(menu);
        // 点击按钮：弹/收浮层（手动管理 checked 视觉，避免 ClickListener over 状态在
        // backdrop 场景下卡死导致按钮变深色）
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (menu.isPopupOpen()) {
                    menu.closePopup();
                    btn.setChecked(false);
                } else {
                    // 关闭其它菜单的浮层 + 清除其它按钮的 checked
                    for (BsMenu m : menus) {
                        if (m != menu) {
                            m.closePopup();
                            m.button.setChecked(false);
                        }
                    }
                    menu.showPopup();
                    btn.setChecked(true);
                }
            }
        });
        add(btn).padRight(2);
        return menu;
    }

    /**
     * 关闭所有菜单的浮层（外部点击关闭 backdrop 时 BsMenuPopup 自行 close，但按钮的 checked
     * 状态需要这里清；可由 backdrop close 钩子或屏幕 refresh 调用）。
     */
    public void closeAllMenus() {
        for (BsMenu m : menus) {
            m.closePopup();
            m.button.setChecked(false);
        }
    }
}
