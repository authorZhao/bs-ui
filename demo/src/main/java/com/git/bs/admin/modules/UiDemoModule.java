package com.git.bs.admin.modules;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.git.bs.admin.AdminModule;
import com.git.bs.admin.BsAdminShell;
import com.git.bs.demo.BsControlsSkinScreen;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * UI 模块（精简版入口）：把 BsControlsSkinScreen 的 44 个控件演示搬进 admin 模板，
 * 按三个二级菜单分组：
 * <ul>
 *   <li>UI 模块/通用UI —— 标签/按钮/输入/表单/卡片等基础控件</li>
 *   <li>UI 模块/业务UI —— 表格/树/搜索/工具栏/文件项等业务控件</li>
 *   <li>UI 模块/图形UI —— 折线/柱状/饼图/雷达等图表</li>
 * </ul>
 *
 * <p>本类只作为入口注册"UI 模块"占位，实际子模块由 {@link BsAdminShell} 启动时
 * 调用 {@link #registerAll(BsAdminShell)} 批量注册。</p>
 *
 * <p>实现复用 {@link BsControlsSkinScreen#fillModuleContent(int, Table)}，
 * 不重复实现 44 个 fill 方法。</p>
 */
@Slf4j
public class UiDemoModule implements AdminModule {

    public static final String PATH = "UI 模块";

    /** 通用UI：基础控件（标签/按钮/输入/选择/表单/卡片/徽标等）。值为 MODULES 的 index。 */
    private static final int[] GENERAL_UI = {
            0,   // Labels 标签
            1,   // Buttons 按钮
            2,   // ImageButton 图标按钮
            3,   // Inputs 输入框
            4,   // Selects 下拉
            5,   // Radio & Check 单选/多选
            6,   // Slider 滑块
            7,   // Misc 杂项
            10,  // Form 表单
            17,  // Cards 卡片
            18,  // Badge 徽标
            22,  // Icons 图标库
            24,  // Collapse & Accordion 折叠
            25,  // ButtonGroup & Alert
            26,  // InputNumber & InputGroup
            36,  // Wave1-Basics Switch/Avatar/Timeline...
            37,  // Wave1-Inputs AutoComplete/TagInput...
    };

    /** 业务UI：导航/布局/数据展示/业务组件。 */
    private static final int[] BUSINESS_UI = {
            8,   // MenuBar 菜单栏
            12,  // Tree 树状列表
            13,  // Table 表格+分页
            19,  // Profile 个人面板
            20,  // Layout 管理后台
            21,  // Breadcrumb 面包屑
            23,  // Progress & Toast
            27,  // Navbar & Offcanvas
            33,  // P2-Content Placeholder/Figure/ListGroup...
            34,  // P2-Carousel 轮播图
            38,  // Wave1-Feedback Result/LoadingOverlay
            39,  // Wave2-Data DataTable/PropertySheet
            41,  // Wave2-Business SearchBar/Toolbar/FileItem/Transfer
            43,  // Wave3-Misc Affix/Drawer
    };

    /** 图形UI：各类图表。 */
    private static final int[] GRAPHICS_UI = {
            28,  // Charts-Line 折线图
            29,  // Charts-Bar 柱状图
            30,  // Charts-Pie 饼图
            31,  // Charts-Legend 图例与系列切换
            32,  // Charts-Hover Hover 数据查看
            35,  // Charts-Extended Area/Spline/Scatter/Radar...
    };

    /** BsControlsSkinScreen 内容工厂（复用 44 个 fill 方法）。 */
    private static BsControlsSkinScreen contentFactory;

    /** 取内容工厂单例（懒加载，skin 用当前主题 skin）。 */
    private static BsControlsSkinScreen factory() {
        if (contentFactory == null) {
            contentFactory = new BsControlsSkinScreen(BsUI.getSkin());
        }
        return contentFactory;
    }

    /**
     * 把 44 个控件演示按"通用UI/业务UI/图形UI"三组注册到 shell。
     * 由 BsAdminShell 构造时调用。
     */
    public static void registerAll(BsAdminShell shell) {
        registerGroup(shell, "通用UI", GENERAL_UI);
        registerGroup(shell, "业务UI", BUSINESS_UI);
        registerGroup(shell, "图形UI", GRAPHICS_UI);
    }

    /** 注册一个分组下的所有模块。 */
    private static void registerGroup(BsAdminShell shell, String groupName, int[] indices) {
        for (int idx : indices) {
            final int moduleIdx = idx;
            String name = BsControlsSkinScreen.MODULES.get(idx);
            // 菜单名取后半段（"Labels  标签" → "标签"），中文优先
            String menuName = simplifyName(name);
            final String path = "UI 模块/" + groupName + "/" + menuName;
            shell.register(new AdminModule() {
                @Override public String getPath() { return path; }
                @Override public Actor buildView(BsAdminShell s) {
                    return buildModuleView(moduleIdx);
                }
            });
        }
    }

    /** "Labels  标签" → "标签"；无中文则返回原名。 */
    private static String simplifyName(String name) {
        // 按多个空格分割，取最后一段
        String[] parts = name.split("\\s{2,}|\\s+");
        if (parts.length > 1) {
            return parts[parts.length - 1];
        }
        return name;
    }

    // ============ AdminModule 入口（"UI 模块"本身是分组占位） ============

    @Override
    public String getPath() {
        return PATH;
    }

    @Override
    public Actor buildView(BsAdminShell shell) {
        // "UI 模块"本身不可达（它是分组），这里给个说明页
        Skin skin = BsUI.getSkin();
        Table root = new Table();
        root.top().left();
        root.pad(20);
        root.defaults().top().left();

        Label title = new Label("UI 控件演示", skin);
        title.setFontScale(1.4f);
        title.setColor(BsTheme.tp());
        root.add(title).left().padBottom(8).row();

        Label hint = new Label("请在左侧菜单选择分类：通用UI / 业务UI / 图形UI", skin);
        hint.setColor(BsTheme.tm());
        root.add(hint).left().padBottom(4).row();

        Label hint2 = new Label("共 " + BsControlsSkinScreen.MODULES.size() + " 个控件演示，"
                + "源自 BsControlsSkinScreen。", skin);
        hint2.setColor(BsTheme.tm());
        root.add(hint2).left().row();

        return root;
    }

    /**
     * 构造一个具体控件演示页的内容（供注册的真实模块调用）。
     */
    public static Actor buildModuleView(int moduleIdx) {
        Skin skin = BsUI.getSkin();
        Table root = new Table();
        root.top().left();
        root.pad(12);
        root.defaults().top().left();

        // 模块标题
        String name = BsControlsSkinScreen.MODULES.get(moduleIdx);
        Label title = new Label(name, skin);
        title.setFontScale(1.3f);
        title.setColor(BsTheme.tp());
        root.add(title).left().padBottom(8).row();

        // 内容（复用 BsControlsSkinScreen 的 fill 方法）
        Table content = new Table(skin);
        content.defaults().pad(6).left();
        try {
            factory().fillModuleContent(moduleIdx, content);
        } catch (Throwable t) {
            log.error("fillModuleContent 失败 idx={}", moduleIdx, t);
            content.add(new Label("加载失败: " + t.getMessage(), skin));
        }
        root.add(content).growX().top().row();
        return root;
    }
}
