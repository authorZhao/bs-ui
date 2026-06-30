package com.git.bs.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/// 级联选择器（Cascader）：多级联动选择，如 省 > 市 > 区。
///
/// 只读输入框，点击弹出多列浮层：每列是一级选项，选中某项后展开下一列（其子项），
/// 选到**叶子节点**（无子项）时回填完整路径并关闭。
///
/// 用法：
/// ```java
/// BsCascader.Option root = new BsCascader.Option().label("广东").value("gd")
///         .child(new BsCascader.Option().label("深圳").value("sz")
///                 .child(new BsCascader.Option().label("南山区").value("ns")));
///
/// BsCascader c = new BsCascader(skin)
///         .setOptions(List.of(root))
///         .setOnChange(path -> setStatus(joinLabels(path)));
/// ```
///
/// 实现：继承 {@link BsTextField}（只读），浮层按当前选择路径动态重建列；
/// 每列用 {@link ScrollPane} + TextButton（事件清晰，长列表可纵向滚动）。
@Slf4j
public class BsCascader extends BsTextField {

    /// 级联选项节点。
    public static final class Option {
        /** 选项值（可空，仅用于业务标识）。 */
        public String value;
        /** 选项显示文本。 */
        public String label;
        /** 子级选项。为空表示叶子节点。 */
        public final List<Option> children = new ArrayList<>();

        public Option label(String l) { this.label = l; return this; }
        public Option value(String v) { this.value = v; return this; }
        public Option child(Option c) { if (c != null) children.add(c); return this; }
        public Option children(List<Option> cs) { if (cs != null) children.addAll(cs); return this; }
        public boolean isLeaf() { return children.isEmpty(); }
    }

    private List<Option> options = new ArrayList<>();
    private Consumer<List<Option>> onChange;
    private float colW = 140f;
    private float colH = 240f;

    private Actor backdrop;
    private Table popupRoot;
    private boolean open;
    /** 当前展开路径（每级选中的 Option）。 */
    private final List<Option> path = new ArrayList<>();

    public BsCascader(Skin skin) {
        super("", skin);
        setTextFieldFilter((field, c) -> false);   // 只读，由浮层回填
        addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { openPopup(); }
        });
    }

    public BsCascader setOptions(List<Option> opts) {
        this.options = opts != null ? opts : new ArrayList<>();
        return this;
    }

    public BsCascader setOnChange(Consumer<List<Option>> c) {
        this.onChange = c;
        return this;
    }

    public BsCascader setColumnSize(float w, float h) {
        this.colW = w;
        this.colH = h;
        return this;
    }

    /// 当前已选路径（叶子选齐才是完整路径；中途为部分路径）。
    public List<Option> getPath() {
        return new ArrayList<>(path);
    }

    private void openPopup() {
        if (open) {
            closePopup();
            return;
        }
        Stage stage = getStage();
        if (stage == null) return;
        Skin skin = BsUI.getSkin();
        path.clear();

        popupRoot = new Table();
        popupRoot.setBackground(skin.getDrawable("bs-window-bg"));
        popupRoot.setTouchable(Touchable.enabled);
        rebuildColumns(skin);

        backdrop = new Actor();
        backdrop.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        backdrop.setTouchable(Touchable.enabled);
        backdrop.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                closePopup();
                return true;
            }
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) { closePopup(); return true; }
                return false;
            }
        });

        Vector2 pos = localToStageCoordinates(new Vector2(0, 0));
        float x = pos.x;
        float y = pos.y - popupRoot.getHeight();
        if (y < 0) y = pos.y + getHeight();
        if (x + popupRoot.getWidth() > stage.getWidth()) x = stage.getWidth() - popupRoot.getWidth();
        if (x < 0) x = 0;
        popupRoot.setPosition(x, y);

        stage.addActor(backdrop);
        stage.addActor(popupRoot);
        stage.setKeyboardFocus(backdrop);
        open = true;
    }

    private void rebuildColumns(Skin skin) {
        popupRoot.clearChildren();

        // 列 0 = root；之后每列 = path[i].children
        List<List<Option>> cols = new ArrayList<>();
        if (!options.isEmpty()) {
            cols.add(options);
            for (Option o : path) {
                if (!o.isLeaf()) {
                    cols.add(o.children);
                } else {
                    break;
                }
            }
        }
        if (cols.isEmpty()) {
            com.badlogic.gdx.scenes.scene2d.ui.Label empty =
                    new com.badlogic.gdx.scenes.scene2d.ui.Label("(无选项)", skin);
            empty.setColor(BsTheme.tm());
            popupRoot.add(empty).pad(20);
            popupRoot.pack();
            return;
        }

        Table colsRow = new Table();
        colsRow.pad(6);
        for (int level = 0; level < cols.size(); level++) {
            final int lvl = level;
            List<Option> colOpts = cols.get(level);
            Table col = new Table();
            for (Option opt : colOpts) {
                final Option o = opt;
                TextButton btn = new TextButton(opt.label == null ? "" : opt.label, skin, "bs-menu-item");
                if (lvl < path.size() && path.get(lvl) == o) {
                    btn.setChecked(true);   // 高亮当前路径上的选项
                }
                btn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) { select(lvl, o); }
                });
                col.add(btn).left().growX().pad(1).row();
            }
            ScrollPane sp = new ScrollPane(col, skin);
            sp.setScrollingDisabled(true, false);   // 仅纵向滚动
            sp.setFadeScrollBars(false);
            colsRow.add(sp).width(colW).height(colH).padRight(4);
        }
        popupRoot.add(colsRow);
        popupRoot.pack();
    }

    private void select(int level, Option opt) {
        // 截断 path 到 level，追加 opt
        while (path.size() > level) {
            path.remove(path.size() - 1);
        }
        path.add(opt);

        if (opt.isLeaf()) {
            // 回填 "a / b / c" + 回调 + 关闭
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) sb.append(" / ");
                sb.append(path.get(i).label);
            }
            setTextProgrammatic(sb.toString());
            if (onChange != null) {
                try { onChange.accept(new ArrayList<>(path)); } catch (Throwable t) { log.warn("BsCascader onChange error", t); }
            }
            closePopup();
        } else {
            rebuildColumns(BsUI.getSkin());   // 展开下一列
        }
    }

    private void closePopup() {
        if (!open) return;
        if (backdrop != null) { backdrop.remove(); backdrop = null; }
        if (popupRoot != null) { popupRoot.remove(); popupRoot = null; }
        open = false;
    }
}
