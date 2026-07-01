package com.git.bs.demo.modules;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.git.bs.ui.BsAboutDialog;
import com.git.bs.ui.BsBadge;
import com.git.bs.ui.BsBadgeButton;
import com.git.bs.ui.BsBreadcrumb;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsLayoutAdmin;
import com.git.bs.ui.BsModal;
import com.git.bs.ui.BsNavbar;
import com.git.bs.ui.BsOffcanvas;
import com.git.bs.ui.BsPagination;
import com.git.bs.ui.BsProfileCard;
import com.git.bs.ui.BsProfilePanel;
import com.git.bs.ui.BsScrollPane;
import com.git.bs.ui.BsTable;
import com.git.bs.ui.BsTree;
import com.git.bs.ui.BsIcon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.git.bs.demo.modules.ModuleSupport.*;

/**
 * 数据展示模块组：Tree / Table / Badge / Breadcrumb / Cards / Profile / Layout / NavbarOffcanvas。
 *
 * <p>{@code demoLayout} 持有 Layout 模块状态（用于切换 sidebar 内容）。</p>
 */
public class BsDataModules {

    private final Skin skin;
    private final Stage stage;
    private final Consumer<String> setStatus;

    private BsLayoutAdmin demoLayout;  // Layout 模块状态

    public BsDataModules(Skin skin, Stage stage, Consumer<String> setStatus) {
        this.skin = skin;
        this.stage = stage;
        this.setStatus = setStatus;
    }

    // ============================ Tree ============================
    public void fillTree(Table c) {
        c.add(sectionTitle(skin, "Tree  —— 树状列表（节点展开/折叠）")).row();

        c.add(new Label("点击 ▸/▾ 切换展开，点击文字同样可切换:", skin)).padBottom(8).row();

        BsTree tree = new BsTree(skin);
        BsTree.Node root = tree.root("项目根目录");
        BsTree.Node src = root.addChild("src/");
        src.setExpanded(true);
        src.addChild("Main.java");
        src.addChild("Utils.java");
        BsTree.Node res = root.addChild("res/");
        res.setExpanded(true);
        BsTree.Node imgs = res.addChild("images/");
        imgs.addChild("logo.png");
        imgs.addChild("bg.jpg");
        res.addChild("sounds/");
        res.addChild("config.json");
        root.addChild("README.md");
        root.addChild(".gitignore");
        root.setExpanded(true);
        tree.refresh();

        tree.setOnNodeClick(n -> setStatus.accept("点击节点: " + n.getText()
                + (n.getChildren().isEmpty() ? " (叶子)" : " (有 " + n.getChildren().size() + " 子节点)")));

        BsScrollPane treeScroll = new BsScrollPane(tree, skin);
        treeScroll.setFadeScrollBars(false);
        c.add(treeScroll).growX().height(280).row();
    }

    // ============================ Table + Pagination ============================
    public void fillTable(Table c) {
        c.add(sectionTitle(skin, "Table + Pagination  —— 表格 + 分页")).row();

        c.add(new Label("点击表头列触发排序回调，点击行触发行回调:", skin)).padBottom(8).row();

        List<String> headers = Arrays.asList("ID", "姓名", "年龄", "状态");
        List<List<String>> allRows = new ArrayList<>();
        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十",
                "郑十一", "王十二", "冯十三", "陈十四", "褚十五", "卫十六", "蒋十七",
                "沈十八", "韩十九", "杨二十", "朱二一", "秦二二", "尤二三", "许二四",
                "何二五", "吕二六", "施二七"};
        String[] statuses = {"active", "inactive", "pending"};
        for (int i = 0; i < 25; i++) {
            allRows.add(Arrays.asList(
                    String.valueOf(i + 1),
                    names[i % names.length],
                    String.valueOf(20 + (i * 3) % 40),
                    statuses[i % statuses.length]
            ));
        }

