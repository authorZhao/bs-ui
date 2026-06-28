package com.git.bs.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.git.bs.common.SkinUtil;
import com.git.bs.demo.BsControlsSkinScreen;
import com.git.bs.demo.BsControlsTestScreen;
import com.git.bs.ui.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Bs 控件测试 App（V2：主题切换走 setScreen 重建）。
 *
 * <p><b>资源生命周期</b>：</p>
 * <ul>
 *   <li>{@link #fonts}：应用级字体池，跨主题复用，应用退出才释放</li>
 *   <li>{@link #skin}：每次切主题重建一份（含主题色 + drawable + style）</li>
 *   <li>{@link #overlayStage}：旋转器专用 stage，独立于 screen，切主题时也活着</li>
 * </ul>
 *
 * <p><b>主题切换流程</b>（{@link #applyTheme(BsTheme)}）：</p>
 * <ol>
 *   <li>显示旋转器 overlay</li>
 *   <li>下一帧（postRunnable）：用新主题创建新 skin，绑定字体池</li>
 *   <li>新建 screen，game.setScreen（旧 screen 暂存）</li>
 *   <li>oldScreen.dispose()（释放 actor 对旧 drawable 的引用）</li>
 *   <li>从 oldSkin 移除字体引用（防止 skin.dispose 误删字体）</li>
 *   <li>oldSkin.dispose()（释放 Texture）</li>
 *   <li>隐藏旋转器</li>
 * </ol>
 */
@Slf4j
public class BsSkinApp extends Game {

    private static final String SKIN_CP = "com/git/bs/ui/skin";
    private static final String TTF = SKIN_CP + "/LXGWWenKaiMonoLite-Light.ttf";
    private static final String CHARS = SKIN_CP + "/chinese.txt";

    @Getter
    private Skin skin;

    /** 4 档字号定义（suffix → size）。 */
    private static final String[][] FONT_SIZES = {
            {"sm", "14"},
            {"md", "18"},
            {"lg", "24"},
            {"xl", "32"},
    };

    /** 应用级字体池（跨主题复用，不随 skin dispose）。 */
    @Getter
    private final Map<String, BitmapFont> fonts = new HashMap<>();

    /** 当前主题（默认 Light）。 */
    @Getter
    private BsTheme currentTheme = BsLightTheme.INSTANCE;

    /** 字符集（启动时加载，分帧生成字体复用）。 */
    private String chars;

    /** 字体加载完成回调（全部完成时触发）。 */
    public interface FontsReadyListener { void onFontsReady(); }
    private FontsReadyListener fontsReadyListener;
    public void setFontsReadyListener(FontsReadyListener l) { this.fontsReadyListener = l; }

    /** 已加载完成的字号数（业务方可轮询）。 */
    private volatile int loadedCount = 0;

    /** 旋转器专用 stage（独立于 screen，切主题时也活着）。 */
    private Stage overlayStage;
    /** 旋转器容器（显示/隐藏切换可见性）。 */
    private Table loadingOverlay;
    /** 是否正在切换主题（防止重入）。 */
    private boolean themeSwitching = false;

    @Override
    public void create() {
        log.info("BsControlsTest init: 同步加载 default 字体 + 分帧预热 4 档字号");
        BsUI.init();
        long t0 = System.currentTimeMillis();
        chars = loadChars();


        var lightFileHandle = Gdx.files.internal(SKIN_CP + "/" + currentTheme.name() + ".json");
        var darkTheme = BsDarkTheme.INSTANCE;

        var lightSkin = BsSkinLoader.loadAndAugmentWithCache(lightFileHandle, currentTheme);


        // ① 同步生成 default (18px)，UI 立即可用
        BitmapFont defaultFont = lightSkin.getFont("default");
        fonts.put("default", defaultFont);



        log.info("default 字体就绪，UI 可启动，耗时 {}ms", System.currentTimeMillis() - t0);

        skin = lightSkin;
        BsUI.registerTheme(currentTheme.name(), currentTheme, skin);
        bindDefaultFontStyles(skin, defaultFont);

        setFontsReadyListener(() -> {
            var darkFileHandle = Gdx.files.internal(SKIN_CP + "/"+ darkTheme.name() + ".json");
            var fontCache = SkinUtil.getFontCache(this.skin);
            BsSkinLoader.loadAndAugmentWithCache(darkFileHandle, currentTheme, fontCache);
            //var darkTheme = BsDarkTheme.INSTANCE;
            Skin darkSkin = BsUI.buildSkin(darkTheme, defaultFont);
            for (String suffix : new String[]{"sm", "md", "lg", "xl"}) {
                BitmapFont f = fonts.get(suffix);
                if (f != null) BsSkinLoader.bindFontStyles(darkSkin,suffix,  f);
            }
            BsUI.registerTheme(darkTheme.name(), darkTheme, darkSkin);
        });


        // ③ 旋转器 overlay stage（独立）
        overlayStage = new Stage(new ScreenViewport());
        buildLoadingOverlay();

        // ④ 立即进入测试台
        setScreen(new BsControlsSkinScreen(this));

        // ⑤ 分帧加载 sm/md/lg/xl
        scheduleRemainingFonts(0);

        // ⑥ 监听主题切换（外部调 BsUI.setTheme 时触发）
        BsUI.get().addOnThemeChangeListener(theme ->
                Gdx.app.postRunnable(() -> applyTheme(theme)));
    }


    /** 递归 postRunnable：每帧生成 1 个字号。 */
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
                long elapsed = System.currentTimeMillis() - t;
                log.info("分帧字体生成完成 [{}] size={} 耗时={}ms", suffix, size, elapsed);
                fonts.put(suffix, f);
                // 注册到当前 skin（在线注册，不切主题）
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

    // =================== 主题切换（核心） ===================

    /**
     * 切换主题：显示旋转器 → 用新主题建新 skin + 新 screen → 切转 → 清理旧。
     * <p>必须在 GL 线程调用（内部不再 postRunnable，调用方负责）。</p>
     */
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

        // 1. 显示旋转器
        showLoadingOverlay(true);

        // 2. 下一帧执行实际切换（让旋转器先渲染一帧）
        Gdx.app.postRunnable(() -> {
            try {
                com.badlogic.gdx.Screen oldScreen = getScreen();

                // 3. 新 skin + 新 screen
                this.currentTheme = theme;
                this.skin = BsUI.getSkin();
                // 字号字体绑定
                for (String suffix : new String[]{"sm", "md", "lg", "xl"}) {
                    BitmapFont f = fonts.get(suffix);
                    if (f != null) BsSkinLoader.bindFontStyles(this.skin, suffix, f);
                }

                // 4. setScreen（新 screen 用新 skin）
                setScreen(new BsControlsSkinScreen(this));

                // 5. 释放旧 screen（actor 释放对旧 drawable 的引用）
                if (oldScreen != null) {
                    try { oldScreen.dispose(); } catch (Throwable t) { log.warn("oldScreen dispose", t); }
                }

                log.info("applyTheme: 切换完成");
            } catch (Throwable t) {
                log.error("applyTheme 失败", t);
            } finally {
                // 8. 隐藏旋转器
                showLoadingOverlay(false);
                themeSwitching = false;
            }
        });
    }

    // =================== 旋转器 overlay ===================

    private void buildLoadingOverlay() {
        loadingOverlay = new Table();
        loadingOverlay.setFillParent(true);
        // 半透明遮罩
        loadingOverlay.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                makeSolidTexture(new Color(0, 0, 0, 0.5f))));
        // "切换主题中..." + 旋转点
        Label label = new Label("正在切换主题", new Label.LabelStyle(
                fonts.get("default"), Color.WHITE));
        loadingOverlay.add(label).padBottom(20).row();
        // 旋转点（3 个圆点循环 alpha）
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

    // =================== render / dispose ===================

    @Override
    public void render() {
        super.render();
        // 渲染旋转器 overlay（如果可见）
        if (overlayStage != null) {
            // 更新旋转点 alpha
            if (loadingOverlay != null && loadingOverlay.isVisible()) {
                float t = (System.currentTimeMillis() % 1500) / 1500f;  // 0~1 循环
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
        java.util.List<Skin> allSkins = new java.util.ArrayList<>(BsUI.registeredSkins());
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
        // 字体最后释放
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
