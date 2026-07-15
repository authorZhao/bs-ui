package com.git.bs.test;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.git.bs.common.PlatformStatic;
import com.git.bs.common.impl.DeskPlatform;
import com.git.bs.winsettings.WinSettingsApp;

/**
 * Windows 设置启动器。
 * <p>IDEA 右键 Run；或 {@code ./gradlew :lwjgl3:run -PmainClass=com.git.bs.test.WinSettingsLauncher}。</p>
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
