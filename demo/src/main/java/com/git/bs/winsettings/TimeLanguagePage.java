package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

/**
 * 时间和语言页（按 Win11 真实结构）。
 *
 * <p>设置组：日期和时间 / 语言和区域 / 打字 / 语音 / 中文输入法。</p>
 */
@Slf4j
public class TimeLanguagePage extends CategoryPage {

    public TimeLanguagePage(Skin skin) {
        super("时间和语言", skin);

        group("日期和时间",
                SettingItem.toggle("自动设置时间", "", true),
                SettingItem.toggle("自动设置时区", "", true),
                SettingItem.select("时区", "", new String[]{"(UTC+08:00) 北京","(UTC+09:00) 东京","(UTC+00:00) 伦敦","(UTC-08:00) 太平洋"}, "(UTC+08:00) 北京"),
                SettingItem.button("立即同步", "从 Internet 时间服务器同步", "同步",
                        () -> log.info("同步时间")),
                SettingItem.value("当前时间", "", "2026-07-09 14:30:00"),
                SettingItem.toggle("夏令时自动调整", "", false)
        );

        group("语言和区域",
                SettingItem.select("Windows 显示语言", "", new String[]{"中文(简体, 中国)","English (United States)","日本語 (日本)"}, "中文(简体, 中国)"),
                SettingItem.button("首选语言", "添加、删除、上移语言", "管理"),
                SettingItem.select("国家或地区", "用于内容推荐", new String[]{"中国","美国","日本","英国","韩国"}, "中国"),
                SettingItem.select("区域格式", "日期/时间/数字格式", new String[]{"中文(中国)","English (United States)","日本語(日本)"}, "中文(中国)")
        );

        group("打字",
                SettingItem.toggle("自动纠错", "纠正拼写错误的单词", true),
                SettingItem.toggle("自动拼写", "高亮拼写错误", true),
                SettingItem.toggle("输入法自动学习", "记录输入习惯", true),
                SettingItem.toggle("触控键盘打字纠错", "", true),
                SettingItem.button("高级键盘设置", "输入法切换顺序、语言栏", "打开")
        );

        group("语音",
                SettingItem.toggle("在线语音识别", "语音输入、语音访问", true),
                SettingItem.select("语音语言", "", new String[]{"中文(简体, 中国)","English (United States)"}, "中文(简体, 中国)"),
                SettingItem.toggle("麦克风访问", "允许应用使用麦克风", true),
                SettingItem.link("语音隐私", "管理语音数据", "管理")
        );

        group("中文输入法",
                SettingItem.value("微软拼音", "", "已启用"),
                SettingItem.value("微软五笔", "", "未启用"),
                SettingItem.select("默认输入模式", "中/英文", new String[]{"中文","英文"}, "中文"),
                SettingItem.select("默认标点", "中/英文标点", new String[]{"中文标点","英文标点"}, "中文标点"),
                SettingItem.toggle("简繁体切换", "Ctrl + Shift + F", true),
                SettingItem.button("输入法选项", "微软拼音/五笔详细设置", "选项")
        );
    }
}
