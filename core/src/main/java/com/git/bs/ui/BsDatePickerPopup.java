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
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.function.Consumer;

/**
 * 日期（+时间）选择器浮层：月份导航 + 日期网格 + 今日按钮 + （可选）时/分/秒输入框。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsDatePickerPopup popup = new BsDatePickerPopup(skin, true); // 含时间
 * popup.setOnPick(dt -> textField.setText(dt.toString()));
 * popup.show(stage, anchor, LocalDateTime.now());
 * }</pre>
 *
 * <p>实现：透明全屏 backdrop 捕获"点外部"关闭；月份导航按钮切换年月；
 * 日期网格用 {@link TextButton}（独立 actor，事件清晰）。
 * 含时间模式时底部增加"时:分:秒"3 个数字输入框，点"确定"按钮提交。</p>
 */
@Slf4j
public class BsDatePickerPopup {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private final boolean withTime;
    private Table root;
    private Actor backdrop;
    private boolean open;
    private Consumer<LocalDateTime> onPick;
    private Runnable onClose;

    private YearMonth currentMonth;
    private LocalDateTime initial;
    private LocalDate selectedDate;
    private LocalTime selectedTime;

    // 时间输入框（withTime=true 时创建）
    private BsTextField hourField, minuteField, secondField;

    public BsDatePickerPopup(Skin skin) {
        this(skin, false);
    }

    public BsDatePickerPopup(Skin skin, boolean withTime) {
        this.withTime = withTime;
    }

    public void setOnPick(Consumer<LocalDateTime> onPick) { this.onPick = onPick; }
    public void setOnClose(Runnable onClose) { this.onClose = onClose; }

    public void show(Stage stage, Actor anchor, LocalDateTime initialDt) {
        if (open) close();
        this.initial = initialDt != null ? initialDt : LocalDateTime.now();
        this.selectedDate = this.initial.toLocalDate();
        this.selectedTime = this.initial.toLocalTime();
        this.currentMonth = YearMonth.from(this.initial);

        Vector2 pos = anchor.localToStageCoordinates(new Vector2(0, 0));
        float anchorBottom = pos.y;
        float anchorTop = pos.y + anchor.getHeight();

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

        root = new Table();
        root.setBackground(BsUI.getSkin().getDrawable("bs-window-bg"));
        root.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        buildContent();

        root.pack();
        // 定位
        float x = pos.x;
        float y = anchorBottom - root.getHeight();
        if (y < 0) y = anchorTop;
        if (x + root.getWidth() > stage.getWidth()) x = stage.getWidth() - root.getWidth();
        if (x < 0) x = 0;
        root.setPosition(x, y);

        stage.addActor(backdrop);
        stage.addActor(root);
        stage.setKeyboardFocus(backdrop);
        open = true;
    }

