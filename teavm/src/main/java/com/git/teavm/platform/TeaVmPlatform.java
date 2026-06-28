package com.git.teavm.platform;


import com.badlogic.gdx.utils.Timer;
import com.git.bs.common.Platform;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author authorZhao
 * @since 2025-08-12
 */
public class TeaVmPlatform implements Platform {

    /** 跟踪所有 libGDX Timer.Task，cancelAll 时统一 cancel。 */
    private static final List<Timer.Task> tasks = new ArrayList<>();

    @Override
    public String getPlatformName() {
        return "web-gl";
    }

    @Override
    public void exit() {
        // Nothing to do
    }

    @Override
    public boolean setWindowIcons(String windowTitle, String iconPath) {
        return false;
    }

    @Override
    public String chooseJarFile() {
        return "";
    }

    @Override
    public void scheduleOne(Runnable runnable, long delay, TimeUnit unit) {
        // 关键：完全放弃 TeaVM 的 Window.setTimeout / TimerHandler —— 后者在 wasm-gc target 下
        // 生成 JSObject 桥时会 array index out of bounds。
        // 改用 libGDX 的 com.badlogic.gdx.utils.Timer：纯 Java，由 libGDX 主循环驱动，
        // 平台无关，wasm-gc 安全。回调时直接调用 runnable.run()。
        float delaySec = Math.max(0f, unit.toMillis(delay) / 1000f);
        Timer.Task task = new Timer.Task() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    synchronized (tasks) {
                        tasks.remove(this);
                    }
                }
            }
        };
        synchronized (tasks) {
            tasks.add(task);
        }
        Timer.schedule(task, delaySec);
    }

    @Override
    public void schedule(Runnable runnable, long delay, long period, TimeUnit unit) {
        long periodMillis = Math.max(0L, unit.toMillis(period));
        if (periodMillis <= 0) {
            // 没有周期，按一次性处理
            scheduleOne(runnable, delay, unit);
            return;
        }
        float delaySec = Math.max(0f, unit.toMillis(delay) / 1000f);
        float intervalSec = periodMillis / 1000f;
        // schedule 的首次执行在 delay 后，后续每 intervalSec 执行一次
        // —— 与 Java ScheduledExecutor 行为一致。
        Timer.Task task = new Timer.Task() {
            @Override
            public void run() {
                runnable.run();
            }
        };
        synchronized (tasks) {
            tasks.add(task);
        }
        Timer.schedule(task, delaySec, intervalSec);
    }

    @Override
    public void cancelAll() {
        synchronized (tasks) {
            for (Timer.Task t : tasks) {
                if (t.isScheduled()) {
                    t.cancel();
                }
            }
            tasks.clear();
        }
    }

    @Override
    public Map<String, String> getenv() {
        return Map.of();
    }

    /**
     * Web 端系统 dark mode 检测：通过浏览器 {@code window.matchMedia('(prefers-color-scheme: dark)')}
     * 查询用户系统/浏览器的暗色主题偏好。
     * <p>支持的浏览器：Chrome/Edge 76+、Firefox 67+、Safari 12.1+。
     * 不支持的浏览器返回 false（fallback 到 light）。</p>
     */
    @Override
    public boolean isSystemDarkMode() {
        try {
            return nativePrefersDarkColorScheme();
        } catch (Throwable t) {
            // 浏览器不支持或 TeaVM JS 桥失败，回退 false
            return false;
        }
    }

    /**
     * TeaVM JSBody：调用 window.matchMedia('(prefers-color-scheme: dark)').matches。
     * <p>使用 var 语法避免在浏览器不识别 matchMedia 时整段失败 ——
     * 老浏览器 matchMedia 返回 undefined，try-catch 兜底回 false。</p>
     */
    @JSBody(script = """
            try {
                if (typeof window === 'undefined' || !window.matchMedia) return false;
                return window.matchMedia('(prefers-color-scheme: dark)').matches;
            } catch (e) {
                return false;
            }
            """)
    private static native boolean nativePrefersDarkColorScheme();
}
