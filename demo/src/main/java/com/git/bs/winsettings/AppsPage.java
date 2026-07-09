package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 应用页（按 Win11 真实结构）。
 *
 * <p>设置组：应用管理 / 默认应用 / 可选功能 / 高级应用设置 / 应用执行别名。</p>
 */
@Slf4j
public class AppsPage extends CategoryPage {

    public AppsPage(Skin skin) {
        super(BsI18n.get("nav.apps"), skin);

        group(BsI18n.get("apps.group_installed"),
                SettingItem.value(BsI18n.get("apps.installed_count"), "", BsI18n.get("apps.installed_count_value")),
                SettingItem.page("\u229E", BsI18n.get("apps.installed_apps"), BsI18n.get("apps.installed_apps_desc"), "apps/installed"),
                SettingItem.select(BsI18n.get("apps.sort_by"), "", new String[]{BsI18n.get("apps.sort_name"), BsI18n.get("apps.sort_size"), BsI18n.get("apps.sort_date")}, BsI18n.get("apps.sort_name")),
                SettingItem.select(BsI18n.get("apps.filter"), BsI18n.get("apps.filter_desc"), new String[]{BsI18n.get("apps.all_drives"), "C:", "D:"}, BsI18n.get("apps.all_drives"))
        );

        group(BsI18n.get("apps.group_default_apps"),
                SettingItem.button(BsI18n.get("apps.set_default_by_app"), BsI18n.get("apps.set_default_by_app_desc"), BsI18n.get("apps.set")),
                SettingItem.button(BsI18n.get("apps.set_default_by_file_type"), BsI18n.get("apps.set_default_by_file_type_desc"), BsI18n.get("apps.set")),
                SettingItem.button(BsI18n.get("apps.set_default_by_link_type"), BsI18n.get("apps.set_default_by_link_type_desc"), BsI18n.get("apps.set")),
                SettingItem.button(BsI18n.get("apps.reset_all_defaults"), BsI18n.get("apps.reset_all_defaults_desc"), BsI18n.get("apps.reset"))
        );

        group(BsI18n.get("apps.group_optional_features"),
                SettingItem.value(BsI18n.get("apps.installed_features"), "", BsI18n.get("apps.installed_features_value")),
                SettingItem.button(BsI18n.get("apps.add_optional_feature"), BsI18n.get("apps.add_optional_feature_desc"), BsI18n.get("apps.add_feature")),
                SettingItem.button(BsI18n.get("apps.more_windows_features"), BsI18n.get("apps.more_windows_features_desc"), BsI18n.get("apps.view"))
        );

        group(BsI18n.get("apps.group_advanced"),
                SettingItem.select(BsI18n.get("apps.app_install_source"), BsI18n.get("apps.app_install_source_desc"), new String[]{BsI18n.get("apps.source_anywhere_warn"), BsI18n.get("apps.source_anywhere"), BsI18n.get("apps.source_store_only"), BsI18n.get("apps.source_store_recommended")}, BsI18n.get("apps.source_store_recommended")),
                SettingItem.toggle(BsI18n.get("apps.archive_apps"), BsI18n.get("apps.archive_apps_desc"), false),
                SettingItem.toggle(BsI18n.get("apps.uninstall_unused"), BsI18n.get("apps.uninstall_unused_desc"), false)
        );

        group(BsI18n.get("apps.group_aliases"),
                SettingItem.toggle(BsI18n.get("apps.app_execution_aliases"), BsI18n.get("apps.app_execution_aliases_desc"), true),
                SettingItem.link(BsI18n.get("apps.manage_aliases"), BsI18n.get("apps.manage_aliases_desc"), BsI18n.get("apps.manage"))
        );

        group(BsI18n.get("apps.group_startup"),
                SettingItem.value(BsI18n.get("apps.startup_apps"), "", BsI18n.get("apps.startup_apps_value")),
                SettingItem.button(BsI18n.get("apps.manage_startup"), BsI18n.get("apps.manage_startup_desc"), BsI18n.get("apps.manage")),
                SettingItem.toggle(BsI18n.get("apps.sort_by_status"), BsI18n.get("apps.sort_by_status_desc"), false)
        );
    }
}
