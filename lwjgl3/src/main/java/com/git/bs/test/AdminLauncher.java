package com.git.bs.test;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.git.bs.admin.AdminLoginScreen;
import com.git.bs.common.PlatformStatic;
import com.git.bs.common.impl.DeskPlatform;
import com.git.bs.game.AdminApp;

/**
 * bs-ui 管理后台模板启动器。
 * <p>用法（IDEA）：右键 Run；或命令行
 * {@code ./gradlew :lwjgl3:run -PmainClass=com.git.bs.test.AdminLauncher}。</p>
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
