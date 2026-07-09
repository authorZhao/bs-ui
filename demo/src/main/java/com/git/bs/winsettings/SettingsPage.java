package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/** 所有设置页基类（主页 / 各分类页）。buildView 由调用方（WinSettingsScreen）塞进内容区。 */
public abstract class SettingsPage {

    protected final Skin skin;

    protected SettingsPage(Skin skin) {
        this.skin = skin;
    }

    /** 构建页面根 Actor。Router 用于主页卡片点击跳转。 */
    public abstract Actor buildView(Router router);
}
