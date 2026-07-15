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

/**
 * Light 浅色主题（Bootstrap 5 默认配色）。
 *
 * <p>所有 token 值都来自改造前 BsSkinFactory/Bs 组件的硬编码 hex，
 * 切换到此主题时视觉应与改造前完全一致。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public final class BsLightTheme extends BsAbstractTheme {

    public static final BsLightTheme INSTANCE = new BsLightTheme();

    private BsLightTheme() {
        super("bs-light", false);
        // 文本
        put("text-primary",     0x1A1A1F);
        put("text-secondary",   0x495057);
        put("text-muted",       0x808088);
        put("text-disabled",    0x9999A0);
        put("text-on-primary",  0xFFFFFF);
        put("text-on-dark",     0xFFFFFF);
        // 背景
        put("bg-body",          0xF5F6F8);
        put("bg-surface",       0xFCFCFC);
        put("bg-elevated",      0xFFFFFF);
        put("bg-hover",         0xE7F1FF);
        put("bg-header",        0xF8F9FA);
        // 边框
        put("border",           0xCCCCCC);
        put("border-strong",    0xDEE2E6);
        // 6 色主色
        put("primary",          0x0D6EFD);
        put("secondary",        0x6C757D);
        put("success",          0x198754);
        put("danger",           0xDC3545);
        put("warning",          0xFFC107);
        put("info",             0x0DCAF0);
        put("light",            0xF8F9FA);
        put("dark",             0x212529);
        // 特殊（含 alpha 用 8 位 hex）
        put("shadow",           0x66000000);  // #00000066
        put("overlay",          0x73000000);  // #00000073
        put("link",             0x0D6EFD);
        put("link-hover",       0x0A58CA);
        put("focus-ring",       0x80BDFF);
        put("cursor",           0x333333);
    }
}
