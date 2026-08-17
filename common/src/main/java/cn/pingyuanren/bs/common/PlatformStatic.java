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
