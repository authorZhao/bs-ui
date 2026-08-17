/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库
 * Copyright (c) 2026 bs-ui contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */

package cn.pingyuanren.bs.winsettings;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ObjectMap;
import cn.pingyuanren.bs.i18n.BsI18n;
import cn.pingyuanren.bs.ui.BsDarkTheme;
import cn.pingyuanren.bs.ui.BsLightTheme;
import cn.pingyuanren.bs.ui.BsSkin;
import cn.pingyuanren.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * Windows 设置（bs-ui 复刻）入口：加载烘焙 skin + Light 主题。
 *
 * <p>不走运行时字体生成 —— 设置界面用正常字号，烘焙 skin 的 sm/md/lg/xl 完全够用。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class WinSettingsApp extends Game {

    @Override
    public void create() {
        // 资源 pak 加解密（必须在 BsUI.init 之前；无 pak 时自动跳过，走明文 classpath）
        cn.pingyuanren.bs.res.PakBootstrap.init();
        BsUI.init();
        // 注册 winsettings 业务翻译包（core 自带的通用 key 之外，业务专属 key 在这里）
        BsI18n.addBundle("cn/pingyuanren/bs/demo/i18n/");
        BsI18n.init();                          // i18n：加载 core + demo 业务包，默认 zh_cn
        cn.pingyuanren.bs.ui.BsEmoji.load();          // 加载 emoji atlas（WinRow/NavItem 用彩色 emoji 替代文字 emoji）
        cn.pingyuanren.bs.ui.BsEmoji.loadHeads();     // 加载头像 atlas（左导航栏默认头像）
        BsUI.setTheme(BsDarkTheme.INSTANCE);   // Win11 设置亮色风格
        setScreen(new WinSettingsScreen());
        // 主题切换（个性化卡「色彩模式」选亮/暗）→ 重建 screen，让所有 actor 用新 skin。
        // 重建时传入当前页 key，保持页面不跳回主页。
        BsUI.get().addOnThemeChangeListener(theme ->
                Gdx.app.postRunnable(() -> {
                    String key = (getScreen() instanceof WinSettingsScreen)
                            ? ((WinSettingsScreen) getScreen()).currentKey() : "home";
                    setScreen(new WinSettingsScreen(key));
                }));
        // 语言切换 → 同样重建 screen，让所有 actor 用新语言文案（仿主题切换）
        BsI18n.addListener(() -> Gdx.app.postRunnable(() -> {
            String key = (getScreen() instanceof WinSettingsScreen)
                    ? ((WinSettingsScreen) getScreen()).currentKey() : "home";
            setScreen(new WinSettingsScreen(key));
        }));
    }

    @Override
    public void dispose() {
        super.dispose();
        BsUI.registeredSkins().forEach(Skin::dispose);
        BsSkin.disposeFontCache();
        cn.pingyuanren.bs.ui.BsEmoji.dispose();
        cn.pingyuanren.bs.ui.BsIcon.dispose();
        BsUI.dispose();
    }
}
