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

import com.badlogic.gdx.utils.FloatArray;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * 运维监控模拟数据源 —— 纯内存生成，覆盖大屏全部模块。
 *
 * <p>每秒 {@link #tick()} 推进一步，用「均值回归 + 周期 + 噪声 + 偶发事件」生成：</p>
 * <ul>
 *   <li>核心指标：CPU / 内存 / 网络(netIn+netOut) / QPS / 错误率 + 时序历史</li>
 *   <li>JVM：堆 / 非堆 / GC 累计 / 线程数 + 堆时序</li>
 *   <li>业务：活跃用户</li>
 *   <li>节点：6 个服务节点的状态/CPU/内存/QPS（偶发离线）</li>
 *   <li>访问日志：每秒 push 1-2 条（IP/方法/路径/状态码/耗时，状态码按真实分布）</li>
 *   <li>告警：CPU>85 / 节点离线 / 5xx 触发</li>
 *   <li>机房环境：温度 / 湿度（缓慢漂移）</li>
 * </ul>
 * @author authorZhao
 * @since 2026-07-16
 */
public class MockMonitorDataSource {

    public static final int HISTORY = 60;
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] METHODS = {"GET", "POST", "PUT", "DELETE"};
    private static final String[] PATHS = {
            "/api/user/list", "/api/order/create", "/api/login", "/api/product/12",
            "/api/cart", "/api/payment", "/api/search", "/api/export/report"
    };

    // —— 核心瞬时值 ——
    public float cpu, mem, netIn, netOut, qps, err, activeUsers;
    public float prevCpu, prevMem, prevQps;
    public final FloatArray cpuHistory = new FloatArray(HISTORY);
    public final FloatArray memHistory = new FloatArray(HISTORY);
    public final FloatArray netInHistory = new FloatArray(HISTORY);
    public final FloatArray netOutHistory = new FloatArray(HISTORY);

    // —— JVM ——
    public float heap, nonHeap;     // 0~100 占比
    public int gc, threads;
    public final FloatArray heapHistory = new FloatArray(HISTORY);

    // —— 机房环境 ——
    public float temp = 23f, humidity = 46f;

    // —— 节点 ——
    public final List<NodeStatus> nodes = new ArrayList<>();

    // —— 日志（最近 30 条，新条目放头部） ——
    public final Deque<LogEntry> logs = new ArrayDeque<>();

    // —— 告警（最近 8 条） ——
    public final Deque<Alert> alerts = new ArrayDeque<>();

    private float t = 0;
    private float spikeCpu = 0;
    private final Random rnd = new Random();

    public interface Listener { void onTick(); }
    private final List<Listener> listeners = new ArrayList<>();
    public void addListener(Listener l) { listeners.add(l); }

    public MockMonitorDataSource() {
        nodes.add(new NodeStatus("web-01",   "Web"));
        nodes.add(new NodeStatus("web-02",   "Web"));
        nodes.add(new NodeStatus("app-01",   "App"));
        nodes.add(new NodeStatus("gateway",  "GW"));
        nodes.add(new NodeStatus("db-01",    "DB"));
        nodes.add(new NodeStatus("redis",    "Cache"));
        for (NodeStatus n : nodes) { n.cpu = 30 + rnd.nextFloat() * 30; n.mem = 40 + rnd.nextFloat() * 30; n.qps = 100 + rnd.nextInt(400); }
    }

    public void tick() {
        t += 1;
        prevCpu = cpu; prevMem = mem; prevQps = qps;

        // CPU：均值回归 + 周期 + 噪声 + 偶发尖峰
        float cpuTarget = 42 + (float) Math.sin(t / 8.0) * 14;
        cpu += (cpuTarget - cpu) * 0.15f + (rnd.nextFloat() - 0.5f) * 10;
        if (spikeCpu > 0.5f) { cpu += spikeCpu; spikeCpu *= 0.65f; }
        if (rnd.nextFloat() < 0.03f) spikeCpu = 25 + rnd.nextFloat() * 25;
        cpu = clamp(cpu, 5, 99);

        float memTarget = 62 + (float) Math.sin(t / 20.0) * 8;
        mem += (memTarget - mem) * 0.08f + (rnd.nextFloat() - 0.5f) * 3;
        mem = clamp(mem, 40, 92);

        float qpsTarget = 1200 + (float) Math.sin(t / 12.0) * 600;
        qps += (qpsTarget - qps) * 0.18f + (rnd.nextFloat() - 0.5f) * 250;
        qps = clamp(qps, 200, 3000);

        // 网络入出：跟随 QPS
        netIn = clamp(qps * 0.18f + (rnd.nextFloat() - 0.5f) * 40, 10, 800);
        netOut = clamp(qps * 0.12f + (rnd.nextFloat() - 0.5f) * 30, 5, 600);

        err = cpu > 85 ? clamp(rnd.nextFloat() * 3.5f, 0, 5) : clamp(rnd.nextFloat() * 0.4f, 0, 5);
        activeUsers = clamp(800 + (float) Math.sin(t / 15.0) * 300 + (rnd.nextFloat() - 0.5f) * 60, 100, 2000);

        // JVM
        heap = clamp(heap + ((55 + (float) Math.sin(t / 10.0) * 15) - heap) * 0.1f + (rnd.nextFloat() - 0.5f) * 3, 30, 92);
        nonHeap = clamp(nonHeap + ((42 + (float) Math.sin(t / 25.0) * 6) - nonHeap) * 0.05f, 25, 65);
        if (heap > 80 && rnd.nextFloat() < 0.2f) { gc += 1; heap -= 15; }   // GC 回收
        threads = (int) clamp(threads + (rnd.nextFloat() - 0.5f) * 4, 80, 160);

        // 机房环境：缓慢漂移
        temp = clamp(temp + (rnd.nextFloat() - 0.5f) * 0.3f, 18, 30);
        humidity = clamp(humidity + (rnd.nextFloat() - 0.5f) * 0.5f, 30, 70);

        // 节点
        for (NodeStatus n : nodes) {
            n.cpu = clamp(n.cpu + (cpu - n.cpu) * 0.1f + (rnd.nextFloat() - 0.5f) * 8, 5, 99);
            n.mem = clamp(n.mem + (mem - n.mem) * 0.08f + (rnd.nextFloat() - 0.5f) * 3, 30, 95);
            n.qps = (int) clamp(n.qps + (qps / nodes.size() - n.qps) * 0.2f + (rnd.nextFloat() - 0.5f) * 30, 20, 800);
            if (rnd.nextFloat() < 0.005f) n.online = !n.online;   // 偶发离线/恢复
            if (!n.online && rnd.nextFloat() < 0.3f) n.online = true;
        }

        push(cpuHistory, cpu); push(memHistory, mem);
        push(netInHistory, netIn); push(netOutHistory, netOut);
        push(heapHistory, heap);

        // 访问日志：每秒 1-2 条
        int n = 1 + (rnd.nextFloat() < 0.4f ? 1 : 0);
        for (int i = 0; i < n; i++) pushLog();

        // 告警触发
        if (cpu > 85 && rnd.nextFloat() < 0.3f) {
            pushAlert("严重", "CPU 负载过高 " + Math.round(cpu) + "%");
        }
        for (NodeStatus node : nodes) {
            if (!node.online && rnd.nextFloat() < 0.5f) pushAlert("警告", node.name + " 节点离线");
        }

        for (Listener l : listeners) {
            try { l.onTick(); } catch (Throwable ignored) {}
        }
    }

    private void pushLog() {
        String ip = (rnd.nextFloat() < 0.5f ? "10.0." : "192.168.") + rnd.nextInt(10) + "." + rnd.nextInt(250);
        String method = METHODS[rnd.nextInt(METHODS.length)];
        String path = PATHS[rnd.nextInt(PATHS.length)];
        int status;
        float roll = rnd.nextFloat();
        if (roll < 0.8f) status = 200;
        else if (roll < 0.86f) status = 304;
        else if (roll < 0.95f) status = 404;
        else status = 500;
        float cost = status >= 500 ? 300 + rnd.nextFloat() * 400 : 10 + rnd.nextFloat() * 180;
        logs.addFirst(new LogEntry(LocalTime.now().format(HMS), ip, method, path, status, cost));
        while (logs.size() > 30) logs.removeLast();
        if (status >= 500 && rnd.nextFloat() < 0.5f) {
            pushAlert("错误", path + " 返回 " + status);
        }
    }

    private void pushAlert(String level, String msg) {
        alerts.addFirst(new Alert(LocalTime.now().format(HMS), level, msg));
        while (alerts.size() > 8) alerts.removeLast();
    }

    private static void push(FloatArray a, float v) {
        a.add(v);
        if (a.size > HISTORY) a.removeIndex(0);
    }

    public static float trendPct(float prev, float cur) {
        if (Math.abs(prev) < 0.001f) return 0;
        return (cur - prev) / prev * 100f;
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    // =================== 数据结构 ===================

    public static class NodeStatus {
        public final String name, role;
        public boolean online = true;
        public float cpu, mem;
        public int qps;
        public NodeStatus(String name, String role) { this.name = name; this.role = role; }
    }

    public static class LogEntry {
        public final String time, ip, method, path;
        public final int status;
        public final float cost;
        public LogEntry(String time, String ip, String method, String path, int status, float cost) {
            this.time = time; this.ip = ip; this.method = method; this.path = path; this.status = status; this.cost = cost;
        }
    }

    public static class Alert {
        public final String time, level, msg;
        public Alert(String time, String level, String msg) {
            this.time = time; this.level = level; this.msg = msg;
        }
    }
}
