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
package com.git.bs.ui.ext;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;

/// 拖放（DnD）上下文：包装 libGDX 自带的 `DragAndDrop`，提供 Bs 风格门面。
///
/// 一个 `BsDnd` 实例管理一组 source/target（source 与 target 必须共享同一实例才能联动）。
///
/// 用法：
/// ```java
/// BsDnd dnd = new BsDnd();
///
/// dnd.source(itemActor)
///    .payload("item-42")
///    .onDropped((payload, overTarget) -> { if (overTarget != null) move(payload); });
///
/// dnd.target(binActor)
///    .onDrop((payload, fromActor) -> delete(payload));
/// ```
///
/// 说明：拖动视觉默认用一个半透明 Label（显示 payload 的 `toString`），可用 `dragActor` 自定义。
/// 选择包装核心 `DragAndDrop` 而非自研——它是纯 scene2d、TeaVM 安全，避开多点/触屏事件坑。
/// @author authorZhao
/// @since 2026-07-16
public final class BsDnd {

    private final DragAndDrop dnd = new DragAndDrop();

    /// 注册一个拖动源。
    public <T> BsDragSource<T> source(Actor actor) {
        return new BsDragSource<>(dnd, actor);
    }

    /// 注册一个放置目标。
    public BsDropTarget target(Actor actor) {
        return new BsDropTarget(dnd, actor);
    }

    /// 拖动视觉相对于指针的偏移（像素）。
    public BsDnd setDragActorOffset(float x, float y) {
        dnd.setDragActorPosition(x, y);
        return this;
    }

    /** 暴露底层 `DragAndDrop`（高级用法，如自定义拖动延迟等）。 */
    public DragAndDrop underlying() {
        return dnd;
    }
}
