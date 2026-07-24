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
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/// 评论 / 聊天气泡：头像 + 昵称 + 时间 + 正文，纯文本版（富文本版后续）。
///
/// 头像支持三种来源（按 avatar() 调用决定）：
/// - **真实图片**：`avatar(drawable)`，自动裁成圆形（`makeRoundDrawable`）。
/// - **首字母占位**：`avatar(null)` 且已设 `name`，生成"主题色圆形 + 首字母"头像。
/// - **默认占位**：构造时自带 secondary 色圆背景（无文字），保证不调 avatar 也不空白。
///
/// 两种用法：
/// - **聊天**：`self(true/false)` 控制左右侧 + 气泡底色（自己=primary-soft，对方=surface）。
/// - **评论流**：`bubble(false)` 无气泡背景，头像 + 文字平铺。
///
/// 用法：
/// ```java
/// // 对方消息（左），首字母圆形头像（从 name 取首字）
/// new BsComment(skin).avatar(null).name("张三").time("12:30").text("你好！");
/// // 传真实头像 drawable
/// new BsComment(skin).avatar(avatarD).name("张三").time("12:30").text("你好！");
/// // 自己消息（右，primary 气泡）
/// new BsComment(skin).self(true).avatar(meD).name("我").text("收到 👍").maxWidth(260);
/// // 评论（无气泡）
/// new BsComment(skin).avatar(d).name("李四").text("这条评论很有用").bubble(false);
/// ```
///
/// 实现：`Table`，两列 `[头像][气泡]`；self 时反转列序并右对齐。
/// 头像是 `Stack`：底圆背景（Container）+ 文字层（Label），便于切换图片/首字母。
/// 正文 Label wrap + maxWidth 控制换行宽度。
/// @author authorZhao
/// @since 2026-07-16
public class BsComment extends Table {

    private final Skin skin;
    private final Label nameLabel;
    private final Label timeLabel;
    private final Label bodyLabel;
    private final Table bubble;
    private final Container<Image> avatarWrap;
    private final com.badlogic.gdx.scenes.scene2d.ui.Stack avatarStack;
    private final Label avatarLabel;   // 首字母层（头像无图片时显示）
    private float avatarSize = 40;
    /** 当前头像是否处于占位模式（无真实图片，显示首字母/纯色圆）。 */
    private boolean avatarPlaceholder = true;
    private float maxWidth = 0f;

    public BsComment() {
        this(BsUI.getSkin());
    }

    public BsComment(Skin skin) {
        this.skin = skin;
        Color tp = skin.get("bs-text-primary", Color.class);
        Color tm = skin.get("bs-text-muted", Color.class);

        nameLabel = new Label("", skin);
        nameLabel.setColor(tp);

        Label.LabelStyle timeStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        timeStyle.font = skin.getFont("font-sm");
        timeLabel = new Label("", timeStyle);
        timeLabel.setColor(tm);

        bodyLabel = new Label("", skin);
        bodyLabel.setColor(tp);
        bodyLabel.setWrap(true);

        Table header = new Table();
        header.add(nameLabel).left().padRight(8);
        header.add(timeLabel).left();

        bubble = new Table();
        bubble.pad(8);
        bubble.defaults().left();
        bubble.add(header).left().row();
        bubble.add(bodyLabel).left().growX();

        Image avatarImg = new Image();
        avatarImg.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        avatarWrap = new Container<>(avatarImg);
        avatarWrap.fill();
        avatarWrap.size(avatarSize);

        // 首字母层：头像无图片时叠在圆背景上（白色首字，居中）
        Label.LabelStyle avatarStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        avatarStyle.font = skin.getFont("font-lg");
        avatarLabel = new Label("", avatarStyle);
        avatarLabel.setColor(Color.WHITE);
        avatarLabel.setAlignment(com.badlogic.gdx.utils.Align.center);

        // Stack 叠层：[圆背景 Image][首字母 Label]
        avatarStack = new com.badlogic.gdx.scenes.scene2d.ui.Stack();
        avatarStack.add(avatarWrap);
        avatarStack.add(avatarLabel);
        avatarStack.setSize(avatarSize, avatarSize);

        // 默认 other 侧（左）
        defaults().pad(4);
        left().top();
        add(avatarStack).size(avatarSize, avatarSize).top().padRight(8);
        add(bubble).top().left();
        bubble.setBackground(skin.getDrawable("bs-window-bg"));

        // 默认占位：secondary 色圆形 + 无文字（调用方 avatar(null) 且有 name 时才显示首字母）
        applyDefaultAvatar();
    }

    // =================== API ===================

    /**
     * 设置头像。传 null 或不调时走首字母占位（从 name 取首字 + 主题色圆形背景）；
     * 传 Drawable 时自动裁成圆形（`makeRoundDrawable`）。
     */
    public BsComment avatar(Drawable d) {
        Image img = avatarWrap.getActor();
        if (d == null) {
            avatarPlaceholder = true;
            applyDefaultAvatar();   // 首字母或纯色圆背景
        } else {
            avatarPlaceholder = false;
            img.setDrawable(BsSkinFactory.makeRoundDrawable(d, (int) avatarSize));
            avatarLabel.setText("");   // 有图片时不显示首字母
        }
        return this;
    }

