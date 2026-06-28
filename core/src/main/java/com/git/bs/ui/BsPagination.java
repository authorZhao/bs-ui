package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.IntConsumer;

/**
 * Bootstrap 风格分页：上一页 / 页码 / 下一页 + 当前页/总页数显示。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsPagination pg = new BsPagination(skin);
 * pg.setTotalPages(10);
 * pg.setCurrentPage(1);
 * pg.setOnChange(page -> {
 *     table.setData(sliceForPage(page), headers);
 *     setStatus("跳到第 " + page + " 页");
 * });
 * }</pre>
 *
 * <p>页码按钮最多显示 {@link #maxPageButtons} 个（默认 7）；
 * 超过则中间用 "..." 折叠。</p>
 */
@Slf4j
public class BsPagination extends Table {

    @Getter private int currentPage = 1;
    @Getter private int totalPages = 1;
    private int maxPageButtons = 7;
    private IntConsumer onChange;

    public BsPagination(Skin skin) {
        defaults().pad(2);
        rebuild();
    }

    public void setTotalPages(int total) {
        this.totalPages = Math.max(1, total);
        if (currentPage > totalPages) currentPage = totalPages;
        rebuild();
    }

    public void setCurrentPage(int page) {
        int p = Math.max(1, Math.min(totalPages, page));
        if (p == currentPage) return;
        this.currentPage = p;
        rebuild();
    }

    public void setOnChange(IntConsumer cb) { this.onChange = cb; }

    public void setMaxPageButtons(int n) {
        this.maxPageButtons = Math.max(5, n);
        rebuild();
    }

    private void gotoPage(int p) {
        int target = Math.max(1, Math.min(totalPages, p));
        if (target == currentPage) return;
        currentPage = target;
        rebuild();
        if (onChange != null) {
            try { onChange.accept(target); } catch (Throwable t) { log.warn("onChange error", t); }
        }
    }

    private void rebuild() {
        clearChildren();
        Skin skin = BsUI.getSkin();

        Color primary = BsPalette.PRIMARY.getMain();   // 主色蓝
        Color normalCol = BsTheme.ts();                 // 普通页码灰

        // 上一页：用 Pixmap 箭头 Image（避免字体不含 ‹ › 字符）
        boolean prevEnabled = currentPage > 1;
        add(makeArrowImage(prevEnabled, false, () -> gotoPage(currentPage - 1))).size(24, 24).pad(2);

        // 页码按钮（带"..."折叠）
        int[] pages = computePageButtons();
        for (int i = 0; i < pages.length; i++) {
            int p = pages[i];
            if (p == 0) {
                add(new com.badlogic.gdx.scenes.scene2d.ui.Label("…", skin)).width(20).center();
                continue;
            }
            // 当前页用主色蓝（独立 LabelStyle），其它页用普通灰字
            Color c = (p == currentPage) ? primary : normalCol;
            final int target = p;
            add(makePageLabel(String.valueOf(p), c, p != currentPage, () -> gotoPage(target)))
                    .size(28, 28).pad(2);
        }

        // 下一页
        boolean nextEnabled = currentPage < totalPages;
        add(makeArrowImage(nextEnabled, true, () -> gotoPage(currentPage + 1))).size(24, 24).pad(2);

        // 当前页 / 总页数
        add(new com.badlogic.gdx.scenes.scene2d.ui.Label(
                "  " + currentPage + " / " + totalPages, skin)).padLeft(8);
    }

    /** 翻页箭头：用 skin 里的 bs-arrow-* drawable（程序化三角，不依赖字体）。 */
    private com.badlogic.gdx.scenes.scene2d.ui.Image makeArrowImage(
            boolean enabled, boolean pointRight, Runnable action) {
        String name = (pointRight ? "bs-arrow-right" : "bs-arrow-left") + (enabled ? "" : "-disabled");
        com.badlogic.gdx.scenes.scene2d.ui.Image img = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                BsUI.getSkin().getDrawable(name));
        if (enabled && action != null) {
            img.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            img.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { action.run(); }
            });
        }
        return img;
    }

    /** 页码数字 Label（独立 LabelStyle 设 fontColor， setColor(white) 防 parent color 干扰）。 */
    private com.badlogic.gdx.scenes.scene2d.ui.Label makePageLabel(
            String text, Color color, boolean clickable, Runnable action) {
        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle ls =
                new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        ls.font = BsUI.getSkin().getFont("default");
        ls.fontColor = color;
        com.badlogic.gdx.scenes.scene2d.ui.Label label =
                new com.badlogic.gdx.scenes.scene2d.ui.Label(text, ls);
        label.setColor(Color.WHITE);
        if (clickable && action != null) {
            label.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            label.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { action.run(); }
            });
        }
        return label;
    }

    /**
     * 计算要显示的页码按钮数组（0 表示 "..."）。
     * 总数不超过 maxPageButtons。
     */
    private int[] computePageButtons() {
        int total = totalPages;
        int cur = currentPage;
        if (total <= maxPageButtons) {
            int[] all = new int[total];
            for (int i = 0; i < total; i++) all[i] = i + 1;
            return all;
        }
        // 折叠：1 ... (cur-1) cur (cur+1) ... total
        // 简化版：保留首末 + cur 前后
        java.util.List<Integer> list = new java.util.ArrayList<>();
        list.add(1);
        int start = Math.max(2, cur - 1);
        int end = Math.min(total - 1, cur + 1);
        if (start > 2) list.add(0);
        for (int i = start; i <= end; i++) list.add(i);
        if (end < total - 1) list.add(0);
        list.add(total);
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
