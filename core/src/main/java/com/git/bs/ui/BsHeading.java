package com.git.bs.ui;

/**
 * Bootstrap 风格标题（h1-h6）。
 *
 * <p>标题默认 {@link #bold() 开启粗体}：取 skin 的 {@code font-{level}-bold}，缺则降级。
 * 真加粗依赖 app 用 borderWidth 描边法生成对应字体（见 {@link BsText}）。</p>
 *
 * <p>6 级标题一级一档映射到 6 档字号：</p>
 * <ul>
 *   <li>h1 → xxl(40) / h2 → xl(32) / h3 → lg(24) / h4 → md(18) / h5 → sm(14) / h6 → xs(12)</li>
 * </ul>
 * <p>需 app/烘焙提供 font-xs ~ font-xxl 六档字体；缺档自动降级（见 {@link BsText}）。</p>
 *
 * <p>建议布局时用 {@link #spacing(int)} 给上下 pad，复现 Bootstrap 标题节奏：</p>
 * <pre>{@code
 * float[] s = BsHeading.spacing(1);
 * root.add(new BsHeading("用户管理", 1)).growX()
 *     .padTop(s[0]).padBottom(s[1]).row();
 * }</pre>
 */
public class BsHeading extends BsText {

    private final int level;

    public BsHeading(CharSequence text) {
        this(text, 1);
    }

    public BsHeading(CharSequence text, int level) {
        super(text, sizeForLevel(clampLevel(level)), Variant.DEFAULT, true);   // 标题默认粗体
        this.level = clampLevel(level);
    }

    public int getLevel() { return level; }

    /** 标题对应字号档。 */
    public Size sizeOfLevel() {
        return sizeForLevel(level);
    }

    static int clampLevel(int l) {
        return Math.max(1, Math.min(6, l));
    }

    /** level → 字号档映射（一级一档）。 */
    static Size sizeForLevel(int level) {
        switch (level) {
            case 1: return Size.XXL;
            case 2: return Size.XL;
            case 3: return Size.LG;
            case 4: return Size.MD;
            case 5: return Size.SM;
            default: return Size.XS;   // h6
        }
    }

    /**
     * 建议的上下间距 [top, bottom]（px），复现 Bootstrap 标题 margin 节奏：
     * 级别越低、间距越小。返回 [topPad, bottomPad]。
     */
    public static float[] spacing(int level) {
        switch (clampLevel(level)) {
            case 1: return new float[]{16f, 8f};
            case 2: return new float[]{14f, 8f};
            case 3: return new float[]{12f, 6f};
            case 4: return new float[]{10f, 6f};
            case 5: return new float[]{8f, 4f};
            default: return new float[]{6f, 4f};
        }
    }
}
