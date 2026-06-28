package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Bootstrap 风格空状态（Empty State）—— 列表/搜索无结果时显示。
 *
 * <p>结构：[图标] + 标题 + 副描述 + 可选操作按钮。
 * 常用在 {@link BsTable}、{@link BsList}、{@link BsListGroup} 数据为空时替换内容。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsEmpty empty = new BsEmpty(skin)
 *         .icon(BsIcon.get("inbox"))
 *         .title("暂无数据")
 *         .description("点击下方按钮添加第一条记录")
 *         .actionButton("添加", () -> setStatus("点击了添加"));
 * stage.addActor(empty);
 * }</pre>
 */
public class BsEmpty extends Table {

    public BsEmpty(Skin skin) {
        center();
        defaults().center().padTop(6);
        // 图标占位（用大字 emoji 兜底，无 icon 时显示一个圆角灰底）
        Label iconLabel = new Label("📭", skin);
        iconLabel.setFontScale(2.5f);
        add(iconLabel).padBottom(8).row();

        Label title = new Label("暂无数据", skin);
        title.setColor(BsTheme.tp());
        title.setFontScale(1.1f);
        add(title).row();

        Label desc = new Label("没有符合条件的记录", skin);
        desc.setColor(BsTheme.ts());
        desc.setFontScale(0.95f);
        add(desc).row();
    }

    /** 用图标 drawable 替换默认 emoji。 */
    public BsEmpty icon(Drawable d) {
        if (d == null) return this;
        // 清掉第一个 cell（emoji），改为 Image
        getCells().first().clearActor();
        com.badlogic.gdx.scenes.scene2d.ui.Image img = new com.badlogic.gdx.scenes.scene2d.ui.Image(d);
        img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        getCells().first().setActor(img);
        getCells().first().size(64, 64);
        return this;
    }

    public BsEmpty title(String t) {
        Label l = (Label) getCells().get(1).getActor();
        l.setText(t);
        return this;
    }

    public BsEmpty description(String d) {
        Label l = (Label) getCells().get(2).getActor();
        l.setText(d);
        return this;
    }

    /** 添加一个操作按钮（在描述下方）。 */
    public BsEmpty actionButton(String label, Runnable onClick) {
        BsButton btn = new BsButton(label, BsUI.getSkin(), BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        btn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                if (onClick != null) {
                    try { onClick.run(); } catch (Throwable ignored) {}
                }
                return true;
            }
        });
        add(btn).padTop(14).row();
        return this;
    }
}
