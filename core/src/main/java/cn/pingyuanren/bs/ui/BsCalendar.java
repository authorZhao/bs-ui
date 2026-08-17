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

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import cn.pingyuanren.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/// 可内嵌的月历面板（Calendar）：月份导航 + 星期表头 + 6×7 日期网格。
///
/// 把 `BsDatePickerPopup` 里锁死的网格逻辑抽成独立组件，供内嵌展示、
/// `BsDateRangePicker` 等复用。
///
/// 两种模式：
/// - `SINGLE`：单选，`setOnSelect(Consumer)` 回调。
/// - `RANGE`：区间选，先点起点再点终点（自动校正顺序）；
///   `setOnRange(BiConsumer)` 回调，起点选定后传 `(start, null)`，终点点齐传 `(start, end)`。
///
/// 用法：
/// ```java
/// // 单选
/// BsCalendar cal = new BsCalendar(skin)
///         .setOnSelect(d -> setStatus("选了 " + d));
/// // 区间
/// BsCalendar range = new BsCalendar(skin, BsCalendar.Mode.RANGE)
///         .setOnRange((s, e) -> { if (e != null) setStatus(s + " ~ " + e); });
/// stage.addActor(cal);
/// ```
///
/// 实现：`Table`，clearChildren + rebuild 重建（切月/选择后）。
/// 今天=主色字、选中=checked、区间内=primary-soft 底 + 主色字、跨月灰显。周一为首列。
/// @author authorZhao
/// @since 2026-07-16
@Slf4j
public class BsCalendar extends Table {

    public enum Mode { SINGLE, RANGE }

