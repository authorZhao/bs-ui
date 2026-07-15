/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库。
 * Copyright (c) 2026 bs-ui contributors
 *
 * 基于 Apache License 2.0 开源，允许商用、修改和再分发。
 * 使用本库的产品须在“关于”界面标注本项目，详见 LICENSE。
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * Bootstrap 风格通用文本原语 —— 整个排版体系的基础积木。
 *
 * <p>统一封装「字号档 × 颜色变体 × 粗体 × 斜体」。遵循字体管线分层
 * （见 memory/font-pipeline-layering）：core 只从 skin 取字体（消费），不生成；
 * 粗体字体的生产由 app/烘焙用 borderWidth 描边法提供并注册进 skin。</p>
 *
 * <ul>
 *   <li><b>字号</b>：{@link Size#DEFAULT} 沿用 skin 当前默认字体（跟随 skin）；
 *       XS/SM/MD/LG/XL/XXL 取 skin 的 font-xs/sm/md/lg/xl/xxl（6 档烘焙位图字体，缺档降级）。</li>
 *   <li><b>颜色变体</b>：走 {@link BsTheme} token。</li>
 *   <li><b>粗体</b>：取 skin 的 {@code font-{size}-bold}（缺则降级回非粗体，不报错）。
 *       <b>真加粗依赖 app 用 borderWidth 描边法生成该 key 的字体并注册进 skin</b>
 *       （生成时 borderColor=WHITE，渲染时由 fontColor 染成任意色 = 彩色加粗字）。</li>
 *   <li><b>斜体</b>：draw 阶段对 batch 套 ~12° 剪切矩阵，core 自实现，<b>立即生效</b>，
 *       不依赖 skin 斜体字体。CJK 斜体观感一般，按需开启。</li>
 * </ul>
 *
 * <pre>{@code
 * new BsText("正文");                                        // 跟随 skin 默认
 * new BsText("标题", Size.LG).bold().italic();
 * new BsText("错误", Size.MD, Variant.DANGER).bold();
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsText extends Label {

    /** 字号档：DEFAULT 沿用 skin 当前默认字体；其余对应 font-xs/sm/md/lg/xl/xxl（6 档）。 */
    public enum Size { DEFAULT, XS, SM, MD, LG, XL, XXL }

    /** 颜色变体：映射到 BsTheme 文本色 / Variant 主色 token。 */
    public enum Variant {
        DEFAULT,    // bs-text-primary
        SECONDARY,  // bs-text-secondary
        MUTED,      // bs-text-muted
        DISABLED,   // bs-text-disabled
        PRIMARY,    // bs-primary
        SUCCESS,    // bs-success
        WARNING,    // bs-warning
        DANGER,     // bs-danger
        INFO,       // bs-info
        ON_DARK     // bs-text-on-dark
    }

    /** 斜体剪切量：tan(~12°)。 */
    private static final float ITALIC_SHEAR = 0.21f;

    // draw 阶段复用的临时矩阵（单 GL 线程，Scene2d.draw 同类型不嵌套，可静态复用）
    private static final Matrix4 T_TO_ORIGIN = new Matrix4();
    private static final Matrix4 SHEAR = new Matrix4();
    private static final Matrix4 T_BACK = new Matrix4();
    private static final Matrix4 COMPOSED = new Matrix4();

    /** draw 阶段复用的临时坐标向量（实例字段，单 GL 线程）。 */
    private final Vector2 tmp = new Vector2();

    private Size size = Size.DEFAULT;
    private Variant variant = Variant.DEFAULT;
    private boolean bold;
    private boolean italic;

    public BsText(CharSequence text) {
        super(text, styleFor(Size.DEFAULT, Variant.DEFAULT, false));
    }

    public BsText(CharSequence text, Size size) {
        this(text, size, Variant.DEFAULT, false);
    }

    public BsText(CharSequence text, Size size, Variant variant) {
        this(text, size, variant, false);
    }

    public BsText(CharSequence text, Size size, Variant variant, boolean bold) {
        super(text, styleFor(size, variant, bold));
        this.size = size;
        this.variant = variant;
        this.bold = bold;
    }

    public BsText setSize(Size s) {
        this.size = s;
        setStyle(styleFor(s, variant, bold));
        return this;
    }

    public BsText setVariant(Variant v) {
        this.variant = v;
        setStyle(styleFor(size, v, bold));
        return this;
    }

    /** 粗体：取 skin 的 font-{size}-bold，缺则视觉降级（不报错）。真加粗需 app 提供该字体。 */
    public BsText setBold(boolean b) {
        this.bold = b;
        setStyle(styleFor(size, variant, b));
        return this;
    }

    /** 链式开启粗体。 */
    public BsText bold() { return setBold(true); }

    /** 斜体：draw 阶段 shear 渲染，core 自实现，无需 skin 斜体字体。 */
    public BsText setItalic(boolean it) {
        this.italic = it;
        return this;
    }

    /** 链式开启斜体。 */
    public BsText italic() { return setItalic(true); }

    public Size getSize() { return size; }
    public Variant getVariant() { return variant; }
    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!italic) { super.draw(batch, parentAlpha); return; }
        // batch.transformMatrix 作用在 stage 世界坐标空间，pivot 必须用世界坐标（label 中心）
        Vector2 c = localToStageCoordinates(tmp.set(getWidth() / 2f, getHeight() / 2f));
        float pivotX = c.x;
        float pivotY = c.y;
        Matrix4 cur = batch.getTransformMatrix();

        // 复合 = T(pivot) · Shear · T(-pivot) · M：先把轴点移到原点，剪切，再移回
        T_TO_ORIGIN.setToTranslation(-pivotX, -pivotY, 0);
        SHEAR.idt();
        SHEAR.val[Matrix4.M01] = ITALIC_SHEAR;   // x 分量受 y 影响 → 向右倾斜
        T_BACK.setToTranslation(pivotX, pivotY, 0);

        COMPOSED.set(T_BACK).mul(SHEAR).mul(T_TO_ORIGIN).mul(cur);
        batch.setTransformMatrix(COMPOSED);
        super.draw(batch, parentAlpha);
        batch.setTransformMatrix(cur);   // 还原，避免污染同 batch 其他绘制
    }

    /** 构造 LabelStyle：复制 skin 默认 style → 按需换 font / fontColor。 */
    static LabelStyle styleFor(Size size, Variant variant, boolean bold) {
        Skin skin = BsUI.getSkin();
        LabelStyle ls = new LabelStyle(skin.get(LabelStyle.class));
        BitmapFont f = resolveFont(skin, size, bold);
        if (f != null) ls.font = f;
        ls.fontColor = colorOf(variant);
        return ls;
    }

    /** 按 size + bold 解析 skin 字体；命中候选 key 返回，否则 null（保留默认字体）。 */
    static BitmapFont resolveFont(Skin skin, Size size, boolean bold) {
        for (String key : fontKeyCandidates(size, bold)) {
            if (skin.has(key, BitmapFont.class)) return skin.getFont(key);
        }
        return null;
    }

    /** 字体 key 优先级：bold 时先找粗体 key、再降级普通 key；DEFAULT 走 default/font。 */
    private static String[] fontKeyCandidates(Size size, boolean bold) {
        if (size == Size.DEFAULT) {
            return bold ? new String[]{"default-bold", "font-bold", "default", "font"}
                    : new String[]{"default", "font"};
        }
        String s = size.name().toLowerCase();
        return bold
                ? new String[]{"font-" + s + "-bold", "font-" + s}
                : new String[]{"font-" + s};
    }

    static Color colorOf(Variant v) {
        switch (v) {
            case SECONDARY: return BsTheme.ts();
            case MUTED: return BsTheme.tm();
            case DISABLED: return BsTheme.td();
            case PRIMARY: return BsTheme.colorOf("primary");
            case SUCCESS: return BsTheme.colorOf("success");
            case WARNING: return BsTheme.colorOf("warning");
            case DANGER: return BsTheme.colorOf("danger");
            case INFO: return BsTheme.colorOf("info");
            case ON_DARK: return BsUI.getSkin().get("bs-text-on-dark", Color.class);
            default: return BsTheme.tp();
        }
    }
}
