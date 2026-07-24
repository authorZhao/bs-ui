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

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * 环形进度 —— {@link BsCircularProgress} 的兼容别名。
 *
 * <p><b>历史</b>：早期 {@link BsCircularProgress} 用 48 颗位图圆点铺圆周（珠串），
 * 放大后间隙/发虚明显；本子类用 ShapeRenderer 画连续三角形带弧，作为"平滑版"存在。
 * 2026-07-24 方案 A 统一：{@link BsCircularProgress} 自身也改为 ShapeRenderer 连续弧，
 * 两者渲染完全一致，本类保留仅为向后兼容（已有代码 {@code new BsRingProgress(...)} 无需改）。</p>
 *
 * <p><b>新代码请直接用 {@link BsCircularProgress}</b>，本类不新增功能，未来版本可能移除。</p>
 *
 * @author authorZhao
 * @since 2026-07-16
 * @deprecated 渲染逻辑已合并到 {@link BsCircularProgress}，请直接使用父类。本类仅为兼容保留。
 */
@Deprecated
public class BsRingProgress extends BsCircularProgress {

    public BsRingProgress(Skin skin) {
        super(skin);
    }

    public BsRingProgress(Skin skin, Variant variant) {
        super(skin, variant);
    }
}
