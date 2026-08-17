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

package cn.pingyuanren.bs.admin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import cn.pingyuanren.bs.admin.modules.BusinessDemoModule;
import cn.pingyuanren.bs.admin.modules.DashboardModule;
import cn.pingyuanren.bs.admin.modules.UiDemoModule;
import cn.pingyuanren.bs.admin.modules.UserListModule;
import cn.pingyuanren.bs.game.AdminApp;
import cn.pingyuanren.bs.ui.BsAboutDialog;
import cn.pingyuanren.bs.ui.BsBreadcrumb;
import cn.pingyuanren.bs.ui.BsButton;
import cn.pingyuanren.bs.ui.BsLayoutAdmin;
import cn.pingyuanren.bs.ui.BsModal;
import cn.pingyuanren.bs.ui.BsSwitch;
import cn.pingyuanren.bs.ui.BsTheme;
import cn.pingyuanren.bs.ui.BsToast;
import cn.pingyuanren.bs.ui.BsAdminTheme;
import cn.pingyuanren.bs.ui.BsDarkTheme;
import cn.pingyuanren.bs.ui.BsLightTheme;
import cn.pingyuanren.bs.ui.BsUI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin 模板主屏：
 * <ul>
 *   <li>组合 {@link BsLayoutAdmin}（顶部栏 + 侧边栏 + 内容区）</li>
 *   <li>顶部栏插入面包屑 + 右上角用户菜单（个人中心/设置/主题切换/退出登录）</li>
 *   <li>侧边栏菜单树由注册的 {@link AdminModule} 自动生成</li>
 *   <li>内容区用 ScrollPane 包装模块 buildView 返回值</li>
 * </ul>
 *
 * <p><b>新增模块</b>：实现 {@link AdminModule}，构造时 {@code register(new XxxModule())}。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsAdminShell extends ScreenAdapter {

    public static final int WIN_W = 1280;
    public static final int WIN_H = 800;

    private final AdminApp app;
    @Getter
    private final Skin skin;
    @Getter
    private final Stage stage;

    private final BsLayoutAdmin layout;
    private final BsBreadcrumb breadcrumb;
    private final Table contentHost;
    private final Container<Actor> scrollWrap;

    /** 已注册模块（按注册顺序）。 */
    private final List<AdminModule> modules = new ArrayList<>();
    /** path -> module 索引（O(1) 查找）。 */
    private final Map<String, AdminModule> pathIndex = new HashMap<>();
    /** 一级菜单名 -> 已注册的 root SidebarItem 镜像（BsLayoutAdmin 未暴露 root 列表）。 */
    private final Map<String, BsLayoutAdmin.SidebarItem> rootSidebarMirror = new LinkedHashMap<>();
    /** 当前激活模块的 path。 */
    private String currentPath;
    /** 上次放大字号时 sidebar 的按钮数（用于检测 core 内部 rebuild 后重新放大）。 */
    private int lastEnlargedSidebarButtonCount = -1;
    /** topBar 字号是否已放大（topBar 不重建，只放大一次，避免重复累乘）。 */
    private boolean topBarEnlarged = false;

    public BsAdminShell(AdminApp app) {
        this.app = app;
        this.skin = app.getSkin();
        this.stage = new Stage(new ScreenViewport());

        // 内容宿主：top/left，模块返回的 actor 会被塞进来再包进 scroll
        contentHost = new Table();
        contentHost.top().left();
        contentHost.defaults().top().left();

        scrollWrap = new Container<>();
        scrollWrap.fill(true, true);
        // scrollWrap 不设背景，让 BsLayoutAdmin 的 contentWrap 圆角卡片背景透出来
        // （之前设 bg-body 平铺色会盖住 contentWrap 的圆角 NinePatch，看不出卡片风格）

        // 布局骨架
        layout = new BsLayoutAdmin(skin);
        layout.setLogo("bs-ui Admin");
        layout.setFillParent(true);
        // 放大侧边栏宽度：默认 280 + sidebarWrap 圆角 pad 后有效区约 270；
        // 提到 296 补偿 pad 吃掉的宽度，避免长菜单名截断
        layout.setSidebarWidth(296);
        // 深色侧边栏风格：仅暗色主题（admin/dark）用白字，亮色主题用正常深色字
        // 否则 light 主题下侧栏背景变浅，白字看不清
        cn.pingyuanren.bs.ui.BsTheme theme = BsUI.currentTheme();
        boolean sidebarDark = theme != null && theme.isDark();
        layout.setSidebarDarkStyle(sidebarDark);
        // 放大顶部栏高度（默认 48 偏矮，字挤），同时记录到 layout 的展开高度
        layout.setTopBarExpandedHeight(60);

        // 面包屑（插到 topBar 中）
        breadcrumb = new BsBreadcrumb(skin);

        // 通过命名查找 topMenuRow（新增 hideTopBtn 后 cell 索引会偏移，统一用 findActor）
        Table topMenuRow = (Table) layout.getTopBar().findActor("topMenuRow");
        if (topMenuRow != null) {
            topMenuRow.add(breadcrumb).left().padLeft(8);
        }
        // 顶栏菜单搜索框：输入实时过滤 sidebar 菜单
        layout.addMenuSearchBox();

        // 顶栏常驻「关于」入口（醒目，登录后任何时候可见）
        layout.addTopMenu("ⓘ 关于", () -> BsAboutDialog.show(stage, skin, "bs-ui Admin", false));

        // 内容区：把 scrollWrap 放进 contentWrap（圆角卡片），留 8px 内边距让内容不贴卡片边缘
        layout.setContent(scrollWrap);
        layout.contentPadding(8);

        // 用户区
        layout.setUserInfo(AdminContext.get().getCurrentUser(), null);
        layout.addUserMenuItem("个人中心", () ->
                BsToast.show(stage, skin, "个人中心为示例占位", BsToast.Variant.INFO));
        layout.addUserMenuItem("设置", this::showSettingsModal);
        // 主题切换：三选一（Light / Dark / Admin）
        layout.addUserMenuItem("☀ Light", () -> switchTheme(BsLightTheme.INSTANCE));
        layout.addUserMenuItem("🌙 Dark", () -> switchTheme(BsDarkTheme.INSTANCE));
        layout.addUserMenuItem("🛡 Admin", () -> switchTheme(BsAdminTheme.INSTANCE));
        layout.addUserMenuItem("ⓘ 关于 bs-ui", () -> BsAboutDialog.show(stage, skin, "bs-ui Admin", false));
        layout.addUserMenuItem("退出登录", this::logout);

        // 注册内置模块
        register(new DashboardModule());
        // 用户管理下三子项（用户实现，角色/权限占位）
        register(new UserListModule());
        registerPlaceholder("用户管理/角色", "角色管理为示例占位，未实现");
        registerPlaceholder("用户管理/权限", "权限管理为示例占位，未实现");
        // UI 模块：注册 48 个二级菜单（BsControlsSkinScreen 全部控件演示）作为「UI 模块」下的叶子
        // 注意：不单独 register(new UiDemoModule())，否则会出现两个「UI 模块」一级标题
        //       （register 单段 path 会建一个叶子，registerAll 又会建一个同名 root）
        UiDemoModule.registerAll(this);
        // 注入真实 stage，让依赖 stage 的弹窗（Modal/Dialogs/Pickers...）在 UI 模块里正常工作
        UiDemoModule.bindStage(stage);
        register(new BusinessDemoModule());

        // 重建侧边栏（注册完后）
        layout.rebuildSidebar();

        // 应用顶部栏 + 侧边栏样式（深色侧边栏白字 + 放大字号 + 折叠按钮）
        applySidebarStyle();

        stage.addActor(layout);

        // 默认进入首页
        navigate(DashboardModule.PATH);
    }

    // ========================= 注册 =========================

    /**
     * 注册一个模块。会按 path 拆段挂到侧边栏菜单树。
     */
    public void register(AdminModule module) {
        modules.add(module);
        pathIndex.put(module.getPath(), module);
        addModuleToSidebar(module.getPath(), () -> navigate(module.getPath()));
    }

    /**
     * 注册一个占位项（点击弹 Toast）。用于"角色/权限"这种只展示菜单结构的演示。
     */
    public void registerPlaceholder(String path, String toastMsg) {
        pathIndex.put(path, null); // 占位标记
        addModuleToSidebar(path, () ->
                BsToast.show(stage, skin, toastMsg, BsToast.Variant.WARNING));
    }

    /** 按 path 拆段，挂到侧边栏（支持任意层级）。 */
    private void addModuleToSidebar(String path, Runnable onClick) {
        String[] segs = path.split("/");
        if (segs.length == 1) {
            layout.addSideMenu(segs[0], onClick);
            return;
        }
        // 取或建一级 root（默认折叠：只显示一级标题，点开才展开子项）
        BsLayoutAdmin.SidebarItem root = rootSidebarMirror.get(segs[0]);
        if (root == null) {
            root = new BsLayoutAdmin.SidebarItem(segs[0]);
            root.expanded = false;
            rootSidebarMirror.put(segs[0], root);
            layout.addSideMenuTree(root);
        }
        // 逐层往下找/建（用累计路径作镜像 key）
        BsLayoutAdmin.SidebarItem parent = root;
        StringBuilder cumPath = new StringBuilder(segs[0]);
        for (int i = 1; i < segs.length; i++) {
            cumPath.append('/').append(segs[i]);
            boolean isLeaf = (i == segs.length - 1);
            if (isLeaf) {
                parent.addChild(segs[i], onClick);
                break;
            }
            // 中间层：先查现有
            BsLayoutAdmin.SidebarItem child = rootSidebarMirror.get(cumPath.toString());
            if (child == null) {
                // 新建中间层（onClick=null，点击时 core 会自动展开/折叠）
                parent.addChild(segs[i], null);
                child = parent.children.get(parent.children.size() - 1);
                child.expanded = true;
                rootSidebarMirror.put(cumPath.toString(), child);
            }
            parent = child;
        }
        layout.rebuildSidebar();
    }

    // ========================= 导航 =========================

    /** 跳转到指定 path 的模块（更新面包屑 + 内容区）。 */
    public void navigate(String path) {
        this.currentPath = path;
        AdminModule module = pathIndex.get(path);

        // 更新面包屑：首页 › seg1 › seg2 ...
        breadcrumb.clearItems();
        breadcrumb.addRoot("首页", () -> navigate(DashboardModule.PATH));
        String[] segs = path.split("/");
        for (int i = 0; i < segs.length; i++) {
            final String segPath = joinSegs(segs, i);
            if (i == segs.length - 1) {
                breadcrumb.addCurrent(segs[i]);
            } else {
                final String p = segPath;
                breadcrumb.addItem(segs[i], () -> navigate(p));
            }
        }

        // 内容区
        Actor view;
        if (module == null) {
            // 占位项：导航不会进来（占位点击直接 Toast），但防御性兜底
            view = placeholderView(path);
        } else {
            try {
                view = module.buildView(this);
            } catch (Throwable t) {
                log.error("buildView 失败 path={}", path, t);
                view = placeholderView(path);
            }
        }
        setContentActor(view);
    }

    private static String joinSegs(String[] segs, int includeUpTo) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= includeUpTo; i++) {
            if (i > 0) sb.append('/');
            sb.append(segs[i]);
        }
        return sb.toString();
    }

    /** 把 actor 包进 ScrollPane 塞进内容区（内容超出可纵向滚动）。 */
    private void setContentActor(Actor actor) {
        contentHost.clearChildren();
        contentHost.top().left();
        if (actor != null) {
            // growX 横向撑满；高度按内容（不 growY），让 ScrollPane 在内容高时能滚动
            contentHost.add(actor).growX().top().left();
        }
        // 用透明背景的 ScrollPane style，避免 default 的 bs-window-bg 白底盖住内容区主题色
        com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle spStyle =
                new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle();
        spStyle.hScroll = skin.getDrawable("bs-scrollpane-h-bar");
        spStyle.vScroll = skin.getDrawable("bs-scrollpane-v-bar");
        cn.pingyuanren.bs.ui.BsScrollPane scroll = new cn.pingyuanren.bs.ui.BsScrollPane(contentHost, skin);
        scroll.setStyle(spStyle);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setForceScroll(false, true);
        scrollWrap.setActor(scroll);
    }

    private Actor placeholderView(String path) {
        Table t = new Table();
        Label l = new Label("模块 [" + path + "] 暂未实现", skin);
        l.setColor(BsTheme.tm());
        t.add(l).pad(40).center();
        return t;
    }

    // ========================= 顶部菜单/用户菜单 =========================

    private void showSettingsModal() {
        BsModal modal = new BsModal("设置", skin);
        Table form = new Table(skin);
        form.pad(10);
        form.defaults().left().pad(6);

        form.add(new Label("外观与行为演示（不持久化）", skin)).colspan(2).padBottom(10).row();
        BsSwitch s1 = new BsSwitch(skin).setLabel("紧凑模式");
        BsSwitch s2 = new BsSwitch(skin).setLabel("显示侧边栏提示");
        BsSwitch s3 = new BsSwitch(skin).setLabel("开启通知");
        s1.setOnChange(b -> BsToast.show(stage, skin, "紧凑模式：" + (b ? "开" : "关"), BsToast.Variant.INFO));
        s2.setOnChange(b -> BsToast.show(stage, skin, "侧边栏提示：" + (b ? "开" : "关"), BsToast.Variant.INFO));
        s3.setOnChange(b -> BsToast.show(stage, skin, "通知：" + (b ? "开" : "关"), BsToast.Variant.INFO));
        form.add(s1).padRight(20);
        form.add(new Label("紧凑模式", skin)).row();
        form.add(s2).padRight(20);
        form.add(new Label("侧边栏提示", skin)).row();
        form.add(s3).padRight(20);
        form.add(new Label("通知", skin)).row();

        modal.content(form).contentWidth(420);
        modal.addButton("关闭", modal::close, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        modal.showModal(stage);
    }

    /**
     * 切换主题：若已是该主题则 Toast 提示，否则 setTheme 触发 AdminApp listener 重建 shell。
     */
    private void switchTheme(BsTheme target) {
        BsTheme current = BsUI.currentTheme();
        if (current == target) {
            BsToast.show(stage, skin, "已是" + targetDisplayName(target) + "主题", BsToast.Variant.INFO, 1500);
            return;
        }
        try {
            BsUI.setTheme(target);
        } catch (Throwable t) {
            log.warn("setTheme 失败", t);
        }
    }

    private static String targetDisplayName(BsTheme target) {
        if (target == BsLightTheme.INSTANCE) return "Light";
        if (target == BsDarkTheme.INSTANCE) return "Dark";
        if (target == BsAdminTheme.INSTANCE) return "Admin";
        return target.name();
    }

    private void logout() {
        AdminContext.get().logout();
        try {
            app.setScreen(new AdminLoginScreen(app));
        } catch (Throwable t) {
            log.error("退出登录跳转失败", t);
        }
    }

    // ========================= 生命周期 =========================

    /** 应用 sidebar/topBar 视觉样式：放大字号、折叠按钮。颜色全走主题（admin.json）。 */
    private void applySidebarStyle() {
        // 顶部栏：bg-header 底色 + 底部 1px 分隔线（隔离顶部与内容区）
        layout.getTopBar().setBackground(topBarBgDrawable());

        // 顶部栏：放大字号 + text-primary 深色字（只做一次，topBar 不重建）
        if (!topBarEnlarged) {
            styleActors(layout.getTopBar(), 1, BsTheme.tp());
            // 折叠按钮：换成"☰ 折叠"文字 + 加大点击区域（用命名查找，避免新增 hideTopBtn 后索引偏移）
            com.badlogic.gdx.scenes.scene2d.Actor toggleActor = layout.getTopBar().findActor("toggleSidebarBtn");
            if (toggleActor instanceof com.badlogic.gdx.scenes.scene2d.ui.TextButton) {
                com.badlogic.gdx.scenes.scene2d.ui.TextButton toggleBtn =
                        (com.badlogic.gdx.scenes.scene2d.ui.TextButton) toggleActor;
                toggleBtn.setText("☰  折叠");
                com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle tbs =
                        new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle(toggleBtn.getStyle());
                tbs.font = skin.getFont("font-lg");
                toggleBtn.setStyle(tbs);
                toggleBtn.setSize(110, 40);
            }
            topBarEnlarged = true;
        }
        // 侧边栏菜单：只放大字号，字色由 core setSidebarDarkStyle 控制（白字 + 按层级透明度）
        styleActors(layout.getSidebar(), 1, null);
        lastEnlargedSidebarButtonCount = currentSidebarButtonCount();
    }

    /**
     * 递归遍历 actor 子树，把 Label/TextButton 的字号升 {@code steps} 档（sm→md→lg→xl，到顶不再升）；
     * color 非 null 时同时设字色（sidebar 传 null 保留 core 按层级设的白字透明度）。
     *
     * <p>用字号档升级替代原来的 fontScale 倍数缩放，避免字体发虚。</p>
     */
    private void styleActors(com.badlogic.gdx.scenes.scene2d.Actor actor, int steps,
                             com.badlogic.gdx.graphics.Color color) {
        if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.Label) {
            com.badlogic.gdx.scenes.scene2d.ui.Label l = (com.badlogic.gdx.scenes.scene2d.ui.Label) actor;
            com.badlogic.gdx.graphics.g2d.BitmapFont f = bumpFont(l.getStyle().font, steps);
            if (f != null) {
                com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle ls =
                        new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(l.getStyle());
                ls.font = f;
                l.setStyle(ls);
            }
            if (color != null) l.setColor(color);
        } else if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.TextButton) {
            com.badlogic.gdx.scenes.scene2d.ui.TextButton b = (com.badlogic.gdx.scenes.scene2d.ui.TextButton) actor;
            com.badlogic.gdx.graphics.g2d.BitmapFont f = bumpFont(b.getLabel().getStyle().font, steps);
            if (f != null) {
                com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle ts =
                        new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle(b.getStyle());
                ts.font = f;
                b.setStyle(ts);
            }
            if (color != null) b.getLabel().setColor(color);
        }
        if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.Table) {
            com.badlogic.gdx.scenes.scene2d.ui.Table t = (com.badlogic.gdx.scenes.scene2d.ui.Table) actor;
            for (com.badlogic.gdx.scenes.scene2d.Actor child : t.getChildren()) {
                styleActors(child, steps, color);
            }
        }
    }

    /** 把当前 font 在 sm/md(default)/lg/xl 序列上升 {@code steps} 档；无法识别或已到顶返回 null（保持原样）。 */
    private com.badlogic.gdx.graphics.g2d.BitmapFont bumpFont(com.badlogic.gdx.graphics.g2d.BitmapFont cur, int steps) {
        com.badlogic.gdx.scenes.scene2d.ui.Skin sk = skin;
        java.util.List<String> order = java.util.Arrays.asList("font-sm", "default", "font-lg", "font-xl");
        for (int i = 0; i < order.size(); i++) {
            if (sk.has(order.get(i), com.badlogic.gdx.graphics.g2d.BitmapFont.class)
                    && sk.getFont(order.get(i)) == cur) {
                int target = Math.min(i + steps, order.size() - 1);
                return sk.getFont(order.get(target));
            }
        }
        return null;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // 清屏色走主题 bg-body（admin.json 为 #f0f2f5 浅灰）
        ScreenUtils.clear(BsTheme.bb());
        stage.act(delta);
        stage.draw();
        // core 内部点击分组/箭头会 rebuildSidebar，新建按钮字号回到默认，这里检测并重新放大
        int curCount = currentSidebarButtonCount();
        if (curCount != lastEnlargedSidebarButtonCount) {
            applySidebarStyle();
        }
    }

    /** 统计当前 sidebar 菜单按钮数（用于检测 core 重建）。 */
    private int currentSidebarButtonCount() {
        com.badlogic.gdx.scenes.scene2d.Actor sidebar = layout.getSidebar();
        if (!(sidebar instanceof com.badlogic.gdx.scenes.scene2d.ui.Table)) return 0;
        com.badlogic.gdx.utils.Array<com.badlogic.gdx.scenes.scene2d.Actor> kids =
                ((com.badlogic.gdx.scenes.scene2d.ui.Table) sidebar).getChildren();
        if (kids.size == 0) return 0;
        // sidebar 第一个子是 menuScroll（ScrollPane），它包着 sidebarMenuList
        com.badlogic.gdx.scenes.scene2d.Actor first = kids.first();
        if (first instanceof com.badlogic.gdx.scenes.scene2d.ui.ScrollPane) {
            com.badlogic.gdx.scenes.scene2d.Actor inner = ((com.badlogic.gdx.scenes.scene2d.ui.ScrollPane) first).getActor();
            if (inner instanceof com.badlogic.gdx.scenes.scene2d.ui.Table) {
                return ((com.badlogic.gdx.scenes.scene2d.ui.Table) inner).getChildren().size;
            }
        }
        // 兼容旧结构（直接是 Table）
        if (first instanceof com.badlogic.gdx.scenes.scene2d.ui.Table) {
            return ((com.badlogic.gdx.scenes.scene2d.ui.Table) first).getChildren().size;
        }
        return 0;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        // 主题切换会重建 BsAdminShell（见 AdminApp.applyTheme），旧 shell dispose 时
        // 清空 UiDemoModule 的内容工厂缓存，避免新 shell 复用旧 skin 导致配色/字体错乱
        UiDemoModule.resetFactory();
        stage.dispose();
    }

    /** 构造顶部栏背景 drawable：bg-header 底色 + 底部 1px border 色分隔线。 */
    private static com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable topBarBgDrawable() {
        com.badlogic.gdx.graphics.Color headerBg = BsTheme.bhH();   // bg-header（admin.json 白）
        com.badlogic.gdx.graphics.Color line = BsTheme.bd();        // border（admin.json 浅灰）
        int w = 4, h = 4;
        com.badlogic.gdx.graphics.Pixmap pix = new com.badlogic.gdx.graphics.Pixmap(w, h,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pix.setColor(headerBg);
        pix.fill();
        pix.setColor(line);
        pix.drawRectangle(0, 0, w, 1);
        com.badlogic.gdx.graphics.Texture tex = new com.badlogic.gdx.graphics.Texture(pix);
        pix.dispose();
        // NinePatch：left=0 right=0 top=3 bottom=1 —— 只有顶部 3px 和底部 1px 不拉伸
        // 拉伸时白底上下扩展，底部 1px 线保持 1px 贴在底边
        com.badlogic.gdx.graphics.g2d.NinePatch np = new com.badlogic.gdx.graphics.g2d.NinePatch(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(tex), 0, 0, 3, 1);
        return new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(np);
    }
}
