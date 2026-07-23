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
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.git.bs.ui.BsSkin;
import com.git.bs.demo.BsControlsSkinScreen;
import com.git.bs.demo.BsControlsTestScreen;
import com.git.bs.i18n.BsI18n;
import com.git.bs.ui.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
public class BsSkinApp extends Game {

    private static final String SKIN_CP = "com/git/bs/ui/skin";
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
     * <p>默认返回 18（与 BsControlsTestApp 启动时 generateFont(chars, 18) 一致）；
     * 用户改字号后请在子类覆盖此方法返回真实值。</p>
     */
    public int defaultFontSize() {
        return 18;
    }

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
    private BsTheme currentTheme = BsDarkTheme.INSTANCE;

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
        com.git.bs.res.PakBootstrap.init(); // 资源 pak 加解密（必须在 BsUI.init 之前；无 pak 时自动跳过）
        log.info("BsControlsTest init: 同步加载 default 字体 + 分帧预热 4 档字号");
        BsUI.init();
//        // 观测/验证：皮肤加载后打印从 pak 服务过的路径；bs.exit=true 时 1s 自动退出（无界面验证）
//        java.util.Set<String> served = com.git.bs.res.PakFileHandle.servedPaths();
//        log.info("[pak] 已从 pak 服务 {} 个资源", served.size());
//        served.forEach(p -> log.info("[pak]   pak> {}", p));
//        if ("true".equalsIgnoreCase(System.getProperty("bs.exit"))) {
//            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
//                @Override public void run() { Gdx.app.exit(); }
//            }, 1f);
//        }
        BsI18n.addBundle("com/git/bs/demo/i18n/");
        BsI18n.init();
        log.info("BsControlsTest 所有主题皮肤加载完毕");
        long t0 = System.currentTimeMillis();
        BsUI.setTheme(currentTheme);
        skin = BsUI.getSkin();

        var fontCache = BsSkin.getFontCache(skin);
        fonts.putAll(fontCache);

        // ③ 旋转器 overlay stage（独立）
        overlayStage = new Stage(new ScreenViewport());
        buildLoadingOverlay();

        // ④ 立即进入测试台
        setScreen(new BsControlsSkinScreen(this));

        // ⑥ 监听主题切换（外部调 BsUI.setTheme 时触发）
        BsUI.get().addOnThemeChangeListener(theme ->
                Gdx.app.postRunnable(() -> applyTheme(theme)));
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
        com.badlogic.gdx.utils.ScreenUtils.clear(com.git.bs.ui.BsTheme.bgBodyColor(), true);
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

    @Override
    public void dispose() {
        super.dispose();
        BsUI.registeredSkins().forEach(Skin::dispose);
        BsSkin.disposeFontCache();
        com.git.bs.ui.BsEmoji.dispose();
        com.git.bs.ui.BsIcon.dispose();
        if (overlayStage != null) overlayStage.dispose();
        BsUI.dispose();
    }

}
