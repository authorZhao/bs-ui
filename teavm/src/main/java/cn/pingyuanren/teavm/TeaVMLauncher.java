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

package cn.pingyuanren.teavm;


import cn.pingyuanren.bs.common.PlatformStatic;
import cn.pingyuanren.bs.dashboard.DashboardApp;
import cn.pingyuanren.bs.game.BsControlsTestApp;
import cn.pingyuanren.bs.game.BsSkinApp;
import cn.pingyuanren.bs.winsettings.WinSettingsApp;
import cn.pingyuanren.teavm.platform.TeaVmPlatform;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;

/**
 * Launches the TeaVM/HTML application.
 */
public class TeaVMLauncher {
    public static void main(String[] args) {
        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        registerPlatform();
        //// If width and height are each greater than 0, then the app will use a fixed size.
        //config.width = 640;
        //config.height = 480;
        //// If width and height are both 0, then the app will use all available space.
        config.width = 1280;
        config.height = 800;
        config.preloadListener = assetLoader -> {
            assetLoader.loadScript("freetype.js");
        };
        new WebApplication(new BsSkinApp(), config);
    }
    private static void registerPlatform() {
        // 直接 new 实例注入，绕开反射 —— TeaVM wasm-gc 下
        // Class.getConstructors()[0].newInstance() 会 array index out of bounds。
        PlatformStatic.registerInstance(new TeaVmPlatform());
    }
}
