package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 蓝牙和其他设备页（按 Win11 真实结构）。
 *
 * <p>设置组：蓝牙 / 鼠标 / 键盘 / 触控 / 笔和 Windows Ink / 自动播放与 USB / 打印机和扫描仪。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BluetoothPage extends CategoryPage {

    public BluetoothPage(Skin skin) {
        super(BsI18n.get("nav.bluetooth"), skin);

        group(BsI18n.get("bluetooth.group_bluetooth"),
                SettingItem.toggle(BsI18n.get("bluetooth.bluetooth"), BsI18n.get("bluetooth.bluetooth_desc"), true),
                SettingItem.value(BsI18n.get("bluetooth.paired_devices"), "", BsI18n.get("bluetooth.paired_devices_value")),
                SettingItem.button(BsI18n.get("bluetooth.add_device"), BsI18n.get("bluetooth.add_device_desc"), BsI18n.get("bluetooth.add"),
                        () -> log.info("添加蓝牙设备"))
        );

        group(BsI18n.get("bluetooth.group_mouse"),
                SettingItem.select(BsI18n.get("bluetooth.mouse_primary_button"), BsI18n.get("bluetooth.mouse_primary_button_desc"), new String[]{BsI18n.get("bluetooth.right"), BsI18n.get("bluetooth.left")}, BsI18n.get("bluetooth.left")),
                SettingItem.select(BsI18n.get("bluetooth.scroll_lines"), BsI18n.get("bluetooth.scroll_lines_desc"), new String[]{"1", "2", "3", "5", BsI18n.get("bluetooth.screen")}, "3"),
                SettingItem.toggle(BsI18n.get("bluetooth.hover_scroll"), BsI18n.get("bluetooth.hover_scroll_desc"), true),
                SettingItem.toggle(BsI18n.get("bluetooth.cursor_move"), BsI18n.get("bluetooth.cursor_move_desc"), false)
        );

        group(BsI18n.get("bluetooth.group_keyboard"),
                SettingItem.select(BsI18n.get("bluetooth.repeat_rate"), BsI18n.get("bluetooth.repeat_rate_desc"), new String[]{BsI18n.get("bluetooth.slow"), BsI18n.get("bluetooth.slower"), BsI18n.get("bluetooth.medium"), BsI18n.get("bluetooth.faster"), BsI18n.get("bluetooth.fast")}, BsI18n.get("bluetooth.medium")),
                SettingItem.select(BsI18n.get("bluetooth.repeat_delay"), BsI18n.get("bluetooth.repeat_delay_desc"), new String[]{BsI18n.get("bluetooth.short"), BsI18n.get("bluetooth.medium"), BsI18n.get("bluetooth.long")}, BsI18n.get("bluetooth.medium")),
                SettingItem.toggle(BsI18n.get("bluetooth.filter_keys"), BsI18n.get("bluetooth.filter_keys_desc"), false),
                SettingItem.toggle(BsI18n.get("bluetooth.sticky_keys"), BsI18n.get("bluetooth.sticky_keys_desc"), false)
        );

        group(BsI18n.get("bluetooth.group_touch"),
                SettingItem.toggle(BsI18n.get("bluetooth.touch_sensitivity"), BsI18n.get("bluetooth.touch_sensitivity_desc"), false),
                SettingItem.toggle(BsI18n.get("bluetooth.three_finger_gestures"), BsI18n.get("bluetooth.three_finger_gestures_desc"), true),
                SettingItem.toggle(BsI18n.get("bluetooth.touch_indicator"), BsI18n.get("bluetooth.touch_indicator_desc"), false)
        );

        group(BsI18n.get("bluetooth.group_pen_ink"),
                SettingItem.toggle(BsI18n.get("bluetooth.pen_gestures"), BsI18n.get("bluetooth.pen_gestures_desc"), true),
                SettingItem.toggle(BsI18n.get("bluetooth.handwriting"), BsI18n.get("bluetooth.handwriting_desc"), true),
                SettingItem.toggle(BsI18n.get("bluetooth.pen_click"), BsI18n.get("bluetooth.pen_click_desc"), true),
                SettingItem.button(BsI18n.get("bluetooth.calibrate_pen"), BsI18n.get("bluetooth.calibrate_pen_desc"), BsI18n.get("bluetooth.calibrate"))
        );

        group(BsI18n.get("bluetooth.group_autoplay_usb"),
                SettingItem.toggle(BsI18n.get("bluetooth.autoplay"), BsI18n.get("bluetooth.autoplay_desc"), false),
                SettingItem.select(BsI18n.get("bluetooth.removable_drive"), BsI18n.get("bluetooth.removable_drive_desc"), new String[]{BsI18n.get("bluetooth.choose_action"), BsI18n.get("bluetooth.open_folder"), BsI18n.get("bluetooth.import_photos"), BsI18n.get("bluetooth.do_nothing")}, BsI18n.get("bluetooth.choose_action")),
                SettingItem.toggle(BsI18n.get("bluetooth.usb_notification"), BsI18n.get("bluetooth.usb_notification_desc"), true),
                SettingItem.toggle(BsI18n.get("bluetooth.usb_power_saving"), BsI18n.get("bluetooth.usb_power_saving_desc"), true)
        );

        group(BsI18n.get("bluetooth.group_printers"),
                SettingItem.value(BsI18n.get("bluetooth.default_printer"), "", "HP LaserJet Pro"),
                SettingItem.button(BsI18n.get("bluetooth.add_printer"), BsI18n.get("bluetooth.add_printer_desc"), BsI18n.get("bluetooth.add")),
                SettingItem.toggle(BsI18n.get("bluetooth.let_windows_manage_printer"), "", true)
        );
    }
}