    private void buildContent() {
        Skin skin = BsUI.getSkin();
        root.clearChildren();

        // 月份导航：‹ 年月 ›
        Table nav = new Table();
        // 中间标题保留 bs-menu-title 样式；两侧箭头改用 skin 的 bs-arrow-* drawable
        // （程序化三角，不依赖字体字符，在 light/dark 主题下都是主色，对比稳定）
        com.badlogic.gdx.scenes.scene2d.ui.Image prev = arrowImage(false);
        com.badlogic.gdx.scenes.scene2d.ui.Image next = arrowImage(true);
        TextButton title = new TextButton(currentMonth.format(DateTimeFormatter.ofPattern(datePattern())), skin, "bs-menu-title");
        title.setDisabled(true);
        prev.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                currentMonth = currentMonth.minusMonths(1); rebuild();
            }
        });
        next.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                currentMonth = currentMonth.plusMonths(1); rebuild();
            }
        });
        nav.add(prev).size(36, 28);
        nav.add(title).growX().pad(0, 4, 0, 4);
        nav.add(next).size(36, 28);
        root.add(nav).growX().pad(4).row();

        // 星期表头（周一~周日，中国习惯）
        Table header = new Table();
        DayOfWeek[] order = { DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY };
        for (DayOfWeek dow : order) {
            String name = dow.getDisplayName(TextStyle.NARROW, BsI18n.javaLocale());
            com.badlogic.gdx.scenes.scene2d.ui.Label h =
                    new com.badlogic.gdx.scenes.scene2d.ui.Label(name, skin);
            h.setColor(BsTheme.ts());
            header.add(h).width(36).center();
        }
        root.add(header).pad(2, 4, 2, 4).row();

        // 日期网格：6 行 × 7 列
        Table grid = new Table();
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int firstDow = firstOfMonth.getDayOfWeek().getValue();
        LocalDate start = firstOfMonth.minusDays(firstDow - 1);
        LocalDate today = LocalDate.now();
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                final LocalDate d = start.plusDays(row * 7 + col);
                boolean inMonth = YearMonth.from(d).equals(currentMonth);
                String label = String.valueOf(d.getDayOfMonth());
                TextButton cell = new TextButton(label, skin, "bs-menu-item");
                if (!inMonth) {
                    cell.getLabel().setColor(BsTheme.tm());
                }
                if (d.equals(today)) {
                    cell.getLabel().setColor(BsPalette.PRIMARY.getMain());
                }
                if (d.equals(selectedDate)) {
                    cell.setChecked(true);
                }
                cell.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        selectedDate = d;
                        // 含时间模式：等用户点"确定"才提交，避免时间被吞
                        if (!withTime) {
                            confirm();
                        } else {
                            rebuild();
                        }
                    }
                });
                grid.add(cell).size(36, 28).pad(1);
            }
            grid.row();
        }
        root.add(grid).pad(2, 4, 2, 4).row();

        // 今日按钮（仅日期模式直接确认；含时间模式只设日期部分）
        TextButton todayBtn = new TextButton(BsI18n.get("core.datepicker.today", "今日 ({0})", today.format(ISO)), skin, "bs-menu-title");
        todayBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedDate = today;
                if (!withTime) {
                    confirm();
                } else {
                    rebuild();
                }
            }
        });
        root.add(todayBtn).growX().pad(4).row();

        // 时间输入区（仅 withTime=true）
        if (withTime) {
            Table timeRow = new Table();
            timeRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label(BsI18n.get("core.datepicker.time", "时间:"), skin)).padRight(6);
            hourField = makeTimeField(selectedTime.getHour());
            minuteField = makeTimeField(selectedTime.getMinute());
            secondField = makeTimeField(selectedTime.getSecond());
            timeRow.add(hourField).width(40);
            timeRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label(":", skin)).pad(2);
            timeRow.add(minuteField).width(40);
            timeRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label(":", skin)).pad(2);
            timeRow.add(secondField).width(40);
            root.add(timeRow).pad(6, 4, 2, 4).row();

            // 确定按钮
            TextButton ok = new TextButton(BsI18n.get("btn.ok", "确定"), skin, "bs-btn-primary");
            ok.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    confirm();
                }
            });
            root.add(ok).growX().pad(4);
        }
    }

    /** 创建一个 2 位数字输入框，初始值 v。 */
    private BsTextField makeTimeField(int v) {
        BsTextField f = new BsTextField(String.format("%02d", v), BsUI.getSkin());
        f.setTextFieldFilter((field, c) -> Character.isDigit(c));
        f.setMaxLength(2);
        return f;
    }

    /** 读取时间输入框的值，校验范围，组装 LocalDateTime 触发 onPick 并关闭。 */
    private void confirm() {
        LocalTime t = selectedTime;
        if (withTime) {
            int h = parseClamp(hourField.getText(), 0, 23, selectedTime.getHour());
            int m = parseClamp(minuteField.getText(), 0, 59, selectedTime.getMinute());
            int s = parseClamp(secondField.getText(), 0, 59, selectedTime.getSecond());
            t = LocalTime.of(h, m, s);
        }
        LocalDateTime dt = LocalDateTime.of(selectedDate, t);
        log.info("BsDatePickerPopup picked: {}", dt);
        if (onPick != null) {
            try { onPick.accept(dt); } catch (Throwable t2) { log.warn("onPick error", t2); }
        }
        close();
    }

    private static int parseClamp(String s, int min, int max, int fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try {
            int v = Integer.parseInt(s);
            if (v < min || v > max) return fallback;
            return v;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void rebuild() {
        // 含时间模式 rebuild 时保留时间输入框当前值
        if (withTime && hourField != null) {
            selectedTime = LocalTime.of(
                    parseClamp(hourField.getText(), 0, 23, selectedTime.getHour()),
                    parseClamp(minuteField.getText(), 0, 59, selectedTime.getMinute()),
                    parseClamp(secondField.getText(), 0, 59, selectedTime.getSecond()));
        }
        buildContent();
        root.pack();
    }

    public void close() {
        if (!open) return;
        if (backdrop != null) { backdrop.remove(); backdrop = null; }
        if (root != null) { root.remove(); root = null; }
        open = false;
        if (onClose != null) {
            try { onClose.run(); } catch (Throwable t) { log.warn("onClose error", t); }
            onClose = null;
        }
    }

    public boolean isOpen() { return open; }

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

    /** 月份切换箭头：用 skin 的 bs-arrow-* drawable（程序化三角，主题主色，对比稳定）。 */
    private com.badlogic.gdx.scenes.scene2d.ui.Image arrowImage(boolean pointRight) {
        String name = pointRight ? "bs-arrow-right" : "bs-arrow-left";
        com.badlogic.gdx.scenes.scene2d.ui.Image img = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                BsUI.getSkin().getDrawable(name));
        img.setScaling(com.badlogic.gdx.utils.Scaling.contain);
        img.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        return img;
    }
}
