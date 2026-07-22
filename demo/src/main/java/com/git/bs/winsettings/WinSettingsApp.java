package com.git.bs.winsettings;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ObjectMap;
import com.git.bs.i18n.BsI18n;
import com.git.bs.ui.BsDarkTheme;
import com.git.bs.ui.BsLightTheme;
import com.git.bs.ui.BsSkin;
import com.git.bs.ui.BsUI;
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
        // P1 spike：资源 pak 包装（-Dbs.pak.spike=true 启用）。必须在 BsUI.init() 之前。
        com.git.bs.res.PakBootstrap.init();
        BsUI.init();
        // P1 spike 观测：皮肤加载后，打印从内存包服务过的路径（空 = 未启用 spike）
        java.util.Set<String> served = com.git.bs.res.PakFileHandle.servedPaths();
        log.info("[pak-spike] 已从内存包服务 {} 个资源：", served.size());
        served.forEach(p -> log.info("[pak-spike]   pak> {}", p));
        // spike 验证：bs.pak.spike=exit 或 bs.exit=true 时，打印后 1s 自动退出（无界面拿干净日志）
        boolean doExit = "exit".equalsIgnoreCase(System.getProperty("bs.pak.spike"))
                || "true".equalsIgnoreCase(System.getProperty("bs.exit"));
        if (doExit) {
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override public void run() { Gdx.app.exit(); }
            }, 1f);
        }
        // 注册 winsettings 业务翻译包（core 自带的通用 key 之外，业务专属 key 在这里）
        BsI18n.addBundle("com/git/bs/demo/i18n/");
        BsI18n.init();                          // i18n：加载 core + demo 业务包，默认 zh_cn
        com.git.bs.ui.BsEmoji.load();          // 加载 emoji atlas（WinRow/NavItem 用彩色 emoji 替代文字 emoji）
        com.git.bs.ui.BsEmoji.loadHeads();     // 加载头像 atlas（左导航栏默认头像）
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
        com.git.bs.ui.BsEmoji.dispose();
        BsUI.dispose();
    }
}
