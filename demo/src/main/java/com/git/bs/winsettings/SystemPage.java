package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统设置页（完整示例）—— 演示 CategoryPage 的声明式用法。
 *
 * <p>涵盖 Win11 系统分类的主要设置组：显示 / 声音 / 通知与专注 / 电源与电池 / 存储 / 关于。
 * 所有操作只打日志（{@link CategoryPage} 统一处理），少数项演示带业务回调。</p>
 */
@Slf4j
public class SystemPage extends CategoryPage {

    public SystemPage(Skin skin) {
        super(BsI18n.get("nav.system"), skin);

        // 二级页面入口（Win11 系统页顶部：每行带 › 箭头进子页）
        group("",
                SettingItem.page("🖥", BsI18n.get("system.display"), BsI18n.get("system.display_desc"), "system/display"),
                SettingItem.page("🔊", BsI18n.get("system.sound"), BsI18n.get("system.sound_desc"), "system/sound"),
                SettingItem.page("🔔", BsI18n.get("system.notifications"), BsI18n.get("system.notifications_desc"), "system/notifications"),
                SettingItem.page("🔋", BsI18n.get("system.power"), BsI18n.get("system.power_desc"), "system/power"),
                SettingItem.page("💾", BsI18n.get("system.storage"), BsI18n.get("system.storage_desc"), "system/storage")
        );

        group(BsI18n.get("system.group_display"),
                SettingItem.value(BsI18n.get("system.resolution"), BsI18n.get("system.resolution_desc"), BsI18n.get("system.resolution_value")),
                SettingItem.value(BsI18n.get("system.scale"), BsI18n.get("system.scale_desc"), "100%"),
                SettingItem.toggle(BsI18n.get("system.night_light"), BsI18n.get("system.night_light_desc"), false, c -> log.info("应用夜间模式: {}", c)),
                SettingItem.select(BsI18n.get("system.orientation"), BsI18n.get("system.orientation_desc"),
                        new String[]{BsI18n.get("system.orient_landscape"), BsI18n.get("system.orient_portrait"),
                                BsI18n.get("system.orient_landscape_flip"), BsI18n.get("system.orient_portrait_flip")},
                        BsI18n.get("system.orient_landscape"))
        );

        group(BsI18n.get("system.group_sound"),
                SettingItem.select(BsI18n.get("system.output"), BsI18n.get("system.output_desc"),
                        new String[]{BsI18n.get("system.speakers"), BsI18n.get("system.headphones"), BsI18n.get("system.hdmi_output")},
                        BsI18n.get("system.speakers")),
                SettingItem.select(BsI18n.get("system.input"), BsI18n.get("system.input_desc"),
                        new String[]{BsI18n.get("system.mic_array"), BsI18n.get("system.line_in")},
                        BsI18n.get("system.mic_array")),
                SettingItem.toggle(BsI18n.get("system.spatial_audio"), "Windows Sonic for Headphones", false)
        );

        group(BsI18n.get("system.group_focus"),
                SettingItem.toggle(BsI18n.get("system.notifications"), BsI18n.get("system.notifications_allow"), true),
                SettingItem.toggle(BsI18n.get("system.focus_assist"), BsI18n.get("system.focus_assist_desc"), false),
                SettingItem.toggle(BsI18n.get("system.do_not_disturb"), BsI18n.get("system.do_not_disturb_desc"), false)
        );

        group(BsI18n.get("system.group_power"),
                SettingItem.select(BsI18n.get("system.power_mode"), BsI18n.get("system.power_mode_desc"),
                        new String[]{BsI18n.get("system.power_balanced"), BsI18n.get("system.power_best_perf"), BsI18n.get("system.power_best_efficiency")},
                        BsI18n.get("system.power_balanced")),
                SettingItem.select(BsI18n.get("system.screen_sleep"), BsI18n.get("system.screen_sleep_desc"),
                        new String[]{BsI18n.get("common.5min"), BsI18n.get("common.10min"), BsI18n.get("common.15min"),
                                BsI18n.get("common.30min"), BsI18n.get("common.never")},
                        BsI18n.get("common.10min")),
                SettingItem.button(BsI18n.get("system.power_sleep"), BsI18n.get("system.advanced_power"), BsI18n.get("common.open"), () -> log.info("打开电源选项"))
        );

        group(BsI18n.get("system.group_storage"),
                SettingItem.value(BsI18n.get("system.disk_c"), BsI18n.get("system.disk_usage"), "128 GB / 256 GB (50%)"),
                SettingItem.toggle(BsI18n.get("system.storage_sense"), BsI18n.get("system.storage_sense_desc"), true),
                SettingItem.button(BsI18n.get("system.temp_files"), BsI18n.get("system.temp_files_desc"), BsI18n.get("system.cleanup"), () -> log.info("清理临时文件"))
        );

        group(BsI18n.get("system.group_about"),
                SettingItem.value(BsI18n.get("system.device_name"), "", "DESKTOP-BSUI"),
                SettingItem.value(BsI18n.get("system.processor"), "", "Intel Core i7-12700H @ 2.30GHz"),
                SettingItem.value(BsI18n.get("system.memory"), "", "16.0 GB (15.8 GB usable)"),
                SettingItem.value(BsI18n.get("system.system_type"), "", "64-bit OS, x64 processor"),
                SettingItem.value(BsI18n.get("system.version"), "", "Windows 11 Pro 23H2"),
                SettingItem.link(BsI18n.get("system.windows_spec"), "", BsI18n.get("system.view_spec"), () -> log.info("查看 Windows 规格"))
        );
    }
}
