/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库。
 * Copyright (c) 2026 bs-ui contributors
 *
 * 基于 Apache License 2.0 开源，允许商用、修改和再分发。
 * 使用本库的产品须在“关于”界面标注本项目，详见 LICENSE。
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Bootstrap 风格日期（+时间）选择器：只读 TextField，点击时弹出 {@link BsDatePickerPopup}。
 *
 * <p>两种模式：</p>
 * <ul>
 *   <li>{@code new BsDatePicker(skin)} —— 仅日期，回显 {@code yyyy-MM-dd}，回调返回 {@link LocalDate}</li>
 *   <li>{@code new BsDatePicker(skin, true)} —— 含时间（精确到秒），回显 {@code yyyy-MM-dd HH:mm:ss}，
 *       用 {@link #getDateTime()} 取 {@link LocalDateTime}</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsDatePicker picker = new BsDatePicker(skin, true); // 含时间
 * picker.setValue(LocalDateTime.now());
 * picker.setOnChange(dt -> setStatus("选了: " + dt));
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsDatePicker extends BsTextField {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final boolean withTime;
    private LocalDateTime value;          // 始终用 LocalDateTime 内部存储，纯日期模式时 time=00:00:00
    private Consumer<LocalDateTime> onChange;
    private BsDatePickerPopup popup;

    public BsDatePicker(Skin skin) {
        this(skin, false);
    }

    public BsDatePicker(Skin skin, boolean withTime) {
        super("", skin);
        this.withTime = withTime;
        // 拒绝所有键盘输入（只读，由 popup 选择日期回填）
        setTextFieldFilter((field, c) -> false);
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openPopup();
            }
        });
    }

    public boolean isWithTime() { return withTime; }

    /** 仅日期模式设置初始值。 */
    public void setValue(LocalDate date) {
        this.value = date != null ? date.atStartOfDay() : null;
        refreshText();
    }

    /** 设置完整日期+时间值（任何模式都可用）。 */
    public void setValue(LocalDateTime dt) {
        this.value = dt;
        refreshText();
    }

    private void refreshText() {
        if (value == null) { setTextProgrammatic(""); return; }
        setTextProgrammatic(withTime ? value.format(DATETIME_FMT) : value.toLocalDate().format(DATE_FMT));
    }

    /** 仅日期模式取值。含时间模式时返回 date 部分（time 部分用 {@link #getDateTime()}）。 */
    public LocalDate getValue() {
        return value != null ? value.toLocalDate() : null;
    }

    /** 含时间模式取完整值；仅日期模式返回 date+00:00:00。 */
    public LocalDateTime getDateTime() {
        return value;
    }

    public String getValueAsIsoString() {
        if (value == null) return "";
        return withTime ? value.format(DATETIME_FMT) : value.toLocalDate().format(DATE_FMT);
    }

    public void setOnChange(Consumer<LocalDateTime> onChange) {
        this.onChange = onChange;
    }

    private void openPopup() {
        if (popup != null && popup.isOpen()) { popup.close(); return; }
        if (getStage() == null) return;
        popup = new BsDatePickerPopup(BsUI.getSkin(), withTime);
        LocalDateTime init = value != null ? value : LocalDateTime.now();
        popup.setOnPick(dt -> {
            setValue(dt);
            if (onChange != null) {
                try { onChange.accept(dt); } catch (Throwable t) { log.warn("onChange error", t); }
            }
        });
        popup.show(getStage(), this, init);
    }
}
