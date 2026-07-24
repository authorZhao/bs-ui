package com.git.bs.demo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.git.bs.demo.modules.BsBasicModules;
import com.git.bs.demo.modules.BsChartModules;
import com.git.bs.demo.modules.BsDataModules;
import com.git.bs.demo.modules.BsFeedbackModules;
import com.git.bs.demo.modules.BsFormModules;
import com.git.bs.demo.modules.BsWaveModules;
import com.git.bs.demo.modules.ModuleSupport;
import com.git.bs.game.BsSkinApp;
import com.git.bs.ui.BsAboutDialog;
import com.git.bs.ui.BsAdminTheme;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsCheckBox;
import com.git.bs.ui.BsDarkTheme;
import com.git.bs.ui.BsLightTheme;
import com.git.bs.ui.BsLineChart;
import com.git.bs.ui.BsList;
import com.git.bs.ui.BsModal;
import com.git.bs.ui.BsScrollPane;
import com.git.bs.ui.BsSkinExporter;
import com.git.bs.ui.BsSkinFactory;
import com.git.bs.ui.BsText;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

/**
 * Bs UI 皮肤演示主屏幕（皮肤导入版）。
 *
 * <p>与 {@link BsControlsTestScreen}（代码生成版）共享同一套 module 类，区别仅在：
 * <ul>
 *   <li>由 {@link BsSkinApp} 驱动（皮肤从外部导入，而非代码内联生成）；</li>
 *   <li>额外暴露内容工厂 API（{@link #fillModuleContent(int, Table)} +
 *       {@code (Skin)} 构造器 + {@link #setStage(Stage)}），供 admin UiDemo 模块复用
 *       48+ 个 fill 方法，避免重复实现。</li>
 * </ul>
 *
 * <p>本类只保留：布局骨架 + 模块路由 + 屏幕生命周期 + 皮肤导出对话框 + 内容工厂 API。
 * 49 个 {@code fillXxx} 内容方法已按分类拆分到 {@link com.git.bs.demo.modules} 下。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsControlsSkinScreen extends ScreenAdapter {

    public static final int WIN_W = 1280;
    public static final int WIN_H = 800;

    private final BsSkinApp app;
    private final Skin skin;
    private Stage stage;

    /** 右侧内容容器；切换模块时 clear + add。 */
    private Table contentHost;
    private BsScrollPane contentScroll;

    /** 底部状态行：显示最近一次控件事件。 */
    private Label statusLine;

    /** 左侧模块导航。 */
    private BsList<String> nav;

    /** 模块列表（ContextMenu 插在 index 9，与 BsControlsTestScreen 保持一致）。
     *  public 供 admin UiDemoModule 复用。 */
    public static final List<String> MODULES = buildModuleList();

    /** 主题切换下拉（重建后恢复选中项）。 */
    private com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String> themeToggle;

    // =================== 6 个 module 路由 ===================
    // 懒构造：工厂模式（(Skin) 构造器）下 stage 可能晚到（setStage 注入），
    // 故在真正需要 fill 时才用当前 stage 实例化 module，保证弹出层挂到正确 stage。
    private BsBasicModules basicMods;
    private BsFormModules formMods;
    private BsDataModules dataMods;
    private BsFeedbackModules feedbackMods;
    private BsChartModules chartMods;
    private BsWaveModules waveMods;

    /** 独立屏幕构造（自建 stage / nav / root）。 */
    public BsControlsSkinScreen(BsSkinApp app) {
        this.app = app;
        this.skin = app.getSkin();
        this.stage = new Stage(new StretchViewport(WIN_W, WIN_H));

        buildLayout();
        switchModule(0);
        // 主题切换由 BsSkinApp 的 listener 处理（重建整个 screen），本类不监听
    }

    /**
     * 内容工厂模式构造：只用于复用 49 个 fill 方法（admin UiDemo 模块用），
     * 不建 stage / nav / root，避免无用开销。{@link #fillModuleContent} 可直接调用。
     *
     * @param skin 当前 skin
     */
    public BsControlsSkinScreen(Skin skin) {
        this.app = null;
        this.skin = skin;
        // 创建一个独立未渲染的 Stage 占位（admin 模块不 render 它），避免 attach(null) NPE。
        // admin shell 构造后会调用 setStage 注入真实 stage。
        this.stage = new Stage(new com.badlogic.gdx.utils.viewport.ScreenViewport());
        this.statusLine = new Label("(就绪)", skin);
        this.statusLine.setColor(BsTheme.ts());
    }

    /**
     * 注入真实 stage（admin shell 构造后调用）。
     * <p>工厂模式构造创建的是占位 stage，导致 {@code BsModal.showModal(this.stage)} /
     * {@code BsTooltip.attach(this.stage)} 失效。注入真实 stage 后，Pickers/DateTime/Overlay
     * /Modal/Dialogs 等模块才能正常弹窗。</p>
     * <p>同时清空已构造的 module 缓存，让下次 fill 用新 stage 重建（避免闭包捕获旧 stage）。</p>
     */
    public void setStage(Stage s) {
        this.stage = s;
        // 清空 module 缓存：它们捕获了旧 stage，需重新构造
        basicMods = null;
        formMods = null;
        dataMods = null;
        feedbackMods = null;
        chartMods = null;
        waveMods = null;
    }

    /** 懒构造 / 取 6 个 module（每次用当前 stage + statusLine）。 */
    private void ensureModules() {
        Consumer<String> setStatus = this::setStatus;
        if (basicMods == null) {
            basicMods    = new BsBasicModules(skin, stage, setStatus);
            formMods     = new BsFormModules(skin, stage, setStatus);
            dataMods     = new BsDataModules(skin, stage, setStatus);
            feedbackMods = new BsFeedbackModules(skin, stage, setStatus);
            chartMods    = new BsChartModules(skin);
            waveMods     = new BsWaveModules(skin, stage, setStatus);
        }
    }

    /**
     * 构建 root + 顶部标题栏 + 左导航 + 右内容 + 底部状态栏。
     * <p>首次构造和主题切换重建 UI 都走这里。</p>
     */
    private void buildLayout() {
        Table root = new Table();
        root.setBackground(BsSkinFactory.drawableOf(BsTheme.bb()));
        root.setFillParent(true);
        stage.addActor(root);

        // 顶部标题行：左标题 + 右主题切换（直接用最大号烘焙字体，不缩放，避免发虚）
        Table header = new Table();
        BsText title = new BsText("Bs UI 皮肤演示台  /  Skin-imported controls playground",
                BsText.Size.XL).bold();
        header.add(title).pad(10).left().growX();

        themeToggle = new com.badlogic.gdx.scenes.scene2d.ui.SelectBox<>(skin);
        com.badlogic.gdx.utils.Array<String> themeNames = new com.badlogic.gdx.utils.Array<>();
        themeNames.add("☀ Light");
        themeNames.add("🌙 Dark");
        themeNames.add("🛡 Admin");
        themeToggle.setItems(themeNames);
        String currentName = BsUI.currentThemeName();
        if (currentName == null) currentName = "";
        themeToggle.setSelectedIndex(
                currentName.contains("admin") ? 2
                : currentName.contains("dark") ? 1 : 0);
        themeToggle.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                int idx = themeToggle.getSelectedIndex();
                BsTheme target = idx == 2 ? BsAdminTheme.INSTANCE
                        : idx == 1 ? BsDarkTheme.INSTANCE : BsLightTheme.INSTANCE;
                BsUI.setTheme(target);
            }
        });
        header.add(themeToggle).pad(8).right();

        // 关于按钮（醒目：主色实心）
        BsButton aboutBtn = new BsButton("ⓘ 关于", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.SM);
        aboutBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsAboutDialog.show(stage, skin, "Bs UI 皮肤演示台", false);
            }
        });
        header.add(aboutBtn).pad(8, 8, 8, 4).right();
        root.add(header).growX().row();

        // 主区：左导航 + 右内容（可滚动）
        Table body = new Table();
        nav = new BsList<>(skin);
        nav.setItems(ModuleSupport.items(MODULES));
        nav.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                int idx = nav.getSelectedIndex();
                if (idx >= 0) switchModule(idx);
            }
        });
        BsScrollPane navScroll = new BsScrollPane(nav, skin);
        navScroll.setFadeScrollBars(false);
        navScroll.setScrollingDisabled(true, false);
        Container<Actor> navWrap = new Container<>(navScroll);
        navWrap.background(BsSkinFactory.drawableOf(BsTheme.bs())).pad(4);
        navWrap.fill();
        body.add(navWrap).width(220).top().padRight(8).expandY();

        contentHost = new Table();
        contentScroll = new BsScrollPane(contentHost, skin);
        contentScroll.setFadeScrollBars(false);
        body.add(contentScroll).grow().top();

        root.add(body).grow().padLeft(10).padRight(10).row();

        // 底部状态栏
        statusLine = new Label("(就绪)", skin);
        statusLine.setColor(BsTheme.ts());
        root.add(statusLine).pad(8).expandX().left().row();
    }

    /**
     * 模块列表（ContextMenu 插在 index 9，MenuBar 之后；原 Pickers 及之后顺延 +1）。
     * 与 {@link BsControlsTestScreen#buildModuleList()} 保持一致。
     */
    private static List<String> buildModuleList() {
        List<String> modules = new java.util.ArrayList<>();
        modules.add("Labels  标签");
        modules.add("Buttons  按钮");
        modules.add("ImageButton  图标按钮");
        modules.add("Inputs  输入框");
        modules.add("Selects  下拉");
        modules.add("Radio & Check  单选/多选");
        modules.add("Slider  滑块");
        modules.add("Misc  杂项（Tooltip/禁用等）");
        modules.add("MenuBar  菜单栏");
        modules.add("ContextMenu  右键菜单");                       // 新增（index 9）
        modules.add("Pickers  选择器（日期/颜色）");                 // 原 9 → 10
        modules.add("Form  表单（校验）");                           // 原 10 → 11
        modules.add("DateTime  日期时间选择器");                     // 原 11 → 12
        modules.add("Tree  树状列表");                               // 原 12 → 13
        modules.add("Table  表格 + 分页");                           // 原 13 → 14
        modules.add("Overlay  Tooltip/Spinner/Popover/Link");       // 原 14 → 15
        modules.add("Modal  通用模态框");                            // 原 15 → 16
        modules.add("Dialogs  对话框库（动画）");                    // 原 16 → 17
        modules.add("Cards  卡片");                                  // 原 17 → 18
        modules.add("Badge  徽标");                                  // 原 18 → 19
        modules.add("Profile  个人面板");                            // 原 19 → 20
        modules.add("Layout  管理后台");                             // 原 20 → 21
        modules.add("Breadcrumb  面包屑");                           // 原 21 → 22
        modules.add("Icons  图标库");                                // 原 22 → 23
        modules.add("Progress & Toast  进度条/轻提示");             // 原 23 → 24
        modules.add("Collapse & Accordion  折叠");                   // 原 24 → 25
        modules.add("ButtonGroup & Alert  按钮组/警告条");          // 原 25 → 26
        modules.add("InputNumber & InputGroup  数字/输入组");       // 原 26 → 27
        modules.add("Navbar & Offcanvas  导航栏/抽屉");             // 原 27 → 28
        modules.add("Charts-Line  折线图");                          // 原 28 → 29
        modules.add("Charts-Bar  柱状图");                           // 原 29 → 30
        modules.add("Charts-Pie  饼图");                             // 原 30 → 31
        modules.add("Charts-Legend  图例与系列切换");               // 原 31 → 32
        modules.add("Charts-Hover  Hover 数据查看");                // 原 32 → 33
        modules.add("Charts-Bar3D  3D 柱状图");                     // 新增（index 34）
        modules.add("P2-Content  Placeholder/Figure/ListGroup/FloatingLabel");  // 原 33 → 35
        modules.add("P2-Carousel  轮播图");                          // 原 34 → 36
        modules.add("Charts-Extended  Area/Spline/Scatter/Radar/Doughnut");     // 原 35 → 37
        modules.add("Wave1-Basics  Switch/Avatar/Timeline/Statistic/Steps/Empty/Rating");  // 原 36 → 38
        modules.add("Wave1-Inputs  AutoComplete/TagInput/DescriptionList");                // 原 37 → 39
        modules.add("Wave1-Feedback  Result/LoadingOverlay");                              // 原 38 → 40
        modules.add("Wave2-Data  DataTable/PropertySheet");                                // 原 39 → 41
        modules.add("Wave3-Editor  StatusBar");                                            // 原 40 → 42
        modules.add("Wave2-Business  SearchBar/Toolbar/FileItem/Transfer");                // 原 41 → 43
        modules.add("Wave3-EditorPro  Inspector/NodePalette/MiniMap");                     // 原 42 → 44
        modules.add("Wave3-Misc  Affix/Drawer");                                           // 原 43 → 45
        modules.add("Wave4-Pickers  Calendar/DateRange/Time/Cascader");                    // 原 44 → 46
        modules.add("Wave4-Display  Anchor/Comment/Circular/RangeSlider");                 // 原 45 → 47
        modules.add("Wave4-Form  FormValidator/Rule");                                     // 原 46 → 48
        modules.add("Wave4-Data  DnD/VirtualList/DataGrid");                               // 原 47 → 49
        return modules;
    }

    private void setStatus(String text) {
        statusLine.setText(text);
    }

    /** 把对话框页尺寸下拉索引映射成像素值（顺序与 showExportDialog 中 pageSizeItems 一致）。 */
    private static int parsePageSize(int selectedIndex) {
        switch (selectedIndex) {
            case 1:  return 2048;
            case 2:  return 4096;
            case 3:  return 512;
            default: return 1024;
        }
    }

    // =================== 皮肤导出 ===================

    /**
     * 弹出导出皮肤对话框：让用户输入导出名 + 选择字符集，确认后调用
     * {@link BsSkinExporter#export} 把当前 skin 导出到本地目录。
     */
    private void showExportDialog() {
        final String defaultName = BsUI.currentThemeName() == null ? "bs-skin" : BsUI.currentThemeName();
        final com.badlogic.gdx.scenes.scene2d.ui.TextField nameField =
                new com.badlogic.gdx.scenes.scene2d.ui.TextField(defaultName, skin);

        final java.util.List<String[]> charsEntries = app.availableCharsEntries();
        final com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String> charsBox =
                new com.badlogic.gdx.scenes.scene2d.ui.SelectBox<>(skin);
        com.badlogic.gdx.utils.Array<String> charsItems = new com.badlogic.gdx.utils.Array<>();
        for (String[] entry : charsEntries) charsItems.add(entry[0]);
        charsBox.setItems(charsItems);
        charsBox.setSelectedIndex(0);

        final BsCheckBox bitmapBox = new BsCheckBox("烘焙 BitmapFont（.fnt + .png）", skin);

        // 字号多选：基于 skin 实际存在的 font-{suffix} 生成；缺档不暴露给用户
        final java.util.List<String> availableSizes = app.availableFontSizes();
        final java.util.Map<String, BsCheckBox> sizeBoxes = new java.util.LinkedHashMap<>();
        if (!availableSizes.isEmpty()) {
            // 默认全选
            for (String suf : availableSizes) {
                BsCheckBox cb = new BsCheckBox(suf, skin);
                cb.setChecked(true);
                sizeBoxes.put(suf, cb);
            }
        }

        // 独立烘焙 default-font：默认勾选。default 字号从 App 取（用户改字号代码后此值同步变）
        final int defaultSize = app.defaultFontSize();
        final BsCheckBox defaultFontBox = new BsCheckBox(
                "独立烘焙 default-font (" + defaultSize + "px)", skin);
        defaultFontBox.setChecked(true);

        // 字体页尺寸：默认 1024，可选 512/1024/2048/4096。页越大产 png 越少，但单纹理越大
        final com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String> pageSizeBox =
                new com.badlogic.gdx.scenes.scene2d.ui.SelectBox<>(skin);
        com.badlogic.gdx.utils.Array<String> pageSizeItems = new com.badlogic.gdx.utils.Array<>();
        pageSizeItems.add("1024 × 1024（默认，兼容性最好）");
        pageSizeItems.add("2048 × 2048（页更少，推荐桌面/teaVM）");
        pageSizeItems.add("4096 × 4096（极大，仅高内存设备）");
        pageSizeItems.add("512 × 512（极小，仅 ASCII 场景）");
        pageSizeBox.setItems(pageSizeItems);
        pageSizeBox.setSelectedIndex(0);

        Table form = new Table(skin);
        form.defaults().pad(6).left().growX();
        form.add(new Label("导出名（生成 <名>.json / .atlas / .png）", skin)).row();
        form.add(nameField).growX().row();
        form.add(new Label("字符集（影响字体生成范围与加载速度）", skin)).padTop(4).row();
        form.add(charsBox).growX().left().row();
        if (!sizeBoxes.isEmpty()) {
            form.add(new Label("导出字号（缺档不显示）", skin)).padTop(4).row();
            Table sizeRow = new Table(skin);
            for (BsCheckBox cb : sizeBoxes.values()) {
                sizeRow.add(cb).padRight(8);
            }
            form.add(sizeRow).growX().left().row();
        }
        form.add(defaultFontBox).padTop(4).left().row();
        form.add(new Label("字体页尺寸（仅烘焙模式；页越大字体 png 越少）", skin)).padTop(4).row();
        form.add(pageSizeBox).growX().left().row();
        form.add(bitmapBox).padTop(4).left().row();
        Label.LabelStyle hintStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        hintStyle.font = skin.getFont("font-sm");
        Label hint = new Label("目录：bs-skin-export/   ·   多主题共用字体与字符集   ·   勾选「烘焙 BitmapFont」可让运行时免 FreeType/TTF/freetype.js", hintStyle);
        hint.setColor(BsTheme.tm());
        hint.setWrap(true);
        form.add(hint).padTop(4).growX().row();

        new BsModal("导出皮肤", skin)
                .content(form)
                .contentWidth(440)
                .separator(true)
                .addButton("取消", () -> setStatus("导出取消"), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE)
                .addButton("导出", () -> {
                        // 收集勾选的字号；空集 → BsSkinExporter 不导出任何 font-* 档位
                        java.util.Set<String> picked = new java.util.LinkedHashSet<>();
                        for (java.util.Map.Entry<String, BsCheckBox> en : sizeBoxes.entrySet()) {
                            if (en.getValue().isChecked()) picked.add(en.getKey());
                        }
                        int pageSize = parsePageSize(pageSizeBox.getSelectedIndex());
                        doExport(nameField.getText(),
                                charsEntries.get(charsBox.getSelectedIndex())[1],
                                bitmapBox.isChecked(),
                                picked,
                                defaultFontBox.isChecked(),
                                defaultSize,
                                pageSize);
                    },
                        BsButton.Variant.PRIMARY, BsButton.Style.SOLID)
                .showModal(stage);
    }

    /**
     * 执行导出：在 GL 线程同步导出（含 PixmapPacker / 文件 IO），完成后更新状态栏。
     *
     * @param sizeSuffixes      要导出的字号后缀集合（{@code null} = 全部，空集合 = 不导出任何 font-* 档位）
     * @param includeDefaultFont 是否独立写入 default-font 段
     * @param defaultFontSize   default 字号（includeDefaultFont=true 时用）
     * @param fontPageSize      字体页尺寸（1024/2048/4096/512）
     */
    private void doExport(String nameRaw, String charsCp, boolean bitmapFont,
                          java.util.Set<String> sizeSuffixes,
                          boolean includeDefaultFont, int defaultFontSize,
                          int fontPageSize) {
        final String name = (nameRaw == null || nameRaw.trim().isEmpty()) ? "bs-skin" : nameRaw.trim();
        final BsSkinApp appRef = this.app;
        Gdx.app.postRunnable(() -> {
            try {
                com.badlogic.gdx.files.FileHandle outDir =
                        Gdx.files.local("bs-skin-export");
                if (!outDir.exists()) outDir.mkdirs();

                com.badlogic.gdx.files.FileHandle ttfSource =
                        Gdx.files.internal(appRef.ttfPath());
                com.badlogic.gdx.files.FileHandle charsFile =
                        Gdx.files.internal(charsCp);

                long t0 = System.currentTimeMillis();
                BsSkinExporter.export(skin, outDir, name, ttfSource, charsFile, bitmapFont,
                        sizeSuffixes, includeDefaultFont, defaultFontSize, fontPageSize);
                long elapsed = System.currentTimeMillis() - t0;

                String charsName = charsFile.name();
                String msg = "✓ 导出完成：" + outDir.path() + "/" + name + ".*   ("
                        + elapsed + "ms, " + charsName + ")";
                log.info(msg);
                setStatus(msg);
                showExportResultDialog(true, outDir.path(), null);
            } catch (Throwable t) {
                log.error("皮肤导出失败", t);
                setStatus("✗ 导出失败：" + t.getMessage());
                showExportResultDialog(false, null, t.getMessage());
            }
        });
    }

    /** 导出结果提示框（成功 / 失败）。 */
    private void showExportResultDialog(boolean ok, String path, String error) {
        Label content = new Label(ok
                ? "已导出到：\n" + path + "\n\n包含：json + atlas + png + ttf + 字符集 txt"
                : "导出失败：\n" + error,
                skin);
        content.setWrap(true);
        new BsModal(ok ? "导出成功" : "导出失败", skin)
                .content(content)
                .contentWidth(440)
                .separator(true)
                .addButton("关闭", () -> {}, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE)
                .showModal(stage);
    }

    // =================== 模块路由 ===================

    private void switchModule(int index) {
        contentHost.clearChildren();
        Table content = new Table(skin);
        content.defaults().pad(6).left();
        fillModuleContent(index, content);
        contentHost.add(content).growX().top();
        contentHost.row();
        contentHost.add().expandY();   // 占位避免内容太短时贴底
    }

    /**
     * 把指定模块（0~49）的内容填充到外部 host Table。供 admin UiDemo 模块复用，
     * 避免重复实现 50 个 fill 方法。
     *
     * @param index 模块索引（对应 {@link #MODULES} 列表顺序）
     * @param host  外部容器（建议 defaults().pad(6).left()）
     */
    public void fillModuleContent(int index, Table host) {
        ensureModules();   // 懒构造（工厂模式下 stage 可能刚由 setStage 注入）
        switch (index) {
            case 0: basicMods.fillLabels(host); break;
            case 1: basicMods.fillButtons(host); break;
            case 2: basicMods.fillImageButtons(host); break;
            case 3: basicMods.fillInputs(host); break;
            case 4: basicMods.fillSelects(host); break;
            case 5: basicMods.fillRadioCheck(host); break;
            case 6: basicMods.fillSliders(host); break;
            case 7: basicMods.fillMisc(host); break;
            case 8: basicMods.fillMenuBar(host); break;
            case 9: basicMods.fillContextMenu(host); break;     // 新增
            case 10: formMods.fillPickers(host); break;
            case 11: formMods.fillForm(host); break;
            case 12: formMods.fillDateTime(host); break;
            case 13: dataMods.fillTree(host); break;
            case 14: dataMods.fillTable(host); break;
            case 15: formMods.fillOverlay(host); break;
            case 16: formMods.fillModal(host); break;
            case 17: formMods.fillDialogs(host); break;
            case 18: dataMods.fillCards(host); break;
            case 19: dataMods.fillBadge(host); break;
            case 20: dataMods.fillProfile(host); break;
            case 21: dataMods.fillLayout(host); break;
            case 22: dataMods.fillBreadcrumb(host); break;
            case 23: feedbackMods.fillIcons(host); break;
            case 24: feedbackMods.fillProgressToast(host); break;
            case 25: formMods.fillCollapseAccordion(host); break;
            case 26: feedbackMods.fillButtonGroupAlert(host); break;
            case 27: formMods.fillInputNumberGroup(host); break;
            case 28: dataMods.fillNavbarOffcanvas(host); break;
            case 29: chartMods.fillChartsLine(host); break;
            case 30: chartMods.fillChartsBar(host); break;
            case 31: chartMods.fillChartsPie(host); break;
            case 32: chartMods.fillChartsLegend(host); break;
            case 33: chartMods.fillChartsHover(host); break;
            case 34: chartMods.fillChartsBar3D(host); break;   // 新增
            case 35: waveMods.fillP2Content(host); break;
            case 36: waveMods.fillP2Carousel(host); break;
            case 37: chartMods.fillChartsExtended(host); break;
            case 38: waveMods.fillWave1Basics(host); break;
            case 39: waveMods.fillWave1Inputs(host); break;
            case 40: waveMods.fillWave1Feedback(host); break;
            case 41: waveMods.fillWave2Data(host); break;
            case 42: waveMods.fillWave3Editor(host); break;
            case 43: waveMods.fillWave2Business(host); break;
            case 44: waveMods.fillWave3EditorPro(host); break;
            case 45: waveMods.fillWave3Misc(host); break;
            case 46: waveMods.fillWave4Pickers(host); break;
            case 47: waveMods.fillWave4Display(host); break;
            case 48: waveMods.fillWave4Form(host); break;
            case 49: waveMods.fillWave4Data(host); break;
            default:
                host.add(new Label("(未知模块)", skin));
        }
    }

    // =================== Screen 生命周期 ===================

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // 用当前主题 body 底色清屏：写死浅灰会让 Dark 主题边缘仍显示浅色
        com.badlogic.gdx.graphics.Color bb = BsTheme.bgBodyColor();
        ScreenUtils.clear(bb.r, bb.g, bb.b, 1f);
        // 同步 Shift/Ctrl 修饰键给图表（折线点击隔离用）
        BsLineChart.setModifiers(
                Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_LEFT)
                        || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_RIGHT),
                Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.CONTROL_LEFT)
                        || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.CONTROL_RIGHT));
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
