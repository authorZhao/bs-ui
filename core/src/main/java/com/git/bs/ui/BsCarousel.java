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
    private final Table slideLayer;        // 堆叠所有 slide 的层
    private final Table controlLayer;      // 左右箭头
    private final Table indicators;        // 底部指示点
    private int current = 0;
    private boolean autoPlay = true;
    private float interval = 3f;
    private float timer = 0f;
    private float fadeDuration = 0.4f;

    public BsCarousel(Skin skin) {
        setClip(true);   // 裁剪超出范围的 slide
        setBackground(skin.newDrawable("white", BsTheme.bhH()));

        // slide 层（所有 slide 叠在同一位置）
        slideLayer = new Table();
        slideLayer.setFillParent(true);

        // 控制层：左右箭头
        controlLayer = new Table();
        controlLayer.setFillParent(true);
        controlLayer.left();
        controlLayer.add(makeArrow("‹", () -> prev())).expandX().left().pad(8);
        controlLayer.add(makeArrow("›", () -> next())).expandX().right().pad(8);

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

    /** 添加一张 slide（任意 actor，铺满整个 carousel）。 */
    public BsCarousel addSlide(Actor slide) {
        slides.add(slide);
        slideLayer.clearChildren();
        // 所有 slide 都堆在 cell 里，位置 0,0，size = carousel
        for (Actor s : slides) {
            slideLayer.add(s).grow();
            slideLayer.row();
        }
        // 只显示当前
        updateVisibility(false);
        rebuildIndicators();
        return this;
    }

    private void rebuildIndicators() {
        indicators.clearChildren();
        for (int i = 0; i < slides.size(); i++) {
            final int idx = i;
            Label dot = new Label("●", BsUI.getSkin());
            dot.setColor(i == current
                    ? new Color(1, 1, 1, 1f)
                    : new Color(1, 1, 1, 0.4f));
            dot.setFontScale(1.2f);
            Container<Label> wrap = new Container<>(dot);
            wrap.setTouchable(Touchable.enabled);
            wrap.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { goTo(idx); }
            });
            indicators.add(wrap).pad(4);
        }
    }

    private Actor makeArrow(String symbol, Runnable onClick) {
        Skin skin = BsUI.getSkin();
        Label arrow = new Label(symbol, skin);
        arrow.setColor(new Color(1, 1, 1, 0.85f));
        arrow.setFontScale(2f);
        Container<Label> wrap = new Container<>(arrow);
        wrap.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.3f)));
        wrap.pad(4, 10, 4, 10);
        wrap.setTouchable(Touchable.enabled);
        wrap.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                try { onClick.run(); } catch (Throwable t) { log.warn("arrow", t); }
            }
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                arrow.setColor(Color.WHITE);
            }
            @Override public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                arrow.setColor(new Color(1, 1, 1, 0.85f));
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

    /** 切到指定索引（带淡入淡出）。 */
    public void goTo(int idx) {
        if (idx < 0 || idx >= slides.size() || idx == current) return;
        int old = current;
        current = idx;
        timer = 0;
        // 淡入淡出
        if (old >= 0 && old < slides.size()) {
            Actor oldA = slides.get(old);
            oldA.addAction(Actions.sequence(
                    Actions.fadeOut(fadeDuration, Interpolation.fade),
                    Actions.visible(false)
            ));
        }
        Actor newA = slides.get(current);
        newA.setVisible(true);
        newA.setColor(1, 1, 1, 0);
        newA.toFront();
        newA.addAction(Actions.fadeIn(fadeDuration, Interpolation.fade));
        rebuildIndicators();
    }

    private void updateVisibility(boolean animate) {
        for (int i = 0; i < slides.size(); i++) {
            Actor s = slides.get(i);
            if (i == current) {
                s.setVisible(true);
                s.setColor(1, 1, 1, 1);
            } else {
                s.setVisible(false);
                s.setColor(1, 1, 1, 0);
            }
        }
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
    public BsCarousel setFadeDuration(float sec) { this.fadeDuration = sec; return this; }
    public int getCurrent() { return current; }
    public int getSlideCount() { return slides.size(); }
}
