package com.git.bs.test;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.git.bs.common.impl.DeskPlatform;
import com.git.bs.common.PlatformStatic;
import com.git.bs.game.BsControlsTestApp;
import com.git.bs.demo.BsControlsTestScreen;

/**
 * Bs UI 控件测试启动器。
 * <p>用法（IDEA）：右键 Run；或命令行 {@code ./gradlew :lwjgl3:run -PmainClass=com.git.bs.test.BsControlsLauncher}。</p>
 */
public class BsControlsLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Bs UI 控件测试");
        config.setWindowedMode(BsControlsTestScreen.WIN_W, BsControlsTestScreen.WIN_H);
        config.useVsync(true);
        config.setSamples(4);
        config.setIdleFPS(60);
        config.setForegroundFPS(20);
        PlatformStatic.registerImpl(DeskPlatform.class);
        new Lwjgl3Application(new BsControlsTestApp(), config);
    }
}
