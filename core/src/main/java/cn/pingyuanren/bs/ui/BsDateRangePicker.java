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

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

/// 日期区间选择器（DateRangePicker）：只读输入框，点击弹出 `BsCalendar`（RANGE 模式），
/// 选齐起止日期后回填 `yyyy-MM-dd ~ yyyy-MM-dd`。
///
/// 弥补 `BsDatePicker`（单值）的不足，用于订单/日志等区间筛选场景。
///
/// 用法：
/// ```java
/// BsDateRangePicker rp = new BsDateRangePicker(skin)
///         .setOnChange((start, end) -> setStatus(start + " ~ " + end));
/// rp.setRange(LocalDate.now().minusDays(7), LocalDate.now());
/// ```
///
/// 实现：继承 `BsTextField`（只读），点击弹出浮层（backdrop 收外点 + Esc 关闭），
/// 内含一个 `BsCalendar` RANGE 模式；起止选齐触发回调并关闭。
/// `BsDatePicker(skin, true)` 已覆盖"日期+时间"，本组件聚焦"纯日期区间"。
///
/// v1 不含：双月并排、快捷选项（今天/近7天/近30天）、时间部分。
/// @author authorZhao
/// @since 2026-07-16
@Slf4j
public class BsDateRangePicker extends BsTextField {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private LocalDate start;
    private LocalDate end;
    private BiConsumer<LocalDate, LocalDate> onChange;

    // 浮层状态
    private Actor backdrop;
    private Table popupRoot;
    private boolean open;

    public BsDateRangePicker(Skin skin) {
        super("", skin);
        setTextFieldFilter((field, c) -> false);   // 只读，由浮层选择回填
        addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { openPopup(); }
        });
    }

    public BsDateRangePicker setRange(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
        refreshText();
        return this;
    }

    public LocalDate getStart() { return start; }

    public LocalDate getEnd() { return end; }

    public BsDateRangePicker setOnChange(BiConsumer<LocalDate, LocalDate> c) {
        this.onChange = c;
        return this;
    }

    private void refreshText() {
        String s = start != null ? start.format(FMT) : "";
        String e = end != null ? end.format(FMT) : "";
        setTextProgrammatic(s.isEmpty() && e.isEmpty() ? "" : s + " ~ " + e);
    }

    private void openPopup() {
        if (open) {
            closePopup();
            return;
        }
        Stage stage = getStage();
        if (stage == null) return;
        Skin skin = BsUI.getSkin();

        BsCalendar calendar = new BsCalendar(skin, BsCalendar.Mode.RANGE);
        if (start != null) {
            calendar.setRange(start, end);
        } else {
            calendar.setMonth(YearMonth.now());
        }
        calendar.setOnRange((s, e) -> {
            // 起止选齐才回填关闭；只选了起点时保持浮层打开，允许跨月选终点
            if (s != null && e != null) {
                start = s;
                end = e;
                refreshText();
                if (onChange != null) {
                    try { onChange.accept(s, e); } catch (Throwable t) { log.warn("BsDateRangePicker onChange error", t); }
                }
                closePopup();
            }
        });

        popupRoot = new Table();
        popupRoot.setBackground(skin.getDrawable("bs-window-bg"));
        popupRoot.setTouchable(Touchable.enabled);
        popupRoot.add(calendar).pad(6);
        popupRoot.pack();

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

        // 定位：默认在输入框下方，空间不够翻上方
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

    private void closePopup() {
        if (!open) return;
        if (backdrop != null) { backdrop.remove(); backdrop = null; }
        if (popupRoot != null) { popupRoot.remove(); popupRoot = null; }
        open = false;
    }
}
