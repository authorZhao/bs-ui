package com.git.bs.ui.ext;

import com.git.bs.ui.BsTextField;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/// 声明式表单校验引擎（Form 增强，独立于 `BsForm`，不修改既有类）。
///
/// 弥补 `BsForm` 仅支持命令式 `Function` 校验的不足：声明式 {@link BsRule} 规则、
/// 跨字段联动、异步校验。与 UI 解耦——只返回错误 map，由调用方决定如何展示。
///
/// 用法：
/// ```java
/// BsFormValidator v = new BsFormValidator();
/// v.addField("user", userField::getText, BsRule.required("请输入用户名"), BsRule.minLen(3));
/// v.addField("email", emailField, BsRule.email());
/// v.addField("pwd", pwdField::getText, BsRule.required());
/// v.addField("confirm", confirmField::getText,
///         BsRule.crossField(ctx -> ctx.get("pwd").equals(ctx.self()) ? null : "两次密码不一致"));
///
/// // 同步校验
/// Map<String, String> errors = v.validateAll();
/// if (errors.isEmpty()) submit();
///
/// // 异步校验（如查重），checker 须在 GL 线程回调 consumer
/// v.addAsyncRule("user", (val, onResult) -> backend.checkUser(val, ok -> onResult.accept(ok ? null : "用户名已存在")));
/// v.validateAsync(errors -> { if (errors.isEmpty()) submit(); });
/// ```
///
/// 说明：异步 checker 自行决定线程，**回调 consumer 必须在 GL 线程**（用 `Gdx.app.postRunnable`）。
/// `validateAsync` 先跑同步规则，同步不通过的字段不触发其异步；全部异步完成后回调。
@Slf4j
public final class BsFormValidator {

    /// 校验上下文：取自身值或其它字段当前值（跨字段用）。
    public static final class Context {
        private final Map<String, String> values;
        private final String selfKey;

        Context(Map<String, String> values, String selfKey) {
            this.values = values;
            this.selfKey = selfKey;
        }

        /// 当前字段值（不会为 null，空值返回 ""）。
        public String self() {
            return values.getOrDefault(selfKey, "");
        }

        /// 取其它字段当前值（跨字段校验）。
        public String get(String key) {
            return values.getOrDefault(key, "");
        }
    }

    private final Map<String, Supplier<String>> getters = new LinkedHashMap<>();
    private final Map<String, List<BsRule>> rules = new LinkedHashMap<>();
    private final Map<String, BiConsumer<String, Consumer<String>>> async = new LinkedHashMap<>();

    /** 注册一个字段：key + 取值器 + 规则。 */
    public BsFormValidator addField(String key, Supplier<String> getter, BsRule... rs) {
        getters.put(key, getter);
        List<BsRule> list = new ArrayList<>();
        for (BsRule r : rs) {
            if (r != null) list.add(r);
        }
        rules.put(key, list);
        return this;
    }

    /** 便捷重载：直接传 `BsTextField`（取值器用其 `getText`）。 */
    public BsFormValidator addField(String key, BsTextField tf, BsRule... rs) {
        return addField(key, tf == null ? () -> "" : tf::getText, rs);
    }

    /** 注册异步规则：checker 取值，完成后在 GL 线程回调 `onResult`（null=通过，非 null=错误消息）。 */
    public BsFormValidator addAsyncRule(String key, BiConsumer<String, Consumer<String>> checker) {
        if (checker != null) {
            async.put(key, checker);
        }
        return this;
    }

    /// 同步校验全部，返回 `key → 错误消息`（通过的字段不含；全通过则空 map）。
    public Map<String, String> validateAll() {
        Map<String, String> values = gather();
        Map<String, String> errors = new LinkedHashMap<>();
        for (String key : getters.keySet()) {
            Context ctx = new Context(values, key);
            for (BsRule r : rules.getOrDefault(key, Collections.emptyList())) {
                String err;
                try {
                    err = r.check(ctx);
                } catch (Throwable t) {
                    log.warn("BsFormValidator rule error on {}: {}", key, t.toString());
                    err = "校验异常";
                }
                if (err != null) {
                    errors.put(key, err);
                    break;
                }
            }
        }
        return errors;
    }

    /** 同步是否全通过。 */
    public boolean isValid() {
        return validateAll().isEmpty();
    }

    /// 先跑同步规则，同步通过且注册了异步的字段再触发异步；
    /// 全部异步完成后回调最终 errors（含同步错误 + 异步错误）。
    public void validateAsync(Consumer<Map<String, String>> callback) {
        Map<String, String> errors = validateAll();
        List<String> pending = new ArrayList<>();
        for (String key : async.keySet()) {
            if (!errors.containsKey(key)) {
                pending.add(key);
            }
        }
        if (pending.isEmpty()) {
            fire(callback, errors);
            return;
        }
        Map<String, String> values = gather();
        final int[] remaining = { pending.size() };
        for (String key : pending) {
            async.get(key).accept(values.getOrDefault(key, ""), err -> {
                if (err != null && !err.isEmpty()) {
                    errors.put(key, err);
                }
                remaining[0]--;
                if (remaining[0] == 0) {
                    fire(callback, errors);
                }
            });
        }
    }

    private void fire(Consumer<Map<String, String>> callback, Map<String, String> errors) {
        if (callback != null) {
            try { callback.accept(errors); } catch (Throwable t) { log.warn("BsFormValidator callback error", t); }
        }
    }

    private Map<String, String> gather() {
        Map<String, String> v = new LinkedHashMap<>();
        for (Map.Entry<String, Supplier<String>> e : getters.entrySet()) {
            try {
                v.put(e.getKey(), e.getValue().get());
            } catch (Throwable t) {
                log.warn("BsFormValidator getter error on {}: {}", e.getKey(), t.toString());
                v.put(e.getKey(), "");
            }
        }
        return v;
    }
}
