package com.git.bs.demo;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.ScreenAdapter;
import com.git.bs.demo.modules.BsBasicModules;
import com.git.bs.demo.modules.BsChartModules;
import com.git.bs.demo.modules.BsDataModules;
import com.git.bs.demo.modules.BsFeedbackModules;
import com.git.bs.demo.modules.BsFormModules;
import com.git.bs.demo.modules.BsWaveModules;
import com.git.bs.game.BsControlsTestApp;
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
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

/**
 * Bs UI 控件测试主屏幕。
 *
 * <p>本类只保留：布局骨架（顶部标题栏 + 左导航 + 右内容 + 底部状态栏）、模块路由
 * ({@link #switchModule})、屏幕生命周期 (show/render/resize/dispose)，以及与布局耦合较深
 * 的皮肤导出对话框。48+ 个 {@code fillXxx} 内容方法已按分类拆分到
 * {@link com.git.bs.demo.modules} 下的 6 个 module 类。</p>
 */
@Slf4j
public class BsControlsTestScreen extends ScreenAdapter {

    public static final int WIN_W = 1280;
    public static final int WIN_H = 800;

    private final BsControlsTestApp app;
    private final Skin skin;
    private final Stage stage;

    /** 右侧内容容器；切换模块时 clear + add。 */
    private Table contentHost;
    private BsScrollPane contentScroll;

    /** 底部状态行：显示最近一次控件事件。 */
    private Label statusLine;

    /** 左侧模块导航（重建 UI 时需要恢复选中索引）。 */
    private BsList<String> nav;

    /** 模块列表（在 buildLayout 重建时复用）。 */
    private static final java.util.List<String> MODULES = buildModuleList();

    /** 主题切换下拉（重建后恢复选中项）。 */
    private com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String> themeToggle;

    // =================== 6 个 module 路由 ===================
    private BsBasicModules basicMods;
    private BsFormModules formMods;
    private BsDataModules dataMods;
    private BsFeedbackModules feedbackMods;
    private BsChartModules chartMods;
    private BsWaveModules waveMods;

    public BsControlsTestScreen(BsControlsTestApp app) {
        this.app = app;
        this.skin = app.getSkin();
        this.stage = new Stage(new StretchViewport(WIN_W, WIN_H));

        // 初始化 6 个 module（传入共享依赖；不再依赖主类实例）
        basicMods    = new BsBasicModules(skin, stage, this::setStatus);
        formMods     = new BsFormModules(skin, stage, this::setStatus);
        dataMods     = new BsDataModules(skin, stage, this::setStatus);
        feedbackMods = new BsFeedbackModules(skin, stage, this::setStatus);
        chartMods    = new BsChartModules(skin);
        waveMods     = new BsWaveModules(skin, stage, this::setStatus);

        buildLayout();
        switchModule(0);
        // 主题切换由 BsControlsTestApp.applyTheme 处理（重建整个 screen），本类不监听
    }

