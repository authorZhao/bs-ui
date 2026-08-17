/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库
 * Copyright (c) 2026 bs-ui contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */

package cn.pingyuanren.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import cn.pingyuanren.bs.i18n.BsI18n;

/**
 * 二级页面：显示（按 Win11「系统 › 显示」）。
 *
 * <p>设置组：显示器 / 亮度和颜色 / 夜间模式 / 缩放与多显示器。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class DisplayPage extends CategoryPage {

    public DisplayPage(Skin skin) {
        super(BsI18n.get("display.title"),
                BsI18n.get("nav.home") + "  ›  " + BsI18n.get("nav.system") + "  ›  " + BsI18n.get("display.title"), skin);

        group(BsI18n.get("display.group_monitor"),
                SettingItem.value(BsI18n.get("display.monitor"), "", BsI18n.get("display.monitor_value")),
                SettingItem.select(BsI18n.get("display.resolution"), "", new String[]{BsI18n.get("display.res_4k_recommended"), BsI18n.get("display.res_2k"), BsI18n.get("display.res_1080p")}, BsI18n.get("display.res_4k_recommended")),
                SettingItem.select(BsI18n.get("display.scaling"), BsI18n.get("display.scaling_desc"), new String[]{"100%", "125%", "150%", "175%", "200%"}, "150%"),
                SettingItem.select(BsI18n.get("display.orientation"), "", new String[]{BsI18n.get("display.orient_landscape"), BsI18n.get("display.orient_portrait"), BsI18n.get("display.orient_landscape_flip"), BsI18n.get("display.orient_portrait_flip")}, BsI18n.get("display.orient_landscape"))
        );

        group(BsI18n.get("display.group_brightness_color"),
                SettingItem.select(BsI18n.get("display.brightness"), "", new String[]{"100%", "80%", "60%", "40%", "20%"}, "80%"),
                SettingItem.select("HDR", BsI18n.get("display.hdr_desc"), new String[]{BsI18n.get("common.on"), BsI18n.get("common.off")}, BsI18n.get("common.on")),
                SettingItem.select(BsI18n.get("display.color_profile"), "", new String[]{"sRGB IEC61966-2.1", "DCI-P3", BsI18n.get("display.custom")}, "sRGB IEC61966-2.1"),
                SettingItem.button(BsI18n.get("display.color_calibration"), BsI18n.get("display.color_calibration_desc"), BsI18n.get("display.calibrate"))
        );

        group(BsI18n.get("display.group_night_light"),
                SettingItem.toggle(BsI18n.get("display.night_light"), BsI18n.get("display.night_light_desc"), false),
                SettingItem.select(BsI18n.get("display.intensity"), "", new String[]{BsI18n.get("display.weak"), BsI18n.get("display.medium"), BsI18n.get("display.strong")}, BsI18n.get("display.medium")),
                SettingItem.select(BsI18n.get("display.schedule"), "", new String[]{BsI18n.get("display.sunset_to_sunrise"), BsI18n.get("display.custom_time"), BsI18n.get("common.off")}, BsI18n.get("display.sunset_to_sunrise")),
                SettingItem.button(BsI18n.get("display.night_light_settings"), BsI18n.get("display.adjust_color_temp"), BsI18n.get("display.settings"))
        );

        group(BsI18n.get("display.group_multi_display"),
                SettingItem.select(BsI18n.get("display.multi_display_setup"), "", new String[]{BsI18n.get("display.extend_displays"), BsI18n.get("display.duplicate_displays"), BsI18n.get("display.display_1_only"), BsI18n.get("display.display_2_only")}, BsI18n.get("display.extend_displays")),
                SettingItem.toggle(BsI18n.get("display.snap_windows"), BsI18n.get("display.snap_windows_desc"), true),
                SettingItem.toggle(BsI18n.get("display.show_snap_layouts"), "", true),
                SettingItem.link(BsI18n.get("display.advanced_display"), BsI18n.get("display.advanced_display_desc"), BsI18n.get("display.view"))
        );
    }
}
