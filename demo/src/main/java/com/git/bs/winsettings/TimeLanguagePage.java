package com.git.bs.winsettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import com.git.bs.ui.BsDatePicker;
import com.git.bs.ui.BsToast;
import com.git.bs.ui.BsTimePicker;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间和语言页（按 Win11 真实结构）。
 *
 * <p>设置组：日期和时间 / 语言和区域 / 打字 / 语音 / 中文输入法。</p>
 *
 * <p><b>Windows 显示语言</b>：真正驱动 i18n 切换。选中后调 {@link BsI18n#setLocale}，
 * App 监听器会重建 screen，所有文案变新语言。</p>
 *
 * <p><b>演示</b>：日期时间组里的「设置日期 / 设置时间」用真正的 {@link BsDatePicker}（含时间）
 * 和 {@link BsTimePicker}，点击弹出选择面板，选完回显。点击「立即同步」按钮 → 自动取系统当前
 * {@link LocalDateTime#now()}，刷新两个选择器的显示 + 弹 Toast 反馈。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class TimeLanguagePage extends CategoryPage {

    /** 显示语言选项：显示文案 → locale code。日本語暂不支持 json，选中后 fallback 到 key 不崩。 */
    private static final String[][] DISPLAY_LANGUAGES = {
            {"中文(简体, 中国)",       "zh_cn"},
            {"English (United States)", "en_us"},
            {"日本語 (日本)",           "ja_jp"},
    };

    private static final DateTimeFormatter SYNC_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 日期+时间选择器（点击弹日历面板，选完回显 yyyy-MM-dd HH:mm:ss）。 */
    private final BsDatePicker dateTimePicker;
    /** 仅时间选择器（点击弹时:分:秒步进面板）。 */
    private final BsTimePicker timePicker;

    public TimeLanguagePage(Skin skin) {
        super(BsI18n.get("nav.timelanguage"), skin);

        // 创建选择器：初始值用当前系统时间，让回显真实可信
        LocalDateTime now = LocalDateTime.now();
        dateTimePicker = new BsDatePicker(skin, true);
        dateTimePicker.setValue(now);
        dateTimePicker.setOnChange(dt -> log.info("[时间语言] 用户调整日期时间: {}", dt));

        timePicker = new BsTimePicker(skin);
        timePicker.setValue(now.toLocalTime());
        timePicker.setOnChange(t -> log.info("[时间语言] 用户调整时间: {}", t));

        group(BsI18n.get("timelang.group_date_time"),
                SettingItem.toggle(BsI18n.get("timelang.set_time_automatically"), "", true),
                SettingItem.toggle(BsI18n.get("timelang.set_timezone_automatically"), "", true),
                SettingItem.select(BsI18n.get("timelang.timezone"), "", new String[]{BsI18n.get("timelang.tz_beijing"), BsI18n.get("timelang.tz_tokyo"), BsI18n.get("timelang.tz_london"), BsI18n.get("timelang.tz_pacific")}, BsI18n.get("timelang.tz_beijing")),
                // 真正的日期+时间选择器（CUSTOM 控件，Container 固定宽度避免文本截断）
                SettingItem.custom(BsI18n.get("timelang.set_date"), BsI18n.get("timelang.current_time"),
                        () -> fixedWidth(dateTimePicker, 230f)),
                SettingItem.custom(BsI18n.get("timelang.set_time"), "",
                        () -> fixedWidth(timePicker, 110f)),
                // 点击「立即同步」→ 取系统当前时间，刷新两个选择器 + 弹 Toast 回显
                SettingItem.button(BsI18n.get("timelang.sync_now"), BsI18n.get("timelang.sync_now_desc"), BsI18n.get("timelang.sync"),
                        () -> syncNow()),
                SettingItem.toggle(BsI18n.get("timelang.dst_auto_adjust"), "", false)
        );

        // Windows 显示语言：真正切换 i18n。选中 → BsI18n.setLocale → App 重建 screen
        group(BsI18n.get("timelang.group_language_region"),
                SettingItem.select(BsI18n.get("timelang.windows_display_language"), "", displayLangLabels(), currentDisplayLangLabel(),
                        selected -> {
                            String locale = localeOfDisplayLang(selected);
                            log.info("切换 Windows 显示语言: {} ({})", selected, locale);
                            BsI18n.setLocale(locale);
                        }),
                SettingItem.button(BsI18n.get("timelang.preferred_languages"), BsI18n.get("timelang.preferred_languages_desc"), BsI18n.get("timelang.manage")),
                SettingItem.select(BsI18n.get("timelang.country_region"), BsI18n.get("timelang.country_region_desc"), new String[]{BsI18n.get("timelang.country_china"), BsI18n.get("timelang.country_us"), BsI18n.get("timelang.country_japan"), BsI18n.get("timelang.country_uk"), BsI18n.get("timelang.country_korea")}, BsI18n.get("timelang.country_china")),
                SettingItem.select(BsI18n.get("timelang.regional_format"), BsI18n.get("timelang.regional_format_desc"), new String[]{BsI18n.get("timelang.format_chinese_cn"), "English (United States)", "日本語(日本)"}, BsI18n.get("timelang.format_chinese_cn"))
        );

        group(BsI18n.get("timelang.group_typing"),
                SettingItem.toggle(BsI18n.get("timelang.autocorrect"), BsI18n.get("timelang.autocorrect_desc"), true),
                SettingItem.toggle(BsI18n.get("timelang.autospell"), BsI18n.get("timelang.autospell_desc"), true),
                SettingItem.toggle(BsI18n.get("timelang.ime_auto_learning"), BsI18n.get("timelang.ime_auto_learning_desc"), true),
                SettingItem.toggle(BsI18n.get("timelang.touch_keyboard_typing"), "", true),
                SettingItem.button(BsI18n.get("timelang.advanced_keyboard"), BsI18n.get("timelang.advanced_keyboard_desc"), BsI18n.get("common.open"))
        );

        group(BsI18n.get("timelang.group_speech"),
                SettingItem.toggle(BsI18n.get("timelang.online_speech_recognition"), BsI18n.get("timelang.online_speech_recognition_desc"), true),
                SettingItem.select(BsI18n.get("timelang.speech_language"), "", new String[]{BsI18n.get("timelang.speech_chinese"), BsI18n.get("timelang.speech_english")}, BsI18n.get("timelang.speech_chinese")),
                SettingItem.toggle(BsI18n.get("timelang.microphone_access"), BsI18n.get("timelang.microphone_access_desc"), true),
                SettingItem.link(BsI18n.get("timelang.speech_privacy"), BsI18n.get("timelang.speech_privacy_desc"), BsI18n.get("timelang.manage"))
        );

        group(BsI18n.get("timelang.group_chinese_ime"),
                SettingItem.value(BsI18n.get("timelang.ms_pinyin"), "", BsI18n.get("common.enabled")),
                SettingItem.value(BsI18n.get("timelang.ms_wubi"), "", BsI18n.get("common.disabled")),
                SettingItem.select(BsI18n.get("timelang.default_input_mode"), BsI18n.get("timelang.default_input_mode_desc"), new String[]{BsI18n.get("timelang.chinese"), BsI18n.get("timelang.english")}, BsI18n.get("timelang.chinese")),
                SettingItem.select(BsI18n.get("timelang.default_punctuation"), BsI18n.get("timelang.default_punctuation_desc"), new String[]{BsI18n.get("timelang.chinese_punctuation"), BsI18n.get("timelang.english_punctuation")}, BsI18n.get("timelang.chinese_punctuation")),
                SettingItem.toggle(BsI18n.get("timelang.simplified_traditional_toggle"), "Ctrl + Shift + F", true),
                SettingItem.button(BsI18n.get("timelang.ime_options"), BsI18n.get("timelang.ime_options_desc"), BsI18n.get("timelang.options"))
        );
    }

    /**
     * 点击「立即同步」：取系统当前 {@link LocalDateTime#now()}，刷新日期/时间两个选择器，
     * 弹 Toast 回显同步后的时间。
     */
    private void syncNow() {
        LocalDateTime now = LocalDateTime.now();
        // 刷新两个 picker 的显示（picker 内部 value + 文本回填）
        dateTimePicker.setValue(now);
        timePicker.setValue(now.toLocalTime());
        String formatted = now.format(SYNC_FMT);
        log.info("[时间语言] 立即同步 → {}", formatted);

        Stage stage = currentStage();
        if (stage != null) {
            BsToast.show(stage, skin,
                    BsI18n.get("demo.sync_success", formatted),
                    BsToast.Variant.SUCCESS, 3.5f);
        }
    }

    /** 拿当前 stage：WinSettingsScreen.show() 里 {@code Gdx.input.setInputProcessor(stage)}。 */
    private static Stage currentStage() {
        try {
            Object ip = Gdx.input.getInputProcessor();
            if (ip instanceof Stage) return (Stage) ip;
        } catch (Throwable ignored) {}
        return null;
    }

    /** 把 actor 包到固定宽度的 Container 里（解决 TextField 默认 prefWidth 偏窄文本截断的问题）。 */
    private static Container<com.badlogic.gdx.scenes.scene2d.Actor> fixedWidth(
            com.badlogic.gdx.scenes.scene2d.Actor a, float w) {
        Container<com.badlogic.gdx.scenes.scene2d.Actor> c = new Container<>(a);
        c.width(w);
        return c;
    }

    // =================== 显示语言工具 ===================

    /** 所有显示语言选项的文案数组（给 SelectBox.setItems 用）。 */
    private static String[] displayLangLabels() {
        String[] a = new String[DISPLAY_LANGUAGES.length];
        for (int i = 0; i < DISPLAY_LANGUAGES.length; i++) a[i] = DISPLAY_LANGUAGES[i][0];
        return a;
    }

    /** 当前 locale 对应的显示文案（给 SelectBox 初始选中用）。找不到回退到第一个。 */
    private static String currentDisplayLangLabel() {
        String loc = BsI18n.currentLocale();
        for (String[] e : DISPLAY_LANGUAGES) {
            if (e[1].equals(loc)) return e[0];
        }
        return DISPLAY_LANGUAGES[0][0];
    }

    /** 显示文案 → locale code。找不到回退到 zh_cn。 */
    private static String localeOfDisplayLang(String label) {
        if (label == null) return "zh_cn";
        for (String[] e : DISPLAY_LANGUAGES) {
            if (e[0].equals(label)) return e[1];
        }
        return "zh_cn";
    }
}
