package org.slf4j.spi;

import org.slf4j.Marker;
import org.slf4j.event.Level;

/**
 * TeaVM 兼容最小 slf4j 替换：fluent API 的占位接口。
 * 项目代码（Lombok @Slf4j 生成的调用）不使用 fluent API，
 * 此接口仅用于让 {@link org.slf4j.Logger} 的 default 方法编译通过。
 */
public interface LoggingEventBuilder {
    LoggingEventBuilder addMarker(Marker marker);
    LoggingEventBuilder setCause(Throwable cause);
    LoggingEventBuilder addArgument(Object p);
    LoggingEventBuilder addArgument(Object[] p);
    LoggingEventBuilder addKeyValue(String key, Object value);
    void log();
    void log(String message);
    void log(String message, Object... args);
    void setCause(Throwable t, Level level);
}
