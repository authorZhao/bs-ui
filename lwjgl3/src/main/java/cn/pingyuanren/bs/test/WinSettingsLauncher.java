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
import cn.pingyuanren.bs.winsettings.WinSettingsApp;

/**
 * Windows 设置启动器。
 * <p>IDEA 右键 Run；或 {@code ./gradlew :lwjgl3:run -PmainClass=cn.pingyuanren.bs.test.WinSettingsLauncher}。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class WinSettingsLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Windows 设置 (bs-ui)");
        config.setWindowedMode(1100, 720);
        config.useVsync(true);
        config.setSamples(4);
        config.setIdleFPS(30);
        config.setForegroundFPS(30);
        PlatformStatic.registerImpl(DeskPlatform.class);
        new Lwjgl3Application(new WinSettingsApp(), config);
    }
}
