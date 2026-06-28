package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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

    /** sidebar 当前/目标宽度。 */
    private float sidebarExpandedW = 180;
    private float sidebarCollapsedW = 0;
    private boolean sidebarCollapsed = false;
    /** 折叠/展开动画时长。 */
    private static final float COLLAPSE_DURATION = 0.25f;

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
    private int selectedSidebarIndex = -1;
    /** 顶层 SidebarItem 数据（保留以便 rebuild）。 */
    private final List<SidebarItem> rootSidebarItems = new ArrayList<>();

    public BsLayoutAdmin(Skin skin) {
        setBackground(skin.newDrawable("white", BsTheme.bhH()));
        top();
        left();

        // ========== 顶部栏 ==========
        topBar = new Table();
        topBar.setBackground(skin.newDrawable("white", Color.WHITE));
        topBar.pad(6, 12, 6, 12);
        topBar.left();

        // 折叠按钮（左上角 ☰ 或 ‹›）
        TextButton toggleBtn = new TextButton("☰", skin, "bs-link");
        toggleBtn.getLabel().setFontScale(1.3f);
        toggleBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { toggleSidebar(); }
        });
        topBar.add(toggleBtn).size(32, 32).padRight(12).left();

        logoLabel = makeLabel("Logo", BsTheme.tp(), 1.3f);
        topBar.add(logoLabel).padRight(20).left();

        topMenuRow = new Table();
        topMenuRow.left();
        topMenuRow.defaults().pad(0, 6, 0, 6).left();
        topBar.add(topMenuRow).growX().left();

        userInfoRow = new Table();
        userInfoRow.right();
        userInfoRow.defaults().pad(0, 4, 0, 4);
        topBar.add(userInfoRow).right();

        add(topBar).growX().height(48).row();

        // ========== 下半部分：sidebar + content ==========
        Table body = new Table();

        sidebarWrap = new Table();
        sidebarWrap.setBackground(skin.newDrawable("white", BsTheme.be()));

        sidebar = new Table();
        sidebar.pad(8);
        sidebar.top().left();

        sidebarMenuList = new Table();
        sidebarMenuList.top().left();
        sidebarMenuList.defaults().growX().left().pad(2);
        sidebar.add(sidebarMenuList).growY().width(sidebarExpandedW - 16);

        // sidebarWrap 包 sidebar，宽度变化时驱动折叠动画
        Container<Table> sidebarOuter = new Container<>(sidebar);
        sidebarOuter.fill(false, true).top().left().pad(0);
        sidebarWrap.add(sidebarOuter).growY().width(sidebarExpandedW);
        sidebarWrap.pack();

        body.add(sidebarWrap).growY().width(sidebarExpandedW).top();

        contentWrap = new Container<>();
        contentWrap.setBackground(skin.newDrawable("white", BsTheme.bb()));
        contentWrap.fill(true);
        contentWrap.pad(10);
        content = new Table();
        contentWrap.setActor(content);
        body.add(contentWrap).grow().top();

        add(body).grow().row();
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
        final boolean isLeaf = item.isLeaf();
        final int idx = allSidebarButtons.size();
        final Skin skin = BsUI.getSkin();
        Table row = new Table();
        row.left();
        row.defaults().left().pad(0);

        // 缩进（每层 14px）
        if (depth > 0) row.add().width(depth * 14);

        // 展开/折叠箭头（非叶子）
        if (isLeaf) {
            row.add().width(20);  // 占位对齐
        } else {
            TextButton arrow = new TextButton(item.expanded ? "▾" : "▸", skin, "bs-link");
            arrow.getLabel().setFontScale(0.9f);
            arrow.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    item.expanded = !item.expanded;
                    rebuildSidebar();
                }
            });
            row.add(arrow).size(20, 24).padRight(2);
        }

        // 文字
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
        // 按 depth 调字色（深度越深越浅，与 BsTree 同风格）
        Color tc = BsTheme.tp();
        if (depth == 1) tc = BsTheme.ts();
        else if (depth >= 2) tc = BsTheme.tm();
        TextButton.TextButtonStyle ts = new TextButton.TextButtonStyle(skin.get("bs-menu-title", TextButton.TextButtonStyle.class));
        ts.fontColor = tc;
        textBtn.setStyle(ts);

        row.add(textBtn).growX().height(28);
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
        sidebarMenuList.clearChildren();
        topSidebarButtons.clear();
        allSidebarButtons.clear();
        for (SidebarItem root : rootSidebarItems) {
            renderSidebarItem(root, 0);
            sidebarMenuList.row();
        }
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

    /** 设置展开时 sidebar 宽度（默认 180）。 */
    public void setSidebarWidth(float w) {
        this.sidebarExpandedW = w;
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
                // 同步父 Table cell 宽度
                Table body = (Table) getCells().get(1).getActor();
                if (body != null && body.getCells().size > 0) {
                    body.getCells().get(0).width(cur);
                    body.invalidate();
                }
                if (t >= 1f) {
                    sidebarMenuList.setVisible(targetW > 0);
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
