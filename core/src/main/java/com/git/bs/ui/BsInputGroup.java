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
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * Bootstrap 5 风格输入组（Input group）—— 在输入框前后追加图标/文字/按钮。
 *
 * <p>常见场景：</p>
 * <ul>
 *   <li>前缀：@ 用户名、¥ 金额、https:// 网址</li>
 *   <li>后缀：.com 邮箱、单位 kg/cm、搜索按钮</li>
 *   <li>前后组合：[¥] [金额] [.00]</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * // 简单前缀文字
 * BsInputGroup g1 = new BsInputGroup(skin)
 *         .prependText("@")
 *         .field(new BsTextField("", skin));
 *
 * // 前缀图标 + 后缀按钮
 * BsInputGroup g2 = new BsInputGroup(skin)
 *         .prependIcon(BsIcon.get("envelope"))
 *         .field(new BsTextField("", skin))
 *         .appendButton("发送", () -> setStatus("发送"), BsButton.Variant.PRIMARY);
 *
 * stage.addActor(g2);
 * }</pre>
 *
 * <p>实现：Table 横向 = [prefix addons] [field] [suffix addons]。
 * 前缀/后缀的容器底色用浅灰（bs-text-field-bg 染灰），与输入框边框贴合无缝。
 * field 用 cell.padLeft(-1)/padRight(-1) 让边框贴合。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsInputGroup extends Table {

    private final Table prefixWrap;
    private final Table suffixWrap;
    private TextField field;
    private float addonHeight = 32;

    public BsInputGroup(Skin skin) {
        left();
        prefixWrap = new Table();
        prefixWrap.left();
        suffixWrap = new Table();
        suffixWrap.left();
    }

    /** 重建布局（每次 addXxx 后调用）。 */
    private void relayout() {
        clearChildren();
        if (prefixWrap.getChildren().size > 0) {
            add(prefixWrap).height(addonHeight).padRight(-1);
        }
        if (field != null) {
            // 若没有前/后缀，普通 add；否则 padLeft/Right 设 -1 让边框贴合
            if (prefixWrap.getChildren().size > 0) {
                add(field).height(addonHeight).padLeft(-1).padRight(-1);
            } else {
                add(field).height(addonHeight).padRight(suffixWrap.getChildren().size > 0 ? -1 : 0);
            }
        }
        if (suffixWrap.getChildren().size > 0) {
            add(suffixWrap).height(addonHeight);
        }
    }

    // ========================= 前缀 =========================

    /** 前缀文字（如 @ / ¥ / https://）。 */
    public BsInputGroup prependText(String text) {
        prefixWrap.add(makeTextAddon(text)).padRight(-1);
        relayout();
        return this;
    }

    /** 前缀图标（如 envelope / user）。 */
    public BsInputGroup prependIcon(Drawable icon) {
        prefixWrap.add(makeIconAddon(icon)).padRight(-1);
        relayout();
        return this;
    }

    /** 前缀按钮（带点击回调）。 */
    public BsInputGroup prependButton(String label, Runnable onClick, BsButton.Variant variant) {
        prefixWrap.add(makeButtonAddon(label, onClick, variant)).padRight(-1);
        relayout();
        return this;
    }

    // ========================= 后缀 =========================

    /** 后缀文字（如 .com / kg）。 */
    public BsInputGroup appendText(String text) {
        suffixWrap.add(makeTextAddon(text)).padLeft(-1);
        relayout();
        return this;
    }

    /** 后缀图标。 */
    public BsInputGroup appendIcon(Drawable icon) {
        suffixWrap.add(makeIconAddon(icon)).padLeft(-1);
        relayout();
        return this;
    }

    /** 后缀按钮（带点击回调）。 */
    public BsInputGroup appendButton(String label, Runnable onClick, BsButton.Variant variant) {
        suffixWrap.add(makeButtonAddon(label, onClick, variant)).padLeft(-1);
        relayout();
        return this;
    }

    /** 设置输入框（必填）。 */
    public BsInputGroup field(TextField field) {
        this.field = field;
        relayout();
        return this;
    }

    public TextField getField() { return field; }

    public BsInputGroup setAddonHeight(float h) {
        this.addonHeight = h;
        relayout();
        return this;
    }

    // ========================= addon 构造 =========================

    /** 文字 addon：浅灰底 + 深灰字 + 居中。 */
    private Actor makeTextAddon(String text) {
        Skin skin = BsUI.getSkin();
        Label l = new Label(text, skin);
        l.setColor(BsTheme.ts());
        Container<Label> c = new Container<>(l);
        c.setBackground(skin.getDrawable("bs-menu-title-up"));   // 浅灰底
        c.pad(0, 10, 0, 10);
        c.fill();
        return c;
    }

    /** 图标 addon：浅灰底 + 居中图标。 */
    private Actor makeIconAddon(Drawable icon) {
        Image img = new Image(icon);
        img.setScaling(Scaling.fit);
        Container<Image> c = new Container<>(img);
        c.setBackground(BsUI.getSkin().getDrawable("bs-menu-title-up"));
        c.pad(0, 8, 0, 8);
        c.fill();
        return c;
    }

    /** 按钮 addon：BsButton（SM），保持原配色。 */
    private Actor makeButtonAddon(String label, Runnable onClick, BsButton.Variant variant) {
        BsButton btn = new BsButton(label, BsUI.getSkin(), variant, BsButton.Style.SOLID, BsButton.Size.SM);
        if (onClick != null) {
            btn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override
                public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                    try { onClick.run(); } catch (Throwable t) {}
                    return true;
                }
            });
        }
        return btn;
    }
}
