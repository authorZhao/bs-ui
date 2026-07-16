package com.git.teavm;


import com.git.bs.common.PlatformStatic;
import com.git.bs.dashboard.DashboardApp;
import com.git.bs.game.BsControlsTestApp;
import com.git.bs.game.BsSkinApp;
import com.git.bs.winsettings.WinSettingsApp;
import com.git.teavm.platform.TeaVmPlatform;
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
        config.width = 1100;
        config.height = 720;
        config.preloadListener = assetLoader -> {
            assetLoader.loadScript("freetype.js");
        };
        new WebApplication(new WinSettingsApp(), config);
    }
    private static void registerPlatform() {
        // 直接 new 实例注入，绕开反射 —— TeaVM wasm-gc 下
        // Class.getConstructors()[0].newInstance() 会 array index out of bounds。
        PlatformStatic.registerInstance(new TeaVmPlatform());
    }
}
