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

package cn.pingyuanren.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * 所有设置页基类（主页 / 各分类页）。buildView 由调用方（WinSettingsScreen）塞进内容区。
 * @author authorZhao
 * @since 2026-07-16
 */
public abstract class SettingsPage {

    protected final Skin skin;

    protected SettingsPage(Skin skin) {
        this.skin = skin;
    }

    /** 构建页面根 Actor。Router 用于主页卡片点击跳转。 */
    public abstract Actor buildView(Router router);
}
