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

package cn.pingyuanren.bs.game;

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
import cn.pingyuanren.bs.demo.BsControlsTestScreen;
import cn.pingyuanren.bs.i18n.BsI18n;
import cn.pingyuanren.bs.ui.*;
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
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsControlsTestApp extends Game {

    private static final String SKIN_CP = "cn/pingyuanren/bs/ui/skin";
    private static final String TTF = SKIN_CP + "/LXGWWenKaiScreen.ttf";
    private static final String CHARS = SKIN_CP + "/chinese.txt";
    private static final String CHARS_COMMON = SKIN_CP + "/common.txt";

    /** 导出皮肤时复用的 TTF 文件 classpath 路径。 */
    public String ttfPath() { return TTF; }
    /** 导出皮肤时复用的完整字符集（63197 字符，含生僻字）。 */
    public String charsPath() { return CHARS; }
    /** 导出皮肤时复用的精简字符集（32870 字符，8000+ 常用汉字 + ASCII）。 */
    public String charsPathCommon() { return CHARS_COMMON; }

    /**
     * 返回实际可用的字符集条目（显示名 → classpath 路径），按优先级排序。
     * 仅返回 classpath 里真实存在的文件，避免导出对话框暴露不存在的选项。
     */
    public java.util.List<String[]> availableCharsEntries() {
        java.util.List<String[]> entries = new java.util.ArrayList<>();
        if (Gdx.files.internal(CHARS_COMMON).exists()) {
            entries.add(new String[]{BsI18n.get("app.charset_common"), CHARS_COMMON});
        }
        if (Gdx.files.internal(CHARS).exists()) {
            entries.add(new String[]{BsI18n.get("app.charset_full"), CHARS});
        }
        return entries;
    }

    /**
     * 返回当前 skin 里真实存在的字号后缀（xs/sm/md/lg/xl/xxl）。
     * 用于导出对话框的「字号多选」：让用户只勾选确实存在的档位。
     *
     * @return 有序集合（按 xs→xxl 排序），元素形如 {@code "xs"}；空集合表示 skin 里没有任何 font-* 字体
     */
    public java.util.List<String> availableFontSizes() {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (skin == null) return out;
        String[] all = {"xs", "sm", "md", "lg", "xl", "xxl"};
        for (String s : all) {
            if (skin.has("font-" + s, BitmapFont.class)) out.add(s);
        }
        return out;
    }

    /**
     * 返回 skin 里 {@code default} 字体的实际字号（px）。
     * 用于导出对话框的「独立烘焙 default-font」：让用户改了默认字号代码后，
     * 导出端能正确生成 default-font 配置（不再写死复用 md）。
     * <p>默认返回 18（与启动时 generateFont(chars, 18) 一致）；
     * 用户改字号后请同步修改此方法的返回值（或干脆改成字段）。</p>
     */
    public int defaultFontSize() {
        return 18;
    }

    /** 4 档字号定义（suffix → size）。 */
    private static final String[][] FONT_SIZES = {
            {"xs", "12"},
            {"sm", "14"},
            {"md", "18"},
            {"lg", "24"},
            {"xl", "32"},
            {"xxl", "48"},
    };

    @Getter
    private Skin skin;

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
        BsI18n.addBundle("cn/pingyuanren/bs/demo/i18n/");
        BsI18n.init();
        log.info("BsControlsTest init: 同步加载全部 5 档字号字体");
        long t0 = System.currentTimeMillis();
        chars = loadChars();

        // ① 同步生成 default (18px) + sm/md/lg/xl 四档（测试 App 不在乎启动耗时）
        BitmapFont defaultFont = generateFont(chars, 18);
        fonts.put("default", defaultFont);
        java.util.Map<String, BitmapFont> sizeFonts = new java.util.LinkedHashMap<>();
        for (String[] entry : FONT_SIZES) {
            String suffix = entry[0];
            int size = Integer.parseInt(entry[1]);
            BitmapFont f = generateFont(chars, size);
            fonts.put("font-" + suffix, f);
            sizeFonts.put(suffix, f);
        }
        loadedCount = FONT_SIZES.length;
        log.info("全部 {} 档字体就绪，耗时 {}ms", FONT_SIZES.length + 1, System.currentTimeMillis() - t0);

        // ② Light（default skin）—— 传入尺寸字体，派生尺寸变体时字号正确
        BsUI.registerDefaultSkin(defaultFont, sizeFonts);
        skin = BsUI.getSkin();

        // ③ Dark / Admin 同步注册（同样传尺寸字体）
        var darkTheme = BsDarkTheme.INSTANCE;
        Skin darkSkin = BsUI.buildSkin(darkTheme, defaultFont, sizeFonts);
        BsUI.registerTheme(darkTheme.name(), darkTheme, darkSkin);

        var adminTheme = BsAdminTheme.INSTANCE;
        Skin adminSkin = BsUI.buildSkin(adminTheme, defaultFont, sizeFonts);
        BsUI.registerTheme(adminTheme.name(), adminTheme, adminSkin);

        // ④ 旋转器 overlay stage（独立）
        overlayStage = new Stage(new ScreenViewport());
        buildLoadingOverlay();

        // ⑤ 立即进入测试台
        setScreen(new BsControlsTestScreen(this));

        // ⑥ 监听主题切换（外部调 BsUI.setTheme 时触发）
        BsUI.get().addOnThemeChangeListener(theme ->
                Gdx.app.postRunnable(() -> applyTheme(theme)));
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

                // 3. 新 skin + 新 screen（skin 在 buildSkin 时已注册尺寸字体）
                this.currentTheme = theme;
                this.skin = BsUI.getSkin();

                setScreen(new BsControlsTestScreen(this));

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
        Label label = new Label(BsI18n.get("app.switching_theme"), new Label.LabelStyle(
                fonts.get("default"), Color.WHITE));
        loadingOverlay.add(label).padBottom(20).row();
        // 旋转点（3 个圆点循环 alpha）
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

    // =================== render / dispose ===================

    @Override
    public void render() {
        // 用当前主题 body 底色清屏（Light #F5F6F8 / Dark #212529），切换主题后基础色调自动变
        com.badlogic.gdx.utils.ScreenUtils.clear(cn.pingyuanren.bs.ui.BsTheme.bgBodyColor(), true);
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
}
