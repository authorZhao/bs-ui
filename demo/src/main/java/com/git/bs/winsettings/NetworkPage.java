package com.git.bs.winsettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.git.bs.i18n.BsI18n;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsModal;
import com.git.bs.ui.BsSelectBox;
import com.git.bs.ui.BsSwitch;
import com.git.bs.ui.BsText;
import com.git.bs.ui.BsTextField;
import lombok.extern.slf4j.Slf4j;

/**
 * 网络和 Internet 页（按 Win11 真实结构）。
 *
 * <p>设置组：Wi-Fi / 以太网 / VPN 与代理 / 移动热点 / 高级网络设置。</p>
 * @author authorZhao
 * @since 2026-07-16
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
                SettingItem.button(BsI18n.get("network.manage_known_networks"), BsI18n.get("network.manage_known_networks_desc"), BsI18n.get("network.manage"),
                        () -> showKnownNetworksDialog())
        );

        group(BsI18n.get("network.group_ethernet"),
                SettingItem.value(BsI18n.get("network.status"), "", BsI18n.get("network.status_connected")),
                SettingItem.value(BsI18n.get("network.ip_address"), "", "192.168.1.100"),
                SettingItem.value(BsI18n.get("network.subnet_mask"), "", "255.255.255.0"),
                SettingItem.value(BsI18n.get("network.dns_server"), "", "192.168.1.1"),
                SettingItem.select(BsI18n.get("network.ip_assignment"), BsI18n.get("network.ip_assignment_desc"), new String[]{BsI18n.get("network.automatic_dhcp"), BsI18n.get("network.manual")}, BsI18n.get("network.automatic_dhcp"))
        );

        group(BsI18n.get("network.group_vpn_proxy"),
                SettingItem.button("VPN", BsI18n.get("network.vpn_desc"), BsI18n.get("network.add_vpn"),
                        () -> showAddVpnDialog()),
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

    // =================== 模态框 ===================

    /** 取当前 stage（WinSettingsScreen.show 里 setInputProcessor(stage)）。 */
    private static Stage currentStage() {
        return (Stage) Gdx.input.getInputProcessor();
    }

    /** 管理已知网络：模拟列表 + 忘记/属性操作。 */
    private void showKnownNetworksDialog() {
        Stage stage = currentStage();
        if (stage == null) return;

        Table body = new Table(skin);
        body.pad(4);
        body.defaults().growX().left().padBottom(4);

        body.add(new BsText(BsI18n.get("network.known_networks_hint"), BsText.Size.SM, BsText.Variant.MUTED)).padBottom(10).row();

        // 模拟 3 个已知网络
        String[][] networks = {
                {"Home-WiFi-5G", "已连接", "信号强"},
                {"Office-Guest", "未连接", "信号中"},
                {"CoffeeShop-Free", "未连接", "信号弱"},
        };
        for (String[] net : networks) {
            Table row = new Table();
            row.left();
            row.defaults().left().padRight(12);
            row.add(new BsText(net[0], BsText.Size.DEFAULT).bold()).padRight(16);
            row.add(new BsText(net[1], BsText.Size.SM, "已连接".equals(net[1]) ? BsText.Variant.SUCCESS : BsText.Variant.MUTED));
            row.add(new BsText(net[2], BsText.Size.SM, BsText.Variant.SECONDARY)).growX();

            BsButton forgetBtn = new BsButton(BsI18n.get("network.forget"), skin,
                    BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
            final String netName = net[0];
            forgetBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y) {
                    log.info("[网络] 忘记网络 {}", netName);
                }
            });
            row.add(forgetBtn).right();
            body.add(row).growX().row();
        }

        final BsModal modal = new BsModal(BsI18n.get("network.manage_known_networks"), skin);
        modal.content(body).contentWidth(460).separator(true);
        modal.addButton(BsI18n.get("home.close"), modal::close, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        modal.showModal(stage);
    }

    /** 添加 VPN：连接名 / 服务器 / 类型 / 用户名密码。 */
    private void showAddVpnDialog() {
        Stage stage = currentStage();
        if (stage == null) return;

        Table body = new Table(skin);
        body.pad(4);
        body.defaults().growX().left().pad(5);

        // 连接名
        body.add(new BsText(BsI18n.get("network.vpn_connection_name"), BsText.Size.SM, BsText.Variant.MUTED)).padTop(4).row();
        BsTextField nameField = new BsTextField("", skin);
        nameField.setMessageText("My VPN");
        body.add(nameField).growX().row();

        // 服务器名或地址
        body.add(new BsText(BsI18n.get("network.vpn_server"), BsText.Size.SM, BsText.Variant.MUTED)).padTop(8).row();
        BsTextField serverField = new BsTextField("", skin);
        serverField.setMessageText("vpn.example.com");
        body.add(serverField).growX().row();

        // VPN 类型
        body.add(new BsText(BsI18n.get("network.vpn_type"), BsText.Size.SM, BsText.Variant.MUTED)).padTop(8).row();
        BsSelectBox<String> typeBox = new BsSelectBox<>(skin);
        typeBox.setItems(
                "L2TP/IPsec",
                "PPTP",
                "IKEv2",
                "SSTP",
                "OpenVPN",
                "WireGuard"
        );
        body.add(typeBox).growX().row();

        // 用户名
        body.add(new BsText(BsI18n.get("network.vpn_username"), BsText.Size.SM, BsText.Variant.MUTED)).padTop(8).row();
        BsTextField userField = new BsTextField("", skin);
        body.add(userField).growX().row();

        // 密码
        body.add(new BsText(BsI18n.get("network.vpn_password"), BsText.Size.SM, BsText.Variant.MUTED)).padTop(8).row();
        BsTextField passField = new BsTextField("", skin);
        passField.setPasswordMode(true);
        passField.setPasswordCharacter('•');
        body.add(passField).growX().row();

        // 记住凭据
        Table rememberRow = new Table();
        rememberRow.left();
        rememberRow.defaults().left().padRight(10);
        rememberRow.add(new BsText(BsI18n.get("network.vpn_remember"), BsText.Size.DEFAULT));
        BsSwitch rememberSw = new BsSwitch(skin);
        rememberSw.setChecked(true);
        rememberRow.add(rememberSw);
        body.add(rememberRow).padTop(10).row();

        final BsModal modal = new BsModal(BsI18n.get("network.add_vpn"), skin);
        modal.content(body).contentWidth(420).separator(true);
        modal.addButton(BsI18n.get("home.cancel"), modal::close, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        modal.addButton(BsI18n.get("network.save"), () -> {
                    log.info("[网络] 添加 VPN: name={}, server={}, type={}",
                            nameField.getText(), serverField.getText(), typeBox.getSelected());
                    modal.close();
                },
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID);
        modal.showModal(stage);
    }
}
