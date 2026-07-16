package com.git.bs.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.git.bs.admin.AdminLoginScreen;
import com.git.bs.common.SkinUtil;
import com.git.bs.ui.BsAdminTheme;
import com.git.bs.ui.BsDarkTheme;
import com.git.bs.ui.BsLightTheme;
import com.git.bs.ui.BsSkinLoader;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsUI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin 模板入口 App，骨架完全镜像 {@link BsSkinApp}：
 * 字体池 / BsSkinLoader 加载 / 主题切换 / applyTheme 重建 screen，
 * 唯一差别是进入 {@link AdminLoginScreen} 而非 BsControlsSkinScreen。
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class AdminApp extends Game {

    private static final String SKIN_CP = "com/git/bs/ui/skin";
    private static final String TTF = SKIN_CP + "/LXGWWenKaiScreen.ttf";
    private static final String CHARS = SKIN_CP + "/chinese.txt";
    private static final String CHARS_COMMON = SKIN_CP + "/common.txt";

    /** 导出皮肤时复用的 TTF 文件 classpath 路径。 */
    public String ttfPath() { return TTF; }
    /** 导出皮肤时复用的完整字符集（含生僻字）。 */
    public String charsPath() { return CHARS; }
    /** 导出皮肤时复用的精简字符集（8000+ 常用汉字 + ASCII）。 */
    public String charsPathCommon() { return CHARS_COMMON; }

    /**
     * 返回实际可用的字符集条目（显示名 → classpath 路径），按优先级排序。
     * 仅返回 classpath 里真实存在的文件，避免导出对话框暴露不存在的选项。
     */
    public java.util.List<String[]> availableCharsEntries() {
        java.util.List<String[]> entries = new java.util.ArrayList<>();
        if (Gdx.files.internal(CHARS_COMMON).exists()) {
            entries.add(new String[]{"精简字符集 (common.txt)", CHARS_COMMON});
        }
        if (Gdx.files.internal(CHARS).exists()) {
            entries.add(new String[]{"完整字符集 (chinese.txt)", CHARS});
        }
        return entries;
    }

    @Getter
    private Skin skin;

    private static final String[][] FONT_SIZES = {
            {"sm", "14"},
            {"md", "18"},
            {"lg", "24"},
            {"xl", "32"},
    };

    @Getter
    private final Map<String, BitmapFont> fonts = new HashMap<>();

    @Getter
    private BsTheme currentTheme = BsAdminTheme.INSTANCE;

    private String chars;

    public interface FontsReadyListener { void onFontsReady(); }
    private FontsReadyListener fontsReadyListener;
    public void setFontsReadyListener(FontsReadyListener l) { this.fontsReadyListener = l; }

    private volatile int loadedCount = 0;

    private Stage overlayStage;
    private Table loadingOverlay;
    private boolean themeSwitching = false;

    @Override
    public void create() {
        long t0 = System.currentTimeMillis();
        log.info("AdminApp init: 同步加载 default 字体 + 分帧预热 4 档字号 t={}", t0);
        BsUI.init();
        long t1 = System.currentTimeMillis();
        log.info("BsControlsTest 所有主题皮肤加载完毕 t1={}", t1);

        BsUI.setTheme(currentTheme);
        skin = BsUI.getSkin();

        var fontCache = SkinUtil.getFontCache(skin);
        fonts.putAll(fontCache);


        overlayStage = new Stage(new ScreenViewport());
        buildLoadingOverlay();

        // 进入登录页（与 BsSkinApp 的唯一差别）
        setScreen(new AdminLoginScreen(this));

        BsUI.get().addOnThemeChangeListener(theme ->
                Gdx.app.postRunnable(() -> applyTheme(theme)));
    }




    /** 主题切换：重建 screen。必须在 GL 线程调用。 */
    public synchronized void applyTheme(BsTheme theme) {
        if (themeSwitching) {
            log.warn("applyTheme 重入，忽略");
            return;
        }
        if (theme == currentTheme) {
            log.info("applyTheme 主题未变化，忽略");
            return;
        }
        themeSwitching = true;
        log.info("applyTheme: 开始切换 {} -> {}", currentTheme.name(), theme.name());

        showLoadingOverlay(true);

        Gdx.app.postRunnable(() -> {
            try {
                com.badlogic.gdx.Screen oldScreen = getScreen();

                this.currentTheme = theme;
                this.skin = BsUI.getSkin();
                for (String suffix : new String[]{"sm", "md", "lg", "xl"}) {
                    BitmapFont f = fonts.get(suffix);
                    if (f != null) BsSkinLoader.bindFontStyles(this.skin, suffix, f);
                }

                // 切换后根据登录态决定进入哪个 screen
                com.badlogic.gdx.Screen next = com.git.bs.admin.AdminContext.get().isLogged()
                        ? new com.git.bs.admin.BsAdminShell(this)
                        : new AdminLoginScreen(this);
                setScreen(next);

                if (oldScreen != null) {
                    try { oldScreen.dispose(); } catch (Throwable t) { log.warn("oldScreen dispose", t); }
                }

                log.info("applyTheme: 切换完成");
            } catch (Throwable t) {
                log.error("applyTheme 失败", t);
            } finally {
                showLoadingOverlay(false);
                themeSwitching = false;
            }
        });
    }

    private void buildLoadingOverlay() {
        loadingOverlay = new Table();
        loadingOverlay.setFillParent(true);
        loadingOverlay.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                makeSolidTexture(new Color(0, 0, 0, 0.5f))));
        Label label = new Label("正在切换主题", new Label.LabelStyle(
                fonts.get("default"), Color.WHITE));
        loadingOverlay.add(label).padBottom(20).row();
        Table dots = new Table();
        for (int i = 0; i < 3; i++) {
            Label dot = new Label("•", new Label.LabelStyle(fonts.get("font-xl"), Color.WHITE));
            dot.setName("dot-" + i);
            dots.add(dot).pad(4);
        }
        loadingOverlay.add(dots);
        loadingOverlay.setVisible(false);
        overlayStage.addActor(loadingOverlay);
    }

    private void showLoadingOverlay(boolean show) {
        if (loadingOverlay != null) loadingOverlay.setVisible(show);
    }

    private static Texture makeSolidTexture(Color color) {
        com.badlogic.gdx.graphics.Pixmap pix = new com.badlogic.gdx.graphics.Pixmap(4, 4,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pix.setColor(color);
        pix.fill();
        Texture tex = new Texture(pix);
        pix.dispose();
        return tex;
    }

    @Override
    public void render() {
        // 用当前主题 body 底色清屏，不同主题基础色调自动变
        com.badlogic.gdx.utils.ScreenUtils.clear(com.git.bs.ui.BsTheme.bgBodyColor(), true);
        super.render();
        if (overlayStage != null) {
            if (loadingOverlay != null && loadingOverlay.isVisible()) {
                float t = (System.currentTimeMillis() % 1500) / 1500f;
                for (int i = 0; i < 3; i++) {
                    com.badlogic.gdx.scenes.scene2d.Actor dot = loadingOverlay.findActor("dot-" + i);
                    if (dot instanceof Label) {
                        float phase = (t * 3 + i) % 3;
                        float alpha = phase < 1 ? phase : (phase < 2 ? 1 : Math.max(0.2f, 1 - (phase - 2)));
                        ((Label) dot).getColor().a = alpha;
                    }
                }
            }
            overlayStage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
            overlayStage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (overlayStage != null) overlayStage.getViewport().update(width, height, true);
    }

    public int getLoadedFontCount() { return loadedCount; }
    public boolean isAllFontsReady() { return loadedCount >= FONT_SIZES.length; }

    @Override
    public void dispose() {
        super.dispose();
        java.util.List<Skin> allSkins = new ArrayList<>(BsUI.registeredSkins());
        if (skin != null && !allSkins.contains(skin)) allSkins.add(skin);
        for (Skin s : allSkins) {
            if (s == null) continue;
            for (String key : fonts.keySet()) {
                try { s.remove(key, BitmapFont.class); } catch (Throwable ignored) {}
                try { s.remove("font-" + key, BitmapFont.class); } catch (Throwable ignored) {}
            }
            try { s.remove("font", BitmapFont.class); } catch (Throwable ignored) {}
            try { s.remove("lxgw", BitmapFont.class); } catch (Throwable ignored) {}
            try { s.dispose(); } catch (Throwable ignored) {}
        }
        for (BitmapFont f : fonts.values()) {
            try { f.dispose(); } catch (Throwable ignored) {}
        }
        fonts.clear();
        if (overlayStage != null) overlayStage.dispose();
        BsUI.dispose();
    }

    private void bindDefaultFontStyles(Skin skin, BitmapFont defaultFont) {
        BsSkinLoader.bindFontStyles(skin, "sm", defaultFont);
        BsSkinLoader.bindFontStyles(skin, "md", defaultFont);
        BsSkinLoader.bindFontStyles(skin, "lg", defaultFont);
        BsSkinLoader.bindFontStyles(skin, "xl", defaultFont);
    }

    /** 程序化构建并注册一个备用主题 skin（同步，不依赖字号字体预热）。 */
    private void registerExtraTheme(BsTheme theme, BitmapFont defaultFont) {
        try {
            if (BsUI.currentThemeName() != null
                    && BsUI.currentThemeName().equals(theme.name())) return;  // 已激活跳过
            Skin s = BsUI.buildSkin(theme, defaultFont);
            bindDefaultFontStyles(s, defaultFont);
            BsUI.registerTheme(theme.name(), theme, s);
        } catch (Throwable t) {
            log.warn("registerExtraTheme {} 失败", theme.name(), t);
        }
    }
}
