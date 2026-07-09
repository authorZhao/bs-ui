package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 辅助功能页（按 Win11 真实结构）。
 *
 * <p>设置组：视觉效果 / 文本大小 / 讲述人 / 放大镜 / 颜色过滤器 / 对比度主题 /
 * 键盘 / 鼠标与光标 / 眼睛控制。</p>
 */
@Slf4j
public class AccessibilityPage extends CategoryPage {

    public AccessibilityPage(Skin skin) {
        super(BsI18n.get("nav.accessibility"), skin);

        group(BsI18n.get("accessibility.group_visual_effects"),
                SettingItem.toggle(BsI18n.get("accessibility.always_show_scrollbar"), "", true),
                SettingItem.toggle(BsI18n.get("accessibility.transparency"), "", true),
                SettingItem.toggle(BsI18n.get("accessibility.turn_off_animations"), BsI18n.get("accessibility.turn_off_animations_desc"), false),
                SettingItem.select(BsI18n.get("accessibility.notification_duration"), "", new String[]{BsI18n.get("common.5min"), BsI18n.get("common.10min"), BsI18n.get("common.15min")}, BsI18n.get("common.5min"))
        );

        group(BsI18n.get("accessibility.group_text_size"),
                SettingItem.select(BsI18n.get("accessibility.text_size"), BsI18n.get("accessibility.text_size_desc"), new String[]{"100%", "120%", "150%", "200%"}, "100%"),
                SettingItem.value(BsI18n.get("accessibility.description"), "", BsI18n.get("accessibility.preview_text")),
                SettingItem.button(BsI18n.get("accessibility.apply"), BsI18n.get("accessibility.apply_text_size"), BsI18n.get("accessibility.apply"))
        );

        group(BsI18n.get("accessibility.group_narrator"),
                SettingItem.toggle(BsI18n.get("accessibility.narrator"), BsI18n.get("accessibility.narrator_desc"), false),
                SettingItem.toggle(BsI18n.get("accessibility.auto_start_narrator"), "", false),
                SettingItem.select(BsI18n.get("accessibility.narrator_voice"), "", new String[]{BsI18n.get("accessibility.voice_huihui"), "English (David)", "English (Zira)"}, BsI18n.get("accessibility.voice_huihui")),
                SettingItem.select(BsI18n.get("accessibility.verbosity"), "", new String[]{BsI18n.get("accessibility.verbosity_min"), BsI18n.get("accessibility.verbosity_normal"), BsI18n.get("accessibility.verbosity_detailed")}, BsI18n.get("accessibility.verbosity_normal")),
                SettingItem.toggle(BsI18n.get("accessibility.emphasize_bold"), "", false),
                SettingItem.link(BsI18n.get("accessibility.narrator_home"), "", BsI18n.get("accessibility.view"))
        );

        group(BsI18n.get("accessibility.group_magnifier"),
                SettingItem.toggle(BsI18n.get("accessibility.magnifier"), BsI18n.get("accessibility.magnifier_desc"), false),
                SettingItem.select(BsI18n.get("accessibility.zoom_level"), "", new String[]{"100%", "150%", "200%", "300%", "400%"}, "200%"),
                SettingItem.select(BsI18n.get("accessibility.magnifier_mode"), "", new String[]{BsI18n.get("accessibility.mode_full_screen"), BsI18n.get("accessibility.mode_lens"), BsI18n.get("accessibility.mode_docked")}, BsI18n.get("accessibility.mode_full_screen")),
                SettingItem.toggle(BsI18n.get("accessibility.magnifier_invert_colors"), "", false)
        );

        group(BsI18n.get("accessibility.group_color_filters"),
                SettingItem.toggle(BsI18n.get("accessibility.color_filters"), BsI18n.get("accessibility.color_filters_desc"), false),
                SettingItem.select(BsI18n.get("accessibility.filter_type"), BsI18n.get("accessibility.filter_type_desc"), new String[]{BsI18n.get("accessibility.deuteranopia"), BsI18n.get("accessibility.protanopia"), BsI18n.get("accessibility.tritanopia"), BsI18n.get("accessibility.grayscale"), BsI18n.get("accessibility.invert")}, BsI18n.get("accessibility.deuteranopia")),
                SettingItem.toggle(BsI18n.get("accessibility.shortcut_key"), "Win + Ctrl + C " + BsI18n.get("accessibility.toggle"), true)
        );

        group(BsI18n.get("accessibility.group_contrast_themes"),
                SettingItem.select(BsI18n.get("accessibility.contrast_theme"), "", new String[]{BsI18n.get("accessibility.theme_none"), BsI18n.get("accessibility.theme_aquatic"), BsI18n.get("accessibility.theme_desert"), BsI18n.get("accessibility.theme_dusk"), BsI18n.get("accessibility.theme_night_sky"), "DUSK (" + BsI18n.get("accessibility.high_contrast") + ")"}, BsI18n.get("accessibility.theme_none")),
                SettingItem.toggle(BsI18n.get("accessibility.notify_after_turning_off_contrast"), "", true),
                SettingItem.link(BsI18n.get("accessibility.custom_colors"), "", BsI18n.get("accessibility.edit_theme_colors"))
        );

        group(BsI18n.get("accessibility.group_keyboard"),
                SettingItem.toggle(BsI18n.get("accessibility.sticky_keys"), BsI18n.get("accessibility.sticky_keys_desc"), false),
                SettingItem.toggle(BsI18n.get("accessibility.filter_keys"), BsI18n.get("accessibility.filter_keys_desc"), false),
                SettingItem.toggle(BsI18n.get("accessibility.toggle_keys"), BsI18n.get("accessibility.toggle_keys_desc"), false),
                SettingItem.toggle(BsI18n.get("accessibility.on_screen_keyboard"), BsI18n.get("accessibility.on_screen_keyboard_desc"), false),
                SettingItem.value(BsI18n.get("accessibility.osk_shortcut"), "", "Win + Ctrl + O")
        );

        group(BsI18n.get("accessibility.group_mouse_cursor"),
                SettingItem.select(BsI18n.get("accessibility.pointer_size"), "", new String[]{"1 (" + BsI18n.get("accessibility.small") + ")", "2", "3", "4 (" + BsI18n.get("accessibility.medium") + ")", "5", "6 (" + BsI18n.get("accessibility.large") + ")"}, "1 (" + BsI18n.get("accessibility.small") + ")"),
                SettingItem.select(BsI18n.get("accessibility.pointer_color"), "", new String[]{BsI18n.get("accessibility.color_white"), BsI18n.get("accessibility.color_black"), BsI18n.get("accessibility.color_invert"), BsI18n.get("accessibility.accent_color_label")}, BsI18n.get("accessibility.color_white")),
                SettingItem.toggle(BsI18n.get("accessibility.cursor_move"), BsI18n.get("accessibility.cursor_move_desc"), false)
        );

        group(BsI18n.get("accessibility.group_eye_control"),
                SettingItem.toggle(BsI18n.get("accessibility.eye_control"), BsI18n.get("accessibility.eye_control_desc"), false),
                SettingItem.value(BsI18n.get("accessibility.eye_tracker"), "", BsI18n.get("accessibility.not_detected")),
                SettingItem.link(BsI18n.get("accessibility.eye_tracker_settings"), "", BsI18n.get("accessibility.configure"))
        );
    }
}
