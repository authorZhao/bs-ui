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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap 5 风格 Toast 轻提示（吐司）。
 *
 * <p>屏幕角落短暂出现、自动消失的轻量通知，不阻断用户操作。6 色 contextual，
 * 默认右上角堆叠，定时消失（默认 3s）。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * // 静态快捷入口（最常用）
 * BsToast.show(stage, skin, "保存成功", BsToast.Variant.SUCCESS);
 * BsToast.show(stage, skin, "网络异常", BsToast.Variant.DANGER, 5f);
 *
 * // builder 风格（更多控制）
 * new BsToast(skin, "用户已创建", BsToast.Variant.SUCCESS)
 *     .title("操作完成")
 *     .placement(BsToast.Placement.BOTTOM_RIGHT)
 *     .duration(4f)
 *     .show(stage);
 * }</pre>
 *
 * <p>实现：每个 Toast 是一个 Table（圆角白底 + 左侧色条 + 标题/正文），
 * show 时按 placement 堆叠到 stage 上，定时用 Actions.sequence(fadeIn → delay → fadeOut → remove)。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsToast extends Table {

    public enum Variant { PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO }
    public enum Placement { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT, TOP_CENTER }

    private static final float DEFAULT_DURATION = 3f;
    private static final float FADE_TIME = 0.3f;

    private final Variant variant;
    private String title;
    private String message;
    private float duration = DEFAULT_DURATION;
    private Placement placement = Placement.TOP_RIGHT;
    private float toastWidth = 320;

    /** 全局活动 Toast 列表（按 placement 分组用于堆叠布局）。 */
    private static final List<BsToast> ACTIVE = new ArrayList<>();

    public BsToast(Skin skin, String message, Variant variant) {
        this(skin, null, message, variant);
    }

    public BsToast(Skin skin, String title, String message, Variant variant) {
        this.title = title;
        this.message = message;
        this.variant = variant;
        build();
    }

    private void build() {
        Skin skin = BsUI.getSkin();
        Color accent = colorOf(skin, variant);
        Color bg = lightTint(accent, 0.88f);   // 淡彩背景（与白 1:9 混合）
        Color titleColor = darker(accent, 0.35f);  // 标题用 variant 深色
        Color bodyColor = darker(accent, 0.15f);   // 正文略深，保证可读

        // 整体背景染 variant 淡色
        setBackground(skin.newDrawable("bs-" + variant.name().toLowerCase() + "-soft-bg"));
        pad(10);
        // 左侧色条（6px 宽，饱和 accent 色，让颜色一目了然）
        Table body = new Table();
        body.left().top();
        body.pad(0, 8, 0, 8);
        if (title != null && !title.isEmpty()) {
            Label t = new Label(title, skin);
            t.setColor(titleColor);
            t.setFontScale(1.05f);
            body.add(t).left().row();
        }
        Label msg = new Label(message == null ? "" : message, skin);
        msg.setColor(bodyColor);
        msg.setWrap(true);
        body.add(msg).growX().left();

        // 色条 + body
        add(colorStripe(variant)).width(6).growY();
        add(body).width(toastWidth - 20).padLeft(8).padRight(8).grow();
    }

    /** 生成色条 actor（用 white drawable 染 accent 饱和色）。 */
    private Actor colorStripe(Variant v) {
        Skin skin = BsUI.getSkin();
        Container<Actor> c = new Container<>();
        Drawable d = skin.newDrawable("white", colorOf(skin, v));
        c.setBackground(d);
        c.fill();
        return c;
    }

    /** 基色与白色混合（factor 越大越白）。 */
    private static Color lightTint(Color base, float factor) {
        return new Color(
                base.r + (1 - base.r) * factor,
                base.g + (1 - base.g) * factor,
                base.b + (1 - base.b) * factor,
                1f);
    }

    /** 基色变深（factor 越大越黑）。 */
    private static Color darker(Color base, float factor) {
        return new Color(
                base.r * (1 - factor),
                base.g * (1 - factor),
                base.b * (1 - factor),
                1f);
    }

    // ========================= builder =========================

    public BsToast title(String t) { this.title = t; rebuild(); return this; }
    public BsToast message(String m) { this.message = m; rebuild(); return this; }
    public BsToast duration(float sec) { this.duration = sec; return this; }
    public BsToast placement(Placement p) { this.placement = p; return this; }
    public BsToast toastWidth(float w) { this.toastWidth = w; rebuild(); return this; }

    private void rebuild() {
        clearChildren();
        build();
    }

    // ========================= show / 堆叠 =========================

    /** 显示到 stage，按 placement 堆叠定位。 */
    public void show(Stage stage) {
        if (stage == null) return;
        setSize(toastWidth, getPrefHeight());
        pack();
        // 入场：透明 → 淡入
        setColor(1, 1, 1, 0);
        stage.addActor(this);
        ACTIVE.add(this);

        positionOnStage(stage);

        addAction(Actions.sequence(
                Actions.fadeIn(FADE_TIME, Interpolation.fade),
                Actions.delay(Math.max(0.1f, duration - FADE_TIME * 2)),
                Actions.fadeOut(FADE_TIME, Interpolation.fade),
                Actions.run(this::onRemove),
                Actions.removeActor()
        ));
    }

    /** 移除时刷新堆叠布局。 */
    private void onRemove() {
        ACTIVE.remove(this);
        Stage st = getStage();
        if (st != null) {
            relayoutPlacement(st, placement);
        }
    }

    /** 计算自身位置（按 placement + 同 placement 已存在的 toast 数堆叠）。 */
    private void positionOnStage(Stage stage) {
        relayoutPlacement(stage, placement);
    }

    /** 重新布局指定 placement 的所有 Toast（堆叠 + 边距）。 */
    private static void relayoutPlacement(Stage stage, Placement p) {
        float margin = 12;
        float gap = 8;
        float stageW = stage.getWidth();
        float stageH = stage.getHeight();
        List<BsToast> sameSide = new ArrayList<>();
        for (BsToast t : ACTIVE) {
            if (t.getStage() == stage && t.placement == p) sameSide.add(t);
        }
        float y;
        boolean top = (p == Placement.TOP_RIGHT || p == Placement.TOP_LEFT || p == Placement.TOP_CENTER);
        boolean right = (p == Placement.TOP_RIGHT || p == Placement.BOTTOM_RIGHT);
        boolean center = (p == Placement.TOP_CENTER);
        y = top ? stageH - margin : margin;
        for (BsToast t : sameSide) {
            t.pack();
            float x;
            if (center) {
                x = (stageW - t.getWidth()) / 2f;
            } else if (right) {
                x = stageW - margin - t.getWidth();
            } else {
                x = margin;
            }
            if (top) {
                t.setPosition(x, y - t.getHeight());
                y -= t.getHeight() + gap;
            } else {
                t.setPosition(x, y);
                y += t.getHeight() + gap;
            }
        }
    }

    // ========================= 静态便捷入口 =========================

    public static BsToast show(Stage stage, Skin skin, String message, Variant variant) {
        return show(stage, skin, null, message, variant, DEFAULT_DURATION, Placement.TOP_RIGHT);
    }

    public static BsToast show(Stage stage, Skin skin, String message, Variant variant, float duration) {
        return show(stage, skin, null, message, variant, duration, Placement.TOP_RIGHT);
    }

    public static BsToast show(Stage stage, Skin skin, String title, String message, Variant variant) {
        return show(stage, skin, title, message, variant, DEFAULT_DURATION, Placement.TOP_RIGHT);
    }

    public static BsToast show(Stage stage, Skin skin, String title, String message,
                                Variant variant, float duration, Placement placement) {
        BsToast t = new BsToast(skin, title, message, variant)
                .duration(duration)
                .placement(placement);
        try {
            t.show(stage);
        } catch (Throwable ex) {
            log.warn("BsToast show failed", ex);
        }
        return t;
    }

    /** 立即清除所有活动 Toast。 */
    public static void clearAll() {
        for (BsToast t : new ArrayList<>(ACTIVE)) {
            try {
                t.remove();
            } catch (Throwable ignored) {}
        }
        ACTIVE.clear();
    }

    // ========================= 颜色 =========================

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
}
