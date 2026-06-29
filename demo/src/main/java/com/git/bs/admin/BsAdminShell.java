package com.git.bs.admin;

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
import com.git.bs.admin.modules.BusinessDemoModule;
import com.git.bs.admin.modules.DashboardModule;
import com.git.bs.admin.modules.UiDemoModule;
import com.git.bs.admin.modules.UserListModule;
import com.git.bs.game.AdminApp;
import com.git.bs.ui.BsBreadcrumb;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsLayoutAdmin;
import com.git.bs.ui.BsModal;
import com.git.bs.ui.BsSwitch;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsToast;
import com.git.bs.ui.BsAdminTheme;
import com.git.bs.ui.BsDarkTheme;
import com.git.bs.ui.BsUI;
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
        // 内容区背景走主题 bg-body（admin.json 为 #f0f2f5 浅灰）
        scrollWrap.setBackground(com.git.bs.ui.BsUI.drawableOf(BsTheme.bb()));

        // 布局骨架
        layout = new BsLayoutAdmin(skin);
        layout.setLogo("bs-ui Admin");
        layout.setFillParent(true);
        // 放大侧边栏宽度（默认 180 偏窄，三层菜单缩进+长文字会截断）—— 280 容下"图形UI/Hover 数据查看"
        layout.setSidebarWidth(280);
        // 启用深色侧边栏风格（菜单字色走 text-on-dark 白字，配 admin 主题深蓝灰侧边栏）
        layout.setSidebarDarkStyle(true);
        // 放大顶部栏高度（默认 48 偏矮，字挤）
        layout.getCells().get(0).height(60);

        // 面包屑（插到 topBar 中：logo 之后、topMenuRow 之前由 cell 顺序决定，
        // 这里直接 add 到 topBar 末尾会落在 topMenuRow 之后，仍可见且简单可靠）
        breadcrumb = new BsBreadcrumb(skin);

        // 组装：topBar 顺序是 toggleBtn / logoLabel / topMenuRow(growX) / userInfoRow
        // 我们把 breadcrumb 插在 topMenuRow 之前：先清掉 topMenuRow 占位再重建顺序较麻烦，
        // 简单做法 —— 直接复用 topMenuRow 作为面包屑容器（topMenuRow 默认 growX 左对齐）
        Table topMenuRow = layout.getTopBar().getChildren().size >= 4
                ? (Table) layout.getTopBar().getChildren().get(2)
                : null;
        if (topMenuRow != null) {
            topMenuRow.add(breadcrumb).left().padLeft(8);
        }

        // 内容区：把 scrollWrap 作为内容（contentPadding=0 避免露出 contentWrap 的白边）
        layout.setContent(scrollWrap);
        layout.contentPadding(0);

        // 用户区
        layout.setUserInfo(AdminContext.get().getCurrentUser(), null);
        layout.addUserMenuItem("个人中心", () ->
                BsToast.show(stage, skin, "个人中心为示例占位", BsToast.Variant.INFO));
        layout.addUserMenuItem("设置", this::showSettingsModal);
        // 主题切换项：根据当前主题显示反向文案
        layout.addUserMenuItem(themeMenuItemText(), this::toggleTheme);
        layout.addUserMenuItem("退出登录", this::logout);

        // 注册内置模块
        register(new DashboardModule());
        // 用户管理下三子项（用户实现，角色/权限占位）
        register(new UserListModule());
        registerPlaceholder("用户管理/角色", "角色管理为示例占位，未实现");
        registerPlaceholder("用户管理/权限", "权限管理为示例占位，未实现");
        // UI 模块：注册"UI 模块"分组入口 + 三类子菜单（通用UI/业务UI/图形UI，共 37 个控件演示）
        register(new UiDemoModule());
        UiDemoModule.registerAll(this);
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
        // 取或建一级 root
        BsLayoutAdmin.SidebarItem root = rootSidebarMirror.get(segs[0]);
        if (root == null) {
            root = new BsLayoutAdmin.SidebarItem(segs[0]);
            root.expanded = true;
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
        com.git.bs.ui.BsScrollPane scroll = new com.git.bs.ui.BsScrollPane(contentHost, skin);
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

    private String themeMenuItemText() {
        return "admin".equals(BsUI.currentThemeName()) ? "切换到 Dark 主题" : "切换到 Admin 主题";
    }

    private void toggleTheme() {
        // 在 admin ↔ dark 之间切换（两者都在 AdminApp 注册过）
        BsTheme next = "admin".equals(BsUI.currentThemeName())
                ? BsDarkTheme.INSTANCE
                : BsAdminTheme.INSTANCE;
        try {
            BsUI.setTheme(next);
        } catch (Throwable t) {
            log.warn("setTheme 失败", t);
        }
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
        // 内容区背景：bg-body（admin.json #f0f2f5）
        scrollWrap.setBackground(com.git.bs.ui.BsUI.drawableOf(BsTheme.bb()));

        // 顶部栏：放大字号 + text-primary 深色字（只做一次，topBar 不重建）
        if (!topBarEnlarged) {
            styleActors(layout.getTopBar(), 1.25f, BsTheme.tp());
            // 折叠按钮：换成"☰ 折叠"文字 + 加大点击区域
            com.badlogic.gdx.scenes.scene2d.Actor first = layout.getTopBar().getChildren().first();
            if (first instanceof com.badlogic.gdx.scenes.scene2d.ui.TextButton) {
                com.badlogic.gdx.scenes.scene2d.ui.TextButton toggleBtn =
                        (com.badlogic.gdx.scenes.scene2d.ui.TextButton) first;
                toggleBtn.setText("☰  折叠");
                toggleBtn.getLabel().setFontScale(1.2f);
                toggleBtn.setSize(110, 40);
            }
            topBarEnlarged = true;
        }
        // 侧边栏菜单：只放大字号，字色由 core setSidebarDarkStyle 控制（白字 + 按层级透明度）
        styleActors(layout.getSidebar(), 1.2f, null);
        lastEnlargedSidebarButtonCount = currentSidebarButtonCount();
    }

    /**
     * 递归遍历 actor 子树，把 Label/TextButton 的 fontScale 乘以 factor；
     * color 非 null 时同时设字色（sidebar 传 null 保留 core 按层级设的白字透明度）。
     */
    private void styleActors(com.badlogic.gdx.scenes.scene2d.Actor actor, float factor,
                             com.badlogic.gdx.graphics.Color color) {
        if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.Label) {
            com.badlogic.gdx.scenes.scene2d.ui.Label l = (com.badlogic.gdx.scenes.scene2d.ui.Label) actor;
            l.setFontScale(l.getFontScaleX() * factor);
            if (color != null) l.setColor(color);
        } else if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.TextButton) {
            com.badlogic.gdx.scenes.scene2d.ui.TextButton b = (com.badlogic.gdx.scenes.scene2d.ui.TextButton) actor;
            b.getLabel().setFontScale(b.getLabel().getFontScaleX() * factor);
            if (color != null) b.getLabel().setColor(color);
        }
        if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.Table) {
            com.badlogic.gdx.scenes.scene2d.ui.Table t = (com.badlogic.gdx.scenes.scene2d.ui.Table) actor;
            for (com.badlogic.gdx.scenes.scene2d.Actor child : t.getChildren()) {
                styleActors(child, factor, color);
            }
        }
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
