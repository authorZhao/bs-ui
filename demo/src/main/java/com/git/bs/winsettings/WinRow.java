package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.git.bs.ui.BsEmoji;
import com.git.bs.ui.BsSkinFactory;
import com.git.bs.ui.BsText;
import com.git.bs.ui.BsTheme;

/**
 * Win11 风格的行列表项 —— 设置界面里大量复用的通用行样式。
 *
 * <p>结构：{@code [图标] 标题 / 描述   [右侧控件或箭头]}，整行 hover 微亮（setBackground）。</p>
 *
 * <ul>
 *   <li>{@code icon} 可为 null（详情页设置项通常无图标）</li>
 *   <li>{@code trailing} 右侧控件（BsSwitch/BsSelectBox/BsButton/BsText 值/箭头），可为 null</li>
 *   <li>{@code onClick} 非 null 时整行可点击（推荐项导航用）；为 null 时仅 trailing 控件交互</li>
 * </ul>
 *
 * <p><b>hover 与下拉共存（事件区分）</b>：行内含 {@link BsSelectBox}（libgdx SelectBox）时，
 * 展开下拉后 popup 是 stage 顶层 actor，鼠标移到 popup 上会触发本行的 exit → setBackground(null)
 * → invalidate → 导致 SelectBox 关闭已展开的下拉。<b>解法</b>：exit 时检查 {@code toActor}，
 * 若它是行内某个 SelectBox 的 {@link SelectBox#getList() popup list}，则视为「没真离开」，
 * 保持 hover 背景（不 invalidate），下拉保持打开；鼠标到别处才真 exit 取消 hover。</p>
 *
 * <pre>{@code
 * // 导航行（主页推荐设置）
 * WinRow.nav(skin, "🔊", "声音", "输出/输入设备", () -> router.navigate("system"))
 * // 设置行（带 toggle 控件）
 * new WinRow(skin, null, "夜间模式", "减弱蓝光", new BsSwitch(skin), null)
 * }</pre>
 */
public class WinRow extends Table {

    public WinRow(Skin skin, String icon, String title, String desc, Actor trailing, Runnable onClick) {
        left().top();
        defaults().left().pad(11, 14, 11, 14);
        setTouchable(Touchable.enabled);

        if (icon != null && !icon.isEmpty()) {
            Actor iconActor = emojiOrText(icon);
            if (iconActor != null) add(iconActor).padRight(12).top();
        }
        Table text = new Table();
        text.left();
        text.defaults().left();
        text.add(new BsText(title == null ? "" : title, BsText.Size.DEFAULT));
        if (desc != null && !desc.isEmpty()) {
            text.row();
            text.add(new BsText(desc, BsText.Size.SM, BsText.Variant.MUTED)).padTop(2);
        }
        add(text).growX();

        if (trailing != null) {
            add(trailing).right().padLeft(16).top();
        }

        // hover：setBackground。注意必须用 BsSkinFactory.drawableOf 生成纯色 Drawable，
        // 不能用 skin.newDrawable("white", color) —— 后者基于 2×2 的 NinePatchDrawable(切边1px)，
        // 画到大尺寸时四个角区只占 1px、中间无拉伸区，结果几乎画不出任何东西。
        final Drawable hover = BsSkinFactory.drawableOf(BsTheme.bh());
        addListener(new InputListener() {
            @Override public void enter(InputEvent e, float x, float y, int pointer, Actor from) {
                if (pointer == -1) setBackground(hover);
            }
            @Override public void exit(InputEvent e, float x, float y, int pointer, Actor to) {
                if (pointer != -1) return;
                if (isOurSelectBoxPopup(to)) return;   // 鼠标进入行内下拉 → 保持 hover，不关 popup
                setBackground((Drawable) null);
            }
        });
        // 点击：仅导航行（onClick != null）添加 ClickListener；设置行（含 BsSelectBox 等）
        // 不加任何点击监听，控件自由处理事件。
        if (onClick != null) {
            addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    onClick.run();
                }
            });
        }
    }

    /**
     * 判断 toActor 是否属于本行内某个 {@link SelectBox} 已展开的下拉 popup。
     *
     * <p>从 {@link SelectBox#getList()} 向上爬到 popup 根（stage 直接子，通常是 SelectBoxList 容器），
     * 然后检查 toActor 是否在该 popup 根的子树内 —— 这样 List、ScrollPane（滚动条/边距）、
     * SelectBoxList 外壳、list item 全都算「没离开」，避免命中容器部分时误判 exit 关闭下拉。</p>
     */
    private boolean isOurSelectBoxPopup(Actor to) {
        if (to == null) return false;
        // 1. 收集本行所有 SelectBox 的 popup 根
        Array<Actor> popupRoots = new Array<>();
        Array<Actor> stack = new Array<>();
        stack.add(this);
        while (stack.size > 0) {
            Actor a = stack.pop();
            if (a instanceof SelectBox) {
                try {
                    Actor list = ((SelectBox<?>) a).getList();
                    if (list != null) {
                        // 爬到 stage 直接子（popup 根）
                        Actor root = list;
                        while (root.getParent() != null && root.getParent().getParent() != null) {
                            root = root.getParent();
                        }
                        popupRoots.add(root);
                    }
                } catch (Throwable ignored) {}
            }
            if (a instanceof Group) {
                stack.addAll(((Group) a).getChildren());
            }
        }
        // 2. to 是否在任一 popup 根的子树内（含根自己）
        Actor cur = to;
        while (cur != null) {
            for (Actor r : popupRoots) {
                if (cur == r) return true;
            }
            cur = cur.getParent();
        }
        return false;
    }

    /** 导航行：trailing 为 › 箭头，整行点击触发回调（主页推荐设置用）。 */
    public static WinRow nav(Skin skin, String icon, String title, String desc, Runnable onClick) {
        return new WinRow(skin, icon, title, desc, new BsText("›", BsText.Size.LG, BsText.Variant.MUTED), onClick);
    }

    /**
     * 把 icon 字符串转成 Actor：
     * <ul>
     *   <li>如果 BsEmoji 已加载且该字符串对应 emoji region → 返回 {@link Image}（彩色 emoji 图标）</li>
     *   <li>否则 → 返回 {@link BsText}（文字符号，如 ⌂ ⓑ ⊞ ⟳ ⚡）</li>
     * </ul>
     */
    private static Actor emojiOrText(String icon) {
        if (BsEmoji.isLoaded()) {
            Drawable emojiD = BsEmoji.get(icon);
            if (emojiD != null) {
                Image img = new Image(emojiD);
                // emoji 32×32 源图，缩放到接近 LG 文字的视觉大小（约 24px）
                img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                return img;
            }
        }
        return new BsText(icon, BsText.Size.LG);
    }
}
