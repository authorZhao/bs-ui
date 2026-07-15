package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.i18n.BsI18n;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

/**
 * 占位页：尚未实现的分类页用它兜底（保持 12 项导航都能点进去）。
 * 下一轮会逐个替换为真实分类页（BluetoothPage / NetworkPage / ...）。
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
class PlaceholderPage extends CategoryPage {

    PlaceholderPage(String title, Skin skin) {
        super(title, skin);
        group(title + BsI18n.get("placeholder.building"),
                SettingItem.value(BsI18n.get("placeholder.tip"),
                        BsI18n.get("placeholder.tip_desc"), BsI18n.get("placeholder.coming_soon")),
                SettingItem.button(BsI18n.get("placeholder.back_home"), "", BsI18n.get("placeholder.back"), null),
                SettingItem.link(BsI18n.get("placeholder.demo"), "", BsI18n.get("placeholder.click_log"), null)
        );
    }
}
