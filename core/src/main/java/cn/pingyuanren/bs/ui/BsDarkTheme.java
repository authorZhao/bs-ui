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

/**
 * Dark 暗色主题（参考 Bootstrap 5.3 dark 配色）。
 *
 * <p>关键调整：
 * <ul>
 *   <li>背景层级：bgBody(#212529) < bgSurface(#2B3035) < bgElevated(#343A40)</li>
 *   <li>主色提亮：primary 用 #3D8BFD（dark 下蓝色更亮才显眼）</li>
 *   <li>遮罩更浓：overlay 70% alpha</li>
 * </ul>
 *
 * <p><b>注意</b>：softBg 不在 skin 里写死，由 {@link BsTheme#softBgOf(Skin, String)} 动态计算。
 * Dark 主题下 softBg 算法（主色 + bgSurface 1:1 混合）需要在组件取色时单独处理
 * （或在 applyColorsToSkin 后单独覆盖，这里用默认算法即可）。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public final class BsDarkTheme extends BsAbstractTheme {

    public static final BsDarkTheme INSTANCE = new BsDarkTheme();

    private BsDarkTheme() {
        super("bs-dark", true);
        // 文本
        put("text-primary",     0xF8F9FA);
        put("text-secondary",   0xADB5BD);
        put("text-muted",       0x6C757D);
        put("text-disabled",    0x495057);
        put("text-on-primary",  0xFFFFFF);
        put("text-on-dark",     0xFFFFFF);
        // 背景
        put("bg-body",          0x212529);
        put("bg-surface",       0x2B3035);
        put("bg-elevated",      0x343A40);
        put("bg-hover",         0x2C3E50);
        put("bg-header",        0x343A40);
        // 边框
        put("border",           0x495057);
        put("border-strong",    0x5A6268);
        // 6 色（dark 下提亮）
        put("primary",          0x3D8BFD);
        put("secondary",        0x6C757D);
        put("success",          0x2DBE60);
        put("danger",           0xE55B6B);
        put("warning",          0xFFCA2C);
        put("info",             0x4FD3E6);
        put("light",            0xF8F9FA);
        put("dark",             0x343A40);   // 比 bg-body(0x212529) 亮一档，暗底下才可见
        // 特殊
        put("shadow",           0x99000000);  // #00000099
        put("overlay",          0xB3000000);  // #000000B3
        put("link",             0x3D8BFD);
        put("link-hover",       0x5BA0FD);
        put("focus-ring",       0x803D8BFD);  // primary + 50% alpha
        put("cursor",           0xADB5BD);
    }
}
