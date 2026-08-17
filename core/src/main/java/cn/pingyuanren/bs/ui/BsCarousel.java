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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap 5 风格轮播图（Carousel）—— 自动播放 + 左右箭头 + 指示点。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsCarousel carousel = new BsCarousel(skin);
 * carousel.setSize(640, 320);
 * carousel.setAutoPlay(true);
 * carousel.setInterval(3f);
 * carousel.addSlide(makeSlide("第一张", 0x0D6EFD));
 * carousel.addSlide(makeSlide("第二张", 0xDC3545));
 * carousel.addSlide(makeSlide("第三张", 0x198754));
 * stage.addActor(carousel);
 * }</pre>
 *
 * <p>实现：所有 slide 叠在同一个 Table（堆叠 via addActor 直接放到同位置），
 * 用 alpha 切换。act 中累积时间，到达 interval 自动切换。
 * 左右箭头 + 底部指示点可手动切换。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsCarousel extends Table {

    private final List<Actor> slides = new ArrayList<>();
    private final com.badlogic.gdx.scenes.scene2d.Group slideLayer;  // 横向并排放所有 slide 的层，切换=平移 X
    private final Table controlLayer;      // 左右箭头
    private final Table indicators;        // 底部指示点
    private int current = 0;
    private boolean autoPlay = true;
    private float interval = 3f;
    private float timer = 0f;
    private float slideDuration = 0.4f;    // 平移动画时长

    public BsCarousel(Skin skin) {
        setClip(true);   // 裁剪超出范围的 slide（横向滑动只露出当前一张）
        setBackground(BsSkinFactory.drawableOf(BsTheme.bhH()));

        // slide 层：用 Group 自由绝对定位，所有 slide 横向并排（i*width, 0）。
        // 切换时平移 slideLayer 的 X 到 -current*width，配合 setClip 实现左右滑动。
        slideLayer = new com.badlogic.gdx.scenes.scene2d.Group();

        // 控制层：左右箭头（绝对定位到左右边缘、垂直居中）
        controlLayer = new Table();
        controlLayer.setFillParent(true);
        controlLayer.center();
        // 左箭头固定在左边缘，右箭头固定在右边缘，中间用 expandX 空白 cell 撑开
        // 箭头 cell 不 expandX（只占自然宽），避免被拉成贯穿高度的竖条
        controlLayer.add(makeArrow("‹", () -> prev())).left().pad(8);
        controlLayer.add().expandX();   // 中间弹性占位（Table 默认透传事件，不拦截 slide 点击）
        controlLayer.add(makeArrow("›", () -> next())).right().pad(8);

        // 指示点层：底部圆点
        indicators = new Table();
        indicators.padBottom(8);

        addActor(slideLayer);
        addActor(controlLayer);
        Table bottomWrap = new Table();
        bottomWrap.setFillParent(true);
        bottomWrap.bottom();
        bottomWrap.add(indicators).padBottom(8);
        addActor(bottomWrap);
    }

    /** 添加一张 slide（任意 actor，铺满整个 carousel 宽高）。 */
    public BsCarousel addSlide(Actor slide) {
        slides.add(slide);
        slideLayer.addActor(slide);
        layoutSlides();         // 重新定位所有 slide 的横向位置
        snapToCurrent();        // 平移到当前（无动画）
        rebuildIndicators();
        return this;
    }

    /**
     * 按当前尺寸定位所有 slide：第 i 张放在 (i*width, 0)，尺寸 = carousel 尺寸。
     * carousel 尺寸变化时也要调（已 override setSize/layout）。
     */
    private void layoutSlides() {
        float w = getWidth();
        float h = getHeight();
        for (int i = 0; i < slides.size(); i++) {
            Actor s = slides.get(i);
            s.setBounds(i * w, 0, w, h);
        }
        slideLayer.setSize(slides.size() * w, h);
    }

    /** 立即跳到当前索引（无动画），平移 slideLayer。 */
    private void snapToCurrent() {
        slideLayer.setX(-current * getWidth());
    }

    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
        layoutSlides();
        snapToCurrent();
    }

    @Override
    protected void sizeChanged() {
        layoutSlides();
        snapToCurrent();
    }

    private void rebuildIndicators() {
        Skin skin0 = BsUI.getSkin();
        Label.LabelStyle lgStyle = new Label.LabelStyle(skin0.get(Label.LabelStyle.class));
        lgStyle.font = skin0.getFont("font-lg");
        indicators.clearChildren();
        for (int i = 0; i < slides.size(); i++) {
            final int idx = i;
            Label dot = new Label("●", lgStyle);
            dot.setColor(i == current
                    ? new Color(1, 1, 1, 1f)
                    : new Color(1, 1, 1, 0.4f));
            Container<Label> wrap = new Container<>(dot);
            wrap.setTouchable(Touchable.enabled);
            wrap.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { goTo(idx); }
            });
            indicators.add(wrap).pad(4);
        }
    }

    private Actor makeArrow(String symbol, Runnable onClick) {
        // 用 bootstrap-icons 的 chevron-left/right（程序化 SVG 图标，白色染色），
        // 不用 ‹ › 字符——字体不含这两个字形会显示空白。symbol 仅用于判断方向。
        boolean pointRight = "›".equals(symbol);
        Drawable iconD = BsIcon.get(pointRight ? "chevron-right" : "chevron-left",
                new Color(1, 1, 1, 0.85f));
        Actor arrowActor;
        if (iconD != null) {
            com.badlogic.gdx.scenes.scene2d.ui.Image img =
                    new com.badlogic.gdx.scenes.scene2d.ui.Image(iconD);
            img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            arrowActor = img;
        } else {
            // atlas 未加载兜底：用 BsSkinFactory 的程序化三角箭头（白色，24×24）
            arrowActor = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                    BsSkinFactory.arrowDrawable(Color.WHITE, pointRight));
        }
        Container<Actor> wrap = new Container<>(arrowActor);
        wrap.setBackground(BsSkinFactory.drawableOf(new Color(0, 0, 0, 0.3f)));
        // 固定箭头方块大小（Bootstrap carousel 左右箭头是圆形/圆角小方块），
        // fill(false) 防止被父 cell 拉伸成贯穿高度的竖条
        wrap.size(36, 36);
        wrap.fill(false);
        wrap.setTouchable(Touchable.enabled);
        wrap.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                try { onClick.run(); } catch (Throwable t) { log.warn("arrow", t); }
            }
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                arrowActor.setColor(Color.WHITE);
            }
            @Override public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                arrowActor.setColor(new Color(1, 1, 1, 0.85f));
            }
        });
        return wrap;
    }

    /** 下一张。 */
    public void next() {
        if (slides.isEmpty()) return;
        int n = (current + 1) % slides.size();
        goTo(n);
    }

    /** 上一张。 */
    public void prev() {
        if (slides.isEmpty()) return;
        int n = (current - 1 + slides.size()) % slides.size();
        goTo(n);
    }

    /** 切到指定索引（带左右滑动动画）。 */
    public void goTo(int idx) {
        if (idx < 0 || idx >= slides.size() || idx == current) return;
        current = idx;
        timer = 0;
        // 平移 slideLayer：X 从当前位置滑到 -current*width
        slideLayer.clearActions();
        slideLayer.addAction(Actions.moveTo(-current * getWidth(), 0,
                slideDuration, Interpolation.fade));
        rebuildIndicators();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (autoPlay && !slides.isEmpty()) {
            timer += delta;
            if (timer >= interval) {
                timer = 0;
                next();
            }
        }
    }

    public BsCarousel setAutoPlay(boolean v) { this.autoPlay = v; return this; }
    public BsCarousel setInterval(float sec) { this.interval = sec; return this; }
    /** 设置切换动画时长（左右滑动）。 */
    public BsCarousel setSlideDuration(float sec) { this.slideDuration = sec; return this; }
    /** @deprecated 改用 {@link #setSlideDuration}，左右滑动模式下无淡入淡出。保留向后兼容。 */
    @Deprecated
    public BsCarousel setFadeDuration(float sec) { this.slideDuration = sec; return this; }
    public int getCurrent() { return current; }
    public int getSlideCount() { return slides.size(); }
}
