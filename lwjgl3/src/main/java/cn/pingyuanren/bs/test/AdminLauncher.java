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
import cn.pingyuanren.bs.admin.AdminLoginScreen;
import cn.pingyuanren.bs.common.PlatformStatic;
import cn.pingyuanren.bs.common.impl.DeskPlatform;
import cn.pingyuanren.bs.game.AdminApp;

/**
 * bs-ui 管理后台模板启动器。
 * <p>用法（IDEA）：右键 Run；或命令行
 * {@code ./gradlew :lwjgl3:run -PmainClass=cn.pingyuanren.bs.test.AdminLauncher}。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class AdminLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("bs-ui 管理后台模板");
        config.setWindowedMode(AdminLoginScreen.WIN_W, AdminLoginScreen.WIN_H);
        config.useVsync(true);
        config.setSamples(4);
        config.setIdleFPS(60);
        config.setForegroundFPS(20);
        config.setWindowIcon("icons/logo.png");
        PlatformStatic.registerImpl(DeskPlatform.class);
        new Lwjgl3Application(new AdminApp(), config);
    }
}
