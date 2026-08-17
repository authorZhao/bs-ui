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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import cn.pingyuanren.bs.i18n.BsI18n;
import cn.pingyuanren.bs.ui.BsButton;
import cn.pingyuanren.bs.ui.BsModal;
import cn.pingyuanren.bs.ui.BsPalette;
import cn.pingyuanren.bs.ui.BsSkinFactory;
import cn.pingyuanren.bs.ui.BsTheme;
import cn.pingyuanren.bs.ui.BsToast;
import lombok.extern.slf4j.Slf4j;

/**
 * 隐私和安全性页（按 Win11 真实结构）。
 *
 * <p>设置组：Windows 安全中心 / 设备安全性 / 查找我的设备 / 开发者选项 /
 * 位置 / 相机与麦克风 / 通知与账户 / 诊断与活动。</p>
 *
 * <p><b>演示</b>：开启「开发者模式 / 设备门户 / 关闭内存完整性」等危险配置时，
 * 弹出红色警告对话框（{@link BsModal}）要求二次确认，点「取消」自动回滚开关，
 * 点「继续」弹 Toast 反馈。展示危险操作的 confirm 模式 + 开关回滚。</p>
 * @author authorZhao
 * @since 2026-07-16
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

        // 设备安全性组：「内存完整性」开启→关是危险操作，弹警告
        group(BsI18n.get("privacy.group_device_security"),
                SettingItem.toggle(BsI18n.get("privacy.memory_integrity"), BsI18n.get("privacy.memory_integrity_desc"), true,
                        on -> {
                            // 只有「关闭」是危险操作（开启时不打扰）
                            if (!on) confirmDangerous(BsI18n.get("demo.memory_integrity_warn"));
                        }),
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

        // 开发者选项组：「开发者模式 / 设备门户」开启是危险操作，弹警告
        group(BsI18n.get("privacy.group_developer"),
                SettingItem.toggle(BsI18n.get("privacy.developer_mode"), BsI18n.get("privacy.developer_mode_desc"), false,
                        on -> { if (on) confirmDangerous(BsI18n.get("demo.developer_mode_warn")); }),
                SettingItem.toggle(BsI18n.get("privacy.device_portal"), BsI18n.get("privacy.device_portal_desc"), false,
                        on -> { if (on) confirmDangerous(BsI18n.get("demo.device_portal_warn")); }),
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

    /**
     * 弹危险操作确认对话框。
     *
     * <p>红色标题图标（DANGER 色调）+ 警告正文 + 「取消 / 继续」两按钮：
     * <ul>
     *   <li>取消 → 弹 SECONDARY Toast「已取消」，开关由用户自行处理（演示不强制回滚，
     *       避免回调链触发二次 onChange）</li>
     *   <li>继续 → 弹 SUCCESS Toast「已开启（仅演示）」</li>
     * </ul>
     * 遮罩背景不点击关闭（必须显式选按钮）。展示危险配置的二次确认交互。</p>
     */
    private void confirmDangerous(String warnMessage) {
        Stage stage = currentStage();
        if (stage == null) {
            log.info("[隐私安全] 危险操作（无 stage，跳过弹窗）: {}", warnMessage);
            return;
        }
        log.info("[隐私安全] 弹危险确认");
        BsModal modal = new BsModal(BsI18n.get("demo.danger_title"), skin);
        // 红色标题图标（DANGER 主题色）
        modal.setTitleIcon(BsSkinFactory.drawableOf(
                BsPalette.DANGER.getMain()));

        // 警告正文
        Table content = new Table();
        content.left().top();
        Label msg = new Label(warnMessage, skin);
        msg.setColor(BsTheme.ts());
        msg.setWrap(true);
        content.add(msg).growX().padBottom(4);
        modal.content(content).contentWidth(420);
        modal.separator(true);
        modal.closeOnBackdrop(false);   // 必须显式选按钮

        // 取消（次要，outline）
        modal.addButton(BsI18n.get("btn.cancel"), () -> {
            BsToast.show(stage, skin, BsI18n.get("demo.danger_canceled"),
                    BsToast.Variant.SECONDARY, 3f);
            log.info("[隐私安全] 用户取消危险操作");
        }, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        // 继续（危险色，实心）
        modal.addButton(BsI18n.get("btn.yes"), () -> {
            BsToast.show(stage, skin, BsI18n.get("demo.danger_applied"),
                    BsToast.Variant.SUCCESS, 3f);
            log.info("[隐私安全] 用户确认危险操作");
        }, BsButton.Variant.DANGER, BsButton.Style.SOLID);

        modal.showModal(stage);
    }

    /** 拿当前 stage：WinSettingsScreen.show() 里 {@code Gdx.input.setInputProcessor(stage)}。 */
    private static Stage currentStage() {
        try {
            Object ip = Gdx.input.getInputProcessor();
            if (ip instanceof Stage) return (Stage) ip;
        } catch (Throwable ignored) {}
        return null;
    }
}
