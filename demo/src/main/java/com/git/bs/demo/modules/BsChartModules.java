package com.git.bs.demo.modules;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.git.bs.ui.BsAreaChart;
import com.git.bs.ui.BsBarChart;
import com.git.bs.ui.BsBarChart3D;
import com.git.bs.ui.BsChart;
import com.git.bs.ui.BsDoughnutChart;
import com.git.bs.ui.BsLineChart;
import com.git.bs.ui.BsPieChart;
import com.git.bs.ui.BsRadarChart;
import com.git.bs.ui.BsScatterChart;
import com.git.bs.ui.BsSplineChart;

import java.util.Arrays;
import java.util.List;

import static com.git.bs.demo.modules.ModuleSupport.*;

/**
 * 图表模块组：ChartsLine / ChartsBar / ChartsPie / ChartsLegend / ChartsHover / ChartsExtended。
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsChartModules {

    private final Skin skin;

    public BsChartModules(Skin skin) {
        this.skin = skin;
    }

    // ============================ Charts-Line ============================
    public void fillChartsLine(Table c) {
        c.add(sectionTitle(skin, "Charts-Line  —— 折线图（坐标轴 + 数据点 + 多系列）")).row();

        c.add(new Label("① 单系列折线（带 X/Y 坐标轴 + 网格 + 数据点）:", skin)).padTop(8).left().row();
        BsLineChart line1 = new BsLineChart();
        line1.setSize(640, 240);
        line1.setSkinFont(skin);
        line1.setShowPoints(true);
        line1.setLegendPlacement(BsChart.LegendPlacement.NONE);
        line1.setData(BsChart.pointsOfY(3, 5, 4, 8, 7, 10, 6, 9));
        c.add(wrapChart(line1, 640, 240)).padTop(4).row();

        c.add(new Label("② 多系列折线（销量 vs 库存，图例顶部）:", skin)).padTop(14).left().row();
        BsLineChart line2 = new BsLineChart();
        line2.setSize(640, 240);
        line2.setSkinFont(skin);
        line2.setLegendPlacement(BsChart.LegendPlacement.TOP);
        List<BsChart.Series> lineSeries2 = new java.util.ArrayList<>();
        lineSeries2.add(new BsChart.Series("销量", BsChart.pointsOfY(3, 5, 4, 8, 7, 10, 6)));
        lineSeries2.add(new BsChart.Series("库存", BsChart.pointsOfY(8, 7, 9, 5, 6, 4, 7)));
        lineSeries2.add(new BsChart.Series("目标", BsChart.pointsOfY(5, 6, 6, 7, 8, 8, 9)));
        line2.setMultiSeries(lineSeries2);
        c.add(wrapChart(line2, 640, 240)).padTop(4).row();

        c.add(new Label("③ X/Y 坐标可自定义（非等距）:", skin)).padTop(14).left().row();
        BsLineChart line3 = new BsLineChart();
        line3.setSize(640, 240);
        line3.setSkinFont(skin);
        line3.setLegendPlacement(BsChart.LegendPlacement.NONE);
        line3.setData(BsChart.points(0, 1, 10, 4, 20, 8, 40, 15, 80, 25));
        c.add(wrapChart(line3, 640, 240)).padTop(4).row();

        c.add(new Label("④ 无网格 + 无数据点（极简）:", skin)).padTop(14).left().row();
        BsLineChart line4 = new BsLineChart();
        line4.setSize(640, 180);
        line4.setSkinFont(skin);
        line4.setShowGrid(false);
        line4.setShowPoints(false);
        line4.setLineWidth(3f);
        line4.setLegendPlacement(BsChart.LegendPlacement.NONE);
        line4.setData(BsChart.pointsOfY(20, 35, 28, 42, 38, 55, 48, 60));
        c.add(wrapChart(line4, 640, 180)).padTop(4).row();

        c.add(new Label("(鼠标 hover 数据点会显示坐标数值；详见 Charts-Hover 模块)", skin)).padTop(8).row();
    }

    // ============================ Charts-Bar ============================
    public void fillChartsBar(Table c) {
        c.add(sectionTitle(skin, "Charts-Bar  —— 柱状图（垂直/水平 + 多系列分组）")).row();

        c.add(new Label("① 单系列柱状（季度销量）:", skin)).padTop(8).left().row();
        BsBarChart bar1 = new BsBarChart();
        bar1.setSize(640, 240);
        bar1.setSkinFont(skin);
        bar1.setCategories("Q1", "Q2", "Q3", "Q4");
        bar1.setMultiSeries(List.of(
                new BsChart.Series("销量", BsChart.pointsOfY(35, 48, 60, 72))
        ));
        bar1.setLegendPlacement(BsChart.LegendPlacement.NONE);
        c.add(wrapChart(bar1, 640, 240)).padTop(4).row();

        c.add(new Label("② 多系列分组（2024 vs 2025）:", skin)).padTop(14).left().row();
        BsBarChart bar2 = new BsBarChart();
        bar2.setSize(640, 240);
        bar2.setSkinFont(skin);
        bar2.setCategories("Q1", "Q2", "Q3", "Q4");
        bar2.setMultiSeries(Arrays.asList(
                new BsChart.Series("2024", BsChart.pointsOfY(35, 48, 60, 72)),
                new BsChart.Series("2025", BsChart.pointsOfY(45, 55, 68, 88))
        ));
        bar2.setLegendPlacement(BsChart.LegendPlacement.TOP);
        c.add(wrapChart(bar2, 640, 240)).padTop(4).row();

        c.add(new Label("③ 3 系列分组（地区对比）:", skin)).padTop(14).left().row();
        BsBarChart bar3 = new BsBarChart();
        bar3.setSize(640, 260);
        bar3.setSkinFont(skin);
        bar3.setCategories("北京", "上海", "广州", "深圳", "杭州");
        bar3.setMultiSeries(Arrays.asList(
                new BsChart.Series("男", BsChart.pointsOfY(120, 150, 100, 130, 90)),
                new BsChart.Series("女", BsChart.pointsOfY(110, 140, 95, 125, 85))
        ));
        bar3.setLegendPlacement(BsChart.LegendPlacement.TOP);
        c.add(wrapChart(bar3, 640, 260)).padTop(4).row();

        c.add(new Label("④ 水平柱状图（HORIZONTAL）:", skin)).padTop(14).left().row();
        BsBarChart bar4 = new BsBarChart();
        bar4.setSize(640, 240);
        bar4.setSkinFont(skin);
        bar4.setOrientation(BsBarChart.Orientation.HORIZONTAL);
        bar4.setCategories("A", "B", "C", "D", "E");
        bar4.setMultiSeries(List.of(
                new BsChart.Series("数量", BsChart.pointsOfY(20, 35, 50, 28, 42))
        ));
        bar4.setLegendPlacement(BsChart.LegendPlacement.NONE);
        c.add(wrapChart(bar4, 640, 240)).padTop(4).row();

        c.add(new Label("(鼠标 hover 柱子显示数值；图例可点击切换显示)", skin)).padTop(8).row();
    }

    // ============================ Charts-Pie ============================
    public void fillChartsPie(Table c) {
        c.add(sectionTitle(skin, "Charts-Pie  —— 饼图（扇形 + 环形 + 图例百分比）")).row();

        c.add(new Label("① 基础饼图 + 右侧图例（含百分比）:", skin)).padTop(8).left().row();
        BsPieChart pie1 = new BsPieChart();
        pie1.setSize(560, 280);
        pie1.setSkinFont(skin);
        pie1.setLegendPlacement(BsChart.LegendPlacement.RIGHT);
        pie1.setSlices(
                "Chrome", 65,
                "Firefox", 15,
                "Safari", 12,
                "Edge", 5,
                "Other", 3
        );
        c.add(wrapChart(pie1, 560, 280)).padTop(4).row();

        c.add(new Label("② 环形图（donutHole=0.55）:", skin)).padTop(14).left().row();
        BsPieChart donut = new BsPieChart();
        donut.setSize(360, 320);
        donut.setSkinFont(skin);
        donut.setDonutHole(0.55f);
        donut.setLegendPlacement(BsChart.LegendPlacement.RIGHT);
        donut.setSlices(
                "Chrome", 50,
                "Firefox", 25,
                "Safari", 15,
                "Edge", 10
        );
        c.add(wrapChart(donut, 360, 320)).padTop(4).row();

        c.add(new Label("③ 顶部图例饼图（横向布局）:", skin)).padTop(14).left().row();
        BsPieChart pie3 = new BsPieChart();
        pie3.setSize(560, 280);
        pie3.setSkinFont(skin);
        pie3.setLegendPlacement(BsChart.LegendPlacement.TOP);
        pie3.setSlices(
                "前端", 40,
                "后端", 35,
                "运维", 15,
                "测试", 10
        );
        c.add(wrapChart(pie3, 560, 280)).padTop(4).row();

        c.add(new Label("④ 无图例饼图:", skin)).padTop(14).left().row();
        BsPieChart pie4 = new BsPieChart();
        pie4.setSize(280, 280);
        pie4.setSkinFont(skin);
        pie4.setLegendPlacement(BsChart.LegendPlacement.NONE);
        pie4.setSlices(
                "A", 30,
                "B", 25,
                "C", 20,
                "D", 15,
                "E", 10
        );
        c.add(wrapChart(pie4, 280, 280)).padTop(4).row();

        c.add(new Label("(鼠标 hover 扇形会外推并显示百分比 tooltip；点击图例切换)", skin)).padTop(8).row();
    }

    // ============================ Charts-Legend ============================
    public void fillChartsLegend(Table c) {
        c.add(sectionTitle(skin, "Charts-Legend  —— 图例位置 / 点击切换 / 单击隔离")).row();

        c.add(new Label("① 图例位置对比（4 个折线图，分别 TOP/BOTTOM/LEFT/RIGHT）:", skin)).padTop(8).left().row();
        Table legendPosRow = new Table();
        legendPosRow.defaults().pad(6);
        legendPosRow.add(makeLineWithLegend("图例=TOP", BsChart.LegendPlacement.TOP)).size(320, 200);
        legendPosRow.add(makeLineWithLegend("图例=BOTTOM", BsChart.LegendPlacement.BOTTOM)).size(320, 200);
        legendPosRow.row();
        legendPosRow.add(makeLineWithLegend("图例=LEFT", BsChart.LegendPlacement.LEFT)).size(320, 200);
        legendPosRow.add(makeLineWithLegend("图例=RIGHT", BsChart.LegendPlacement.RIGHT)).size(320, 200);
        c.add(legendPosRow).row();

        c.add(new Label("② 点击图例切换系列显隐（点击图中顶部图例条目，对应系列会隐藏/恢复）:",
                skin)).padTop(14).left().row();
        BsLineChart toggleChart = new BsLineChart();
        toggleChart.setSize(640, 260);
        toggleChart.setSkinFont(skin);
        toggleChart.setLegendPlacement(BsChart.LegendPlacement.TOP);
        toggleChart.setMultiSeries(Arrays.asList(
                new BsChart.Series("CPU", BsChart.pointsOfY(40, 55, 50, 65, 70, 60, 75)),
                new BsChart.Series("内存", BsChart.pointsOfY(30, 35, 40, 38, 45, 50, 48)),
                new BsChart.Series("磁盘", BsChart.pointsOfY(20, 25, 30, 28, 35, 40, 42)),
                new BsChart.Series("网络", BsChart.pointsOfY(10, 20, 15, 25, 30, 28, 35))
        ));
        c.add(wrapChart(toggleChart, 640, 260)).padTop(4).row();

        c.add(new Label("③ 点击隔离（点击图中数据点 → 只显示该系列；Shift+点击 → 多选对比）:",
                skin)).padTop(14).left().row();
        BsLineChart isoChart = new BsLineChart();
        isoChart.setSize(640, 260);
        isoChart.setSkinFont(skin);
        isoChart.setClickToIsolate(true);
        isoChart.setLegendPlacement(BsChart.LegendPlacement.TOP);
        isoChart.setMultiSeries(Arrays.asList(
                new BsChart.Series("北京", BsChart.pointsOfY(15, 22, 28, 35, 42, 38, 45)),
                new BsChart.Series("上海", BsChart.pointsOfY(20, 28, 35, 42, 50, 48, 55)),
                new BsChart.Series("广州", BsChart.pointsOfY(25, 32, 38, 45, 52, 55, 60)),
                new BsChart.Series("深圳", BsChart.pointsOfY(18, 25, 30, 38, 44, 42, 50))
        ));
        c.add(wrapChart(isoChart, 640, 260)).padTop(4).row();

        c.add(new Label("(③ 中按住 Shift 键再点数据点可保留多条做对比；只点一下其他都隐藏)",
                skin)).padTop(8).row();
    }

    private BsLineChart makeLineWithLegend(String label, BsChart.LegendPlacement placement) {
        BsLineChart chart = new BsLineChart();
        chart.setSize(320, 200);
        chart.setSkinFont(skin);
        chart.setLegendPlacement(placement);
        chart.setMultiSeries(Arrays.asList(
                new BsChart.Series("A", BsChart.pointsOfY(3, 5, 4, 8, 7)),
                new BsChart.Series("B", BsChart.pointsOfY(1, 4, 6, 5, 7))
        ));
        return chart;
    }

    // ============================ Charts-Hover ============================
    public void fillChartsHover(Table c) {
        c.add(sectionTitle(skin, "Charts-Hover  —— 鼠标 hover 查看坐标/数值/百分比")).row();

        c.add(new Label("① 折线图 Hover（移动鼠标到数据点附近，显示坐标）:", skin)).padTop(8).left().row();
        BsLineChart line = new BsLineChart();
        line.setSize(640, 260);
        line.setSkinFont(skin);
        line.setHoverEnabled(true);
        line.setHitRadius(20);
        line.setLegendPlacement(BsChart.LegendPlacement.TOP);
        line.setMultiSeries(Arrays.asList(
                new BsChart.Series("玩家在线", BsChart.pointsOfY(120, 180, 240, 280, 320, 380, 420, 480, 460, 500)),
                new BsChart.Series("同时在线峰值", BsChart.pointsOfY(80, 130, 180, 220, 260, 320, 360, 400, 390, 430))
        ));
        c.add(wrapChart(line, 640, 260)).padTop(4).row();

        c.add(new Label("② 柱状图 Hover（移动鼠标到柱子上，显示数值）:", skin)).padTop(14).left().row();
        BsBarChart bar = new BsBarChart();
        bar.setSize(640, 260);
        bar.setSkinFont(skin);
        bar.setHoverEnabled(true);
        bar.setLegendPlacement(BsChart.LegendPlacement.TOP);
        bar.setCategories("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        bar.setMultiSeries(Arrays.asList(
                new BsChart.Series("本周", BsChart.pointsOfY(120, 140, 135, 160, 180, 220, 200)),
                new BsChart.Series("上周", BsChart.pointsOfY(110, 130, 128, 155, 170, 210, 195))
        ));
        c.add(wrapChart(bar, 640, 260)).padTop(4).row();

        c.add(new Label("③ 饼图 Hover（扇形外推 + 显示百分比）:", skin)).padTop(14).left().row();
        BsPieChart pie = new BsPieChart();
        pie.setSize(560, 320);
        pie.setSkinFont(skin);
        pie.setHoverEnabled(true);
        pie.setLegendPlacement(BsChart.LegendPlacement.RIGHT);
        pie.setSlices(
                "学生", 1200,
                "教师", 350,
                "职工", 280,
                "访客", 480,
                "其他", 150
        );
        c.add(wrapChart(pie, 560, 320)).padTop(4).row();

        c.add(new Label("④ 关闭 Hover（对比效果）:", skin)).padTop(14).left().row();
        BsLineChart lineNoHover = new BsLineChart();
        lineNoHover.setSize(640, 200);
        lineNoHover.setSkinFont(skin);
        lineNoHover.setHoverEnabled(false);
        lineNoHover.setLegendPlacement(BsChart.LegendPlacement.TOP);
        lineNoHover.setMultiSeries(Arrays.asList(
                new BsChart.Series("数据 A", BsChart.pointsOfY(5, 8, 6, 12, 9, 15, 11)),
                new BsChart.Series("数据 B", BsChart.pointsOfY(3, 5, 7, 9, 8, 11, 13))
        ));
        c.add(wrapChart(lineNoHover, 640, 200)).padTop(4).row();

        c.add(new Label("(hover 命中半径可调：折线 setHitRadius，默认 12px)", skin)).padTop(8).row();
    }

    // ============================ Charts-Bar3D ============================
    public void fillChartsBar3D(Table c) {
        c.add(sectionTitle(skin, "Charts-Bar3D  —— 真 3D 柱状图（等距投影 + 三面明暗 + 可旋转）")).row();

        c.add(new Label("① 单系列 3D 柱状（季度销量，顶亮/正面中/侧面暗）:", skin)).padTop(8).left().row();
        BsBarChart3D bar1 = new BsBarChart3D();
        bar1.setSize(640, 260);
        bar1.setSkinFont(skin);
        bar1.setCategories("Q1", "Q2", "Q3", "Q4");
        bar1.setMultiSeries(List.of(
                new BsChart.Series("销量", BsChart.pointsOfY(35, 48, 60, 72))
        ));
        bar1.setLegendPlacement(BsChart.LegendPlacement.NONE);
        c.add(wrapChart(bar1, 640, 260)).padTop(4).row();

        c.add(new Label("② 多系列分组 3D（2024 vs 2025）:", skin)).padTop(14).left().row();
        BsBarChart3D bar2 = new BsBarChart3D();
        bar2.setSize(640, 260);
        bar2.setSkinFont(skin);
        bar2.setCategories("Q1", "Q2", "Q3", "Q4");
        bar2.setMultiSeries(Arrays.asList(
                new BsChart.Series("2024", BsChart.pointsOfY(35, 48, 60, 72)),
                new BsChart.Series("2025", BsChart.pointsOfY(45, 55, 68, 88))
        ));
        bar2.setLegendPlacement(BsChart.LegendPlacement.TOP);
        c.add(wrapChart(bar2, 640, 260)).padTop(4).row();

        c.add(new Label("③ 拖拽旋转视角（按住鼠标拖动可绕 Y 轴旋转）:", skin)).padTop(14).left().row();
        final BsBarChart3D bar3 = new BsBarChart3D();
        bar3.setSize(640, 280);
        bar3.setSkinFont(skin);
        bar3.setCategories("北京", "上海", "广州", "深圳", "杭州");
        bar3.setMultiSeries(Arrays.asList(
                new BsChart.Series("男", BsChart.pointsOfY(120, 150, 100, 130, 90)),
                new BsChart.Series("女", BsChart.pointsOfY(110, 140, 95, 125, 85))
        ));
        bar3.setLegendPlacement(BsChart.LegendPlacement.TOP);
        attachDragRotate(bar3);
        c.add(wrapChart(bar3, 640, 280)).padTop(4).row();

        c.add(new Label("④ 加大柱深 + 俯仰角（更夸张的立体感）:", skin)).padTop(14).left().row();
        BsBarChart3D bar4 = new BsBarChart3D();
        bar4.setSize(640, 260);
        bar4.setSkinFont(skin);
        bar4.setBarDepth(56);
        bar4.setPitchDegrees(28);
        bar4.setYawDegrees(45);
        bar4.setCategories("A", "B", "C", "D", "E");
        bar4.setMultiSeries(List.of(
                new BsChart.Series("数量", BsChart.pointsOfY(20, 35, 50, 28, 42))
        ));
        bar4.setLegendPlacement(BsChart.LegendPlacement.NONE);
        c.add(wrapChart(bar4, 640, 260)).padTop(4).row();

        c.add(new Label("(鼠标 hover 柱子显示数值；③ 中拖拽可旋转 yaw；点击图例切换系列显隐)", skin)).padTop(8).row();
    }

    /** 给 3D 柱状图绑定拖拽旋转：按住鼠标左右拖动改变 yawDeg。 */
    private void attachDragRotate(BsBarChart3D chart) {
        final float[] dragStart = {-1};
        final float[] yawStart = {0};
        chart.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                     float x, float y, int pointer, int button) {
                dragStart[0] = event.getStageX();
                yawStart[0] = chart.getYawDegrees();
                return true;
            }
            @Override
            public void touchDragged(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                     float x, float y, int pointer) {
                if (dragStart[0] < 0) return;
                float dx = event.getStageX() - dragStart[0];
                // 拖动 2px ≈ 1°，规整到 [0, 360)
                float yaw = (yawStart[0] + dx * 0.5f) % 360f;
                if (yaw < 0) yaw += 360f;
                chart.setYawDegrees(yaw);
            }
            @Override
            public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y, int pointer, int button) {
                dragStart[0] = -1;
            }
        });
    }

    // ============================ Charts-Extended ============================
    public void fillChartsExtended(Table c) {
        c.add(sectionTitle(skin, "Charts-Extended  —— 面积/曲线/散点/雷达/环形")).row();

        // AreaChart
        c.add(new Label("① BsAreaChart 面积图（折线下方填充半透明色）:", skin)).padTop(8).left().row();
        BsAreaChart area = new BsAreaChart();
        area.setSize(640, 220);
        area.setSkinFont(skin);
        area.setLegendPlacement(BsChart.LegendPlacement.TOP);
        area.setMultiSeries(Arrays.asList(
                new BsChart.Series("访问量", BsChart.pointsOfY(20, 35, 40, 55, 70, 85, 90, 75, 88, 95)),
                new BsChart.Series("独立访客", BsChart.pointsOfY(10, 18, 22, 30, 40, 50, 60, 55, 62, 70))
        ));
        c.add(wrapChart(area, 640, 220)).padTop(4).row();

        // SplineChart
        c.add(new Label("② BsSplineChart 平滑曲线（Catmull-Rom 插值）:", skin)).padTop(14).left().row();
        BsSplineChart spline = new BsSplineChart();
        spline.setSize(640, 220);
        spline.setSkinFont(skin);
        spline.setLegendPlacement(BsChart.LegendPlacement.TOP);
        spline.setMultiSeries(Arrays.asList(
                new BsChart.Series("用户增长", BsChart.pointsOfY(5, 12, 25, 38, 50, 65, 88, 110, 135, 160)),
                new BsChart.Series("预期目标", BsChart.pointsOfY(10, 18, 28, 38, 50, 62, 75, 90, 105, 120))
        ));
        c.add(wrapChart(spline, 640, 220)).padTop(4).row();

        // ScatterChart
        c.add(new Label("③ BsScatterChart 散点图（身高/体重分布）:", skin)).padTop(14).left().row();
        BsScatterChart scatter = new BsScatterChart();
        scatter.setSize(640, 280);
        scatter.setSkinFont(skin);
        scatter.setPointRadius(5);
        scatter.setLegendPlacement(BsChart.LegendPlacement.TOP);
        scatter.setMultiSeries(Arrays.asList(
                new BsChart.Series("男", BsChart.points(
                        160, 55, 165, 60, 170, 65, 175, 70, 180, 75, 178, 78, 172, 68, 182, 80,
                        168, 58, 185, 82, 176, 72)),
                new BsChart.Series("女", BsChart.points(
                        150, 45, 155, 50, 158, 52, 162, 55, 168, 60, 165, 58, 170, 62, 160, 52,
                        170, 65, 155, 48, 162, 56))
        ));
        c.add(wrapChart(scatter, 640, 280)).padTop(4).row();

        // RadarChart
        c.add(new Label("④ BsRadarChart 雷达图（角色属性对比）:", skin)).padTop(14).left().row();
        Table radarRow = new Table();
        radarRow.defaults().pad(10);

        BsRadarChart radar1 = new BsRadarChart();
        radar1.setSize(320, 320);
        radar1.setSkinFont(skin);
        radar1.setMaxValue(100);
        radar1.setAxes("攻击", "防御", "速度", "智力", "运气");
        radar1.setLegendPlacement(BsChart.LegendPlacement.TOP);
        radar1.setMultiSeries(Arrays.asList(
                new BsChart.Series("战士", BsChart.pointsOfY(85, 90, 40, 50, 60)),
                new BsChart.Series("法师", BsChart.pointsOfY(30, 40, 60, 95, 70))
        ));
        radarRow.add(wrapChart(radar1, 320, 320));

        BsRadarChart radar2 = new BsRadarChart();
        radar2.setSize(320, 320);
        radar2.setSkinFont(skin);
        radar2.setMaxValue(100);
        radar2.setAxes("语数", "英语", "物理", "化学", "生物", "历史");
        radar2.setLegendPlacement(BsChart.LegendPlacement.TOP);
        radar2.setMultiSeries(Arrays.asList(
                new BsChart.Series("学生 A", BsChart.pointsOfY(90, 85, 75, 80, 70, 95)),
                new BsChart.Series("学生 B", BsChart.pointsOfY(70, 60, 95, 85, 80, 65))
        ));
        radarRow.add(wrapChart(radar2, 320, 320));
        c.add(radarRow).padTop(4).row();

        // DoughnutChart
        c.add(new Label("⑤ BsDoughnutChart 环形图（中心显示总值）:", skin)).padTop(14).left().row();
        BsDoughnutChart donut = new BsDoughnutChart();
        donut.setSize(560, 320);
        donut.setSkinFont(skin);
        donut.setLegendPlacement(BsChart.LegendPlacement.RIGHT);
        donut.setCenterLabel("总员工数", "2460");
        donut.setSlices(
                "研发", 1200,
                "销售", 600,
                "运营", 350,
                "市场", 180,
                "管理", 130
        );
        c.add(wrapChart(donut, 560, 320)).padTop(4).row();

        c.add(new Label("(⑤ 中环形图中心绘制了「总员工数 2460」，便于一眼看总数)", skin)).padTop(8).row();
    }
}
