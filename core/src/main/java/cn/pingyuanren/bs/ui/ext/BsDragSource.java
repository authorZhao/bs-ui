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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import cn.pingyuanren.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/// 拖动源（由 {@link BsDnd#source(Actor)} 创建）。包装 `DragAndDrop.Source`。
///
/// 链式设置：`payload`（拖挂数据）、`dragActor`（拖动时的视觉，默认半透明 Label）、
/// `onDragStart`、`onDropped`（`payload` + 目标 actor，目标为 null 表示放空）。
/// @author authorZhao
/// @since 2026-07-16
@Slf4j
public class BsDragSource<T> {

    private final DragAndDrop dnd;
    private final Actor actor;
    private T payload;
    private Function<T, Actor> dragActorProvider;
    private Consumer<T> onDragStart;
    private BiConsumer<T, Actor> onDropped;   // (payload, targetActor-or-null)

    BsDragSource(DragAndDrop dnd, Actor actor) {
        this.dnd = dnd;
        this.actor = actor;
        dnd.addSource(new DragAndDrop.Source(actor) {
            @Override
            public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                DragAndDrop.Payload p = new DragAndDrop.Payload();
                p.setObject(payload);
                Actor vis = dragActorProvider != null ? dragActorProvider.apply(payload) : defaultDragActor(payload);
                p.setDragActor(vis);
                if (onDragStart != null) {
                    try { onDragStart.accept(payload); } catch (Throwable t) { log.warn("BsDragSource onDragStart error", t); }
                }
                return p;
            }

            @Override
            public void dragStop(InputEvent event, float x, float y, int pointer,
                                 DragAndDrop.Payload payload, DragAndDrop.Target target) {
                if (onDropped == null) return;
                Actor targetActor = target != null ? target.getActor() : null;
                try {
                    @SuppressWarnings("unchecked")
                    T data = (T) (payload != null ? payload.getObject() : null);
                    onDropped.accept(data, targetActor);
                } catch (Throwable t) {
                    log.warn("BsDragSource onDropped error", t);
                }
            }
        });
    }

    /// 设置拖挂数据。
    public BsDragSource<T> payload(T payload) { this.payload = payload; return this; }

    /// 自定义拖动时的视觉 actor（按 payload 生成）。
    public BsDragSource<T> dragActor(Function<T, Actor> provider) { this.dragActorProvider = provider; return this; }

    /// 拖动开始回调。
    public BsDragSource<T> onDragStart(Consumer<T> c) { this.onDragStart = c; return this; }

    /// 放下回调：`(payload, 目标actor)`，目标为 null 表示未落在任何 target 上。
    public BsDragSource<T> onDropped(BiConsumer<T, Actor> c) { this.onDropped = c; return this; }

    public Actor getActor() { return actor; }

    private Actor defaultDragActor(T payload) {
        Label l = new Label(payload == null ? "" : payload.toString(), BsUI.getSkin());
        Color c = l.getColor();
        l.setColor(c.r, c.g, c.b, 0.85f);
        return l;
    }
}
