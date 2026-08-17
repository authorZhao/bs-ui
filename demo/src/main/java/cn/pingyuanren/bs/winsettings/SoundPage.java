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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import cn.pingyuanren.bs.i18n.BsI18n;
import cn.pingyuanren.bs.ui.BsSlider;
import cn.pingyuanren.bs.ui.BsTheme;
import lombok.extern.slf4j.Slf4j;

/**
 * 二级页面：声音（按 Win11「系统 › 声音」）。
 *
 * <p>设置组：输出 / 输入 / 高级。</p>
 *
 * <p><b>演示</b>：「主音量」「输入音量」用 {@link BsSlider} 滑块（0-100，步进 1），
 * 旁边带百分比文字实时回显。拖动滑块时文字同步更新。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class SoundPage extends CategoryPage {

    public SoundPage(Skin skin) {
        super(BsI18n.get("sound.title"),
                BsI18n.get("nav.home") + "  ›  " + BsI18n.get("nav.system") + "  ›  " + BsI18n.get("sound.title"), skin);

        group(BsI18n.get("sound.group_output"),
                SettingItem.select(BsI18n.get("sound.output_device"), BsI18n.get("sound.output_device_desc"), new String[]{BsI18n.get("sound.speakers_realtek"), BsI18n.get("sound.headphones"), BsI18n.get("sound.hdmi_output")}, BsI18n.get("sound.speakers_realtek")),
                // 主音量：滑块 + 百分比回显（拖动实时更新文字）
                SettingItem.custom(BsI18n.get("sound.master_volume"), "",
                        () -> volumeSlider(skin, 80)),
                SettingItem.toggle(BsI18n.get("sound.spatial_audio"), "Windows Sonic for Headphones", false),
                SettingItem.toggle(BsI18n.get("sound.mono_audio"), BsI18n.get("sound.mono_audio_desc"), false),
                SettingItem.button(BsI18n.get("sound.device_properties"), BsI18n.get("sound.device_properties_desc"), BsI18n.get("sound.properties"))
        );

        group(BsI18n.get("sound.group_input"),
                SettingItem.select(BsI18n.get("sound.input_device"), BsI18n.get("sound.input_device_desc"), new String[]{BsI18n.get("sound.mic_array_realtek"), BsI18n.get("sound.line_in")}, BsI18n.get("sound.mic_array_realtek")),
                // 输入音量：滑块 + 百分比回显
                SettingItem.custom(BsI18n.get("sound.input_volume"), "",
                        () -> volumeSlider(skin, 80)),
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

    /**
     * 构建音量滑块行：[滑块 growX] [百分比文字 40px]。
     *
     * <p>滑块 0-100 步进 1，初始值 {@code initial}。拖动时百分比文字实时回显。
     * 整体宽度固定 220px（Win11 设置页音量条宽度）。</p>
     */
    private Actor volumeSlider(Skin skin, int initial) {
        BsSlider slider = new BsSlider(0, 100, 1, false, skin);
        slider.setValue(initial);
        Label label = new Label(initial + "%", skin);
        label.setColor(BsTheme.ts());
        label.setAlignment(com.badlogic.gdx.utils.Align.right);

        // 拖动 → 更新百分比文字
        slider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                int v = (int) slider.getValue();
                label.setText(v + "%");
                log.info("[声音] 音量调到 {}%", v);
            }
        });

        Table row = new Table();
        row.add(slider).growX().padRight(8);
        row.add(label).width(42).right();
        // 固定整行宽度，避免挤占左标题
        Container<Actor> wrap = new Container<>(row);
        wrap.width(220f);
        return wrap;
    }
}
