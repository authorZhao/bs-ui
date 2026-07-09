package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 游戏页（按 Win11 真实结构）。
 *
 * <p>设置组：Xbox Game Bar / 捕获 / 游戏模式 / 游戏控制器 / 相关设置。</p>
 */
@Slf4j
public class GamingPage extends CategoryPage {

    public GamingPage(Skin skin) {
        super(BsI18n.get("nav.gaming"), skin);

        group("Xbox Game Bar",
                SettingItem.toggle(BsI18n.get("gaming.use_game_bar"), BsI18n.get("gaming.use_game_bar_desc"), true),
                SettingItem.value(BsI18n.get("gaming.open_shortcut"), "", "Win + G"),
                SettingItem.toggle(BsI18n.get("gaming.open_with_controller"), BsI18n.get("gaming.open_with_controller_desc"), true),
                SettingItem.link(BsI18n.get("gaming.game_bar_settings"), BsI18n.get("gaming.game_bar_settings_desc"), BsI18n.get("gaming.settings"))
        );

        group(BsI18n.get("gaming.group_capture"),
                SettingItem.toggle(BsI18n.get("gaming.record_game"), BsI18n.get("gaming.record_game_desc"), true),
                SettingItem.select(BsI18n.get("gaming.capture_quality"), "", new String[]{BsI18n.get("gaming.quality_standard"), BsI18n.get("gaming.quality_high"), BsI18n.get("gaming.quality_ultra")}, BsI18n.get("gaming.quality_high")),
                SettingItem.select(BsI18n.get("gaming.video_framerate"), "", new String[]{"30 fps", "60 fps"}, "60 fps"),
                SettingItem.select(BsI18n.get("gaming.audio_quality"), "", new String[]{"128 kbps", "160 kbps", "192 kbps"}, "192 kbps"),
                SettingItem.toggle(BsI18n.get("gaming.open_mic_when_recording"), "", true),
                SettingItem.value(BsI18n.get("gaming.save_location"), "", "C:\\Users\\author\\Videos\\Captures"),
                SettingItem.button(BsI18n.get("gaming.open_save_location"), BsI18n.get("gaming.open_save_location_desc"), BsI18n.get("common.open"))
        );

        group(BsI18n.get("gaming.group_game_mode"),
                SettingItem.toggle(BsI18n.get("gaming.game_mode"), BsI18n.get("gaming.game_mode_desc"), true),
                SettingItem.value(BsI18n.get("gaming.status"), "", BsI18n.get("gaming.status_enabled")),
                SettingItem.toggle(BsI18n.get("gaming.dynamic_lighting"), BsI18n.get("gaming.dynamic_lighting_desc"), false)
        );

        group(BsI18n.get("gaming.group_controllers"),
                SettingItem.value(BsI18n.get("gaming.connected_controllers"), "", BsI18n.get("gaming.connected_controllers_value")),
                SettingItem.button(BsI18n.get("gaming.manage_controllers"), BsI18n.get("gaming.manage_controllers_desc"), BsI18n.get("gaming.manage")),
                SettingItem.link(BsI18n.get("gaming.controller_settings"), BsI18n.get("gaming.controller_settings_desc"), BsI18n.get("gaming.settings"))
        );

        group(BsI18n.get("gaming.group_related"),
                SettingItem.link("HDCP " + BsI18n.get("gaming.hdcp_settings"), BsI18n.get("gaming.hdcp_desc"), BsI18n.get("common.open")),
                SettingItem.link("Display Gamma", BsI18n.get("gaming.display_gamma_desc"), BsI18n.get("gaming.adjust")),
                SettingItem.button(BsI18n.get("gaming.game_troubleshoot"), BsI18n.get("gaming.game_troubleshoot_desc"), BsI18n.get("update.run"))
        );
    }
}
