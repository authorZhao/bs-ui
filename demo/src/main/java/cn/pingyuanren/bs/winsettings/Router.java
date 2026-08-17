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

/**
 * 页面路由接口：主页卡片 / 导航项点击后跳转到指定分类页。
 * @author authorZhao
 * @since 2026-07-16
 */
public interface Router {
    /** 跳转到 key 对应的页面（home/system/bluetooth/...）。 */
    void navigate(String key);
}
