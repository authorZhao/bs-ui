package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 账户页（按 Win11 真实结构）。
 *
 * <p>设置组：你的信息 / 登录选项 / 家庭和其他用户 / 备份与凭据 / 设备密码恢复。</p>
 */
@Slf4j
public class AccountsPage extends CategoryPage {

    public AccountsPage(Skin skin) {
        super(BsI18n.get("nav.accounts"), skin);

        group(BsI18n.get("accounts.group_your_info"),
                SettingItem.value(BsI18n.get("accounts.account"), "", "authorZhao (Microsoft " + BsI18n.get("accounts.account_suffix") + ")"),
                SettingItem.value(BsI18n.get("accounts.account_type"), "", BsI18n.get("accounts.administrator")),
                SettingItem.value(BsI18n.get("accounts.logged_in_devices"), "", BsI18n.get("accounts.sync_to_onedrive")),
                SettingItem.button(BsI18n.get("accounts.manage_account"), BsI18n.get("accounts.manage_microsoft_account"), BsI18n.get("accounts.manage")),
                SettingItem.button(BsI18n.get("accounts.use_local_account"), BsI18n.get("accounts.use_local_account_desc"), BsI18n.get("accounts.use_local_account"))
        );

        group(BsI18n.get("accounts.group_signin_options"),
                SettingItem.value(BsI18n.get("accounts.windows_hello_face"), BsI18n.get("accounts.windows_hello_face_desc"), BsI18n.get("accounts.configured")),
                SettingItem.toggle(BsI18n.get("accounts.windows_hello_fingerprint"), BsI18n.get("accounts.windows_hello_fingerprint_desc"), true),
                SettingItem.value("PIN", BsI18n.get("accounts.windows_hello_pin"), BsI18n.get("accounts.pin_configured")),
                SettingItem.button(BsI18n.get("accounts.add_change_pin"), "", BsI18n.get("accounts.change")),
                SettingItem.toggle(BsI18n.get("accounts.dynamic_lock"), BsI18n.get("accounts.dynamic_lock_desc"), false),
                SettingItem.select(BsI18n.get("accounts.passwordless_signin"), BsI18n.get("accounts.passwordless_signin_desc"), new String[]{BsI18n.get("common.disabled"), BsI18n.get("common.enabled")}, BsI18n.get("common.enabled")),
                SettingItem.select(BsI18n.get("accounts.screen_timeout_lock"), BsI18n.get("accounts.screen_timeout_lock_desc"), new String[]{BsI18n.get("common.never"), BsI18n.get("accounts.1min"), BsI18n.get("accounts.3min"), BsI18n.get("common.5min"), BsI18n.get("common.15min")}, BsI18n.get("common.5min"))
        );

        group(BsI18n.get("accounts.group_email_accounts"),
                SettingItem.value(BsI18n.get("accounts.primary_account"), "", "author@outlook.com"),
                SettingItem.button(BsI18n.get("accounts.add_account"), BsI18n.get("accounts.add_work_school_account"), BsI18n.get("accounts.add_account")),
                SettingItem.value(BsI18n.get("accounts.added_accounts"), "", BsI18n.get("accounts.added_accounts_value"))
        );

        group(BsI18n.get("accounts.group_family_others"),
                SettingItem.value(BsI18n.get("accounts.other_users"), "", BsI18n.get("accounts.other_users_value")),
                SettingItem.button(BsI18n.get("accounts.add_account"), BsI18n.get("accounts.add_family_or_other"), BsI18n.get("accounts.add_account"),
                        () -> log.info("添加用户账户")),
                SettingItem.button(BsI18n.get("accounts.set_work_school_account"), BsI18n.get("accounts.set_work_school_account_desc"), BsI18n.get("accounts.connect")),
                SettingItem.toggle(BsI18n.get("accounts.allow_family_slideshow"), "", false)
        );

        group(BsI18n.get("accounts.group_windows_backup"),
                SettingItem.value(BsI18n.get("accounts.backup_status"), "", BsI18n.get("accounts.sync_to_onedrive")),
                SettingItem.button(BsI18n.get("accounts.backup_settings"), BsI18n.get("accounts.backup_settings_desc"), BsI18n.get("accounts.backup"), () -> log.info("Windows 备份")),
                SettingItem.button(BsI18n.get("accounts.remember_my_apps"), BsI18n.get("accounts.remember_my_apps_desc"), BsI18n.get("accounts.manage"))
        );

        group(BsI18n.get("accounts.group_credentials"),
                SettingItem.button(BsI18n.get("accounts.credential_manager"), BsI18n.get("accounts.credential_manager_desc"), BsI18n.get("accounts.manage")),
                SettingItem.value(BsI18n.get("accounts.account_recovery"), BsI18n.get("accounts.account_recovery_desc"), BsI18n.get("accounts.configured")),
                SettingItem.link(BsI18n.get("accounts.view_account_activity"), BsI18n.get("accounts.view_account_activity_desc"), BsI18n.get("accounts.view"))
        );
    }
}
