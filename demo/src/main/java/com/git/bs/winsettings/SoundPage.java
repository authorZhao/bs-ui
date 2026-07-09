package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * 二级页面：声音（按 Win11「系统 › 声音」）。
 *
 * <p>设置组：输出 / 输入 / 高级。</p>
 */
public class SoundPage extends CategoryPage {

    public SoundPage(Skin skin) {
        super("声音", "主页  ›  系统  ›  声音", skin);

        group("输出",
                SettingItem.select("输出设备", "选择播放设备", new String[]{"扬声器 (Realtek Audio)","耳机","HDMI 输出"}, "扬声器 (Realtek Audio)"),
                SettingItem.select("主音量", "", new String[]{"100%","80%","60%","40%","静音"}, "80%"),
                SettingItem.toggle("空间音频", "Windows Sonic for Headphones", false),
                SettingItem.toggle("单声道音频", "左右声道合并", false),
                SettingItem.button("设备属性", "测试、配置输出设备", "属性")
        );

        group("输入",
                SettingItem.select("输入设备", "选择录音设备", new String[]{"麦克风阵列 (Realtek)","线路输入"}, "麦克风阵列 (Realtek)"),
                SettingItem.select("输入音量", "", new String[]{"100%","80%","60%","40%"}, "80%"),
                SettingItem.button("设备属性", "测试麦克风", "属性"),
                SettingItem.link("管理声音设备", "启用/禁用设备", "管理")
        );

        group("高级",
                SettingItem.toggle("系统声音", "系统事件提示音", true),
                SettingItem.button("音量合成器", "按应用调整音量", "打开"),
                SettingItem.select("默认采样率", "", new String[]{"44100 Hz (CD)","48000 Hz (DVD)","96000 Hz (专业)"}, "48000 Hz (DVD)"),
                SettingItem.select("独占模式", "允许应用独占控制设备", new String[]{"开","关"}, "开"),
                SettingItem.link("所有声音设备", "查看完整设备列表", "查看")
        );
    }
}
