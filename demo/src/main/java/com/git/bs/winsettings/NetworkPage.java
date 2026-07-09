package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

/**
 * 网络和 Internet 页（按 Win11 真实结构）。
 *
 * <p>设置组：Wi-Fi / 以太网 / VPN 与代理 / 移动热点 / 高级网络设置。</p>
 */
@Slf4j
public class NetworkPage extends CategoryPage {

    public NetworkPage(Skin skin) {
        super("网络和 Internet", skin);

        group("Wi-Fi",
                SettingItem.toggle("Wi-Fi", "启用无线网络连接", true),
                SettingItem.value("当前网络", "", "Home-5G (已连接, 安全)"),
                SettingItem.value("信号强度", "", "强 (4 格)"),
                SettingItem.toggle("随机硬件地址", "提高不同网络的隐私", false),
                SettingItem.button("管理已知网络", "查看/管理已保存的 Wi-Fi", "管理")
        );

        group("以太网",
                SettingItem.value("状态", "", "已连接"),
                SettingItem.value("IP 地址", "", "192.168.1.100"),
                SettingItem.value("子网掩码", "", "255.255.255.0"),
                SettingItem.value("DNS 服务器", "", "192.168.1.1"),
                SettingItem.select("IP 分配", "DHCP/手动", new String[]{"自动 (DHCP)","手动"}, "自动 (DHCP)")
        );

        group("VPN 与代理",
                SettingItem.button("VPN", "添加或管理 VPN 连接", "添加 VPN"),
                SettingItem.toggle("使用代理服务器", "通过代理访问 Internet", false),
                SettingItem.value("代理地址", "", "未配置"),
                SettingItem.toggle("自动检测设置", "自动检测代理配置", true)
        );

        group("移动热点",
                SettingItem.toggle("移动热点", "与其他设备共享网络", false),
                SettingItem.value("热点名称", "", "DESKTOP-BSUI 5243"),
                SettingItem.value("Internet 共享来源", "", "Wi-Fi"),
                SettingItem.button("编辑", "修改热点名称/密码", "编辑")
        );

        group("飞行模式与无线",
                SettingItem.toggle("飞行模式", "禁用所有无线通信", false),
                SettingItem.toggle("Wi-Fi", "", true),
                SettingItem.toggle("蓝牙", "", true)
        );

        group("高级网络设置",
                SettingItem.button("网络重置", "重置网络适配器和设置", "重置", () -> log.info("网络重置")),
                SettingItem.button("网络适配器选项", "查看适配器、IP 配置", "打开"),
                SettingItem.select("DNS over HTTPS", "加密 DNS", new String[]{"关闭","自动","手动"}, "关闭")
        );
    }
}