    private static final DayOfWeek[] WEEK = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };

    private final Mode mode;

    private boolean showNav = true;
    private float cellW = 36f;
    private float cellH = 28f;

    private YearMonth currentMonth;
    private LocalDate selected;               // SINGLE
    private LocalDate rangeStart, rangeEnd;   // RANGE

    private Consumer<LocalDate> onSelect;
    private BiConsumer<LocalDate, LocalDate> onRange;

    public BsCalendar(Skin skin) {
        this(skin, Mode.SINGLE);
    }

    public BsCalendar(Skin skin, Mode mode) {
        this.mode = mode;
        this.currentMonth = YearMonth.now();
        rebuild();
    }

    // =================== API ===================

    public BsCalendar setMonth(YearMonth ym) {
        this.currentMonth = ym != null ? ym : YearMonth.now();
        rebuild();
        return this;
    }

    /// SINGLE：设置选中值（同时把视图切到该值所在月）。
    public BsCalendar setValue(LocalDate d) {
        this.selected = d;
        if (d != null) currentMonth = YearMonth.from(d);
        rebuild();
        return this;
    }

    /// RANGE：设置区间（视图切到 start 所在月）。
    public BsCalendar setRange(LocalDate start, LocalDate end) {
        this.rangeStart = start;
        this.rangeEnd = end;
        if (start != null) currentMonth = YearMonth.from(start);
        rebuild();
        return this;
    }

    public BsCalendar setCellSize(float w, float h) {
        this.cellW = w;
        this.cellH = h;
        rebuild();
        return this;
    }

    public BsCalendar setShowNav(boolean b) {
        this.showNav = b;
        rebuild();
        return this;
    }

    public BsCalendar setOnSelect(Consumer<LocalDate> c) { this.onSelect = c; return this; }

    public BsCalendar setOnRange(BiConsumer<LocalDate, LocalDate> c) { this.onRange = c; return this; }

    public LocalDate getValue() { return selected; }

    public LocalDate getRangeStart() { return rangeStart; }

    public LocalDate getRangeEnd() { return rangeEnd; }

    // =================== 内部 ===================

    private void rebuild() {
        clearChildren();
        Skin skin = BsUI.getSkin();   // 主题安全：重建时取当前 skin

        if (showNav) {
            Table nav = new Table();
            // 中间标题保留 bs-menu-title 样式（字体较粗，整体协调）
            TextButton title = new TextButton(currentMonth.format(DateTimeFormatter.ofPattern(datePattern())), skin, "bs-menu-title");
            title.setDisabled(true);
            // 两侧箭头改用 skin 的 bs-arrow-* drawable（程序化三角，不依赖字体字符，
            // 在 light/dark 主题下都是主色，对比稳定）
            com.badlogic.gdx.scenes.scene2d.ui.Image prev = arrowImage(false, () -> {
                currentMonth = currentMonth.minusMonths(1);
                rebuild();
            });
            com.badlogic.gdx.scenes.scene2d.ui.Image next = arrowImage(true, () -> {
                currentMonth = currentMonth.plusMonths(1);
                rebuild();
            });
            nav.add(prev).size(cellW, cellH);
            nav.add(title).growX().pad(0, 4, 0, 4);
            nav.add(next).size(cellW, cellH);
            add(nav).growX().pad(4).row();
        }

        // 星期表头（周一~周日）
        Table header = new Table();
        for (DayOfWeek dow : WEEK) {
            Label h = new Label(dow.getDisplayName(TextStyle.NARROW, BsI18n.javaLocale()), skin);
            h.setColor(BsTheme.ts());
            header.add(h).width(cellW).center();
        }
        add(header).pad(2, 4, 2, 4).row();

        // 6×7 日期网格
        LocalDate first = currentMonth.atDay(1);
        int firstDow = first.getDayOfWeek().getValue();   // MONDAY=1 .. SUNDAY=7
        LocalDate cursor = first.minusDays(firstDow - 1);
        LocalDate today = LocalDate.now();

        Table grid = new Table();
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                final LocalDate d = cursor.plusDays(row * 7L + col);
                boolean inMonth = YearMonth.from(d).equals(currentMonth);
                TextButton cell = new TextButton(String.valueOf(d.getDayOfMonth()), skin, "bs-menu-item");
                if (!inMonth) {
                    cell.getLabel().setColor(BsTheme.tm());
                }
                if (d.equals(today)) {
                    cell.getLabel().setColor(BsPalette.PRIMARY.getMain());
                }
                // 选中 / 区间高亮
                boolean isEnd = mode == Mode.RANGE && (d.equals(rangeStart) || d.equals(rangeEnd));
                boolean isSel = mode == Mode.SINGLE ? d.equals(selected) : isEnd;
                boolean inRange = mode == Mode.RANGE && rangeStart != null && rangeEnd != null
                        && d.isAfter(rangeStart) && d.isBefore(rangeEnd);
                if (isSel) cell.setChecked(true);
                if (inRange) {
                    cell.setBackground(skin.getDrawable("bs-primary-soft-bg"));
                    cell.getLabel().setColor(BsPalette.PRIMARY.getMain());
                }
                cell.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent e, float x, float y) { onCellClick(d); }
                });
                grid.add(cell).size(cellW, cellH).pad(1);
            }
            grid.row();
        }
        add(grid).pad(2, 4, 2, 4).row();
    }

    private void onCellClick(LocalDate d) {
        if (mode == Mode.SINGLE) {
            selected = d;
            rebuild();
            if (onSelect != null) {
                try { onSelect.accept(d); } catch (Throwable t) { log.warn("BsCalendar onSelect error", t); }
            }
            return;
        }
        // RANGE：无起点 / 已选齐 → 开始新区间；有起点无终点 → 设终点
        if (rangeStart == null || rangeEnd != null) {
            rangeStart = d;
            rangeEnd = null;
            rebuild();
            if (onRange != null) {
                try { onRange.accept(rangeStart, null); } catch (Throwable t) { log.warn("BsCalendar onRange error", t); }
            }
        } else {
            if (d.isBefore(rangeStart)) {
                rangeEnd = rangeStart;
                rangeStart = d;
            } else {
                rangeEnd = d;
            }
            rebuild();
            if (onRange != null) {
                try { onRange.accept(rangeStart, rangeEnd); } catch (Throwable t) { log.warn("BsCalendar onRange error", t); }
            }
        }
    }

    /** 月份切换箭头：用 skin 的 bs-arrow-* drawable（程序化三角，主题主色，对比稳定）。 */
    private com.badlogic.gdx.scenes.scene2d.ui.Image arrowImage(boolean pointRight, Runnable action) {
        String name = pointRight ? "bs-arrow-right" : "bs-arrow-left";
        com.badlogic.gdx.scenes.scene2d.ui.Image img = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                BsUI.getSkin().getDrawable(name));
        img.setScaling(com.badlogic.gdx.utils.Scaling.contain);
        img.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        if (action != null) {
            img.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
            });
        }
        return img;
    }

    /** 根据当前 Locale 选月份标题格式串：中文用 "yyyy 年 MM 月"，英文用 "MMMM yyyy"，其他用 "yyyy-MM"。 */
    private static String datePattern() {
        java.util.Locale locale = BsI18n.javaLocale();
        if (java.util.Locale.CHINA.equals(locale) || java.util.Locale.CHINESE.equals(locale)) {
            return "yyyy 年 MM 月";
        } else if (java.util.Locale.US.equals(locale) || java.util.Locale.ENGLISH.equals(locale)) {
            return "MMMM yyyy";
        } else {
            return "yyyy-MM";
        }
    }
}
