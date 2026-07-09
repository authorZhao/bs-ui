package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

/**
 * 占位页：尚未实现的分类页用它兜底（保持 12 项导航都能点进去）。
 * 下一轮会逐个替换为真实分类页（BluetoothPage / NetworkPage / ...）。
 */
@Slf4j
class PlaceholderPage extends CategoryPage {

    PlaceholderPage(String title, Skin skin) {
        super(title, skin);
        group(title + "（建设中）",
                SettingItem.value("提示", "此分类页待实现，下一轮补全全部设置项", "敬请期待"),
                SettingItem.button("返回主页", "", "返回", null),
                SettingItem.link("占位演示", "", "点这里打日志", null)
        );
    }
}
