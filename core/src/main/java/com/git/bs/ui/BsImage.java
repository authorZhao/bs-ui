package com.git.bs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import lombok.extern.slf4j.Slf4j;

/// Bootstrap 风格独立图片组件（无图注，区别于 `BsFigure` 的"图+说明"）。
///
/// 多来源、可缩放、可设 fallback/边框/点击。适合头像、缩略图、占位图、按钮内图标位等。
///
/// 用法：
/// ```java
/// // 从 classpath 资源加载（Texture 由组件托管）
/// BsImage img = new BsImage()
///         .path("icons/logo.png")
///         .size(120, 120)
///         .fit()
///         .bordered(true)
///         .onClick(() -> System.out.println("clicked"));
///
/// // 直接给 Drawable（调用方自管生命周期）
/// BsImage img2 = new BsImage().drawable(skin.getDrawable("bs-primary-up")).size(64, 64);
/// ```
///
/// 生命周期：通过 `path(String)` / `file(FileHandle)` 加载的 `Texture`
/// 由本组件托管，组件不再使用时调 `dispose()` 释放。通过 `drawable(Drawable)` /
/// `region(TextureRegion)` 传入的资源由调用方管理。
///
/// v1 不含：圆角裁剪（需 shader/mask）、图片预览/缩放弹层、http 远程图、懒加载。
@Slf4j
public class BsImage extends Table {

    private final Skin skin;
    private final Image image;
    private final Table imgWrap;

    /// 通过 path/file 加载、由本组件托管 dispose 的 Texture。
    private Texture ownedTexture;
    /// 无图时显示的兜底 Drawable（null 则用 bs-bg-elevated 纯色块）。
    private Drawable fallback;

    private float imgW = 100f;
    private float imgH = 100f;

    public BsImage() { this(BsUI.getSkin()); }

    public BsImage(Skin skin) {
        this.skin = skin;
        defaults().growX();
        left().top();

        imgWrap = new Table();
        imgWrap.setBackground(skin.getDrawable("bs-window-bg"));
        image = new Image();
        image.setScaling(Scaling.fit);
        imgWrap.add(image).size(imgW, imgH).center();
        add(imgWrap).growX().row();
    }

    // =================== 来源 ===================

    /// 直接设置 Drawable（调用方管理生命周期）。传 null 显示 fallback。
    public BsImage drawable(Drawable d) {
        disposeOwned();
        image.setDrawable(d != null ? d : currentFallback());
        return this;
    }

    /// 用 Texture 作为图片（本组件托管 dispose）。传 null 显示 fallback。
    public BsImage texture(Texture t) {
        disposeOwned();
        if (t == null) {
            image.setDrawable(currentFallback());
            return this;
        }
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        ownedTexture = t;
        image.setDrawable(new TextureRegionDrawable(t));
        return this;
    }

    /// 用 TextureRegion 作为图片（调用方管理 Texture 生命周期）。传 null 显示 fallback。
    public BsImage region(TextureRegion r) {
        disposeOwned();
        image.setDrawable(r != null ? new TextureRegionDrawable(r) : currentFallback());
        return this;
    }

    /// 从 classpath/internal 路径加载（Texture 由组件托管）。加载失败显示 fallback。
    public BsImage path(String internalPath) {
        return file(internalPath == null ? null : Gdx.files.internal(internalPath));
    }

    /// 从任意 FileHandle 加载（Texture 由组件托管）。文件不存在/失败显示 fallback。
    public BsImage file(FileHandle file) {
        disposeOwned();
        if (file == null || !file.exists()) {
            image.setDrawable(currentFallback());
            return this;
        }
        try {
            Texture t = new Texture(file);
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            ownedTexture = t;
            image.setDrawable(new TextureRegionDrawable(t));
        } catch (Throwable e) {
            log.warn("BsImage 加载失败 {}: {}", file, e.toString());
            image.setDrawable(currentFallback());
        }
        return this;
    }

    // =================== 配置 ===================

    /// 图片尺寸（同时也是占位区域大小）。
    public BsImage size(float w, float h) {
        this.imgW = w;
        this.imgH = h;
        imgWrap.getCell(image).size(w, h);
        return this;
    }

    public BsImage scaling(Scaling s) {
        image.setScaling(s);
        return this;
    }

    /// 保持比例、完整显示（默认）。
    public BsImage fit() { return scaling(Scaling.fit); }

    /// 保持比例、填满区域（可能裁切）。
    public BsImage fill() { return scaling(Scaling.fill); }

    /// 拉伸填满（不保持比例）。
    public BsImage stretch() { return scaling(Scaling.stretch); }

    /// 无图时的兜底图。
    public BsImage fallback(Drawable d) {
        this.fallback = d;
        if (image.getDrawable() == null) image.setDrawable(currentFallback());
        return this;
    }

    /// 是否显示圆角底边框（默认有）。
    public BsImage bordered(boolean show) {
        imgWrap.setBackground(show ? skin.getDrawable("bs-window-bg") : null);
        return this;
    }

    /// 是否按组件 bounds 矩形裁剪内容（fill 模式下有用；非圆角）。
    public BsImage clip(boolean c) {
        imgWrap.setClip(c);
        return this;
    }

    /// 点击回调（设了才接收点击）。
    public BsImage onClick(Runnable r) {
        setTouchable(Touchable.enabled);
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (r != null) {
                    try { r.run(); } catch (Throwable t) { log.warn("BsImage onClick error", t); }
                }
            }
        });
        return this;
    }

    // =================== 生命周期 ===================

    /// 释放本组件托管的 Texture（path/file 加载的）。多次调用安全。
    public void dispose() {
        disposeOwned();
    }

    private void disposeOwned() {
        if (ownedTexture != null) {
            try { ownedTexture.dispose(); } catch (Throwable ignored) {}
            ownedTexture = null;
        }
    }

    private Drawable currentFallback() {
        if (fallback != null) return fallback;
        Color c = skin.get("bs-bg-elevated", Color.class);
        return BsSkinFactory.drawableOf(c);
    }
}
