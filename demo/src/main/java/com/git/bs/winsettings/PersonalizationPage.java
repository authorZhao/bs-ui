package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.ui.BsDarkTheme;
import com.git.bs.ui.BsLightTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

/**
 * 个性化页（按 Win11 真实结构）。色彩模式 select 接真实换肤（BsUI.setTheme）。
 *
 * <p>设置组：背景 / 颜色 / 主题 / 锁屏界面 / 任务栏与开始 / 触摸键盘 / 字体。</p>
 */
@Slf4j
public class PersonalizationPage extends CategoryPage {

    public PersonalizationPage(Skin skin) {
        super("个性化", skin);

        group("背景",
                SettingItem.select("背景类型", "图片 / 纯色 / 幻灯片", new String[]{"图片","纯色","幻灯片","Windows 聚焦"}, "图片"),
                SettingItem.value("当前背景", "", "Windows 默认 (img0)"),
                SettingItem.link("浏览更多背景", "从文件选择背景", "浏览"),
                SettingItem.select("选择契合度", "填充/适应/拉伸/居中/跨区", new String[]{"填充","适应","拉伸","居中","跨区"}, "填充")
        );

        group("颜色",
                SettingItem.select("色彩模式", "亮 / 暗 / 自定义",
                        new String[]{"亮","暗","自定义"},
                        BsUI.currentTheme().isDark() ? "暗" : "亮",
                        m -> {
                            // 真实换肤：选亮/暗 → BsUI.setTheme → App 监听器重建 screen
                            if ("暗".equals(m)) BsUI.setTheme(BsDarkTheme.INSTANCE);
                            else if ("亮".equals(m)) BsUI.setTheme(BsLightTheme.INSTANCE);
                        }),
                SettingItem.toggle("透明效果", "半透明任务栏、开始菜单、窗口", true),
                SettingItem.select("主题色（强调色）", "用于任务栏/开始的高亮色", new String[]{"蓝","青","绿","黄","橙","红","粉"}, "蓝"),
                SettingItem.toggle("在开始和任务栏上显示强调色", "", true),
                SettingItem.toggle("显示主题色在标题栏和窗口边框", "", true)
        );

        group("主题",
                SettingItem.value("当前主题", "", "Windows (暗色)"),
                SettingItem.button("管理主题", "保存、切换、安装主题", "管理主题"),
                SettingItem.link("在 Microsoft Store 获取更多主题", "", "获取")
        );

        group("锁屏界面",
                SettingItem.select("锁屏背景", "", new String[]{"Windows 聚焦","图片","幻灯片"}, "Windows 聚焦"),
                SettingItem.toggle("在锁屏上获取有趣的事实、提示等", "", true),
                SettingItem.select("屏幕保护程序", "超时后启动屏保", new String[]{"无","3D 文字","气泡","彩带","空白"}, "无"),
                SettingItem.link("屏幕保护设置", "", "设置")
        );

        group("任务栏与开始",
                SettingItem.select("任务栏对齐", "左 / 中", new String[]{"左","中"}, "中"),
                SettingItem.toggle("自动隐藏任务栏", "", false),
                SettingItem.toggle("在桌面模式下自动隐藏任务栏", "", false),
                SettingItem.toggle("开始显示最近添加的应用", "", true),
                SettingItem.toggle("开始显示最常用的应用", "", true),
                SettingItem.toggle("显示任务栏上的搜索", "", true),
                SettingItem.toggle("显示任务视图按钮", "", true)
        );

        group("触摸键盘",
                SettingItem.toggle("显示触摸键盘", "无键盘时自动显示", false),
                SettingItem.select("触摸键盘主题", "", new String[]{"亮","暗","跟随系统"}, "跟随系统"),
                SettingItem.toggle("在标准键盘布局外显示数字键盘", "", false)
        );

        group("字体",
                SettingItem.value("已安装字体", "", "178 个字体"),
                SettingItem.button("安装字体", "拖入 .ttf/.otf 文件安装", "安装"),
                SettingItem.link("在 Microsoft Store 获取更多字体", "", "获取")
        );
    }
}
