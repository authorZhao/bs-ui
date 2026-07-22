package com.git.bs.dashboard;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ObjectMap;
import com.git.bs.ui.BsDarkTheme;
import com.git.bs.ui.BsLightTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * 运维监控大屏入口。
 *
 * <p>皮肤：直接 {@link BsUI#init()} 加载烘焙 skin（light/dark/admin，含 sm/md/lg/xl 4 档 CJK 字体），
 * 默认 dark 主题 —— 不做运行时 CJK 字体生成（慢 + 重复造轮子）。</p>
 *
 * <p>大数字字体：KPI 的百分比/计数要大（64px），CJK 烘焙最大 xl=32 不够。但数字只需 0-9 + ASCII，
 * 字符集仅 95 个 —— 运行时 FreeType 生成一份 64px 纯 ASCII 字体（&lt;10ms、KB 级），注册成
 * {@code font-big-num} 供所有 skin 公用。这正是"动态字体甜区=小字符集"的合理用法。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class DashboardApp extends Game {

    private static final String SKIN_CP = "com/git/bs/ui/skin";
    private static final String TTF = SKIN_CP + "/LXGWWenKaiScreen.ttf";

    /** 大数字字体用的 ASCII 字符集（95 个可打印字符，足够数字/单位/小标签）。 */
    private static final String ASCII =
            " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

    private BitmapFont bigNumFont;

    @Override
    public void create() {
        // 1. 加载烘焙 skin（含 4 档 CJK 字体 + drawable + style），切 dark
        BsUI.init();
        BsUI.setTheme(BsLightTheme.INSTANCE);

        // 2. 运行时生成大数字字体（纯 ASCII 64px），注册进所有主题 skin 公用
        bigNumFont = generateAsciiFont(64);
        for (Skin s : BsUI.registeredSkins()) {
            if (s != null) s.add("font-big-num", bigNumFont, BitmapFont.class);
        }

        setScreen(new DashboardScreen());
    }

    private BitmapFont generateAsciiFont(int size) {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(TTF));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = size;
            p.characters = ASCII;
            p.minFilter = Texture.TextureFilter.Linear;
            p.magFilter = Texture.TextureFilter.Linear;
            p.hinting = FreeTypeFontGenerator.Hinting.AutoMedium;
            return gen.generateFont(p);
        } finally {
            gen.dispose();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        // 烘焙 skin 的字体由 app 主动释放；字体全皮肤公用，Set 去重。
        Set<BitmapFont> fontSet = new HashSet<>();
        for (Skin s : BsUI.registeredSkins()) {
            if (s == null) continue;
            ObjectMap<String, BitmapFont> all = s.getAll(BitmapFont.class);
            // 只 remove 非 null key——Skin.remove(null) 会抛 "name cannot be null"（skin 里偶有 null-key
            // 字体条目）；先收集 key 再 remove，避免迭代中改 map 触发 ConcurrentModification。
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (ObjectMap.Entry<String, BitmapFont> e : all) {
                if (e.key != null) {
                    keys.add(e.key);
                    fontSet.add(e.value);
                }
            }
            for (String k : keys) {
                try { s.remove(k, BitmapFont.class); } catch (Throwable ignored) {}
            }
        }
        if (bigNumFont != null) fontSet.add(bigNumFont);
        for (BitmapFont f : fontSet) {
            try { f.dispose(); } catch (Throwable ignored) {}
        }
        BsUI.dispose();
    }
}
