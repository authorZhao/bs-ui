/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库
 * Copyright (c) 2026 bs-ui contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */

package cn.pingyuanren.bs.admin.modules;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import cn.pingyuanren.bs.admin.AdminModule;
import cn.pingyuanren.bs.admin.BsAdminShell;
import cn.pingyuanren.bs.ui.BsCard;
import cn.pingyuanren.bs.ui.BsTheme;
import cn.pingyuanren.bs.ui.BsUI;

/**
 * 主页面板：4 个统计卡 + 最近活动列表。
 * 路径："首页"（点击 logo / 面包屑首页都进这里）。
 * @author authorZhao
 * @since 2026-07-16
 */
public class DashboardModule implements AdminModule {

    public static final String PATH = "首页";

    @Override
    public String getPath() {
        return PATH;
    }

    @Override
    public Actor buildView(BsAdminShell shell) {
        Skin skin = BsUI.getSkin();
        Table root = new Table();
        root.top().left();
        root.pad(16);
        root.defaults().top().left().pad(8);

        // 标题
        Label.LabelStyle xl = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        xl.font = skin.getFont("font-xl");
        Label title = new Label("仪表盘", xl);
        title.setColor(BsTheme.tp());
        root.add(title).left().padBottom(8).row();

        // 4 个统计卡（2x2），高度按内容自适应（之前写死 110，内容溢出底部）
        Table statsRow1 = new Table();
        statsRow1.defaults().pad(8).growX();
        statsRow1.add(statCard(skin, "用户数", "1,284", "+12%"));
        statsRow1.add(statCard(skin, "订单数", "3,421", "+5%"));

        Table statsRow2 = new Table();
        statsRow2.defaults().pad(8).growX();
        statsRow2.add(statCard(skin, "收入 (¥)", "98,210", "+8%"));
        statsRow2.add(statCard(skin, "活跃用户", "456", "+3%"));

        root.add(statsRow1).growX().row();
        root.add(statsRow2).growX().row();

        // 最近活动
        root.add(activityCard(skin)).growX().padTop(16).row();

        return root;
    }

    private BsCard statCard(Skin skin, String name, String value, String delta) {
        BsCard card = new BsCard(skin);

        Label n = new Label(name, skin);
        n.setColor(BsTheme.ts());
        Label.LabelStyle vStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        vStyle.font = skin.getFont("font-xl");
        Label v = new Label(value, vStyle);
        v.setColor(BsTheme.tp());
        Label d = new Label(delta, skin);
        d.setColor(BsTheme.colorOf("success"));

        // 用 addCustom 走 bodyTable 的标准接口（bodyTable.defaults().growX().left() 已配置好），
        // 之前 clearChildren + 自建 defaults 会丢 growX，导致 Label 宽度异常、数字跑到卡片外
        card.addCustom(n);
        Table valueRow = new Table();
        valueRow.add(v).left();
        card.addCustom(valueRow);
        card.addCustom(d);
        return card;
    }

    private BsCard activityCard(Skin skin) {
        BsCard card = new BsCard(skin);
        card.title("最近活动");
        Table body = card.getBodyTable();
        body.top().left();
        body.defaults().top().left().pad(4);

        String[] acts = {
                "admin 登录系统",
                "新增用户 zhangsan",
                "更新订单 #10234 状态为已发货",
                "lisi 修改了个人资料",
                "系统主题切换为 Dark"
        };
        for (int i = 0; i < acts.length; i++) {
            Label dot = new Label("•", skin);
            dot.setColor(BsTheme.colorOf("primary"));
            Label t = new Label(acts[i], skin);
            t.setColor(BsTheme.tp());
            Table line = new Table();
            line.defaults().left().pad(0, 4, 0, 4);
            line.add(dot).width(16).top();
            line.add(t).growX().left();
            body.add(line).growX().left().row();
        }
        return card;
    }
}
