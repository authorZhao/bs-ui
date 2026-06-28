package com.git.log.jul;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

public class MyFormatter extends Formatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        // 创建日期格式化对象
        String timestamp = DATE_TIME_FORMATTER.format(LocalDateTime.now());

        // 获取日志级别名称
        String levelName = record.getLevel().getName();
        //懒得设置语言
        switch (levelName){
            case "警告"-> levelName = "warn";
            case "信息" -> levelName = "info";
            case "错误","SEVERE"-> levelName = "error";
        }

        // 获取线程名称
        String threadName = Thread.currentThread().getName();

        // 获取Logger名称（通常是类名）
        String loggerName = record.getLoggerName();

        // 日志消息
        String message = formatMessage(record);
        //%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n
        // 组装最终日志字符串
        if("error".equals(levelName)){

           message += Optional.ofNullable(record.getThrown()).stream()
               .map(Throwable::getStackTrace)
               .flatMap(Arrays::stream)
               .map(StackTraceElement::toString)
                .collect(Collectors.joining(System.lineSeparator()));
        }
        return String.format("%s [%s] [%s] %s - %s%n",
                timestamp, threadName, levelName, loggerName, message);
    }
}
