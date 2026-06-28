package org.slf4j;

import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * TeaVM 兼容最小 slf4j 替换：{@code Logger} 接口。
 * <p>
 * 仅实现 Lombok {@code @Slf4j} 生成的传统调用路径；fluent API（atInfo/atLevel 等）
 * 返回 NOP builder（{@link #nopBuilder()}），项目代码不使用 fluent API。
 * </p>
 */
public interface Logger {
    String ROOT_LOGGER_NAME = "ROOT";

    String getName();

    // ---- fluent API（项目不用，保留接口契约，返回 null 即可）----
    default LoggingEventBuilder makeLoggingEventBuilder(Level level) { return null; }
    default LoggingEventBuilder atLevel(Level level) { return null; }
    default boolean isEnabledForLevel(Level level) { return false; }
    default LoggingEventBuilder atTrace() { return null; }
    default LoggingEventBuilder atDebug() { return null; }
    default LoggingEventBuilder atInfo() { return null; }
    default LoggingEventBuilder atWarn() { return null; }
    default LoggingEventBuilder atError() { return null; }

    // ---- trace ----
    boolean isTraceEnabled();
    void trace(String msg);
    void trace(String format, Object arg);
    void trace(String format, Object arg1, Object arg2);
    void trace(String format, Object... arguments);
    void trace(String msg, Throwable t);
    boolean isTraceEnabled(Marker marker);
    void trace(Marker marker, String msg);
    void trace(Marker marker, String format, Object arg);
    void trace(Marker marker, String format, Object arg1, Object arg2);
    void trace(Marker marker, String format, Object... argArray);
    void trace(Marker marker, String msg, Throwable t);

    // ---- debug ----
    boolean isDebugEnabled();
    void debug(String msg);
    void debug(String format, Object arg);
    void debug(String format, Object arg1, Object arg2);
    void debug(String format, Object... arguments);
    void debug(String msg, Throwable t);
    boolean isDebugEnabled(Marker marker);
    void debug(Marker marker, String msg);
    void debug(Marker marker, String format, Object arg);
    void debug(Marker marker, String format, Object arg1, Object arg2);
    void debug(Marker marker, String format, Object... argArray);
    void debug(Marker marker, String msg, Throwable t);

    // ---- info ----
    boolean isInfoEnabled();
    void info(String msg);
    void info(String format, Object arg);
    void info(String format, Object arg1, Object arg2);
    void info(String format, Object... arguments);
    void info(String msg, Throwable t);
    boolean isInfoEnabled(Marker marker);
    void info(Marker marker, String msg);
    void info(Marker marker, String format, Object arg);
    void info(Marker marker, String format, Object arg1, Object arg2);
    void info(Marker marker, String format, Object... argArray);
    void info(Marker marker, String msg, Throwable t);

    // ---- warn ----
    boolean isWarnEnabled();
    void warn(String msg);
    void warn(String format, Object arg);
    void warn(String format, Object... arguments);
    void warn(String format, Object arg1, Object arg2);
    void warn(String msg, Throwable t);
    boolean isWarnEnabled(Marker marker);
    void warn(Marker marker, String msg);
    void warn(Marker marker, String format, Object arg);
    void warn(Marker marker, String format, Object arg1, Object arg2);
    void warn(Marker marker, String format, Object... argArray);
    void warn(Marker marker, String msg, Throwable t);

    // ---- error ----
    boolean isErrorEnabled();
    void error(String msg);
    void error(String format, Object arg);
    void error(String format, Object arg1, Object arg2);
    void error(String format, Object... arguments);
    void error(String msg, Throwable t);
    boolean isErrorEnabled(Marker marker);
    void error(Marker marker, String msg);
    void error(Marker marker, String format, Object arg);
    void error(Marker marker, String format, Object arg1, Object arg2);
    void error(Marker marker, String format, Object... argArray);
    void error(Marker marker, String msg, Throwable t);
}
