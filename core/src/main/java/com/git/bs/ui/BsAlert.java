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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Bootstrap 5 风格 Alert 警告横条 —— 页面内静态横条提示，
 * 支持 6 色 contextual + 可选关闭按钮，不阻断用户操作（区别于对话框）。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsAlert alert = new BsAlert(skin, "操作成功", BsAlert.Variant.SUCCESS);
 * alert.setDismissible(true);
 * alert.setOnClose(() -> System.out.println("closed"));
 * container.add(alert).growX().row();
 *
 * // 富内容
 * BsAlert warn = new BsAlert(skin, "注意", "本次操作将影响 <b>3 条</b>记录", BsAlert.Variant.WARNING);
 * warn.setContentActor(myTable);    // 替换默认文本
 * }</pre>
 *
 * <p>实现：Table 横向布局 = [左色条] [标题/正文 wrap] [×关闭]。
 * 背景用淡色填充（同 variant 的 light 版本），左色条用饱和色，文字用深色。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsAlert extends Table {

    /** 色条背景缓存：按颜色 int 值缓存 roundRect。 */
    private static final java.util.Map<Integer, Drawable> STRIPE_CACHE = new java.util.HashMap<>();

    public enum Variant { PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO }

    private final Variant variant;
    private String title;
    private String message;
    private boolean dismissible = false;
    private Runnable onClose;
    private Actor contentActor;
    private float prefWidth = 0;

    public BsAlert(Skin skin, String message, Variant variant) {
        this(skin, null, message, variant);
    }

    public BsAlert(Skin skin, String title, String message, Variant variant) {
        this.title = title;
        this.message = message;
        this.variant = variant;
        build();
    }

    private void build() {
        clearChildren();
        Skin skin = BsUI.getSkin();
        Color accent = colorOf(skin, variant);
        // 用 BsSkinFactory 注册的 bs-X-soft-bg（饱和色 + 白 1:9 混合，圆角 6）
        // 不同 variant 的背景色差异明显，info/warn/error/success 一眼区分
        setBackground(skin.getDrawable("bs-" + variant.name().toLowerCase() + "-soft-bg"));
        pad(10, 14, 10, 14);
        left();

        // 左色条（6px 宽，饱和 accent 色）——roundRect 3px 圆角 + 按色缓存，不用 white 1×1
        Container<Actor> stripe = new Container<>();
        Drawable stripeD = STRIPE_CACHE.computeIfAbsent(accent.toIntBits(),
                k -> BsSkinFactory.roundRect(accent, accent, 3, 0));
        stripe.setBackground(stripeD);
        stripe.fill();
        add(stripe).width(6).growY().padRight(10).top();

        // 标题 + 正文
        Table textWrap = new Table();
        textWrap.left().top();
        if (title != null && !title.isEmpty()) {
            Label.LabelStyle tStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
            tStyle.font = skin.getFont("font-lg");
            Label t = new Label(title, tStyle);
            t.setColor(darker(accent, 0.45f));   // 标题用 variant 加深色，呼应背景
            textWrap.add(t).left().row();
        }
        if (contentActor != null) {
            textWrap.add(contentActor).growX().left();
        } else if (message != null && !message.isEmpty()) {
            Label m = new Label(message, skin);
            // 正文用 variant 略深色，保证在淡彩背景上可读
            m.setColor(darker(accent, 0.25f));
            m.setWrap(true);
            textWrap.add(m).growX().left();
        }
        add(textWrap).growX();

        // 右侧关闭按钮
        if (dismissible) {
            Label.LabelStyle xStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
            xStyle.font = skin.getFont("font-xl");
            Label x = new Label("×", xStyle);
            x.setColor(darker(accent, 0.3f));
            Container<Label> xWrap = new Container<>(x);
            xWrap.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            xWrap.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    close();
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    ((Label) ((Container<?>) event.getListenerActor()).getActor())
                            .setColor(darker(accent, 0.6f));
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    ((Label) ((Container<?>) event.getListenerActor()).getActor())
                            .setColor(darker(accent, 0.3f));
                }
            });
            add(xWrap).padLeft(8).top().right();
        }
    }

    /** 关闭（淡出移除）。 */
    public void close() {
        if (onClose != null) {
            try { onClose.run(); } catch (Throwable ignored) {}
        }
        remove();
    }

    // ========================= builder =========================

    public BsAlert setTitle(String title) { this.title = title; build(); return this; }
    public BsAlert setMessage(String m) { this.message = m; build(); return this; }
    public BsAlert setDismissible(boolean d) { this.dismissible = d; build(); return this; }
    public BsAlert setOnClose(Runnable r) { this.onClose = r; return this; }
    public BsAlert setContentActor(Actor a) { this.contentActor = a; build(); return this; }
    public BsAlert setPrefWidth(float w) { this.prefWidth = w; return this; }

    public Variant getVariant() { return variant; }

    // ========================= 颜色工具 =========================

    public static Color colorOf(Skin skin, Variant v) {
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

    /**
     * 推荐 contentActor 内文本使用的颜色：variant 主色加深 35%。
     * <p><b>背景是浅色 soft-bg（始终为浅色，与主题无关，Bootstrap alert 规范），
     * 因此内容必须用深色文本</b>——直接用 {@code new Label(..., skin)} 会拿到当前主题
     * 的 bs-text-primary（dark 主题下是浅色），导致内容在浅色 Alert 背景上"看不见"。
     * 富内容里的 Label 请用此色显式染色。</p>
     * <p>例：</p>
     * <pre>{@code
     * Table content = new Table();
     * Color c = BsAlert.contentTextColor(variant);
     * Label l = new Label("...", skin);
     * l.setColor(c);
     * content.add(l).left().row();
     * alert.setContentActor(content);
     * }</pre>
     */
    public static Color contentTextColor(Variant v) {
        return darker(colorOf(null, v), 0.35f);
    }

    /** 给基色与白色混合（factor 越大越白）。 */
    private static Color lightTint(Color base, float factor) {
        return new Color(
                base.r + (1 - base.r) * factor,
                base.g + (1 - base.g) * factor,
                base.b + (1 - base.b) * factor,
                1f);
    }

    /** 给基色变深（factor 越大越黑）。 */
    private static Color darker(Color base, float factor) {
        return new Color(
                base.r * (1 - factor),
                base.g * (1 - factor),
                base.b * (1 - factor),
                1f);
    }
}
