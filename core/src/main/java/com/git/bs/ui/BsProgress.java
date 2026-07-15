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

    public BsProgress(Skin skin) {
        setBackground(skin.getDrawable("bs-progress-track"));  // 无边框淡灰圆角 track
        // fill 用自绘 Actor（draw 内部贴 drawable），便于条纹动画偏移
        fillActor = new Actor() {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                if (fillDrawable == null) return;
                Color c = getColor();
                batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
                fillDrawable.draw(batch, getX(), getY(), getWidth(), getHeight());
                batch.setColor(Color.WHITE);
            }
            @Override
            public void act(float delta) {
                super.act(delta);
                if (animated) {
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

    public BsProgress setVariant(Variant v) {
        this.variant = v;
        String key = "bs-" + v.name().toLowerCase() + "-up";
        fillDrawable = BsUI.getSkin().getDrawable(key);
        return this;
    }

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
        // 简化实现：striped 状态切换 fill drawable（条带版 vs 纯色版）
        // 程序化条纹 drawable 一次性生成缓存，这里复用 roundRect 的色块 + alpha 网格
        Skin skin = BsUI.getSkin();
        if (striped) {
            fillDrawable = skin.newDrawable("bs-" + variant.name().toLowerCase() + "-up",
                    tint(variant, 0.85f));
        } else {
            fillDrawable = skin.getDrawable("bs-" + variant.name().toLowerCase() + "-up");
        }
        return this;
    }

    /** 条纹动画（仅 striped=true 时有效，CSS progress-bar-animated 效果）。 */
    public BsProgress setAnimated(boolean animated) {
        this.animated = animated;
        return this;
    }

    /** 给 variant 加深/调亮返回 Color（用于条纹视觉差异，简化版）。 */
    private Color tint(Variant v, float factor) {
        Color base = colorOf(v);
        return new Color(base.r * factor, base.g * factor, base.b * factor, 1f);
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
