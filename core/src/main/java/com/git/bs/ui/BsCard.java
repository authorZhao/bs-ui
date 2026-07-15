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
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 风格卡片：图片（顶部或左侧）+ 标题 + 副标题 + 正文 + 页脚按钮。
 *
 * <p>两种布局：</p>
 * <ul>
 *   <li>{@link Orientation#VERTICAL} —— 图片在顶部，文字在下（Bootstrap card 默认）</li>
 *   <li>{@link Orientation#HORIZONTAL} —— 图片在左，文字在右（media object 风格）</li>
 * </ul>
 *
 * <p>用法（builder 风格）：</p>
 * <pre>{@code
 * BsCard card = new BsCard(skin)
 *         .orientation(BsCard.Orientation.VERTICAL)
 *         .image(drawableFromPath("bs/test/img/xxx.png"))
 *         .title("卡片标题")
 *         .subtitle("副标题文字")
 *         .body("这里是正文内容，支持多行长文本自动换行。")
 *         .footerButton("了解更多", () -> setStatus("点击了卡片按钮"));
 * stage.addActor(card);
 * card.pack();
 * }</pre>
 *
 * <p>实现：extends {@link Table}，用 bs-window-bg（圆角白底）作背景；image 用 {@link Image}；
 * 文字用独立 LabelStyle（避免 shared style fontColor 干扰）；footer 是 Table 容器。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsCard extends Table {

    public enum Orientation { VERTICAL, HORIZONTAL }

    @Getter private Orientation orientation = Orientation.VERTICAL;

    // 内容区容器（业务方拿引用可继续 add 自定义 actor）
    @Getter private final Table bodyTable;
    @Getter private final Table footerTable;
    private Image image;
    private final Container<Image> imageWrap;
    /** 图片尺寸：宽/高。<=0 表示"自适应"（垂直=宽撑满、按 ratio 算高度；水平=高撑满、按 ratio 算宽度）。 */
    private float imageW = 0, imageH = 0;
    /** 图片宽高比（h/w），垂直布局且 imageH<=0 时按 imageW*ratio 算高度。默认 0.55。 */
    private float imageRatio = 0.55f;

    public BsCard(Skin skin) {
        setBackground(skin.getDrawable("bs-window-bg"));
        pad(0);
        setTouchable(null);  // 默认不挡事件（点击穿透）

        imageWrap = new Container<>();
        imageWrap.fill(true);

        bodyTable = new Table();
        bodyTable.pad(10).left().top();
        bodyTable.defaults().growX().left();

        footerTable = new Table();
        footerTable.pad(8, 10, 10, 10);
        footerTable.right();
        footerTable.defaults().pad(4);

        rebuild();
    }

    // ========================= builder API =========================

    public BsCard orientation(Orientation o) {
        this.orientation = o;
        rebuild();
        return this;
    }

    /** 设置顶部/左侧图片。null 则不显示图片。 */
    public BsCard image(Drawable img) {
        if (img != null) {
            image = new Image(img);
            image.setScaling(com.badlogic.gdx.utils.Scaling.fill);
            imageWrap.setActor(image);
            imageWrap.setVisible(true);
        } else {
            imageWrap.setVisible(false);
        }
        rebuild();
        return this;
    }

    /**
     * 设置图片尺寸。
     * <ul>
     *   <li>垂直布局：w<=0 表示宽度撑满；h<=0 表示按 {@link #imageRatio} 自适应</li>
     *   <li>水平布局：建议 w/h 都给具体值（如 100×80）</li>
     * </ul>
     */
    public BsCard imageSize(float w, float h) {
        this.imageW = w;
        this.imageH = h;
        rebuild();
        return this;
    }

    /**
     * 设置图片宽高比（h/w）。垂直布局且未指定高度时按这个比例算高度。
     * 默认 0.55（类似 Bootstrap card-img-top 16:9 视觉，更精致不占太多空间）。
     */
    public BsCard imageRatio(float ratio) {
        this.imageRatio = ratio;
        rebuild();
        return this;
    }

    /** 标题（粗体感深色）。 */
    public BsCard title(String t) {
        addOrReplace("title", makeLabel(t, BsTheme.tp(), 1.15f), true);
        return this;
    }

    /** 副标题（灰色，小号）。 */
    public BsCard subtitle(String s) {
        addOrReplace("subtitle", makeLabel(s, BsTheme.tm(), 0.95f), true);
        return this;
    }

    /** 正文（自动换行）。 */
    public BsCard body(String b) {
        Label body = makeLabel(b, BsTheme.ts(), 1f);
        body.setWrap(true);
        addOrReplace("body", body, true);
        return this;
    }

    /** 在正文区追加任意 actor（如状态标签、图标行）。 */
    public BsCard addCustom(Actor a) {
        bodyTable.add(a).padTop(6).row();
        return this;
    }

    /** 页脚加按钮（右对齐）。 */
    public BsCard footerButton(String text, Runnable onClick) {
        return footerButton(text, onClick, BsButton.Variant.PRIMARY, BsButton.Style.SOLID);
    }

    public BsCard footerButton(String text, Runnable onClick, BsButton.Variant v, BsButton.Style s) {
        BsButton btn = new BsButton(text, BsUI.getSkin(), v, s, BsButton.Size.SM);
        btn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                try { if (onClick != null) onClick.run(); } catch (Throwable t) { log.warn("footer onClick", t); }
            }
        });
        footerTable.add(btn);
        rebuild();
        return this;
    }

    /** 页脚加链接风格按钮（透明背景）。 */
    public BsCard footerLink(String text, Runnable onClick) {
        BsLink link = new BsLink(text, BsUI.getSkin());
        link.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                try { if (onClick != null) onClick.run(); } catch (Throwable t) { log.warn("footerLink onClick", t); }
            }
        });
        footerTable.add(link).pad(4);
        rebuild();
        return this;
    }

    // ========================= 内部工具 =========================

    private Label makeLabel(String text, Color color, float scale) {
        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = BsUI.getSkin().getFont("default");
        ls.fontColor = color;
        Label l = new Label(text == null ? "" : text, ls);
        l.setColor(Color.WHITE);
        l.setFontScale(scale);
        return l;
    }

    /** 在 bodyTable 中按 name 占位替换：同名的 actor 先 remove 再 add。 */
    private void addOrReplace(String name, Actor actor, boolean leftAlign) {
        actor.setUserObject(name);  // 用 userObject 标记
        // 先移除同名旧 actor
        for (Actor a : new com.badlogic.gdx.utils.Array<>(bodyTable.getChildren())) {
            if (name.equals(a.getUserObject())) bodyTable.removeActor(a);
        }
        bodyTable.add(actor).padTop(4).growX().left().row();
    }

    /** 根据 orientation 重建整个布局。 */
    private void rebuild() {
        clearChildren();
        if (orientation == Orientation.VERTICAL) {
            if (imageWrap.isVisible()) {
                // 垂直布局：宽度撑满；高度 = imageH（>0）否则按 ratio × 估算宽度
                float estWidth = estimateCardWidth();
                float h = imageH > 0 ? imageH : estWidth * imageRatio;
                add(imageWrap).growX().height(h).row();
            }
            add(bodyTable).growX().top().row();
            if (footerTable.hasChildren()) {
                add(footerTable).growX().row();
            }
        } else {
            // HORIZONTAL：图片左（固定 w×h）+ 内容右
            if (imageWrap.isVisible()) {
                float w = imageW > 0 ? imageW : 100;
                float h = imageH > 0 ? imageH : 80;
                add(imageWrap).width(w).height(h).top();
            }
            Table right = new Table();
            right.pad(10).left().top();
            for (Actor a : new com.badlogic.gdx.utils.Array<>(bodyTable.getChildren())) {
                a.remove();
                right.add(a).growX().left().padTop(4).row();
            }
            if (footerTable.hasChildren()) {
                for (Actor a : new com.badlogic.gdx.utils.Array<>(footerTable.getChildren())) {
                    a.remove();
                    right.add(a).padTop(8).right().row();
                }
            }
            add(right).growX().top();
        }
    }

    /** 估算卡片宽度（用于垂直布局图片高度按 ratio 自适应）。
     *  如果父容器设过 width，用父容器；否则用 stage 宽度的一部分；再不行用默认 320。 */
    private float estimateCardWidth() {
        if (getWidth() > 0) return getWidth();
        if (getStage() != null) return Math.min(360, getStage().getWidth() * 0.4f);
        return 320;
    }
}
