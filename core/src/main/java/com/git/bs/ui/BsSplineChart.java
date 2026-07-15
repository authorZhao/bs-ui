/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库。
 * Copyright (c) 2026 bs-ui contributors
 *
 * 基于 Apache License 2.0 开源，允许商用、修改和再分发。
 * 使用本库的产品须在“关于”界面标注本项目，详见 LICENSE。
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap 风格平滑曲线图（SplineChart）—— 用 Catmull-Rom 插值把折线变成平滑曲线。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsSplineChart chart = new BsSplineChart();
 * chart.setSize(480, 240);
 * chart.setSkinFont(skin);
 * chart.setMultiSeries(Arrays.asList(
 *     new BsChart.Series("用户增长", BsChart.pointsOfY(5, 12, 25, 38, 50, 65, 88))
 * ));
 * stage.addActor(chart);
 * }</pre>
 *
 * <p>实现：把相邻两点之间用 Catmull-Rom 算法插值出 N 个细分点（默认 16 段），
 * 然后用 line 连接。Catmull-Rom 在所有点处平滑过渡且经过原始数据点。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsSplineChart extends BsLineChart {

    /** 每段插值的细分数（越大越平滑，性能略降）。 */
    private int segments = 16;

    public BsSplineChart setSegments(int n) { this.segments = Math.max(2, n); return this; }

    @Override
    protected void drawChart(ShapeRenderer sr) {
        for (int idx = 0; idx < seriesList.size(); idx++) {
            if (hidden.get(idx)) continue;
            Series s = seriesList.get(idx);
            if (s.points.size() < 2) continue;
            Color c = s.color != null ? s.color : Color.GRAY;

            // 1. 用 Catmull-Rom 插值生成平滑点序列
            List<float[]> smooth = catmullRom(s.points, segments);
            // 2. 逐段连线
            sr.setColor(c);
            for (int i = 1; i < smooth.size(); i++) {
                float[] a = smooth.get(i - 1);
                float[] b = smooth.get(i);
                rectLine(sr, a[0], a[1], b[0], b[1], lineWidth);
            }
            // 3. 数据点
            if (showPoints) {
                for (Point p : s.points) {
                    float sx = toScreenX(p.x);
                    float sy = toScreenY(p.y);
                    sr.setColor(c);
                    sr.circle(sx, sy, pointRadius + 1);
                    sr.setColor(Color.WHITE);
                    sr.circle(sx, sy, pointRadius - 1);
                }
            }
        }
    }

    /**
     * Catmull-Rom 样条插值：对 N 个原始点生成 (N-1)*segments 个细分点。
     * 端点处用 ghost point（镜像延伸）保证平滑。
     */
    private List<float[]> catmullRom(List<Point> pts, int seg) {
        List<float[]> out = new ArrayList<>((pts.size() - 1) * seg + 1);
        int n = pts.size();
        // 把原始点先转到屏幕坐标
        float[][] sp = new float[n][2];
        for (int i = 0; i < n; i++) {
            sp[i][0] = toScreenX(pts.get(i).x);
            sp[i][1] = toScreenY(pts.get(i).y);
        }
        // ghost 点：首尾各延伸一个（线性外推）
        float[] p0 = (n >= 2) ? new float[]{2 * sp[0][0] - sp[1][0], 2 * sp[0][1] - sp[1][1]} : sp[0];
        float[] pN = (n >= 2) ? new float[]{2 * sp[n - 1][0] - sp[n - 2][0], 2 * sp[n - 1][1] - sp[n - 2][1]} : sp[n - 1];

        for (int i = 0; i < n - 1; i++) {
            float[] prev = (i == 0) ? p0 : sp[i - 1];
            float[] cur = sp[i];
            float[] next = sp[i + 1];
            float[] next2 = (i + 2 < n) ? sp[i + 2] : pN;
            for (int t = 0; t < seg; t++) {
                float u = t / (float) seg;
                out.add(catmullRomPoint(prev, cur, next, next2, u));
            }
        }
        out.add(new float[]{sp[n - 1][0], sp[n - 1][1]});
        return out;
    }

    /** Catmull-Rom 插值单点（u 在 [0,1]）。 */
    private float[] catmullRomPoint(float[] p0, float[] p1, float[] p2, float[] p3, float u) {
        float u2 = u * u;
        float u3 = u2 * u;
        float x = 0.5f * ((2 * p1[0])
                + (-p0[0] + p2[0]) * u
                + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * u2
                + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * u3);
        float y = 0.5f * ((2 * p1[1])
                + (-p0[1] + p2[1]) * u
                + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * u2
                + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * u3);
        return new float[]{x, y};
    }
}
