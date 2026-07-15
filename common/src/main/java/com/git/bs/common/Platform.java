/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库。
 * Copyright (c) 2026 bs-ui contributors
 *
 * 基于 Apache License 2.0 开源，允许商用、修改和再分发。
 * 使用本库的产品须在“关于”界面标注本项目，详见 LICENSE。
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package com.git.bs.common;

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
