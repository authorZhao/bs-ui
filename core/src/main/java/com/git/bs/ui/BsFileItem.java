package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 风格文件项（FileItem）—— 文件列表/资源管理器中的单行。
 *
 * <p>结构：</p>
 * <pre>
 * [icon] 文件名.ext        1.2 KB     [⋯ 操作]
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsFileItem item = new BsFileItem(skin)
 *         .name("screenshot.png")
 *         .size(145_678)
 *         .icon(fileTypeIcon("png"))
 *         .onClick(() -> openFile("screenshot.png"))
 *         .actionButton("删除", () -> delete(), BsButton.Variant.DANGER);
 * stage.addActor(item);
 * }</pre>
 */
@Slf4j
public class BsFileItem extends Table {

    private final Image iconImage;
    private final Label nameLabel;
    private final Label sizeLabel;
    private final Table actionRow;
    private final Container<Table> actionWrap;
    private boolean selected = false;

    public BsFileItem(Skin skin) {
        left();
        defaults().pad(0).center();
        pad(6, 10, 6, 10);

        iconImage = new Image();
        iconImage.setScaling(Scaling.fit);
        add(iconImage).size(24).padRight(10).left();

        nameLabel = new Label("", skin);
        nameLabel.setColor(BsTheme.tp());
        add(nameLabel).growX().left();

        sizeLabel = new Label("", skin);
        sizeLabel.setColor(BsTheme.ts());
        sizeLabel.setFontScale(0.9f);
        add(sizeLabel).padRight(10).right();

        actionRow = new Table();
        actionRow.defaults().pad(2);
        actionWrap = new Container<>(actionRow);
        actionWrap.setVisible(false);
        add(actionWrap).right();

        // 点击选中
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setSelected(!selected);
            }
        });

        applyIdleStyle();
    }

    public BsFileItem name(String n) {
        nameLabel.setText(n == null ? "" : n);
        return this;
    }

    /** 设置字节数（自动格式化为 KB/MB/GB）。 */
    public BsFileItem size(long bytes) {
        sizeLabel.setText(formatSize(bytes));
        return this;
    }

    /** 直接设置显示文本（不经过字节格式化）。 */
    public BsFileItem sizeText(String text) {
        sizeLabel.setText(text);
        return this;
    }

    public BsFileItem icon(Drawable d) {
        iconImage.setDrawable(d);
        return this;
    }

    /** 添加一个操作按钮（右侧）。 */
    public BsFileItem actionButton(String label, Runnable onClick, BsButton.Variant variant) {
        BsButton btn = new BsButton(label, BsUI.getSkin(), variant, BsButton.Style.OUTLINE, BsButton.Size.SM);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                event.stop();
                if (onClick != null) {
                    try { onClick.run(); } catch (Throwable t) { log.warn("action", t); }
                }
            }
        });
        actionRow.add(btn);
        actionWrap.setVisible(true);
        return this;
    }

    public BsFileItem setOnClick(Runnable r) {
        if (r != null) {
            clearListeners();
            addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    try { r.run(); } catch (Throwable t) { log.warn("click", t); }
                }
            });
        }
        return this;
    }

    public BsFileItem setSelected(boolean s) {
        this.selected = s;
        applyIdleStyle();
        return this;
    }

    public boolean isSelected() { return selected; }

    private void applyIdleStyle() {
        Skin skin = BsUI.getSkin();
        setBackground(selected
                ? skin.getDrawable("bs-list-selection")
                : skin.getDrawable("bs-window-bg"));
    }

    /** 字节格式化（B/KB/MB/GB）。 */
    public static String formatSize(long bytes) {
        if (bytes < 0) return "";
        if (bytes < 1024) return bytes + " B";
        double v = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int idx = -1;
        for (int i = 0; i < units.length; i++) {
            v /= 1024;
            idx = i;
            if (v < 1024) break;
        }
        return String.format(v >= 100 ? "%.0f %s" : "%.1f %s", v, units[idx]);
    }
}
