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

package cn.pingyuanren.bs.test;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import cn.pingyuanren.bs.common.PlatformStatic;
import cn.pingyuanren.bs.common.impl.DeskPlatform;
import cn.pingyuanren.bs.dashboard.DashboardApp;

/**
 * 运维监控大屏启动器。
 * <p>用法（IDEA）：右键 Run；或命令行
 * {@code ./gradlew :lwjgl3:run -PmainClass=cn.pingyuanren.bs.test.DashboardLauncher}。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class DashboardLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("运维监控大屏");
        config.setWindowedMode(1600, 900);
        config.useVsync(true);
        config.setSamples(4);
        config.setIdleFPS(60);
        config.setForegroundFPS(60);   // 大屏要流畅实时动画，不限速到 20
        PlatformStatic.registerImpl(DeskPlatform.class);
        new Lwjgl3Application(new DashboardApp(), config);
    }
}
