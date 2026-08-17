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
package cn.pingyuanren.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Bootstrap 5 风格进度条（progress-bar）。
 *
 * <p>结构：外层 track（灰色背景）+ 内层 fill（contextual 色）+ 可选文字标签。
 * 6 色变体（primary/secondary/success/danger/warning/info），可选条纹动画。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsProgress progress = new BsProgress(skin);
 * progress.setSize(360, 20);
 * progress.setVariant(BsProgress.Variant.SUCCESS);
 * progress.setProgress(0.65f);   // 65%
 * progress.setStriped(true);     // 显示条纹
 * progress.setAnimated(true);    // 条纹动画（CSS 风格，scene2d 用 act 驱动）
 * stage.addActor(progress);
 * }</pre>
 *
 * <p>实现：继承 {@link Table} 作容器；fill 是一个自绘 Actor（重写 draw 贴一段 drawable），
 * 通过 setWidth 控制填充宽度。stripes 用程序化 Pixmap 离屏贴图（避免每帧重算）。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsProgress extends Table {

    public enum Variant { PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO }

    private final Actor fillActor;
    private final Label label;
    private final Table labelWrap;   // 标签居中容器（叠在 fill 上方）
    private Variant variant = Variant.PRIMARY;
    private float progress = 0f;     // 0~1
    private boolean showLabel = false;
    private boolean striped = false;
    private boolean animated = false;
    private float stripeOffset = 0f;

    /** 当前 fill 颜色对应的 drawable（来自 BsSkinFactory 注册的 bs-{color}-up）。 */
    private Drawable fillDrawable;
    /** striped 模式下的斜纹纹理（striped=true 时由 setStriped 生成，remove 时 dispose）。 */
    private Texture stripedTexture;
    private TextureRegion stripedRegion;

    public BsProgress(Skin skin) {
        setBackground(skin.getDrawable("bs-progress-track"));  // 无边框淡灰圆角 track
        // fill 用自绘 Actor（draw 内部贴 drawable / 斜纹纹理），便于条纹动画偏移
        fillActor = new Actor() {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                Color c = getColor();
                float alpha = c.a * parentAlpha;
                if (striped && stripedRegion != null) {
                    // 条纹模式：用 TextureRegion 偏移实现流动动画（stripeOffset 驱动水平平移）
                    // 按 fill 宽度循环贴 40px 宽的条纹纹理
                    batch.setColor(c.r, c.g, c.b, alpha);
                    float w = getWidth(), h = getHeight();
                    // 用 batch.draw(texture, x, y, w, h, srcX, srcY, srcW, srcH, flip) 形式做纹理偏移：
                    // 取 stripedRegion 整张，srcX = stripeOffset 实现平移，配合 Repeat wrap 循环
                    Texture tex = stripedRegion.getTexture();
                    int texW = tex.getWidth();
                    // 把 fill 区域映射成"纹理坐标偏移 stripeOffset 后的子图"——
                    // 用 scaled src 方式：srcX 随 stripeOffset 变化，srcW = texW，绘制时按 fill 宽度缩放
                    // 简化：直接用 batch.draw 的 region 形式 + u/v 偏移（libgdx 用 setRegion 调整）
                    // 最稳：临时改 region 的 x，draw 完恢复
                    float oldX = stripedRegion.getRegionX();
                    float oldW = stripedRegion.getRegionWidth();
                    stripedRegion.setRegionX((int) (stripeOffset % texW));
                    // region 宽度也要调整避免越界——用 Repeat wrap 已设，直接整宽
                    stripedRegion.setRegionWidth(texW);
                    batch.draw(stripedRegion, getX(), getY(), w, h);
                    stripedRegion.setRegionX((int) oldX);
                    stripedRegion.setRegionWidth((int) oldW);
                    batch.setColor(Color.WHITE);
                } else if (fillDrawable != null) {
                    batch.setColor(c.r, c.g, c.b, alpha);
                    fillDrawable.draw(batch, getX(), getY(), getWidth(), getHeight());
                    batch.setColor(Color.WHITE);
                }
            }
            @Override
            public void act(float delta) {
                super.act(delta);
                if (animated) {
                    // 条纹流动：每秒移 40px（一个周期），% 40 循环
                    stripeOffset = (stripeOffset + delta * 40f) % 40f;
                }
            }
        };
        fillActor.setVisible(false);   // 默认 progress=0 不显示
        addActor(fillActor);
        // 文字标签（默认隐藏），居中
        label = new Label("", skin);
        label.setColor(Color.WHITE);
        labelWrap = new Table();
        labelWrap.add(label);
        labelWrap.setVisible(false);
        addActor(labelWrap);

        setVariant(Variant.PRIMARY);
        setHeight(18);  // 默认高度
    }

    /** 设置进度（0~1，会被 clamp）。progress=0 时隐藏 fill，避免残留像素。 */
    public BsProgress setProgress(float p) {
        this.progress = Math.max(0f, Math.min(1f, p));
        // 0% 时彻底隐藏 fillActor，避免圆角边缘残留
        fillActor.setVisible(this.progress > 0.0001f);
        invalidateHierarchy();
        layout();   // 立即更新 fill 宽度
        if (showLabel) {
            label.setText(Math.round(this.progress * 100) + "%");
        }
        return this;
    }

    public float getProgress() { return progress; }

    public Variant getVariant() { return variant; }

    /** 显示进度百分比文字（叠在 fill 上，白字）。 */
    public BsProgress setShowLabel(boolean show) {
        this.showLabel = show;
        labelWrap.setVisible(show);
        if (show) label.setText(Math.round(progress * 100) + "%");
        return this;
    }

    /** 条纹背景（45° 斜纹，CSS progress-bar-striped 效果）。 */
    public BsProgress setStriped(boolean striped) {
        this.striped = striped;
        // 生成/释放条纹纹理（真斜纹，variant 色 + 半透交替；动画用 stripeOffset 平移）
        if (striped) {
            if (stripedTexture == null) {
                stripedTexture = BsSkinFactory.stripedTexture(colorOf(variant));
                stripedRegion = new TextureRegion(stripedTexture);
            }
        }
        // 非 striped 模式用纯色 fillDrawable（setVariant 已设），纹理保留到 remove 统一释放
        return this;
    }

    /** 条纹动画（仅 striped=true 时有效，CSS progress-bar-animated 效果）。 */
    public BsProgress setAnimated(boolean animated) {
        this.animated = animated;
        return this;
    }

    /** 切换 variant 时重建条纹纹理（颜色变了，旧纹理失效）。 */
    public BsProgress setVariant(Variant v) {
        this.variant = v;
        String key = "bs-" + v.name().toLowerCase() + "-up";
        fillDrawable = BsUI.getSkin().getDrawable(key);
        // striped 模式下纹理要按新色重建
        if (stripedTexture != null) {
            stripedTexture.dispose();
            stripedTexture = BsSkinFactory.stripedTexture(colorOf(v));
            stripedRegion = new TextureRegion(stripedTexture);
        }
        return this;
    }

    @Override
    public boolean remove() {
        if (stripedTexture != null) {
            stripedTexture.dispose();
            stripedTexture = null;
            stripedRegion = null;
        }
        return super.remove();
    }

    private static Color colorOf(Variant v) {
        switch (v) {
            case PRIMARY:   return BsPalette.PRIMARY.getMain();
            case SECONDARY: return BsPalette.SECONDARY.getMain();
            case SUCCESS:   return BsPalette.SUCCESS.getMain();
            case DANGER:    return BsPalette.DANGER.getMain();
            case WARNING:   return BsPalette.WARNING.getMain();
            case INFO:      return BsPalette.INFO.getMain();
        }
        return Color.GRAY;
    }

    @Override
    public void layout() {
        super.layout();
        // fill 填充进度宽度，高度 = 容器内高
        float pad = 2;  // 内缩 2px，避免 fill 溢出 track 圆角
        float w = (getWidth() - 2 * pad) * progress;
        float h = getHeight() - 2 * pad;
        fillActor.setBounds(pad, pad, Math.max(0, w), Math.max(0, h));
        // label 居中于整个 track（不计 fill 宽度，文字稳定）
        labelWrap.setBounds(0, 0, getWidth(), getHeight());
    }
}
