package com.git.bs.ui;

/**
 * Light 浅色主题（Bootstrap 5 默认配色）。
 *
 * <p>所有 token 值都来自改造前 BsSkinFactory/Bs 组件的硬编码 hex，
 * 切换到此主题时视觉应与改造前完全一致。</p>
 */
public final class BsLightTheme extends BsAbstractTheme {

    public static final BsLightTheme INSTANCE = new BsLightTheme();

    private BsLightTheme() {
        super("light", false);
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
        // 特殊（含 alpha 用 8 位 hex）
        put("shadow",           0x66000000);  // #00000066
        put("overlay",          0x73000000);  // #00000073
        put("link",             0x0D6EFD);
        put("link-hover",       0x0A58CA);
        put("focus-ring",       0x80BDFF);
        put("cursor",           0x333333);
    }
}
