package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.i18n.BsI18n;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台布局：
 * <ul>
 *   <li>顶部栏：Logo + 顶部菜单 + 用户区（点击弹出下拉菜单）</li>
 *   <li>左上角折叠按钮：点击让 sidebar 宽度动画过渡（展开/收起）</li>
 *   <li>侧边栏：支持多级树状菜单（SidebarItem.children 递归），每项可展开/折叠</li>
 *   <li>主内容区：{@link #setContent(Actor)}</li>
 * </ul>
 */
@Slf4j
public class BsLayoutAdmin extends Table {

    /** 侧边栏菜单项（树状）。 */
    public static class SidebarItem {
        public final String text;
        public final Runnable onClick;
        public final List<SidebarItem> children = new ArrayList<>();
        public boolean expanded;
        public SidebarItem(String text) { this(text, null); }
        public SidebarItem(String text, Runnable onClick) { this.text = text; this.onClick = onClick; }
        public SidebarItem addChild(String text, Runnable onClick) {
            SidebarItem c = new SidebarItem(text, onClick);
            children.add(c);
            return this;
        }
        public SidebarItem addChild(String text) { return addChild(text, null); }
        public boolean isLeaf() { return children.isEmpty(); }
    }

    @Getter private final Table topBar;
    @Getter private final Table sidebarWrap;       // sidebar 外层（控制宽度）
    @Getter private final Table sidebar;            // sidebar 内容容器
    @Getter private final Table content;
    private final Container<Actor> contentWrap;
    private final Label logoLabel;
    private final Table topMenuRow;
    private final Table userInfoRow;
    private final Table sidebarMenuList;
    /** sidebar 菜单的 ScrollPane（setSidebarWidth 时同步更新宽度 cell）。 */
    private com.badlogic.gdx.scenes.scene2d.ui.ScrollPane menuScroll;

    /** sidebar 当前/目标宽度。 */
    private float sidebarExpandedW = 180;
    private float sidebarCollapsedW = 0;
    private boolean sidebarCollapsed = false;
    /** 折叠/展开动画时长。 */
    private static final float COLLAPSE_DURATION = 0.25f;

    /** 菜单文字超长时 hover 显示全名的 tooltip 列表（每个菜单按钮一个，rebuild 时清理重建）。 */
    private final java.util.List<BsTooltip> menuTooltips = new java.util.ArrayList<>();
    /** 菜单过滤词（顶栏搜索框输入）；非空时 sidebar 只显示包含该词的项。 */
    private String menuFilter = "";

    /** 用户下拉菜单选项。 */
    public static class UserMenuItem {
        public final String text;
        public final Runnable action;
        public UserMenuItem(String t, Runnable r) { text = t; action = r; }
    }
    private final List<UserMenuItem> userMenuItems = new ArrayList<>();

    /** 所有顶层 sidebar 项（用于切换选中态）。 */
    private final List<TextButton> topSidebarButtons = new ArrayList<>();
    /** 所有 sidebar 节点按钮（含子级，用于选中态管理）。 */
    private final List<TextButton> allSidebarButtons = new ArrayList<>();
    /** sidebar 是否用深色风格（白字）。默认 false，admin 模板设 true。 */
    private boolean sidebarDarkStyle = false;
    /** 顶栏是否隐藏（toggleTopBar 切换）。 */
    private boolean topBarHidden = false;
    /** 顶栏展开时高度（构造时记录，hide/show 时来回切换）。 */
    private float topBarExpandedH = 48f;
    /** 让 sidebar 菜单字色走 text-on-dark（白），适配深色侧边栏背景。 */
    public BsLayoutAdmin setSidebarDarkStyle(boolean on) {
        this.sidebarDarkStyle = on;
        rebuildSidebar();
        return this;
    }
    private int selectedSidebarIndex = -1;
    /** 顶层 SidebarItem 数据（保留以便 rebuild）。 */
    private final List<SidebarItem> rootSidebarItems = new ArrayList<>();

    public BsLayoutAdmin(Skin skin) {
        setBackground(BsSkinFactory.drawableOf(BsTheme.bhH()));
        top();
        left();

        // ========== 顶部栏 ==========
        topBar = new Table();
        topBar.setBackground(BsSkinFactory.drawableOf(BsTheme.bhH()));
        topBar.pad(6, 12, 6, 12);
        topBar.left();

        // 折叠按钮（左上角 ☰ 或 ‹›）
        TextButton toggleBtn = new TextButton("☰", skin, "bs-link");
        toggleBtn.setName("toggleSidebarBtn");
        toggleBtn.getLabel().setFontScale(1.3f);
        toggleBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { toggleSidebar(); }
        });
        topBar.add(toggleBtn).size(32, 32).padRight(12).left();

        // 顶栏隐藏按钮（▾ 收起 / ▴ 展开）
        TextButton hideTopBtn = new TextButton("▾", skin, "bs-link");
        hideTopBtn.setName("hideTopBtn");
        hideTopBtn.getLabel().setFontScale(1.2f);
        hideTopBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { toggleTopBar(); }
        });
        topBar.add(hideTopBtn).size(32, 32).padRight(8).left();

        logoLabel = makeLabel("Logo", BsTheme.tp(), 1.3f);
        topBar.add(logoLabel).padRight(20).left();

        topMenuRow = new Table();
        topMenuRow.setName("topMenuRow");
        topMenuRow.left();
        topMenuRow.defaults().pad(0, 6, 0, 6).left();
        topBar.add(topMenuRow).growX().left();

        userInfoRow = new Table();
        userInfoRow.setName("userInfoRow");
        userInfoRow.right();
        userInfoRow.defaults().pad(0, 4, 0, 4);
        topBar.add(userInfoRow).right();

        add(topBar).growX().height(48).row();
        topBarExpandedH = 48;

        // ========== 下半部分：sidebar + content ==========
        Table body = new Table();

        sidebarWrap = new Table();
        // 侧边栏卡片背景：圆角 NinePatch（fill=bg-elevated, border=border-strong, corner=10）
        com.badlogic.gdx.scenes.scene2d.utils.Drawable sidebarBgDrawable =
                BsSkinFactory.roundRect(BsTheme.be(), BsTheme.bds(), 10, 1);
        sidebarWrap.setBackground(sidebarBgDrawable);
        // top().left()：内部 sidebarOuter cell 左上对齐（Table 默认居中，会把内容推到中间）
        sidebarWrap.top().left();
        // 与窗口边缘留间距（圆角卡片视觉），左侧 pad 小一点让菜单更靠左
        sidebarWrap.pad(6, 6, 6, 2);

        sidebar = new Table();
        sidebar.pad(4, 0, 4, 0);
        sidebar.top().left();
        // sidebar 内部用透明背景，让 sidebarWrap 的圆角透出来
        sidebar.setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);

        sidebarMenuList = new Table();
        sidebarMenuList.top().left();
        // 行 pad：上下 1 留行间距，左右 0 让菜单按钮紧贴滚动框左边
        sidebarMenuList.defaults().growX().left().pad(1, 0, 1, 0);
        // 用 ScrollPane 包裹菜单列表，菜单项超出侧边栏高度时可滚动
        // 用独立 style（透明背景），避免 default style 的 bs-window-bg 白底盖住 sidebar 深色
        com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle menuScrollStyle =
                new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle();
        menuScrollStyle.hScroll = skin.getDrawable("bs-scrollpane-h-bar");
        menuScrollStyle.vScroll = skin.getDrawable("bs-scrollpane-v-bar");
        menuScroll = new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane(sidebarMenuList, menuScrollStyle);
        menuScroll.setFadeScrollBars(false);
        menuScroll.setScrollingDisabled(true, false);  // 只允许纵向滚动
        menuScroll.setForceScroll(false, true);        // 内容超高强制出滚动条
        menuScroll.setVariableSizeKnobs(false);
        // menuScroll 撑满 sidebar 宽度（之前写死 sidebarExpandedW-16 会偏移且不跟随折叠）
        sidebar.add(menuScroll).growY().growX().expandY();

        // sidebarWrap 包 sidebar，宽度变化时驱动折叠动画
        Container<Table> sidebarOuter = new Container<>(sidebar);
        // fill(true,true)：把 sidebar 拉到 sidebarWrap 全宽全高
        sidebarOuter.fill(true, true).top().left().pad(0);
        // sidebarOuter 用 grow 撑满 sidebarWrap 内部可用宽度（不写死 width，否则与 pad 冲突且折叠时不跟随）
        sidebarWrap.add(sidebarOuter).grow().top().left();

        body.add(sidebarWrap).growY().width(sidebarExpandedW).top()
                .padLeft(10).padRight(8);   // 窗口左边距 10 + 卡片间隔 8

        contentWrap = new Container<>();
        // 内容区卡片背景：圆角 NinePatch（fill=bg-surface, border=border, corner=10）
        contentWrap.setBackground(BsSkinFactory.roundRect(BsTheme.bs(), BsTheme.bd(), 10, 1));
        contentWrap.fill(true);
        contentWrap.pad(10);
        content = new Table();
        contentWrap.setActor(content);
        body.add(contentWrap).grow().top().padTop(6).padBottom(6).padRight(10);  // 窗口右边距 10

        add(body).grow().padBottom(6).row();   // 底部窗口边距
    }

    // ========================= builder API =========================

    public BsLayoutAdmin setLogo(String text) {
        logoLabel.setText(text);
        return this;
    }

    public BsLayoutAdmin addTopMenu(String text, Runnable onClick) {
        BsLink link = new BsLink(text, BsUI.getSkin());
        link.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                try { if (onClick != null) onClick.run(); } catch (Throwable t) { log.warn("topMenu onClick", t); }
            }
        });
        topMenuRow.add(link).pad(0, 8, 0, 8);
        return this;
    }

    /** 设置用户区（点击弹出下拉菜单）。 */
    public BsLayoutAdmin setUserInfo(String userName, Runnable defaultOnClick) {
        userInfoRow.clearChildren();
        final Skin skin = BsUI.getSkin();
        final TextButton userBtn = new TextButton(userName, skin, "bs-menu-title");
        userBtn.setProgrammaticChangeEvents(false);
        userBtn.addListener(new ClickListener() {
            private BsMenuPopup popup;
            @Override public void clicked(InputEvent event, float x, float y) {
                if (popup != null && popup.isOpen()) { popup.close(); return; }
                if (userMenuItems.isEmpty()) {
                    // 没设菜单项，直接触发默认回调
                    try { if (defaultOnClick != null) defaultOnClick.run(); } catch (Throwable t) { /* ignore */ }
                    return;
                }
                List<String> labels = new ArrayList<>();
                List<Runnable> actions = new ArrayList<>();
                for (UserMenuItem mi : userMenuItems) {
                    labels.add(mi.text);
                    actions.add(mi.action);
                }
                popup = new BsMenuPopup(skin);
                popup.show(userBtn.getStage(), userBtn, labels, actions);
            }
        });
        userInfoRow.add(userBtn).height(32);
        return this;
    }

    /** 加用户下拉菜单选项。 */
    public BsLayoutAdmin addUserMenuItem(String text, Runnable action) {
        userMenuItems.add(new UserMenuItem(text, action));
        return this;
    }

    /** 加侧边栏顶层项（可点击叶子节点）。 */
    /**
     * 在顶栏 topMenuRow 插入菜单搜索框，输入时实时过滤 sidebar 菜单。
     * 宽度固定 200，放在面包屑之后。
     */
    public BsLayoutAdmin addMenuSearchBox() {
        final Skin skin = BsUI.getSkin();
        final com.badlogic.gdx.scenes.scene2d.ui.TextField search =
                new com.badlogic.gdx.scenes.scene2d.ui.TextField("", skin);
        search.setMessageText(BsI18n.get("core.layout.search", "🔍 搜索菜单"));
        // 限制只占固定宽度，不挤掉面包屑
        topMenuRow.add(search).width(200).height(28).padLeft(12);
        search.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                setMenuFilter(search.getText());
            }
        });
        return this;
    }

    public BsLayoutAdmin addSideMenu(String text, Runnable onClick) {
        SidebarItem item = new SidebarItem(text, onClick);
        return addSideMenuTree(item);
    }

    /** 加侧边栏树状菜单（支持多级）。 */
    public BsLayoutAdmin addSideMenuTree(SidebarItem root) {
        rootSidebarItems.add(root);
        renderSidebarItem(root, 0);
        sidebarMenuList.row();
        if (selectedSidebarIndex < 0 && !topSidebarButtons.isEmpty()) {
            selectSidebar(0);
        }
        return this;
    }

    /** 递归渲染 sidebar 节点。 */
    private void renderSidebarItem(SidebarItem item, int depth) {
        // 过滤：menuFilter 非空时，只渲染自身或后代匹配的项
        if (!menuFilter.isEmpty() && !matchesFilter(item, menuFilter)) {
            return;
        }
        final boolean isLeaf = item.isLeaf();
        final int idx = allSidebarButtons.size();
        final Skin skin = BsUI.getSkin();
        Table row = new Table();
        row.left();
        row.defaults().left().pad(0);
        // 层级缩进：每深一级 +20px（depth=0 不缩进，depth=1 缩进 20...）
        if (depth > 0) row.add().width(depth * 20);

        // 箭头占位统一对齐：非叶子显示 ▾/▸，叶子留等宽占位
        // 保证所有菜单的文字起点 X 一致（不会出现"二级比一级还左"）
        if (!isLeaf) {
            TextButton arrow = new TextButton(item.expanded ? "▾" : "▸", skin, "bs-link");
            arrow.getLabel().setFontScale(0.9f);
            arrow.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    item.expanded = !item.expanded;
                    rebuildSidebar();
                }
            });
            row.add(arrow).size(18, 24).padRight(2);
        } else {
            row.add().width(20);  // 叶子留与箭头等宽的占位，文字与父级对齐
        }

        // 文字（原封使用 item.text，不加层级前缀字符，避免污染菜单名）
        final TextButton textBtn = new TextButton(item.text, skin, "bs-menu-title");
        textBtn.setProgrammaticChangeEvents(false);
        textBtn.left();
        textBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // 选中视觉
                selectSidebarButton(idx);
                // 触发回调
                try { if (item.onClick != null) item.onClick.run(); } catch (Throwable t) { log.warn("sideMenu onClick", t); }
                // 非叶子节点也切换展开/折叠
                if (!item.isLeaf()) {
                    item.expanded = !item.expanded;
                    rebuildSidebar();
                }
            }
        });
        // hover 显示全名 tooltip（菜单宽度固定，长名字会被裁剪，hover 时在右侧弹出完整名）
        attachMenuTooltip(textBtn, item.text);
        // 按 depth 调字色（深度越深越浅），全部走主题色 token
        // 深色侧边栏风格下：一级用 text-on-dark 全亮，二三级按 token 降透明度
        Color tc;
        if (sidebarDarkStyle) {
            Color base = skin.get("bs-text-on-dark", Color.class);
            if (depth == 0) {
                tc = base;
            } else if (depth == 1) {
                tc = new Color(base.r, base.g, base.b, 0.85f);
            } else {
                tc = new Color(base.r, base.g, base.b, 0.7f);
            }
        } else {
            tc = BsTheme.tp();
            if (depth == 1) tc = BsTheme.ts();
            else if (depth >= 2) tc = BsTheme.tm();
        }
        // 一级菜单字号稍大（视觉更突出）
        float fontScale = depth == 0 ? 1.05f : 1.0f;
        TextButton.TextButtonStyle ts = new TextButton.TextButtonStyle(skin.get("bs-menu-title", TextButton.TextButtonStyle.class));
        ts.fontColor = tc;
        // 深色侧边栏模式：按钮背景用透明（让 sidebarWrap 的深色透出来，否则 bs-menu-title-up 白色会盖住）
        if (sidebarDarkStyle) {
            com.badlogic.gdx.scenes.scene2d.utils.Drawable transparent =
                    skin.has("bs-transparent", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)
                            ? skin.getDrawable("bs-transparent")
                            : BsSkinFactory.drawableOf(new Color(0, 0, 0, 0));
            ts.up = transparent;
            ts.over = transparent;
            ts.down = transparent;
        }
        textBtn.setStyle(ts);
        // setStyle 后再设对齐/字号（setStyle 会重建 Label，覆盖之前的设置）
        textBtn.left();
        textBtn.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
        textBtn.getLabel().setFontScale(fontScale);

        // textBtn 不 growX（按文字宽度），避免宽按钮里 Label 被居中导致文字偏右
        row.add(textBtn).height(28).padLeft(2).left();
        sidebarMenuList.add(row).growX().height(28).pad(1).row();

        // 记录顶层 + 全部
        if (depth == 0) topSidebarButtons.add(textBtn);
        allSidebarButtons.add(textBtn);

        // 递归渲染子项（仅 expanded 时）
        if (!isLeaf && item.expanded) {
            for (SidebarItem c : item.children) {
                renderSidebarItem(c, depth + 1);
            }
        }
    }

    /** 全量重建 sidebar 菜单（保留 SidebarItem 数据，重渲染按钮）。 */
    public void rebuildSidebar() {
        // 清理上一轮菜单 tooltip（按钮已被 clear，监听失效；detach 避免 stage 残留）
        for (BsTooltip t : menuTooltips) {
            try { t.detach(); } catch (Throwable ignored) {}
        }
        menuTooltips.clear();

        sidebarMenuList.clearChildren();
        topSidebarButtons.clear();
        allSidebarButtons.clear();
        // 有过滤词时强制展开所有父节点，否则匹配的子项看不到
        if (!menuFilter.isEmpty()) {
            for (SidebarItem root : rootSidebarItems) {
                if (matchesFilter(root, menuFilter)) {
                    setExpandedRecursively(root, true);
                }
            }
        }
        for (SidebarItem root : rootSidebarItems) {
            renderSidebarItem(root, 0);
            sidebarMenuList.row();
        }
    }

    /** item 自身或任一后代 text 包含 filter（忽略大小写）则 true；filter 空 时恒 true。 */
    private boolean matchesFilter(SidebarItem item, String filter) {
        if (filter == null || filter.isEmpty()) return true;
        if (item.text != null && item.text.toLowerCase().contains(filter.toLowerCase())) return true;
        for (SidebarItem c : item.children) {
            if (matchesFilter(c, filter)) return true;
        }
        return false;
    }

    /** 递归设置 item 及所有后代的 expanded。 */
    private void setExpandedRecursively(SidebarItem item, boolean expanded) {
        item.expanded = expanded;
        for (SidebarItem c : item.children) setExpandedRecursively(c, expanded);
    }

    /**
     * 为菜单按钮创建 tooltip，显示完整文字（菜单宽度固定时长名字会被裁剪）。
     * <p>tooltip 在按钮首次 enter 且 stage 就绪时才 attach（rebuild 时 layout 可能还没加到 stage）。
     * 每次 rebuild 时 {@link #rebuildSidebar} 会 detach 上一轮 tooltip 并清空列表。</p>
     */
    private void attachMenuTooltip(TextButton btn, String text) {
        final BsTooltip tip = new BsTooltip(btn, text, BsUI.getSkin(), BsTooltip.Placement.RIGHT);
        tip.setShowDelay(0.4f);
        menuTooltips.add(tip);
        // 懒 attach：按钮 enter 时若还没 attach 则 attach 到当前 stage
        btn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            private boolean attached = false;
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (attached || pointer != -1) return;  // pointer!=-1 表示鼠标按下，忽略
                Stage s = btn.getStage();
                if (s == null) return;
                tip.attach(s);
                attached = true;
            }
        });
    }

    /** 设置菜单过滤词（顶栏搜索框输入）；非空时 sidebar 只显示包含该词的项。触发 rebuild。 */
    public BsLayoutAdmin setMenuFilter(String filter) {
        String f = filter == null ? "" : filter.trim();
        if (f.equals(this.menuFilter)) return this;
        this.menuFilter = f;
        rebuildSidebar();
        return this;
    }

    /** 选中 sidebar 按钮（视觉高亮）。 */
    public BsLayoutAdmin selectSidebarButton(int idx) {
        for (int i = 0; i < allSidebarButtons.size(); i++) {
            allSidebarButtons.get(i).setChecked(i == idx);
        }
        selectedSidebarIndex = idx;
        return this;
    }

    /** 选中第 N 个顶层 sidebar 项。 */
    public BsLayoutAdmin selectSidebar(int topIdx) {
        if (topIdx < 0 || topIdx >= topSidebarButtons.size()) return this;
        TextButton target = topSidebarButtons.get(topIdx);
        // 找到 allSidebarButtons 中的索引
        for (int i = 0; i < allSidebarButtons.size(); i++) {
            if (allSidebarButtons.get(i) == target) {
                selectSidebarButton(i);
                break;
            }
        }
        return this;
    }

    /** 设置主内容区。 */
    public BsLayoutAdmin setContent(Actor actor) {
        content.clearChildren();
        if (actor != null) content.add(actor).grow();
        return this;
    }

    public BsLayoutAdmin contentPadding(float pad) {
        contentWrap.pad(pad);
        return this;
    }

    // ========================= 折叠/展开 sidebar =========================

    /** 切换 sidebar 折叠状态（带动画）。 */
    public void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        animateSidebarTo(sidebarCollapsed ? sidebarCollapsedW : sidebarExpandedW);
    }

    public boolean isSidebarCollapsed() { return sidebarCollapsed; }

    /**
     * 立即收起 sidebar（无动画），用于初始化时默认收起。
     * <p>必须在 layout 已加到 stage 且 cell 布局完成后调用（BsAdminShell 构造末尾）。</p>
     */
    public void collapseSidebarImmediate() {
        sidebarCollapsed = true;
        float w = sidebarCollapsedW;
        sidebarWrap.setWidth(w);
        // menuScroll 用 growX，sidebar 宽度变化自动跟随，无需手动设 cell 宽度
        Table body = (Table) getCells().get(1).getActor();
        if (body != null && body.getCells().size > 0) {
            body.getCells().get(0).width(w);
            body.invalidate();
            contentWrap.invalidateHierarchy();
        }
        sidebarMenuList.setVisible(false);
    }

    /** 设置展开时 sidebar 宽度（默认 180）。 */
    public void setSidebarWidth(float w) {
        this.sidebarExpandedW = w;
        // menuScroll 用 growX 自动跟随 sidebar 宽度，无需手动设 cell 宽度
        sidebar.invalidate();
        if (!sidebarCollapsed) animateSidebarTo(w);
    }

    /** 动画过渡 sidebar 宽度。 */
    private void animateSidebarTo(float targetW) {
        float fromW = sidebarWrap.getWidth();
        // 用 Actions 包装：手动驱动宽度
        sidebarWrap.clearActions();
        sidebarWrap.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            float elapsed = 0f;
            @Override
            public boolean act(float delta) {
                elapsed += delta;
                float t = MathUtils.clamp(elapsed / COLLAPSE_DURATION, 0, 1);
                float cur = MathUtils.lerp(fromW, targetW, t);
                sidebarWrap.setWidth(cur);
                // 同步父 Table cell 宽度，并 invalidateHierarchy 让 content 卡片自动右扩 + sidebar 内部跟随
                Table body = (Table) getCells().get(1).getActor();
                if (body != null && body.getCells().size > 0) {
                    body.getCells().get(0).width(cur);
                    body.invalidate();
                    contentWrap.invalidateHierarchy();
                }
                if (t >= 1f) {
                    sidebarMenuList.setVisible(targetW > 0);
                    return true;
                }
                return false;
            }
        });
    }

    // ========================= 顶栏隐藏/展开 =========================

    /** 切换顶栏完全隐藏/显示（带动画）。 */
    public void toggleTopBar() {
        topBarHidden = !topBarHidden;
        float targetH = topBarHidden ? 0f : topBarExpandedH;
        animateTopBarTo(targetH);
        // 更新隐藏按钮文字（▾ 收起 / ▴ 展开）
        Actor hideBtn = topBar.findActor("hideTopBtn");
        if (hideBtn instanceof TextButton) {
            ((TextButton) hideBtn).getLabel().setText(topBarHidden ? "▴" : "▾");
        }
    }

    public boolean isTopBarHidden() { return topBarHidden; }

    /** 设置顶栏展开时高度（BsAdminShell 放大后调用同步记录值）。 */
    public void setTopBarExpandedHeight(float h) {
        this.topBarExpandedH = h;
        if (!topBarHidden) {
            getCells().get(0).height(h);
            invalidateHierarchy();
        }
    }

    /** 动画过渡顶栏高度（this Table 的 cell 0 = topBar）。 */
    private void animateTopBarTo(float targetH) {
        float fromH = topBar.getHeight();
        topBar.clearActions();
        // 隐藏时动画结束后 setVisible(false)；显示时一开始就可见
        if (targetH > 0) topBar.setVisible(true);
        topBar.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            float elapsed = 0f;
            @Override
            public boolean act(float delta) {
                elapsed += delta;
                float t = MathUtils.clamp(elapsed / COLLAPSE_DURATION, 0, 1);
                float cur = MathUtils.lerp(fromH, targetH, t);
                getCells().get(0).height(cur);
                invalidate();
                if (t >= 1f) {
                    topBar.setVisible(targetH > 0);
                    return true;
                }
                return false;
            }
        });
    }

    // ========================= 工具 =========================

    private Label makeLabel(String text, Color color, float scale) {
        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = BsUI.getSkin().getFont("default");
        ls.fontColor = color;
        Label l = new Label(text, ls);
        l.setColor(Color.WHITE);
        l.setFontScale(scale);
        return l;
    }
}
