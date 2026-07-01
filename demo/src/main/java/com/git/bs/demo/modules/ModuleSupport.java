package com.git.bs.demo.modules;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.ui.BsAlert;
import com.git.bs.ui.BsAvatar;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsOffcanvas;
import com.git.bs.ui.BsToast;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * 模块共享静态辅助工具：抽取被多个 module 类共用的 helper（避免每个模块重复定义）。
 *
 * <p>不持有任何可变状态；所有方法都是无副作用的工厂方法。</p>
 */
@Slf4j
public final class ModuleSupport {

    private ModuleSupport() {}

    /** 把 java.util.List&lt;String&gt; 转成 libgdx 的 Array&lt;String&gt;（SelectBox/List.setItems 需要）。 */
    public static com.badlogic.gdx.utils.Array<String> items(java.util.List<String> in) {
        com.badlogic.gdx.utils.Array<String> a = new com.badlogic.gdx.utils.Array<>(in.size());
        for (String s : in) a.add(s);
        return a;
    }

    /** 生成模块大标题 Label（深色、fontScale 1.4）。 */
    public static Label sectionTitle(Skin skin, String text) {
        Label l = new Label(text, skin);
        l.setColor(new Color(0.1f, 0.1f, 0.15f, 1f));
        l.setFontScale(1.4f);
        return l;
    }

