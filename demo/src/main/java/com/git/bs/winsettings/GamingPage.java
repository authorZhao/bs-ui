package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

/**
 * 游戏页（按 Win11 真实结构）。
 *
 * <p>设置组：Xbox Game Bar / 捕获 / 游戏模式 / 游戏控制器 / 相关设置。</p>
 */
@Slf4j
public class GamingPage extends CategoryPage {

    public GamingPage(Skin skin) {
        super("游戏", skin);

        group("Xbox Game Bar",
                SettingItem.toggle("使用 Xbox Game Bar", "录制游戏片段、截图、与好友聊天", true),
                SettingItem.value("打开快捷键", "", "Win + G"),
                SettingItem.toggle("使用控制器打开", "按控制器上的 Xbox 按钮打开", true),
                SettingItem.link("Game Bar 设置", "在游戏中覆盖界面", "设置")
        );

        group("捕获",
                SettingItem.toggle("录制游戏", "在游戏中录制片段", true),
                SettingItem.select("捕获质量", "", new String[]{"标准 (720p)","高 (1080p)","极高 (1080p, 高码率)"}, "高 (1080p)"),
                SettingItem.select("视频帧率", "", new String[]{"30 fps","60 fps"}, "60 fps"),
                SettingItem.select("音频质量", "", new String[]{"128 kbps","160 kbps","192 kbps"}, "192 kbps"),
                SettingItem.toggle("录制时打开麦克风", "", true),
                SettingItem.value("保存位置", "", "C:\\Users\\author\\Videos\\Captures"),
                SettingItem.button("打开保存位置", "查看捕获的文件", "打开")
        );

        group("游戏模式",
                SettingItem.toggle("游戏模式", "优化 PC 进行游戏", true),
                SettingItem.value("状态", "", "已启用 (将停止后台任务)"),
                SettingItem.toggle("动态照明", "控制 RGB 设备", false)
        );

        group("游戏控制器",
                SettingItem.value("已连接控制器", "", "1 个 Xbox 无线控制器"),
                SettingItem.button("管理控制器", "查看/配置连接的控制器", "管理"),
                SettingItem.link("控制器设置", "震动、按钮映射", "设置")
        );

        group("相关设置",
                SettingItem.link("HDCP 设置", "HDMI 高带宽数字内容保护", "打开"),
                SettingItem.link("Display Gamma", "调整显示器 Gamma", "调整"),
                SettingItem.button("游戏疑难解答", "诊断游戏问题", "运行")
        );
    }
}
