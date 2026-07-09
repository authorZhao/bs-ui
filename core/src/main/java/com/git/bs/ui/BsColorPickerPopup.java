package com.git.bs.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * 颜色选择器浮层（参考 VISUI 风格）：SV 色块 + Hue 滑条 + Hex/RGB 输入 + 预设色板。
 *
 * <p>布局：</p>
 * <pre>
 * ┌─────────────────────────────┐
 * │  ┌──────────┐  ┌────────┐   │
 * │  │  SV 方块 │  │ 色相条 │   │
 * │  │  (S×L)   │  │  H 0-360│   │
 * │  └──────────┘  └────────┘   │
 * │  当前色预览（横条）          │
 * │  Hex: [#0D6EFD____]          │
 * │  预设色板（12 色 4×3）       │
 * │  [取消]            [确定]    │
 * └─────────────────────────────┘
 * </pre>
 *
 * <p>实现要点：</p>
 * <ul>
 *   <li>SV 方块：根据当前 hue 生成 128×128 Pixmap，X=S(0~1)，Y=L(顶部 1→底部 0)。
 *       hue 变化时重建 Texture（成本可控，仅 hue slider change 时触发）。</li>
 *   <li>Hue 滑条：自生成的色相渐变 drawable 作为 slider 背景。简化：用 HSL 模型，
 *       用一个 {@link BsSlider}(0~360) 让用户拖动改 hue。</li>
 *   <li>选 SV：在 SV 方块上点击/拖动，坐标 → s/l。</li>
 *   <li>Hex 输入：双向同步（输入 #RRGGBB 解析回 HSL，颜色变化时回填 hex）。</li>
 * </ul>
 */
@Slf4j
public class BsColorPickerPopup {

    private static final int[] PRESET_HEX = {
            0x000000, 0xFFFFFF, 0x6C757D, 0xDEE2E6,
            0x0D6EFD, 0x6610F2, 0x6F42C1, 0xD63384,
            0xDC3545, 0xFD7E14, 0xFFC107, 0x198754,
            0x20C997, 0x0DCAF0, 0x50C878, 0x95A5A6
    };

    /** 预设色 Drawable 静态缓存（按 RGB int 为 key），跨实例复用，避免每次 show 都重建 16 个 Texture。
     *  <p>生命周期跟随应用；退出时调 {@link #disposePresetCache()} 释放。参见 {@link BsModal#disposePathCache()}。 */
    private static final java.util.Map<Integer, TextureRegionDrawable> PRESET_CACHE = new java.util.HashMap<>();

    private static final int SV_SIZE = 140;
    private static final int HUE_W = 24;
    private static final int HUE_H = SV_SIZE;

    private Table root;
    private Actor backdrop;
    private boolean open;
    private Consumer<Color> onPick;
    private Runnable onClose;

    /** HSV 状态（内部转 HSL 用，但接口概念是 HSV：S/v 都是 0~1）。 */
    private float h = 210f, s = 1f, v = 1f;

    // UI 引用
    private Image svImage;
    private TextureRegionDrawable svDrawable;
    private Texture svTexture;
    private Texture hueTexture;   // hue 渐变条 Texture，close 时释放（不存 skin 托管）
    private Image hueImage;
    private Image cursorSV;
    private BsSlider hueSlider;
    private Image previewImage;
    private Drawable previewDrawable;   // 白底，靠 Image.setColor 染色
    private TextField hexField;
    /** makeSolidDrawable 创建的 Texture 列表（2×2 小图，preview/cursor/knob/预设色板），close 时统一释放。 */
    private final Array<Texture> solidTextures = new Array<>();

    public BsColorPickerPopup(Skin skin) {
    }

    public void setOnPick(Consumer<Color> onPick) { this.onPick = onPick; }
    public void setOnClose(Runnable onClose) { this.onClose = onClose; }

    public void show(Stage stage, Actor anchor, Color initial) {
        if (open) close();
        if (initial != null) {
            float[] hsv = rgbToHsv(initial.r, initial.g, initial.b);
            h = hsv[0]; s = hsv[1]; v = hsv[2];
        }

        Vector2 pos = anchor.localToStageCoordinates(new Vector2(0, 0));
        float anchorBottom = pos.y;
        float anchorTop = pos.y + anchor.getHeight();

        backdrop = new Actor();
        backdrop.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        backdrop.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        backdrop.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                close(); return true;
            }
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) { close(); return true; }
                return false;
            }
        });

        // 预生成 hue 渐变（一次性）；previewDrawable 是白底，靠 Image.setColor 染色
        previewDrawable = makeSolidDrawable(Color.WHITE);

        root = new Table();
        root.setBackground(BsUI.getSkin().getDrawable("bs-window-bg"));
        root.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        buildContent();

        root.pack();
        float x = pos.x;
        float y = anchorBottom - root.getHeight();
        if (y < 0) y = anchorTop;
        if (x + root.getWidth() > stage.getWidth()) x = stage.getWidth() - root.getWidth();
        if (x < 0) x = 0;
        root.setPosition(x, y);

        stage.addActor(backdrop);
        stage.addActor(root);
        stage.setKeyboardFocus(backdrop);
        open = true;
    }

    private void buildContent() {
        Skin skin = BsUI.getSkin();
        root.clearChildren();

        // ===== 上半部：SV 方块 + Hue 滑条 =====
        Table topRow = new Table();

        // SV 方块（可点击/拖动选 SV）
        svTexture = makeSvTexture(h);
        svDrawable = new TextureRegionDrawable(new TextureRegion(svTexture));
        svImage = new Image(svDrawable);
        svImage.setSize(SV_SIZE, SV_SIZE);
        svImage.setScaling(com.badlogic.gdx.utils.Scaling.stretch);
        // 选色光标：用 Group 作为容器，让 cursor 可以绝对定位（Group 不强制子 actor fill）
        cursorSV = new Image(makeSolidDrawable(new Color(1, 1, 1, 0.9f)));
        cursorSV.setSize(8, 8);
        com.badlogic.gdx.scenes.scene2d.Group svGroup = new com.badlogic.gdx.scenes.scene2d.Group();
        svGroup.setSize(SV_SIZE, SV_SIZE);
        svGroup.addActor(svImage);
        svGroup.addActor(cursorSV);

        // 给 SV 方块加拖动监听（事件挂在 Group 上，坐标已是 Group 局部）
        svGroup.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly);
        svImage.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        svImage.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                handleSvPick(x, y);
                return true;
            }
            @Override public void touchDragged(InputEvent event, float x, float y, int pointer) {
                handleSvPick(x, y);
            }
        });

        topRow.add(svGroup).size(SV_SIZE, SV_SIZE).pad(4);

        // Hue 滑条（垂直）：用 hue 渐变图作为 slider bg
        // hue 渐变必须用真实像素（不能靠 setColor 染色），保留 new Texture，但实例持有、close 时 dispose。
        // 每次打开都覆盖 skin 里的 bs-hue-slider style（指向当前实例的 hueTexture），
        // 避免复用旧 style 指向已 dispose 的 Texture。
        hueTexture = makeHueBarTexture();
        Drawable hueDrawable = new TextureRegionDrawable(new TextureRegion(hueTexture));
        com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle hueStyle =
                new com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle();
        hueStyle.background = hueDrawable;
        hueStyle.knob = makeSolidDrawable(Color.WHITE);
        skin.add("bs-hue-slider", hueStyle, com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle.class);
        hueSlider = new BsSlider(0, 360, 1, true, skin, "bs-hue-slider");
        hueSlider.setValue(h);
        hueSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                h = hueSlider.getValue();
                rebuildSvTexture();
                updatePreview();
            }
        });
        topRow.add(hueSlider).size(HUE_W + 6, HUE_H).pad(4).top();

        root.add(topRow).pad(4).row();

        // ===== 当前色预览 + Hex =====
        Table previewRow = new Table();
        previewImage = new Image(previewDrawable);
        previewImage.setColor(hsvToColor());   // Image.setColor 染色（白底 × actor color = 目标色）
        Container<Image> previewWrap = new Container<>(previewImage);
        previewWrap.size(60, 28);
        previewWrap.background(skin.newDrawable("white", BsTheme.bh()));
        previewRow.add(previewWrap).padRight(6);

        previewRow.add(new Label("Hex:", skin)).padRight(4);
        hexField = new TextField(colorToHex(hsvToColor()), skin);
        hexField.setTextFieldFilter((field, c) -> "0123456789abcdefABCDEF#".indexOf(c) >= 0);
        hexField.setMaxLength(7);
        hexField.setTextFieldListener((field, key) -> {
            if (key == '\n' || key == '\t') {
                Color parsed = parseHex(field.getText());
                if (parsed != null) {
                    float[] hsv = rgbToHsv(parsed.r, parsed.g, parsed.b);
                    h = hsv[0]; s = hsv[1]; v = hsv[2];
                    hueSlider.setValue(h);
                    rebuildSvTexture();
                    updatePreview();
                }
            }
        });
        previewRow.add(hexField).width(80);
        root.add(previewRow).pad(4).row();

        // ===== 预设色板 =====
        root.add(new Label(BsI18n.get("core.color.preset", "预设"), skin)).left().pad(4).row();
        Table swatches = new Table();
        int cols = 8;
        for (int i = 0; i < PRESET_HEX.length; i++) {
            final int hex = PRESET_HEX[i];
            Color c = new Color(((hex >> 16) & 0xFF) / 255f,
                    ((hex >> 8) & 0xFF) / 255f, (hex & 0xFF) / 255f, 1f);
            // 预设色块用 Image（不是 TextButton）+ ClickListener；
            // TextButton 会被 style.up（default=蓝色）覆盖 setBackground，导致全蓝。
            // Drawable 走静态缓存 PRESET_CACHE（按 hex 复用），避免每次 show 重建 16 个 Texture。
            Image swatch = new Image(presetDrawable(hex, c));
            swatch.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            swatch.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    float[] hsv = rgbToHsv(c.r, c.g, c.b);
                    h = hsv[0]; s = hsv[1]; v = hsv[2];
                    hueSlider.setValue(h);
                    rebuildSvTexture();
                    updatePreview();
                }
            });
            swatches.add(swatch).size(28, 22).pad(1);
            if ((i + 1) % cols == 0) swatches.row();
        }
        root.add(swatches).pad(2, 4, 4, 4).row();

        // ===== 确认 / 取消 =====
        Table btnRow = new Table();
        TextButton cancel = new TextButton(BsI18n.get("btn.cancel", "取消"), skin, "bs-btn-secondary");
        cancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { close(); }
        });
        TextButton ok = new TextButton(BsI18n.get("btn.ok", "确定"), skin, "bs-btn-primary");
        ok.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { confirm(); }
        });
        btnRow.add(cancel).width(80).padRight(6);
        btnRow.add(ok).width(80);
        root.add(btnRow).pad(4);

        // cursor 初始定位
        updateSvCursor();
    }

    private void handleSvPick(float x, float y) {
        // SV 方块坐标系：左下角原点，x→右(=s)，y→上(=v)
        // scene2d InputListener 的 x/y 已是 actor 局部坐标（左下原点）
        float sx = com.badlogic.gdx.math.MathUtils.clamp(x / SV_SIZE, 0, 1);
        float sy = com.badlogic.gdx.math.MathUtils.clamp(y / SV_SIZE, 0, 1);
        // 注意：SV 方块习惯是 X=S（左→右饱和度增加），Y=V（上→下明度递减，但 scene2d y 越上越大）
        // 所以 v = y（y=1 顶部 = v 高 = 鲜艳）
        s = sx;
        v = sy;
        updateSvCursor();
        updatePreview();
    }

    private void updateSvCursor() {
        if (cursorSV == null) return;
        // 把 cursor 定位到 (s*SV_SIZE - cursorW/2, v*SV_SIZE - cursorH/2)
        // Stack 中 cursor 居中后通过 setBounds 移动；Stack 默认子元素 fill，需要 setPosition
        float cx = s * SV_SIZE - cursorSV.getWidth() / 2f;
        float cy = v * SV_SIZE - cursorSV.getHeight() / 2f;
        cursorSV.setPosition(cx, cy);
    }

    private void updatePreview() {
        Color c = hsvToColor();
        if (previewImage != null) previewImage.setColor(c);
        if (hexField != null) {
            hexField.setText(colorToHex(c));
        }
    }

    private void rebuildSvTexture() {
        if (svTexture != null) svTexture.dispose();
        svTexture = makeSvTexture(h);
        svDrawable.setRegion(new TextureRegion(svTexture));
    }

    private void confirm() {
        Color c = hsvToColor();
        log.info("BsColorPickerPopup picked: {}", colorToHex(c));
        if (onPick != null) {
            try { onPick.accept(c); } catch (Throwable t) { log.warn("onPick error", t); }
        }
        close();
    }

    // =================== 颜色 / Pixmap 工具 ===================

    private Color hsvToColor() {
        float[] rgb = hsvToRgb(h, s, v);
        return new Color(rgb[0], rgb[1], rgb[2], 1f);
    }

    /** HSV→RGB。h: 0~360, s/v: 0~1。 */
    private static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float hp = (h % 360f) / 60f;
        float x = c * (1 - Math.abs((hp % 2f) - 1f));
        float r, g, b;
        if (hp < 1)      { r = c; g = x; b = 0; }
        else if (hp < 2) { r = x; g = c; b = 0; }
        else if (hp < 3) { r = 0; g = c; b = x; }
        else if (hp < 4) { r = 0; g = x; b = c; }
        else if (hp < 5) { r = x; g = 0; b = c; }
        else             { r = c; g = 0; b = x; }
        float m = v - c;
        return new float[]{r + m, g + m, b + m};
    }

    /** RGB→HSV。返回 [h(0~360), s(0~1), v(0~1)]。 */
    private static float[] rgbToHsv(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = 60f * (((g - b) / d) % 6f);
        else if (max == g) h = 60f * ((b - r) / d + 2f);
        else h = 60f * ((r - g) / d + 4f);
        if (h < 0) h += 360f;
        float s = max == 0 ? 0 : d / max;
        return new float[]{h, s, max};
    }

    private static String colorToHex(Color c) {
        return String.format("#%02X%02X%02X",
                Math.round(c.r * 255), Math.round(c.g * 255), Math.round(c.b * 255));
    }

    private static Color parseHex(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) return null;
        try {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            return new Color(r / 255f, g / 255f, b / 255f, 1f);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 生成一个 SV 色块（X=S 0~1，Y=V 0~1；hue 固定）。 */
    private static Texture makeSvTexture(float hue) {
        Pixmap pix = new Pixmap(SV_SIZE, SV_SIZE, Pixmap.Format.RGB888);
        for (int y = 0; y < SV_SIZE; y++) {
            for (int x = 0; x < SV_SIZE; x++) {
                float sx = x / (float) (SV_SIZE - 1);
                // y 反转：scene2d y=0 在底部，V=0 在底部 → 不反转
                float vy = y / (float) (SV_SIZE - 1);
                float[] rgb = hsvToRgb(hue, sx, vy);
                int r = Math.round(rgb[0] * 255);
                int g = Math.round(rgb[1] * 255);
                int b = Math.round(rgb[2] * 255);
                // RGB888 Pixmap 用 int 编码：rr gggggg bbbbbbb (实际 libgdx 用 r<<16|g<<8|b)
                pix.setColor(r / 255f, g / 255f, b / 255f, 1f);
                pix.drawPixel(x, SV_SIZE - 1 - y); // 上下翻转，让顶部=V 最大
            }
        }
        Texture tex = new Texture(pix);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pix.dispose();
        return tex;
    }

    /** 生成一个色相渐变 Texture（垂直，顶=红，过绿过蓝回红）。调用方负责 dispose。 */
    private static Texture makeHueBarTexture() {
        Pixmap pix = new Pixmap(8, HUE_H, Pixmap.Format.RGB888);
        for (int y = 0; y < HUE_H; y++) {
            // 顶部 y=0 → h=360（红）；底部 → h=0（红）；中间绿/蓝
            float hue = 360f * (1f - y / (float) (HUE_H - 1));
            float[] rgb = hsvToRgb(hue, 1f, 1f);
            pix.setColor(rgb[0], rgb[1], rgb[2], 1f);
            pix.fillRectangle(0, y, 8, 1);
        }
        Texture tex = new Texture(pix);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pix.dispose();
        return tex;
    }

    /**
     * 纯色 drawable：自建 Pixmap 染色（不依赖 skin 的 "white" drawable），Texture 登记
     * 到 {@link #solidTextures}，close 时统一释放。
     *
     * <p>不能改用 {@code skin.newDrawable("white", c)}：在浮层里反复 tint 同名 drawable 会命中
     * libgdx 的 TintedDrawable 缓存，导致预设色板色块渲染为透明/错误颜色（看不见）。
     * 故走 {@link BsSkinFactory#solidTexture} 自建 Texture，再登记生命周期。</p>
     */
    private Drawable makeSolidDrawable(Color c) {
        Texture tex = BsSkinFactory.solidTexture(c);
        solidTextures.add(tex);
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    /**
     * 预设色 Drawable：按 RGB int 查静态缓存 {@link #PRESET_CACHE}，命中则复用，未命中则建并登记。
     * <p>预设色是常量、跨实例共享，故走缓存而非 {@link #makeSolidDrawable}（后者每次新建并随 close 释放）。
     * 应用退出时由 {@link #disposePresetCache()} 统一释放。</p>
     */
    private static Drawable presetDrawable(int hex, Color c) {
        TextureRegionDrawable d = PRESET_CACHE.get(hex);
        if (d == null) {
            d = new TextureRegionDrawable(new TextureRegion(BsSkinFactory.solidTexture(c)));
            PRESET_CACHE.put(hex, d);
        }
        return d;
    }

    public void close() {
        if (!open) return;
        if (backdrop != null) { backdrop.remove(); backdrop = null; }
        if (root != null) { root.remove(); root = null; }
        if (svTexture != null) { svTexture.dispose(); svTexture = null; }
        if (hueTexture != null) { hueTexture.dispose(); hueTexture = null; }
        // 释放本实例 makeSolidDrawable 产物（preview/cursor/knob，3 个 2×2 小 Texture）。
        // 预设色板走静态缓存 PRESET_CACHE，不在此释放。
        for (Texture t : solidTextures) {
            t.dispose();
        }
        solidTextures.clear();
        open = false;
        if (onClose != null) {
            try { onClose.run(); } catch (Throwable t) { log.warn("onClose error", t); }
            onClose = null;
        }
    }

    /** 释放预设色板静态缓存（应用退出时调用）。参见 {@link BsModal#disposePathCache()}。 */
    public static void disposePresetCache() {
        for (TextureRegionDrawable d : PRESET_CACHE.values()) {
            Texture t = d.getRegion().getTexture();
            if (t != null) t.dispose();
        }
        PRESET_CACHE.clear();
    }

    public boolean isOpen() { return open; }
}
