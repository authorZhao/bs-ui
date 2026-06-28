package org.slf4j;

import org.slf4j.helpers.ConsoleLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * TeaVM 兼容最小 slf4j 替换：{@code LoggerFactory}。
 * <p>
 * <b>不</b>调用 {@code ServiceLoader}、{@code ClassLoader.getResources}、
 * {@code SecurityManager}、{@code LinkedBlockingQueue} 等任何在 TeaVM 目标里
 * 缺失的 JVM 原生 API。直接返回 {@link ConsoleLogger}。
 * </p>
 * <p>
 * 这是替换官方 {@code org.slf4j.LoggerFactory} 类的关键：官方版本静态初始化时会崩。
 * </p>
 */
public class LoggerFactory {

    private static final Map<String, Logger> CACHE = new HashMap<>();

    private LoggerFactory() {}

    public static Logger getLogger(String name) {
        synchronized (CACHE) {
            Logger logger = CACHE.get(name);
            if (logger == null) {
                logger = new ConsoleLogger(name);
                CACHE.put(name, logger);
            }
            return logger;
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz == null ? "null" : clazz.getName());
    }
}
