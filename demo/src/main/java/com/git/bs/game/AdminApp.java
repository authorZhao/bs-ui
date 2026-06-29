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
 */
@Slf4j
public class AdminApp extends Game {

    private static final String SKIN_CP = "com/git/bs/ui/skin";
    private static final String TTF = SKIN_CP + "/LXGWWenKaiMonoLite-Light.ttf";
    private static final String CHARS = SKIN_CP + "/chinese.txt";

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
        log.info("AdminApp init: 同步加载 default 字体 + 分帧预热 4 档字号");
        BsUI.init();
        long t0 = System.currentTimeMillis();
        chars = loadChars();

        // 加载 admin 皮肤（admin.json + admin.atlas + admin.png，颜色在 JSON 里可直接改）
        var skinFileHandle = Gdx.files.internal(SKIN_CP + "/admin.json");


        var defaultSkin = BsSkinLoader.loadAndAugmentWithCache(skinFileHandle, currentTheme);

        BitmapFont defaultFont = defaultSkin.getFont("default");
        fonts.put("default", defaultFont);

        log.info("default 字体就绪，UI 可启动，耗时 {}ms", System.currentTimeMillis() - t0);

        skin = defaultSkin;
        BsUI.registerTheme(currentTheme.name(), currentTheme, skin);
        bindDefaultFontStyles(skin, defaultFont);
        log.info("AdminApp 当前激活主题: {}", BsUI.currentThemeName());



        setFontsReadyListener(() -> {


            var darkTheme = BsDarkTheme.INSTANCE;

            var darkFileHandle = Gdx.files.internal(SKIN_CP + "/" + darkTheme.name() + ".json");
            var fontCache = SkinUtil.getFontCache(this.skin);
            BsSkinLoader.loadAndAugmentWithCache(darkFileHandle, currentTheme, fontCache);
            Skin darkSkin = BsUI.buildSkin(darkTheme, defaultFont);
            for (String suffix : new String[]{"sm", "md", "lg", "xl"}) {
                BitmapFont f = fonts.get(suffix);
                if (f != null) BsSkinLoader.bindFontStyles(darkSkin, suffix, f);
            }
            BsUI.registerTheme(darkTheme.name(), darkTheme, darkSkin);
        });

        overlayStage = new Stage(new ScreenViewport());
        buildLoadingOverlay();

        // 进入登录页（与 BsSkinApp 的唯一差别）
        setScreen(new AdminLoginScreen(this));

        scheduleRemainingFonts(0);

        BsUI.get().addOnThemeChangeListener(theme ->
                Gdx.app.postRunnable(() -> applyTheme(theme)));
    }

    private void scheduleRemainingFonts(int idx) {
        if (idx >= FONT_SIZES.length) {
            log.info("✓ 所有 {} 档字号加载完成", FONT_SIZES.length);
            if (fontsReadyListener != null) {
                try { fontsReadyListener.onFontsReady(); } catch (Throwable e) { log.warn("onFontsReady", e); }
            }
            return;
        }
        final int nextIdx = idx;
        Gdx.app.postRunnable(() -> {
            String suffix = FONT_SIZES[nextIdx][0];
            int size = Integer.parseInt(FONT_SIZES[nextIdx][1]);
            try {
                long t = System.currentTimeMillis();
                BitmapFont f = generateFont(chars, size);
                log.info("分帧字体生成完成 [{}] size={} 耗时={}ms", suffix, size, System.currentTimeMillis() - t);
                fonts.put(suffix, f);
                BsSkinLoader.bindFontStyles(skin, suffix, f);
                loadedCount++;
            } catch (Throwable e) {
                log.warn("字体生成失败 size=" + size, e);
            }
            scheduleRemainingFonts(nextIdx + 1);
        });
    }

    private String loadChars() {
        return Gdx.files.internal(CHARS).readString(StandardCharsets.UTF_8.name());
    }

    private BitmapFont generateFont(String chars, int size) {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(TTF));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = size;
            p.minFilter = Texture.TextureFilter.Linear;
            p.magFilter = Texture.TextureFilter.Linear;
            p.hinting = FreeTypeFontGenerator.Hinting.AutoMedium;
            p.characters = chars;
            return gen.generateFont(p);
        } finally {
            gen.dispose();
        }
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
            Label dot = new Label("•", new Label.LabelStyle(fonts.get("default"), Color.WHITE));
            dot.setName("dot-" + i);
            dot.setFontScale(2f);
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
}
