package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import com.git.bs.ui.BsDarkTheme;
import com.git.bs.ui.BsLightTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

/**
 * 个性化页（按 Win11 真实结构）。色彩模式 select 接真实换肤（BsUI.setTheme）。
 *
 * <p>设置组：背景 / 颜色 / 主题 / 锁屏界面 / 任务栏与开始 / 触摸键盘 / 字体。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class PersonalizationPage extends CategoryPage {

    public PersonalizationPage(Skin skin) {
        super(BsI18n.get("nav.personalization"), skin);

        group(BsI18n.get("personalization.group_background"),
                SettingItem.select(BsI18n.get("personalization.background_type"), BsI18n.get("personalization.background_type_desc"), new String[]{BsI18n.get("personalization.bg_picture"), BsI18n.get("personalization.bg_solid_color"), BsI18n.get("personalization.bg_slideshow"), BsI18n.get("personalization.bg_spotlight")}, BsI18n.get("personalization.bg_picture")),
                SettingItem.value(BsI18n.get("personalization.current_background"), "", BsI18n.get("personalization.current_background_value")),
                SettingItem.link(BsI18n.get("personalization.browse_background"), BsI18n.get("personalization.browse_background_desc"), BsI18n.get("personalization.browse")),
                SettingItem.select(BsI18n.get("personalization.fit"), BsI18n.get("personalization.fit_desc"), new String[]{BsI18n.get("personalization.fill"), BsI18n.get("personalization.fit_fit"), BsI18n.get("personalization.stretch"), BsI18n.get("personalization.center"), BsI18n.get("personalization.span")}, BsI18n.get("personalization.fill"))
        );

        group(BsI18n.get("personalization.group_color"),
                SettingItem.select(BsI18n.get("personalization.color_mode"), BsI18n.get("personalization.color_mode_desc"),
                        new String[]{BsI18n.get("personalization.mode_light"), BsI18n.get("personalization.mode_dark"), BsI18n.get("personalization.mode_custom")},
                        BsUI.currentTheme().isDark() ? BsI18n.get("personalization.mode_dark") : BsI18n.get("personalization.mode_light"),
                        m -> {
                            // 真实换肤：选亮/暗 → BsUI.setTheme → App 监听器重建 screen
                            if (BsI18n.get("personalization.mode_dark").equals(m)) BsUI.setTheme(BsDarkTheme.INSTANCE);
                            else if (BsI18n.get("personalization.mode_light").equals(m)) BsUI.setTheme(BsLightTheme.INSTANCE);
                        }),
                SettingItem.toggle(BsI18n.get("personalization.transparency"), BsI18n.get("personalization.transparency_desc"), true),
                SettingItem.select(BsI18n.get("personalization.accent_color"), BsI18n.get("personalization.accent_color_desc"), new String[]{BsI18n.get("personalization.color_blue"), BsI18n.get("personalization.color_cyan"), BsI18n.get("personalization.color_green"), BsI18n.get("personalization.color_yellow"), BsI18n.get("personalization.color_orange"), BsI18n.get("personalization.color_red"), BsI18n.get("personalization.color_pink")}, BsI18n.get("personalization.color_blue")),
                SettingItem.toggle(BsI18n.get("personalization.show_accent_start_taskbar"), "", true),
                SettingItem.toggle(BsI18n.get("personalization.show_accent_titlebar_border"), "", true)
        );

        group(BsI18n.get("personalization.group_themes"),
                SettingItem.value(BsI18n.get("personalization.current_theme"), "", BsI18n.get("personalization.current_theme_value")),
                SettingItem.button(BsI18n.get("personalization.manage_themes"), BsI18n.get("personalization.manage_themes_desc"), BsI18n.get("personalization.manage_themes")),
                SettingItem.link(BsI18n.get("personalization.get_more_themes"), "", BsI18n.get("personalization.get"))
        );

        group(BsI18n.get("personalization.group_lockscreen"),
                SettingItem.select(BsI18n.get("personalization.lockscreen_background"), "", new String[]{BsI18n.get("personalization.bg_spotlight"), BsI18n.get("personalization.bg_picture"), BsI18n.get("personalization.bg_slideshow")}, BsI18n.get("personalization.bg_spotlight")),
                SettingItem.toggle(BsI18n.get("personalization.lockscreen_fun_facts"), "", true),
                SettingItem.select(BsI18n.get("personalization.screensaver"), BsI18n.get("personalization.screensaver_desc"), new String[]{BsI18n.get("personalization.saver_none"), BsI18n.get("personalization.saver_3d_text"), BsI18n.get("personalization.saver_bubbles"), BsI18n.get("personalization.saver_ribbons"), BsI18n.get("personalization.saver_blank")}, BsI18n.get("personalization.saver_none")),
                SettingItem.link(BsI18n.get("personalization.screensaver_settings"), "", BsI18n.get("personalization.settings"))
        );

        group(BsI18n.get("personalization.group_taskbar_start"),
                SettingItem.select(BsI18n.get("personalization.taskbar_alignment"), BsI18n.get("personalization.taskbar_alignment_desc"), new String[]{BsI18n.get("personalization.align_left"), BsI18n.get("personalization.align_center")}, BsI18n.get("personalization.align_center")),
                SettingItem.toggle(BsI18n.get("personalization.auto_hide_taskbar"), "", false),
                SettingItem.toggle(BsI18n.get("personalization.auto_hide_taskbar_desktop"), "", false),
                SettingItem.toggle(BsI18n.get("personalization.start_recently_added"), "", true),
                SettingItem.toggle(BsI18n.get("personalization.start_most_used"), "", true),
                SettingItem.toggle(BsI18n.get("personalization.taskbar_search"), "", true),
                SettingItem.toggle(BsI18n.get("personalization.task_view_button"), "", true)
        );

        group(BsI18n.get("personalization.group_touch_keyboard"),
                SettingItem.toggle(BsI18n.get("personalization.show_touch_keyboard"), BsI18n.get("personalization.show_touch_keyboard_desc"), false),
                SettingItem.select(BsI18n.get("personalization.touch_keyboard_theme"), "", new String[]{BsI18n.get("personalization.mode_light"), BsI18n.get("personalization.mode_dark"), BsI18n.get("personalization.follow_system")}, BsI18n.get("personalization.follow_system")),
                SettingItem.toggle(BsI18n.get("personalization.show_numpad"), "", false)
        );

        group(BsI18n.get("personalization.group_fonts"),
                SettingItem.value(BsI18n.get("personalization.installed_fonts"), "", BsI18n.get("personalization.installed_fonts_value")),
                SettingItem.button(BsI18n.get("personalization.install_fonts"), BsI18n.get("personalization.install_fonts_desc"), BsI18n.get("personalization.install")),
                SettingItem.link(BsI18n.get("personalization.get_more_fonts"), "", BsI18n.get("personalization.get"))
        );
    }
}
