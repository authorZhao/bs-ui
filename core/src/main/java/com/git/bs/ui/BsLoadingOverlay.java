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

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 风格全屏加载遮罩（Loading Overlay）——
 * 异步操作时显示 spinner + 文字 + 模态遮罩，阻断用户操作。
 *
 * <p>用法：</p>
 * <pre>{@code
 * // 显示
 * BsLoadingOverlay overlay = BsLoadingOverlay.show(stage, skin, "加载中...");
 *
 * // 异步操作完成后关闭
 * overlay.close();
 *
 * // 带进度的版本
 * BsLoadingOverlay overlay = BsLoadingOverlay.show(stage, skin, "上传中", 0.5);
 * overlay.setProgress(0.8);
 * overlay.setText("即将完成...");
 * overlay.close();
 * }</pre>
 *
 * <p>实现：{@code setFillParent(true)} 全屏覆盖，半透明黑色遮罩 + 中央白底圆角卡片
 * （spinner + 文字 + 可选进度条）。modal=true 时阻挡所有点击事件。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsLoadingOverlay extends Table {

    private final BsSpinner spinner;
    private final Label label;
    private final BsProgress progress;
    private final Table card;
    private float autoCloseSec = -1f;
    private float elapsed = 0;
    private boolean showProgress = false;

    public BsLoadingOverlay(Skin skin) {
        // 用 bs-overlay token（主题驱动，暗色主题下也是合适的半透明遮罩）
        setBackground(skin.newDrawable("white", BsTheme.ov()));
        setFillParent(true);
        setTouchable(Touchable.enabled);   // 拦截点击
        center();
        defaults().center();

        // 中央卡片
        card = new Table();
        card.setBackground(skin.getDrawable("bs-window-bg"));
        card.pad(28, 36, 28, 36);
        card.defaults().pad(6).center();

        // spinner
        spinner = new BsSpinner(skin, BsSpinner.Style.BORDER,
                BsPalette.PRIMARY.getMain());
        spinner.setSize(36, 36);
        Container<BsSpinner> spinWrap = new Container<>(spinner);
        spinWrap.size(36, 36);
        card.add(spinWrap).size(36, 36).row();

        // 文字
        label = new Label(BsI18n.get("core.loading", "加载中..."), skin);
        label.setColor(BsTheme.tp());
        card.add(label).padTop(8).row();

        // 进度条（默认隐藏）
        progress = new BsProgress(skin);
        progress.setWidth(180);
        progress.setVisible(false);
        card.add(progress).width(180).height(14).padTop(10).row();

        add(card);
    }

    /** 设置加载文字。 */
    public BsLoadingOverlay setText(String t) {
        label.setText(t == null ? "" : t);
        return this;
    }

    /** 显示进度条并设置当前进度（0~1）。 */
    public BsLoadingOverlay setProgress(float p) {
        if (!showProgress) {
            showProgress = true;
            progress.setVisible(true);
        }
        progress.setProgress(p);
        return this;
    }

    /** 设置自动关闭延迟（秒），<=0 表示不自动关闭。 */
    public BsLoadingOverlay setAutoCloseAfter(float sec) {
        this.autoCloseSec = sec;
        this.elapsed = 0;
        return this;
    }

    /** 显示到 stage。 */
    public BsLoadingOverlay showOn(Stage stage) {
        if (stage == null) return this;
        stage.addActor(this);
        toFront();
        return this;
    }

    /** 关闭（移除）。 */
    public void close() {
        try {
            remove();
        } catch (Throwable t) {
            log.warn("close", t);
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (autoCloseSec > 0) {
            elapsed += delta;
            if (elapsed >= autoCloseSec) {
                close();
                autoCloseSec = -1;
            }
        }
    }

    // ========================= 静态便捷入口 =========================

    public static BsLoadingOverlay show(Stage stage, Skin skin, String text) {
        BsLoadingOverlay o = new BsLoadingOverlay(skin);
        o.setText(text);
        o.showOn(stage);
        return o;
    }

    public static BsLoadingOverlay show(Stage stage, Skin skin, String text, float initialProgress) {
        BsLoadingOverlay o = new BsLoadingOverlay(skin);
        o.setText(text);
        o.setProgress(initialProgress);
        o.showOn(stage);
        return o;
    }

    public static BsLoadingOverlay show(Stage stage, Skin skin, String text, float autoCloseSec, boolean autoClose) {
        BsLoadingOverlay o = new BsLoadingOverlay(skin);
        o.setText(text);
        if (autoClose) o.setAutoCloseAfter(autoCloseSec);
        o.showOn(stage);
        return o;
    }
}
