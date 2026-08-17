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
package cn.pingyuanren.bs.common;

import com.badlogic.gdx.utils.Json;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author authorZhao
 * @since 2025-08-12
 */
public interface Platform {
    /// 获取平台名称
    String getPlatformName();

    void exit();

    boolean setWindowIcons(String windowTitle,String iconPath);

    String chooseJarFile();

    void scheduleOne(Runnable runnable, long delay, TimeUnit unit);

    void schedule(Runnable runnable, long delay, long period, TimeUnit unit);

    void cancelAll();

    Map<String, String> getenv();

    /**
     * 检测系统当前是否处于暗色模式（dark mode）。
     * <p>用于主题跟随系统。各平台实现：
     * <ul>
     *   <li>桌面（LWJGL3）：AWT Panel.background 亮度判断（macOS/Windows 都生效）</li>
     *   <li>Web（TeaVM）：window.matchMedia('(prefers-color-scheme: dark)')</li>
     *   <li>Android/iOS：暂不支持，返回 false</li>
     * </ul>
     * <p>默认实现返回 false（平台不支持时不报错）。
     *
     * @return true 表示系统处于 dark mode；false 表示 light mode 或不支持
     */
    default boolean isSystemDarkMode() {
        return false;
    }

    default  String toJson(Object object) {
        return new Json().toJson(object);
    }

    default <T> T fromJson(String json, Class<T> type){
        return new Json().fromJson(type,json);
    }
}
