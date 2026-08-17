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

package cn.pingyuanren.bs.dashboard;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import cn.pingyuanren.bs.ui.BsAreaChart;
import cn.pingyuanren.bs.ui.BsBarChart;
import cn.pingyuanren.bs.ui.BsChart;
import cn.pingyuanren.bs.ui.BsCircularProgress;
import cn.pingyuanren.bs.ui.BsDoughnutChart;
import cn.pingyuanren.bs.ui.BsLineChart;
import cn.pingyuanren.bs.ui.BsScrollPane;
import cn.pingyuanren.bs.ui.BsStatistic;
import cn.pingyuanren.bs.ui.BsText;
import cn.pingyuanren.bs.ui.BsTheme;
import cn.pingyuanren.bs.ui.BsUI;
import cn.pingyuanren.bs.ui.layout.BsCol;
import cn.pingyuanren.bs.ui.layout.BsFlow;
import cn.pingyuanren.bs.ui.layout.BsGrid;
import cn.pingyuanren.bs.ui.layout.BsRow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 运维监控大屏主屏幕（铺满版）—— 全面采用 bsui 四种基础布局组件。
 *
 * <p>三栏布局，所有模块由 {@link MockMonitorDataSource} 每秒 tick 驱动：</p>
 * <ul>
 *   <li><b>左栏</b>：服务节点列表（{@link BsCol} 纵排）
 *       + 节点负载徽章云（{@link BsFlow} 流式，宽度变化自动换行）
 *       + 机房环境（{@link BsCol}）</li>
 *   <li><b>中栏</b>：6 个 KPI（{@link BsGrid} 6 列网格）
 *       + CPU/内存折线（{@link BsCol}）
 *       + JVM（{@link BsRow} 横排）
 *       + 底部三图（{@link BsGrid} 3 列网格）</li>
 *   <li><b>右栏</b>：实时访问日志 + 告警时间线（{@link BsCol} + ScrollPane）</li>
 * </ul>
 *
 * <p>四种布局语义对照：</p>
 * <table>
 *   <tr><th>布局</th><th>用途</th><th>本屏使用处</th></tr>
 *   <tr><td>{@link BsRow}</td><td>横排</td><td>顶部 header、JVM 行、每行节点状态</td></tr>
 *   <tr><td>{@link BsCol}</td><td>纵排</td><td>左栏、CPU/内存折线卡、日志/告警列表</td></tr>
 *   <tr><td>{@link BsGrid}</td><td>格子</td><td>6 个 KPI、底部 3 张图表</td></tr>
 *   <tr><td>{@link BsFlow}</td><td>流式</td><td>节点负载徽章云</td></tr>
 * </table>
 *
 * <p>KPI 数值用运行时生成的 font-big-num（64px ASCII）；所有图表均开启 hover tooltip。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class DashboardScreen implements Screen {

    private Stage stage;
    private final MockMonitorDataSource data = new MockMonitorDataSource();
    private float tickAccum = 0;
    private BitmapFont bigNum;
    private final SimpleDateFormat clockFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // KPI
    private BsStatistic cpuStat, memStat, netStat, qpsStat, userStat, errStat;
    // 折线
    private BsLineChart cpuChart, memChart, heapChart;
    // JVM
    private BsCircularProgress heapRing;
    private BsText gcText, threadText;
    // 节点（[status, cpu]）
    private final List<BsText[]> nodeRows = new ArrayList<>();
    // 节点负载徽章云（流式）：每个节点一个 BsText
    private final List<BsText> nodeChips = new ArrayList<>();
    // 环境
    private BsText tempText, humText, clock;
    // 日志 / 告警容器
    private Table logTable, alertTable;
    // 底部图表
    private BsBarChart nodeQpsChart;
    private BsDoughnutChart httpStatusChart;
    private BsAreaChart netChart;

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        Skin skin = BsUI.getSkin();
        bigNum = skin.has("font-big-num", BitmapFont.class) ? skin.get("font-big-num", BitmapFont.class) : null;

        for (int i = 0; i < MockMonitorDataSource.HISTORY; i++) data.tick();

        Table root = new Table();
        root.setFillParent(true);
        root.top().left();
        root.pad(10);
        root.defaults().top().left();

        root.add(buildHeader()).growX().padBottom(8).row();

        // body 三栏各自独立垂直滚动 —— 任一栏内容超出视口自己滚，互不挤压（右栏不会再被中栏借走高度）
        Table body = new Table();
        body.defaults().top().left().growY();
        body.add(makeScroll(skin, buildLeft(skin))).width(230).padRight(8);
        body.add(makeScroll(skin, buildCenter(skin))).growX();
        body.add(makeScroll(skin, buildRight(skin))).width(310);
        root.add(body).growX().growY().padBottom(8).row();

        stage.addActor(root);

        data.addListener(this::refresh);
        refresh();
    }

    // =================== 布局 ===================

    /** 顶部 header：用 BsRow 横排（标题 + 副标题 + 时钟，整体左对齐）。 */
    private Actor buildHeader() {
        clock = new BsText("", BsText.Size.MD, BsText.Variant.SECONDARY);
        return new BsRow()
                .gap(20)
                .align("center")      // 垂直方向居中（标题大、副标题小）
                .pad(2)
                .add(new BsText("运维监控大屏", BsText.Size.XL).bold())
                .add(new BsText("Real-time Operations Dashboard", BsText.Size.SM, BsText.Variant.MUTED))
                .add(clock);
    }

    private Actor buildLeft(Skin skin) {
        // 外层用 Table（要配 Cell 的 growX/height），内部子结构用 4 种布局组件
        Table col = new Table();
        col.top().left();
        col.defaults().growX().left().top();

        col.add(sectionTitle("服务节点")).padBottom(6).row();
        // 节点列表：每行用 BsRow（状态 + CPU%）
        BsCol nodeList = new BsCol().gap(2).align("left");
        nodeRows.clear();
        for (MockMonitorDataSource.NodeStatus n : data.nodes) {
            BsText status = new BsText("● " + n.name, BsText.Size.DEFAULT);
            BsText cpu = new BsText("--%", BsText.Size.SM);
            BsRow row = new BsRow().gap(6).align("center")
                    .add(status).add(cpu);
            nodeList.add(row);
            nodeRows.add(new BsText[]{status, cpu});
        }
        col.add(wrapCard(skin, nodeList)).padBottom(8).row();

        col.add(sectionTitle("节点负载")).padBottom(6).row();
        // 节点负载徽章云：BsFlow 流式 —— 宽度不够自动换行
        BsFlow flow = new BsFlow().gap(6).rowGap(4).align("topleft").pad(4);
        nodeChips.clear();
        for (int i = 0; i < data.nodes.size(); i++) {
            BsText chip = new BsText("--", BsText.Size.SM, BsText.Variant.SECONDARY);
            nodeChips.add(chip);
            flow.add(chip);
        }
        col.add(wrapCard(skin, flow)).padBottom(8).row();

        col.add(sectionTitle("机房环境")).padBottom(6).row();
        // 环境温湿度：2 个键值对，用 BsCol 纵排，每行内 BsRow（label + value）
        BsCol env = new BsCol().gap(4).align("left").pad(4);
        tempText = new BsText("--", BsText.Size.LG);
        humText = new BsText("--", BsText.Size.LG);
        env.add(new BsRow().gap(10).align("center")
                .add(new BsText("温度", BsText.Size.SM, BsText.Variant.SECONDARY))
                .add(tempText));
        env.add(new BsRow().gap(10).align("center")
                .add(new BsText("湿度", BsText.Size.SM, BsText.Variant.SECONDARY))
                .add(humText));
        col.add(wrapCard(skin, env)).growX().top().row();
        return col;
    }

    private Actor buildCenter(Skin skin) {
        Table col = new Table();
        col.top().left();
        col.defaults().growX().left().top();

        // KPI 行 6 个：BsGrid 6 列网格（BsStatistic 自带卡片背景，直接放入，避免双层边框）
        BsGrid kpiGrid = new BsGrid(6).gap(4).pad(0).growX();
        cpuStat = kpi("CPU %");
        memStat = kpi("内存 %");
        netStat = kpi("网络 Mbps");
        qpsStat = kpi("QPS");
        userStat = kpi("在线用户");
        errStat = kpi("错误率 %");
        kpiGrid.append(cpuStat, memStat, netStat, qpsStat, userStat, errStat);
        // 不写死 height —— BsStatistic 用 64px bigNum 字体 + pad(16,20,16,20)，内容高度约 130px，
        // 写死 115 会导致大数字下半部分溢出叠到 CPU 折线卡上。改为 minHeight 保底、内容自适应。
        col.add(kpiGrid).growX().minHeight(120).padBottom(8).row();

        // CPU + 内存 折线：Table 纵排（要 Cell.height 控制图高），加大高度与间距
        Table chartBox = new Table();
        chartBox.top().left();
        chartBox.defaults().growX().left();
        chartBox.add(sectionTitle("CPU 使用率 (%)")).padBottom(4).row();
        cpuChart = makeLineChart();
        chartBox.add(cpuChart).height(210).row();
        chartBox.add(sectionTitle("内存使用率 (%)")).padTop(10).padBottom(4).row();
        memChart = makeLineChart();
        chartBox.add(memChart).height(210).padBottom(4).row();
        col.add(wrapCard(skin, chartBox)).growX().top().padBottom(8).row();

        // JVM 行：Table 横排（ring + stats + heapChart，要 Cell 的 size/grow/height）
        col.add(sectionTitle("JVM 监控")).padBottom(6).row();
        Table jvmRow = new Table();
        jvmRow.left().top();
        jvmRow.defaults().pad(6).left();
        heapRing = new BsCircularProgress(skin, BsCircularProgress.Variant.PRIMARY);
        heapRing.setShowLabel(true);
        jvmRow.add(heapRing).size(116).padRight(14);

        // JVM 文本标签用 BsCol 纵排（无需 Cell 尺寸）
        BsCol jvmStats = new BsCol().gap(2).align("left");
        gcText = new BsText("GC: --", BsText.Size.DEFAULT);
        threadText = new BsText("Threads: --", BsText.Size.DEFAULT);
        jvmStats.add(new BsText("堆内存", BsText.Size.SM, BsText.Variant.MUTED));
        jvmStats.add(gcText);
        jvmStats.add(new BsText("Full GC 累计 / 活动线程", BsText.Size.SM, BsText.Variant.MUTED));
        jvmStats.add(threadText);
        jvmRow.add(jvmStats).growY();

        heapChart = makeLineChart();
        heapChart.setYTickCount(2).setXTickCount(2);
        jvmRow.add(wrapCard(skin, heapChart)).growX().height(150);
        col.add(wrapCard(skin, jvmRow)).growX().top().height(170).padBottom(8).row();

        // 底部三图：BsGrid 3 列网格
        col.add(buildBottomCharts(skin)).growX().top().row();
        return col;
    }

    private Actor buildBottomCharts(Skin skin) {
        BsGrid row = new BsGrid(3).gap(8).pad(0).growX();

        // 1) 节点 QPS 柱状图
        Table nodeBox = new Table();
        nodeBox.top().left();
        nodeBox.defaults().growX().left();
        nodeBox.add(sectionTitle("节点 QPS")).padBottom(4).row();
        nodeQpsChart = new BsBarChart();
        nodeQpsChart.setSkinFont(skin);
        nodeQpsChart.setOrientation(BsBarChart.Orientation.VERTICAL);
        nodeQpsChart.setHoverEnabled(true);
        nodeQpsChart.setLegendVisible(false);
        String[] nodeNames = new String[data.nodes.size()];
        for (int i = 0; i < data.nodes.size(); i++) nodeNames[i] = data.nodes.get(i).name;
        nodeQpsChart.setCategories(nodeNames);
        nodeQpsChart.setMultiSeries(Arrays.asList(
                new BsChart.Series("QPS", BsChart.pointsOfY(new float[nodeNames.length]))));
        nodeBox.add(nodeQpsChart).height(150).row();
        row.add(wrapCard(skin, nodeBox));

        // 2) HTTP 状态码 环形图
        Table httpBox = new Table();
        httpBox.top().left();
        httpBox.defaults().growX().left();
        httpBox.add(sectionTitle("HTTP 状态码")).padBottom(4).row();
        httpStatusChart = new BsDoughnutChart();
        httpStatusChart.setSkinFont(skin);
        httpStatusChart.setHoverEnabled(true);
        httpStatusChart.setLegendVisible(true);
        httpStatusChart.setLegendPlacement(BsChart.LegendPlacement.BOTTOM);
        httpStatusChart.setCenterLabel("请求", "--");
        httpStatusChart.setSlices("2xx", 1, "3xx", 0, "4xx", 0, "5xx", 0);
        httpBox.add(httpStatusChart).height(150).row();
        row.add(wrapCard(skin, httpBox));

        // 3) 网络流量 面积图（双系列入/出）
        Table netBox = new Table();
        netBox.top().left();
        netBox.defaults().growX().left();
        netBox.add(sectionTitle("网络流量 (Mbps)")).padBottom(4).row();
        netChart = new BsAreaChart();
        netChart.setSkinFont(skin);
        netChart.setHoverEnabled(true);
        netChart.setLegendVisible(true);
        netChart.setLegendPlacement(BsChart.LegendPlacement.BOTTOM);
        netChart.setMultiSeries(Arrays.asList(
                new BsChart.Series("入站", BsChart.pointsOfY(new float[MockMonitorDataSource.HISTORY]),
                        BsTheme.colorOf("primary")),
                new BsChart.Series("出站", BsChart.pointsOfY(new float[MockMonitorDataSource.HISTORY]),
                        BsTheme.colorOf("success"))));
        netBox.add(netChart).height(150).row();
        row.add(wrapCard(skin, netBox));

        return row;
    }

    private Actor buildRight(Skin skin) {
        // 右栏整体由外层 makeScroll 统一垂直滚动，内部不再嵌 ScrollPane（避免嵌套滚动）
        Table col = new Table();
        col.top().left();
        col.defaults().growX().left().top();

        // 日志区
        col.add(sectionTitle("实时访问日志")).padBottom(6).row();
        logTable = new Table();
        logTable.top().left();
        logTable.defaults().left();
        col.add(wrapCard(skin, logTable)).growX().top().padBottom(8).row();

        // 告警区
        col.add(sectionTitle("告警时间线")).padBottom(6).row();
        alertTable = new Table();
        alertTable.top().left();
        alertTable.defaults().left();
        col.add(wrapCard(skin, alertTable)).growX().top();
        return col;
    }

    // =================== 辅助 ===================

    private BsLineChart makeLineChart() {
        BsLineChart c = new BsLineChart();
        c.setSkinFont(BsUI.getSkin());
        c.setLegendVisible(false);
        c.setHoverEnabled(true);     // 开启鼠标悬停：显示数据点高亮 + x/y 数值 tooltip
        c.setHitRadius(20);          // 放大命中半径，鼠标靠近即可触发
        return c;
    }

    private BsStatistic kpi(String title) {
        BsStatistic s = new BsStatistic(BsUI.getSkin()).title(title).value("--");
        if (bigNum != null) s.valueFont(bigNum);
        return s;
    }

    private BsText sectionTitle(String t) {
        return new BsText(t, BsText.Size.MD, BsText.Variant.PRIMARY).bold();
    }

    private Table wrapCard(Skin skin, Actor body) {
        Table card = new Table();
        card.setBackground(skin.getDrawable("bs-window-bg"));
        card.pad(8);
        card.add(body).grow();
        return card;
    }

    /**
     * 把一栏内容包成可垂直滚动的 ScrollPane：栏内容 prefHeight 不受限，
     * 超出视口高度时纵向滚动；横向锁定（栏宽固定，不出现水平滚动条）。
     * <p>widget 用 Table 是为了在 ScrollPane 里能 growX 撑满栏宽。</p>
     */
    private BsScrollPane makeScroll(Skin skin, Actor content) {
        Table holder = new Table();
        holder.top().left();
        holder.add(content).growX().top().left();
        BsScrollPane sp = new BsScrollPane(holder, skin);
        sp.setScrollingDisabled(true, false);   // 仅纵向滚动
        sp.setFadeScrollBars(false);
        sp.setForceScroll(false, true);
        sp.setScrollbarsOnTop(false);
        return sp;
    }

    // =================== 刷新 ===================

    private void refresh() {
        // KPI
        cpuStat.value(Math.round(data.cpu) + "%").trend(MockMonitorDataSource.trendPct(data.prevCpu, data.cpu));
        memStat.value(Math.round(data.mem) + "%").trend(MockMonitorDataSource.trendPct(data.prevMem, data.mem));
        netStat.value(String.valueOf(Math.round(data.netIn)));
        netStat.trendText("Mbps", BsTheme.ts());
        qpsStat.value(String.valueOf(Math.round(data.qps)));
        qpsStat.trendText("req/s", BsTheme.ts());
        userStat.value(String.valueOf(Math.round(data.activeUsers)));
        userStat.trendText("active", BsTheme.ts());
        errStat.value(String.format("%.2f", data.err));
        errStat.trendText(data.err > 1 ? "告警" : "正常", data.err > 1 ? BsTheme.colorOf("danger") : BsTheme.colorOf("success"));

        // 折线
        cpuChart.setData(BsChart.pointsOfY(toArray(data.cpuHistory)));
        memChart.setData(BsChart.pointsOfY(toArray(data.memHistory)));
        heapChart.setData(BsChart.pointsOfY(toArray(data.heapHistory)));

        // JVM
        heapRing.setPercent(data.heap / 100f);
        heapRing.setVariant(data.heap > 80 ? BsCircularProgress.Variant.DANGER
                : data.heap > 65 ? BsCircularProgress.Variant.WARNING
                : BsCircularProgress.Variant.PRIMARY);
        gcText.setText("GC: " + data.gc);
        threadText.setText("Threads: " + data.threads);

        // 节点列表
        for (int i = 0; i < nodeRows.size() && i < data.nodes.size(); i++) {
            MockMonitorDataSource.NodeStatus n = data.nodes.get(i);
            BsText status = nodeRows.get(i)[0];
            BsText cpu = nodeRows.get(i)[1];
            status.setText((n.online ? "● " : "✕ ") + n.name);
            status.setVariant(!n.online ? BsText.Variant.DANGER
                    : n.cpu > 85 ? BsText.Variant.WARNING : BsText.Variant.SUCCESS);
            cpu.setText(Math.round(n.cpu) + "%");
            cpu.setVariant(n.cpu > 85 ? BsText.Variant.DANGER : BsText.Variant.SECONDARY);
        }

        // 节点负载徽章云（流式）：每秒刷新文字 + 颜色
        for (int i = 0; i < nodeChips.size() && i < data.nodes.size(); i++) {
            MockMonitorDataSource.NodeStatus n = data.nodes.get(i);
            BsText chip = nodeChips.get(i);
            chip.setText(n.name + " " + Math.round(n.cpu) + "%");
            chip.setVariant(!n.online ? BsText.Variant.DANGER
                    : n.cpu > 85 ? BsText.Variant.WARNING
                    : n.cpu > 60 ? BsText.Variant.WARNING : BsText.Variant.SUCCESS);
        }

        // 环境
        tempText.setText(String.format("%.1f ℃", data.temp));
        humText.setText(String.format("%.1f%%", data.humidity));

        // 底部图表
        refreshBottomCharts();

        // 日志重建（最近 30 条，全部入表，滚动查看）
        logTable.clearChildren();
        for (MockMonitorDataSource.LogEntry e : data.logs) {
            String path = e.path.length() > 16 ? e.path.substring(0, 16) + "~" : e.path;
            String line = e.time + "  " + e.status + "  " + path + "  " + Math.round(e.cost) + "ms";
            BsText l = new BsText(line, BsText.Size.SM,
                    e.status >= 500 ? BsText.Variant.DANGER
                    : e.status == 404 ? BsText.Variant.WARNING : BsText.Variant.SECONDARY);
            l.setWrap(true);
            logTable.add(l).width(275).left().padTop(2).row();
        }

        // 告警重建（全部 8 条，滚动查看）
        alertTable.clearChildren();
        for (MockMonitorDataSource.Alert a : data.alerts) {
            boolean critical = a.level.equals("严重") || a.level.equals("错误");
            BsText l = new BsText(a.time + " [" + a.level + "] " + a.msg, BsText.Size.SM,
                    critical ? BsText.Variant.DANGER : BsText.Variant.WARNING);
            l.setWrap(true);
            alertTable.add(l).width(275).left().padTop(3).row();
        }

        if (clock != null) clock.setText(clockFmt.format(new Date()));
    }

    /** 刷新底部三图：节点 QPS 柱状、HTTP 状态环形、网络流量面积。 */
    private void refreshBottomCharts() {
        // 节点 QPS
        float[] qpsVals = new float[data.nodes.size()];
        for (int i = 0; i < data.nodes.size(); i++) qpsVals[i] = data.nodes.get(i).qps;
        nodeQpsChart.setMultiSeries(Arrays.asList(
                new BsChart.Series("QPS", BsChart.pointsOfY(qpsVals))));

        // HTTP 状态码：按 2xx/3xx/4xx/5xx 统计 logs
        int c2 = 0, c3 = 0, c4 = 0, c5 = 0;
        for (MockMonitorDataSource.LogEntry e : data.logs) {
            if (e.status >= 500) c5++;
            else if (e.status >= 400) c4++;
            else if (e.status >= 300) c3++;
            else c2++;
        }
        int total = c2 + c3 + c4 + c5;
        httpStatusChart.setCenterLabel("请求", String.valueOf(total));
        // 全 0 时给 2xx 一个占位 1，避免空图
        httpStatusChart.setSlices(
                "2xx", total == 0 ? 1 : c2,
                "3xx", c3,
                "4xx", c4,
                "5xx", c5);

        // 网络流量（双系列）
        netChart.setMultiSeries(Arrays.asList(
                new BsChart.Series("入站", BsChart.pointsOfY(toArray(data.netInHistory)),
                        BsTheme.colorOf("primary")),
                new BsChart.Series("出站", BsChart.pointsOfY(toArray(data.netOutHistory)),
                        BsTheme.colorOf("success"))));
    }

    private static float[] toArray(FloatArray a) {
        float[] r = new float[a.size];
        for (int i = 0; i < a.size; i++) r[i] = a.get(i);
        return r;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(BsTheme.bgBodyColor(), true);
        tickAccum += delta;
        if (tickAccum >= 1f) { tickAccum -= 1f; data.tick(); }
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { if (stage != null) stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
