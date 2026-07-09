package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * 二级页面：显示（按 Win11「系统 › 显示」）。
 *
 * <p>设置组：显示器 / 亮度和颜色 / 夜间模式 / 缩放与多显示器。</p>
 */
public class DisplayPage extends CategoryPage {

    public DisplayPage(Skin skin) {
        super("显示", "主页  ›  系统  ›  显示", skin);

        group("显示器",
                SettingItem.value("显示器", "", "DELL U2720Q (27 英寸, 4K)"),
                SettingItem.select("显示分辨率", "", new String[]{"3840 × 2160 (推荐)","2560 × 1440","1920 × 1080"}, "3840 × 2160 (推荐)"),
                SettingItem.select("缩放", "文本/图标大小", new String[]{"100%","125%","150%","175%","200%"}, "150%"),
                SettingItem.select("显示方向", "", new String[]{"横向","纵向","横向翻转","纵向翻转"}, "横向")
        );

        group("亮度和颜色",
                SettingItem.select("亮度", "", new String[]{"100%","80%","60%","40%","20%"}, "80%"),
                SettingItem.select("HDR", "高动态范围", new String[]{"开","关"}, "开"),
                SettingItem.select("颜色配置文件", "", new String[]{"sRGB IEC61966-2.1","DCI-P3","自定义"}, "sRGB IEC61966-2.1"),
                SettingItem.button("颜色校准", "校准显示器颜色", "校准")
        );

        group("夜间模式",
                SettingItem.toggle("夜间模式", "减弱蓝光保护眼睛", false),
                SettingItem.select("强度", "", new String[]{"弱","中","强"}, "中"),
                SettingItem.select("计划", "", new String[]{"日落到日出","自定义时间","关闭"}, "日落到日出"),
                SettingItem.button("夜间模式设置", "调整色温", "设置")
        );

        group("缩放与多显示器",
                SettingItem.select("多显示器设置", "", new String[]{"扩展这些显示器","复制这些显示器","仅 1 号显示器","仅 2 号显示器"}, "扩展这些显示器"),
                SettingItem.toggle("贴靠窗口", "拖动窗口自动排列", true),
                SettingItem.toggle("在窗口边缘显示贴靠布局", "", true),
                SettingItem.link("高级显示设置", "适配器、刷新率、颜色深度", "查看")
        );
    }
}
