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

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/// 时间选择器（TimePicker）：只读输入框，点击弹出 `时:分(:秒)` 面板。
///
/// 每个单位用 `▲`/`▼` 步进按钮 + 可输入的 2 位数字框——**不依赖滚轮**，
/// 无键盘（触屏）也能操作，有键盘可直接输入。
///
/// 用法：
/// ```java
/// BsTimePicker tp = new BsTimePicker(skin)
///         .setOnChange(t -> setStatus("选了 " + t));
/// tp.setValue(LocalTime.now());
/// ```
///
/// 实现：继承 {@link BsTextField}（只读），点击弹出浮层（backdrop 收外点 + Esc 关闭）；
/// 步进/输入即时生效，点"确定"提交。`withSeconds=false` 时只精确到分钟。
/// @author authorZhao
/// @since 2026-07-16
@Slf4j
public class BsTimePicker extends BsTextField {

    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final boolean withSeconds;
    private LocalTime value;
    private Consumer<LocalTime> onChange;

    private Actor backdrop;
    private Table popupRoot;
    private boolean open;
    private BsTextField hourF, minF, secF;

    public BsTimePicker(Skin skin) {
        this(skin, true);
    }

    public BsTimePicker(Skin skin, boolean withSeconds) {
        super("", skin);
        this.withSeconds = withSeconds;
        setTextFieldFilter((field, c) -> false);   // 只读，由浮层回填
        addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { openPopup(); }
        });
    }

    public BsTimePicker setValue(LocalTime t) {
        this.value = t;
        setTextProgrammatic(t == null ? "" : (withSeconds ? t.format(HMS) : t.format(HM)));
        return this;
    }

    public LocalTime getValue() { return value; }

    public BsTimePicker setOnChange(Consumer<LocalTime> c) {
        this.onChange = c;
        return this;
    }

    private void openPopup() {
        if (open) {
            closePopup();
            return;
        }
        Stage stage = getStage();
        if (stage == null) return;
        Skin skin = BsUI.getSkin();
        LocalTime init = value != null ? value : LocalTime.now();

        popupRoot = new Table();
        popupRoot.setBackground(skin.getDrawable("bs-window-bg"));
        popupRoot.setTouchable(Touchable.enabled);

        Table row = new Table();
        row.pad(8);
        hourF = addUnit(row, init.getHour(), 23, skin);
        row.add(colon(skin)).pad(3);
        minF = addUnit(row, init.getMinute(), 59, skin);
        if (withSeconds) {
            row.add(colon(skin)).pad(3);
            secF = addUnit(row, init.getSecond(), 59, skin);
        }
        popupRoot.add(row).row();

        // 快捷 + 确定
        Table bar = new Table();
        TextButton now = new TextButton(BsI18n.get("core.time.now", "现在"), skin, "bs-btn-secondary");
        TextButton ok = new TextButton(BsI18n.get("btn.ok", "确定"), skin, "bs-btn-primary");
        now.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                LocalTime n = LocalTime.now();
                hourF.setText(String.format("%02d", n.getHour()));
                minF.setText(String.format("%02d", n.getMinute()));
                if (secF != null) secF.setText(String.format("%02d", n.getSecond()));
            }
        });
        ok.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { confirm(); }
        });
        bar.add(now).growX().padRight(6);
        bar.add(ok).growX();
        popupRoot.add(bar).growX().pad(8);
        popupRoot.pack();

        backdrop = makeBackdrop(stage);
        positionBelow(popupRoot, stage);
        stage.addActor(backdrop);
        stage.addActor(popupRoot);
        stage.setKeyboardFocus(backdrop);
        open = true;
    }

    /// 构建一个单位列：上箭头 / 输入框 / 下箭头。返回输入框。
    private BsTextField addUnit(Table parent, int initVal, int max, Skin skin) {
        Table col = new Table();
        BsTextField f = new BsTextField(String.format("%02d", initVal), skin);
        f.setTextFieldFilter((field, c) -> Character.isDigit(c));
        f.setMaxLength(2);
        // 上下箭头用 skin 的 bs-arrow-up/down drawable（程序化三角，不依赖字体字符，
        // 在 light/dark 主题下都是主色，对比稳定）
        com.badlogic.gdx.scenes.scene2d.ui.Image up = arrowImage(true);
        com.badlogic.gdx.scenes.scene2d.ui.Image down = arrowImage(false);
        up.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { step(f, max, 1); }
        });
        down.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { step(f, max, -1); }
        });
        col.add(up).size(24, 16).pad(1).row();
        col.add(f).width(44).pad(2).row();
        col.add(down).size(24, 16).pad(1);
        parent.add(col);
        return f;
    }

    /** 步进箭头：用 skin 的 bs-arrow-up/down drawable（主题主色，多主题对比稳定）。 */
    private com.badlogic.gdx.scenes.scene2d.ui.Image arrowImage(boolean pointUp) {
        String name = pointUp ? "bs-arrow-up" : "bs-arrow-down";
        com.badlogic.gdx.scenes.scene2d.ui.Image img = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                BsUI.getSkin().getDrawable(name));
        img.setScaling(com.badlogic.gdx.utils.Scaling.contain);
        img.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        return img;
    }

    private void step(BsTextField f, int max, int delta) {
        int cur = parseClamp(f.getText(), 0, max, 0);
        int range = max + 1;
        int v = ((cur + delta) % range + range) % range;   // 带负数安全的回绕
        f.setText(String.format("%02d", v));
        syncValue(true);   // 实时同步到外层输入框 + 触发回调
    }

    private static com.badlogic.gdx.scenes.scene2d.ui.Label colon(Skin skin) {
        com.badlogic.gdx.scenes.scene2d.ui.Label l = new com.badlogic.gdx.scenes.scene2d.ui.Label(":", skin);
        l.setColor(BsTheme.tp());
        return l;
    }

    private void confirm() {
        syncValue(true);
        closePopup();
    }

    /// 把浮层内三个输入框的值同步到 value + 外层输入框；fireCallback=true 时触发 onChange。
    private void syncValue(boolean fireCallback) {
        if (hourF == null) return;   // 浮层未打开
        int h = parseClamp(hourF.getText(), 0, 23, value != null ? value.getHour() : 0);
        int m = parseClamp(minF.getText(), 0, 59, value != null ? value.getMinute() : 0);
        int s = withSeconds ? parseClamp(secF.getText(), 0, 59, value != null ? value.getSecond() : 0) : 0;
        LocalTime t = LocalTime.of(h, m, s);
        value = t;
        setTextProgrammatic(withSeconds ? t.format(HMS) : t.format(HM));
        if (fireCallback && onChange != null) {
            try { onChange.accept(t); } catch (Throwable ex) { log.warn("BsTimePicker onChange error", ex); }
        }
    }

    private static int parseClamp(String s, int min, int max, int fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try {
            int v = Integer.parseInt(s);
            return (v < min || v > max) ? fallback : v;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Actor makeBackdrop(Stage stage) {
        Actor b = new Actor();
        b.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        b.setTouchable(Touchable.enabled);
        b.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                closePopup();
                return true;
            }
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) { closePopup(); return true; }
                return false;
            }
        });
        return b;
    }

    private void positionBelow(Table pop, Stage stage) {
        Vector2 pos = localToStageCoordinates(new Vector2(0, 0));
        float x = pos.x;
        float y = pos.y - pop.getHeight();
        if (y < 0) y = pos.y + getHeight();
        if (x + pop.getWidth() > stage.getWidth()) x = stage.getWidth() - pop.getWidth();
        if (x < 0) x = 0;
        pop.setPosition(x, y);
    }

    private void closePopup() {
        if (!open) return;
        syncValue(false);   // 兜底：关闭前把浮层内当前值同步到外层输入框（不重复触发回调）
        if (backdrop != null) { backdrop.remove(); backdrop = null; }
        if (popupRoot != null) { popupRoot.remove(); popupRoot = null; }
        hourF = minF = secF = null;
        open = false;
    }
}
