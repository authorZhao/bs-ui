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

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author authorZhao
 * @since 2025-08-12
 */
@Slf4j
public class PlatformStatic {
    private static final Map<Class<?>, Object> CLASS_OBJECT_MAP = new HashMap<>();
    private static Class<? extends Platform> platformClazz;
    private static Platform platform;

    /**
     * 直接注入 Platform 实例，绕过反射实例化。
     * <p>
     * TeaVM wasm-gc target 下 {@code clazz.getConstructors()[0].newInstance()} 会
     * array index out of bounds（getConstructors 反射元数据未被完整生成），
     * 因此各 Launcher 应优先调用 {@link #registerInstance(Platform)} 直接注入实例。
     * </p>
     */
    public static void registerInstance(Platform instance) {
        platform = instance;
    }

    public static <T extends Platform> void registerImpl(Class<T> clazz) {
        platformClazz = clazz;
    }

    public static Platform getPlatform() {
        try {
            if (platform == null && platformClazz != null) {
                // fallback：仅在未通过 registerInstance 注入时走反射。
                // TeaVM wasm-gc 下此分支会失败，Launcher 应使用 registerInstance。
                platform = (Platform) platformClazz.getConstructors()[0].newInstance();
            }
            return platform;
        } catch (Exception e) {
            System.err.println("getPlatform error");
            throw new RuntimeException();
        }
    }

    public static <T> T getInstance(Class<T> clazz) {
        try {
            Object o = CLASS_OBJECT_MAP.get(clazz);


            if (o == null) {
                o = CLASS_OBJECT_MAP.values().stream().filter(i -> clazz.isAssignableFrom(i.getClass())).findFirst().orElse(null);
                if (o != null) {
                    CLASS_OBJECT_MAP.put(clazz, o);
                }
            }

            return clazz.cast(o);
        } catch (Exception e) {
            System.err.println("getPlatform error");
            throw new RuntimeException();
        }
    }

    public static <T> void setInstance(T t) {
        try {
            CLASS_OBJECT_MAP.put(t.getClass(), t);
        } catch (Exception e) {
            System.err.println("getPlatform error");
            throw new RuntimeException();
        }
    }
}
