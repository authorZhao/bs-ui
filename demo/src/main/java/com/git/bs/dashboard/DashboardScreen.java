package com.git.bs.dashboard;

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
import com.git.bs.ui.BsChart;
import com.git.bs.ui.BsCircularProgress;
import com.git.bs.ui.BsLineChart;
import com.git.bs.ui.BsRingProgress;
import com.git.bs.ui.BsScrollPane;
import com.git.bs.ui.BsStatistic;
import com.git.bs.ui.BsText;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsUI;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 运维监控大屏主屏幕（铺满版）。
 *
 * <p>三栏布局，所有模块由 {@link MockMonitorDataSource} 每秒 tick 驱动：</p>
 * <ul>
 *   <li><b>左栏</b>：服务节点列表（状态灯 + CPU%）+ 机房环境（温湿度）</li>
 *   <li><b>中栏</b>：6 个 KPI 大数字（64px bigNum 字体）+ CPU/内存折线 + JVM（堆仪表+GC+线程+堆时序）</li>
 *   <li><b>右栏</b>：实时访问日志（滚动）+ 告警时间线</li>
 * </ul>
 *
 * <p>文字统一用 {@link BsText}（setVariant 改 style.fontColor，避免裸 Label 的 setColor 被默认
 * fontColor 遮盖导致偏色）。KPI 数值用运行时生成的 font-big-num（64px ASCII）。</p>
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
    // 环境
    private BsText tempText, humText, clock;
    // 日志 / 告警容器
    private Table logTable, alertTable;

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

        Table body = new Table();
        body.defaults().top().left();
        body.add(buildLeft(skin)).width(230).growY().padRight(8);
        body.add(buildCenter(skin)).grow().growY().padRight(8);
        body.add(buildRight(skin)).width(310).growY();
        root.add(body).growX().growY().padBottom(8).row();

        stage.addActor(root);

        data.addListener(this::refresh);
        refresh();
    }

    // =================== 布局 ===================

    private Table buildHeader() {
        Table h = new Table();
        h.left();
        h.defaults().left();
        h.add(new BsText("运维监控大屏", BsText.Size.XL).bold()).padRight(20);
        h.add(new BsText("Real-time Operations Dashboard", BsText.Size.SM, BsText.Variant.MUTED)).padRight(20);
        clock = new BsText("", BsText.Size.MD, BsText.Variant.SECONDARY);
        h.add(clock);
        h.add().growX();
        return h;
    }

    private Table buildLeft(Skin skin) {
        Table col = new Table();
        col.top().left();
        col.defaults().growX().left().top();

        col.add(sectionTitle("服务节点")).padBottom(6).row();
        Table nodeList = new Table();
        nodeList.top().left();
        nodeRows.clear();
        for (MockMonitorDataSource.NodeStatus n : data.nodes) {
            BsText status = new BsText("● " + n.name, BsText.Size.DEFAULT);
            BsText cpu = new BsText("--%", BsText.Size.SM);
            Table row = new Table();
            row.defaults().left();
            row.add(status).width(120);
            row.add(cpu).width(55);
            row.add().growX();
            nodeList.add(row).growX().pad(3).row();
            nodeRows.add(new BsText[]{status, cpu});
        }
        col.add(wrapCard(skin, nodeList)).growX().top().padBottom(8).row();

        col.add(sectionTitle("机房环境")).padBottom(6).row();
        Table env = new Table();
        env.left().top();
        env.defaults().left().pad(4);
        tempText = new BsText("--", BsText.Size.LG);
        humText = new BsText("--", BsText.Size.LG);
        env.add(new BsText("温度", BsText.Size.SM, BsText.Variant.SECONDARY)).padRight(10);
        env.add(tempText).row();
        env.add(new BsText("湿度", BsText.Size.SM, BsText.Variant.SECONDARY)).padRight(10);
        env.add(humText).row();
        col.add(wrapCard(skin, env)).growX().top().row();
        col.add().growY();
        return col;
    }

    private Table buildCenter(Skin skin) {
        Table col = new Table();
        col.top().left();
        col.defaults().growX().left().top();

        // KPI 行 6 个大数字
        Table kpiRow = new Table();
        kpiRow.defaults().pad(2);
        cpuStat = kpi("CPU %");
        memStat = kpi("内存 %");
        netStat = kpi("网络 Mbps");
        qpsStat = kpi("QPS");
        userStat = kpi("在线用户");
        errStat = kpi("错误率 %");
        kpiRow.add(wrapCard(skin, cpuStat)).growX();
        kpiRow.add(wrapCard(skin, memStat)).growX();
        kpiRow.add(wrapCard(skin, netStat)).growX();
        kpiRow.add(wrapCard(skin, qpsStat)).growX();
        kpiRow.add(wrapCard(skin, userStat)).growX();
        kpiRow.add(wrapCard(skin, errStat)).growX();
        col.add(kpiRow).growX().height(115).padBottom(8).row();

        // CPU + 内存 折线（叠放在一个卡里）
        Table chartBox = new Table();
        chartBox.top().left();
        chartBox.defaults().growX().left();
        chartBox.add(sectionTitle("CPU 使用率 (%)")).padBottom(4).row();
        cpuChart = makeLineChart();
        chartBox.add(cpuChart).growX().height(170).row();
        chartBox.add(sectionTitle("内存使用率 (%)")).padTop(6).padBottom(4).row();
        memChart = makeLineChart();
        chartBox.add(memChart).growX().height(170).padBottom(8).row();
        col.add(wrapCard(skin, chartBox)).growX().top().padBottom(8).row();

        // JVM 行
        col.add(sectionTitle("JVM 监控")).padBottom(6).row();
        Table jvm = new Table();
        jvm.left().top();
        jvm.defaults().pad(6).left();
        heapRing = new BsRingProgress(skin, BsCircularProgress.Variant.PRIMARY);
        heapRing.setShowLabel(true);
        jvm.add(heapRing).size(96).padRight(14);
        Table jvmStats = new Table();
        jvmStats.left().top();
        jvmStats.defaults().left().pad(2);
        gcText = new BsText("GC: --", BsText.Size.DEFAULT);
        threadText = new BsText("Threads: --", BsText.Size.DEFAULT);
        jvmStats.add(new BsText("堆内存", BsText.Size.SM, BsText.Variant.MUTED)).row();
        jvmStats.add(gcText).padBottom(6).row();
        jvmStats.add(new BsText("Full GC 累计 / 活动线程", BsText.Size.SM, BsText.Variant.MUTED)).row();
        jvmStats.add(threadText);
        jvm.add(jvmStats).growY();
        heapChart = makeLineChart();
        heapChart.setYTickCount(2).setXTickCount(2);   // 小图刻度稀疏，避免密集重叠看不清
        jvm.add(wrapCard(skin, heapChart)).growX().height(110);
        col.add(wrapCard(skin, jvm)).growX().top().row();
        col.add().growY();
        return col;
    }

    private Table buildRight(Skin skin) {
        Table col = new Table();
        col.top().left();
        col.defaults().growX().left().top();

        col.add(sectionTitle("实时访问日志")).padBottom(6).row();
        logTable = new Table();
        logTable.top().left();
        logTable.defaults().left();
        BsScrollPane logScroll = new BsScrollPane(logTable, skin);
        logScroll.setScrollingDisabled(true, false);
        logScroll.setFadeScrollBars(false);
        col.add(wrapCard(skin, logScroll)).growX().height(340).padBottom(8).row();

        col.add(sectionTitle("告警时间线")).padBottom(6).row();
        alertTable = new Table();
        alertTable.top().left();
        alertTable.defaults().left();
        col.add(wrapCard(skin, alertTable)).growX().top().row();
        col.add().growY();
        return col;
    }

    // =================== 辅助 ===================

    private BsLineChart makeLineChart() {
        BsLineChart c = new BsLineChart();
        c.setSkinFont(BsUI.getSkin());
        c.setLegendVisible(false);
        c.setHoverEnabled(false);
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

        // 节点
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

        // 环境
        tempText.setText(String.format("%.1f ℃", data.temp));
        humText.setText(String.format("%.1f%%", data.humidity));

        // 日志重建（最近 18 条）
        logTable.clearChildren();
        int li = 0;
        for (MockMonitorDataSource.LogEntry e : data.logs) {
            if (li++ >= 18) break;
            String path = e.path.length() > 16 ? e.path.substring(0, 16) + "~" : e.path;
            String line = e.time + "  " + e.status + "  " + path + "  " + Math.round(e.cost) + "ms";
            BsText l = new BsText(line, BsText.Size.SM,
                    e.status >= 500 ? BsText.Variant.DANGER
                    : e.status == 404 ? BsText.Variant.WARNING : BsText.Variant.SECONDARY);
            l.setWrap(true);
            logTable.add(l).width(275).left().row();
        }

        // 告警重建（最近 6 条）
        alertTable.clearChildren();
        int ai = 0;
        for (MockMonitorDataSource.Alert a : data.alerts) {
            if (ai++ >= 6) break;
            boolean critical = a.level.equals("严重") || a.level.equals("错误");
            BsText l = new BsText(a.time + " [" + a.level + "] " + a.msg, BsText.Size.SM,
                    critical ? BsText.Variant.DANGER : BsText.Variant.WARNING);
            l.setWrap(true);
            alertTable.add(l).width(275).left().padTop(3).row();
        }

        if (clock != null) clock.setText(clockFmt.format(new Date()));
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
