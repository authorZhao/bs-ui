package com.git.bs.test;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.git.bs.common.PlatformStatic;
import com.git.bs.common.impl.DeskPlatform;
import com.git.bs.dashboard.DashboardApp;

/**
 * 运维监控大屏启动器。
 * <p>用法（IDEA）：右键 Run；或命令行
 * {@code ./gradlew :lwjgl3:run -PmainClass=com.git.bs.test.DashboardLauncher}。</p>
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
