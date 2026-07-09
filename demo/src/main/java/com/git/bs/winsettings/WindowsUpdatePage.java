package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * Windows 更新页（按 Win11 真实结构）。
 *
 * <p>设置组：检查更新 / 更多选项 / 高级选项 / 预览体验 / 故障排除。</p>
 */
@Slf4j
public class WindowsUpdatePage extends CategoryPage {

    public WindowsUpdatePage(Skin skin) {
        super(BsI18n.get("nav.update"), skin);

        group(BsI18n.get("update.group_check"),
                SettingItem.value(BsI18n.get("update.update_status"), "", BsI18n.get("update.up_to_date")),
                SettingItem.value(BsI18n.get("update.last_check"), "", BsI18n.get("update.last_check_value")),
                SettingItem.button(BsI18n.get("update.check_for_updates"), BsI18n.get("update.check_for_updates_desc"), BsI18n.get("update.check_for_updates"),
                        () -> log.info("[Windows 更新] 检查更新")),
                SettingItem.value(BsI18n.get("update.available_updates"), "", BsI18n.get("update.no_updates"))
        );

        group(BsI18n.get("update.group_more_options"),
                SettingItem.button(BsI18n.get("update.update_history"), BsI18n.get("update.update_history_desc"), BsI18n.get("update.view")),
                SettingItem.button(BsI18n.get("update.uninstall_updates"), BsI18n.get("update.uninstall_updates_desc"), BsI18n.get("update.uninstall")),
                SettingItem.select(BsI18n.get("update.pause_updates"), BsI18n.get("update.pause_updates_desc"), new String[]{BsI18n.get("update.no_pause"), BsI18n.get("update.pause_1w"), BsI18n.get("update.pause_2w"), BsI18n.get("update.pause_3w"), BsI18n.get("update.pause_4w"), BsI18n.get("update.pause_5w")}, BsI18n.get("update.no_pause")),
                SettingItem.value(BsI18n.get("update.pause_deadline"), "", BsI18n.get("update.not_paused")),
                SettingItem.button(BsI18n.get("update.check_online"), BsI18n.get("update.check_online_desc"), BsI18n.get("update.check"))
        );

        group(BsI18n.get("update.group_advanced"),
                SettingItem.toggle(BsI18n.get("update.notify_during_use"), BsI18n.get("update.notify_during_use_desc"), true),
                SettingItem.toggle(BsI18n.get("update.auto_download"), BsI18n.get("update.auto_download_desc"), true),
                SettingItem.toggle(BsI18n.get("update.update_other_ms_products"), BsI18n.get("update.update_other_ms_products_desc"), true),
                SettingItem.toggle(BsI18n.get("update.metered_connection"), BsI18n.get("update.metered_connection_desc"), false),
                SettingItem.button(BsI18n.get("update.delivery_optimization"), BsI18n.get("update.delivery_optimization_desc"), BsI18n.get("update.configure")),
                SettingItem.button(BsI18n.get("update.active_hours"), BsI18n.get("update.active_hours_desc"), BsI18n.get("update.adjust")),
                SettingItem.select(BsI18n.get("update.notification_method"), "", new String[]{BsI18n.get("update.notify_banner"), BsI18n.get("update.notify_sound"), BsI18n.get("update.notify_silent")}, BsI18n.get("update.notify_banner"))
        );

        group(BsI18n.get("update.group_insider"),
                SettingItem.value(BsI18n.get("update.insider_status"), "", BsI18n.get("update.not_joined")),
                SettingItem.link(BsI18n.get("update.join_insider"), BsI18n.get("update.join_insider_desc"), BsI18n.get("update.join")),
                SettingItem.value(BsI18n.get("update.channel"), "", "\u2014")
        );

        group(BsI18n.get("update.group_troubleshoot"),
                SettingItem.button(BsI18n.get("update.update_troubleshooter"), BsI18n.get("update.update_troubleshooter_desc"), BsI18n.get("update.run")),
                SettingItem.value(BsI18n.get("update.last_troubleshoot"), "", BsI18n.get("update.never_run")),
                SettingItem.button(BsI18n.get("update.reset_components"), BsI18n.get("update.reset_components_desc"), BsI18n.get("update.reset"))
        );
    }
}
