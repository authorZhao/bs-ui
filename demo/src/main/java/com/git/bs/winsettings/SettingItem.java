package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 一行设置项的数据描述 + 静态工厂。{@link CategoryPage} 按 {@link #type} 渲染对应控件，
 * 操作时统一打印日志，可选通过 {@link #action} 回调扩展业务逻辑。
 *
 * <p>类型：</p>
 * <ul>
 *   <li>{@link Type#TOGGLE} 开关 / {@link Type#SELECT} 下拉 / {@link Type#BUTTON} 按钮 /
 *       {@link Type#VALUE} 只读值 / {@link Type#LINK} 链接 —— 卡片内设置项</li>
 *   <li>{@link Type#PAGE} 二级页面入口：trailing 显示 › 箭头，整行点击跳转到 {@link #pageKey} 子页</li>
 *   <li>{@link Type#CUSTOM} 自定义控件：通过 {@link #customControl} 工厂提供任意 Actor（如 BsDatePicker）</li>
 * </ul>
 * @author authorZhao
 * @since 2026-07-16
 */
public class SettingItem {

    public enum Type { TOGGLE, SELECT, BUTTON, VALUE, LINK, PAGE, CUSTOM }

    public final String title;
    public final String desc;
    public final Type type;
    /** 操作回调（SELECT/BUTTON/LINK 传字符串值或 null，TOGGLE 传 "true"/"false"）；null 表示仅打日志。 */
    public final Consumer<String> action;

    public boolean toggleOn;
    public String[] options;
    public String selected;
    public String value;
    /** PAGE 类型：跳转的二级页 key（如 "apps/installed"、"system/display"）。 */
    public String pageKey;
    /** 可选左侧图标符号（如 "🔊"），CategoryPage 渲染时显示；默认 null 无图标。 */
    public String icon;
    /** CUSTOM 类型：右侧自定义控件的工厂（每次渲染调一次，返回的 Actor 直接放 trailing）。 */
    public Supplier<Actor> customControl;

    private SettingItem(String title, String desc, Type type, Consumer<String> action) {
        this.title = title; this.desc = desc; this.type = type; this.action = action;
    }

    // ---- 工厂（带回调） ----

    public static SettingItem toggle(String title, String desc, boolean on, Consumer<Boolean> cb) {
        SettingItem it = new SettingItem(title, desc, Type.TOGGLE,
                cb == null ? null : s -> cb.accept(Boolean.parseBoolean(s)));
        it.toggleOn = on;
        return it;
    }

    public static SettingItem select(String title, String desc, String[] options, String selected, Consumer<String> cb) {
        SettingItem it = new SettingItem(title, desc, Type.SELECT, cb);
        it.options = options;
        it.selected = selected;
        return it;
    }

    public static SettingItem button(String title, String desc, String label, Runnable r) {
        SettingItem it = new SettingItem(title, desc, Type.BUTTON, r == null ? null : s -> r.run());
        it.value = label;
        return it;
    }

    public static SettingItem link(String title, String desc, String label, Runnable r) {
        SettingItem it = new SettingItem(title, desc, Type.LINK, r == null ? null : s -> r.run());
        it.value = label;
        return it;
    }

    public static SettingItem value(String title, String desc, String value) {
        SettingItem it = new SettingItem(title, desc, Type.VALUE, null);
        it.value = value;
        return it;
    }

    /** 二级页面入口：整行带 › 箭头，点击跳转到 pageKey 子页。 */
    public static SettingItem page(String title, String desc, String pageKey) {
        SettingItem it = new SettingItem(title, desc, Type.PAGE, null);
        it.pageKey = pageKey;
        return it;
    }

    /** 二级页面入口（带图标）。 */
    public static SettingItem page(String icon, String title, String desc, String pageKey) {
        SettingItem it = page(title, desc, pageKey);
        it.icon = icon;
        return it;
    }

    /**
     * 自定义控件：{@link CategoryPage} 把 {@code controlFactory.get()} 返回的 Actor 直接放行右侧 trailing。
     * 适合 BsDatePicker / BsTimePicker / BsColorPicker 等无法用现有 type 表达的控件。
     * @param controlFactory 每次渲染调一次（构造时立即调，返回值持有到行生命周期）
     */
    public static SettingItem custom(String title, String desc, Supplier<Actor> controlFactory) {
        SettingItem it = new SettingItem(title, desc, Type.CUSTOM, null);
        it.customControl = controlFactory;
        return it;
    }

    // ---- 工厂（仅日志，无业务回调） ----

    public static SettingItem toggle(String title, String desc, boolean on) {
        return toggle(title, desc, on, null);
    }

    public static SettingItem select(String title, String desc, String[] options, String selected) {
        return select(title, desc, options, selected, null);
    }

    public static SettingItem button(String title, String desc, String label) {
        return button(title, desc, label, (Runnable) null);
    }

    public static SettingItem link(String title, String desc, String label) {
        return link(title, desc, label, (Runnable) null);
    }
}

/** 一组设置（渲染为一张卡片）：可选标题 + 多个设置项。package-private，同包内 CategoryPage 使用。 */
class SettingGroup {
    final String title;
    final List<SettingItem> items;
    SettingGroup(String title, List<SettingItem> items) {
        this.title = title;
        this.items = items;
    }
}
