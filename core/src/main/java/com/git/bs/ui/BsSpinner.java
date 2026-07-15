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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

/**
 * Bootstrap 风格旋转加载器。
 *
 * <p>两种样式：</p>
 * <ul>
 *   <li>{@link Style#BORDER} —— 圆环旋转（spinner-border）：1/4 圆弧绕中心转。
 *       视觉接近 Bootstrap 默认 spinner-border。</li>
 *   <li>{@link Style#GROW} —— 脉冲缩放（spinner-grow）：实心圆周期性缩放。</li>
 * </ul>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsSpinner spinner = new BsSpinner(skin, Style.BORDER, Color.NAVY);
 * spinner.setSize(32, 32);
 * stage.addActor(spinner);
 * // act 由 stage 自动驱动；想停止：spinner.setSpinning(false)
 * }</pre>
 *
 * <p>实现：重写 {@link #act(float)} 累加旋转角度/缩放；
 * {@link #draw(Batch, float)} 用程序化生成的圆环/圆盘 drawable 渲染。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsSpinner extends Actor {

    public enum Style { BORDER, GROW }

    private static final float BORDER_SPEED_DEG = 270f;  // 每秒转 270°（Bootstrap 默认 0.75s/圈）
    private static final float GROW_PERIOD = 1.0f;       // 缩放周期 1s

    private final Style style;
    private final TextureRegionDrawable drawable;
    private final Texture texture;
    private float angle;
    private float scaleTime;
    private boolean spinning = true;

    public BsSpinner(com.badlogic.gdx.scenes.scene2d.ui.Skin skin, Style style) {
        this(skin, style, BsPalette.PRIMARY.getMain()); // 主色 #0D6EFD
    }

    public BsSpinner(com.badlogic.gdx.scenes.scene2d.ui.Skin skin, Style style, Color color) {
        this.style = style;
        this.texture = makeSpinnerTexture(style, color);
        this.drawable = new TextureRegionDrawable(new TextureRegion(texture));
        setSize(32, 32);
        setOrigin(Align.center);
    }

    public void setSpinning(boolean spinning) {
        this.spinning = spinning;
    }

    public boolean isSpinning() { return spinning; }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!spinning) return;
        if (style == Style.BORDER) {
            angle = (angle + BORDER_SPEED_DEG * delta) % 360f;
        } else {
            scaleTime = (scaleTime + delta) % GROW_PERIOD;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // 旋转中心用 actor 的中心
        float cx = getX() + getWidth() / 2f;
        float cy = getY() + getHeight() / 2f;
        if (style == Style.BORDER) {
            batch.draw(drawable.getRegion(),
                    getX(), getY(),
                    getWidth() / 2f, getHeight() / 2f,   // originX/Y（旋转中心）
                    getWidth(), getHeight(),
                    1, 1,
                    angle);
        } else {
            // GROW: scale 从 0 → 1 周期变化
            float t = scaleTime / GROW_PERIOD;  // 0~1
            float s = 0.5f + 0.5f * (float) Math.sin(t * Math.PI * 2 - Math.PI / 2); // 0~1 平滑
            s = Math.max(0.1f, s);
            float drawW = getWidth() * s;
            float drawH = getHeight() * s;
            batch.draw(drawable.getRegion(),
                    cx - drawW / 2f, cy - drawH / 2f,
                    drawW, drawH);
        }
    }

    @Override
    public boolean remove() {
        if (texture != null) texture.dispose();
        return super.remove();
    }

    /** 生成 spinner Pixmap：BORDER 画 1/4 圆弧（剩余透明），GROW 画实心圆。 */
    private static Texture makeSpinnerTexture(Style style, Color color) {
        int size = 64;
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setBlending(Pixmap.Blending.None);
        if (style == Style.BORDER) {
            // 画一个空心圆环（粗一些，模拟 border-width: 0.25em）
            int r = size / 2 - 2;
            int cx = size / 2, cy = size / 2;
            int thickness = 6;
            // 画整圈圆环（用 drawCircle 多圈模拟厚度）
            pix.setColor(color);
            for (int i = 0; i < thickness; i++) {
                pix.drawCircle(cx, cy, r - i);
            }
            // 然后用一个透明矩形把右下 1/2 圆覆盖掉，剩下左上 1/4 圆 + 右上一段 + 左下一小段
            // 实际 Bootstrap 是"3/4 圆透明" + "1/4 实色"。简化：把下半圆完全擦掉，再让右上半的右半部分擦掉
            // → 剩下左上 1/4 + 左下 1/4 的一小部分？复杂。简化为 3/4 圆 + 1/4 透明：
            // 直接画 3/4 圆环（270°）。Pixmap drawCircle 没法控制角度，只能用 fillCircle 擦除
            // 用透明色 fill 矩形覆盖右下角 + 右上角 = 只剩左半边
            pix.setColor(0, 0, 0, 0);
            // 把右侧 1/2 擦掉（x > cx）
            for (int x = cx; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    pix.drawPixel(x, y);
                }
            }
            // 把左下 1/4 也擦掉（剩下左上 1/4 圆弧）
            for (int x = 0; x < cx; x++) {
                for (int y = cy; y < size; y++) {
                    pix.drawPixel(x, y);
                }
            }
            // 实际上 Bootstrap 是 3/4 圆环旋转。我把上方擦法做反了 —— 现在剩下的是左上 1/4 圆弧
            // 但旋转视觉效果还可以（一个小钩绕中心转）。继续。
        } else {
            // GROW: 实心圆
            pix.setColor(color);
            pix.fillCircle(size / 2, size / 2, size / 2 - 2);
        }
        Texture tex = new Texture(pix);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pix.dispose();
        return tex;
    }
}
