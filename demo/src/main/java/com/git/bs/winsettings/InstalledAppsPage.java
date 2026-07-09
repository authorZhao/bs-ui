package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsText;
import com.git.bs.ui.BsTextField;
import lombok.extern.slf4j.Slf4j;

/**
 * 二级页面：已安装的应用（按 Win11「应用 › 已安装的应用」）。
 *
 * <p>结构：面包屑 + 标题 + 搜索框 + 排序筛选条 + 应用列表卡片（每行：图标 + 名称 + 发布方/大小/日期 + 卸载按钮）。
 * 非声明式（应用列表是动态行），所以直接 extends {@link SettingsPage} 而非 CategoryPage。</p>
 */
@Slf4j
public class InstalledAppsPage extends SettingsPage {

    /** {图标, 名称, 发布方, 大小, 安装日期}。 */
    private static final String[][] APPS = {
            {"🌐", "Microsoft Edge",       "Microsoft Corporation", "245 MB", "2024-12-01"},
            {"💬", "Microsoft Teams",       "Microsoft Corporation", "310 MB", "2024-11-15"},
            {"🛒", "Microsoft Store",       "Microsoft Corporation", "95 MB",  "2024-10-20"},
            {"📷", "照片",                 "Microsoft Corporation", "88 MB",  "2024-09-10"},
            {"🎵", "媒体播放器",            "Microsoft Corporation", "65 MB",  "2024-08-22"},
            {"📁", "文件资源管理器",         "Microsoft Corporation", "45 MB",  "2024-07-30"},
            {"⚙",  "设置",                 "Microsoft Corporation", "38 MB",  "2024-06-15"},
            {"🎨", "画图",                 "Microsoft Corporation", "24 MB",  "2024-05-18"},
            {"📝", "记事本",               "Microsoft Corporation", "12 MB",  "2024-04-10"},
            {"🎮", "Xbox",                "Microsoft Corporation", "120 MB", "2024-03-05"},
            {"🔢", "计算器",               "Microsoft Corporation", "18 MB",  "2024-02-20"},
            {"🌐", "Visual Studio Code",   "Microsoft Corporation", "350 MB", "2024-01-12"},
    };

    public InstalledAppsPage(Skin skin) {
        super(skin);
    }

    @Override
    public Actor buildView(Router router) {
        Table col = new Table();
        col.top().left();
        col.defaults().growX().left().top();
        col.add(new BsText("主页  ›  应用  ›  已安装的应用", BsText.Size.SM, BsText.Variant.MUTED)).row();
        col.add(new BsText("已安装的应用", BsText.Size.XL).bold()).padBottom(10).row();

        // 搜索框
        BsTextField search = new BsTextField("", skin);
        search.setMessageText("搜索应用");
        col.add(search).growX().padBottom(10).row();

        // 排序 + 筛选条
        Table filter = new Table();
        filter.left();
        filter.defaults().left().padRight(20);
        filter.add(new BsText("排序:  名称 ▾", BsText.Size.SM, BsText.Variant.SECONDARY));
        filter.add(new BsText("筛选:  所有驱动器 ▾", BsText.Size.SM, BsText.Variant.SECONDARY));
        filter.add(new BsText(APPS.length + " 个应用", BsText.Size.SM, BsText.Variant.MUTED));
        col.add(filter).padBottom(8).row();

        // 应用列表卡片
        Table card = new Table();
        card.setBackground(skin.getDrawable("bs-window-bg"));
        card.pad(4).left().top();
        card.defaults().growX().left().top();
        for (String[] a : APPS) {
            card.add(appRow(a)).growX().row();
        }
        col.add(card).growX().row();
        col.add().height(220).row();   // 底部留白：扩大滚动范围
        return col;
    }

    /** 单个应用行：图标 + 名称 + 发布方/大小/日期 + 卸载按钮。 */
    private Actor appRow(String[] a) {
        BsButton uninstall = new BsButton("卸载", skin,
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        uninstall.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                log.info("[已安装的应用] 卸载 {}", a[1]);
            }
        });
        return new WinRow(skin, a[0], a[1], a[2] + "  ·  " + a[3] + "  ·  " + a[4], uninstall, null);
    }
}
