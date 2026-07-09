package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;

/**
 * 二级页面：声音（按 Win11「系统 › 声音」）。
 *
 * <p>设置组：输出 / 输入 / 高级。</p>
 */
public class SoundPage extends CategoryPage {

    public SoundPage(Skin skin) {
        super(BsI18n.get("sound.title"),
                BsI18n.get("nav.home") + "  ›  " + BsI18n.get("nav.system") + "  ›  " + BsI18n.get("sound.title"), skin);

        group(BsI18n.get("sound.group_output"),
                SettingItem.select(BsI18n.get("sound.output_device"), BsI18n.get("sound.output_device_desc"), new String[]{BsI18n.get("sound.speakers_realtek"), BsI18n.get("sound.headphones"), BsI18n.get("sound.hdmi_output")}, BsI18n.get("sound.speakers_realtek")),
                SettingItem.select(BsI18n.get("sound.master_volume"), "", new String[]{"100%", "80%", "60%", "40%", BsI18n.get("sound.mute")}, "80%"),
                SettingItem.toggle(BsI18n.get("sound.spatial_audio"), "Windows Sonic for Headphones", false),
                SettingItem.toggle(BsI18n.get("sound.mono_audio"), BsI18n.get("sound.mono_audio_desc"), false),
                SettingItem.button(BsI18n.get("sound.device_properties"), BsI18n.get("sound.device_properties_desc"), BsI18n.get("sound.properties"))
        );

        group(BsI18n.get("sound.group_input"),
                SettingItem.select(BsI18n.get("sound.input_device"), BsI18n.get("sound.input_device_desc"), new String[]{BsI18n.get("sound.mic_array_realtek"), BsI18n.get("sound.line_in")}, BsI18n.get("sound.mic_array_realtek")),
                SettingItem.select(BsI18n.get("sound.input_volume"), "", new String[]{"100%", "80%", "60%", "40%"}, "80%"),
                SettingItem.button(BsI18n.get("sound.device_properties"), BsI18n.get("sound.test_mic"), BsI18n.get("sound.properties")),
                SettingItem.link(BsI18n.get("sound.manage_devices"), BsI18n.get("sound.manage_devices_desc"), BsI18n.get("sound.manage"))
        );

        group(BsI18n.get("sound.group_advanced"),
                SettingItem.toggle(BsI18n.get("sound.system_sounds"), BsI18n.get("sound.system_sounds_desc"), true),
                SettingItem.button(BsI18n.get("sound.volume_mixer"), BsI18n.get("sound.volume_mixer_desc"), BsI18n.get("common.open")),
                SettingItem.select(BsI18n.get("sound.default_sample_rate"), "", new String[]{"44100 Hz (CD)", "48000 Hz (DVD)", BsI18n.get("sound.96000_pro")}, "48000 Hz (DVD)"),
                SettingItem.select(BsI18n.get("sound.exclusive_mode"), BsI18n.get("sound.exclusive_mode_desc"), new String[]{BsI18n.get("common.on"), BsI18n.get("common.off")}, BsI18n.get("common.on")),
                SettingItem.link(BsI18n.get("sound.all_devices"), BsI18n.get("sound.all_devices_desc"), BsI18n.get("sound.view"))
        );
    }
}
