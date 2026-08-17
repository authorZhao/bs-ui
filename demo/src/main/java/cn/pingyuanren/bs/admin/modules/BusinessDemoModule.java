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
import cn.pingyuanren.bs.ui.BsButton;
import cn.pingyuanren.bs.ui.BsDataTable;
import cn.pingyuanren.bs.ui.BsModal;
import cn.pingyuanren.bs.ui.BsSwitch;
import cn.pingyuanren.bs.ui.BsTheme;
import cn.pingyuanren.bs.ui.BsToast;
import cn.pingyuanren.bs.ui.BsUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 业务 UI 模块：示例业务页 —— 简化版任务/订单管理。
 * 路径："业务模块"。展示如何在单个模块里组合多个 bs-ui 组件：
 * BsDataTable + BsSwitch（状态切换） + BsModal（详情弹窗）。
 * @author authorZhao
 * @since 2026-07-16
 */
public class BusinessDemoModule implements AdminModule {

    public static final String PATH = "业务模块";

    private static final String[] HEADERS = {"订单号", "客户", "金额(¥)", "状态", "创建时间"};

    private static final String[][] SEED = {
            {"#10234", "Acme 公司", "1,280", "已支付",   "2024-06-01 10:00"},
            {"#10235", "Globex",   "880",   "待支付",   "2024-06-02 11:20"},
            {"#10236", "Initech",  "5,600", "已发货",   "2024-06-03 14:30"},
            {"#10237", "Umbrella", "320",   "已取消",   "2024-06-04 09:15"},
            {"#10238", "Hooli",    "12,000","已支付",   "2024-06-05 16:45"},
            {"#10239", "Pied Piper","780",  "待支付",   "2024-06-06 08:50"}
    };

    private final List<List<String>> orders = new ArrayList<>();
    private BsDataTable table;
    private BsAdminShell shellRef;

    public BusinessDemoModule() {
        for (String[] row : SEED) orders.add(new ArrayList<>(Arrays.asList(row)));
    }

    @Override
    public String getPath() {
        return PATH;
    }

    @Override
    public Actor buildView(BsAdminShell shell) {
        this.shellRef = shell;
        Skin skin = BsUI.getSkin();

        Table root = new Table();
        root.top().left();
        root.pad(16);
        root.defaults().top().left();

        Label.LabelStyle xl = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        xl.font = skin.getFont("font-xl");
        Label title = new Label("订单管理（业务示例）", xl);
        title.setColor(BsTheme.tp());
        root.add(title).left().padBottom(4).row();

        Label hint = new Label("点击表格行查看详情；下方开关可整体切换所有订单为已支付/待支付（演示批量操作）", skin);
        hint.setColor(BsTheme.tm());
        root.add(hint).left().padBottom(8).row();

        // 表格
        table = new BsDataTable(skin);
        table.setHeaders(HEADERS);
        refresh();
        table.setOnRowSelect(idx -> showDetail(shell, idx));
        root.add(table).growX().padBottom(12).row();

        // 批量操作行：开关 + 按钮
        Table ops = new Table();
        ops.defaults().pad(8).left();
        Label swLabel = new Label("全部置为已支付", skin);
        swLabel.setColor(BsTheme.tp());
        BsSwitch sw = new BsSwitch(skin).setLabel("");
        sw.setOnChange(b -> {
            String target = b ? "已支付" : "待支付";
            for (List<String> r : orders) {
                if (!"已取消".equals(r.get(3)) && !"已发货".equals(r.get(3))) {
                    r.set(3, target);
                }
            }
            refresh();
            BsToast.show(shell.getStage(), skin, "未取消/未发货订单全部置为 " + target, BsToast.Variant.SUCCESS, 1.2f);
        });
        ops.add(swLabel);
        ops.add(sw).padRight(20);

        BsButton resetBtn = new BsButton("重置演示数据", skin, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        resetBtn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y, int p, int b) { return true; }
            @Override
            public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y, int p, int b) {
                orders.clear();
                for (String[] row : SEED) orders.add(new ArrayList<>(Arrays.asList(row)));
                refresh();
                BsToast.show(shell.getStage(), skin, "已重置为初始数据", BsToast.Variant.INFO, 1f);
            }
        });
        ops.add(resetBtn);
        ops.add().growX();
        root.add(ops).growX().row();

        return root;
    }

    private void refresh() {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> r : orders) copy.add(new ArrayList<>(r));
        table.setData(copy);
    }

    private void showDetail(BsAdminShell shell, int idx) {
        List<String> row = table.getRow(idx);
        if (row == null || row.isEmpty()) return;
        Skin skin = BsUI.getSkin();
        BsModal modal = new BsModal("订单详情 - " + row.get(0), skin);

        Table body = new Table(skin);
        body.pad(10);
        body.defaults().pad(6).left();
        detailLine(body, "订单号", row.get(0));
        detailLine(body, "客户",   row.get(1));
        detailLine(body, "金额",   "¥" + row.get(2));
        detailLine(body, "状态",   row.get(3));
        detailLine(body, "创建时间", row.get(4));

        // 在弹窗里放一个开关：切换该订单状态
        Table swLine = new Table();
        swLine.defaults().left().pad(0, 4, 0, 4);
        swLine.add(new Label("已支付：", skin));
        BsSwitch sw = new BsSwitch(skin).setChecked("已支付".equals(row.get(3)));
        sw.setOnChange(b -> {
            String target = b ? "已支付" : "待支付";
            // 在原始 orders 里找到对应订单号
            for (List<String> r : orders) {
                if (r.get(0).equals(row.get(0))) {
                    r.set(3, target);
                    break;
                }
            }
            refresh();
            BsToast.show(shell.getStage(), skin, "订单 " + row.get(0) + " → " + target, BsToast.Variant.SUCCESS, 1f);
        });
        swLine.add(sw);
        body.add(swLine).colspan(2).padTop(10).row();

        modal.content(body).contentWidth(420);
        modal.addButton("关闭", modal::close, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        modal.showModal(shell.getStage());
    }

    private void detailLine(Table body, String k, String v) {
        Label kl = new Label(k, body.getSkin());
        kl.setColor(BsTheme.ts());
        Label vl = new Label(v, body.getSkin());
        vl.setColor(BsTheme.tp());
        Table line = new Table();
        line.defaults().left().pad(0, 4, 0, 4);
        line.add(kl).width(80).left();
        line.add(vl).growX().left();
        body.add(line).colspan(2).growX().left().row();
    }
}
