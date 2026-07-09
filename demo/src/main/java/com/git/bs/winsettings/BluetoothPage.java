package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

/**
 * 蓝牙和其他设备页（按 Win11 真实结构）。
 *
 * <p>设置组：蓝牙 / 鼠标 / 键盘 / 触控 / 笔和 Windows Ink / 自动播放与 USB / 打印机和扫描仪。</p>
 */
@Slf4j
public class BluetoothPage extends CategoryPage {

    public BluetoothPage(Skin skin) {
        super("蓝牙和其他设备", skin);

        group("蓝牙",
                SettingItem.toggle("蓝牙", "发现可被其他设备检测到", true),
                SettingItem.value("已配对设备", "", "● 蓝牙耳机  ● 鼠标  ● 键盘"),
                SettingItem.button("添加设备", "添加蓝牙、鼠标、键盘、笔、控制器等无线设备", "+ 添加",
                        () -> log.info("添加蓝牙设备"))
        );

        group("鼠标",
                SettingItem.select("鼠标主按钮", "左/右键作为主按钮", new String[]{"右","左"}, "左"),
                SettingItem.select("滚轮每次滚动行数", "滚轮一次滚动的行数", new String[]{"1","2","3","5","屏幕"}, "3"),
                SettingItem.toggle("将鼠标悬停在窗口上时滚动", "悬停时滚动非活动窗口", true),
                SettingItem.toggle("光标在指针下移动", "根据指针移动方向调整光标", false)
        );

        group("键盘",
                SettingItem.select("重复速率", "按住按键时重复的速度", new String[]{"慢","较慢","中","较快","快"}, "中"),
                SettingItem.select("重复延迟", "重复开始前的延迟", new String[]{"短","中","长"}, "中"),
                SettingItem.toggle("使用筛选键", "忽略短暂或重复的按键", false),
                SettingItem.toggle("使用粘滞键", "一次按一个键实现组合键", false)
        );

        group("触控",
                SettingItem.toggle("触控灵敏度", "提高触控响应灵敏度", false),
                SettingItem.toggle("三指手势", "三指拖拽/点击", true),
                SettingItem.toggle("触控指示器", "显示触控点指示", false)
        );

        group("笔和 Windows Ink",
                SettingItem.toggle("笔手势", "用笔执行快捷手势", true),
                SettingItem.toggle("手写", "手写面板转文字", true),
                SettingItem.toggle("触控笔单击", "单击启动应用", true),
                SettingItem.button("校准笔", "校准笔输入", "校准")
        );

        group("自动播放与 USB",
                SettingItem.toggle("自动播放", "所有媒体和设备的自动播放", false),
                SettingItem.select("可移动驱动器", "默认操作", new String[]{"选择操作","打开文件夹","导入照片","不执行"}, "选择操作"),
                SettingItem.toggle("USB 通知", "USB 设备连接时通知", true),
                SettingItem.toggle("USB 节电", "USB 设备省电模式", true)
        );

        group("打印机和扫描仪",
                SettingItem.value("默认打印机", "", "HP LaserJet Pro"),
                SettingItem.button("添加打印机或扫描仪", "搜索并安装", "添加"),
                SettingItem.toggle("让 Windows 管理默认打印机", "", true)
        );
    }
}
