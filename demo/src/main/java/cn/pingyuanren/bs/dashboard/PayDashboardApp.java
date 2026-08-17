package cn.pingyuanren.bs.dashboard;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ObjectMap;
import cn.pingyuanren.bs.ui.BsDarkTheme;
import cn.pingyuanren.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * 支付订单分析大屏入口。
 *
 * <p>皮肤走 {@link BsUI#init()} 烘焙 skin（含 CJK 字体），默认 light 主题。
 * 标题字体运行时从 TTF 生成 48px CJK+ASCII（字符集 &lt;60，生成 &lt;10ms），
 * 注册成 {@code font-pay-title} 供所有 skin 公用。</p>
 *
 * @author authorZhao
 * @since 2026-08-13
 */
@Slf4j
public class PayDashboardApp extends Game {

    /** TTF 在 assets 目录下的相对路径（已从 assets-skin 迁移到 assets）。 */
    private static final String TTF = "bs/test/img/LXGWWenKaiScreen.ttf";

    /** 标题 + 图表标签涉及的全部 CJK 字符（去重后 ~25 字，生成极快）。 */
    private static final String CJK =
            "支付订单分析图每日趋势折线月度汇总柱状占比饼已关待数量日期";

    private static final String ASCII =
            " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

    private BitmapFont titleFont;

    @Override
    public void create() {
        BsUI.init();
        BsUI.setTheme(BsDarkTheme.INSTANCE);

        titleFont = generateTitleFont(48);
        if (titleFont != null) {
            for (Skin s : BsUI.registeredSkins()) {
                if (s != null) s.add("font-pay-title", titleFont, BitmapFont.class);
            }
        }

        setScreen(new PayDashboardScreen());
    }

    private BitmapFont generateTitleFont(int size) {
        try {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(TTF));
            try {
                FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
                p.size = size;
                p.characters = CJK + ASCII;
                p.minFilter = Texture.TextureFilter.Linear;
                p.magFilter = Texture.TextureFilter.Linear;
                p.hinting = FreeTypeFontGenerator.Hinting.AutoMedium;
                return gen.generateFont(p);
            } finally {
                gen.dispose();
            }
        } catch (Throwable e) {
            log.warn("生成标题字体失败，降级到 skin 默认字体: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        Set<BitmapFont> fontSet = new HashSet<>();
        for (Skin s : BsUI.registeredSkins()) {
            if (s == null) continue;
            ObjectMap<String, BitmapFont> all = s.getAll(BitmapFont.class);
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
        if (titleFont != null) fontSet.add(titleFont);
        for (BitmapFont f : fontSet) {
            try { f.dispose(); } catch (Throwable ignored) {}
        }
        BsUI.dispose();
    }
}
