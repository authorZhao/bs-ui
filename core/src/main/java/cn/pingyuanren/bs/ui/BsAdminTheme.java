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
 * Admin 管理后台主题（深色风格，参考 Element Plus Dark + Ant Design Pro 暗色）。
 *
 * <p>命名约定：name = "bs-admin"，对齐 bs-light / bs-dark。
 * 资源文件：{@code cn/pingyuanren/bs/ui/skin/bs-admin.json} + bs-admin.atlas + bs-admin.png。</p>
 *
 * <p>配色方向（参考主流 admin 模板）：</p>
 * <ul>
 *   <li>背景层次：bg-body &lt; bg-surface &lt; bg-elevated（蓝灰冷调，层次清晰）</li>
 *   <li>主色 Element 蓝 #409EFF（admin 后台经典）</li>
 *   <li>文本：高对比 primary #F0F2F5，secondary #A8B2C0 偏冷灰</li>
 *   <li>边框：稍亮 #3C4858 让分割线在深底上清晰</li>
 * </ul>
 * @author authorZhao
 * @since 2026-07-16
 */
public final class BsAdminTheme extends BsAbstractTheme {

    public static final BsAdminTheme INSTANCE = new BsAdminTheme();

    private BsAdminTheme() {
        super("bs-admin", true);  // 深色主题，name 对齐 bs- 前缀约定
        // 文本（高对比，深背景上）
        put("text-primary",     0xF0F2F5);   // 主文本：偏暖白，可读性高
        put("text-secondary",   0xA8B2C0);   // 次文本：冷灰
        put("text-muted",       0x6B7280);   // 弱文本：占位/提示
        put("text-disabled",    0x4B5563);   // 禁用
        put("text-on-primary",  0xFFFFFF);   // 主色按钮上的字
        put("text-on-dark",     0xFFFFFF);   // 深底高亮字
        // 背景（蓝灰冷调，层次：body < surface < elevated）
        put("bg-body",          0x1F2D3D);   // 内容区最底：深蓝灰
        put("bg-surface",       0x253242);   // 卡片/面板：稍亮
        put("bg-elevated",      0x2A3A4F);   // 侧栏/顶栏：再亮一点，强调结构
        put("bg-hover",         0x304056);   // 列表/菜单悬停
        put("bg-header",        0x2A3A4F);   // 顶部栏（与 elevated 一致）
        // 边框（亮一点让分割线清晰）
        put("border",           0x3C4858);
        put("border-strong",    0x4B5563);
        // 6 色（Element Plus 风格）
        put("primary",          0x409EFF);   // Element 经典蓝
        put("secondary",        0x909399);   // 中灰
        put("success",          0x67C23A);   // 绿
        put("danger",           0xF56C6C);   // 红
        put("warning",          0xE6A23C);   // 黄
        put("info",             0x909399);   // 中灰（与 secondary 同）
        put("light",            0xF0F2F5);   // 浅底
        put("dark",             0x303133);   // 深底（admin 偏冷灰）
        // 特殊
        put("shadow",           0x66000000);  // 半透明黑阴影
        put("overlay",          0x99000000);  // 遮罩 60%
        put("link",             0x409EFF);
        put("link-hover",       0x66B1FF);   // hover 提亮
        put("focus-ring",       0xA0CFFF);   // 焦点环：浅蓝
        put("cursor",           0xF0F2F5);   // 输入光标
    }
}
