package com.git.bs.test;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.git.bs.common.PlatformStatic;
import com.git.bs.common.impl.DeskPlatform;
import com.git.bs.dashboard.PayDashboardApp;

/**
 * 支付订单分析大屏启动器。
 * <p>用法（IDEA）：右键 Run；或命令行
 * {@code ./gradlew :lwjgl3:run -PmainClass=com.git.bs.test.PayDashboardLauncher}。</p>
 *
 * @author authorZhao
 * @since 2026-08-13
 */
public class PayDashboardLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("支付订单分析图");
        config.setWindowedMode(1600, 900);
        config.useVsync(true);
        config.setSamples(4);
        config.setIdleFPS(60);
        config.setForegroundFPS(60);
        PlatformStatic.registerImpl(DeskPlatform.class);
        new Lwjgl3Application(new PayDashboardApp(), config);
    }
}
