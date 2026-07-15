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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bootstrap 风格底部状态栏（Status Bar）—— IDE / 编辑器类应用标配。
 *
 * <p>结构：左侧状态文字（多段，用分隔符隔开） | 右侧指示器（缩放 / 坐标 / 语言切换等）。</p>
 *
 * <pre>
 * ┌──────────────────────────────────────────────────┐
 * │ ● Ready │ dialogue3.dsl │ 已保存   缩放:100% | x:120,y:80 | 中文 ▾ │
 * └──────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsStatusBar bar = new BsStatusBar(skin);
 * bar.setLeftText("Ready");            // 左侧主文字（用颜色点指示状态）
 * bar.setLeftDot(BsStatusBar.DotColor.SUCCESS);
 * bar.setRight("zoom", "100%");
 * bar.setRight("coords", "x:120,y:80");
 * bar.setRight("lang", "中文");
 * bar.setOnRightClick("lang", () -> cycleLanguage());
 * stage.addActor(bar);
 *
 * // 状态变更时更新
 * bar.setLeftText("正在编译...");
 * bar.setLeftDot(BsStatusBar.DotColor.WARNING);
 * }</pre>
 *
 * <p>实现：横向 Table = [dot + 左侧 segments] + fill 占位 + [右侧 segments]。
 * 每个 segment 有 label + 可选 click 回调。底色淡灰，高度固定。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsStatusBar extends Table {

    public enum DotColor { SUCCESS, WARNING, DANGER, INFO, IDLE }

    private final Table leftZone;
    private final Table rightZone;
    private final DotActor leftDot;
    private final Label leftLabel;
    private final Map<String, Label> rightSegments = new LinkedHashMap<>();
    private final Map<String, Runnable> rightClicks = new LinkedHashMap<>();

    public BsStatusBar(Skin skin) {
        setBackground(skin.getDrawable("bs-menu-bar-bg"));
        pad(4, 10, 4, 10);
        left().center();
        defaults().pad(0);

        // dot 用自绘 actor（避免字体不含 ● 导致颜色失效）
        leftDot = new DotActor(idleDotColor(), 5);

        leftLabel = new Label("Ready", skin);
        leftLabel.setColor(new Color(0.3f, 0.3f, 0.32f, 1f));
        leftLabel.setFontScale(0.9f);

        leftZone = new Table();
        leftZone.left();
        leftZone.defaults().pad(0);
        leftZone.add(leftDot).size(10, 10).padRight(6);
        leftZone.add(leftLabel);

        rightZone = new Table();
        rightZone.right();
        rightZone.defaults().pad(0);

        add(leftZone).left();
        add().growX();   // 中间填充把右侧推到最右
        add(rightZone).right();
        setHeight(28);
    }

    // ========================= 左侧 =========================

    public BsStatusBar setLeftText(String text) {
        leftLabel.setText(text == null ? "" : text);
        return this;
    }

    public BsStatusBar setLeftDot(DotColor c) {
        leftDot.setColor(toGdxColor(c));
        return this;
    }

    /** 自绘实心圆点 Actor（避免依赖字体的 ● 字符）。 */
    private static class DotActor extends com.badlogic.gdx.scenes.scene2d.Actor {
        private Color color;
        private final float radius;
        private static ShapeRenderer sr;

        DotActor(Color color, float radius) {
            this.color = color;
            this.radius = radius;
            setSize(radius * 2, radius * 2);
        }

        @Override
        public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
            if (sr == null) sr = new ShapeRenderer();
            batch.end();
            try {
                sr.setProjectionMatrix(batch.getProjectionMatrix());
                sr.setTransformMatrix(batch.getTransformMatrix());
                sr.begin(ShapeRenderer.ShapeType.Filled);
                try {
                    sr.translate(getX(), getY(), 0);
                    sr.setColor(color.r, color.g, color.b, color.a * parentAlpha);
                    sr.circle(getWidth() / 2f, getHeight() / 2f, radius);
                } finally {
                    sr.identity();
                    sr.end();
                }
            } finally {
                batch.begin();
            }
        }

        public void setColor(Color c) { this.color = c; }
    }

    /** 在左侧追加一段（分隔符自动加在段间）。 */
    public BsStatusBar addLeftSegment(String text) {
        Skin skin = BsUI.getSkin();
        if (leftZone.getChildren().size > 2) {
            Label sep = new Label("│", skin);
            sep.setColor(new Color(0xC0 / 255f, 0xC8 / 255f, 0xD0 / 255f, 1f));
            sep.setFontScale(0.85f);
            leftZone.add(sep).padLeft(8).padRight(8);
        }
        Label l = new Label(text, skin);
        l.setColor(new Color(0x49 / 255f, 0x50 / 255f, 0x57 / 255f, 1f));
        l.setFontScale(0.9f);
        leftZone.add(l);
        return this;
    }

    // ========================= 右侧 =========================

    /** 设置/新增右侧一段（按 key 索引，已存在则更新文本）。 */
    public BsStatusBar setRight(String key, String text) {
        Skin skin = BsUI.getSkin();
        Label existing = rightSegments.get(key);
        if (existing != null) {
            existing.setText(text);
            return this;
        }
        // 新增段
        if (rightZone.getChildren().size > 0) {
            Label sep = new Label("│", skin);
            sep.setColor(new Color(0xC0 / 255f, 0xC8 / 255f, 0xD0 / 255f, 1f));
            sep.setFontScale(0.85f);
            rightZone.add(sep).padLeft(8).padRight(8);
        }
        Label l = new Label(text, skin);
        l.setColor(new Color(0x49 / 255f, 0x50 / 255f, 0x57 / 255f, 1f));
        l.setFontScale(0.9f);
        Container<Label> wrap = new Container<>(l);
        wrap.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        wrap.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                Runnable r = rightClicks.get(key);
                if (r != null) {
                    try { r.run(); } catch (Throwable t) { log.warn("status bar click", t); }
                }
                return true;
            }
            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                l.setColor(BsPalette.PRIMARY.getMain());
            }
            @Override
            public void exit(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                l.setColor(BsTheme.ts());
            }
        });
        rightZone.add(wrap);
        rightSegments.put(key, l);
        return this;
    }

    /** 设置某段点击回调（key 必须已存在）。 */
    public BsStatusBar setOnRightClick(String key, Runnable cb) {
        rightClicks.put(key, cb);
        return this;
    }

    public String getRightText(String key) {
        Label l = rightSegments.get(key);
        return l == null ? null : l.getText().toString();
    }

    public BsStatusBar removeRight(String key) {
        Label l = rightSegments.remove(key);
        rightClicks.remove(key);
        if (l != null) {
            // 简化：重建 rightZone
            java.util.List<String> keys = new java.util.ArrayList<>(rightSegments.keySet());
            java.util.Map<String, String> texts = new LinkedHashMap<>();
            for (String k : keys) texts.put(k, rightSegments.get(k).getText().toString());
            rightZone.clearChildren();
            rightSegments.clear();
            for (Map.Entry<String, String> e : texts.entrySet()) setRight(e.getKey(), e.getValue());
        }
        return this;
    }

    // ========================= 颜色 =========================

    private Color idleDotColor() {
        return BsTheme.tm();
    }

    private Color toGdxColor(DotColor c) {
        switch (c) {
            case SUCCESS: return BsPalette.SUCCESS.getMain();
            case WARNING: return BsPalette.WARNING.getMain();
            case DANGER:  return BsPalette.DANGER.getMain();
            case INFO:    return BsPalette.PRIMARY.getMain();
            case IDLE:    return idleDotColor();
        }
        return idleDotColor();
    }
}
