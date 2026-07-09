package com.git.bs.winsettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.git.bs.ui.BsButton;
import com.git.bs.i18n.BsI18n;
import com.git.bs.ui.BsScrollPane;
import com.git.bs.ui.BsText;
import com.git.bs.ui.BsTextField;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Win11 设置主屏：左导航（12 项）+ 右内容区（路由切换）。
 *
 * <p>结构：</p>
 * <ul>
 *   <li>左栏 240px：圆形头像 + 搜索框 + 12 个分类导航项（选中高亮）</li>
 *   <li>右栏 grow：{@link BsScrollPane} 包内容区，navigate(key) 换内容为对应页的 buildView</li>
 * </ul>
 *
 * <p>实现 {@link Router}，主页卡片点击 / 导航项点击都走 {@link #navigate(String)}。
 * 支持「初始页」构造参数：主题切换重建 screen 时传入 {@link #currentKey()} 保持当前页。</p>
 */
@Slf4j
public class WinSettingsScreen implements Screen, Router {

    private Stage stage;
    private Skin skin;
    private final Map<String, SettingsPage> pages = new LinkedHashMap<>();
    private NavItem[] navItems;
    private Table contentArea;
    /** 当前所在页 key（主题切换重建时用它保持页面）。 */
    private String current;

    /** {key, 图标符号, 导航文案的 i18n key} —— 渲染时调 BsI18n.get 取文案，与 HomePage 卡片 key 对齐。 */
    private static final String[][] NAV = {
            {"home",           "⌂", "nav.home"},
            {"system",         "🖥", "nav.system"},
            {"bluetooth",      "ⓑ", "nav.bluetooth"},
            {"network",        "📶", "nav.network"},
            {"personalization","🎨", "nav.personalization"},
            {"apps",           "⊞", "nav.apps"},
            {"accounts",       "👤", "nav.accounts"},
            {"timelanguage",   "🕐", "nav.timelanguage"},
            {"gaming",         "🎮", "nav.gaming"},
            {"accessibility",  "♿", "nav.accessibility"},
            {"privacy",        "🔒", "nav.privacy"},
            {"update",         "⟳", "nav.update"},
    };

    public WinSettingsScreen() {
        this("home");
    }

    /** 主题切换重建时用：传入要停留的页面 key。 */
    public WinSettingsScreen(String initialKey) {
        this.current = initialKey == null ? "home" : initialKey;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = BsUI.getSkin();

        // 点击非输入框区域（按钮/导航/空白等）→ 清除键盘焦点，搜索框光标停止闪烁。
        // 单纯鼠标移动不失焦（保持 Win11 行为）。仅处理本程序内点击。
        stage.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int pointer, int button) {
                if (!(e.getTarget() instanceof com.badlogic.gdx.scenes.scene2d.ui.TextField)) {
                    stage.setKeyboardFocus(null);
                }
                return false;   // 不消费，让目标 actor 正常处理点击
            }
        });

        // 注册页面：home + system 已完整实现，其余 10 页暂用占位（下一轮批量补全）
        pages.put("home", new HomePage(skin));
        pages.put("system", new SystemPage(skin));
        pages.put("bluetooth", new BluetoothPage(skin));
        pages.put("network", new NetworkPage(skin));
        pages.put("personalization", new PersonalizationPage(skin));
        pages.put("apps", new AppsPage(skin));
        pages.put("accounts", new AccountsPage(skin));
        pages.put("update", new WindowsUpdatePage(skin));
        pages.put("timelanguage", new TimeLanguagePage(skin));
        pages.put("gaming", new GamingPage(skin));
        pages.put("accessibility", new AccessibilityPage(skin));
        pages.put("privacy", new PrivacySecurityPage(skin));
        // 二级页面（子页）：点 PAGE 行的 › 箭头进入
        pages.put("apps/installed", new InstalledAppsPage(skin));
        pages.put("system/display", new DisplayPage(skin));
        pages.put("system/sound", new SoundPage(skin));
        for (String[] n : NAV) {
            pages.putIfAbsent(n[0], new PlaceholderPage(BsI18n.get(n[2]), skin));
        }

        Table root = new Table();
        root.setFillParent(true);
        root.left().top();
        root.add(buildNav()).width(240).growY().top().left();

        contentArea = new Table();
        contentArea.left().top();
        BsScrollPane contentScroll = new BsScrollPane(contentArea, skin);
        contentScroll.setScrollingDisabled(true, false);
        contentScroll.setFadeScrollBars(false);
        root.add(contentScroll).grow().top().left();

        stage.addActor(root);
        navigate(current);
    }

    /** 左导航：圆形头像 + 搜索框 + 12 个 NavItem。 */
    private Actor buildNav() {
        Table nav = new Table();
        nav.top().left();
        nav.setBackground(skin.getDrawable("bs-window-bg"));
        nav.defaults().growX().left();

        // 圆形头像 + 名字（导航栏最顶）
        Drawable avD = skin.has("bs-circle", Drawable.class)
                ? skin.getDrawable("bs-circle")
                : skin.newDrawable("white", BsTheme.colorOf("primary"));
        Image avatar = new Image(avD);
        avatar.setScaling(Scaling.stretch);
        avatar.setColor(BsTheme.colorOf("primary"));
        Table profile = new Table();
        profile.left();
        profile.defaults().left().pad(10, 14, 4, 14);
        profile.add(avatar).size(34).padRight(10);
        profile.add(new BsText("authorZhao", BsText.Size.DEFAULT).bold());
        nav.add(profile).growX().row();

        BsTextField search = new BsTextField("", skin);
        search.setMessageText(BsI18n.get("nav.search"));
        nav.add(search).growX().pad(12).row();

        Table navList = new Table();
        navList.left().top();
        navList.defaults().growX().left();
        navItems = new NavItem[NAV.length];
        for (int i = 0; i < NAV.length; i++) {
            final String key = NAV[i][0];
            NavItem item = new NavItem(skin, key, NAV[i][1], BsI18n.get(NAV[i][2]), () -> navigate(key));
            navList.add(item).growX().row();
            navItems[i] = item;
        }
        nav.add(navList).growX().top().pad(4, 8, 8, 8).row();
        nav.add().growY();   // 撑底
        return nav;
    }

    @Override
    public void navigate(String key) {
        SettingsPage p = pages.get(key);
        if (p == null) {
            log.warn("页面未实现: {}，使用占位", key);
            p = new PlaceholderPage(key, skin);   // 兜底：任何未注册的 key 都显示"建设中"，不空白
        }
        this.current = key;
        log.info("导航到 {}", key);
        for (NavItem it : navItems) it.setSelected(it.key.equals(key));
        contentArea.clearChildren();
        contentArea.add(p.buildView(this)).growX().top().left();
        contentArea.row();
        contentArea.add().growX().height(220);   // 底部留白：扩大滚动范围，避免内容刚好≈视口高时边界抖动
    }

    /** 当前页 key —— 主题切换重建时用它保持页面。 */
    public String currentKey() {
        return current;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(BsTheme.bgBodyColor(), true);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { if (stage != null) stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { if (stage != null) stage.dispose(); }

    /** 导航项：ghost 无边框按钮。未选中透明无边框（hover 微亮），选中 solid primary（蓝填充，明显）。 */
    private class NavItem extends BsButton {
        final String key;
        private final Skin itemSkin;

        NavItem(Skin skin, String key, String iconSym, String text, Runnable onClick) {
            super(iconSym + "  " + text, skin, BsButton.Variant.SECONDARY, BsButton.Style.GHOST, BsButton.Size.MD);
            this.key = key;
            this.itemSkin = skin;
            left();
            addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { onClick.run(); }
            });
            setSelected(false);
        }

        void setSelected(boolean s) {
            try {
                String styleName = s ? "bs-btn-primary" : "bs-btn-ghost-secondary";
                setStyle(itemSkin.get(styleName, com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle.class));
            } catch (Throwable ignored) {}
        }
    }
}
