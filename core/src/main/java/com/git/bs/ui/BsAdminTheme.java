package com.git.bs.ui;

/**
 * Admin 管理后台主题（深色风格，参考 dark 主题）。
 *
 * <p>颜色值与 {@code com/git/bs/ui/skin/admin.json} 的 Color 段保持一致（双保险）。
 * 改色改 admin.json 即可，这里同步更新。</p>
 */
public final class BsAdminTheme extends BsAbstractTheme {

    public static final BsAdminTheme INSTANCE = new BsAdminTheme();

    private BsAdminTheme() {
        super("admin", true);  // 深色主题
        // 文本（亮色，深背景上）
        put("text-primary",     0xF8FAFC);
        put("text-secondary",   0xCDD3DB);
        put("text-muted",       0x999FA8);
        put("text-disabled",    0x666B73);
        put("text-on-primary",  0xFFFFFF);
        put("text-on-dark",     0xFFFFFF);
        // 背景（深色）
        put("bg-body",          0x212530);   // 内容区深灰
        put("bg-surface",       0x2B303D);   // 卡片深灰
        put("bg-elevated",      0x212936);   // 侧边栏深蓝灰
        put("bg-hover",         0x262D3B);   // 侧边栏悬停
        put("bg-header",        0x2B303D);   // 顶部栏深灰
        // 边框
        put("border",           0x4D5462);
        put("border-strong",    0x666E7C);
        // 6 色
        put("primary",          0x409EFF);
        put("secondary",        0x909399);
        put("success",          0x67C23A);
        put("danger",           0xF56C6C);
        put("warning",          0xE6A23C);
        put("info",             0x909399);
        // 特殊
        put("shadow",           0x66000000);
        put("overlay",          0x73000000);
        put("link",             0x409EFF);
        put("link-hover",       0x337ECC);
        put("focus-ring",       0xA0CFFF);
        put("cursor",           0xF8FAFC);
    }
}
