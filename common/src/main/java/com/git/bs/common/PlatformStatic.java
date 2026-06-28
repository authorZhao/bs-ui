package com.git.bs.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author authorZhao
 * @since 2025-08-12
 */
public class PlatformStatic {
    private static Map<Class<?>,?> classObjectMap = new HashMap<>();
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
    public static void registerInstance(Platform instance){
        platform = instance;
    }

    public static <T extends Platform> void registerImpl(Class<T> clazz){
        platformClazz = clazz;
    }

    public static Platform getPlatform(){
        try {
            if (platform == null && platformClazz != null) {
                // fallback：仅在未通过 registerInstance 注入时走反射。
                // TeaVM wasm-gc 下此分支会失败，Launcher 应使用 registerInstance。
                platform = (Platform) platformClazz.getConstructors()[0].newInstance();
            }
            return platform;
        }catch (Exception e){
            System.err.println("getPlatform error");
            throw new RuntimeException();
        }
    }
}
