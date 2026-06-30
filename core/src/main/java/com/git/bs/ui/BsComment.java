package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/// 评论 / 聊天气泡：头像 + 昵称 + 时间 + 正文，纯文本版（富文本版后续）。
///
/// 两种用法：
/// - **聊天**：`self(true/false)` 控制左右侧 + 气泡底色（自己=primary-soft，对方=surface）。
/// - **评论流**：`bubble(false)` 无气泡背景，头像 + 文字平铺。
///
/// 用法：
/// ```java
/// // 对方消息（左）
/// new BsComment(skin).avatar(avatarD).name("张三").time("12:30").text("你好！");
/// // 自己消息（右，primary-soft 气泡）
/// new BsComment(skin).self(true).avatar(meD).name("我").text("收到 👍").maxWidth(260);
/// // 评论（无气泡）
/// new BsComment(skin).avatar(d).name("李四").text("这条评论很有用").bubble(false);
/// ```
///
/// 实现：`Table`，两列 `[头像][气泡]`；self 时反转列序并右对齐。
/// 正文 Label wrap + maxWidth 控制换行宽度。
public class BsComment extends Table {

    private final Skin skin;
    private final Label nameLabel;
    private final Label timeLabel;
    private final Label bodyLabel;
    private final Table bubble;
    private final Container<Image> avatarWrap;
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

        timeLabel = new Label("", skin);
        timeLabel.setColor(tm);
        timeLabel.setFontScale(0.85f);

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
        avatarWrap = new Container<>(avatarImg);
        avatarWrap.size(40);

        // 默认 other 侧（左）
        defaults().pad(4);
        left().top();
        add(avatarWrap).top().padRight(8);
        add(bubble).top().left();
        bubble.setBackground(skin.getDrawable("bs-window-bg"));
    }

    // =================== API ===================

    public BsComment avatar(Drawable d) {
        Image img = avatarWrap.getActor();
        if (d == null) {
            img.setDrawable(skin.newDrawable("white", BsPalette.SECONDARY.getMain()));
        } else {
            img.setDrawable(d);
        }
        return this;
    }

    public BsComment avatarSize(float s) {
        avatarWrap.size(s);
        return this;
    }

    public BsComment name(String s) {
        nameLabel.setText(s == null ? "" : s);
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

    /// `self=true`：头像在右、气泡右对齐、primary-soft 底色；`false`：左侧 surface 底色。
    public BsComment self(boolean isSelf) {
        clearChildren();
        defaults().pad(4);
        if (isSelf) {
            top().right();
            bubble.setBackground(skin.getDrawable("bs-primary-soft-bg"));
            add(bubble).top().right().padRight(8);
            add(avatarWrap).top();
        } else {
            top().left();
            bubble.setBackground(skin.getDrawable("bs-window-bg"));
            add(avatarWrap).top().padRight(8);
            add(bubble).top().left();
        }
        if (maxWidth > 0) bubble.getCell(bodyLabel).maxWidth(maxWidth);
        return this;
    }
}
