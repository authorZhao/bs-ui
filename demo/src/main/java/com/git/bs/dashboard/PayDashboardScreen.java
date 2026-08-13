package com.git.bs.dashboard;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.git.bs.ui.BsBarChart;
import com.git.bs.ui.BsChart;
import com.git.bs.ui.BsLineChart;
import com.git.bs.ui.BsPieChart;
import com.git.bs.ui.BsScrollPane;
import com.git.bs.ui.BsText;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 支付订单分析大屏 —— 读取 {@code csv/pay_record.csv}，展示三张图表（上下排列）：
 *
 * <ol>
 *   <li><b>折线图</b>：每日 PAID / CLOSED 数量趋势</li>
 *   <li><b>柱状图</b>：按月汇总 PAID / CLOSED 总量</li>
 *   <li><b>饼状图</b>：PAID / CLOSED / PENDING 总量占比</li>
 * </ol>
 *
 * <p>所有图表开启 hover tooltip，整体垂直滚动；标题用运行时生成的 48px CJK 字体。</p>
 *
 * @author authorZhao
 * @since 2026-08-13
 */
@Slf4j
public class PayDashboardScreen implements Screen {

    private Stage stage;

    // ============================= 生命周期 =============================

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        Skin skin = BsUI.getSkin();

        // 解析 CSV
        PayData data = PayData.load("csv/pay_record.csv");
        if (data.dates.isEmpty()) {
            log.error("CSV 无数据，大屏将为空");
        }

        Table root = new Table();
        root.setFillParent(true);
        root.top().left();
        root.pad(16);
        root.defaults().top().left();

        // 标题
        root.add(buildTitle(skin)).growX().padBottom(16).row();

        // 图表列（可滚动）
        Table chartsCol = new Table();
        chartsCol.top().left();
        chartsCol.defaults().growX().left();

        // 1) 折线图
        chartsCol.add(sectionTitle("每日趋势（折线图）")).padBottom(6).row();
        chartsCol.add(wrapCard(skin, buildLineChart(skin, data)))
                .height(380).padBottom(20).row();

        // 2) 柱状图
        chartsCol.add(sectionTitle("月度汇总（柱状图）")).padBottom(6).row();
        chartsCol.add(wrapCard(skin, buildBarChart(skin, data)))
                .height(380).padBottom(20).row();

        // 3) 饼状图
        chartsCol.add(sectionTitle("状态占比（饼状图）")).padBottom(6).row();
        chartsCol.add(wrapCard(skin, buildPieChart(skin, data)))
                .height(380).padBottom(20).row();

