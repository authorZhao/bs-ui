package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 网络和 Internet 页（按 Win11 真实结构）。
 *
 * <p>设置组：Wi-Fi / 以太网 / VPN 与代理 / 移动热点 / 高级网络设置。</p>
 */
@Slf4j
public class NetworkPage extends CategoryPage {

    public NetworkPage(Skin skin) {
        super(BsI18n.get("nav.network"), skin);

        group("Wi-Fi",
                SettingItem.toggle("Wi-Fi", BsI18n.get("network.wifi_enable_desc"), true),
                SettingItem.value(BsI18n.get("network.current_network"), "", BsI18n.get("network.current_network_value")),
                SettingItem.value(BsI18n.get("network.signal_strength"), "", BsI18n.get("network.signal_strength_value")),
                SettingItem.toggle(BsI18n.get("network.random_hardware_address"), BsI18n.get("network.random_hardware_address_desc"), false),
                SettingItem.button(BsI18n.get("network.manage_known_networks"), BsI18n.get("network.manage_known_networks_desc"), BsI18n.get("network.manage"))
        );

        group(BsI18n.get("network.group_ethernet"),
                SettingItem.value(BsI18n.get("network.status"), "", BsI18n.get("network.status_connected")),
                SettingItem.value(BsI18n.get("network.ip_address"), "", "192.168.1.100"),
                SettingItem.value(BsI18n.get("network.subnet_mask"), "", "255.255.255.0"),
                SettingItem.value(BsI18n.get("network.dns_server"), "", "192.168.1.1"),
                SettingItem.select(BsI18n.get("network.ip_assignment"), BsI18n.get("network.ip_assignment_desc"), new String[]{BsI18n.get("network.automatic_dhcp"), BsI18n.get("network.manual")}, BsI18n.get("network.automatic_dhcp"))
        );

        group(BsI18n.get("network.group_vpn_proxy"),
                SettingItem.button("VPN", BsI18n.get("network.vpn_desc"), BsI18n.get("network.add_vpn")),
                SettingItem.toggle(BsI18n.get("network.use_proxy"), BsI18n.get("network.use_proxy_desc"), false),
                SettingItem.value(BsI18n.get("network.proxy_address"), "", BsI18n.get("network.not_configured")),
                SettingItem.toggle(BsI18n.get("network.auto_detect"), BsI18n.get("network.auto_detect_desc"), true)
        );

        group(BsI18n.get("network.group_mobile_hotspot"),
                SettingItem.toggle(BsI18n.get("network.mobile_hotspot"), BsI18n.get("network.mobile_hotspot_desc"), false),
                SettingItem.value(BsI18n.get("network.hotspot_name"), "", "DESKTOP-BSUI 5243"),
                SettingItem.value(BsI18n.get("network.internet_sharing_source"), "", "Wi-Fi"),
                SettingItem.button(BsI18n.get("network.edit"), BsI18n.get("network.edit_hotspot_desc"), BsI18n.get("network.edit"))
        );

        group(BsI18n.get("network.group_airplane"),
                SettingItem.toggle(BsI18n.get("network.airplane_mode"), BsI18n.get("network.airplane_mode_desc"), false),
                SettingItem.toggle("Wi-Fi", "", true),
                SettingItem.toggle(BsI18n.get("bluetooth.bluetooth"), "", true)
        );

        group(BsI18n.get("network.group_advanced"),
                SettingItem.button(BsI18n.get("network.network_reset"), BsI18n.get("network.network_reset_desc"), BsI18n.get("network.reset"), () -> log.info("网络重置")),
                SettingItem.button(BsI18n.get("network.adapter_options"), BsI18n.get("network.adapter_options_desc"), BsI18n.get("common.open")),
                SettingItem.select("DNS over HTTPS", BsI18n.get("network.doh_desc"), new String[]{BsI18n.get("common.off"), BsI18n.get("common.on"), BsI18n.get("network.manual")}, BsI18n.get("common.off"))
        );
    }
}
