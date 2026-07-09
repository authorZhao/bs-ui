package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

/**
 * 隐私和安全性页（按 Win11 真实结构）。
 *
 * <p>设置组：Windows 安全中心 / 设备安全性 / 查找我的设备 / 开发者选项 /
 * 位置 / 相机与麦克风 / 通知与账户 / 诊断与活动。</p>
 */
@Slf4j
public class PrivacySecurityPage extends CategoryPage {

    public PrivacySecurityPage(Skin skin) {
        super("隐私和安全性", skin);

        group("Windows 安全中心",
                SettingItem.value("病毒和威胁防护", "", "✓ 已启用 (Microsoft Defender)"),
                SettingItem.value("防火墙和网络保护", "", "✓ 正常"),
                SettingItem.value("账户保护", "", "✓ 正常"),
                SettingItem.value("应用和浏览器控制", "", "✓ 正常"),
                SettingItem.button("打开 Windows 安全中心", "", "打开",
                        () -> log.info("打开 Windows 安全中心"))
        );

        group("设备安全性",
                SettingItem.toggle("内存完整性", "核心隔离,防止恶意代码注入", true),
                SettingItem.value("安全启动", "", "✓ 已启用"),
                SettingItem.toggle("TPM 2.0 加密", "BitLocker 设备加密", true),
                SettingItem.value("处理器型号", "", "支持安全功能"),
                SettingItem.button("设备安全性详细信息", "", "查看")
        );

        group("查找我的设备",
                SettingItem.toggle("查找我的设备", "允许设备位置保存到 Microsoft 账户", true),
                SettingItem.value("最近位置", "", "中国, 上海"),
                SettingItem.value("最后更新", "", "今日 09:12"),
                SettingItem.button("查看设备", "在 account.microsoft.com 定位", "查看")
        );

        group("开发者选项",
                SettingItem.toggle("开发者模式", "允许安装任意来源的应用", false),
                SettingItem.toggle("设备门户", "通过浏览器远程管理设备", false),
                SettingItem.toggle("PowerShell 远程签名", "", false),
                SettingItem.toggle("设备发现", "允许其他设备发现此 PC", false),
                SettingItem.value("设备 IP", "", "192.168.1.100"),
                SettingItem.link("开发者文档", "", "查看")
        );

        group("位置",
                SettingItem.toggle("位置服务", "", true),
                SettingItem.toggle("桌面应用可访问位置", "", true),
                SettingItem.value("默认位置", "", "上海"),
                SettingItem.button("清除位置历史", "", "清除")
        );

        group("相机与麦克风",
                SettingItem.toggle("摄像头访问", "", true),
                SettingItem.toggle("桌面应用可访问相机", "", true),
                SettingItem.toggle("麦克风访问", "", true),
                SettingItem.toggle("桌面应用可访问麦克风", "", true),
                SettingItem.button("相机隐私", "查看使用相机的应用", "查看")
        );

        group("通知与账户",
                SettingItem.toggle("允许应用发送通知", "", true),
                SettingItem.toggle("应用可访问账户信息", "", true),
                SettingItem.toggle("应用可访问联系人", "", false),
                SettingItem.toggle("应用可访问日历", "", false),
                SettingItem.toggle("应用可访问通话历史", "", false)
        );

        group("诊断与活动",
                SettingItem.toggle("发送可选诊断数据", "帮助 Microsoft 改进", false),
                SettingItem.toggle("个性化输入", "改进输入法和打字", true),
                SettingItem.toggle("按应用显示广告 ID", "", true),
                SettingItem.value("活动历史", "", "已记录 7 天活动"),
                SettingItem.button("查看诊断数据", "", "查看")
        );
    }
}
