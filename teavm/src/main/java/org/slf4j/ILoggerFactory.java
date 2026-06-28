package org.slf4j;

/** TeaVM 兼容最小 slf4j 替换：Logger 工厂接口。 */
public interface ILoggerFactory {
    Logger getLogger(String name);
}
