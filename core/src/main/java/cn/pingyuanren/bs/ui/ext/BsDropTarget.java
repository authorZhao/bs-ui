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
package cn.pingyuanren.bs.ui.ext;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/// 放置目标（由 {@link BsDnd#target(Actor)} 创建）。包装 `DragAndDrop.Target`。
///
/// 链式设置：`setAccept`（按 payload 判定是否接受，默认全接受）、`onDrop`（`(payload, 源actor)`）。
/// @author authorZhao
/// @since 2026-07-16
@Slf4j
public class BsDropTarget {

    private final DragAndDrop dnd;
    private final Actor actor;
    private final DragAndDrop.Target target;
    private Predicate<Object> accept = o -> true;
    private BiConsumer<Object, Actor> onDrop;   // (payload, sourceActor)

    BsDropTarget(DragAndDrop dnd, Actor actor) {
        this.dnd = dnd;
        this.actor = actor;
        this.target = new DragAndDrop.Target(actor) {
            @Override
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                Object data = payload != null ? payload.getObject() : null;
                boolean ok;
                try { ok = accept.test(data); } catch (Throwable t) { ok = false; }
                return ok;   // true=接受此处放置（决定高亮/可放）
            }

            @Override
            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                if (onDrop == null) return;
                Object data = payload != null ? payload.getObject() : null;
                Actor sourceActor = source != null ? source.getActor() : null;
                try { onDrop.accept(data, sourceActor); } catch (Throwable t) { log.warn("BsDropTarget onDrop error", t); }
            }
        };
        dnd.addTarget(target);
    }

    /// 设置接受判定（按 payload）。默认全接受。
    public BsDropTarget setAccept(Predicate<Object> predicate) {
        if (predicate != null) this.accept = predicate;
        return this;
    }

    /// 放下回调：`(payload, 源actor)`。仅当 `drag` 返回 true（被接受）时 `drop` 才会触发。
    public BsDropTarget onDrop(BiConsumer<Object, Actor> c) { this.onDrop = c; return this; }

    public Actor getActor() { return actor; }
}
