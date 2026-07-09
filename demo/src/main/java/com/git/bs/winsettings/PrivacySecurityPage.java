package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 隐私和安全性页（按 Win11 真实结构）。
 *
 * <p>设置组：Windows 安全中心 / 设备安全性 / 查找我的设备 / 开发者选项 /
 * 位置 / 相机与麦克风 / 通知与账户 / 诊断与活动。</p>
 */
@Slf4j
public class PrivacySecurityPage extends CategoryPage {

    public PrivacySecurityPage(Skin skin) {
        super(BsI18n.get("nav.privacy"), skin);

        group(BsI18n.get("privacy.group_security_center"),
                SettingItem.value(BsI18n.get("privacy.virus_threat_protection"), "", "\u2713 " + BsI18n.get("privacy.enabled_ms_defender")),
                SettingItem.value(BsI18n.get("privacy.firewall_network_protection"), "", "\u2713 " + BsI18n.get("privacy.normal")),
                SettingItem.value(BsI18n.get("privacy.account_protection"), "", "\u2713 " + BsI18n.get("privacy.normal")),
                SettingItem.value(BsI18n.get("privacy.app_browser_control"), "", "\u2713 " + BsI18n.get("privacy.normal")),
                SettingItem.button(BsI18n.get("privacy.open_security_center"), "", BsI18n.get("common.open"),
                        () -> log.info("打开 Windows 安全中心"))
        );

        group(BsI18n.get("privacy.group_device_security"),
                SettingItem.toggle(BsI18n.get("privacy.memory_integrity"), BsI18n.get("privacy.memory_integrity_desc"), true),
                SettingItem.value(BsI18n.get("privacy.secure_boot"), "", "\u2713 " + BsI18n.get("privacy.enabled")),
                SettingItem.toggle(BsI18n.get("privacy.tpm_encryption"), BsI18n.get("privacy.tpm_encryption_desc"), true),
                SettingItem.value(BsI18n.get("privacy.processor_model"), "", BsI18n.get("privacy.supports_security")),
                SettingItem.button(BsI18n.get("privacy.device_security_details"), "", BsI18n.get("privacy.view"))
        );

        group(BsI18n.get("privacy.group_find_my_device"),
                SettingItem.toggle(BsI18n.get("privacy.find_my_device"), BsI18n.get("privacy.find_my_device_desc"), true),
                SettingItem.value(BsI18n.get("privacy.last_location"), "", BsI18n.get("privacy.last_location_value")),
                SettingItem.value(BsI18n.get("privacy.last_updated"), "", BsI18n.get("privacy.last_updated_value")),
                SettingItem.button(BsI18n.get("privacy.view_device"), BsI18n.get("privacy.view_device_desc"), BsI18n.get("privacy.view"))
        );

        group(BsI18n.get("privacy.group_developer"),
                SettingItem.toggle(BsI18n.get("privacy.developer_mode"), BsI18n.get("privacy.developer_mode_desc"), false),
                SettingItem.toggle(BsI18n.get("privacy.device_portal"), BsI18n.get("privacy.device_portal_desc"), false),
                SettingItem.toggle(BsI18n.get("privacy.powershell_remote_signing"), "", false),
                SettingItem.toggle(BsI18n.get("privacy.device_discovery"), BsI18n.get("privacy.device_discovery_desc"), false),
                SettingItem.value(BsI18n.get("privacy.device_ip"), "", "192.168.1.100"),
                SettingItem.link(BsI18n.get("privacy.developer_docs"), "", BsI18n.get("privacy.view"))
        );

        group(BsI18n.get("privacy.group_location"),
                SettingItem.toggle(BsI18n.get("privacy.location_service"), "", true),
                SettingItem.toggle(BsI18n.get("privacy.desktop_apps_location"), "", true),
                SettingItem.value(BsI18n.get("privacy.default_location"), "", BsI18n.get("privacy.shanghai")),
                SettingItem.button(BsI18n.get("privacy.clear_location_history"), "", BsI18n.get("privacy.clear"))
        );

        group(BsI18n.get("privacy.group_camera_mic"),
                SettingItem.toggle(BsI18n.get("privacy.camera_access"), "", true),
                SettingItem.toggle(BsI18n.get("privacy.desktop_apps_camera"), "", true),
                SettingItem.toggle(BsI18n.get("privacy.microphone_access"), "", true),
                SettingItem.toggle(BsI18n.get("privacy.desktop_apps_microphone"), "", true),
                SettingItem.button(BsI18n.get("privacy.camera_privacy"), BsI18n.get("privacy.camera_privacy_desc"), BsI18n.get("privacy.view"))
        );

        group(BsI18n.get("privacy.group_notifications_accounts"),
                SettingItem.toggle(BsI18n.get("privacy.allow_app_notifications"), "", true),
                SettingItem.toggle(BsI18n.get("privacy.app_account_info_access"), "", true),
                SettingItem.toggle(BsI18n.get("privacy.app_contacts_access"), "", false),
                SettingItem.toggle(BsI18n.get("privacy.app_calendar_access"), "", false),
                SettingItem.toggle(BsI18n.get("privacy.app_call_history_access"), "", false)
        );

        group(BsI18n.get("privacy.group_diagnostics"),
                SettingItem.toggle(BsI18n.get("privacy.send_optional_diagnostics"), BsI18n.get("privacy.send_optional_diagnostics_desc"), false),
                SettingItem.toggle(BsI18n.get("privacy.personalized_input"), BsI18n.get("privacy.personalized_input_desc"), true),
                SettingItem.toggle(BsI18n.get("privacy.ad_id_per_app"), "", true),
                SettingItem.value(BsI18n.get("privacy.activity_history"), "", BsI18n.get("privacy.activity_history_value")),
                SettingItem.button(BsI18n.get("privacy.view_diagnostics"), "", BsI18n.get("privacy.view"))
        );
    }
}
