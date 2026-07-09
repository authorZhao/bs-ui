package com.git.bs.winsettings;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ObjectMap;
import com.git.bs.ui.BsDarkTheme;
import com.git.bs.ui.BsLightTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * Windows 设置（bs-ui 复刻）入口：加载烘焙 skin + Light 主题。
 *
 * <p>不走运行时字体生成 —— 设置界面用正常字号，烘焙 skin 的 sm/md/lg/xl 完全够用。</p>
 */
@Slf4j
public class WinSettingsApp extends Game {

    @Override
    public void create() {
        BsUI.init();
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
    }

    @Override
    public void dispose() {
        super.dispose();
        // 烘焙 skin 字体由 app 主动释放（字体全皮肤公用，Set 去重）
        Set<BitmapFont> fontSet = new HashSet<>();
        for (Skin s : BsUI.registeredSkins()) {
            if (s == null) continue;
            ObjectMap<String, BitmapFont> all = s.getAll(BitmapFont.class);
            for (ObjectMap.Entry<String, BitmapFont> e : all) {
                fontSet.add(e.value);
                s.remove(e.key, BitmapFont.class);
            }
        }
        for (BitmapFont f : fontSet) {
            try { f.dispose(); } catch (Throwable ignored) {}
        }
        BsUI.dispose();
    }
}
