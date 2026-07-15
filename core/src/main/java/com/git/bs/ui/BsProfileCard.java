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
import lombok.extern.slf4j.Slf4j;

/**
 * 圆形头像个人信息卡片：居中布局，头像在顶部（强制圆形），下方姓名 + 角色 + 简介 + 统计。
 * 常用于用户主页头图、好友卡片、团队成员展示。
 *
 * <p>不同于 {@link BsProfilePanel}（横向信息 + 方形头像 + 操作按钮在右），
 * 本组件是<b>居中卡片</b>风格，类似 Bootstrap "card" 配圆形头像（avatar-circle）。</p>
 *
 * <p>布局：</p>
 * <pre>
 * ┌─────────────────────────┐
 * │                         │
 * │       ⭕(圆形头像)      │
 * │                         │
 * │       张三              │  ← 姓名（粗大字）
 * │       @zhangsan         │  ← handle（灰色小字）
 * │       [管理员]          │  ← 角色 Badge
 * │                         │
 * │   一段简介文字，居中    │
 * │                         │
 * │  帖子 │ 关注 │ 粉丝    │  ← 横向统计行
 * └─────────────────────────┘
 * </pre>
 *
 * <p>用法（builder 风格）：</p>
 * <pre>{@code
 * BsProfileCard card = new BsProfileCard(skin)
 *         .avatar(avatarDrawable)
 *         .avatarSize(96)
 *         .name("张三")
 *         .handle("@zhangsan")
 *         .role("管理员")
 *         .bio("专注于 libgdx 开发，热爱自制 UI。")
 *         .stat("帖子", "128")
 *         .stat("关注", "1.2k")
 *         .stat("粉丝", "5.6k");
 * stage.addActor(card);
 * card.pack();
 * }</pre>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsProfileCard extends Table {

    private final Container<Image> avatarWrap;
    private final Label nameLabel;
    private final Label handleLabel;
    private final Container<Actor> roleWrap;
    private final Label bioLabel;
    private final Table statsRow;

    /** 头像尺寸（用于圆形剪裁）。 */
    private float avatarSize = 80;

    public BsProfileCard(Skin skin) {
        setBackground(skin.getDrawable("bs-window-bg"));
        pad(20);

        // 居中布局
        center();
        defaults().center().padBottom(6);

        // 头像（圆形，居中）
        avatarWrap = new Container<>();
        avatarWrap.size(avatarSize, avatarSize);
        // 默认灰色圆形占位
        setAvatarPlaceholder(BsTheme.bds());
        add(avatarWrap).size(avatarSize, avatarSize).padBottom(10).row();

        // 姓名（深色大字）
        nameLabel = makeLabel("", BsTheme.tp(), 1.3f);
        add(nameLabel).padBottom(2).row();

        // handle（灰色小字）
        handleLabel = makeLabel("", BsTheme.tm(), 0.95f);
        add(handleLabel).padBottom(6).row();

        // 角色 Badge（居中）
        roleWrap = new Container<>();
        roleWrap.fill(false);
        add(roleWrap).padBottom(8).row();

        // 简介（居中换行）
        bioLabel = makeLabel("", BsTheme.ts(), 1f);
        bioLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        bioLabel.setWrap(true);
        add(bioLabel).growX().padBottom(10).row();

        // 统计行（横向）
        statsRow = new Table();
        statsRow.defaults().pad(0, 12, 0, 12).center();
        add(statsRow).padBottom(4).row();
    }

    // ========================= builder API =========================

    /** 设置头像（强制圆形剪裁）。 */
    public BsProfileCard avatar(Drawable d) {
        if (d != null) {
            Drawable round = BsSkinFactory.makeRoundDrawable(d, (int) avatarSize);
            Image img = new Image(round);
            img.setScaling(com.badlogic.gdx.utils.Scaling.fill);
            avatarWrap.setActor(img);
        } else {
            setAvatarPlaceholder(BsTheme.bds());
        }
        return this;
    }

    /** 设置头像尺寸（自动重新剪裁圆形）。 */
    public BsProfileCard avatarSize(float size) {
        this.avatarSize = size;
        // 更新 cell 尺寸（头像 Image 会被 Table 拉伸到这个大小）
        getCell(avatarWrap).size(size, size);
        avatarWrap.size(size, size);
        return this;
    }

    public BsProfileCard name(String n) { nameLabel.setText(n); return this; }
    public BsProfileCard handle(String h) { handleLabel.setText(h); return this; }

    /** 设置角色（用 Badge 显示，主色蓝）。 */
    public BsProfileCard role(String r) {
        if (r == null || r.isEmpty()) {
            roleWrap.setActor(null);
        } else {
            roleWrap.setActor(new BsBadge(r, BsUI.getSkin(), BsBadge.Variant.PRIMARY));
        }
        return this;
    }

    public BsProfileCard bio(String b) { bioLabel.setText(b); return this; }

    /** 加一条统计（标签 + 数值），追加到统计行。 */
    public BsProfileCard stat(String label, String value) {
        Table stat = new Table();
        stat.defaults().center().pad(0);
        Label num = makeLabel(value, BsTheme.tp(), 1.2f);
        num.setAlignment(com.badlogic.gdx.utils.Align.center);
        Label name = makeLabel(label, BsTheme.tm(), 0.85f);
        name.setAlignment(com.badlogic.gdx.utils.Align.center);
        stat.add(num).row();
        stat.add(name).padTop(2);
        // 加分隔（如果不是第一个）
        if (statsRow.getCells().size > 0) {
            Label sep = makeLabel("│", BsTheme.bd(), 1f);
            statsRow.add(sep).pad(0, 12, 0, 12);
        }
        statsRow.add(stat);
        return this;
    }

    // ========================= 工具 =========================

    private Label makeLabel(String text, Color color, float scale) {
        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = BsUI.getSkin().getFont("default");
        ls.fontColor = color;
        Label l = new Label(text == null ? "" : text, ls);
        l.setColor(Color.WHITE);
        l.setFontScale(scale);
        return l;
    }

    private void setAvatarPlaceholder(Color c) {
        Drawable ph = BsSkinFactory.makeRoundDrawable(
                BsUI.getSkin().newDrawable("white", c), (int) avatarSize);
        Image img = new Image(ph);
        img.setScaling(com.badlogic.gdx.utils.Scaling.fill);
        avatarWrap.setActor(img);
    }
}
