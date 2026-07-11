package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.ui.BsButton;
import com.git.bs.i18n.BsI18n;
import com.git.bs.ui.BsLink;
import com.git.bs.ui.BsSelectBox;
import com.git.bs.ui.BsSwitch;
import com.git.bs.ui.BsText;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 分类设置页基类：声明式填 groups，统一渲染「面包屑 + 大标题 + 多张设置卡片」。
 *
 * <p>子类只需在构造里调 {@link #group(String, SettingItem...)} 填充内容，无需关心渲染。
 * 操作（toggle/select/button/link）默认打印日志；{@link SettingItem.Type#PAGE} 行带 › 箭头、
 * 整行点击跳转到 {@link SettingItem#pageKey} 二级页面。</p>
 *
 * <p>面包屑：一级页默认「主页 › 标题」；二级页用 {@link #CategoryPage(String, String, Skin)}
 * 传入完整面包屑（如「主页 › 系统 › 显示」）。</p>
 */
@Slf4j
public abstract class CategoryPage extends SettingsPage {

    protected final String title;
    protected final String breadcrumb;
    protected final List<SettingGroup> groups = new ArrayList<>();

    protected CategoryPage(String title, Skin skin) {
        this(title, BsI18n.get("nav.home") + "  ›  " + title, skin);
    }

    /** 二级页用：传入完整面包屑（如「主页 › 系统 › 显示」）。 */
    protected CategoryPage(String title, String breadcrumb, Skin skin) {
        super(skin);
        this.title = title;
        this.breadcrumb = breadcrumb;
    }

    /** 子类构造里调用：声明一张设置卡片（可选标题 + 多个设置项）。 */
    protected void group(String name, SettingItem... items) {
        groups.add(new SettingGroup(name, Arrays.asList(items)));
    }

    @Override
    public Actor buildView(Router router) {
        Table col = new Table();
        col.top().left();
        col.defaults().growX().left().top();
        col.add(new BsText(breadcrumb, BsText.Size.SM, BsText.Variant.MUTED)).row();
        col.add(new BsText(title, BsText.Size.XL).bold()).padBottom(10).row();
        for (SettingGroup g : groups) {
            col.add(renderGroup(g, router)).padBottom(8).row();
        }
        return col;
    }

    /** 一张卡片：可选组标题 + 多行设置项。 */
    private Actor renderGroup(SettingGroup g, Router router) {
        Table card = new Table();
        card.setBackground(skin.getDrawable("bs-window-bg"));
        card.pad(14).left().top();
        card.defaults().growX().left().top();

        if (g.title != null && !g.title.isEmpty()) {
            card.add(new BsText(g.title, BsText.Size.SM, BsText.Variant.PRIMARY).bold()).padBottom(8).row();
        }
        Table items = new Table();
        items.top().left();
        items.defaults().growX().left().top();
        for (SettingItem it : g.items) items.add(renderRow(it, router)).row();
        card.add(items).growX();
        return card;
    }

    /** 一行：PAGE → WinRow.nav（箭头+跳转）；其他 → WinRow + 控件。 */
    private Actor renderRow(SettingItem it, Router router) {
        if (it.type == SettingItem.Type.PAGE) {
            final String key = it.pageKey;
            return WinRow.nav(skin, it.icon, it.title, it.desc, () -> router.navigate(key));
        }
        return new WinRow(skin, it.icon, it.title, it.desc, renderControl(it), null);
    }

    /** 按 type 渲染右侧控件，操作统一打日志 + 可选业务回调。 */
    private Actor renderControl(SettingItem it) {
        switch (it.type) {
            case TOGGLE: {
                BsSwitch sw = new BsSwitch(skin);
                sw.setChecked(it.toggleOn);
                sw.setOnChange(c -> {
                    log.info("[{}] {} = {}", title, it.title, c ? "开" : "关");
                    if (it.action != null) it.action.accept(String.valueOf(c));
                });
                return sw;
            }
            case SELECT: {
                BsSelectBox<String> box = new BsSelectBox<>(skin);
                if (it.options != null) box.setItems(it.options);
                if (it.selected != null) {
                    try { box.setSelected(it.selected); } catch (Throwable ignored) {}
                }
                box.addListener(new ChangeListener() {
                    @Override public void changed(ChangeEvent event, Actor actor) {
                        String v = box.getSelected();
                        log.info("[{}] {} = {}", title, it.title, v);
                        if (it.action != null) it.action.accept(v);
                    }
                });
                return box;
            }
            case BUTTON: {
                BsButton btn = new BsButton(it.value != null ? it.value : BsI18n.get("common.open"), skin,
                        BsButton.Variant.PRIMARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
                btn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        log.info("[{}] 点击 {}", title, it.title);
                        if (it.action != null) it.action.accept(null);
                    }
                });
                return btn;
            }
            case LINK: {
                BsLink link = new BsLink(it.value != null ? it.value : BsI18n.get("common.learn_more"), skin);
                link.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        log.info("[{}] 链接 {}", title, it.title);
                        if (it.action != null) it.action.accept(null);
                    }
                });
                return link;
            }
            case VALUE:
            default:
                return new BsText(it.value != null ? it.value : "--", BsText.Size.DEFAULT, BsText.Variant.SECONDARY);
            case CUSTOM: {
                // 自定义控件：直接把工厂返回的 Actor 放 trailing（日期/时间选择器等）
                if (it.customControl != null) {
                    try {
                        Actor a = it.customControl.get();
                        if (a != null) return a;
                    } catch (Throwable t) {
                        log.warn("CUSTOM 控件工厂异常: {}", it.title, t);
                    }
                }
                return new BsText("--", BsText.Size.DEFAULT, BsText.Variant.SECONDARY);
            }
        }
    }
}
