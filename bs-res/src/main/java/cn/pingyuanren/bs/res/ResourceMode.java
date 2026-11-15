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
package cn.pingyuanren.bs.res;

/**
 * 资源模式三态（{@link VolumePakBootstrap} 启动判定用）。
 *
 * @author authorZhao
 * @since 0.3.7
 */
public enum ResourceMode {

    /** 强制散包：跳过 pak 安装，即使 assets 下有索引也走明文磁盘（开发态改散资源立即生效）。 */
    LOOSE,

    /** 强制 pak：探测不到索引/分卷时 warn 并回退明文磁盘（校验 pak 链路问题时用）。 */
    PAK,

    /** 自动探测：探测到索引即 pak，否则明文磁盘（默认）。 */
    AUTO
}
