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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap 风格通用模态框：标准三行结构（标题 / 内容 / 按钮行）。
 *
 * <p>结构：</p>
 * <pre>
 * ┌──────────────────────────────────────┐
 * │   [图标] 标题文字              [×]   │  ← 标题行（可选背景图 banner）
 * ├──────────────────────────────────────┤  ← 可选分隔线
 * │                                      │
 * │   内容（任意 Actor：表单/文本/图片） │  ← 内容行
 * │                                      │
 * ├──────────────────────────────────────┤  ← 可选分隔线
 * │              [取消]  [确认]          │  ← 按钮行
 * └──────────────────────────────────────┘
 * </pre>
 *
 * <p>用法（builder 风格）：</p>
 * <pre>{@code
 * BsModal modal = new BsModal("确认删除", skin)
 *         .titleIcon(drawable)              // 可选：标题前图标
 *         .titleBanner(bannerDrawable)      // 可选：标题行背景图
 *         .content(new Label("..."))        // 必填：内容 actor
 *         .separator(true)                  // 可选：显示分隔线（标题下+按钮上）
 *         .addButton("取消", () -> {}, BsButton.Variant.SECONDARY)
 *         .addButton("确认", () -> doDelete(), BsButton.Variant.PRIMARY);
 * modal.showModal(stage);
 * }</pre>
 *
 * <p>实现：extends {@link Table}（不继承 Window，避免 Window 自带 title 视觉干扰）；
 * 模态 backdrop 复用 BsWindow 同款逻辑；showModal 时居中并 toFront。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsModal extends Table {

    /** 标题行容器（业务方可后续 setTitleIcon/setTitleBanner）。 */
    @Getter private final Table titleRow;
    /** 内容行容器（业务方可后续替换 content）。 */
    @Getter private final Table contentRow;
    /** 按钮行容器（业务方可后续 addButton）。 */
    @Getter private final Table buttonRow;
    /** 标题下分隔线（受 showSeparator 控制，默认隐藏）。 */
    private final Container<Table> sepAfterTitle;
    /** 按钮上分隔线（受 showSeparator 控制，默认隐藏）。 */
    private final Container<Table> sepBeforeButtons;

    private final Label titleLabel;
    private Image titleIconImage;
    private Container<Image> titleIconWrap;
    private Table backdrop;
    private boolean showSeparator = false;
    private final List<TextButton> buttons = new ArrayList<>();

    /** backdrop 点击是否关闭（默认 false，避免误关；业务方可开启）。 */
    @Setter private boolean closeOnBackdropClick = false;

    public BsModal(String title, Skin skin) {
        setBackground(skin.getDrawable("bs-window-bg"));
        setTouchable(Touchable.enabled);
        pad(0);  // 三行各自管 padding，外层不留白

        // 标题行：[图标] 标题文字 [×关闭]
        titleRow = new Table();
        titleRow.pad(14, 18, 14, 18);  // 顶部 padding 加大，避免标题贴上边框
        titleRow.left();
        // 图标占位（默认不显示）
        titleIconWrap = new Container<>();
        titleIconWrap.setVisible(false);
        titleRow.add(titleIconWrap).padRight(8).left();
        // 标题文字（独立 LabelStyle，深色粗体感）
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = skin.getFont("font-lg");
        titleStyle.fontColor = BsTheme.tp();
        titleLabel = new Label(title == null ? "" : title, titleStyle);
        titleLabel.setColor(Color.WHITE);
        titleRow.add(titleLabel).growX().left();
        // 关闭按钮（右上角 X）
        TextButton closeBtn = new TextButton("×", skin, "bs-link");
        TextButton.TextButtonStyle closeStyle = new TextButton.TextButtonStyle(closeBtn.getStyle());
        closeStyle.font = skin.getFont("font-xl");
        closeBtn.setStyle(closeStyle);
        closeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { close(); }
        });
        titleRow.add(closeBtn).size(28, 28).right();
        add(titleRow).growX().row();

        // 标题下分隔线（默认隐藏，showSeparator=true 时显示）
        sepAfterTitle = new Container<>(makeSeparatorLine());
        sepAfterTitle.setVisible(false);
        sepAfterTitle.fillX().height(1f);
        add(sepAfterTitle).growX().row();

        // 内容行：业务方填入
        contentRow = new Table();
        contentRow.pad(14, 18, 14, 18);
        contentRow.left().top();
        add(contentRow).growX().row();

        // 按钮上分隔线
        sepBeforeButtons = new Container<>(makeSeparatorLine());
        sepBeforeButtons.setVisible(false);
        sepBeforeButtons.fillX().height(1f);
        add(sepBeforeButtons).growX().row();

        // 按钮行
        buttonRow = new Table();
        buttonRow.pad(10, 18, 14, 18);
        buttonRow.right();
        add(buttonRow).growX();
    }

    /** 生成一条 1px 淡灰分隔线（用 white drawable 染色）。 */
    private Table makeSeparatorLine() {
        Table line = new Table();
        line.background(BsUI.getSkin().newDrawable("white", BsTheme.bds()));
        line.setHeight(1f);
        return line;
    }

    // ========================= builder API =========================

    public BsModal setTitle(String t) { titleLabel.setText(t); return this; }

    /** 设置标题前的小图标（任意 drawable，建议 16×16 或 24×24）。 */
    public BsModal setTitleIcon(Drawable icon) {
        if (icon != null) {
            titleIconImage = new Image(icon);
            titleIconWrap.setActor(titleIconImage);
            titleIconWrap.size(20, 20);
            titleIconWrap.setVisible(true);
        } else {
            titleIconWrap.setVisible(false);
        }
        return this;
    }

    /** 设置标题行背景图（banner 效果）。 */
    public BsModal setTitleBanner(Drawable banner) {
        if (banner != null) {
            titleRow.setBackground(banner);
            // 浅色 banner 上用深色字
            titleLabel.setColor(BsTheme.tp());
        } else {
            titleRow.setBackground((Drawable) null);
        }
        return this;
    }

    /** 设置整窗背景图（覆盖默认 bs-window-bg）。 */
    public BsModal setBackgroundImage(Drawable bg) {
        if (bg != null) setBackground(bg);
        return this;
    }

    /** 设置内容 actor（替换内容行的子）。 */
    public BsModal content(Actor content) {
        contentRow.clearChildren();
        if (content != null) {
            contentRow.add(content).growX().left();
        }
        return this;
    }

    /** 设置内容行宽度（控制模态框宽度）。 */
    public BsModal contentWidth(float w) {
        if (contentRow.getCells().size > 0) {
            contentRow.getCells().get(0).width(w);
        }
        return this;
    }

    /** 启用/关闭分隔线（标题下 + 按钮上各一条）。默认 false。 */
    public BsModal separator(boolean show) {
        this.showSeparator = show;
        sepAfterTitle.setVisible(show);
        sepBeforeButtons.setVisible(show);
        return this;
    }

    /** 添加底部按钮（从左到右排列；右对齐）。返回 this 供链式调用。 */
    public BsModal addButton(String text, Runnable onClick, BsButton.Variant variant) {
        return addButton(text, onClick, variant, BsButton.Style.SOLID);
    }

    public BsModal addButton(String text, Runnable onClick, BsButton.Variant variant, BsButton.Style style) {
        BsButton btn = new BsButton(text, BsUI.getSkin(), variant, style, BsButton.Size.MD);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                try {
                    if (onClick != null) onClick.run();
                } catch (Throwable t) {
                    log.warn("BsModal button onClick error", t);
                }
                // 默认点击后关闭（业务方在 onClick 里返回前可调 closeOnButton(false) 取消）
                if (closeOnButton) close();
            }
        });
        buttons.add(btn);
        rebuildButtonRow();
        return this;
    }

    /** 是否在按钮点击后自动关闭（默认 true）。 */
    @Setter private boolean closeOnButton = true;

    /** backdrop 点击是否关闭（true=点击背景空白处关闭）。 */
    public BsModal closeOnBackdrop(boolean b) {
        this.closeOnBackdropClick = b;
        return this;
    }

    private void rebuildButtonRow() {
        buttonRow.clearChildren();
        for (int i = 0; i < buttons.size(); i++) {
            if (i > 0) buttonRow.add().width(8);  // 按钮间距
            buttonRow.add(buttons.get(i)).height(32);
        }
    }

    // ========================= 模态显示 =========================

    /**
     * 入场动画策略（showModal 时调用）。
     * 默认 null = 无动画（直接显示）。
     */
    @Setter private EnterAnimation enterAnimation = null;
    /**
     * 出场动画策略（close 时调用）。
     * 默认 null = 无动画（直接 remove）。
     */
    @Setter private ExitAnimation exitAnimation = null;

    /** 入场动画函数式接口（actor 已在 stage 上、已 pack、已居中）。 */
    @FunctionalInterface
    public interface EnterAnimation {
        void play(BsModal modal);
    }

    /** 出场动画函数式接口；onComplete 必须在动画结束时调用（通常触发 modal 真正 remove）。 */
    @FunctionalInterface
    public interface ExitAnimation {
        void play(BsModal modal, Runnable onComplete);
    }

    /** 模态显示：盖 backdrop + 居中加到 stage + 可选入场动画。 */
    public void showModal(Stage stage) {
        // 先 pack 拿到自身尺寸
        pack();

        // backdrop
        Skin skin = BsUI.getSkin();
        backdrop = new Table(skin);
        backdrop.setBackground(skin.newDrawable("white", BsTheme.ov()));
        backdrop.setFillParent(true);
        backdrop.setTouchable(Touchable.enabled);
        backdrop.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (closeOnBackdropClick && event.getTarget() == backdrop) {
                    close();
                }
            }
        });
        stage.addActor(backdrop);
        stage.addActor(this);
        centerOn(stage);
        toFront();

        // 入场动画
        if (enterAnimation != null) {
            try { enterAnimation.play(this); } catch (Throwable t) { /* log */ }
        }
    }

    private void centerOn(Stage stage) {
        setPosition(
                Math.round((stage.getWidth() - getWidth()) / 2f),
                Math.round((stage.getHeight() - getHeight()) / 2f));
    }

    /**
     * 关闭：若有出场动画则先播动画再 remove，否则直接 remove。
     * 重复调用安全（动画进行中再次 close 会被忽略）。
     */
    public void close() {
        if (isClosing()) return;  // 防止动画中重复关闭
        closing = true;
        if (exitAnimation != null && getStage() != null) {
            try {
                exitAnimation.play(this, this::doRemove);
            } catch (Throwable t) {
                doRemove();
            }
        } else {
            doRemove();
        }
    }

    private boolean closing = false;
    public boolean isClosing() { return closing; }

    /** 真正 remove（动画结束后调用，或无动画时直接调）。 */
    private void doRemove() {
        remove();
        if (backdrop != null) {
            backdrop.remove();
            backdrop = null;
        }
        closing = false;
    }

    /**
     * 显示后 {@code seconds} 秒自动关闭（走 close 流程，含 exitAnimation）。
     * <p>适合通知类弹窗（NOTICE/SUCCESS），用户不必手动关闭。
     * 用 scene2d {@link com.badlogic.gdx.scenes.scene2d.actions.Actions#delay} 实现，
     * 无需手动管理 timer，关闭时 actor remove 自动取消未完成的 action。</p>
     *
     * @param seconds 自动关闭延迟；<=0 表示禁用
     */
    public BsModal autoCloseAfter(float seconds) {
        if (seconds <= 0) return this;
        // 用 addAction 加 delay + run；close 已有 closing 标志防重复触发
        addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(seconds),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.run(this::close)
        ));
        return this;
    }

    // ========================= drawable 工具 =========================

    /** 路径 → 已加载 Drawable 的缓存，避免同一张图反复 new Texture 泄漏 GPU 内存。
     *  Drawable 只是 Texture 的视图，多调用方共用同一 Texture（只读渲染，线程安全）。 */
    private static final java.util.Map<String, Drawable> PATH_DRAWABLE_CACHE = new java.util.HashMap<>();

    /**
     * 从图片路径构造 drawable（用作 titleBanner / backgroundImage）。
     *
     * <p><b>带缓存</b>：同一 internalPath 只加载一次 Texture，后续调用复用（返回新 Drawable 视图，
     * 但底层 Texture 共享，不重复占用 GPU 内存）。修正了旧版每次调用都 new Texture 的泄漏。</p>
     *
     * <p>缓存跟随应用生命周期（demo 测试图数量有限）。若需释放，调用 {@link #disposePathCache()}。</p>
     */
    public static Drawable drawableFromPath(String internalPath) {
        if (internalPath == null) return null;
        Drawable cached = PATH_DRAWABLE_CACHE.get(internalPath);
        if (cached != null) return cached;
        Texture tex = new Texture(com.badlogic.gdx.Gdx.files.internal(internalPath));
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Drawable d = new TextureRegionDrawable(new TextureRegion(tex));
        PATH_DRAWABLE_CACHE.put(internalPath, d);
        return d;
    }

    /** 释放 {@link #drawableFromPath} 的缓存 Texture（应用退出时调用）。 */
    public static void disposePathCache() {
        for (Drawable d : PATH_DRAWABLE_CACHE.values()) {
            if (d instanceof TextureRegionDrawable) {
                Texture t = ((TextureRegionDrawable) d).getRegion().getTexture();
                if (t != null) {
                    try { t.dispose(); } catch (Throwable ignored) {}
                }
            }
        }
        PATH_DRAWABLE_CACHE.clear();
    }
}