        root.add(makeScroll(skin, chartsCol)).grow().row();
        stage.addActor(root);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(BsTheme.bgBodyColor(), true);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { if (stage != null) stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { if (stage != null) stage.dispose(); }

    // ============================= 标题 =============================

    private Actor buildTitle(Skin skin) {
        BitmapFont tf = skin.has("font-pay-title", BitmapFont.class)
                ? skin.get("font-pay-title", BitmapFont.class)
                : safeFont(skin, "font-xl");
        Label.LabelStyle style = new Label.LabelStyle(tf, BsTheme.tp());
        Label title = new Label("支付订单分析图", style);
        title.setAlignment(Align.center);
        Table t = new Table();
        t.add(title).expandX().center();
        return t;
    }

    private static BitmapFont safeFont(Skin skin, String key) {
        try {
            if (skin.has(key, BitmapFont.class)) return skin.get(key, BitmapFont.class);
        } catch (Throwable ignored) {}
        return skin.getFont("default");
    }

    // ============================= 图表构建 =============================

    /** 折线图：每日 PAID / CLOSED 趋势。 */
    private Actor buildLineChart(Skin skin, PayData data) {
        String[] dateLabels = new String[data.dates.size()];
        for (int i = 0; i < data.dates.size(); i++) {
            dateLabels[i] = formatDateLabel(data.dates.get(i));
        }

        List<BsChart.Point> paid = new ArrayList<>();
        List<BsChart.Point> closed = new ArrayList<>();
        for (int i = 0; i < data.dates.size(); i++) {
            String dt = data.dates.get(i);
            paid.add(new BsChart.Point(i, data.dailyVal(dt, "PAID")));
            closed.add(new BsChart.Point(i, data.dailyVal(dt, "CLOSED")));
        }

        DateLineChart chart = new DateLineChart();
        chart.setSkinFont(skin);
        chart.setDateLabels(dateLabels);
        chart.setLegendPlacement(BsChart.LegendPlacement.TOP);
        chart.setLegendVisible(true);
        chart.setHoverEnabled(true);
        chart.setHitRadius(15);
        chart.setShowPoints(true);
        chart.setPointRadius(2.5f);
        chart.setLineWidth(2.5f);
        chart.setXTickCount(4);
        chart.setMultiSeries(Arrays.asList(
                new BsChart.Series("已支付", paid, BsTheme.colorOf("success")),
                new BsChart.Series("已关闭", closed, BsTheme.colorOf("danger"))
        ));
        return chart;
    }

    /** 柱状图：按月汇总 PAID / CLOSED。 */
    private Actor buildBarChart(Skin skin, PayData data) {
        String[] cats = new String[data.months.size()];
        for (int i = 0; i < data.months.size(); i++) {
            cats[i] = formatMonthLabel(data.months.get(i));
        }

        List<BsChart.Point> paid = new ArrayList<>();
        List<BsChart.Point> closed = new ArrayList<>();
        for (String month : data.months) {
            paid.add(new BsChart.Point(paid.size(), data.monthlyVal(month, "PAID")));
            closed.add(new BsChart.Point(closed.size(), data.monthlyVal(month, "CLOSED")));
        }

        BsBarChart chart = new BsBarChart();
        chart.setSkinFont(skin);
        chart.setOrientation(BsBarChart.Orientation.VERTICAL);
        chart.setCategories(cats);
        chart.setLegendPlacement(BsChart.LegendPlacement.TOP);
        chart.setLegendVisible(true);
        chart.setHoverEnabled(true);
        chart.setMultiSeries(Arrays.asList(
                new BsChart.Series("已支付", paid, BsTheme.colorOf("success")),
                new BsChart.Series("已关闭", closed, BsTheme.colorOf("danger"))
        ));
        return chart;
    }

    /** 饼状图：PAID / CLOSED / PENDING 总量占比。 */
    private Actor buildPieChart(Skin skin, PayData data) {
        BsPieChart chart = new BsPieChart();
        chart.setSkinFont(skin);
        chart.setLegendPlacement(BsChart.LegendPlacement.RIGHT);
        chart.setLegendVisible(true);
        chart.setHoverEnabled(true);
        chart.setSlices(
                "已支付", data.total("PAID"),
                "已关闭", data.total("CLOSED"),
                "待支付", data.total("PENDING")
        );
        // 语义化配色：PAID→绿, CLOSED→红, PENDING→黄
        List<BsPieChart.Slice> slices = chart.getSlices();
        if (slices.size() >= 3) {
            slices.get(0).color = BsTheme.colorOf("success");
            slices.get(1).color = BsTheme.colorOf("danger");
            slices.get(2).color = BsTheme.colorOf("warning");
        }
        return chart;
    }

    // ============================= 辅助 =============================

    private BsText sectionTitle(String t) {
        return new BsText(t, BsText.Size.LG, BsText.Variant.PRIMARY).bold();
    }

    private Table wrapCard(Skin skin, Actor body) {
        Table card = new Table();
        card.setBackground(skin.getDrawable("bs-window-bg"));
        card.pad(8);
        card.add(body).grow();
        return card;
    }

    private BsScrollPane makeScroll(Skin skin, Actor content) {
        Table holder = new Table();
        holder.top().left();
        holder.add(content).growX().top().left();
        BsScrollPane sp = new BsScrollPane(holder, skin);
        sp.setScrollingDisabled(true, false);
        sp.setFadeScrollBars(false);
        sp.setForceScroll(false, true);
        sp.setScrollbarsOnTop(false);
        return sp;
    }

    /** "2026-04-15" → "04/15" */
    private static String formatDateLabel(String dt) {
        return dt.length() >= 10 ? dt.substring(5, 10).replace("-", "/") : dt;
    }

    /** "2026-04" → "4月" */
    private static String formatMonthLabel(String month) {
        try {
            return Integer.parseInt(month.substring(5)) + "月";
        } catch (Throwable e) {
            return month;
        }
    }

    // ============================= DateLineChart =============================

    /**
     * 折线图子类：X 轴显示日期标签（而非数字索引），tooltip 也展示日期。
     * <p>其余行为（hover 高亮、图例、网格）完全继承 {@link BsLineChart}。</p>
     */
    private static class DateLineChart extends BsLineChart {
        private String[] dateLabels;

        public DateLineChart setDateLabels(String[] labels) {
            this.dateLabels = labels;
            return this;
        }

        @Override
        protected void drawAxisLabels(Batch batch, float parentAlpha) {
            if (!axesVisible) return;
            float oldColor = packColor(font.getColor());
            font.setColor(textColor.r, textColor.g, textColor.b, textColor.a * parentAlpha);

            float plotH = getHeight() - padTop - padBottom - legendPadTop - legendPadBottom;
            float baseY = padBottom + legendPadBottom;

            // Y 轴刻度（数值，与基类一致）
            for (int i = 0; i <= yTickCount; i++) {
                float v = minY + (maxY - minY) * i / (float) yTickCount;
                float sy = baseY + plotH * i / (float) yTickCount;
                String text = fmt(v);
                glyphLayout.setText(font, text);
                font.draw(batch, text,
                        getX() + padLeft + legendPadLeft - glyphLayout.width - 6,
                        getY() + sy + glyphLayout.height / 2f);
            }

            // X 轴刻度（日期标签替代数字）
            float plotW = getWidth() - padLeft - padRight - legendPadLeft - legendPadRight;
            for (int i = 0; i <= xTickCount; i++) {
                float sx = padLeft + legendPadLeft + plotW * i / (float) xTickCount;
                int dataIdx = Math.round(minX + (maxX - minX) * i / (float) xTickCount);
                String text;
                if (dateLabels != null && dataIdx >= 0 && dataIdx < dateLabels.length) {
                    text = dateLabels[dataIdx];
                } else {
                    text = fmt(minX + (maxX - minX) * i / (float) xTickCount);
                }
                glyphLayout.setText(font, text);
                font.draw(batch, text,
                        getX() + sx - glyphLayout.width / 2f,
                        getY() + baseY - 6);
            }

            font.setColor(unpackColor(oldColor));
        }

        @Override
        protected void drawTooltip(Batch batch, float parentAlpha) {
            if (hoverSeriesIdx < 0 || hoverPointIdx < 0) return;
            if (hoverSeriesIdx >= seriesList.size()) return;
            BsChart.Series s = seriesList.get(hoverSeriesIdx);
            if (hoverPointIdx >= s.points.size()) return;
            BsChart.Point p = s.points.get(hoverPointIdx);
            String line1 = (s.label == null ? "系列" + (hoverSeriesIdx + 1) : s.label);
            String dateStr = (dateLabels != null && hoverPointIdx < dateLabels.length)
                    ? dateLabels[hoverPointIdx] : fmt(p.x);
            String line2 = dateStr + "  " + fmt(p.y);
            drawTooltipBox(batch, parentAlpha,
                    new String[]{line1, line2},
                    s.color != null ? s.color : Color.GRAY);
        }
    }

    // ============================= CSV 数据模型 =============================

    /**
     * 解析 pay_record.csv，提供按日 / 按月 / 总计三种聚合视图。
     *
     * <p>CSV 格式：{@code "dt","status","status_desc","cnt"}</p>
     */
    private static class PayData {
        final List<String> dates = new ArrayList<>();
        final List<String> months = new ArrayList<>();
        /** date → (statusDesc → cnt) */
        final Map<String, Map<String, Float>> daily = new LinkedHashMap<>();
        /** month → (statusDesc → total) */
        final Map<String, Map<String, Float>> monthly = new LinkedHashMap<>();
        /** statusDesc → grand total */
        final Map<String, Float> totals = new LinkedHashMap<>();

        private PayData() {}

        float dailyVal(String date, String status) {
            Map<String, Float> m = daily.get(date);
            return m != null ? m.getOrDefault(status, 0f) : 0f;
        }

        float monthlyVal(String month, String status) {
            Map<String, Float> m = monthly.get(month);
            return m != null ? m.getOrDefault(status, 0f) : 0f;
        }

        float total(String status) {
            return totals.getOrDefault(status, 0f);
        }

        static PayData load(String path) {
            PayData d = new PayData();
            try {
                if (!Gdx.files.internal(path).exists()) {
                    log.error("CSV 文件不存在: {}", path);
                    return d;
                }
                String content = Gdx.files.internal(path).readString("UTF-8");
                String[] lines = content.split("\n");
                Set<String> dateSet = new LinkedHashSet<>();
                Set<String> monthSet = new LinkedHashSet<>();

                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length < 4) continue;
                    String dt = unquote(parts[0]);
                    String statusDesc = unquote(parts[2]);
                    float cnt;
                    try {
                        cnt = Float.parseFloat(unquote(parts[3]));
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    dateSet.add(dt);
                    String month = dt.length() >= 7 ? dt.substring(0, 7) : dt;
                    monthSet.add(month);

                    d.daily.computeIfAbsent(dt, k -> new LinkedHashMap<>()).put(statusDesc, cnt);
                    d.monthly.computeIfAbsent(month, k -> new LinkedHashMap<>())
                            .merge(statusDesc, cnt, Float::sum);
                    d.totals.merge(statusDesc, cnt, Float::sum);
                }

                d.dates.addAll(dateSet);
                Collections.sort(d.dates);
                d.months.addAll(monthSet);
                Collections.sort(d.months);
            } catch (Throwable e) {
                log.error("解析 CSV 失败: {}", e.getMessage(), e);
            }
            return d;
        }

        /** 去掉 CSV 字段两端的引号。 */
        private static String unquote(String s) {
            s = s.trim();
            if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                return s.substring(1, s.length() - 1);
            }
            return s;
        }
    }
}
