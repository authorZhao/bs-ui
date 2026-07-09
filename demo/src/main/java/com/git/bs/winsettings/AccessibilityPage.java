package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

/**
 * 辅助功能页（按 Win11 真实结构）。
 *
 * <p>设置组：视觉效果 / 文本大小 / 讲述人 / 放大镜 / 颜色过滤器 / 对比度主题 /
 * 键盘 / 鼠标与光标 / 眼睛控制。</p>
 */
@Slf4j
public class AccessibilityPage extends CategoryPage {

    public AccessibilityPage(Skin skin) {
        super("辅助功能", skin);

        group("视觉效果",
                SettingItem.toggle("始终显示滚动条", "", true),
                SettingItem.toggle("透明效果", "", true),
                SettingItem.toggle("关闭动画", "减少不必要的动画", false),
                SettingItem.select("通知显示时间", "", new String[]{"5 秒","10 秒","15 秒"}, "5 秒")
        );

        group("文本大小",
                SettingItem.select("文本大小", "放大屏幕文本", new String[]{"100%","120%","150%","200%"}, "100%"),
                SettingItem.value("说明", "", "预览: 这是一个示例文本"),
                SettingItem.button("应用", "应用文本大小", "应用")
        );

        group("讲述人",
                SettingItem.toggle("讲述人", "屏幕阅读器朗读屏幕", false),
                SettingItem.toggle("登录后自动启动讲述人", "", false),
                SettingItem.select("讲述人语音", "", new String[]{"中文 简体 (Huihui)","English (David)","English (Zira)"}, "中文 简体 (Huihui)"),
                SettingItem.select("详细程度", "", new String[]{"最低","普通","详细"}, "普通"),
                SettingItem.toggle("强调加粗格式", "", false),
                SettingItem.link("讲述人主页", "", "查看")
        );

        group("放大镜",
                SettingItem.toggle("放大镜", "放大屏幕部分区域", false),
                SettingItem.select("缩放级别", "", new String[]{"100%","150%","200%","300%","400%"}, "200%"),
                SettingItem.select("放大镜模式", "", new String[]{"全屏","镜头","停靠"}, "全屏"),
                SettingItem.toggle("启动放大镜后自动反转颜色", "", false)
        );

        group("颜色过滤器",
                SettingItem.toggle("颜色过滤器", "为色弱用户调整颜色", false),
                SettingItem.select("滤镜类型", "色盲/色弱滤镜", new String[]{"红绿色盲(绿色弱)","红绿色盲(红色弱)","蓝黄色盲","灰度","反转"}, "红绿色盲(绿色弱)"),
                SettingItem.toggle("快捷键", "Win + Ctrl + C 切换", true)
        );

        group("对比度主题",
                SettingItem.select("对比度主题", "", new String[]{"无","水族","沙漠","黄昏","夜空","DUSK(高对比度)"}, "无"),
                SettingItem.toggle("关闭对比度主题后通知", "", true),
                SettingItem.link("自定义颜色", "", "编辑主题颜色")
        );

        group("键盘",
                SettingItem.toggle("粘滞键", "一次按一个键实现组合键 (Ctrl/Shift/Alt)", false),
                SettingItem.toggle("筛选键", "忽略短暂或重复的按键", false),
                SettingItem.toggle("切换键", "按 Caps/Num/Scroll 时发出响声", false),
                SettingItem.toggle("屏幕键盘", "在屏幕上显示虚拟键盘", false),
                SettingItem.value("屏幕键盘快捷键", "", "Win + Ctrl + O")
        );

        group("鼠标与光标",
                SettingItem.select("指针大小", "", new String[]{"1(小)","2","3","4(中)","5","6(大)"}, "1(小)"),
                SettingItem.select("指针颜色", "", new String[]{"白色","黑色","反转","主题色"}, "白色"),
                SettingItem.toggle("光标在指针下移动", "根据指针移动方向调整文本光标", false)
        );

        group("眼睛控制",
                SettingItem.toggle("眼睛控制", "用眼动仪控制 PC", false),
                SettingItem.value("眼动仪", "", "未检测到"),
                SettingItem.link("眼动仪设置", "", "配置")
        );
    }
}
