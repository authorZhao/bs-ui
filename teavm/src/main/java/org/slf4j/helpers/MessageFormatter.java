package org.slf4j.helpers;

/**
 * TeaVM 兼容最小 slf4j 替换：{@code {}} 占位符格式化。
 * <p>仅支持 {@code {}}（不支持参数索引 {@code {0}}），匹配项目实际用法。</p>
 */
public final class MessageFormatter {

    private MessageFormatter() {}

    public static String format(String format, Object arg) {
        return arrayFormat(format, new Object[]{arg});
    }

    public static String format(String format, Object arg1, Object arg2) {
        return arrayFormat(format, new Object[]{arg1, arg2});
    }

    public static String arrayFormat(String format, Object[] args) {
        if (format == null) return null;
        if (args == null || args.length == 0) return format;

        StringBuilder sb = new StringBuilder(format.length() + 16);
        int argIdx = 0;
        int i = 0;
        int len = format.length();
        while (i < len) {
            char c = format.charAt(i);
            if (c == '{' && i + 1 < len && format.charAt(i + 1) == '}') {
                if (argIdx < args.length) {
                    sb.append(stringify(args[argIdx++]));
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else if (c == '\\' && i + 1 < len && format.charAt(i + 1) == '{') {
                // 转义的 {
                sb.append('{');
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static String stringify(Object o) {
        if (o == null) return "null";
        if (o.getClass().isArray()) {
            if (o instanceof Object[]) {
                Object[] arr = (Object[]) o;
                StringBuilder s = new StringBuilder("[");
                for (int j = 0; j < arr.length; j++) {
                    if (j > 0) s.append(", ");
                    s.append(arr[j]);
                }
                return s.append(']').toString();
            }
            return o.toString();
        }
        // Throwable 直接 toString（避免打印栈到占位符）
        return o.toString();
    }
}
