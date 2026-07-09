package com.git.bs.i18n;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * bs-ui 国际化门面（静态，仿 {@code BsUI} 主题门面风格）。
 *
 * <p><b>设计目标</b>：简单实用。所有 UI 文案通过 key 取，找不到 key 返回 key 本身（绝不 NPE），
 * 支持带参文案（{@code {0}{1}} 占位，走 {@link String#format}）。语言切换触发监听器，
 * App 级监听器负责重建当前 Screen（仿 {@code BsUI.setTheme} 模式）。</p>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * // 启动时（App.create 里，紧跟 BsUI.init()）
 * BsI18n.init();                 // 默认加载 zh_cn
 * BsI18n.init("en_us");          // 或指定初始语言
 *
 * // 取文案
 * BsI18n.get("btn.ok")                  → "确定"
 * BsI18n.get("table.page_info", 100, 1, 10)  → "共 100 条，第 1/10 页"
 *
 * // 切语言（触发 UI 重建）
 * BsI18n.setLocale("en_us");
 *
 * // 监听切换（App 级，重建 Screen）
 * BsI18n.addListener(() -> Gdx.app.postRunnable(() -> rebuildScreen()));
 * }</pre>
 *
 * <h3>语言文件</h3>
 * <p>classpath {@code com/git/bs/i18n/{locale}.json}，扁平 key-value：
 * <pre>{@code
 * {
 *   "btn.ok": "确定",
 *   "table.page_info": "共 {0} 条，第 {1}/{2} 页"
 * }
 * }</pre>
 * 用 libgdx {@link Json} 解析（不加依赖，core 已可用）。不依赖 Platform.toJson/fromJson
 * （DeskPlatform 用 fastjson2 + UnquoteFieldName 输出非标准 JSON，不适合手写语言文件）。</p>
 *
 * <h3>占位符约定</h3>
 * <p>用 {@code {0}{1}} 风格，内部转成 {@code %s}/{@code %d} 走 {@link String#format}。
 * 带参文案写法：{@code "共 {0} 条"} → {@code get("key", 100)}。</p>
 *
 * <p><b>fallback 策略</b>：key 不存在 → 返回 key；locale 文件不存在 → 保留空 map（get 全返回 key），
 * 不抛异常，保证 UI 不崩。</p>
 */
public final class BsI18n {

    /** core 自带翻译包的 classpath 目录（btn/alert/dialog/table 等通用 key）。 */
    private static final String CORE_BUNDLE = "com/git/bs/i18n/";
    /** 默认语言。 */
    private static final String DEFAULT_LOCALE = "zh_cn";

    private static String currentLocale = DEFAULT_LOCALE;
    /** 当前语言的全部文案（找不到 key 时用 key 本身 fallback，所以空 map 也安全）。 */
    private static final Map<String, String> messages = new LinkedHashMap<>();
    /**
     * 已注册的翻译包 classpath 目录列表（不含 core 自带的）。
     * 业务模块（如 demo）调 {@link #addBundle} 注册自己的翻译目录，
     * loadLocale 时按"core → 业务（注册顺序）"依次加载合并，后者覆盖前者。
     */
    private static final java.util.List<String> extraBundles = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** 语言切换监听器（App 级，负责重建 Screen）。 */
    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private static boolean initialized = false;

    private BsI18n() {}

    /** 初始化，加载默认语言 {@code zh_cn}。在 {@code App.create()} 里紧跟 {@code BsUI.init()} 调一次。 */
    public static void init() {
        init(DEFAULT_LOCALE);
    }

    /**
     * 初始化并加载指定语言。
     * @param locale 语言代码，如 {@code "zh_cn"} / {@code "en_us"}（对应 {@code {locale}.json}）
     */
    public static synchronized void init(String locale) {
        currentLocale = locale == null ? DEFAULT_LOCALE : locale;
        loadLocale(currentLocale);
        initialized = true;
    }

    /**
     * 注册一个额外的翻译包（业务模块用）。
     *
     * <p><b>用途</b>：bs-ui core 自带的翻译只包含通用 key（btn/alert/dialog 等）。
     * 业务模块（如 demo 的 winsettings）有自己的文案（nav.home/system.display 等），
     * 调本方法注册业务翻译目录后，{@link #loadLocale} 会同时加载 core 和业务包，
     * 业务 key 覆盖 core 同名 key。</p>
     *
     * <p><b>调用时机</b>：在 {@link #init} <b>之前</b>调（init 时才会首次加载）；
     * 如果已 init，调用后会立即重载当前 locale 生效。</p>
     *
     * @param classpathDir classpath 目录路径，如 {@code "com/git/bs/demo/i18n/"}，
     *                     目录下放 {@code zh_cn.json} / {@code en_us.json} 等
     */
    public static synchronized void addBundle(String classpathDir) {
        if (classpathDir == null || classpathDir.isEmpty()) return;
        if (!extraBundles.contains(classpathDir)) extraBundles.add(classpathDir);
        // 已 init 则立即重载，让新 bundle 生效
        if (initialized) loadLocale(currentLocale);
    }

    /**
     * 程序化注册翻译（不依赖文件）。适合测试、或少量动态文案。
     * @param locale 目标语言
     * @param kv key → 文案
     */
    public static synchronized void register(String locale, Map<String, String> kv) {
        if (locale == null || kv == null) return;
        if (locale.equals(currentLocale)) {
            messages.putAll(kv);
        }
        // 注意：非当前 locale 的 register 不持久化（本方法是即时注入当前 map 的简化设计）
    }

    /** 当前语言代码（如 {@code "zh_cn"}）。 */
    public static String currentLocale() {
        return currentLocale;
    }

    /** 当前语言对应的 Java {@link java.util.Locale}（日历/日期组件用）。zh_cn→{@code Locale.CHINA} 等。 */
    public static java.util.Locale javaLocale() {
        switch (currentLocale) {
            case "en_us": return java.util.Locale.US;
            case "ja_jp": return java.util.Locale.JAPAN;
            case "zh_cn":
            default:     return java.util.Locale.CHINA;
        }
    }

    /**
     * 取文案。key 不存在时返回 key 本身（绝不返回 null，绝不抛异常）。
     * @param key 点分 key，如 {@code "btn.ok"}
     */
    public static String get(String key) {
        ensureInit();
        String v = messages.get(key);
        if (v == null) {
            // key 缺失不崩，但打日志方便发现漏配（stderr，不依赖 slf4j，避免 core 早期初始化顺序问题）
            System.err.println("[BsI18n] missing key: " + key + " (locale=" + currentLocale + ")");
            return key;
        }
        return v;
    }

    /**
     * 取文案，带默认值（core 库组件用）。
     *
     * <p><b>用途</b>：bs-ui core 组件（BsDialog/BsEmpty/BsSearchBar 等）内置默认中文文案，
     * 即使外部应用没配 json 也能正常显示。调用方传默认值（通常就是原硬编码中文），
     * json 里有对应 key 就覆盖默认值，没有就用默认值 —— 这样 core 库可独立工作，
     * 业务可选地在自己的 json 里覆盖 core 文案。</p>
     *
     * <p>例：{@code BsI18n.get("core.empty", "暂无数据")} —— zh_cn/en_us 没配则显示"暂无数据"，
     * 配了则用配置值。不打印 missing key 日志（因为有默认值不算 missing）。</p>
     *
     * @param key 点分 key；可为 null（直接返回 defaultValue）
     * @param defaultValue key 不存在时的兜底文案；为 null 时回退到 key 本身
     */
    public static String get(String key, String defaultValue) {
        if (key == null) return defaultValue != null ? defaultValue : "";
        ensureInit();
        String v = messages.get(key);
        if (v != null) return v;
        // 有默认值 → 静默用默认值（core 组件的内置文案，不算 missing）
        if (defaultValue != null) return defaultValue;
        // 默认值也没给 → 回退到 key
        return key;
    }

    /**
     * 取带默认值的文案并填充参数。占位符 {@code {0}{1}} 走 {@link String#format}。
     * @param key 点分 key；null 时直接用 defaultValue 模板
     * @param defaultValue key 不存在时的兜底模板（含 {@code {0}{1}} 占位）
     * @param args 参数
     */
    public static String get(String key, String defaultValue, Object... args) {
        String template = get(key, defaultValue);
        if (args == null || args.length == 0) return template;
        try {
            return String.format(toFormat(template, args.length), args);
        } catch (Throwable t) {
            return template;
        }
    }

    /**
     * 取文案并填充参数。占位符用 {@code {0}{1}} 风格，内部转 {@code %s} 走 {@link String#format}。
     * <p>例：文案 {@code "共 {0} 条，第 {1}/{2} 页"}，调用 {@code get("k", 100, 1, 10)} →
     * {@code "共 100 条，第 1/10 页"}。</p>
     * @param key 点分 key
     * @param args 参数（按 {@code {0}{1}...} 顺序）
     */
    public static String get(String key, Object... args) {
        String template = get(key);
        if (args == null || args.length == 0) return template;
        // {0} → %s, {1} → %s ... 然后 String.format
        String fmt = toFormat(template, args.length);
        try {
            return String.format(fmt, args);
        } catch (Throwable t) {
            // 格式化失败（参数不匹配等）→ 返回模板原文，不崩
            return template;
        }
    }

    /**
     * 切换语言。加载新语言文案后触发所有监听器。App 级监听器收到后应 {@code postRunnable} 重建 Screen。
     * @param locale 语言代码，如 {@code "en_us"}
     */
    public static synchronized void setLocale(String locale) {
        if (locale == null || locale.equals(currentLocale)) return;
        currentLocale = locale;
        loadLocale(locale);
        // 触发监听器（App 负责 postRunnable + setScreen 重建）
        for (Runnable l : listeners) {
            try { l.run(); } catch (Throwable ignored) {}
        }
    }

    /** 注册语言切换监听器（仿 {@code BsUI.addOnThemeChangeListener}）。 */
    public static void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    /** 移除监听器。 */
    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    // =================== 内部 ===================

    private static void ensureInit() {
        if (!initialized) {
            // 懒兜底：未 init 时静默加载默认语言，保证 get() 不崩
            init(DEFAULT_LOCALE);
        }
    }

    /**
     * 从 classpath 加载各 bundle 的 {locale}.json 合并到 messages map。
     * 顺序：core 默认包 → 业务包（addBundle 注册顺序，后者覆盖前者）。
     * 任何文件不存在/解析失败 → 跳过该文件（不崩，get 走 fallback 返回 key）。
     */
    private static void loadLocale(String locale) {
        messages.clear();
        // 1. core 自带包
        loadBundleFile(CORE_BUNDLE, locale);
        // 2. 业务包（按注册顺序，后者覆盖前者）
        for (String dir : extraBundles) {
            loadBundleFile(dir, locale);
        }
    }

    /** 加载单个 bundle 的 {locale}.json 到 messages。文件不存在/解析失败 → 静默跳过。 */
    @SuppressWarnings("unchecked")
    private static void loadBundleFile(String dir, String locale) {
        try {
            String path = dir + locale + ".json";
            var fh = Gdx.files.internal(path);
            if (!fh.exists()) return;
            String text = fh.readString("UTF-8");
            Json json = new Json();
            ObjectMap<String, String> map = json.fromJson(ObjectMap.class, text);
            if (map != null) {
                for (ObjectMap.Entry<String, String> e : map.entries()) {
                    messages.put(e.key, e.value);
                }
            }
        } catch (Throwable ignored) {
            // 单个 bundle 加载失败不影响其他 bundle
        }
    }

    /**
     * 把 {@code {0}{1}} 占位转成 {@code %s} 格式串（用于 {@link String#format}）。
     * 先转义已有的 {@code %} 为 {@code %%}（避免文案里的百分号被误判），再替换占位符。
     * 参数个数不足时多余的 {@code {n}} 原样保留。
     */
    private static String toFormat(String template, int argCount) {
        // 注意：先转义 %，再做 {n}→%s 替换。顺序反了会把新插入的 %s 也转义掉。
        String result = template.replace("%", "%%");
        for (int i = 0; i < argCount; i++) {
            result = result.replace("{" + i + "}", "%s");
        }
        return result;
    }
}
