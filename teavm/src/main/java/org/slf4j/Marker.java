package org.slf4j;

/**
 * TeaVM 兼容的最小 slf4j 替换实现。
 * <p>
 * 原因：官方 slf4j-api 的 {@link LoggerFactory} 静态初始化依赖
 * {@code ClassLoader.getResources}、{@code SecurityManager.getClassContext}、
 * {@code java.util.concurrent.LinkedBlockingQueue}，这些 JVM 原生 API 在
 * TeaVM（Java→JS/WASM）目标里不存在，导致 {@code LoggerFactory.<clinit>} 直接崩。
 * </p>
 * <p>
 * 本套替换仅实现 Lombok {@code @Slf4j} 生成的传统日志调用路径
 * （{@code trace/debug/info/warn/error} + {@code {}} 占位符），
 * 全部输出到 {@code System.out}/{@code System.err}，无 ServiceLoader、无反射、无并发队列。
 * </p>
 */
public interface Marker {
}