        BsTable table = new BsTable(skin);
        table.setHeaders(headers);
        table.setColWidth(100);
        table.setOnRowClick(row -> setStatus.accept("点击行 #" + (row + 1) + ": " + table.getRow(row)));
        table.setOnHeaderClick(col -> setStatus.accept("点击表头列 #" + col + "（业务方决定升降序）"));

        BsPagination pagination = new BsPagination(skin);
        int pageSize = 10;
        pagination.setTotalPages((int) Math.ceil(allRows.size() * 1.0 / pageSize));
        pagination.setCurrentPage(1);
        pagination.setOnChange(page -> {
            int from = (page - 1) * pageSize;
            int to = Math.min(from + pageSize, allRows.size());
            table.setData(allRows.subList(from, to));
            setStatus.accept("切到第 " + page + " 页");
        });

        int from = 0;
        int to = Math.min(pageSize, allRows.size());
        table.setData(allRows.subList(from, to));

        BsScrollPane tableScroll = new BsScrollPane(table, skin);
        tableScroll.setFadeScrollBars(false);
        c.add(tableScroll).growX().height(280).row();
        c.add(pagination).padTop(8).row();
        c.add(new Label("(共 " + allRows.size() + " 行，每页 " + pageSize + "，共 "
                + pagination.getTotalPages() + " 页)", skin)).padTop(4).row();
    }

    // ============================ Badge ============================
    public void fillBadge(Table c) {
        c.add(sectionTitle(skin, "Badge  —— 徽标（消息/数量）")).row();

        c.add(new Label("独立 Badge（6 色 Variant）:", skin)).padTop(8).left().row();
        Table row1 = new Table();
        row1.defaults().pad(6);
        for (BsBadge.Variant v : BsBadge.Variant.values()) {
            row1.add(new BsBadge(v.name(), skin, v));
        }
        c.add(row1).left().row();

        c.add(new Label("带 Badge 的按钮（消息数量红点）:", skin)).padTop(14).left().row();
        Table row2 = new Table();
        row2.defaults().pad(10);

        BsBadgeButton msgBtn = new BsBadgeButton("消息", skin, BsButton.Variant.PRIMARY);
        msgBtn.setBadge(5);
        msgBtn.setOnClick(() -> setStatus.accept("点开消息"));
        msgBtn.pack();
        row2.add(msgBtn);

        BsBadgeButton cartBtn = new BsBadgeButton("购物车", skin, BsButton.Variant.SUCCESS);
        cartBtn.setBadge(12);
        cartBtn.setOnClick(() -> setStatus.accept("点开购物车"));
        cartBtn.pack();
        row2.add(cartBtn);

        BsBadgeButton notifBtn = new BsBadgeButton("通知", skin, BsButton.Variant.WARNING);
        notifBtn.setBadge("99+");
        notifBtn.setOnClick(() -> setStatus.accept("点开通知"));
        notifBtn.pack();
        row2.add(notifBtn);

        BsBadgeButton inboxBtn = new BsBadgeButton("收件箱", skin, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.MD);
        inboxBtn.setBadge(0);
        inboxBtn.setOnClick(() -> setStatus.accept("点开收件箱"));
        inboxBtn.pack();
        row2.add(inboxBtn);

        c.add(row2).left().row();
        c.add(new Label("(收件箱 badge=0 自动隐藏；其他按钮可点击)", skin)).padTop(4).row();
    }

    // ============================ Breadcrumb ============================
    public void fillBreadcrumb(Table c) {
        c.add(sectionTitle(skin, "Breadcrumb  —— 面包屑导航")).row();

        c.add(new Label("Bootstrap 风格面包屑，每段可点击，最后一段为当前页（深色不可点）:",
                skin)).padTop(6).left().row();

        BsBreadcrumb bc1 = new BsBreadcrumb(skin)
                .addItem("首页", () -> setStatus.accept("面包屑: 首页"))
                .addItem("用户列表", () -> setStatus.accept("面包屑: 用户列表"))
                .addItem("详情", () -> setStatus.accept("面包屑: 详情"))
                .addCurrent("张三");
        c.add(bc1).growX().padTop(8).left().row();

        BsBreadcrumb bc2 = new BsBreadcrumb(skin)
                .addItem("Home", () -> setStatus.accept("bc: Home"))
                .addItem("Products", () -> setStatus.accept("bc: Products"))
                .addItem("Electronics", () -> setStatus.accept("bc: Electronics"))
                .addCurrent("Smartphone X12");
        c.add(bc2).growX().padTop(8).left().row();

        c.add(new Label("(点击前置段触发回调；最后一段为当前页不可点)", skin)).padTop(8).row();
    }

    // ============================ Cards ============================
    public void fillCards(Table c) {
        c.add(sectionTitle(skin, "Cards  —— 卡片（图片 + 文字）")).row();

        c.add(new Label("Bootstrap 风格卡片：图片 + 标题 + 副标题 + 正文 + 页脚按钮。",
                skin)).padTop(6).left().row();

        // 1. 垂直布局
        c.add(new Label("垂直布局（顶部图片）:", skin)).padTop(14).left().row();
        try {
            Drawable img1 = BsModal.drawableFromPath("bs/test/img/20251110013443.png");
            com.git.bs.ui.BsCard card1 = new com.git.bs.ui.BsCard(skin)
                    .image(img1)
                    .imageSize(0, 130)
                    .title("项目卡片")
                    .subtitle("用户上传 · 2025-11-10")
                    .body("这是一段卡片正文，介绍这个项目的内容。卡片自动撑满父容器宽度，文字会自动换行。")
                    .footerLink("查看详情", () -> setStatus.accept("点了 查看详情"))
                    .footerButton("收藏", () -> setStatus.accept("点了 收藏"),
                            BsButton.Variant.WARNING, BsButton.Style.SOLID);
            c.add(card1).width(360).growX().row();
        } catch (Throwable t) {
            c.add(new Label("(图片加载失败: " + t.getMessage() + ")", skin)).row();
        }

        // 2. 水平布局
        c.add(new Label("水平布局（左图右文）:", skin)).padTop(14).left().row();
        try {
            Drawable img2 = BsModal.drawableFromPath("bs/test/img/20251109230728.png");
            com.git.bs.ui.BsCard card2 = new com.git.bs.ui.BsCard(skin)
                    .orientation(com.git.bs.ui.BsCard.Orientation.HORIZONTAL)
                    .image(img2)
                    .imageSize(120, 100)
                    .title("通知中心")
                    .body("您有 3 条未读消息。")
                    .footerLink("查看全部", () -> setStatus.accept("查看全部消息"));
            c.add(card2).width(420).growX().row();
        } catch (Throwable t) {
            c.add(new Label("(图片加载失败: " + t.getMessage() + ")", skin)).row();
        }

        // 3. 无图卡片
        c.add(new Label("无图卡片（纯文字 + 链接）:", skin)).padTop(14).left().row();
        com.git.bs.ui.BsCard card3 = new com.git.bs.ui.BsCard(skin)
                .title("关于 Bs UI 框架")
                .subtitle("v0.2 · Bootstrap 风格")
                .body("Bs UI 是一套基于 libgdx scene2d 自制的 Bootstrap 风格 UI 框架，包含按钮、表单、卡片、模态框、对话框等组件。")
                .footerLink("GitHub", () -> setStatus.accept("点了 GitHub"))
                .footerLink("文档", () -> setStatus.accept("点了 文档"));
        c.add(card3).width(420).growX().row();

        c.add(new Label("(卡片背景用 bs-window-bg 圆角白底；图片来自 assets/bs/test/img)",
                skin)).padTop(8).row();
    }

    // ============================ Profile ============================
    public void fillProfile(Table c) {
        c.add(sectionTitle(skin, "Profile  —— 个人信息面板（两种风格）")).row();

        Drawable avatar = null;
        try {
            avatar = BsModal.drawableFromPath("bs/test/img/20251121200555.png");
        } catch (Throwable t) {
            c.add(new Label("(头像加载失败: " + t.getMessage() + ")", skin)).row();
        }

        c.add(new Label("1. BsProfilePanel —— 横向信息（方形头像）:", skin)).padTop(8).left().row();
        BsProfilePanel panel = new BsProfilePanel(skin)
                .avatar(avatar)
                .avatarSize(72, 72)
                .name("authorZhao")
                .handle("@author_zhao · libgdx 开发者")
                .role("超级管理员")
                .bio("专注于 libgdx 游戏开发，热爱自制 UI 框架。当前正在打造 Bootstrap 风格 Bs UI 库。")
                .stat("帖子", "128")
                .stat("关注", "1.2k")
                .stat("粉丝", "5.6k")
                .actionButton("关注", () -> setStatus.accept("关注了 authorZhao"), BsButton.Variant.PRIMARY)
                .actionButton("私信", () -> setStatus.accept("打开私信"),
                        BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        c.add(panel).width(500).growX().padTop(4).row();

        c.add(new Label("2. BsProfileCard —— 圆形头像 + 居中卡片:", skin)).padTop(16).left().row();
        BsProfileCard card = new BsProfileCard(skin)
                .avatar(avatar)
                .avatarSize(96)
                .name("authorZhao")
                .handle("@author_zhao")
                .role("管理员")
                .bio("专注于 libgdx 开发，热爱自制 UI。")
                .stat("帖子", "128")
                .stat("关注", "1.2k")
                .stat("粉丝", "5.6k");
        c.add(card).width(380).padTop(4).row();

        c.add(new Label("(BsProfileCard 用 makeRoundDrawable 强制剪裁头像为圆形)",
                skin)).padTop(8).row();
    }

    // ============================ Layout ============================
    public void fillLayout(Table c) {
        c.add(sectionTitle(skin, "Layout  —— 管理后台布局（树状 sidebar + 折叠 + 用户下拉）")).row();

        c.add(new Label("顶部 ☰ 按钮折叠/展开 sidebar；左侧支持多级树状菜单；右上角用户区点击弹下拉。",
                skin)).padTop(6).left().row();

        demoLayout = new BsLayoutAdmin(skin);
        demoLayout.setLogo("Bs Admin");
        demoLayout.addTopMenu("首页", () -> setStatus.accept("顶部: 首页"));
        demoLayout.addTopMenu("文档", () -> setStatus.accept("顶部: 文档"));
        demoLayout.addTopMenu("关于", () -> BsAboutDialog.show(stage, skin, "Bs UI 控件测试台", false));

        demoLayout.setUserInfo("管理员", null);
        demoLayout.addUserMenuItem("个人中心", () -> setStatus.accept("用户菜单: 个人中心"));
        demoLayout.addUserMenuItem("设置", () -> setStatus.accept("用户菜单: 设置"));
        demoLayout.addUserMenuItem("退出登录", () -> setStatus.accept("用户菜单: 退出登录"));

        BsLayoutAdmin.SidebarItem dash =
                new BsLayoutAdmin.SidebarItem("仪表盘", () -> switchLayoutContent("仪表盘"));
        demoLayout.addSideMenuTree(dash);

        BsLayoutAdmin.SidebarItem userMgmt = new BsLayoutAdmin.SidebarItem("用户管理");
        userMgmt.expanded = true;
        userMgmt.addChild("用户列表", () -> switchLayoutContent("用户 → 列表"));
        userMgmt.addChild("角色管理", () -> switchLayoutContent("用户 → 角色"));
        userMgmt.addChild("权限设置", () -> switchLayoutContent("用户 → 权限"));
        demoLayout.addSideMenuTree(userMgmt);

        BsLayoutAdmin.SidebarItem order = new BsLayoutAdmin.SidebarItem("订单系统");
        order.addChild("待发货", () -> switchLayoutContent("订单 → 待发货"));
        order.addChild("已完成", () -> switchLayoutContent("订单 → 已完成"));
        order.addChild("退款", () -> switchLayoutContent("订单 → 退款"));
        demoLayout.addSideMenuTree(order);

        demoLayout.addSideMenu("设置", () -> switchLayoutContent("设置"));

        switchLayoutContent("仪表盘");

        c.add(demoLayout).growX().height(420).padTop(8).row();
        c.add(new Label("(顶部 ☰ 折叠 sidebar；点击有子项的菜单文字也会展开/折叠)",
                skin)).padTop(4).row();
    }

    private void switchLayoutContent(String name) {
        if (demoLayout == null) return;
        Table page = new Table();
        page.pad(10).left().top();
        page.defaults().left().pad(4);
        Label title = new Label("【" + name + "】页面", skin);
        title.setFontScale(1.4f);
        page.add(title).row();
        page.add(new Label("这是 " + name + " 的演示内容。点击左侧 sidebar 切换不同模块。", skin)).padTop(8).row();
        page.add(new Label("• 数据 1", skin)).padTop(4).row();
        page.add(new Label("• 数据 2", skin)).row();
        page.add(new Label("• 数据 3", skin)).row();
        setStatus.accept("Layout: " + name);
        demoLayout.setContent(page);
    }

    // ============================ Navbar & Offcanvas ============================
    public void fillNavbarOffcanvas(Table c) {
        c.add(sectionTitle(skin, "Navbar & Offcanvas  —— 导航栏 / 侧滑抽屉")).row();

        c.add(new Label("BsNavbar 顶部导航栏（logo + 菜单 + 操作区 + 搜索）:", skin)).padTop(8).left().row();
        BsNavbar navbar = new BsNavbar(skin);
        navbar.setBrand("MyApp");
        Drawable logoIcon = BsIcon.get("house");
        if (logoIcon != null) navbar.setLogo(logoIcon);
        navbar.addMenuItem("文件", menu -> {
            menu.addItem("新建", () -> setStatus.accept("Navbar: 文件 → 新建"));
            menu.addItem("打开...", () -> setStatus.accept("Navbar: 文件 → 打开"));
            menu.addSeparator();
            menu.addItem("退出", () -> setStatus.accept("Navbar: 文件 → 退出"));
        });
        navbar.addMenuItem("编辑", menu -> {
            menu.addItem("撤销", () -> setStatus.accept("Navbar: 编辑 → 撤销"));
            menu.addItem("重做", () -> setStatus.accept("Navbar: 编辑 → 重做"));
        });
        navbar.addMenuItem("帮助", menu -> {
            menu.addItem("关于", () -> BsAboutDialog.show(stage, skin, "Bs UI 控件测试台", false));
        });
        navbar.addAction("设置", () -> setStatus.accept("Navbar: 设置"), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        navbar.addSearchField("搜索内容...");
        navbar.showBottomBorder(true);
        c.add(navbar).growX().padTop(4).row();

        c.add(new Label("BsOffcanvas 侧滑抽屉（左/右/上/下 4 方向）:", skin)).padTop(16).left().row();
        Table drawerRow = new Table();
        drawerRow.defaults().pad(4);
        drawerRow.add(drawerBtn(skin, stage, setStatus, "从左滑入", BsOffcanvas.Placement.LEFT, 320, 0));
        drawerRow.add(drawerBtn(skin, stage, setStatus, "从右滑入", BsOffcanvas.Placement.RIGHT, 320, 0));
        drawerRow.add(drawerBtn(skin, stage, setStatus, "从上滑入", BsOffcanvas.Placement.TOP, 0, 220));
        drawerRow.add(drawerBtn(skin, stage, setStatus, "从下滑入", BsOffcanvas.Placement.BOTTOM, 0, 220));
        c.add(drawerRow).left().row();

        c.add(new Label("(点击按钮弹出抽屉，点遮罩或 × 关闭)", skin)).padTop(8).row();
    }
}
