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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane;

/**
 * 双 ScrollPane + 分隔条（简化版：直接用 SplitPane 组合两个 Actor）。
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsSplitPane extends SplitPane {
    public BsSplitPane(Actor first, Actor second, boolean vertical, Skin skin) {
        super(first, second, vertical, skin);
    }
}
