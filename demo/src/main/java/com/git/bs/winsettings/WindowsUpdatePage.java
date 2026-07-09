package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

/**
 * Windows 更新页（按 Win11 真实结构）。
 *
 * <p>设置组：检查更新 / 更多选项 / 高级选项 / 预览体验 / 故障排除。</p>
 */
@Slf4j
public class WindowsUpdatePage extends CategoryPage {

    public WindowsUpdatePage(Skin skin) {
        super("Windows 更新", skin);

        group("检查更新",
                SettingItem.value("更新状态", "", "✓ 你使用的是最新版本"),
                SettingItem.value("上次检查时间", "", "今日 14:23"),
                SettingItem.button("检查更新", "立即检查 Windows 更新", "检查更新",
                        () -> log.info("[Windows 更新] 检查更新")),
                SettingItem.value("可用更新", "", "无可用更新")
        );

        group("更多选项",
                SettingItem.button("更新历史记录", "查看已安装的更新", "查看"),
                SettingItem.button("卸载更新", "卸载最近的更新", "卸载"),
                SettingItem.select("暂停更新", "暂停累积更新", new String[]{"不暂停","暂停 1 周","暂停 2 周","暂停 3 周","暂停 4 周","暂停 5 周"}, "不暂停"),
                SettingItem.value("暂停截止日期", "", "未暂停"),
                SettingItem.button("检查联机 Microsoft 更新", "从 Microsoft Update 联机检查", "检查")
        );

        group("高级选项",
                SettingItem.toggle("在我使用 Windows 时通知更新完成", "不自动重启", true),
                SettingItem.toggle("自动下载更新", "即使计量连接", true),
                SettingItem.toggle("更新 Windows 时接收其他 Microsoft 产品", "同时更新 Office 等", true),
                SettingItem.toggle("使用计量连接", "限制后台下载", false),
                SettingItem.button("交付优化", "从本地网络/Internet 下载加速", "配置"),
                SettingItem.button("活动小时", "调整自动重启避开时间", "调整"),
                SettingItem.select("更新通知方式", "", new String[]{"横幅通知","声音","静默"}, "横幅通知")
        );

        group("Windows 预览体验计划",
                SettingItem.value("预览体验状态", "", "未加入"),
                SettingItem.link("加入 Windows 预览体验计划", "抢先体验预览版本", "加入"),
                SettingItem.value("频道", "", "—")
        );

        group("故障排除",
                SettingItem.button("Windows 更新疑难解答", "诊断并修复更新问题", "运行"),
                SettingItem.value("上次故障排除", "", "未运行过"),
                SettingItem.button("更新组件重置", "重置更新服务/缓存", "重置")
        );
    }
}
