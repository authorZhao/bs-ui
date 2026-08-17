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

package cn.pingyuanren.bs.winsettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import cn.pingyuanren.bs.i18n.BsI18n;
import cn.pingyuanren.bs.ui.BsLoadingOverlay;
import cn.pingyuanren.bs.ui.BsToast;
import lombok.extern.slf4j.Slf4j;

/**
 * Windows 更新页（按 Win11 真实结构）。
 *
 * <p>设置组：检查更新 / 更多选项 / 高级选项 / 预览体验 / 故障排除。</p>
 *
 * <p><b>演示</b>：点击「检查更新」按钮 → 弹全屏旋转加载遮罩（{@link BsLoadingOverlay}），
 * 模拟进度递增（0→100%），完成后弹 Toast 显示结果（最新 / 发现更新）。展示异步操作的
 * loading + 进度条 + 完成反馈的典型交互。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class WindowsUpdatePage extends CategoryPage {

    /** 检查更新状态（演示用，避免连点重复弹遮罩）。 */
    private boolean checking = false;

    public WindowsUpdatePage(Skin skin) {
        super(BsI18n.get("nav.update"), skin);

        group(BsI18n.get("update.group_check"),
                SettingItem.value(BsI18n.get("update.update_status"), "", BsI18n.get("update.up_to_date")),
                SettingItem.value(BsI18n.get("update.last_check"), "", BsI18n.get("update.last_check_value")),
                SettingItem.button(BsI18n.get("update.check_for_updates"), BsI18n.get("update.check_for_updates_desc"), BsI18n.get("update.check_for_updates"),
                        this::simulateCheckForUpdates),
                SettingItem.value(BsI18n.get("update.available_updates"), "", BsI18n.get("update.no_updates"))
        );

        group(BsI18n.get("update.group_more_options"),
                SettingItem.button(BsI18n.get("update.update_history"), BsI18n.get("update.update_history_desc"), BsI18n.get("update.view")),
                SettingItem.button(BsI18n.get("update.uninstall_updates"), BsI18n.get("update.uninstall_updates_desc"), BsI18n.get("update.uninstall")),
                SettingItem.select(BsI18n.get("update.pause_updates"), BsI18n.get("update.pause_updates_desc"), new String[]{BsI18n.get("update.no_pause"), BsI18n.get("update.pause_1w"), BsI18n.get("update.pause_2w"), BsI18n.get("update.pause_3w"), BsI18n.get("update.pause_4w"), BsI18n.get("update.pause_5w")}, BsI18n.get("update.no_pause")),
                SettingItem.value(BsI18n.get("update.pause_deadline"), "", BsI18n.get("update.not_paused")),
                SettingItem.button(BsI18n.get("update.check_online"), BsI18n.get("update.check_online_desc"), BsI18n.get("update.check"))
        );

        group(BsI18n.get("update.group_advanced"),
                SettingItem.toggle(BsI18n.get("update.notify_during_use"), BsI18n.get("update.notify_during_use_desc"), true),
                SettingItem.toggle(BsI18n.get("update.auto_download"), BsI18n.get("update.auto_download_desc"), true),
                SettingItem.toggle(BsI18n.get("update.update_other_ms_products"), BsI18n.get("update.update_other_ms_products_desc"), true),
                SettingItem.toggle(BsI18n.get("update.metered_connection"), BsI18n.get("update.metered_connection_desc"), false),
                SettingItem.button(BsI18n.get("update.delivery_optimization"), BsI18n.get("update.delivery_optimization_desc"), BsI18n.get("update.configure")),
                SettingItem.button(BsI18n.get("update.active_hours"), BsI18n.get("update.active_hours_desc"), BsI18n.get("update.adjust")),
                SettingItem.select(BsI18n.get("update.notification_method"), "", new String[]{BsI18n.get("update.notify_banner"), BsI18n.get("update.notify_sound"), BsI18n.get("update.notify_silent")}, BsI18n.get("update.notify_banner"))
        );

        group(BsI18n.get("update.group_insider"),
                SettingItem.value(BsI18n.get("update.insider_status"), "", BsI18n.get("update.not_joined")),
                SettingItem.link(BsI18n.get("update.join_insider"), BsI18n.get("update.join_insider_desc"), BsI18n.get("update.join")),
                SettingItem.value(BsI18n.get("update.channel"), "", "—")
        );

        group(BsI18n.get("update.group_troubleshoot"),
                SettingItem.button(BsI18n.get("update.update_troubleshooter"), BsI18n.get("update.update_troubleshooter_desc"), BsI18n.get("update.run")),
                SettingItem.value(BsI18n.get("update.last_troubleshoot"), "", BsI18n.get("update.never_run")),
                SettingItem.button(BsI18n.get("update.reset_components"), BsI18n.get("update.reset_components_desc"), BsI18n.get("update.reset"))
        );
    }

    /**
     * 演示：点击「检查更新」→ 旋转加载遮罩（带进度条递增）→ 完成后弹 Toast。
     *
     * <p>实现：用 libgdx {@link com.badlogic.gdx.utils.Timer} 每 100ms 触发，postRunnable
     * 切回 render 线程更新进度（scene2d 非线程安全）。约 8 秒走完 0→100%，
     * 完成后关闭遮罩、弹 Toast 显示结果。遮罩 modal 拦截所有点击，避免检查期间用户重复操作。</p>
     */
    private void simulateCheckForUpdates() {
        if (checking) {
            log.info("[Windows 更新] 正在检查中，忽略重复点击");
            return;
        }
        Stage stage = currentStage();
        if (stage == null) {
            log.info("[Windows 更新] 检查更新（无 stage，跳过遮罩演示）");
            return;
        }
        checking = true;
        log.info("[Windows 更新] 开始检查更新…");

        // 显示遮罩：spinner + 「正在检查更新…」 + 进度条从 0 开始
        final BsLoadingOverlay overlay = BsLoadingOverlay.show(stage, skin,
                BsI18n.get("demo.update_checking"), 0f);

        // 每 100ms 推进进度（每秒 +12%，约 8s 走完，进度条走得自然不突兀）
        final float[] progress = {0f};
        final float step = 1.2f;   // 每次Tick +1.2%（10次/秒 × 1.2 = 12/s → ~8秒满）
        final com.badlogic.gdx.utils.Timer.Task task = new com.badlogic.gdx.utils.Timer.Task() {
            @Override public void run() {
                // Timer 在独立线程，scene2d 操作必须切回 render 线程
                Gdx.app.postRunnable(() -> tick(stage, overlay, progress, step, this));
            }
        };
        com.badlogic.gdx.utils.Timer.schedule(task, 0f, 0.1f);
    }

    /** 单次进度推进（render 线程内执行）。到 100% 关闭遮罩、弹 Toast、停止定时器。 */
    private void tick(Stage stage, BsLoadingOverlay overlay, float[] progress, float step,
                      com.badlogic.gdx.utils.Timer.Task task) {
        if (overlay.getStage() == null) {
            // 遮罩已被关闭（主题/语言切换重建 screen 等）→ 停止定时器
            task.cancel();
            checking = false;
            return;
        }
        progress[0] = Math.min(100f, progress[0] + step);
        overlay.setProgress(progress[0]);
        overlay.setText(BsI18n.get("demo.update_progress", (int) progress[0]));
        if (progress[0] >= 100f) {
            task.cancel();
            overlay.close();
            checking = false;
            // 模拟结果：80% 概率「最新」，20% 发现 3 个更新
            boolean upToDate = Math.random() < 0.8;
            if (upToDate) {
                BsToast.show(stage, skin,
                        BsI18n.get("demo.update_uptodate"),
                        BsToast.Variant.SUCCESS, 4f);
            } else {
                BsToast.show(stage, skin,
                        BsI18n.get("demo.update_found", 3),
                        BsToast.Variant.INFO, 4f);
            }
            log.info("[Windows 更新] 检查完成: {}", upToDate ? "已是最新" : "发现 3 个更新");
        }
    }

    /** 拿当前 stage：WinSettingsScreen.show() 里 {@code Gdx.input.setInputProcessor(stage)}，所以 InputProcessor 即 stage。 */
    private static Stage currentStage() {
        try {
            Object ip = Gdx.input.getInputProcessor();
            if (ip instanceof Stage) return (Stage) ip;
        } catch (Throwable ignored) {}
        return null;
    }
}
