package org.slf4j.event;

/** TeaVM 兼容最小 slf4j 替换：日志级别枚举。 */
public enum Level {
    ERROR(40, "ERROR"),
    WARN(30, "WARN"),
    INFO(20, "INFO"),
    DEBUG(10, "DEBUG"),
    TRACE(0, "TRACE");

    private final int levelInt;
    private final String levelStr;

    Level(int i, String s) {
        this.levelInt = i;
        this.levelStr = s;
    }

    public int toInt() {
        return levelInt;
    }

    public String toString() {
        return levelStr;
    }
}
