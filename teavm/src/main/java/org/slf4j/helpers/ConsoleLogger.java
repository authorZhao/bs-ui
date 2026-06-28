package org.slf4j.helpers;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * TeaVM 兼容最小 slf4j 替换：把日志打到 {@code System.out}/{@code System.err}。
 * <p>
 * 不读配置、不开线程、不依赖任何 JVM 原生 API，TeaVM 友好。
 * 级别默认全开（trace/debug 也输出），可在运行时通过 {@link #setEnabled(Level, boolean)}
 * 收口；本项目日志量小，直接全开即可。
 * </p>
 */
public class ConsoleLogger implements Logger {

    private final String name;

    public ConsoleLogger(String name) {
        this.name = name == null ? "null" : name;
    }

    @Override
    public String getName() {
        return name;
    }

    // ---- trace ----
    @Override public boolean isTraceEnabled() { return true; }
    @Override public void trace(String msg) { out("TRACE", msg, null); }
    @Override public void trace(String format, Object arg) { out("TRACE", fmt(format, arg), null); }
    @Override public void trace(String format, Object arg1, Object arg2) { out("TRACE", fmt(format, arg1, arg2), null); }
    @Override public void trace(String format, Object... arguments) { out("TRACE", fmt(format, arguments), null); }
    @Override public void trace(String msg, Throwable t) { out("TRACE", msg, t); }
    @Override public boolean isTraceEnabled(Marker marker) { return true; }
    @Override public void trace(Marker marker, String msg) { out("TRACE", msg, null); }
    @Override public void trace(Marker marker, String format, Object arg) { out("TRACE", fmt(format, arg), null); }
    @Override public void trace(Marker marker, String format, Object arg1, Object arg2) { out("TRACE", fmt(format, arg1, arg2), null); }
    @Override public void trace(Marker marker, String format, Object... argArray) { out("TRACE", fmt(format, argArray), null); }
    @Override public void trace(Marker marker, String msg, Throwable t) { out("TRACE", msg, t); }

    // ---- debug ----
    @Override public boolean isDebugEnabled() { return true; }
    @Override public void debug(String msg) { out("DEBUG", msg, null); }
    @Override public void debug(String format, Object arg) { out("DEBUG", fmt(format, arg), null); }
    @Override public void debug(String format, Object arg1, Object arg2) { out("DEBUG", fmt(format, arg1, arg2), null); }
    @Override public void debug(String format, Object... arguments) { out("DEBUG", fmt(format, arguments), null); }
    @Override public void debug(String msg, Throwable t) { out("DEBUG", msg, t); }
    @Override public boolean isDebugEnabled(Marker marker) { return true; }
    @Override public void debug(Marker marker, String msg) { out("DEBUG", msg, null); }
    @Override public void debug(Marker marker, String format, Object arg) { out("DEBUG", fmt(format, arg), null); }
    @Override public void debug(Marker marker, String format, Object arg1, Object arg2) { out("DEBUG", fmt(format, arg1, arg2), null); }
    @Override public void debug(Marker marker, String format, Object... argArray) { out("DEBUG", fmt(format, argArray), null); }
    @Override public void debug(Marker marker, String msg, Throwable t) { out("DEBUG", msg, t); }

    // ---- info ----
    @Override public boolean isInfoEnabled() { return true; }
    @Override public void info(String msg) { out("INFO", msg, null); }
    @Override public void info(String format, Object arg) { out("INFO", fmt(format, arg), null); }
    @Override public void info(String format, Object arg1, Object arg2) { out("INFO", fmt(format, arg1, arg2), null); }
    @Override public void info(String format, Object... arguments) { out("INFO", fmt(format, arguments), null); }
    @Override public void info(String msg, Throwable t) { out("INFO", msg, t); }
    @Override public boolean isInfoEnabled(Marker marker) { return true; }
    @Override public void info(Marker marker, String msg) { out("INFO", msg, null); }
    @Override public void info(Marker marker, String format, Object arg) { out("INFO", fmt(format, arg), null); }
    @Override public void info(Marker marker, String format, Object arg1, Object arg2) { out("INFO", fmt(format, arg1, arg2), null); }
    @Override public void info(Marker marker, String format, Object... argArray) { out("INFO", fmt(format, argArray), null); }
    @Override public void info(Marker marker, String msg, Throwable t) { out("INFO", msg, t); }

    // ---- warn ----
    @Override public boolean isWarnEnabled() { return true; }
    @Override public void warn(String msg) { err("WARN", msg, null); }
    @Override public void warn(String format, Object arg) { err("WARN", fmt(format, arg), null); }
    @Override public void warn(String format, Object... arguments) { err("WARN", fmt(format, arguments), null); }
    @Override public void warn(String format, Object arg1, Object arg2) { err("WARN", fmt(format, arg1, arg2), null); }
    @Override public void warn(String msg, Throwable t) { err("WARN", msg, t); }
    @Override public boolean isWarnEnabled(Marker marker) { return true; }
    @Override public void warn(Marker marker, String msg) { err("WARN", msg, null); }
    @Override public void warn(Marker marker, String format, Object arg) { err("WARN", fmt(format, arg), null); }
    @Override public void warn(Marker marker, String format, Object arg1, Object arg2) { err("WARN", fmt(format, arg1, arg2), null); }
    @Override public void warn(Marker marker, String format, Object... argArray) { err("WARN", fmt(format, argArray), null); }
    @Override public void warn(Marker marker, String msg, Throwable t) { err("WARN", msg, t); }

    // ---- error ----
    @Override public boolean isErrorEnabled() { return true; }
    @Override public void error(String msg) { err("ERROR", msg, null); }
    @Override public void error(String format, Object arg) { err("ERROR", fmt(format, arg), null); }
    @Override public void error(String format, Object arg1, Object arg2) { err("ERROR", fmt(format, arg1, arg2), null); }
    @Override public void error(String format, Object... arguments) { err("ERROR", fmt(format, arguments), null); }
    @Override public void error(String msg, Throwable t) { err("ERROR", msg, t); }
    @Override public boolean isErrorEnabled(Marker marker) { return true; }
    @Override public void error(Marker marker, String msg) { err("ERROR", msg, null); }
    @Override public void error(Marker marker, String format, Object arg) { err("ERROR", fmt(format, arg), null); }
    @Override public void error(Marker marker, String format, Object arg1, Object arg2) { err("ERROR", fmt(format, arg1, arg2), null); }
    @Override public void error(Marker marker, String format, Object... argArray) { err("ERROR", fmt(format, argArray), null); }
    @Override public void error(Marker marker, String msg, Throwable t) { err("ERROR", msg, t); }

    private static String fmt(String format, Object arg) {
        return MessageFormatter.format(format, arg);
    }

    private static String fmt(String format, Object arg1, Object arg2) {
        return MessageFormatter.format(format, arg1, arg2);
    }

    private static String fmt(String format, Object[] args) {
        return MessageFormatter.arrayFormat(format, args);
    }

    private void out(String level, String msg, Throwable t) {
        System.out.println("[" + level + "] " + name + " - " + msg);
        if (t != null) t.printStackTrace(System.out);
    }

    private void err(String level, String msg, Throwable t) {
        System.err.println("[" + level + "] " + name + " - " + msg);
        if (t != null) t.printStackTrace(System.err);
    }
}