    public BsComment avatarSize(float s) {
        this.avatarSize = s;
        avatarWrap.size(s);
        avatarStack.setSize(s, s);
        // 同步所有引用 avatarStack 的 cell（self() 会改变 cell 顺序，遍历更健壮）
        for (Cell<?> cell : getCells()) {
            if (cell.getActor() == avatarStack) {
                cell.size(s, s);
            }
        }
        // 占位头像尺寸变了要重建（makeRoundDrawable 按 size 裁圆）
        if (avatarPlaceholder) {
            applyDefaultAvatar();
        }
        return this;
    }

    public BsComment name(String s) {
        nameLabel.setText(s == null ? "" : s);
        // 占位模式下 name 变化要同步首字母 + 背景色（按 name 哈希）
        if (avatarPlaceholder) {
            applyDefaultAvatar();
        }
        return this;
    }

    public BsComment time(String s) {
        timeLabel.setText(s == null ? "" : s);
        return this;
    }

    public BsComment text(String s) {
        bodyLabel.setText(s == null ? "" : s);
        return this;
    }

    /// 正文最大宽度（超过则换行，控制气泡不会撑满整个 stage）。
    public BsComment maxWidth(float w) {
        this.maxWidth = w;
        bubble.getCell(bodyLabel).maxWidth(w);
        return this;
    }

    /// 是否显示气泡背景（评论流可设 false）。
    public BsComment bubble(boolean b) {
        bubble.setBackground(b ? skin.getDrawable("bs-window-bg") : null);
        return this;
    }

    // =================== 头像占位工具 ===================

    /**
     * 应用默认占位头像：若已设 name 则显示首字母，否则纯色圆背景。
     * <p>背景色按 name 哈希在 6 色 Variant 里取一个，保证同一用户颜色稳定、不同用户颜色不同
     *（微信/钉钉/GitHub identicon 同款思路）。</p>
     * <p>实现：用白色圆 drawable（`circleDrawable`）+ Image.setColor(bg) 染色
     *（白色 × bg = bg，顶点色染色）。不走 makeRoundDrawable(newDrawable("white",bg))——
     * 后者走 drawPixmap 读纹理像素，tint 颜色在顶点色不在像素里，会画出白色圆。</p>
     */
    private void applyDefaultAvatar() {
        Image img = avatarWrap.getActor();
        Color bg = avatarBgColorFor(nameLabel.getText().toString());
        img.setDrawable(BsSkinFactory.circleDrawable((int) avatarSize));
        img.setColor(bg);
        refreshAvatarPlaceholder();
    }

    /** name 变化后刷新首字母（不重建背景，避免每次 name() 都重建 Texture）。 */
    private void refreshAvatarPlaceholder() {
        String n = nameLabel.getText().toString();
        if (n.isEmpty()) {
            avatarLabel.setText("");
            avatarLabel.setColor(new Color(0, 0, 0, 0));   // 隐藏文字
            return;
        }
        avatarLabel.setText(n.substring(0, 1));
        avatarLabel.setColor(Color.WHITE);
    }

    /** 按 name 哈希取头像背景色（6 色 Variant 循环）。 */
    private static Color avatarBgColorFor(String name) {
        BsPalette[] palette = {
                BsPalette.PRIMARY, BsPalette.SUCCESS, BsPalette.DANGER,
                BsPalette.WARNING, BsPalette.INFO, BsPalette.SECONDARY
        };
        if (name == null || name.isEmpty()) return BsPalette.SECONDARY.getMain();
        int idx = Math.abs(name.hashCode()) % palette.length;
        return palette[idx].getMain();
    }

    /// `self=true`：头像在右、气泡右对齐、primary 底 + on-primary 白字（三主题对比稳定）；
    /// `false`：左侧 surface 底色。
    public BsComment self(boolean isSelf) {
        clearChildren();
        defaults().pad(4);
        Color onPrimary = skin.get("bs-text-on-primary", Color.class);
        if (isSelf) {
            top().right();
            bubble.setBackground(skin.getDrawable("bs-primary-up"));
            // self 气泡用主色底，文字必须切到 on-primary（白），否则深底浅字看不清
            nameLabel.setColor(onPrimary);
            timeLabel.setColor(onPrimary);
            bodyLabel.setColor(onPrimary);
            add(bubble).top().right().padRight(8);
            add(avatarStack).size(avatarSize, avatarSize).top();
        } else {
            top().left();
            bubble.setBackground(skin.getDrawable("bs-window-bg"));
            // 还原 other 侧的字色（可能之前被 self(true) 改过）
            nameLabel.setColor(skin.get("bs-text-primary", Color.class));
            timeLabel.setColor(skin.get("bs-text-muted", Color.class));
            bodyLabel.setColor(skin.get("bs-text-primary", Color.class));
            add(avatarStack).size(avatarSize, avatarSize).top().padRight(8);
            add(bubble).top().left();
        }
        if (maxWidth > 0) bubble.getCell(bodyLabel).maxWidth(maxWidth);
        return this;
    }
}
