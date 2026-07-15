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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.Getter;
import lombok.Setter;

/**
 * Bootstrap 5 风格窗口：标题栏 + 拖拽 + 模态遮罩。
 * <p>模态行为：showModal 时盖一层半透明 backdrop；本窗口被 remove 时一并移除 backdrop，
 * 避免遮罩残留导致 stage 无法操作（之前版本的 bug）。backdrop 点击 → 触发 onClose（可关）。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsWindow extends Window {

    @Getter
    private boolean modal;

    /** 模态时创建的 backdrop（remove 时一并移除）。 */
    private Table backdrop;

    /** backdrop 点击时的回调（默认 = 关闭本窗口）。设为 null 表示禁用点击关闭。 */
    @Setter
    private Runnable onCloseClick = this::close;

    public BsWindow(String title, Skin skin) {
        this(title, skin, false);
    }

    public BsWindow(String title, Skin skin, boolean modal) {
        super(title, skin, "default");
        this.modal = modal;
        setMovable(true);
        setKeepWithinStage(true);
        // libGDX Window 标题画在 padTop 区域内（紧贴内容区上方）。
        // padTop 太小：标题 ascender 顶出窗口上边界；
        // padTop 太大：标题与内容区间距过宽。
        // 14 是平衡点：标题完整落在框内，且与内容间距自然（接近 BsModal 的 14）。
        pad(18, 8, 8, 8);
    }

    /** 模态显示：盖 backdrop + 居中加到 stage。重复调用安全（先清理旧 backdrop）。 */
    public void showModal(Stage stage) {
        if (!modal) {
            stage.addActor(this);
            pack();
            centerOn(stage);
            return;
        }
        // 已有 backdrop 则先清掉（防御重复调用）
        removeBackdrop();
        backdrop = new Table(getSkin());
        // 用 bs-overlay token（主题驱动，与 BsModal 的 backdrop 保持一致）
        backdrop.setBackground(getSkin().newDrawable("white", BsTheme.ov()));
        backdrop.setFillParent(true);
        backdrop.setTouchable(Touchable.enabled);
        // backdrop 点击 → 关闭窗口（Bootstrap 5 模态默认行为）
        backdrop.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // 只处理点在 backdrop 自身（非冒泡到 backdrop 的子事件）
                if (event.getTarget() == backdrop && onCloseClick != null) {
                    onCloseClick.run();
                }
            }
        });
        stage.addActor(backdrop);
        stage.addActor(this);
        pack();
        centerOn(stage);
        toFront();
    }

    private void centerOn(Stage stage) {
        setPosition(
                Math.round((stage.getWidth() - getWidth()) / 2f),
                Math.round((stage.getHeight() - getHeight()) / 2f));
    }

    /** 关闭：从 stage 移除本窗口 + backdrop。 */
    public void close() {
        remove();
    }

    @Override
    public boolean remove() {
        boolean r = super.remove();
        removeBackdrop();
        return r;
    }

    private void removeBackdrop() {
        if (backdrop != null) {
            backdrop.remove();
            backdrop = null;
        }
    }

    /** 运行时切换模态属性（仅在下次 showModal 生效）。 */
    public void setModal(boolean modal) {
        this.modal = modal;
    }

    /** backdrop 是否当前存在于 stage 上。 */
    public boolean isBackdropShown() {
        return backdrop != null && backdrop.getStage() != null;
    }

    /** 忽略未使用 import 警告（保留 Actor / Touchable 备用）。 */
    @SuppressWarnings("unused")
    private void unusedRefs() { Actor a = null; Touchable t = Touchable.enabled; }
}
