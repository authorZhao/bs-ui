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
package cn.pingyuanren.bs.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

/**
 * 弹窗动画工具：基于 scene2d {@link Actions} 封装常用入场/出场动画。
 *
 * <p>支持的动画类型：</p>
 * <ul>
 *   <li>{@link #slideInDown(Actor, float)} —— 从上方滑入（dialog 从 stage 顶部下滑到中心）</li>
 *   <li>{@link #slideInUp(Actor, float)} —— 从下方滑入</li>
 *   <li>{@link #fadeIn(Actor, float)} —— 淡入（透明度 0→1）</li>
 *   <li>{@link #scaleIn(Actor, float)} —— 缩放进入（0.85→1.0 + 透明度）</li>
 *   <li>{@link #slideOutUp(Actor, float, Runnable)} —— 上滑消失（关闭时调用）</li>
 *   <li>{@link #fadeOut(Actor, float, Runnable)} —— 淡出消失</li>
 * </ul>
 *
 * <p>用法（typically 在 showModal 后调用）：</p>
 * <pre>{@code
 * modal.showModal(stage);
 * BsAnimations.fadeIn(modal, 0.25f);
 *
 * // 关闭时：
 * BsAnimations.slideOutUp(modal, 0.2f, () -> modal.close());
 * }</pre>
 *
 * <p>注意：调用前需要 actor 已 addActor 到 stage 且 pack() 完成，否则坐标计算不准。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public final class BsAnimations {

    /** 默认动画时长（秒）。 */
    public static final float DEFAULT_DURATION = 0.25f;

    private BsAnimations() {}

    // ========================= 入场动画 =========================

    /** 从 stage 顶部滑入到当前位置（向下）。duration 秒。 */
    public static void slideInDown(Actor actor, float duration) {
        float targetY = actor.getY();
        float startY = actor.getStage().getHeight();  // 从顶部开始
        actor.setY(startY);
        actor.clearActions();
        actor.addAction(Actions.moveTo(actor.getX(), targetY, duration, Interpolation.sineOut));
    }

    /** 从 stage 底部滑入到当前位置（向上）。 */
    public static void slideInUp(Actor actor, float duration) {
        float targetY = actor.getY();
        float startY = -actor.getHeight();
        actor.setY(startY);
        actor.clearActions();
        actor.addAction(Actions.moveTo(actor.getX(), targetY, duration, Interpolation.sineOut));
    }

    /** 淡入（透明度 0→1）。 */
    public static void fadeIn(Actor actor, float duration) {
        actor.getColor().a = 0f;
        actor.clearActions();
        actor.addAction(Actions.fadeIn(duration));
    }

    /** 缩放进入（缩放 0.85→1.0 + 透明度淡入）。需要 actor 先 setOrigin(center)。 */
    public static void scaleIn(Actor actor, float duration) {
        actor.setScale(0.85f);
        actor.getColor().a = 0f;
        actor.setOrigin(actor.getWidth() / 2f, actor.getHeight() / 2f);
        actor.clearActions();
        actor.addAction(Actions.parallel(
                Actions.scaleTo(1f, 1f, duration, Interpolation.sineOut),
                Actions.fadeIn(duration)
        ));
    }

    // ========================= 出场动画 =========================

    /** 上滑消失（actor 移到 stage 顶部之外 + 淡出），结束后调 onComplete。 */
    public static void slideOutUp(Actor actor, float duration, Runnable onComplete) {
        float exitY = actor.getStage().getHeight();
        actor.clearActions();
        actor.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveTo(actor.getX(), exitY, duration, Interpolation.sineIn),
                        Actions.fadeOut(duration)
                ),
                Actions.run(() -> {
                    if (onComplete != null) {
                        try { onComplete.run(); } catch (Throwable t) { /* ignore */ }
                    }
                })
        ));
    }

    /** 淡出消失，结束后调 onComplete。 */
    public static void fadeOut(Actor actor, float duration, Runnable onComplete) {
        actor.clearActions();
        actor.addAction(Actions.sequence(
                Actions.fadeOut(duration),
                Actions.run(() -> {
                    if (onComplete != null) {
                        try { onComplete.run(); } catch (Throwable t) { /* ignore */ }
                    }
                })
        ));
    }

    /** 缩放消失（缩小 + 淡出），结束后调 onComplete。 */
    public static void scaleOut(Actor actor, float duration, Runnable onComplete) {
        actor.setOrigin(actor.getWidth() / 2f, actor.getHeight() / 2f);
        actor.clearActions();
        actor.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.scaleTo(0.85f, 0.85f, duration, Interpolation.sineIn),
                        Actions.fadeOut(duration)
                ),
                Actions.run(() -> {
                    if (onComplete != null) {
                        try { onComplete.run(); } catch (Throwable t) { /* ignore */ }
                    }
                })
        ));
    }

    /** 下滑消失（actor 移到 stage 底部之下 + 淡出）。 */
    public static void slideOutDown(Actor actor, float duration, Runnable onComplete) {
        float exitY = -actor.getHeight();
        actor.clearActions();
        actor.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveTo(actor.getX(), exitY, duration, Interpolation.sineIn),
                        Actions.fadeOut(duration)
                ),
                Actions.run(() -> {
                    if (onComplete != null) {
                        try { onComplete.run(); } catch (Throwable t) { /* ignore */ }
                    }
                })
        ));
    }
}
