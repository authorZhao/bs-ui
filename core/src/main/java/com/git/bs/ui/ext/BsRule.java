package com.git.bs.ui.ext;

import com.git.bs.i18n.BsI18n;

import java.util.function.Function;
import java.util.regex.Pattern;

/// 声明式校验规则，配合 {@link BsFormValidator} 使用。
///
/// 每条规则是一个 `Checker`：传入 {@link BsFormValidator.Context}（可取自身值、也可取其它字段值做跨字段校验），
/// 返回错误消息（不通过）或 `null`（通过）。
///
/// 用法：
/// ```java
/// BsRule.required("请输入用户名"),
/// BsRule.minLen(6),
/// BsRule.email(),
/// BsRule.custom(ctx -> ctx.get("password").equals(ctx.get("confirm")) ? null : "两次密码不一致")
/// ```
public final class BsRule {

    /// 规则检查器：返回错误消息或 `null`。
    @FunctionalInterface
    public interface Checker {
        String check(BsFormValidator.Context ctx);
    }

    private final Checker checker;

    private BsRule(Checker checker) {
        this.checker = checker;
    }

    /// 执行本规则。
    public String check(BsFormValidator.Context ctx) {
        return checker.check(ctx);
    }

    // =================== 内置规则 ===================

    /// 非空（trim 后）。
    public static BsRule required() {
        return required(BsI18n.get("core.rule.required", "必填"));
    }

    /// 非空，自定义错误消息。
    public static BsRule required(String msg) {
        return new BsRule(ctx -> isBlank(ctx.self()) ? msg : null);
    }

    /// 最小长度。
    public static BsRule minLen(int n) {
        return new BsRule(ctx -> {
            int len = ctx.self() == null ? 0 : ctx.self().length();
            return len < n ? BsI18n.get("core.rule.min_length", "至少 {0} 个字符", n) : null;
        });
    }

    /// 最大长度。
    public static BsRule maxLen(int n) {
        return new BsRule(ctx -> {
            int len = ctx.self() == null ? 0 : ctx.self().length();
            return len > n ? BsI18n.get("core.rule.max_length", "最多 {0} 个字符", n) : null;
        });
    }

    /// 正则匹配。
    public static BsRule pattern(String regex, String msg) {
        return pattern(Pattern.compile(regex), msg);
    }

    /// 正则匹配。
    public static BsRule pattern(Pattern p, String msg) {
        return new BsRule(ctx -> isBlank(ctx.self()) || p.matcher(ctx.self()).matches() ? null : msg);
    }

    /// 数值范围（含端点）。非数字或越界则不通过。
    public static BsRule range(double min, double max) {
        return new BsRule(ctx -> {
            String v = ctx.self();
            if (isBlank(v)) return null;   // 空值交给 required 管
            try {
                double d = Double.parseDouble(v);
                return (d < min || d > max) ? BsI18n.get("core.rule.range", "取值范围 {0} ~ {1}", min, max) : null;
            } catch (NumberFormatException e) {
                return BsI18n.get("core.rule.number", "必须是数字");
            }
        });
    }

    /// 邮箱格式。
    public static BsRule email() {
        return pattern("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", BsI18n.get("core.rule.email", "邮箱格式不正确"));
    }

    /// 仅取自身值的自定义规则。
    public static BsRule custom(Function<String, String> fn) {
        return new BsRule(ctx -> fn.apply(ctx.self()));
    }

    /// 完整上下文自定义规则（支持跨字段）。
    public static BsRule crossField(Checker checker) {
        return new BsRule(checker);
    }

    /// 串联多条，遇首条不通过即返回其错误。
    public static BsRule chain(BsRule... rules) {
        return new BsRule(ctx -> {
            for (BsRule r : rules) {
                String e = r.check(ctx);
                if (e != null) return e;
            }
            return null;
        });
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
