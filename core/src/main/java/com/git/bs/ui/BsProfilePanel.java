package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import lombok.extern.slf4j.Slf4j;

/**
 * 个人信息面板：头像 + 姓名 + 角色 + 描述 + 操作按钮。常用于用户主页、侧边栏顶部。
 *
 * <p>布局：</p>
 * <pre>
 * ┌────────────────────────────────┐
 * │   ┌─────┐   姓名              │
 * │   │头像 │   @用户ID / 角色     │
 * │   │     │   描述文字          │
 * │   └─────┘   [关注] [私信]     │
 * └────────────────────────────────┘
 * </pre>
 *
 * <p>用法（builder 风格）：</p>
 * <pre>{@code
 * BsProfilePanel panel = new BsProfilePanel(skin)
 *         .avatar(avatarDrawable)
 *         .avatarSize(72, 72)
 *         .name("张三")
 *         .handle("@zhangsan")
 *         .role("管理员")
 *         .bio("一段自我介绍，可空。")
 *         .stat("帖子", "128")
 *         .stat("关注", "1.2k")
 *         .stat("粉丝", "5.6k")
 *         .actionButton("关注", () -> follow(), BsButton.Variant.PRIMARY)
 *         .actionButton("私信", () -> dm(), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
 * stage.addActor(panel);
 * panel.pack();
 * }</pre>
 *
 * <p>实现：extends {@link Table}，圆角白底背景；头像用 {@link Image}（圆形剪裁由 drawable 自身决定，
 * 没传则用色块占位）；姓名/角色/描述用独立 LabelStyle。</p>
 */
@Slf4j
public class BsProfilePanel extends Table {

    private final Table infoTable;       // 右侧信息列
    private final Table statsRow;        // 统计数据行
    private final Table actionsRow;      // 操作按钮行
    private final Container<Image> avatarWrap;
    private final Label nameLabel;
    private final Label handleLabel;
    private final Label roleLabel;
    private final Label bioLabel;
    private final Table headerRow;

    public BsProfilePanel(Skin skin) {
        setBackground(skin.getDrawable("bs-window-bg"));
        pad(14);

        headerRow = new Table();
        headerRow.left().top();
        headerRow.defaults().pad(0).left().top();

        // 头像占位（默认色块）
        avatarWrap = new Container<>();
        avatarWrap.size(64, 64);
        // 默认灰色头像占位
        setAvatarPlaceholder(BsTheme.bds());
        headerRow.add(avatarWrap).size(64, 64).padRight(14).top();

        // 右侧信息列
        infoTable = new Table();
        infoTable.left().top();
        infoTable.defaults().left().padBottom(4);

        nameLabel = makeLabel("", BsTheme.tp(), 1.2f);
        infoTable.add(nameLabel).growX().row();

        handleLabel = makeLabel("", BsTheme.ts(), 0.95f);
        infoTable.add(handleLabel).growX().row();

        roleLabel = makeLabel("", BsPalette.PRIMARY.getMain(), 0.95f);
        infoTable.add(roleLabel).growX().padTop(2).row();

        bioLabel = makeLabel("", BsTheme.ts(), 1f);
        bioLabel.setWrap(true);
        infoTable.add(bioLabel).growX().padTop(6).row();

        // 统计数据行
        statsRow = new Table();
        statsRow.left();
        statsRow.defaults().padRight(16).left();
        infoTable.add(statsRow).growX().padTop(8).row();

        // 操作按钮行
        actionsRow = new Table();
        actionsRow.left();
        actionsRow.defaults().padRight(6).left();
        infoTable.add(actionsRow).growX().padTop(8).row();

        headerRow.add(infoTable).growX().top();
        add(headerRow).growX().row();
    }

    // ========================= builder API =========================

    /** 设置头像（默认不剪裁，保持原始方形/比例）。 */
    public BsProfilePanel avatar(Drawable d) {
        return avatar(d, false);
    }

    /**
     * 设置头像。
     * @param round true 自动剪裁为圆形（用 {@link BsSkinFactory#makeRoundDrawable}）
     */
    public BsProfilePanel avatar(Drawable d, boolean round) {
        if (d != null) {
            float avatarSize = avatarWrap.getHeight() > 0 ? avatarWrap.getHeight() : 72;
            Drawable finalDrawable = round
                    ? BsSkinFactory.makeRoundDrawable(d, (int) Math.max(64, avatarSize))
                    : d;
            Image img = new Image(finalDrawable);
            img.setScaling(com.badlogic.gdx.utils.Scaling.fill);
            avatarWrap.setActor(img);
        } else {
            setAvatarPlaceholder(BsTheme.bds());
        }
        return this;
    }

    public BsProfilePanel avatarSize(float w, float h) {
        avatarWrap.size(w, h);
        headerRow.getCell(avatarWrap).size(w, h);
        return this;
    }

    public BsProfilePanel name(String n) { nameLabel.setText(n); return this; }

    public BsProfilePanel handle(String h) { handleLabel.setText(h); return this; }

    public BsProfilePanel role(String r) { roleLabel.setText(r); return this; }

    public BsProfilePanel bio(String b) { bioLabel.setText(b); return this; }

    /** 加一条统计数据（标签 + 数值），追加到 statsRow。 */
    public BsProfilePanel stat(String label, String value) {
        Table stat = new Table();
        stat.defaults().left().pad(0);
        Label num = makeLabel(value, BsTheme.tp(), 1.1f);
        Label name = makeLabel(label, BsTheme.tm(), 0.85f);
        stat.add(num).row();
        stat.add(name).padTop(0);
        statsRow.add(stat);
        return this;
    }

    /** 加一个操作按钮（追加到 actionsRow）。 */
    public BsProfilePanel actionButton(String text, Runnable onClick,
                                       BsButton.Variant variant, BsButton.Style style) {
        BsButton btn = new BsButton(text, BsUI.getSkin(), variant, style, BsButton.Size.SM);
        btn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                try { if (onClick != null) onClick.run(); } catch (Throwable t) { log.warn("action onClick", t); }
            }
        });
        actionsRow.add(btn);
        return this;
    }

    public BsProfilePanel actionButton(String text, Runnable onClick, BsButton.Variant variant) {
        return actionButton(text, onClick, variant, BsButton.Style.SOLID);
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
        Image img = new Image(BsUI.getSkin().newDrawable("white", c));
        img.setScaling(com.badlogic.gdx.utils.Scaling.fill);
        avatarWrap.setActor(img);
    }
}
