package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * Bs UI 主题接口（V2：颜色数据全部存放在 skin 的 Color 桶）。
 *
 * <p><b>设计哲学</b>：颜色不再写在代码里（{@code Color primary()}），
 * 而是注册到 {@code skin.Color} 桶（key 为 {@code "bs-primary"} 等）。
 * 这样：</p>
 * <ul>
 *   <li>用户改 skin json 就能定制颜色（无需改 Java 代码）</li>
 *   <li>同一 skin 文件可被多个主题使用（颜色 key 相同，值不同）</li>
 *   <li>组件代码统一 {@code skin.get("bs-X", Color.class)} 取色</li>
 * </ul>
 *
 * <p><b>BsTheme 实现类的职责</b>：</p>
 * <ol>
 *   <li>{@link #name()} / {@link #isDark()}：元信息</li>
 *   <li>{@link #applyColorsToSkin(Skin)}：把所有色 token 注入 skin 的 Color 桶</li>
 * </ol>
 *
 * <p><b>派生色</b>（hover/active）由本接口的静态工具方法从主色 HSL 计算得到，
 * 不需要每个主题单独写。</p>
 *
 * <h3>颜色 token 命名约定（skin Color 桶 key）</h3>
 * <ul>
 *   <li><b>文本</b>：bs-text-primary / bs-text-secondary / bs-text-muted / bs-text-disabled /
 *       bs-text-on-primary / bs-text-on-dark</li>
 *   <li><b>背景</b>：bs-bg-body / bs-bg-surface / bs-bg-elevated / bs-bg-hover / bs-bg-header</li>
 *   <li><b>边框</b>：bs-border / bs-border-strong</li>
 *   <li><b>6 色 Variant</b>：bs-primary / bs-secondary / bs-success / bs-danger / bs-warning / bs-info
 *       （派生色动态计算：hover = 主色提亮 7%，active = 主色加深 7%，softBg = 主色淡彩混合）</li>
 *   <li><b>特殊</b>：bs-overlay / bs-shadow / bs-link / bs-link-hover / bs-focus-ring / bs-cursor</li>
 * </ul>
 */
public interface BsTheme {

    // =================== 元信息 ===================
    /** 主题名："light" / "dark"。 */
    String name();
    /** 是否为暗色主题。 */
    boolean isDark();

    /**
     * 把本主题所有色 token 注入到 skin 的 Color 桶。
     * <p>调用时机：BsSkinFactory.create 时调一次；皮肤切换重建 skin 时调一次。</p>
     * <p>实现应该用 skin.add("bs-X", color, Color.class) 注册所有 token。</p>
     */
    void applyColorsToSkin(Skin skin);

    // =================== 静态取色方法（从当前激活的 skin 读取） ===================
    // 不再需要传 skin 参数，内部走 BsUI.getSkin() 自动取当前 skin。
    // 切换主题后下一次调用自动取新色。

    /** textPrimary */
    static Color tp() { return BsUI.getSkin().get("bs-text-primary", Color.class); }
    /** textSecondary */
    static Color ts() { return BsUI.getSkin().get("bs-text-secondary", Color.class); }
    /** textMuted */
    static Color tm() { return BsUI.getSkin().get("bs-text-muted", Color.class); }
    /** textDisabled */
    static Color td() { return BsUI.getSkin().get("bs-text-disabled", Color.class); }
    /** bgBody */
    static Color bb() { return BsUI.getSkin().get("bs-bg-body", Color.class); }
    /** bgSurface */
    static Color bs() { return BsUI.getSkin().get("bs-bg-surface", Color.class); }
    /** bgElevated */
    static Color be() { return BsUI.getSkin().get("bs-bg-elevated", Color.class); }
    /** bgHover */
    static Color bh() { return BsUI.getSkin().get("bs-bg-hover", Color.class); }
    /** bgHeader */
    static Color bhH() { return BsUI.getSkin().get("bs-bg-header", Color.class); }
    /** border */
    static Color bd() { return BsUI.getSkin().get("bs-border", Color.class); }
    /** borderStrong */
    static Color bds() { return BsUI.getSkin().get("bs-border-strong", Color.class); }
    /** overlay */
    static Color ov() { return BsUI.getSkin().get("bs-overlay", Color.class); }
    /** primary（主蓝） */
    static Color pri() { return BsUI.getSkin().get("bs-primary", Color.class); }

    /**
     * 通过 key 字符串取 Variant 主色。
     * @param key "primary"/"secondary"/"success"/"danger"/"warning"/"info"
     */
    static Color colorOf(String key) {
        return BsUI.getSkin().get("bs-" + key, Color.class);
    }

    // =================== 派生色计算（HSL 数学，与主题无关） ===================

    /** hover = 主色提亮 7%（HSL L + 0.07）。 */
    static Color hoverOf(String key) {
        return lighten(colorOf(key), 0.07f);
    }

    /** active = 主色加深 7%（HSL L - 0.07）。 */
    static Color activeOf(String key) {
        return darken(colorOf(key), 0.07f);
    }

    /** softBg = 主色与白色 9:1 混合（Bootstrap alert bg-* 风格）。 */
    static Color softBgOf(String key) {
        Color c = colorOf(key);
        float factor = 0.88f;
        return new Color(
                c.r + (1 - c.r) * factor,
                c.g + (1 - c.g) * factor,
                c.b + (1 - c.b) * factor,
                1f);
    }

    /** 提亮：HSL 的 L 通道 + amount。 */
    static Color lighten(Color c, float amount) {
        float[] hsl = BsColors.rgbToHsl(c.r, c.g, c.b);
        hsl[2] = Math.min(1f, hsl[2] + amount);
        float[] rgb = BsColors.hslToRgb(hsl[0], hsl[1], hsl[2]);
        return new Color(rgb[0], rgb[1], rgb[2], c.a);
    }

    /** 加深：HSL 的 L 通道 - amount。 */
    static Color darken(Color c, float amount) {
        float[] hsl = BsColors.rgbToHsl(c.r, c.g, c.b);
        hsl[2] = Math.max(0f, hsl[2] - amount);
        float[] rgb = BsColors.hslToRgb(hsl[0], hsl[1], hsl[2]);
        return new Color(rgb[0], rgb[1], rgb[2], c.a);
    }
}
