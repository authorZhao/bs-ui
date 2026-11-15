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
 * 资源模式策略：使用方实现本接口，在 {@link VolumePakBootstrap#init(VolumePakConfig)}
 * 时传入，决定本次运行走哪种文件方式（散包 / pak / 自动探测）。
 *
 * <p>典型实现读游戏自己的开关（JVM 属性/环境变量/配置文件）——见内置的
 * {@link SysPropsResourceModeResolver}；也可以是 lambda：</p>
 * <pre>{@code
 * VolumePakBootstrap.init(VolumePakConfig.builder()
 *         .modeResolver(() -> debugMode ? ResourceMode.LOOSE : ResourceMode.AUTO)
 *         ...);
 * }</pre>
 *
 * @author authorZhao
 * @since 0.3.7
 */
@FunctionalInterface
public interface ResourceModeResolver {

    /** 恒 AUTO（不关心开关时的默认值）。 */
    ResourceModeResolver ALWAYS_AUTO = () -> ResourceMode.AUTO;

    /** 决定本次运行的资源模式。 */
    ResourceMode resolve();
}
