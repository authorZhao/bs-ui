package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统设置页（完整示例）—— 演示 CategoryPage 的声明式用法。
 *
 * <p>涵盖 Win11 系统分类的主要设置组：显示 / 声音 / 通知与专注 / 电源与电池 / 存储 / 关于。
 * 所有操作只打日志（{@link CategoryPage} 统一处理），少数项演示带业务回调。</p>
 */
@Slf4j
public class SystemPage extends CategoryPage {

    public SystemPage(Skin skin) {
        super("系统", skin);

        // 二级页面入口（Win11 系统页顶部：每行带 › 箭头进子页）
        group("",
                SettingItem.page("🖥", "显示", "分辨率、缩放、夜间模式、HDR", "system/display"),
                SettingItem.page("🔊", "声音", "音量、输出/输入设备", "system/sound"),
                SettingItem.page("🔔", "通知", "应用通知、专注助手、勿扰", "system/notifications"),
                SettingItem.page("🔋", "电源和电池", "电源模式、屏幕睡眠、电池", "system/power"),
                SettingItem.page("💾", "存储", "存储感知、磁盘空间、临时文件", "system/storage")
        );

        group("显示",
                SettingItem.value("分辨率", "显示器原生分辨率", "1920 × 1080 (推荐)"),
                SettingItem.value("缩放与布局", "文本/图标大小", "100%"),
                SettingItem.toggle("夜间模式", "减弱蓝光，夜间更护眼", false, c -> log.info("应用夜间模式: {}", c)),
                SettingItem.select("显示方向", "横向 / 纵向", new String[]{"横向", "纵向", "横向翻转", "纵向翻转"}, "横向")
        );

        group("声音",
                SettingItem.select("输出设备", "扬声器 / 耳机",
                        new String[]{"扬声器 (Realtek Audio)", "耳机", "HDMI 输出"}, "扬声器 (Realtek Audio)"),
                SettingItem.select("输入设备", "麦克风",
                        new String[]{"麦克风阵列 (Realtek)", "线路输入"}, "麦克风阵列 (Realtek)"),
                SettingItem.toggle("空间音频", "Windows Sonic for Headphones", false)
        );

        group("通知与专注",
                SettingItem.toggle("通知", "允许应用显示通知", true),
                SettingItem.toggle("专注助手", "屏蔽通知保持专注", false),
                SettingItem.toggle("勿扰模式", "按规则自动开启勿扰", false)
        );

        group("电源与电池",
                SettingItem.select("电源模式", "平衡 / 最佳性能 / 节能",
                        new String[]{"平衡", "最佳性能", "最佳能效"}, "平衡"),
                SettingItem.select("屏幕和睡眠", "无操作多久关闭屏幕",
                        new String[]{"5 分钟", "10 分钟", "15 分钟", "30 分钟", "从不"}, "10 分钟"),
                SettingItem.button("电源和睡眠", "高级电源设置", "打开", () -> log.info("打开电源选项"))
        );

        group("存储",
                SettingItem.value("C 盘 (系统)", "系统盘使用情况", "128 GB / 256 GB (50%)"),
                SettingItem.toggle("存储感知", "自动清理临时文件释放空间", true),
                SettingItem.button("临时文件", "清理临时文件释放空间", "清理", () -> log.info("清理临时文件"))
        );

        group("关于",
                SettingItem.value("设备名称", "", "DESKTOP-BSUI"),
                SettingItem.value("处理器", "", "Intel Core i7-12700H @ 2.30GHz"),
                SettingItem.value("已安装的内存 (RAM)", "", "16.0 GB (可用 15.8 GB)"),
                SettingItem.value("系统类型", "", "64 位操作系统, x64 处理器"),
                SettingItem.value("版本", "", "Windows 11 专业版 23H2"),
                SettingItem.link("Windows 规格", "", "查看规格", () -> log.info("查看 Windows 规格"))
        );
    }
}
