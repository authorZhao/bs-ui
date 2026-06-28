package com.git.bs.common.impl;


import com.git.bs.common.Platform;
import games.spooky.gdx.nativefilechooser.NativeFileChooser;
import games.spooky.gdx.nativefilechooser.desktop.DesktopFileChooser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author authorZhao
 * @since 2025-08-12
 */
public class DeskPlatform implements Platform {

    public static final ScheduledExecutorService single_t_scheduler = Executors.newScheduledThreadPool(1);
    private static final List<ScheduledFuture<?>> FUTURE_LIST = new ArrayList<>();

    static {
//        if (UIUtils.isWindows) {
//            String s = FileUtil.tmpPath("lib/TaskbarIcon.dll");
//            if (s != null) {
//                System.load(s);
//            }
//        }
    }

    @Override
    public String getPlatformName() {
        return "Desktop-lwjgl3";
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public boolean setWindowIcons(String windowTitle, String iconPath) {
        try {
//            if (UIUtils.isWindows) {
//                String s = FileUtil.tmpPath(iconPath);
//                var result = TaskbarIconJNI.setTaskbarIconByTitle(windowTitle, s);
//                return result > 1;
//            }
        } catch (Exception e) {

        }
        return false;


    }

    @Override
    public String chooseJarFile() {
        return "";
    }

    @Override
    public void scheduleOne(Runnable runnable, long delay, TimeUnit unit) {
        ScheduledFuture<?> schedule = single_t_scheduler.schedule(runnable, delay, unit);
        FUTURE_LIST.add(schedule);
    }

    @Override
    public void schedule(Runnable runnable, long delay, long period, TimeUnit unit) {
        ScheduledFuture<?> schedule = single_t_scheduler.scheduleWithFixedDelay(runnable, delay, period,unit);
        FUTURE_LIST.add(schedule);
    }

    @Override
    public void cancelAll() {
        for (var scheduledFuture : FUTURE_LIST) {
            scheduledFuture.cancel(false);
        }
    }

    @Override
    public Map<String, String> getenv() {
        return System.getenv();
    }

    /**
     * 桌面端系统 dark mode 检测：用 AWT Swing 的 Panel.background 亮度判断。
     * <p>macOS/Windows 都生效（系统主题切换时 Swing 会自动更新 LookAndFeel 的 UIManager 颜色）。
     * Linux 取决于 GTK 主题是否被 Swing 正确识别。</p>
     * <p>headless 环境会抛 HeadlessException，捕获后返回 false。</p>
     */
    @Override
    public boolean isSystemDarkMode() {
        try {
            //java.awt.Color bg = javax.swing.UIManager.getColor("Panel.background");
            //if (bg != null) {
            //    // 用 ITU-R BT.601 luma 公式算亮度，<0.5 视为暗色主题
            //    float brightness = (bg.getRed() * 0.299f
            //            + bg.getGreen() * 0.587f
            //            + bg.getBlue() * 0.114f) / 255f;
            //    return brightness < 0.5f;
            //}
        } catch (Throwable ignored) {
            // headless 或非 desktop 环境，回退 false
        }
        return false;
    }
}