    /** 生成 click 监听：点击时把信息写到状态行 + 日志。 */
    public static ClickListener logClick(Consumer<String> setStatus, String category, String detail) {
        return new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                String msg = category + ": " + detail;
                setStatus.accept(msg);
                log.info("[click] {}", msg);
            }
        };
    }

    /** 把自绘图表 Actor 包装成 Container 以便加入 Table。 */
    public static <T extends Actor> Container<T> wrapChart(T chart, float w, float h) {
        return wrapFill(chart, w, h);
    }

    /** 把任意 Actor 包装成 fill + 指定尺寸的 Container（避免匿名 Container 双花括号初始化）。 */
    public static <T extends Actor> Container<T> wrapFill(T actor, float w, float h) {
        Container<T> wrap = new Container<>(actor);
        wrap.fill();
        wrap.size(w, h);
        return wrap;
    }

    /** Progress 模块用的控制按钮。 */
    public static BsButton progBtn(Skin skin, String label, Runnable action) {
        BsButton b = new BsButton(label, skin, BsButton.Variant.SECONDARY, BsButton.Style.SOLID, BsButton.Size.SM);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                try { action.run(); } catch (Throwable t) {
                    log.warn("progBtn action error", t);
                }
            }
        });
        return b;
    }

    /** Toast 不同位置演示按钮。 */
    public static BsButton toastPlaceBtn(Skin skin, com.badlogic.gdx.scenes.scene2d.Stage stage,
                                         Consumer<String> setStatus, String label, BsToast.Placement p) {
        BsButton b = new BsButton(label, skin, BsButton.Variant.INFO, BsButton.Style.OUTLINE, BsButton.Size.SM);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsToast.show(stage, skin, label, "位置: " + p.name(),
                        BsToast.Variant.PRIMARY, 2.5f, p);
                setStatus.accept("Toast 位置: " + p);
            }
        });
        return b;
    }

    /** Offcanvas 抽屉演示按钮。 */
    public static BsButton drawerBtn(Skin skin, com.badlogic.gdx.scenes.scene2d.Stage stage,
                                      Consumer<String> setStatus, String label,
                                      BsOffcanvas.Placement p, float w, float h) {
        BsButton b = new BsButton(label, skin, BsButton.Variant.INFO, BsButton.Style.OUTLINE, BsButton.Size.SM);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsOffcanvas off = new BsOffcanvas(skin, p);
                off.setTitle(label);
                Table content = new Table();
                content.left().pad(8);
                content.add(new Label("这是 " + p.name() + " 方向的抽屉内容。", skin)).left().row();
                content.add(new Label("• 选项 1", skin)).left().padTop(6).row();
                content.add(new Label("• 选项 2", skin)).left().row();
                content.add(new Label("• 选项 3", skin)).left().row();
                off.setContent(content);
                if (w > 0) off.setDrawerWidth(w);
                if (h > 0) off.setDrawerHeight(h);
                off.setOnClose(() -> setStatus.accept("抽屉关闭: " + p));
                off.show(stage);
                setStatus.accept("抽屉打开: " + p);
            }
        });
        return b;
    }

    /** 构造一个触发 Alert 弹窗的按钮（无标题，仅消息）。 */
    public static BsButton alertBtn(Skin skin, com.badlogic.gdx.scenes.scene2d.Stage stage,
                                     Consumer<String> setStatus, String label,
                                     com.git.bs.ui.BsAlertDialog.Level level, String msg) {
        return alertBtn(skin, stage, setStatus, label, level, null, msg);
    }

    /** 构造一个触发 Alert 弹窗的按钮（可带标题；title 为 null 时无标题）。 */
    public static BsButton alertBtn(Skin skin, com.badlogic.gdx.scenes.scene2d.Stage stage,
                                     Consumer<String> setStatus, String label,
                                     com.git.bs.ui.BsAlertDialog.Level level, String title, String msg) {
        BsButton.Variant v = com.git.bs.ui.BsAlertDialog.levelButtonVariant(level);
        BsButton b = new BsButton(label, skin, v, BsButton.Style.SOLID, BsButton.Size.SM);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsAlertDialog.show(stage, skin, level, title, msg);
                setStatus.accept("Alert: " + level);
            }
        });
        return b;
    }

    /** Alert 横条文案（按 variant）。 */
    public static String alertMessage(BsAlert.Variant v) {
        switch (v) {
            case PRIMARY:   return "这是一条 primary 提示,通常用于一般性说明。";
            case SECONDARY: return "这是一条 secondary 提示,样式较中性。";
            case SUCCESS:   return "操作成功完成!数据已保存。";
            case DANGER:    return "操作失败!请检查网络连接后重试。";
            case WARNING:   return "此操作不可逆,请谨慎确认。";
            case INFO:      return "提示:你可以点击右侧 × 关闭本提示。";
        }
        return "";
    }

    /** 手风琴内容构造（标题 + 子项列表）。 */
    public static Table makeAccordionContent(Skin skin, String text) {
        Table t = new Table();
        t.left().pad(8, 4, 8, 4);
        Label l = new Label(text, skin);
        l.setWrap(true);
        t.add(l).growX().left().row();
        t.add(new Label("• 子项 1", skin)).left().row();
        t.add(new Label("• 子项 2", skin)).left().row();
        t.add(new Label("• 子项 3", skin)).left().row();
        return t;
    }

    /** 轮播色块 slide（标题 + 副标题居中）。 */
    public static Actor makeColorSlide(Skin skin, String title, String subtitle, Color bg) {
        Table t = new Table(skin);
        t.setBackground(skin.newDrawable("white", bg));
        t.center();
        Label t1 = new Label(title, skin);
        t1.setColor(Color.WHITE);
        t1.setFontScale(1.6f);
        Label t2 = new Label(subtitle, skin);
        t2.setColor(new Color(1, 1, 1, 0.85f));
        t2.setFontScale(1.1f);
        t.add(t1).row();
        t.add(t2).padTop(8).row();
        return t;
    }

    /** 轮播图片 slide（图 + 角标）。 */
    public static Actor makeImageSlide(Skin skin,
                                        com.badlogic.gdx.scenes.scene2d.utils.Drawable img, String tag) {
        Table t = new Table(skin);
        t.setBackground(img);
        t.bottom().left();
        Label tagLabel = new Label(tag, skin);
        tagLabel.setColor(Color.WHITE);
        tagLabel.setFontScale(1.1f);
        Container<Label> tagWrap = new Container<>(tagLabel);
        tagWrap.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.5f)));
        tagWrap.pad(4, 10, 4, 10);
        t.add(tagWrap).pad(10).left();
        return t;
    }

    /** Avatar 包装（添加下方说明文字）。 */
    public static Table wrapAvatar(Skin skin, BsAvatar av, String label) {
        Table t = new Table();
        t.top();
        t.add(av).row();
        com.badlogic.gdx.scenes.scene2d.ui.Label l = new com.badlogic.gdx.scenes.scene2d.ui.Label(label, skin);
        l.setColor(Color.GRAY);
        l.setFontScale(0.85f);
        t.add(l).padTop(4);
        return t;
    }
}