    /**
     * 构建 root + 顶部标题栏 + 左导航 + 右内容 + 底部状态栏。
     * <p>首次构造和主题切换重建 UI 都走这里。</p>
     */
    private void buildLayout() {
        Table root = new Table();
        root.setBackground(skin.newDrawable("white", BsTheme.bb()));
        root.setFillParent(true);
        stage.addActor(root);

        // 顶部标题行：左标题 + 右 Dark 切换按钮
        Table header = new Table();
        Label title = new Label("Bs UI 控件测试台  /  Bootstrap-styled controls playground",
                skin, "default");
        title.setColor(BsTheme.tp());
        title.setFontScale(1.6f);
        header.add(title).pad(10).left().growX();

        // 主题切换下拉：light / dark / admin 三选一
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

        // 导出皮肤按钮：弹对话框确认导出名 / 字符集 / 目录
        BsButton exportBtn = new BsButton("⭳ 导出皮肤", skin,
                BsButton.Variant.SUCCESS, BsButton.Style.SOLID, BsButton.Size.SM);
        exportBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                showExportDialog();
            }
        });
        header.add(exportBtn).pad(8).right();
        root.add(header).growX().row();

        // 主区：左导航 + 右内容（可滚动）
        Table body = new Table();
        nav = new BsList<>(skin);
        nav.setItems(com.git.bs.demo.modules.ModuleSupport.items(MODULES));
        nav.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                int idx = nav.getSelectedIndex();
                if (idx >= 0) switchModule(idx);
            }
        });
        BsScrollPane navScroll = new BsScrollPane(nav, skin);
        navScroll.setFadeScrollBars(false);
        navScroll.setScrollingDisabled(true, false); // 只允许纵向滚动
        Container<Actor> navWrap = new Container<>(navScroll);
        navWrap.background(skin.newDrawable("white", BsTheme.bs())).pad(4);
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
     */
    private static java.util.List<String> buildModuleList() {
        java.util.List<String> modules = new java.util.ArrayList<>();
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

    // =================== 皮肤导出 ===================

    /**
     * 弹出导出皮肤对话框：让用户输入导出名（默认按当前主题命名）+ 选择字符集，
     * 确认后调用 {@link BsSkinExporter#export} 把当前 skin 导出到本地目录。
     *
     * <p>输出结构：{@code <Gdx.files.local(bs-skin-export/<name>)/} 下
     * {@code <name>.json} + {@code <name>.atlas} + {@code <name>.png} + {@code ttf/} + 字符集 txt。</p>
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

        Table form = new Table(skin);
        form.defaults().pad(6).left().growX();
        form.add(new Label("导出名（生成 <名>.json / .atlas / .png）", skin)).row();
        form.add(nameField).growX().row();
        form.add(new Label("字符集（影响字体生成范围与加载速度）", skin)).padTop(4).row();
        form.add(charsBox).growX().left().row();
        form.add(bitmapBox).padTop(4).left().row();
        Label hint = new Label("目录：bs-skin-export/   ·   多主题共用字体与字符集   ·   勾选「烘焙 BitmapFont」可让运行时免 FreeType/TTF/freetype.js", skin);
        hint.setColor(BsTheme.tm());
        hint.setFontScale(0.9f);
        hint.setWrap(true);
        form.add(hint).padTop(4).growX().row();

        new BsModal("导出皮肤", skin)
                .content(form)
                .contentWidth(440)
                .separator(true)
                .addButton("取消", () -> setStatus("导出取消"), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE)
                .addButton("导出", () -> doExport(nameField.getText(),
                                charsEntries.get(charsBox.getSelectedIndex())[1],
                                bitmapBox.isChecked()),
                        BsButton.Variant.PRIMARY, BsButton.Style.SOLID)
                .showModal(stage);
    }

    /**
     * 执行导出：在 GL 线程同步导出（含 PixmapPacker / 文件 IO，耗时约几十~几百毫秒），
     * 完成后更新状态栏。
     *
     * @param nameRaw     导出名（生成 <名>.json/.atlas/.png）
     * @param charsCp     字符集文件 classpath 路径（如 com/git/bs/ui/skin/chinese.txt）
     */
    private void doExport(String nameRaw, String charsCp, boolean bitmapFont) {
        final String name = (nameRaw == null || nameRaw.trim().isEmpty()) ? "bs-skin" : nameRaw.trim();
        final BsControlsTestApp appRef = this.app;
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
                BsSkinExporter.export(skin, outDir, name, ttfSource, charsFile, bitmapFont);
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

    /**
     * 模块路由：按 index 委托到对应 module 类的 fillXxx 方法。
     *
     * <p>索引 9 是新增的 ContextMenu；原 Pickers 及之后索引全部 +1。</p>
     */
    private void switchModule(int index) {
        contentHost.clearChildren();
        Table content = new Table(skin);
        content.defaults().pad(6).left();
        switch (index) {
            case 0: basicMods.fillLabels(content); break;
            case 1: basicMods.fillButtons(content); break;
            case 2: basicMods.fillImageButtons(content); break;
            case 3: basicMods.fillInputs(content); break;
            case 4: basicMods.fillSelects(content); break;
            case 5: basicMods.fillRadioCheck(content); break;
            case 6: basicMods.fillSliders(content); break;
            case 7: basicMods.fillMisc(content); break;
            case 8: basicMods.fillMenuBar(content); break;
            case 9: basicMods.fillContextMenu(content); break;     // 新增
            case 10: formMods.fillPickers(content); break;
            case 11: formMods.fillForm(content); break;
            case 12: formMods.fillDateTime(content); break;
            case 13: dataMods.fillTree(content); break;
            case 14: dataMods.fillTable(content); break;
            case 15: formMods.fillOverlay(content); break;
            case 16: formMods.fillModal(content); break;
            case 17: formMods.fillDialogs(content); break;
            case 18: dataMods.fillCards(content); break;
            case 19: dataMods.fillBadge(content); break;
            case 20: dataMods.fillProfile(content); break;
            case 21: dataMods.fillLayout(content); break;
            case 22: dataMods.fillBreadcrumb(content); break;
            case 23: feedbackMods.fillIcons(content); break;
            case 24: feedbackMods.fillProgressToast(content); break;
            case 25: formMods.fillCollapseAccordion(content); break;
            case 26: feedbackMods.fillButtonGroupAlert(content); break;
            case 27: formMods.fillInputNumberGroup(content); break;
            case 28: dataMods.fillNavbarOffcanvas(content); break;
            case 29: chartMods.fillChartsLine(content); break;
            case 30: chartMods.fillChartsBar(content); break;
            case 31: chartMods.fillChartsPie(content); break;
            case 32: chartMods.fillChartsLegend(content); break;
            case 33: chartMods.fillChartsHover(content); break;
            case 34: chartMods.fillChartsBar3D(content); break;   // 新增
            case 35: waveMods.fillP2Content(content); break;
            case 36: waveMods.fillP2Carousel(content); break;
            case 37: chartMods.fillChartsExtended(content); break;
            case 38: waveMods.fillWave1Basics(content); break;
            case 39: waveMods.fillWave1Inputs(content); break;
            case 40: waveMods.fillWave1Feedback(content); break;
            case 41: waveMods.fillWave2Data(content); break;
            case 42: waveMods.fillWave3Editor(content); break;
            case 43: waveMods.fillWave2Business(content); break;
            case 44: waveMods.fillWave3EditorPro(content); break;
            case 45: waveMods.fillWave3Misc(content); break;
            case 46: waveMods.fillWave4Pickers(content); break;
            case 47: waveMods.fillWave4Display(content); break;
            case 48: waveMods.fillWave4Form(content); break;
            case 49: waveMods.fillWave4Data(content); break;
            default:
                content.add(new Label("(未知模块)", skin));
        }
        contentHost.add(content).growX().top();
        contentHost.row();
        // 占位避免内容太短时贴底
        contentHost.add().expandY();
    }

    // =================== Screen 生命周期 ===================

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // 用当前主题 body 底色清屏（与 BsControlsTestApp.render 的清屏一致）：
        // 写死浅灰会让 Dark 主题下根 actor 之外的边缘区域仍显示浅色
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
