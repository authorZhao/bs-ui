package com.git.bs.demo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.git.bs.game.BsControlsTestApp;
import com.git.bs.game.BsSkinApp;
import com.git.bs.ui.*;
import com.git.bs.ui.ext.BsDataGrid;
import com.git.bs.ui.ext.BsDnd;
import com.git.bs.ui.ext.BsFormValidator;
import com.git.bs.ui.ext.BsRule;
import com.git.bs.ui.ext.BsVirtualList;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Bs UI 控件测试主屏幕。
 * <p>左侧 BsList 模块导航，右侧 ScrollPane 内容区展示当前模块的所有控件 variant。
 * 模块：Labels / Buttons / ImageButton / Inputs / Selects / RadioCheck / Slider / Misc。</p>
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

    /** 左侧模块导航（重建 UI 时需要恢复选中索引）。 */
    private BsList<String> nav;

    /** 当前模块索引（用于主题切换重建 UI 后恢复）。 */
    private int currentModuleIndex = 0;

    /** 模块列表（在 buildLayout 重建时复用）。 */
    /** 全部 44 个控件演示模块名（按顺序对应 fillModuleContent 的 index）。 */
    public static final List<String> MODULES = buildModuleList();

    /** 主题切换下拉（重建后恢复选中项）。 */
    private com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String> themeToggle;

    public BsControlsSkinScreen(BsSkinApp app) {
        this.app = app;
        this.skin = app.getSkin();
        this.stage = new Stage(new StretchViewport(WIN_W, WIN_H));

        buildLayout();
        switchModule(0);
        // 主题切换由 BsControlsTestApp.applyTheme 处理（重建整个 screen），本类不监听
    }

    /**
     * 内容工厂模式构造：只用于复用 44 个 fill 方法（admin UiDemo 模块用），
     * 不建 stage / nav / root，避免无用开销。fillModuleContent 可直接调用。
     *
     * @param skin 当前 skin
     */
    public BsControlsSkinScreen(Skin skin) {
        this.app = null;
        this.skin = skin;
        // 创建一个独立的 Stage 供 BsTooltip/BsPopover 等 attach 使用（admin 模块不 render 这个 stage，
        // tooltip 不会显示，但避免 attach(null) NPE）
        this.stage = new Stage(new com.badlogic.gdx.utils.viewport.ScreenViewport());
        this.statusLine = new Label("(就绪)", skin);
        this.statusLine.setColor(com.git.bs.ui.BsTheme.ts());
    }

    /**
     * 注入真实 stage（admin shell 构造后调用）。
     * <p>工厂模式构造（{@link #BsControlsSkinScreen(Skin)}）创建的是独立未渲染的 stage，
     * 导致 {@code BsModal.showModal(this.stage)} / {@code BsTooltip.attach(this.stage)} 等弹窗失效。
     * admin shell 注入真实 stage 后，Pickers/DateTime/Overlay/Modal/Dialogs 等模块才能正常弹窗。</p>
     */
    public void setStage(Stage s) {
        this.stage = s;
    }

    /**
     * 构建 root + 顶部标题栏 + 左导航 + 右内容 + 底部状态栏。
     * <p>首次构造和主题切换重建 UI 都走这里。</p>
     */
    private void buildLayout() {
        Table root = new Table();
        root.setBackground(skin.newDrawable("white", com.git.bs.ui.BsTheme.bb()));
        root.setFillParent(true);
        stage.addActor(root);

        // 顶部标题行：左标题 + 右 Dark 切换按钮
        Table header = new Table();
        Label title = new Label("Bs UI 控件测试台  /  Bootstrap-styled controls playground",
                skin, "default");
        title.setColor(com.git.bs.ui.BsTheme.tp());
        title.setFontScale(1.6f);
        header.add(title).pad(10).left().growX();

        // 主题切换下拉：light / dark / admin 三选一
        themeToggle = new com.badlogic.gdx.scenes.scene2d.ui.SelectBox<>(skin);
        com.badlogic.gdx.utils.Array<String> themeNames = new com.badlogic.gdx.utils.Array<>();
        themeNames.add("☀ Light");
        themeNames.add("🌙 Dark");
        themeNames.add("🛡 Admin");
        themeToggle.setItems(themeNames);
        // 按当前主题恢复选中项
        String currentName = BsUI.currentThemeName();
        if (currentName == null) currentName = "";
        themeToggle.setSelectedIndex(
                currentName.contains("admin") ? 2
                : currentName.contains("dark") ? 1 : 0);
        themeToggle.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                int idx = themeToggle.getSelectedIndex();
                com.git.bs.ui.BsTheme target = idx == 2 ? com.git.bs.ui.BsAdminTheme.INSTANCE
                        : idx == 1 ? BsDarkTheme.INSTANCE : BsLightTheme.INSTANCE;
                // 只调 setTheme，由 BsSkinApp 的 listener 处理重建
                BsUI.setTheme(target);
            }
        });
        header.add(themeToggle).pad(8).right();

        // 导出皮肤按钮：弹对话框确认导出名 / 目录
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
        nav.setItems(items(MODULES));
        nav.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                int idx = nav.getSelectedIndex();
                if (idx >= 0) switchModule(idx);
            }
        });
        // 导航用 ScrollPane 包，避免模块多时导航项超出可视区域
        BsScrollPane navScroll = new BsScrollPane(nav, skin);
        navScroll.setFadeScrollBars(false);
        navScroll.setScrollingDisabled(true, false); // 只允许纵向滚动
        Container<Actor> navWrap = new Container<>(navScroll);
        navWrap.background(skin.newDrawable("white", com.git.bs.ui.BsTheme.bs())).pad(4);
        navWrap.fill();
        body.add(navWrap).width(220).top().padRight(8).expandY();

        contentHost = new Table();
        contentScroll = new BsScrollPane(contentHost, skin);
        contentScroll.setFadeScrollBars(false);
        body.add(contentScroll).grow().top();

        root.add(body).grow().padLeft(10).padRight(10).row();

        // 底部状态栏
        statusLine = new Label("(就绪)", skin);
        statusLine.setColor(com.git.bs.ui.BsTheme.ts());
        root.add(statusLine).pad(8).expandX().left().row();
    }

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
        modules.add("Pickers  选择器（日期/颜色）");
        modules.add("Form  表单（校验）");
        modules.add("DateTime  日期时间选择器");
        modules.add("Tree  树状列表");
        modules.add("Table  表格 + 分页");
        modules.add("Overlay  Tooltip/Spinner/Popover/Link");
        modules.add("Modal  通用模态框");
        modules.add("Dialogs  对话框库（动画）");
        modules.add("Cards  卡片");
        modules.add("Badge  徽标");
        modules.add("Profile  个人面板");
        modules.add("Layout  管理后台");
        modules.add("Breadcrumb  面包屑");
        modules.add("Icons  图标库");
        modules.add("Progress & Toast  进度条/轻提示");
        modules.add("Collapse & Accordion  折叠");
        modules.add("ButtonGroup & Alert  按钮组/警告条");
        modules.add("InputNumber & InputGroup  数字/输入组");
        modules.add("Navbar & Offcanvas  导航栏/抽屉");
        modules.add("Charts-Line  折线图");
        modules.add("Charts-Bar  柱状图");
        modules.add("Charts-Pie  饼图");
        modules.add("Charts-Legend  图例与系列切换");
        modules.add("Charts-Hover  Hover 数据查看");
        modules.add("P2-Content  Placeholder/Figure/ListGroup/FloatingLabel");
        modules.add("P2-Carousel  轮播图");
        modules.add("Charts-Extended  Area/Spline/Scatter/Radar/Doughnut");
        modules.add("Wave1-Basics  Switch/Avatar/Timeline/Statistic/Steps/Empty/Rating");
        modules.add("Wave1-Inputs  AutoComplete/TagInput/DescriptionList");
        modules.add("Wave1-Feedback  Result/LoadingOverlay");
        modules.add("Wave2-Data  DataTable/PropertySheet");
        modules.add("Wave3-Editor  StatusBar");
        modules.add("Wave2-Business  SearchBar/Toolbar/FileItem/Transfer");
        modules.add("Wave3-EditorPro  Inspector/NodePalette/MiniMap");
        modules.add("Wave3-Misc  Affix/Drawer");
        modules.add("Wave4-Pickers  Calendar/DateRange/Time/Cascader");
        modules.add("Wave4-Display  Anchor/Comment/Circular/RangeSlider");
        modules.add("Wave4-Form  FormValidator/Rule");
        modules.add("Wave4-Data  DnD/VirtualList/DataGrid");
        return modules;
    }

    private static com.badlogic.gdx.utils.Array<String> items(List<String> in) {
        com.badlogic.gdx.utils.Array<String> a = new com.badlogic.gdx.utils.Array<>(in.size());
        for (String s : in) a.add(s);
        return a;
    }

    private void setStatus(String text) {
        statusLine.setText(text);
    }

    // =================== 皮肤导出 ===================

    /**
     * 弹出导出皮肤对话框：让用户输入导出名（默认按当前主题命名），
     * 确认后调用 {@link BsSkinExporter#export} 把当前 skin 导出到本地目录。
     *
     * <p>输出结构：{@code <Gdx.files.local(bs-skin-export/<name>)/} 下
     * {@code <name>.json} + {@code <name>.atlas} + {@code <name>.png} + {@code ttf/} + {@code chinese.txt}。</p>
     */
    private void showExportDialog() {
        // 默认导出名 = theme.name()（如 bs-light / bs-dark，theme.name 本身已含 bs- 前缀）
        final String defaultName = BsUI.currentThemeName() == null ? "bs-skin" : BsUI.currentThemeName();
        final TextField nameField = new TextField(defaultName, skin);

        // 字符集选择：根据 classpath 实际存在的文件动态填充（避免暴露不存在的选项）
        final java.util.List<String[]> charsEntries = app.availableCharsEntries();
        final SelectBox<String> charsBox = new SelectBox<>(skin);
        com.badlogic.gdx.utils.Array<String> charsItems = new com.badlogic.gdx.utils.Array<>();
        for (String[] entry : charsEntries) charsItems.add(entry[0]);
        charsBox.setItems(charsItems);
        charsBox.setSelectedIndex(0);

        Table form = new Table(skin);
        form.defaults().pad(6).left().growX();
        form.add(new Label("导出名（生成 <名>.json / .atlas / .png）", skin)).row();
        form.add(nameField).growX().row();
        form.add(new Label("字符集（影响字体生成范围与加载速度）", skin)).padTop(4).row();
        form.add(charsBox).growX().left().row();
        Label hint = new Label("目录：bs-skin-export/   ·   多主题共用字体与字符集，主题资源以导出名区分", skin);
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
                                charsEntries.get(charsBox.getSelectedIndex())[1]),
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
    private void doExport(String nameRaw, String charsCp) {
        final String name = (nameRaw == null || nameRaw.trim().isEmpty()) ? "bs-skin" : nameRaw.trim();
        final BsSkinApp appRef = this.app;
        // 导出涉及文件 IO 和 PixmapPacker，放下一帧执行避免阻塞 modal 关闭动画
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
                BsSkinExporter.export(skin, outDir, name, ttfSource, charsFile);
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

    private void switchModule(int index) {
        currentModuleIndex = index;
        contentHost.clearChildren();
        Table content = new Table(skin);
        content.defaults().pad(6).left();
        fillModuleContent(index, content);
        contentHost.add(content).growX().top();
        contentHost.row();
        // 占位避免内容太短时贴底
        contentHost.add().expandY();
    }

    /**
     * 把指定模块（0~43）的内容填充到外部 host Table。供 admin UiDemo 模块复用，
     * 避免重复实现 44 个 fill 方法。
     *
     * @param index 模块索引（对应 MODULES 列表顺序）
     * @param host  外部容器（建议 defaults().pad(6).left()）
     */
    public void fillModuleContent(int index, Table host) {
        switch (index) {
            case 0: fillLabels(host); break;
            case 1: fillButtons(host); break;
            case 2: fillImageButtons(host); break;
            case 3: fillInputs(host); break;
            case 4: fillSelects(host); break;
            case 5: fillRadioCheck(host); break;
            case 6: fillSliders(host); break;
            case 7: fillMisc(host); break;
            case 8: fillMenuBar(host); break;
            case 9: fillPickers(host); break;
            case 10: fillForm(host); break;
            case 11: fillDateTime(host); break;
            case 12: fillTree(host); break;
            case 13: fillTable(host); break;
            case 14: fillOverlay(host); break;
            case 15: fillModal(host); break;
            case 16: fillDialogs(host); break;
            case 17: fillCards(host); break;
            case 18: fillBadge(host); break;
            case 19: fillProfile(host); break;
            case 20: fillLayout(host); break;
            case 21: fillBreadcrumb(host); break;
            case 22: fillIcons(host); break;
            case 23: fillProgressToast(host); break;
            case 24: fillCollapseAccordion(host); break;
            case 25: fillButtonGroupAlert(host); break;
            case 26: fillInputNumberGroup(host); break;
            case 27: fillNavbarOffcanvas(host); break;
            case 28: fillChartsLine(host); break;
            case 29: fillChartsBar(host); break;
            case 30: fillChartsPie(host); break;
            case 31: fillChartsLegend(host); break;
            case 32: fillChartsHover(host); break;
            case 33: fillP2Content(host); break;
            case 34: fillP2Carousel(host); break;
            case 35: fillChartsExtended(host); break;
            case 36: fillWave1Basics(host); break;
            case 37: fillWave1Inputs(host); break;
            case 38: fillWave1Feedback(host); break;
            case 39: fillWave2Data(host); break;
            case 40: fillWave3Editor(host); break;
            case 41: fillWave2Business(host); break;
            case 42: fillWave3EditorPro(host); break;
            case 43: fillWave3Misc(host); break;
            case 44: fillWave4Pickers(host); break;
            case 45: fillWave4Display(host); break;
            case 46: fillWave4Form(host); break;
            case 47: fillWave4Data(host); break;
            default:
                host.add(new Label("(未知模块)", skin));
        }
    }

    // ============================ Labels ============================
    private void fillLabels(Table c) {
        c.add(sectionTitle("Labels  —— 标签（多种 Variant 与字号）")).row();

        c.add(new Label("普通文本 Label —— Bootstrap 风格深灰字", skin)).row();

        // 彩色 Label：放进独立的横向 Table，避免外层 row 把它们挤到右侧不可见
        c.add(new Label("彩色 Label（BsStatusLabel 6 variant）：", skin)).left().row();
        Table colorRow = new Table();
        colorRow.defaults().pad(4);
        for (BsStatusLabel.Variant v : BsStatusLabel.Variant.values()) {
            colorRow.add(new BsStatusLabel(v.name(), skin, v));
        }
        c.add(colorRow).left().row();

        // 多字号 Label：用 label-sm/md/lg/xl LabelStyle（已由 BsControlsTestApp 注入）
        c.add(new Label("多字号 Label：", skin)).left().padTop(8).row();
        Table sizeRow = new Table();
        sizeRow.defaults().pad(6).left();
        sizeRow.add(new Label("SM size=14", skin, "label-sm"));
        sizeRow.add(new Label("MD size=18 (default)", skin, "label-md"));
        sizeRow.row();
        sizeRow.add(new Label("LG size=24", skin, "label-lg"));
        sizeRow.add(new Label("XL size=32", skin, "label-xl"));
        c.add(sizeRow).left().row();

        c.add(new Label("禁用态 Label：", skin)).left().padTop(8).row();
        Label disabled = new Label("disabled 风格（灰色）", skin);
        disabled.setColor(Color.GRAY);
        c.add(disabled).row();

        c.add(new Label("长文本示例：", skin)).left().padTop(8).row();
        c.add(new Label("Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                + "支持自动换行的 Label 在 Table 里会按 cell 宽度展开。", skin))
                .width(700).row();
    }

    // ============================ Buttons ============================
    private void fillButtons(Table c) {
        c.add(sectionTitle("Buttons  —— 文字按钮（6 色 × Solid/Outline × 3 尺寸）")).row();
        for (BsButton.Style st : BsButton.Style.values()) {
            c.add(new Label(st.name() + ":", skin)).padRight(6);
            for (BsButton.Variant v : BsButton.Variant.values()) {
                BsButton b = new BsButton(v.name(), skin, v, st, BsButton.Size.SM);
                b.addListener(logClick("按钮", st + "/" + v));
                c.add(b).padRight(4);
            }
            c.row();
        }
        c.add(new Label("尺寸 Size.SM / MD / LG:", skin)).padTop(10).row();
        c.add(new BsButton("SM 小按钮", skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.SM)).padRight(6);
        c.add(new BsButton("MD 中按钮（默认）", skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD)).padRight(6);
        c.add(new BsButton("LG 大按钮", skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.LG)).row();

        c.add(new Label("禁用按钮:", skin)).padTop(10).row();
        BsButton disabled = new BsButton("Disabled（不可点击）", skin, BsButton.Variant.SECONDARY, BsButton.Style.SOLID, BsButton.Size.MD);
        disabled.setDisabled(true);
        c.add(disabled).row();
    }

    // ============================ Image Buttons ============================
    private void fillImageButtons(Table c) {
        c.add(sectionTitle("ImageButton  —— 图标按钮")).row();
        c.add(new Label("纯图标 Button（用 skin drawable 当背景）；图片资源预留 —— "
                + "若 assets 里没有图标，会用 Bs 样式色块兜底。", skin)).width(700).row();

        Table row = new Table();
        row.defaults().pad(4);
        for (BsButton.Variant v : BsButton.Variant.values()) {
            // 复用 BsButton 当图标按钮（短文本 / 单字符当 icon）；真图标后续替换 Image/ImageButton
            BsButton b = new BsButton("★", skin, v, BsButton.Style.SOLID, BsButton.Size.MD);
            b.addListener(logClick("图标按钮", v.name()));
            row.add(b);
        }
        c.add(row).row();

        c.add(new Label("Image（静态图片，预留）：", skin)).padTop(10).row();
        // 尝试从 assets/textures 取一张 png 作为示例图；找不到就显示占位色块
        Drawable placeholder = skin.getDrawable("bs-primary-up");
        Image img = new Image(placeholder);
        img.setSize(96, 96);
        Table imgRow = new Table();
        imgRow.defaults().pad(4);
        imgRow.add(img).size(96);
        imgRow.add(new Label("← 示例 Image（暂用 Bs 色块占位；未来可外挂 Texture）", skin)).padLeft(8);
        c.add(imgRow).row();
    }

    // ============================ Inputs ============================
    private void fillInputs(Table c) {
        c.add(sectionTitle("Inputs  —— 文本输入框 / 多行 TextArea")).row();

        c.add(new Label("单行 TextField:", skin)).padRight(6);
        BsTextField tf = new BsTextField("", skin);
        tf.setMessageText("请输入用户名…");
        tf.setTextFieldListener((f, ch) -> setStatus("输入: " + f.getText()));
        c.add(tf).width(300).row();

        c.add(new Label("密码框 (password mode):", skin)).padRight(6);
        BsTextField pw = new BsTextField("", skin);
        pw.setMessageText("password");
        pw.setPasswordMode(true);
        pw.setPasswordCharacter('*');
        c.add(pw).width(300).row();

        c.add(new Label("只读 (disabled):", skin)).padRight(6);
        BsTextField ro = new BsTextField("不可编辑的文本", skin);
        ro.setDisabled(true);
        c.add(ro).width(300).row();

        c.add(new Label("多行 TextArea:", skin)).padTop(10).row();
        BsTextArea ta = new BsTextArea("第一行默认文本\n第二行……", skin);
        ta.setMessageText("多行输入");
        c.add(ta).width(600).height(120).row();
    }

    // ============================ Selects ============================
    private void fillSelects(Table c) {
        c.add(sectionTitle("Selects  —— 下拉选择（SelectBox / List）")).row();

        c.add(new Label("SelectBox（点击展开）:", skin)).padRight(6);
        BsSelectBox<String> sb = new BsSelectBox<>(skin);
        sb.setItems(items(java.util.Arrays.asList("选项 1", "选项 2", "选项 3", "长一点的选项 text")));
        sb.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                setStatus("SelectBox 选中: " + sb.getSelected());
            }
        });
        c.add(sb).width(220).row();

        c.add(new Label("List（用 ScrollPane 包裹，所有项可滚动选中）:", skin)).padTop(10).row();
        BsList<String> list = new BsList<>(skin);
        list.setItems(items(java.util.Arrays.asList(
                "苹果", "香蕉", "橙子", "葡萄", "西瓜", "芒果", "荔枝", "榴莲")));
        list.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus("List 选中: " + list.getSelected());
            }
        });
        // 用 ScrollPane 包裹：内容超出可见区时自动滚动，后面的"荔枝/榴莲"也能滚到点中
        BsScrollPane listScroll = new BsScrollPane(list, skin);
        listScroll.setFadeScrollBars(false);
        listScroll.setScrollingDisabled(true, false); // 仅垂直滚动
        c.add(listScroll).width(220).height(140).row();
    }

    // ============================ Radio & Check ============================
    private void fillRadioCheck(Table c) {
        c.add(sectionTitle("Radio & Check  —— 单选 / 多选")).row();

        c.add(new Label("CheckBox（多选，各自独立）:", skin)).padTop(6).row();
        Table cbRow = new Table();
        cbRow.defaults().pad(4).left();
        BsCheckBox cb1 = new BsCheckBox("启用音效", skin);
        BsCheckBox cb2 = new BsCheckBox("启用背景音乐", skin);
        BsCheckBox cb3 = new BsCheckBox("全屏显示", skin);
        cb2.setChecked(true);
        for (BsCheckBox cb : java.util.Arrays.asList(cb1, cb2, cb3)) {
            cb.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    setStatus("CheckBox [" + cb.getText() + "] = " + cb.isChecked());
                }
            });
            cbRow.add(cb).padRight(20);
        }
        c.add(cbRow).row();

        c.add(new Label("RadioButton（单选，同组互斥）:", skin)).padTop(12).row();
        Table rbRow = new Table();
        rbRow.defaults().pad(4).left();
        // 显式 group 管理互斥（避免旧版 static GROUP 跨屏残留）
        BsRadioButtonGroup rbGroup = new BsRadioButtonGroup();
        BsRadioButton r1 = rbGroup.add(new BsRadioButton("简单难度", skin));
        BsRadioButton r2 = rbGroup.add(new BsRadioButton("普通难度", skin));
        BsRadioButton r3 = rbGroup.add(new BsRadioButton("困难难度", skin));
        r2.setChecked(true);
        for (BsRadioButton rb : java.util.Arrays.asList(r1, r2, r3)) {
            rb.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    if (rb.isChecked()) setStatus("RadioButton 选中: " + rb.getText());
                }
            });
            rbRow.add(rb).padRight(20);
        }
        c.add(rbRow).row();
    }

    // ============================ Sliders ============================
    private void fillSliders(Table c) {
        c.add(sectionTitle("Slider  —— 滑块")).row();

        c.add(new Label("水平 Slider 0~100:", skin)).padRight(6);
        BsSlider hSlider = new BsSlider(0, 100, 1, false, skin);
        hSlider.setValue(40);
        hSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                setStatus("水平 Slider = " + hSlider.getValue());
            }
        });
        c.add(hSlider).width(300).row();

        c.add(new Label("垂直 Slider 0~10:", skin)).padTop(10).row();
        BsSlider vSlider = new BsSlider(0, 10, 0.5f, true, skin);
        vSlider.setValue(5);
        vSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                setStatus("垂直 Slider = " + vSlider.getValue());
            }
        });
        c.add(vSlider).size(40, 200).row();
    }

    // ============================ Misc ============================
    private void fillMisc(Table c) {
        c.add(sectionTitle("Misc  —— Tooltip / 禁用按钮 / 提示信息")).row();

        // Tooltip（用 BsButton 才能继承 bs-btn-info 的白字；scene2d TextButton 会用默认黑字看起来黑）
        c.add(new Label("鼠标悬浮在下方按钮上 →", skin)).padRight(4);
        BsButton tipBtn = new BsButton("Hover me!", skin, BsButton.Variant.INFO, BsButton.Style.SOLID, BsButton.Size.MD);
        c.add(tipBtn).row();
        BsTooltip tooltip = new BsTooltip(tipBtn, "这是一个 Tooltip 提示气泡", skin);
        tooltip.attach(stage);

        // 模态窗口示例（点击按钮弹出）
        c.add(new Label("模态窗口:", skin)).padTop(10).row();
        BsButton openModal = new BsButton("打开模态窗口", skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        openModal.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                showDemoModal();
            }
        });
        c.add(openModal).row();

        // 提示信息行
        c.add(new Label("状态标签示例:", skin)).padTop(10).row();
        Table tags = new Table();
        tags.defaults().pad(4);
        tags.add(new BsStatusLabel("Primary", skin, BsStatusLabel.Variant.PRIMARY));
        tags.add(new BsStatusLabel("Success", skin, BsStatusLabel.Variant.SUCCESS));
        tags.add(new BsStatusLabel("Warning", skin, BsStatusLabel.Variant.WARNING));
        tags.add(new BsStatusLabel("Danger", skin, BsStatusLabel.Variant.DANGER));
        tags.add(new BsStatusLabel("Info", skin, BsStatusLabel.Variant.INFO));
        c.add(tags).row();

        // Alert 弹窗（4 种级别，带动画）
        c.add(new Label("Alert 弹窗（4 种级别 + 入场动画）:", skin)).padTop(12).row();
        Table alertRow = new Table();
        alertRow.defaults().pad(4);
        alertRow.add(alertBtn("通知 Notice", com.git.bs.ui.BsAlertDialog.Level.NOTICE,
                "这是 NOTICE 级别的提示，淡入动画。"));
        alertRow.add(alertBtn("警告 Warning", com.git.bs.ui.BsAlertDialog.Level.WARNING,
                "这是 WARNING 级别的提示，从顶部滑入。"));
        alertRow.add(alertBtn("错误 Error", com.git.bs.ui.BsAlertDialog.Level.ERROR,
                "这是 ERROR 级别的提示，缩放进入。"));
        alertRow.add(alertBtn("成功 Success", com.git.bs.ui.BsAlertDialog.Level.SUCCESS,
                "这是 SUCCESS 级别的提示，淡入动画。"));
        c.add(alertRow).row();
    }

    /** 构造一个触发 Alert 弹窗的按钮。 */
    private BsButton alertBtn(String label, com.git.bs.ui.BsAlertDialog.Level level, String msg) {
        BsButton.Variant v = com.git.bs.ui.BsAlertDialog.levelButtonVariant(level);
        BsButton b = new BsButton(label, skin, v, BsButton.Style.SOLID, BsButton.Size.SM);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsAlertDialog.show(stage, skin, level, null, msg);
                setStatus("Alert: " + level);
            }
        });
        return b;
    }

    private void showDemoModal() {
        com.git.bs.ui.BsWindow win =
                new com.git.bs.ui.BsWindow("模态窗口示例", skin, true);
        win.setMovable(true);
        Table content = new Table(skin);
        content.defaults().pad(8);
        content.add(new Label("这是一个模态窗口。", skin)).row();
        content.add(new Label("点击「关闭」按钮 / 点窗口外区域 都可关闭。", skin)).padTop(6).row();
        BsButton closeBtn = new BsButton("关闭", skin, BsButton.Variant.SECONDARY, BsButton.Style.SOLID, BsButton.Size.SM);
        closeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                win.close();   // BsWindow.close 自动清理 backdrop
            }
        });
        content.add(closeBtn).padTop(10);
        win.add(content);
        win.showModal(stage);   // showModal 已挂 backdrop 点击关闭
        setStatus("打开模态窗口");
    }

    // ============================ helpers ============================

    private Label sectionTitle(String text) {
        Label l = new Label(text, skin);
        l.setColor(new Color(0.1f, 0.1f, 0.15f, 1f));
        // FreeType 字体可任意缩放且不糊（Linear filter）；用 setFontScale 模拟大字号标题
        l.setFontScale(1.4f);
        return l;
    }

    /** 生成 click 监听：点击时把信息写到状态行 + 日志。 */
    private ClickListener logClick(String category, String detail) {
        return new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                String msg = category + ": " + detail;
                setStatus(msg);
                log.info("[click] {}", msg);
            }
        };
    }

    // ============================ Screen 生命周期 ============================

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // 用当前主题 body 底色清屏（与 BsSkinApp.render 的清屏一致）：
        // 写死浅灰会让 Dark 主题下根 actor 之外的边缘区域仍显示浅色
        ScreenUtils.clear(com.git.bs.ui.BsTheme.bgBodyColor().r,
                com.git.bs.ui.BsTheme.bgBodyColor().g,
                com.git.bs.ui.BsTheme.bgBodyColor().b, 1f);
        // 同步 Shift/Ctrl 修饰键给图表（折线点击隔离用）
        BsLineChart.setModifiers(
                Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_LEFT)
                        || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_RIGHT),
                Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.CONTROL_LEFT)
                        || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.CONTROL_RIGHT));
        stage.act(delta);
        stage.draw();
    }

    // ============================ MenuBar ============================
    private void fillMenuBar(Table c) {
        c.add(sectionTitle("MenuBar  —— 菜单栏（点击按钮弹出下拉项）")).row();

        c.add(new Label("点击下方菜单按钮，弹出下拉项；点 item 触发回调，点外部或 Esc 关闭。",
                skin)).padBottom(8).row();

        BsMenuBar bar = new BsMenuBar(skin);
        // File 菜单：演示带 separator
        BsMenuBar.BsMenu file = bar.addMenu("File");
        file.addItem("New", () -> setStatus("File → New"));
        file.addItem("Open...", () -> setStatus("File → Open"));
        file.addSeparator();
        file.addItem("Exit", () -> setStatus("File → Exit"));

        // Edit 菜单
        BsMenuBar.BsMenu edit = bar.addMenu("Edit");
        edit.addItem("Undo", () -> setStatus("Edit → Undo"));
        edit.addItem("Redo", () -> setStatus("Edit → Redo"));
        edit.addSeparator();
        edit.addItem("Cut", () -> setStatus("Edit → Cut"));
        edit.addItem("Copy", () -> setStatus("Edit → Copy"));
        edit.addItem("Paste", () -> setStatus("Edit → Paste"));

        // Help 菜单：单项也行
        BsMenuBar.BsMenu help = bar.addMenu("Help");
        help.addItem("About...", () -> BsAboutDialog.show(stage, skin, "Bs UI Skin 演示", false));
        help.addItem("Docs...", () -> setStatus("Help → Docs"));

        c.add(bar).growX().row();

        c.add(new Label("(状态栏会显示你点过的菜单项)", skin)).padTop(10).row();
    }

    // ============================ Pickers ============================
    private void fillPickers(Table c) {
        c.add(sectionTitle("Pickers  —— 日期选择 / 颜色选择")).row();

        // 日期选择器
        c.add(new Label("日期选择器（点击文本框弹出日历）:", skin)).padTop(6).row();
        BsDatePicker datePicker = new BsDatePicker(skin);
        datePicker.setValue(java.time.LocalDate.now());
        datePicker.setOnChange(d -> setStatus("日期 = " + d));
        c.add(datePicker).width(220).left().row();

        c.add(new Label("再来一个日期选择器（不同初始值）:", skin)).padTop(10).row();
        BsDatePicker datePicker2 = new BsDatePicker(skin);
        datePicker2.setValue(java.time.LocalDate.of(2026, 1, 1));
        datePicker2.setOnChange(d -> setStatus("日期2 = " + d));
        c.add(datePicker2).width(220).left().row();

        // 颜色选择器
        c.add(new Label("颜色选择器（点击色块弹出调色板）:", skin)).padTop(14).row();
        Table colorRow = new Table();
        colorRow.defaults().pad(4);
        BsColorPicker colorPicker = new BsColorPicker(skin);
        colorPicker.setSelectedColor(Color.valueOf("#0D6EFD"));
        colorPicker.setOnChange(col -> setStatus(String.format("色: R=%d G=%d B=%d",
                (int) (col.r * 255), (int) (col.g * 255), (int) (col.b * 255))));
        colorRow.add(colorPicker).size(60, 28);

        BsColorPicker colorPicker2 = new BsColorPicker(skin);
        colorPicker2.setSelectedColor(Color.valueOf("#DC3545"));
        colorPicker2.setOnChange(col -> setStatus(String.format("色2: R=%d G=%d B=%d",
                (int) (col.r * 255), (int) (col.g * 255), (int) (col.b * 255))));
        colorRow.add(colorPicker2).size(60, 28);

        c.add(colorRow).left().row();
        c.add(new Label("(选色后状态栏会显示 RGB 值)", skin)).padTop(4).row();
    }

    // ============================ Form ============================
    private void fillForm(Table c) {
        c.add(sectionTitle("Form  —— 通用表单（带校验）")).row();

        c.add(new Label("填写表单（输入时实时校验，校验通过才能提交）:", skin)).padBottom(8).row();

        BsForm form = new BsForm(skin, 90, 220, 180);

        // 用户名：必填
        form.addField("用户名", new BsTextField("", skin),
                v -> (v == null || v.isEmpty()) ? "用户名必填" : null);

        // 邮箱：必填 + 必须含 @
        form.addField("邮箱", new BsTextField("", skin),
                v -> {
                    if (v == null || v.isEmpty()) return "邮箱必填";
                    if (!v.contains("@")) return "邮箱必须包含 @";
                    return null;
                });

        // 年龄：0~150 数字
        form.addField("年龄", new BsTextField("", skin),
                v -> {
                    if (v == null || v.isEmpty()) return "年龄必填";
                    try {
                        int age = Integer.parseInt(v);
                        if (age < 0 || age > 150) return "年龄范围 0~150";
                    } catch (NumberFormatException e) {
                        return "必须是整数";
                    }
                    return null;
                });

        // 备注：可选，无校验
        form.addField("备注", new BsTextField("", skin));

        // 生日：日期选择器（注意：Form 的 validateField 用 getText() 取 TextField 文本，
        // BsDatePicker 也是 TextField 子类， getText 取的是 ISO 日期串）
        BsDatePicker birth = new BsDatePicker(skin);
        birth.setValue(java.time.LocalDate.of(2000, 1, 1));
        form.addField("生日", birth);

        // 提交 / 取消
        form.addSubmitBar(
                "保存", () -> setStatus("提交: " + String.join(" / ", form.collectValues())),
                "取消", () -> setStatus("已取消")
        );

        c.add(form).growX().left().row();
        c.add(new Label("(输入用户名/邮箱/年龄后会即时显示校验错误)", skin)).padTop(8).row();
    }

    // ============================ DateTime ============================
    private void fillDateTime(Table c) {
        c.add(sectionTitle("DateTime  —— 日期时间选择器（精确到秒）")).row();

        c.add(new Label("含时间模式（点击弹出日历+时分秒）:", skin)).padTop(6).row();
        BsDatePicker dt1 = new BsDatePicker(skin, true);
        dt1.setValue(java.time.LocalDateTime.now());
        dt1.setOnChange(dt -> setStatus("选了: " + dt));
        c.add(dt1).width(240).left().row();

        c.add(new Label("另一个含时间选择器:", skin)).padTop(10).row();
        BsDatePicker dt2 = new BsDatePicker(skin, true);
        dt2.setValue(java.time.LocalDateTime.of(2026, 1, 1, 9, 30, 0));
        dt2.setOnChange(dt -> setStatus("dt2 = " + dt));
        c.add(dt2).width(240).left().row();

        c.add(new Label("纯日期模式（对比）:", skin)).padTop(10).row();
        BsDatePicker dt3 = new BsDatePicker(skin);
        dt3.setValue(java.time.LocalDate.now());
        dt3.setOnChange(dt -> setStatus("纯日期: " + dt.toLocalDate()));
        c.add(dt3).width(240).left().row();

        c.add(new Label("(含时间模式选完点'确定'才提交，避免时间被吞)", skin)).padTop(8).row();
    }

    // ============================ Tree ============================
    private void fillTree(Table c) {
        c.add(sectionTitle("Tree  —— 树状列表（节点展开/折叠）")).row();

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

        tree.setOnNodeClick(n -> setStatus("点击节点: " + n.getText()
                + (n.getChildren().isEmpty() ? " (叶子)" : " (有 " + n.getChildren().size() + " 子节点)")));

        BsScrollPane treeScroll = new BsScrollPane(tree, skin);
        treeScroll.setFadeScrollBars(false);
        c.add(treeScroll).growX().height(280).row();
    }

    // ============================ Table + Pagination ============================
    private void fillTable(Table c) {
        c.add(sectionTitle("Table + Pagination  —— 表格 + 分页")).row();

        c.add(new Label("点击表头列触发排序回调，点击行触发行回调:", skin)).padBottom(8).row();

        // 准备 25 行假数据（分 3 页）
        List<String> headers = java.util.Arrays.asList("ID", "姓名", "年龄", "状态");
        List<List<String>> allRows = new java.util.ArrayList<>();
        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十",
                "郑十一", "王十二", "冯十三", "陈十四", "褚十五", "卫十六", "蒋十七",
                "沈十八", "韩十九", "杨二十", "朱二一", "秦二二", "尤二三", "许二四",
                "何二五", "吕二六", "施二七"};
        String[] statuses = {"active", "inactive", "pending"};
        for (int i = 0; i < 25; i++) {
            allRows.add(java.util.Arrays.asList(
                    String.valueOf(i + 1),
                    names[i % names.length],
                    String.valueOf(20 + (i * 3) % 40),
                    statuses[i % statuses.length]
            ));
        }

        BsTable table = new BsTable(skin);
        table.setHeaders(headers);
        table.setColWidth(100);
        table.setOnRowClick(row -> setStatus("点击行 #" + (row + 1) + ": " + table.getRow(row)));
        table.setOnHeaderClick(col -> setStatus("点击表头列 #" + col + "（业务方决定升降序）"));

        BsPagination pagination = new BsPagination(skin);
        int pageSize = 10;
        pagination.setTotalPages((int) Math.ceil(allRows.size() * 1.0 / pageSize));
        pagination.setCurrentPage(1);
        pagination.setOnChange(page -> {
            int from = (page - 1) * pageSize;
            int to = Math.min(from + pageSize, allRows.size());
            table.setData(allRows.subList(from, to));
            setStatus("切到第 " + page + " 页");
        });

        // 初始第 1 页数据
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

    // ============================ Overlay ============================
    private void fillOverlay(Table c) {
        c.add(sectionTitle("Overlay  —— Tooltip / Spinner / Popover / Link")).row();

        // ===== Tooltip（4 方向） =====
        c.add(new Label("Tooltip（鼠标悬停查看 4 方向）:", skin)).padTop(8).left().row();
        Table tipRow = new Table();
        tipRow.defaults().pad(8);
        for (final BsTooltip.Placement p : BsTooltip.Placement.values()) {
            BsButton btn = new BsButton(p.name(), skin,
                    BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
            BsTooltip tip = new BsTooltip(btn, "出现在 " + p.name(), skin, p);
            tip.setShowDelay(0.3f);
            tip.attach(stage);
            tipRow.add(btn);
        }
        c.add(tipRow).left().row();

        // ===== Spinner =====
        c.add(new Label("Spinner 旋转加载器（BORDER + GROW）:", skin)).padTop(12).left().row();
        Table spinRow = new Table();
        spinRow.defaults().pad(10);
        BsSpinner borderSpin = new BsSpinner(skin, BsSpinner.Style.BORDER);
        borderSpin.setSize(28, 28);
        spinRow.add(borderSpin).size(28, 28);
        BsSpinner borderLg = new BsSpinner(skin, BsSpinner.Style.BORDER,
                Color.valueOf("#DC3545"));
        borderLg.setSize(44, 44);
        spinRow.add(borderLg).size(44, 44);
        BsSpinner growSpin = new BsSpinner(skin, BsSpinner.Style.GROW,
                Color.valueOf("#198754"));
        growSpin.setSize(32, 32);
        spinRow.add(growSpin).size(32, 32);
        BsSpinner growOrange = new BsSpinner(skin, BsSpinner.Style.GROW,
                Color.valueOf("#FD7E14"));
        growOrange.setSize(40, 40);
        spinRow.add(growOrange).size(40, 40);
        c.add(spinRow).left().row();

        c.add(new Label("(上方两个 BORDER 旋转圆弧，下方两个 GROW 脉冲缩放)", skin)).padTop(2).row();

        // ===== Popover =====
        c.add(new Label("Popover 弹出层（点击按钮触发）:", skin)).padTop(12).left().row();
        Table popRow = new Table();
        popRow.defaults().pad(8);

        BsButton popBtn1 = new BsButton("点击打开 Popover (右)", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        BsPopover pop1 = new BsPopover(popBtn1, "提示", skin)
                .placement(BsPopover.Placement.RIGHT)
                .content("这是一个 Popover 弹出层，比 Tooltip 更大，可包含富内容。");
        pop1.attach(stage);
        popRow.add(popBtn1);

        BsButton popBtn2 = new BsButton("带确认的 Popover (下)", skin,
                BsButton.Variant.DANGER, BsButton.Style.SOLID, BsButton.Size.MD);
        BsPopover pop2 = new BsPopover(popBtn2, "确认删除", skin)
                .placement(BsPopover.Placement.BOTTOM)
                .content("确定要删除这条记录吗？此操作不可撤销。")
                .onConfirm(() -> setStatus("Popover: 已确认删除"));
        pop2.attach(stage);
        popRow.add(popBtn2);

        c.add(popRow).left().row();

        // ===== Link =====
        c.add(new Label("Link 链接按钮（hover 字色加深）:", skin)).padTop(12).left().row();
        Table linkRow = new Table();
        linkRow.defaults().pad(8);
        BsLink link1 = new BsLink("忘记密码？", skin);
        link1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus("点击: 忘记密码");
            }
        });
        BsLink link2 = new BsLink("注册新账号", skin);
        link2.setColor(Color.valueOf("#198754")); // success 绿
        link2.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus("点击: 注册新账号");
            }
        });
        BsLink link3 = new BsLink("联系我们", skin);
        link3.setColor(Color.valueOf("#DC3545")); // danger 红
        link3.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus("点击: 联系我们");
            }
        });
        linkRow.add(link1);
        linkRow.add(link2);
        linkRow.add(link3);
        c.add(linkRow).left().row();
    }

    // ============================ Modal ============================
    private void fillModal(Table c) {
        c.add(sectionTitle("Modal  —— 通用三行模态框")).row();

        c.add(new Label("点击下方按钮，弹出不同样式的模态框:", skin)).padTop(6).left().row();

        Table btnRow = new Table();
        btnRow.defaults().pad(6);

        // 1. 基础模态框（标题+文字内容+取消/确认，带分隔线）
        BsButton basic = new BsButton("基础模态框", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        basic.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Label content = new Label("这是一个基础模态框。三行结构：标题 / 内容 / 按钮。\n点击取消或确认关闭。", skin);
                content.setWrap(true);
                new BsModal("提示", skin)
                        .content(content)
                        .contentWidth(380)
                        .separator(true)
                        .addButton("取消", () -> setStatus("基础模态: 取消"), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE)
                        .addButton("确认", () -> setStatus("基础模态: 确认"), BsButton.Variant.PRIMARY, BsButton.Style.SOLID)
                        .showModal(stage);
            }
        });
        btnRow.add(basic);

        // 2. 带标题图标的模态框
        BsButton withIcon = new BsButton("带标题图标", skin,
                BsButton.Variant.INFO, BsButton.Style.SOLID, BsButton.Size.MD);
        withIcon.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Label content = new Label("标题前面有一个图标（蓝色色块占位）。可换成任意 drawable。", skin);
                content.setWrap(true);
                new BsModal("操作确认", skin)
                        .setTitleIcon(skin.newDrawable("white", Color.valueOf("#0D6EFD")))
                        .content(content)
                        .contentWidth(360)
                        .separator(true)
                        .addButton("关闭", () -> {}, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE)
                        .showModal(stage);
            }
        });
        btnRow.add(withIcon);

        // 3. 带标题 banner 背景图
        BsButton withBanner = new BsButton("标题 Banner 背景", skin,
                BsButton.Variant.WARNING, BsButton.Style.SOLID, BsButton.Size.MD);
        withBanner.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // 用素材图作 banner
                Drawable banner;
                try {
                    banner = BsModal.drawableFromPath("bs/test/img/20251110013443.png");
                } catch (Throwable t) {
                    banner = skin.newDrawable("white", Color.valueOf("#FD7E14"));
                }
                Label content = new Label("标题行的背景图来自 assets/bs/test/img。\n可以用作产品宣传、品牌 banner 等。", skin);
                content.setWrap(true);
                new BsModal("图片 Banner 演示", skin)
                        .setTitleBanner(banner)
                        .content(content)
                        .contentWidth(380)
                        .separator(true)
                        .addButton("知道了", () -> {}, BsButton.Variant.PRIMARY)
                        .showModal(stage);
            }
        });
        btnRow.add(withBanner);

        // 4. 内容是表单（富内容）
        BsButton formModal = new BsButton("内容是表单", skin,
                BsButton.Variant.SUCCESS, BsButton.Style.SOLID, BsButton.Size.MD);
        formModal.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsForm form = new BsForm(skin, 70, 200, 160);
                form.addField("用户名", new BsTextField("", skin),
                        v -> (v == null || v.isEmpty()) ? "必填" : null);
                form.addField("邮箱", new BsTextField("", skin),
                        v -> (v != null && v.contains("@")) ? null : "邮箱格式错误");
                new BsModal("新建用户", skin)
                        .content(form)
                        .contentWidth(450)
                        .separator(true)
                        .addButton("取消", () -> {}, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE)
                        .addButton("保存", () -> {
                            if (form.validateAll()) {
                                setStatus("表单提交: " + String.join(" / ", form.collectValues()));
                            }
                        }, BsButton.Variant.SUCCESS)
                        .showModal(stage);
            }
        });
        btnRow.add(formModal);

        c.add(btnRow).left().row();
        c.add(new Label("(标题/内容/按钮 三行结构；可选分隔线和标题 banner)", skin)).padTop(8).row();
    }

    // ============================ Dialogs ============================
    private void fillDialogs(Table c) {
        c.add(sectionTitle("Dialogs  —— 对话框库（带动画）")).row();

        c.add(new Label("基于 BsModal 实现的 4 种对话框，每种带不同入场/出场动画:", skin))
                .padTop(6).left().row();

        Table row1 = new Table();
        row1.defaults().pad(6);

        // ===== Alert 4 种级别 =====
        row1.add(new Label("Alert 弹窗:", skin)).right().padRight(8);
        BsButton bNotice = new BsButton("通知 (淡入)", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.SM);
        bNotice.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsAlertDialog.show(stage, skin,
                        com.git.bs.ui.BsAlertDialog.Level.NOTICE,
                        "新消息", "您收到 1 条新消息，请注意查收。");
            }
        });
        BsButton bWarn = new BsButton("警告 (顶部滑入)", skin,
                BsButton.Variant.WARNING, BsButton.Style.SOLID, BsButton.Size.SM);
        bWarn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsAlertDialog.show(stage, skin,
                        com.git.bs.ui.BsAlertDialog.Level.WARNING,
                        "操作不可逆", "此操作将永久删除数据，是否继续？");
            }
        });
        BsButton bError = new BsButton("错误 (缩放)", skin,
                BsButton.Variant.DANGER, BsButton.Style.SOLID, BsButton.Size.SM);
        bError.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsAlertDialog.show(stage, skin,
                        com.git.bs.ui.BsAlertDialog.Level.ERROR,
                        "提交失败", "网络异常，请稍后重试。错误码 500。");
            }
        });
        BsButton bSuccess = new BsButton("成功 (淡入)", skin,
                BsButton.Variant.SUCCESS, BsButton.Style.SOLID, BsButton.Size.SM);
        bSuccess.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsAlertDialog.show(stage, skin,
                        com.git.bs.ui.BsAlertDialog.Level.SUCCESS,
                        "保存成功", "您的修改已成功保存到服务器。");
            }
        });

        // 自动关闭通知（2 秒后淡出）
        BsButton bAutoClose = new BsButton("通知 (2s 自动关闭)", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bAutoClose.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsAlertDialog d = new com.git.bs.ui.BsAlertDialog(
                        "自动关闭", "这条消息 2 秒后自动消失，无需手动关闭。",
                        com.git.bs.ui.BsAlertDialog.Level.NOTICE, skin);
                d.setAutoCloseAfter(2.0f);
                d.showModal(stage);
            }
        });
        row1.add(bNotice);
        row1.add(bAutoClose).row();

        Table row2 = new Table();
        row2.defaults().pad(6);
        row2.add(bWarn);
        row2.add(bError);
        row2.add(bSuccess);

        c.add(row1).left().row();
        c.add(row2).left().padTop(4).row();

        // ===== Confirm / Prompt / Choice =====
        c.add(new Label("交互对话框:", skin)).padTop(14).left().row();
        Table row3 = new Table();
        row3.defaults().pad(6);

        BsButton bConfirm = new BsButton("确认对话框 Confirm", skin,
                BsButton.Variant.INFO, BsButton.Style.OUTLINE, BsButton.Size.MD);
        bConfirm.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsConfirmDialog.show(stage, skin,
                        "确认删除？", "此操作不可撤销，确定要删除这条记录吗？",
                        ok -> setStatus(ok ? "用户点了【是】" : "用户点了【否】"));
            }
        });
        row3.add(bConfirm);

        BsButton bPrompt = new BsButton("文本输入 Prompt", skin,
                BsButton.Variant.SUCCESS, BsButton.Style.OUTLINE, BsButton.Size.MD);
        bPrompt.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsPromptDialog.show(stage, skin,
                        "新建项目", "请输入项目名称：", "my-project",
                        text -> setStatus(text == null ? "用户取消输入" : "输入: " + text));
            }
        });
        row3.add(bPrompt);

        BsButton bChoice = new BsButton("多选一 Choice", skin,
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.MD);
        bChoice.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsChoiceDialog.show(stage, skin,
                        "选择难度", "请选择游戏难度：",
                        java.util.Arrays.asList("简单 / Easy", "普通 / Normal", "困难 / Hard", "地狱 / Inferno"),
                        idx -> setStatus(idx < 0 ? "用户取消" : "选了第 " + idx + " 项"));
            }
        });
        row3.add(bChoice);

        c.add(row3).left().row();
        c.add(new Label("(NOTICE 淡入 / WARNING 上滑入 / ERROR 缩放 / SUCCESS 淡入；交互框带淡出)", skin))
                .padTop(8).row();
    }

    // ============================ Cards ============================
    private void fillCards(Table c) {
        c.add(sectionTitle("Cards  —— 卡片（图片 + 文字）")).row();

        c.add(new Label("Bootstrap 风格卡片：图片 + 标题 + 副标题 + 正文 + 页脚按钮。",
                skin)).padTop(6).left().row();

        // ===== 1. 垂直布局卡片（顶部图片 + 文字）=====
        c.add(new Label("垂直布局（顶部图片）:", skin)).padTop(14).left().row();
        try {
            Drawable img1 =
                    BsModal.drawableFromPath("bs/test/img/20251110013443.png");
            com.git.bs.ui.BsCard card1 = new com.git.bs.ui.BsCard(skin)
                    .image(img1)
                    .imageSize(0, 130)  // 高度 130，宽度自动撑满
                    .title("项目卡片")
                    .subtitle("用户上传 · 2025-11-10")
                    .body("这是一段卡片正文，介绍这个项目的内容。卡片自动撑满父容器宽度，文字会自动换行。")
                    .footerLink("查看详情", () -> setStatus("点了 查看详情"))
                    .footerButton("收藏", () -> setStatus("点了 收藏"),
                            BsButton.Variant.WARNING, BsButton.Style.SOLID);
            c.add(card1).width(360).growX().row();
        } catch (Throwable t) {
            c.add(new Label("(图片加载失败: " + t.getMessage() + ")", skin)).row();
        }

        // ===== 2. 水平布局卡片（左图右文）=====
        c.add(new Label("水平布局（左图右文）:", skin)).padTop(14).left().row();
        try {
            Drawable img2 =
                    BsModal.drawableFromPath("bs/test/img/20251109230728.png");
            com.git.bs.ui.BsCard card2 = new com.git.bs.ui.BsCard(skin)
                    .orientation(com.git.bs.ui.BsCard.Orientation.HORIZONTAL)
                    .image(img2)
                    .imageSize(120, 100)
                    .title("通知中心")
                    .body("您有 3 条未读消息。")
                    .footerLink("查看全部", () -> setStatus("查看全部消息"));
            c.add(card2).width(420).growX().row();
        } catch (Throwable t) {
            c.add(new Label("(图片加载失败: " + t.getMessage() + ")", skin)).row();
        }

        // ===== 3. 无图卡片（纯文字）=====
        c.add(new Label("无图卡片（纯文字 + 链接）:", skin)).padTop(14).left().row();
        com.git.bs.ui.BsCard card3 = new com.git.bs.ui.BsCard(skin)
                .title("关于 Bs UI 框架")
                .subtitle("v0.2 · Bootstrap 风格")
                .body("Bs UI 是一套基于 libgdx scene2d 自制的 Bootstrap 风格 UI 框架，包含按钮、表单、卡片、模态框、对话框等组件。")
                .footerLink("GitHub", () -> setStatus("点了 GitHub"))
                .footerLink("文档", () -> setStatus("点了 文档"));
        c.add(card3).width(420).growX().row();

        c.add(new Label("(卡片背景用 bs-window-bg 圆角白底；图片来自 assets/bs/test/img)", skin))
                .padTop(8).row();
    }

    // ============================ Badge ============================
    private void fillBadge(Table c) {
        c.add(sectionTitle("Badge  —— 徽标（消息/数量）")).row();

        c.add(new Label("独立 Badge（6 色 Variant）:", skin)).padTop(8).left().row();
        Table row1 = new Table();
        row1.defaults().pad(6);
        for (com.git.bs.ui.BsBadge.Variant v : com.git.bs.ui.BsBadge.Variant.values()) {
            row1.add(new com.git.bs.ui.BsBadge(v.name(), skin, v));
        }
        c.add(row1).left().row();

        c.add(new Label("带 Badge 的按钮（消息数量红点）:", skin)).padTop(14).left().row();
        Table row2 = new Table();
        row2.defaults().pad(10);

        com.git.bs.ui.BsBadgeButton msgBtn = new com.git.bs.ui.BsBadgeButton(
                "消息", skin, BsButton.Variant.PRIMARY);
        msgBtn.setBadge(5);
        msgBtn.setOnClick(() -> setStatus("点开消息"));
        msgBtn.pack();
        row2.add(msgBtn);

        com.git.bs.ui.BsBadgeButton cartBtn = new com.git.bs.ui.BsBadgeButton(
                "购物车", skin, BsButton.Variant.SUCCESS);
        cartBtn.setBadge(12);
        cartBtn.setOnClick(() -> setStatus("点开购物车"));
        cartBtn.pack();
        row2.add(cartBtn);

        com.git.bs.ui.BsBadgeButton notifBtn = new com.git.bs.ui.BsBadgeButton(
                "通知", skin, BsButton.Variant.WARNING);
        notifBtn.setBadge("99+");
        notifBtn.setOnClick(() -> setStatus("点开通知"));
        notifBtn.pack();
        row2.add(notifBtn);

        com.git.bs.ui.BsBadgeButton inboxBtn = new com.git.bs.ui.BsBadgeButton(
                "收件箱", skin, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.MD);
        inboxBtn.setBadge(0);  // 0 隐藏
        inboxBtn.setOnClick(() -> setStatus("点开收件箱"));
        inboxBtn.pack();
        row2.add(inboxBtn);

        c.add(row2).left().row();
        c.add(new Label("(收件箱 badge=0 自动隐藏；其他按钮可点击)", skin)).padTop(4).row();
    }

    // ============================ Profile ============================
    private void fillProfile(Table c) {
        c.add(sectionTitle("Profile  —— 个人信息面板（两种风格）")).row();

        Drawable avatar = null;
        try {
            avatar = BsModal.drawableFromPath("bs/test/img/20251121200555.png");
        } catch (Throwable t) {
            c.add(new Label("(头像加载失败: " + t.getMessage() + ")", skin)).row();
        }

        // ===== 1. 横向信息面板（方形头像 + 操作按钮在右）=====
        c.add(new Label("1. BsProfilePanel —— 横向信息（方形头像）:", skin)).padTop(8).left().row();
        com.git.bs.ui.BsProfilePanel panel = new com.git.bs.ui.BsProfilePanel(skin)
                .avatar(avatar)                                    // 默认方形
                .avatarSize(72, 72)
                .name("authorZhao")
                .handle("@author_zhao · libgdx 开发者")
                .role("超级管理员")
                .bio("专注于 libgdx 游戏开发，热爱自制 UI 框架。当前正在打造 Bootstrap 风格 Bs UI 库。")
                .stat("帖子", "128")
                .stat("关注", "1.2k")
                .stat("粉丝", "5.6k")
                .actionButton("关注", () -> setStatus("关注了 authorZhao"), BsButton.Variant.PRIMARY)
                .actionButton("私信", () -> setStatus("打开私信"),
                        BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        c.add(panel).width(500).growX().padTop(4).row();

        // ===== 2. 圆形头像卡片（居中布局）=====
        c.add(new Label("2. BsProfileCard —— 圆形头像 + 居中卡片:", skin)).padTop(16).left().row();
        com.git.bs.ui.BsProfileCard card = new com.git.bs.ui.BsProfileCard(skin)
                .avatar(avatar)                                   // 强制圆形
                .avatarSize(96)
                .name("authorZhao")
                .handle("@author_zhao")
                .role("管理员")
                .bio("专注于 libgdx 开发，热爱自制 UI。")
                .stat("帖子", "128")
                .stat("关注", "1.2k")
                .stat("粉丝", "5.6k");
        c.add(card).width(380).padTop(4).row();

        c.add(new Label("(BsProfileCard 用 makeRoundDrawable 强制剪裁头像为圆形)", skin))
                .padTop(8).row();
    }

    // ============================ Layout ============================
    private com.git.bs.ui.BsLayoutAdmin demoLayout;  // 引用以便切换内容
    private void fillLayout(Table c) {
        c.add(sectionTitle("Layout  —— 管理后台布局（树状 sidebar + 折叠 + 用户下拉）")).row();

        c.add(new Label("顶部 ☰ 按钮折叠/展开 sidebar；左侧支持多级树状菜单；右上角用户区点击弹下拉。",
                skin)).padTop(6).left().row();

        demoLayout = new com.git.bs.ui.BsLayoutAdmin(skin);
        demoLayout.setLogo("Bs Admin");
        demoLayout.addTopMenu("首页", () -> setStatus("顶部: 首页"));
        demoLayout.addTopMenu("文档", () -> setStatus("顶部: 文档"));
        demoLayout.addTopMenu("关于", () -> BsAboutDialog.show(stage, skin, "Bs UI Skin 演示", false));

        // 用户区下拉菜单
        demoLayout.setUserInfo("管理员", null);
        demoLayout.addUserMenuItem("个人中心", () -> setStatus("用户菜单: 个人中心"));
        demoLayout.addUserMenuItem("设置", () -> setStatus("用户菜单: 设置"));
        demoLayout.addUserMenuItem("退出登录", () -> setStatus("用户菜单: 退出登录"));

        // 树状多级 sidebar
        com.git.bs.ui.BsLayoutAdmin.SidebarItem dash =
                new com.git.bs.ui.BsLayoutAdmin.SidebarItem("仪表盘", () -> switchLayoutContent("仪表盘"));
        demoLayout.addSideMenuTree(dash);

        com.git.bs.ui.BsLayoutAdmin.SidebarItem userMgmt =
                new com.git.bs.ui.BsLayoutAdmin.SidebarItem("用户管理");
        userMgmt.expanded = true;
        userMgmt.addChild("用户列表", () -> switchLayoutContent("用户 → 列表"));
        userMgmt.addChild("角色管理", () -> switchLayoutContent("用户 → 角色"));
        userMgmt.addChild("权限设置", () -> switchLayoutContent("用户 → 权限"));
        demoLayout.addSideMenuTree(userMgmt);

        com.git.bs.ui.BsLayoutAdmin.SidebarItem order =
                new com.git.bs.ui.BsLayoutAdmin.SidebarItem("订单系统");
        order.addChild("待发货", () -> switchLayoutContent("订单 → 待发货"));
        order.addChild("已完成", () -> switchLayoutContent("订单 → 已完成"));
        order.addChild("退款", () -> switchLayoutContent("订单 → 退款"));
        demoLayout.addSideMenuTree(order);

        demoLayout.addSideMenu("设置", () -> switchLayoutContent("设置"));

        // 初始内容
        switchLayoutContent("仪表盘");

        c.add(demoLayout).growX().height(420).padTop(8).row();
        c.add(new Label("(顶部 ☰ 折叠 sidebar；点击有子项的菜单文字也会展开/折叠)", skin)).padTop(4).row();
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
        setStatus("Layout: " + name);
        demoLayout.setContent(page);
    }

    // ============================ Breadcrumb ============================
    private void fillBreadcrumb(Table c) {
        c.add(sectionTitle("Breadcrumb  —— 面包屑导航")).row();

        c.add(new Label("Bootstrap 风格面包屑，每段可点击，最后一段为当前页（深色不可点）:",
                skin)).padTop(6).left().row();

        com.git.bs.ui.BsBreadcrumb bc1 = new com.git.bs.ui.BsBreadcrumb(skin)
                .addItem("首页", () -> setStatus("面包屑: 首页"))
                .addItem("用户列表", () -> setStatus("面包屑: 用户列表"))
                .addItem("详情", () -> setStatus("面包屑: 详情"))
                .addCurrent("张三");
        c.add(bc1).growX().padTop(8).left().row();

        com.git.bs.ui.BsBreadcrumb bc2 = new com.git.bs.ui.BsBreadcrumb(skin)
                .addItem("Home", () -> setStatus("bc: Home"))
                .addItem("Products", () -> setStatus("bc: Products"))
                .addItem("Electronics", () -> setStatus("bc: Electronics"))
                .addCurrent("Smartphone X12");
        c.add(bc2).growX().padTop(8).left().row();

        c.add(new Label("(点击前置段触发回调；最后一段为当前页不可点)", skin)).padTop(8).row();
    }

    // ============================ Icons ============================
    private BsScrollPane iconGridScroll;
    private Table iconGrid;
    private BsTextField iconFilterField;

    private void fillIcons(Table c) {
        c.add(sectionTitle("Icons  —— Bootstrap Icons 图标库")).row();

        // 图标浏览（加载 atlas 后显示）
        c.add(new Label("① 图标浏览（加载 atlas 后显示，按名字前缀过滤）：",
                skin)).padTop(10).left().row();

        Table filterRow = new Table();
        filterRow.defaults().pad(4).left();
        filterRow.add(new Label("过滤:", skin)).padRight(4);
        iconFilterField = new BsTextField("", skin);
        iconFilterField.setMessageText("输入图标名前缀，如 gear、arrow、person");
        iconFilterField.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override public boolean keyTyped(InputEvent event, char character) {
                refreshIconGrid();
                return false;
            }
            @Override public boolean keyUp(InputEvent event, int keycode) {
                refreshIconGrid();
                return false;
            }
        });
        filterRow.add(iconFilterField).width(280);
        BsButton loadBtn = new BsButton("重新加载 atlas", skin,
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        loadBtn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (com.git.bs.ui.BsIcon.load()) {
                    refreshIconGrid();
                    setStatus("重新加载 atlas 成功");
                } else {
                    setStatus("atlas 未生成");
                }
                return true;
            }
        });
        filterRow.add(loadBtn);
        c.add(filterRow).growX().padTop(4).row();

        iconGrid = new Table();
        iconGrid.defaults().pad(6);
        iconGrid.top().left();
        iconGridScroll = new BsScrollPane(iconGrid, skin);
        iconGridScroll.setFadeScrollBars(false);
        c.add(iconGridScroll).growX().height(360).padTop(4).row();

        // 启动时尝试自动加载
        if (com.git.bs.ui.BsIcon.load()) {
            refreshIconGrid();
        } else {
            iconGrid.add(new Label("(尚未生成 atlas)", skin)).colspan(8).row();
        }

        // ===== Icon 应用演示（按钮/链接/IconLabel）=====
        c.add(new Label("② Icon 应用演示（按钮 / 链接 / IconLabel / 菜单）：",
                skin)).padTop(14).left().row();
        Table demoRow = new Table();
        demoRow.defaults().pad(8).left();

        // 带图标的按钮
        BsButton settingBtn = new BsButton("设置", skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        Drawable gearIcon = com.git.bs.ui.BsIcon.get("gear");
        if (gearIcon != null) settingBtn.setIcon(gearIcon);
        settingBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus("点了 [设置] 按钮");
            }
        });
        demoRow.add(settingBtn);

        // 带图标的按钮 2
        BsButton userBtn = new BsButton("用户", skin, BsButton.Variant.SUCCESS, BsButton.Style.SOLID, BsButton.Size.MD);
        Drawable personIcon = com.git.bs.ui.BsIcon.get("person");
        if (personIcon != null) userBtn.setIcon(personIcon);
        userBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus("点了 [用户] 按钮");
            }
        });
        demoRow.add(userBtn);

        // 带图标的按钮 3（删除）
        BsButton delBtn = new BsButton("删除", skin, BsButton.Variant.DANGER, BsButton.Style.OUTLINE, BsButton.Size.MD);
        Drawable trashIcon = com.git.bs.ui.BsIcon.get("trash");
        if (trashIcon != null) delBtn.setIcon(trashIcon);
        delBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus("点了 [删除] 按钮");
            }
        });
        demoRow.add(delBtn);

        c.add(demoRow).left().row();

        // 带图标的链接 + IconLabel
        Table demoRow2 = new Table();
        demoRow2.defaults().pad(8).left();

        BsLink inboxLink = new BsLink("收件箱", skin);
        Drawable envelopeIcon = com.git.bs.ui.BsIcon.get("envelope");
        if (envelopeIcon != null) inboxLink.setIcon(envelopeIcon);
        inboxLink.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus("点了 [收件箱] 链接");
            }
        });
        demoRow2.add(inboxLink);

        BsLink homeLink = new BsLink("首页", skin);
        Drawable homeIcon = com.git.bs.ui.BsIcon.get("house");
        if (homeIcon != null) homeLink.setIcon(homeIcon);
        demoRow2.add(homeLink);

        // IconLabel
        Drawable heartIcon = com.git.bs.ui.BsIcon.get("heart");
        if (heartIcon != null) {
            com.git.bs.ui.BsIconLabel il1 = new com.git.bs.ui.BsIconLabel("点赞", skin)
                    .icon(heartIcon)
                    .iconColor(Color.valueOf("#DC3545"));
            demoRow2.add(il1);
        }
        Drawable starIcon = com.git.bs.ui.BsIcon.get("star-fill");
        if (starIcon != null) {
            com.git.bs.ui.BsIconLabel il2 = new com.git.bs.ui.BsIconLabel("收藏", skin)
                    .icon(starIcon)
                    .iconColor(Color.valueOf("#FFC107"));
            demoRow2.add(il2);
        }

        c.add(demoRow2).left().padTop(4).row();

        // 带图标的菜单栏
        c.add(new Label("菜单栏 + 图标：", skin)).padTop(10).left().row();
        BsMenuBar iconBar = new BsMenuBar(skin);
        BsMenuBar.BsMenu fileMenu = iconBar.addMenu("文件",
                com.git.bs.ui.BsIcon.get("folder"));
        if (gearIcon != null) {
            fileMenu.addItem("新建", () -> setStatus("文件 → 新建"));
            fileMenu.addItem("打开", () -> setStatus("文件 → 打开"));
            fileMenu.addSeparator();
            fileMenu.addItem("退出", () -> setStatus("文件 → 退出"));
        }
        BsMenuBar.BsMenu editMenu = iconBar.addMenu("编辑",
                com.git.bs.ui.BsIcon.get("pencil"));
        if (gearIcon != null) {
            editMenu.addItem("撤销", () -> setStatus("编辑 → 撤销"));
            editMenu.addItem("重做", () -> setStatus("编辑 → 重做"));
        }
        c.add(iconBar).growX().padTop(4).row();

        c.add(new Label("(图标来自转换后的 atlas；按钮/链接用 setIcon，菜单用 addMenu(title, icon)，"
                + "Label 用 BsIconLabel)", skin)).padTop(8).row();
    }

    private void refreshIconGrid() {
        if (iconGrid == null) return;
        iconGrid.clearChildren();
        if (!com.git.bs.ui.BsIcon.isLoaded()) {
            iconGrid.add(new Label("(atlas 未加载)", skin)).colspan(8).row();
            return;
        }
        String filter = iconFilterField.getText().trim().toLowerCase();
        List<String> names = new java.util.ArrayList<>(com.git.bs.ui.BsIcon.getAllNames());
        names.sort(String::compareTo);
        // 过滤
        List<String> filtered = new java.util.ArrayList<>();
        for (String n : names) {
            if (filter.isEmpty() || n.toLowerCase().startsWith(filter) || n.toLowerCase().contains(filter)) {
                filtered.add(n);
            }
        }
        // 限制显示数量（避免一次显示太多卡顿）
        int maxShow = 300;
        boolean truncated = filtered.size() > maxShow;
        if (truncated) filtered = filtered.subList(0, maxShow);

        int cols = 8;
        int col = 0;
        // cell 深色背景：圆角，让白色图标清晰可见
        Drawable cellBg =
                skin.newDrawable("white", new Color(
                        0x2C / 255f, 0x3E / 255f, 0x50 / 255f, 1f));
        for (String name : filtered) {
            Drawable d = com.git.bs.ui.BsIcon.get(name);
            if (d == null) continue;
            // 用 white 底 drawable 染白色（图标原本是白底白图，染白色不变；
            // 但 cell 背景是深色，白色图标在深色背景上清晰可见）
            Image img = new Image(d);
            img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            Table cell = new Table();
            cell.setBackground(cellBg);
            cell.pad(4);
            cell.add(img).size(28, 28).row();
            Label lab = new Label(name, skin);
            lab.setFontScale(0.7f);
            lab.setColor(Color.LIGHT_GRAY);
            cell.add(lab);
            cell.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            cell.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    setStatus("点击图标: " + name);
                }
            });
            iconGrid.add(cell).pad(4);
            col++;
            if (col >= cols) { iconGrid.row(); col = 0; }
        }
        if (truncated) {
            iconGrid.row();
            Label more = new Label(
                    "(只显示前 " + maxShow + " 个，更精确过滤查看更多)", skin);
            more.setColor(Color.GRAY);
            iconGrid.add(more).colspan(cols).padTop(8).row();
        }
        if (filtered.isEmpty()) {
            iconGrid.add(new Label(
                    "(没有匹配 \"" + filter + "\" 的图标)", skin)).colspan(cols).padTop(20).row();
        }
    }

    // ============================ Progress & Toast ============================
    private BsProgress demoProgress;
    private float progressValue = 0f;

    private void fillProgressToast(Table c) {
        c.add(sectionTitle("Progress & Toast  —— 进度条 / 轻提示(吐司)")).row();

        // ===== Progress: 6 色 =====
        c.add(new Label("Progress 进度条(6 色 × 60%):", skin)).padTop(8).left().row();
        Table progRow = new Table();
        progRow.defaults().pad(4);
        for (BsProgress.Variant v : BsProgress.Variant.values()) {
            BsProgress p = new BsProgress(skin);
            p.setVariant(v);
            p.setProgress(0.6f);
            p.setShowLabel(true);
            progRow.add(p).width(160).height(20);
            progRow.row();
        }
        c.add(progRow).left().row();

        // ===== 可控进度条 =====
        c.add(new Label("可控进度条(点击 +10% / -10% / 重置 / 条纹 / 动画):", skin)).padTop(12).left().row();
        demoProgress = new BsProgress(skin);
        demoProgress.setVariant(BsProgress.Variant.PRIMARY);
        demoProgress.setProgress(0f);
        demoProgress.setShowLabel(true);
        c.add(demoProgress).width(480).height(22).left().row();

        Table ctrlRow = new Table();
        ctrlRow.defaults().pad(4);
        ctrlRow.add(progBtn("-10%", () -> {
            progressValue = Math.max(0, progressValue - 0.1f);
            demoProgress.setProgress(progressValue);
            setStatus("进度: " + Math.round(progressValue * 100) + "%");
        }));
        ctrlRow.add(progBtn("+10%", () -> {
            progressValue = Math.min(1, progressValue + 0.1f);
            demoProgress.setProgress(progressValue);
            setStatus("进度: " + Math.round(progressValue * 100) + "%");
        }));
        ctrlRow.add(progBtn("重置", () -> {
            progressValue = 0;
            demoProgress.setProgress(0);
            setStatus("进度重置");
        }));
        ctrlRow.add(progBtn("切换条纹", () -> demoProgress.setStriped(true)));
        ctrlRow.add(progBtn("切换动画", () -> demoProgress.setAnimated(true)));
        c.add(ctrlRow).left().row();

        // ===== Toast =====
        c.add(new Label("Toast 轻提示(右上角堆叠,自动消失):", skin)).padTop(14).left().row();
        Table toastRow = new Table();
        toastRow.defaults().pad(4);
        for (final BsToast.Variant v : BsToast.Variant.values()) {
            BsButton b = new BsButton(v.name(), skin, BsButton.Variant.PRIMARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    BsToast.show(stage, skin, v.name() + " 提示", "这是一条 " + v.name() + " 级别的轻提示",
                            v, 3f, BsToast.Placement.TOP_RIGHT);
                    setStatus("Toast: " + v);
                }
            });
            toastRow.add(b);
        }
        c.add(toastRow).left().row();

        c.add(new Label("Toast 不同位置(placement):", skin)).padTop(8).left().row();
        Table placeRow = new Table();
        placeRow.defaults().pad(4);
        placeRow.add(toastPlaceBtn("右上", BsToast.Placement.TOP_RIGHT));
        placeRow.add(toastPlaceBtn("左上", BsToast.Placement.TOP_LEFT));
        placeRow.add(toastPlaceBtn("顶部居中", BsToast.Placement.TOP_CENTER));
        placeRow.add(toastPlaceBtn("右下", BsToast.Placement.BOTTOM_RIGHT));
        placeRow.add(toastPlaceBtn("左下", BsToast.Placement.BOTTOM_LEFT));
        c.add(placeRow).left().row();

        Label toastNote = new Label("(Toast 不阻断操作，3 秒后自动消失；右上角堆叠，新提示会推到下方)", skin);
        c.add(toastNote).padTop(8).row();
    }

    private BsButton progBtn(String label, Runnable action) {
        BsButton b = new BsButton(label, skin, BsButton.Variant.SECONDARY, BsButton.Style.SOLID, BsButton.Size.SM);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                try { action.run(); } catch (Throwable t) {}
            }
        });
        return b;
    }

    private BsButton toastPlaceBtn(String label, BsToast.Placement p) {
        BsButton b = new BsButton(label, skin, BsButton.Variant.INFO, BsButton.Style.OUTLINE, BsButton.Size.SM);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsToast.show(stage, skin, label, "位置: " + p.name(),
                        BsToast.Variant.PRIMARY, 2.5f, p);
                setStatus("Toast 位置: " + p);
            }
        });
        return b;
    }

    // ============================ Collapse & Accordion ============================
    private void fillCollapseAccordion(Table c) {
        c.add(sectionTitle("Collapse & Accordion  —— 折叠 / 手风琴")).row();

        // ===== 单个 Collapse =====
        c.add(new Label("单个 Collapse(点击标题行展开/收起):", skin)).padTop(8).left().row();
        BsCollapse collapse = new BsCollapse(skin);
        collapse.setTitle("用户详情(可折叠)");
        Table collapseContent = new Table();
        collapseContent.left().pad(4);
        collapseContent.add(new Label("姓名: 张三", skin)).left().row();
        collapseContent.add(new Label("邮箱: zhangsan@example.com", skin)).left().row();
        collapseContent.add(new Label("注册时间: 2026-06-26", skin)).left().row();
        collapseContent.add(new Label("简介: 这是一段可折叠的详细内容,展开时显示,收起时隐藏。", skin))
                .width(400).left().row();
        collapse.setContent(collapseContent);
        collapse.setExpanded(true);
        collapse.setOnToggle((src, expanded) -> setStatus("Collapse: " + (expanded ? "展开" : "收起")));
        c.add(collapse).width(500).growX().row();

        // ===== Accordion(单选模式)=====
        c.add(new Label("Accordion 手风琴(单选模式,一次只展开一节):", skin)).padTop(16).left().row();
        BsAccordion acc = new BsAccordion(skin);
        acc.setSingleOpen(true);
        acc.addSection("基本信息", makeAccordionContent("基本信息内容:用户 ID / 注册时间 / 状态"));
        acc.addSection("联系方式", makeAccordionContent("邮箱 / 手机 / 地址"));
        acc.addSection("安全设置", makeAccordionContent("密码强度 / 两步验证 / 登录历史"));
        acc.addSection("高级", makeAccordionContent("API token / Webhook / 订阅偏好"));
        acc.expand(0);
        c.add(acc).width(500).growX().row();

        Label accNote = new Label("(点击节标题切换，单选模式会自动收起其他节)", skin);
        c.add(accNote).padTop(8).row();
    }

    private Table makeAccordionContent(String text) {
        Table t = new Table();
        t.left().pad(8, 4, 8, 4);
        Label l = new Label(text, skin);
        l.setWrap(true);
        t.add(l).growX().left().row();
        t.add(new Label("• 子项 1", skin)).left().row();
        t.add(new Label("• 子项 2", skin)).left().row();
        t.add(new Label("• 子项 3", skin)).left().row();
        return t;
    }

    // ============================ ButtonGroup & Alert ============================
    private void fillButtonGroupAlert(Table c) {
        c.add(sectionTitle("ButtonGroup & Alert  —— 按钮组 / 警告横条")).row();

        // ===== ButtonGroup 单选 =====
        c.add(new Label("ButtonGroup 单选(分段选择器,active 互斥):", skin)).padTop(8).left().row();
        BsButtonGroup single = new BsButtonGroup(skin, BsButtonGroup.Mode.SINGLE);
        single.addToggle("日");
        single.addToggle("周");
        single.addToggle("月");
        single.addToggle("年");
        single.select(1);
        single.setOnChange(idx -> setStatus("ButtonGroup 单选: " + idx));
        c.add(single).left().row();

        // ===== ButtonGroup 多选 =====
        c.add(new Label("ButtonGroup 多选(可同时选中多个):", skin)).padTop(10).left().row();
        BsButtonGroup multi = new BsButtonGroup(skin, BsButtonGroup.Mode.MULTI);
        multi.addToggle("粗体");
        multi.addToggle("斜体");
        multi.addToggle("下划线");
        multi.addToggle("删除线");
        multi.setOnChange(idx -> setStatus("ButtonGroup 多选当前选中: " + multi.getSelectedIndices()));
        c.add(multi).left().row();

        // ===== ButtonGroup 工具栏风格(不同 activeVariant)=====
        c.add(new Label("ButtonGroup 工具栏风格(左对齐/居中/右对齐,不同颜色):", skin)).padTop(10).left().row();
        BsButtonGroup toolbar = new BsButtonGroup(skin, BsButtonGroup.Mode.SINGLE);
        toolbar.addToggle("左对齐", BsButton.Variant.SECONDARY);
        toolbar.addToggle("居中", BsButton.Variant.SECONDARY);
        toolbar.addToggle("右对齐", BsButton.Variant.SECONDARY);
        toolbar.select(0);
        toolbar.setOnChange(idx -> setStatus("工具栏: " + idx));
        c.add(toolbar).left().row();

        // ===== Alert 6 色 =====
        c.add(new Label("Alert 警告横条(6 色,可关闭):", skin)).padTop(14).left().row();
        for (BsAlert.Variant v : BsAlert.Variant.values()) {
            BsAlert alert = new BsAlert(skin, alertMessage(v), v);
            alert.setDismissible(true);
            alert.setOnClose(() -> setStatus("关闭 Alert: " + v));
            c.add(alert).width(520).growX().padTop(4).row();
        }

        // ===== Alert 带标题 + 富内容 =====
        c.add(new Label("Alert 带标题 + 富内容:", skin)).padTop(12).left().row();
        Table alertContent = new Table();
        alertContent.left().pad(0);
        alertContent.add(new Label("• 影响范围: 3 个用户", skin)).left().row();
        alertContent.add(new Label("• 操作可逆: 否", skin)).left().row();
        alertContent.add(new Label("• 建议先备份", skin)).left().row();
        BsAlert titled = new BsAlert(skin, "操作确认", null, BsAlert.Variant.WARNING);
        titled.setContentActor(alertContent);
        titled.setDismissible(false);
        c.add(titled).width(520).growX().padTop(4).row();

        Label alertNote = new Label("(Alert 是页面内静态横条，不阻断操作；区别于对话框模态遮罩)", skin);
        c.add(alertNote).padTop(8).row();
    }

    private String alertMessage(BsAlert.Variant v) {
        switch (v) {
            case PRIMARY:   return "这是一条 primary 提示,通常用于一般性说明。";
            case SECONDARY: return "这是一条 secondary 提示,样式较中性。";
            case SUCCESS:   return "操作成功完成!数据已保存。";
            case DANGER:    return "操作失败!请检查网络连接后重试。";
            case WARNING:   return "此操作不可逆,请谨慎确认。";
            case INFO:      return "提示:你可以点击右侧 × 关闭本提示。";
        }
        return "";
    }

    // ============================ InputNumber & InputGroup ============================
    private void fillInputNumberGroup(Table c) {
        c.add(sectionTitle("InputNumber & InputGroup  —— 数字步进器 / 输入组")).row();

        // ===== BsInputNumber =====
        c.add(new Label("InputNumber 数字步进器（长按 +/- 按钮可连续增减）:", skin)).padTop(8).left().row();
        Table row1 = new Table();
        row1.defaults().pad(6);
        BsInputNumber num1 = new BsInputNumber(skin);
        num1.setRange(0, 100).setStep(1).setValue(20);
        num1.setOnChange(v -> setStatus("步进器 1: " + (int) (double) v));
        row1.add(num1);

        BsInputNumber num2 = new BsInputNumber(skin);
        num2.setRange(-50, 50).setStep(5).setValue(0);
        num2.setOnChange(v -> setStatus("步进器 2: " + (int) (double) v));
        row1.add(num2);

        BsInputNumber num3 = new BsInputNumber(skin);
        num3.setRange(0, 10).setStep(0.5).setDecimals(1).setValue(2.5);
        num3.setOnChange(v -> setStatus("步进器 3 (小数): " + v));
        row1.add(num3);

        c.add(row1).left().row();

        // ===== BsInputGroup =====
        c.add(new Label("InputGroup 输入组（前缀/后缀 文字、图标、按钮）:", skin)).padTop(14).left().row();

        // 前缀文字 @
        c.add(new Label("前缀文字（@ 用户名）:", skin)).padTop(6).left().row();
        BsInputGroup g1 = new BsInputGroup(skin)
                .prependText("@")
                .field(new BsTextField("", skin));
        c.add(g1).left().padTop(4).row();

        // 后缀文字
        c.add(new Label("后缀文字（金额单位）:", skin)).padTop(6).left().row();
        BsInputGroup g2 = new BsInputGroup(skin)
                .prependText("¥")
                .field(new BsTextField("", skin))
                .appendText(".00");
        c.add(g2).left().padTop(4).row();

        // 后缀按钮（搜索框）
        c.add(new Label("后缀按钮（搜索框）:", skin)).padTop(6).left().row();
        BsTextField searchField = new BsTextField("", skin);
        searchField.setMessageText("输入关键词...");
        BsInputGroup g3 = new BsInputGroup(skin)
                .field(searchField)
                .appendButton("搜索", () -> setStatus("搜索: " + searchField.getText()),
                        BsButton.Variant.PRIMARY);
        c.add(g3).left().padTop(4).row();

        // 前缀图标 + 后缀文字
        c.add(new Label("前缀图标 + 后缀文字（邮箱）:", skin)).padTop(6).left().row();
        Drawable envelope =
                com.git.bs.ui.BsIcon.get("envelope");
        BsTextField emailField = new BsTextField("", skin);
        emailField.setMessageText("user@example.com");
        BsInputGroup g4 = new BsInputGroup(skin);
        if (envelope != null) g4.prependIcon(envelope);
        g4.field(emailField).appendText(".com");
        c.add(g4).left().padTop(4).row();

        Label inputNote = new Label("(InputNumber 别叫 Spinner，避免与加载转圈 BsSpinner 重名)", skin);
        c.add(inputNote).padTop(8).row();
    }

    // ============================ Navbar & Offcanvas ============================
    private void fillNavbarOffcanvas(Table c) {
        c.add(sectionTitle("Navbar & Offcanvas  —— 导航栏 / 侧滑抽屉")).row();

        // ===== BsNavbar =====
        c.add(new Label("BsNavbar 顶部导航栏（logo + 菜单 + 操作区 + 搜索）:", skin)).padTop(8).left().row();
        BsNavbar navbar = new BsNavbar(skin);
        navbar.setBrand("MyApp");
        Drawable logoIcon = com.git.bs.ui.BsIcon.get("house");
        if (logoIcon != null) navbar.setLogo(logoIcon);
        navbar.addMenuItem("文件", menu -> {
            menu.addItem("新建", () -> setStatus("Navbar: 文件 → 新建"));
            menu.addItem("打开...", () -> setStatus("Navbar: 文件 → 打开"));
            menu.addSeparator();
            menu.addItem("退出", () -> setStatus("Navbar: 文件 → 退出"));
        });
        navbar.addMenuItem("编辑", menu -> {
            menu.addItem("撤销", () -> setStatus("Navbar: 编辑 → 撤销"));
            menu.addItem("重做", () -> setStatus("Navbar: 编辑 → 重做"));
        });
        navbar.addMenuItem("帮助", menu -> {
            menu.addItem("关于", () -> BsAboutDialog.show(stage, skin, "Bs UI Skin 演示", false));
        });
        navbar.addAction("设置", () -> setStatus("Navbar: 设置"), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        navbar.addSearchField("搜索内容...");
        navbar.showBottomBorder(true);
        c.add(navbar).growX().padTop(4).row();

        // ===== BsOffcanvas 4 方向 =====
        c.add(new Label("BsOffcanvas 侧滑抽屉（左/右/上/下 4 方向）:", skin)).padTop(16).left().row();
        Table drawerRow = new Table();
        drawerRow.defaults().pad(4);
        drawerRow.add(drawerBtn("从左滑入", BsOffcanvas.Placement.LEFT, 320, 0));
        drawerRow.add(drawerBtn("从右滑入", BsOffcanvas.Placement.RIGHT, 320, 0));
        drawerRow.add(drawerBtn("从上滑入", BsOffcanvas.Placement.TOP, 0, 220));
        drawerRow.add(drawerBtn("从下滑入", BsOffcanvas.Placement.BOTTOM, 0, 220));
        c.add(drawerRow).left().row();

        Label offNote = new Label("(点击按钮弹出抽屉，点遮罩或 × 关闭)", skin);
        c.add(offNote).padTop(8).row();
    }

    private BsButton drawerBtn(String label, BsOffcanvas.Placement p, float w, float h) {
        BsButton b = new BsButton(label, skin, BsButton.Variant.INFO, BsButton.Style.OUTLINE, BsButton.Size.SM);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsOffcanvas off = new BsOffcanvas(skin, p);
                off.setTitle(label);
                Table content = new Table();
                content.left().pad(8);
                content.add(new Label("这是 " + p.name() + " 方向的抽屉内容。", skin)).left().row();
                content.add(new Label("• 选项 1", skin)).left().padTop(6).row();
                content.add(new Label("• 选项 2", skin)).left().row();
                content.add(new Label("• 选项 3", skin)).left().row();
                off.setContent(content);
                if (w > 0) off.setDrawerWidth(w);
                if (h > 0) off.setDrawerHeight(h);
                off.setOnClose(() -> setStatus("抽屉关闭: " + p));
                off.show(stage);
                setStatus("抽屉打开: " + p);
            }
        });
        return b;
    }

    // ============================ Charts-Line 折线图 ============================
    private void fillChartsLine(Table c) {
        c.add(sectionTitle("Charts-Line  —— 折线图（坐标轴 + 数据点 + 多系列）")).row();

        c.add(new Label("① 单系列折线（带 X/Y 坐标轴 + 网格 + 数据点）:", skin)).padTop(8).left().row();
        BsLineChart line1 = new BsLineChart();
        line1.setSize(640, 240);
        line1.setSkinFont(skin);
        line1.setShowPoints(true);
        line1.setLegendPlacement(BsChart.LegendPlacement.NONE);
        line1.setData(BsChart.pointsOfY(3, 5, 4, 8, 7, 10, 6, 9));
        c.add(wrapChart(line1, 640, 240)).padTop(4).row();

        c.add(new Label("② 多系列折线（销量 vs 库存，图例顶部）:", skin)).padTop(14).left().row();
        BsLineChart line2 = new BsLineChart();
        line2.setSize(640, 240);
        line2.setSkinFont(skin);
        line2.setLegendPlacement(BsChart.LegendPlacement.TOP);
        List<BsChart.Series> lineSeries2 = new java.util.ArrayList<>();
        lineSeries2.add(new BsChart.Series("销量", BsChart.pointsOfY(3, 5, 4, 8, 7, 10, 6)));
        lineSeries2.add(new BsChart.Series("库存", BsChart.pointsOfY(8, 7, 9, 5, 6, 4, 7)));
        lineSeries2.add(new BsChart.Series("目标", BsChart.pointsOfY(5, 6, 6, 7, 8, 8, 9)));
        line2.setMultiSeries(lineSeries2);
        c.add(wrapChart(line2, 640, 240)).padTop(4).row();

        c.add(new Label("③ X/Y 坐标可自定义（非等距）:", skin)).padTop(14).left().row();
        BsLineChart line3 = new BsLineChart();
        line3.setSize(640, 240);
        line3.setSkinFont(skin);
        line3.setLegendPlacement(BsChart.LegendPlacement.NONE);
        // x 用 0,10,20,40,80 这种非等距
        line3.setData(BsChart.points(0, 1, 10, 4, 20, 8, 40, 15, 80, 25));
        c.add(wrapChart(line3, 640, 240)).padTop(4).row();

        c.add(new Label("④ 无网格 + 无数据点（极简）:", skin)).padTop(14).left().row();
        BsLineChart line4 = new BsLineChart();
        line4.setSize(640, 180);
        line4.setSkinFont(skin);
        line4.setShowGrid(false);
        line4.setShowPoints(false);
        line4.setLineWidth(3f);
        line4.setLegendPlacement(BsChart.LegendPlacement.NONE);
        line4.setData(BsChart.pointsOfY(20, 35, 28, 42, 38, 55, 48, 60));
        c.add(wrapChart(line4, 640, 180)).padTop(4).row();

        Label note = new Label("(鼠标 hover 数据点会显示坐标数值；详见 Charts-Hover 模块)", skin);
        c.add(note).padTop(8).row();
    }

    // ============================ Charts-Bar 柱状图 ============================
    private void fillChartsBar(Table c) {
        c.add(sectionTitle("Charts-Bar  —— 柱状图（垂直/水平 + 多系列分组）")).row();

        c.add(new Label("① 单系列柱状（季度销量）:", skin)).padTop(8).left().row();
        BsBarChart bar1 = new BsBarChart();
        bar1.setSize(640, 240);
        bar1.setSkinFont(skin);
        bar1.setCategories("Q1", "Q2", "Q3", "Q4");
        bar1.setMultiSeries(List.of(
                new BsChart.Series("销量", BsChart.pointsOfY(35, 48, 60, 72))
        ));
        bar1.setLegendPlacement(BsChart.LegendPlacement.NONE);
        c.add(wrapChart(bar1, 640, 240)).padTop(4).row();

        c.add(new Label("② 多系列分组（2024 vs 2025）:", skin)).padTop(14).left().row();
        BsBarChart bar2 = new BsBarChart();
        bar2.setSize(640, 240);
        bar2.setSkinFont(skin);
        bar2.setCategories("Q1", "Q2", "Q3", "Q4");
        bar2.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("2024", BsChart.pointsOfY(35, 48, 60, 72)),
                new BsChart.Series("2025", BsChart.pointsOfY(45, 55, 68, 88))
        ));
        bar2.setLegendPlacement(BsChart.LegendPlacement.TOP);
        c.add(wrapChart(bar2, 640, 240)).padTop(4).row();

        c.add(new Label("③ 3 系列分组（地区对比）:", skin)).padTop(14).left().row();
        BsBarChart bar3 = new BsBarChart();
        bar3.setSize(640, 260);
        bar3.setSkinFont(skin);
        bar3.setCategories("北京", "上海", "广州", "深圳", "杭州");
        bar3.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("男", BsChart.pointsOfY(120, 150, 100, 130, 90)),
                new BsChart.Series("女", BsChart.pointsOfY(110, 140, 95, 125, 85))
        ));
        bar3.setLegendPlacement(BsChart.LegendPlacement.TOP);
        c.add(wrapChart(bar3, 640, 260)).padTop(4).row();

        c.add(new Label("④ 水平柱状图（HORIZONTAL）:", skin)).padTop(14).left().row();
        BsBarChart bar4 = new BsBarChart();
        bar4.setSize(640, 240);
        bar4.setSkinFont(skin);
        bar4.setOrientation(BsBarChart.Orientation.HORIZONTAL);
        bar4.setCategories("A", "B", "C", "D", "E");
        bar4.setMultiSeries(List.of(
                new BsChart.Series("数量", BsChart.pointsOfY(20, 35, 50, 28, 42))
        ));
        bar4.setLegendPlacement(BsChart.LegendPlacement.NONE);
        c.add(wrapChart(bar4, 640, 240)).padTop(4).row();

        Label note = new Label("(鼠标 hover 柱子显示数值；图例可点击切换显示)", skin);
        c.add(note).padTop(8).row();
    }

    // ============================ Charts-Pie 饼图 ============================
    private void fillChartsPie(Table c) {
        c.add(sectionTitle("Charts-Pie  —— 饼图（扇形 + 环形 + 图例百分比）")).row();

        c.add(new Label("① 基础饼图 + 右侧图例（含百分比）:", skin)).padTop(8).left().row();
        BsPieChart pie1 = new BsPieChart();
        pie1.setSize(560, 280);
        pie1.setSkinFont(skin);
        pie1.setLegendPlacement(BsChart.LegendPlacement.RIGHT);
        pie1.setSlices(
                "Chrome", 65,
                "Firefox", 15,
                "Safari", 12,
                "Edge", 5,
                "Other", 3
        );
        c.add(wrapChart(pie1, 560, 280)).padTop(4).row();

        c.add(new Label("② 环形图（donutHole=0.55）:", skin)).padTop(14).left().row();
        BsPieChart donut = new BsPieChart();
        donut.setSize(360, 320);
        donut.setSkinFont(skin);
        donut.setDonutHole(0.55f);
        donut.setLegendPlacement(BsChart.LegendPlacement.RIGHT);
        donut.setSlices(
                "Chrome", 50,
                "Firefox", 25,
                "Safari", 15,
                "Edge", 10
        );
        c.add(wrapChart(donut, 360, 320)).padTop(4).row();

        c.add(new Label("③ 顶部图例饼图（横向布局）:", skin)).padTop(14).left().row();
        BsPieChart pie3 = new BsPieChart();
        pie3.setSize(560, 280);
        pie3.setSkinFont(skin);
        pie3.setLegendPlacement(BsChart.LegendPlacement.TOP);
        pie3.setSlices(
                "前端", 40,
                "后端", 35,
                "运维", 15,
                "测试", 10
        );
        c.add(wrapChart(pie3, 560, 280)).padTop(4).row();

        c.add(new Label("④ 无图例饼图:", skin)).padTop(14).left().row();
        BsPieChart pie4 = new BsPieChart();
        pie4.setSize(280, 280);
        pie4.setSkinFont(skin);
        pie4.setLegendPlacement(BsChart.LegendPlacement.NONE);
        pie4.setSlices(
                "A", 30,
                "B", 25,
                "C", 20,
                "D", 15,
                "E", 10
        );
        c.add(wrapChart(pie4, 280, 280)).padTop(4).row();

        Label note = new Label("(鼠标 hover 扇形会外推并显示百分比 tooltip；点击图例切换)", skin);
        c.add(note).padTop(8).row();
    }

    // ============================ Charts-Legend 图例与切换 ============================
    private void fillChartsLegend(Table c) {
        c.add(sectionTitle("Charts-Legend  —— 图例位置 / 点击切换 / 单击隔离")).row();

        c.add(new Label("① 图例位置对比（4 个折线图，分别 TOP/BOTTOM/LEFT/RIGHT）:", skin)).padTop(8).left().row();
        Table legendPosRow = new Table();
        legendPosRow.defaults().pad(6);
        legendPosRow.add(makeLineWithLegend("图例=TOP", BsChart.LegendPlacement.TOP)).size(320, 200);
        legendPosRow.add(makeLineWithLegend("图例=BOTTOM", BsChart.LegendPlacement.BOTTOM)).size(320, 200);
        legendPosRow.row();
        legendPosRow.add(makeLineWithLegend("图例=LEFT", BsChart.LegendPlacement.LEFT)).size(320, 200);
        legendPosRow.add(makeLineWithLegend("图例=RIGHT", BsChart.LegendPlacement.RIGHT)).size(320, 200);
        c.add(legendPosRow).row();

        c.add(new Label("② 点击图例切换系列显隐（点击图中顶部图例条目，对应系列会隐藏/恢复）:",
                skin)).padTop(14).left().row();
        BsLineChart toggleChart = new BsLineChart();
        toggleChart.setSize(640, 260);
        toggleChart.setSkinFont(skin);
        toggleChart.setLegendPlacement(BsChart.LegendPlacement.TOP);
        toggleChart.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("CPU", BsChart.pointsOfY(40, 55, 50, 65, 70, 60, 75)),
                new BsChart.Series("内存", BsChart.pointsOfY(30, 35, 40, 38, 45, 50, 48)),
                new BsChart.Series("磁盘", BsChart.pointsOfY(20, 25, 30, 28, 35, 40, 42)),
                new BsChart.Series("网络", BsChart.pointsOfY(10, 20, 15, 25, 30, 28, 35))
        ));
        c.add(wrapChart(toggleChart, 640, 260)).padTop(4).row();

        c.add(new Label("③ 点击隔离（点击图中数据点 → 只显示该系列；Shift+点击 → 多选对比）:",
                skin)).padTop(14).left().row();
        BsLineChart isoChart = new BsLineChart();
        isoChart.setSize(640, 260);
        isoChart.setSkinFont(skin);
        isoChart.setClickToIsolate(true);
        isoChart.setLegendPlacement(BsChart.LegendPlacement.TOP);
        isoChart.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("北京", BsChart.pointsOfY(15, 22, 28, 35, 42, 38, 45)),
                new BsChart.Series("上海", BsChart.pointsOfY(20, 28, 35, 42, 50, 48, 55)),
                new BsChart.Series("广州", BsChart.pointsOfY(25, 32, 38, 45, 52, 55, 60)),
                new BsChart.Series("深圳", BsChart.pointsOfY(18, 25, 30, 38, 44, 42, 50))
        ));
        c.add(wrapChart(isoChart, 640, 260)).padTop(4).row();

        Label note = new Label("(③ 中按住 Shift 键再点数据点可保留多条做对比；只点一下其他都隐藏)", skin);
        c.add(note).padTop(8).row();
    }

    private BsLineChart makeLineWithLegend(String label, BsChart.LegendPlacement placement) {
        BsLineChart chart = new BsLineChart();
        chart.setSize(320, 200);
        chart.setSkinFont(skin);
        chart.setLegendPlacement(placement);
        chart.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("A", BsChart.pointsOfY(3, 5, 4, 8, 7)),
                new BsChart.Series("B", BsChart.pointsOfY(1, 4, 6, 5, 7))
        ));
        return chart;
    }

    // ============================ Charts-Hover Hover 数据查看 ============================
    private void fillChartsHover(Table c) {
        c.add(sectionTitle("Charts-Hover  —— 鼠标 hover 查看坐标/数值/百分比")).row();

        c.add(new Label("① 折线图 Hover（移动鼠标到数据点附近，显示坐标）:", skin)).padTop(8).left().row();
        BsLineChart line = new BsLineChart();
        line.setSize(640, 260);
        line.setSkinFont(skin);
        line.setHoverEnabled(true);
        line.setHitRadius(20);   // 放大命中半径便于测试
        line.setLegendPlacement(BsChart.LegendPlacement.TOP);
        line.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("玩家在线", BsChart.pointsOfY(120, 180, 240, 280, 320, 380, 420, 480, 460, 500)),
                new BsChart.Series("同时在线峰值", BsChart.pointsOfY(80, 130, 180, 220, 260, 320, 360, 400, 390, 430))
        ));
        c.add(wrapChart(line, 640, 260)).padTop(4).row();

        c.add(new Label("② 柱状图 Hover（移动鼠标到柱子上，显示数值）:", skin)).padTop(14).left().row();
        BsBarChart bar = new BsBarChart();
        bar.setSize(640, 260);
        bar.setSkinFont(skin);
        bar.setHoverEnabled(true);
        bar.setLegendPlacement(BsChart.LegendPlacement.TOP);
        bar.setCategories("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        bar.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("本周", BsChart.pointsOfY(120, 140, 135, 160, 180, 220, 200)),
                new BsChart.Series("上周", BsChart.pointsOfY(110, 130, 128, 155, 170, 210, 195))
        ));
        c.add(wrapChart(bar, 640, 260)).padTop(4).row();

        c.add(new Label("③ 饼图 Hover（扇形外推 + 显示百分比）:", skin)).padTop(14).left().row();
        BsPieChart pie = new BsPieChart();
        pie.setSize(560, 320);
        pie.setSkinFont(skin);
        pie.setHoverEnabled(true);
        pie.setLegendPlacement(BsChart.LegendPlacement.RIGHT);
        pie.setSlices(
                "学生", 1200,
                "教师", 350,
                "职工", 280,
                "访客", 480,
                "其他", 150
        );
        c.add(wrapChart(pie, 560, 320)).padTop(4).row();

        c.add(new Label("④ 关闭 Hover（对比效果）:", skin)).padTop(14).left().row();
        BsLineChart lineNoHover = new BsLineChart();
        lineNoHover.setSize(640, 200);
        lineNoHover.setSkinFont(skin);
        lineNoHover.setHoverEnabled(false);
        lineNoHover.setLegendPlacement(BsChart.LegendPlacement.TOP);
        lineNoHover.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("数据 A", BsChart.pointsOfY(5, 8, 6, 12, 9, 15, 11)),
                new BsChart.Series("数据 B", BsChart.pointsOfY(3, 5, 7, 9, 8, 11, 13))
        ));
        c.add(wrapChart(lineNoHover, 640, 200)).padTop(4).row();

        Label note = new Label("(hover 命中半径可调：折线 setHitRadius，默认 12px)", skin);
        c.add(note).padTop(8).row();
    }

    /** 把自绘图表 Actor 包装成 Container 以便加入 Table。 */
    private <T extends Actor> Container<T> wrapChart(
            T chart, float w, float h) {
        Container<T> wrap = new Container<>(chart);
        wrap.fill();
        wrap.size(w, h);
        return wrap;
    }

    // ============================ P2-Content: Placeholder/Figure/ListGroup/FloatingLabel ============================
    private void fillP2Content(Table c) {
        c.add(sectionTitle("P2-Content  —— 骨架屏/图文/列表组/浮动标签")).row();

        // ===== BsPlaceholder 骨架屏 =====
        c.add(new Label("① BsPlaceholder 骨架屏（卡片模板 + 列表项模板，带呼吸动画）:",
                skin)).padTop(8).left().row();
        Table skelRow = new Table();
        skelRow.defaults().pad(10);
        BsPlaceholder card = BsPlaceholder.card(skin).pulsing(true);
        skelRow.add(card).size(380, 240).pad(10);

        BsPlaceholder listItem = BsPlaceholder.listItem(skin).pulsing(true);
        skelRow.add(listItem).size(220, 60).pad(10);
        c.add(skelRow).left().row();

        c.add(new Label("② 自定义骨架（多行不同宽度块）:", skin)).padTop(12).left().row();
        BsPlaceholder custom = new BsPlaceholder(skin);
        custom.newRow().col(280, 16);
        custom.newRow().col(220, 12);
        custom.newRow().col(160, 12);
        custom.newRow().col(280, 12);
        custom.pulsing(true).setPulseSpeed(1.2f);
        c.add(custom).left().pad(8).row();

        // ===== BsFigure 图文 =====
        c.add(new Label("③ BsFigure 图文（图片 + 图注）:", skin)).padTop(14).left().row();
        try {
            Drawable img1 =
                    BsModal.drawableFromPath("bs/test/img/20251110013443.png");
            BsFigure fig = new BsFigure(skin)
                    .image(img1)
                    .imageSize(320, 180)
                    .caption("图 1：这是产品发布会的现场照片，演示 Bs UI 框架的应用场景");
            c.add(fig).width(340).growX().padTop(4).row();
        } catch (Throwable t) {
            c.add(new Label("(图片加载失败: " + t.getMessage() + ")", skin)).row();
        }

        // ===== BsListGroup 列表组 =====
        c.add(new Label("④ BsListGroup 列表组（图标/副标题/badge/禁用/选中态）:",
                skin)).padTop(14).left().row();
        BsListGroup group = new BsListGroup(skin);
        group.setItemHeight(48);
        Drawable envelope =
                com.git.bs.ui.BsIcon.get("envelope");
        Drawable gear =
                com.git.bs.ui.BsIcon.get("gear");
        group.addItem(item -> item
                .icon(envelope)
                .title("收件箱")
                .subtitle("12 条未读消息")
                .badge("12")
                .badgeColor(new Color(0xDC / 255f, 0x35 / 255f, 0x45 / 255f, 1f)));
        group.addItem(item -> item
                .title("草稿箱")
                .subtitle("3 篇草稿"));
        group.addItem(item -> item
                .icon(gear)
                .title("设置")
                .subtitle("账户和偏好")
                .badge("NEW")
                .badgeColor(new Color(0x19 / 255f, 0x87 / 255f, 0x54 / 255f, 1f)));
        group.addItem(item -> item
                .title("已发送")
                .disabled(true));
        group.select(0);
        group.setOnSelect(idx -> setStatus("ListGroup 选中: " + idx));
        c.add(group).width(420).growX().padTop(4).row();

        // ===== BsFloatingLabel 浮动标签 =====
        c.add(new Label("⑤ BsFloatingLabel 浮动标签（聚焦/有内容时标签浮到顶部）:",
                skin)).padTop(14).left().row();
        Table floatRow = new Table();
        floatRow.defaults().pad(8).left();
        BsFloatingLabel f1 = new BsFloatingLabel(skin, "用户名");
        f1.setWidth(220);
        floatRow.add(f1).width(220);
        BsFloatingLabel f2 = new BsFloatingLabel(skin, "邮箱");
        f2.setWidth(220);
        floatRow.add(f2).width(220);
        c.add(floatRow).left().row();

        c.add(new Label("(点击输入框聚焦 → 顶部出现标签；输入内容后标签常驻)",
                skin)).padTop(4).row();
    }

    // ============================ P2-Carousel 轮播图 ============================
    private void fillP2Carousel(Table c) {
        c.add(sectionTitle("P2-Carousel  —— 轮播图（自动播放 + 左右箭头 + 指示点）")).row();

        c.add(new Label("① 自动播放 3 秒切换（点击左右箭头或底部圆点手动切换）:",
                skin)).padTop(8).left().row();
        BsCarousel carousel = new BsCarousel(skin);
        carousel.setSize(640, 280);
        carousel.setAutoPlay(true);
        carousel.setInterval(3f);
        carousel.addSlide(makeColorSlide("第一张 Banner", "新品上线",
                new Color(0x0D / 255f, 0x6E / 255f, 0xFD / 255f, 1f)));
        carousel.addSlide(makeColorSlide("第二张 Banner", "限时折扣",
                new Color(0xDC / 255f, 0x35 / 255f, 0x45 / 255f, 1f)));
        carousel.addSlide(makeColorSlide("第三张 Banner", "会员专享",
                new Color(0x19 / 255f, 0x87 / 255f, 0x54 / 255f, 1f)));
        carousel.addSlide(makeColorSlide("第四张 Banner", "马上抢购",
                new Color(0xFF / 255f, 0xC1 / 255f, 0x07 / 255f, 1f)));
        c.add(new Container<BsCarousel>() {{
            setActor(carousel);
            fill();
        }}).size(640, 280).padTop(4).row();

        c.add(new Label("② 5 秒切换（慢速 + 实际图片）:", skin)).padTop(14).left().row();
        BsCarousel imgCarousel = new BsCarousel(skin);
        imgCarousel.setSize(640, 280);
        imgCarousel.setAutoPlay(true);
        imgCarousel.setInterval(5f);
        try {
            Drawable d1 =
                    BsModal.drawableFromPath("bs/test/img/20251110013443.png");
            Drawable d2 =
                    BsModal.drawableFromPath("bs/test/img/20251109230728.png");
            imgCarousel.addSlide(makeImageSlide(d1, "图片轮播 1"));
            imgCarousel.addSlide(makeImageSlide(d2, "图片轮播 2"));
        } catch (Throwable t) {
            imgCarousel.addSlide(makeColorSlide("图片加载失败", "占位",
                    Color.GRAY));
        }
        c.add(new Container<BsCarousel>() {{
            setActor(imgCarousel);
            fill();
        }}).size(640, 280).padTop(4).row();

        c.add(new Label("③ 不自动播放（手动控制）:", skin)).padTop(14).left().row();
        BsCarousel manual = new BsCarousel(skin);
        manual.setSize(640, 200);
        manual.setAutoPlay(false);
        manual.addSlide(makeColorSlide("A", "纯手动控制", new Color(0x6C / 255f, 0x75 / 255f, 0x7D / 255f, 1f)));
        manual.addSlide(makeColorSlide("B", "需要点箭头切换", new Color(0x0D / 255f, 0xCA / 255f, 0xF0 / 255f, 1f)));
        c.add(new Container<BsCarousel>() {{
            setActor(manual);
            fill();
        }}).size(640, 200).padTop(4).row();

        Label carouselNote = new Label("(底部圆点 = 指示器，可点击跳转；左右 ‹ › = 切换箭头)", skin);
        c.add(carouselNote).padTop(8).row();
    }

    /** 生成一个色块 slide（标题 + 副标题居中）。 */
    private Actor makeColorSlide(String title, String subtitle, Color bg) {
        Table t = new Table(skin);
        t.setBackground(skin.newDrawable("white", bg));
        t.center();
        Label t1 = new Label(title, skin);
        t1.setColor(Color.WHITE);
        t1.setFontScale(1.6f);
        Label t2 = new Label(subtitle, skin);
        t2.setColor(new Color(1, 1, 1, 0.85f));
        t2.setFontScale(1.1f);
        t.add(t1).row();
        t.add(t2).padTop(8).row();
        return t;
    }

    /** 生成一个图片 slide（图 + 角标）。 */
    private Actor makeImageSlide(
            Drawable img, String tag) {
        Table t = new Table(skin);
        t.setBackground(img);
        t.bottom().left();
        Label tagLabel = new Label(tag, skin);
        tagLabel.setColor(Color.WHITE);
        tagLabel.setFontScale(1.1f);
        Container<Label> tagWrap = new Container<>(tagLabel);
        tagWrap.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.5f)));
        tagWrap.pad(4, 10, 4, 10);
        t.add(tagWrap).pad(10).left();
        return t;
    }

    // ============================ Charts-Extended: Area/Spline/Scatter/Radar/Doughnut ============================
    private void fillChartsExtended(Table c) {
        c.add(sectionTitle("Charts-Extended  —— 面积/曲线/散点/雷达/环形")).row();

        // ===== BsAreaChart 面积图 =====
        c.add(new Label("① BsAreaChart 面积图（折线下方填充半透明色）:", skin)).padTop(8).left().row();
        BsAreaChart area = new BsAreaChart();
        area.setSize(640, 220);
        area.setSkinFont(skin);
        area.setLegendPlacement(BsChart.LegendPlacement.TOP);
        area.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("访问量", BsChart.pointsOfY(20, 35, 40, 55, 70, 85, 90, 75, 88, 95)),
                new BsChart.Series("独立访客", BsChart.pointsOfY(10, 18, 22, 30, 40, 50, 60, 55, 62, 70))
        ));
        c.add(wrapChart(area, 640, 220)).padTop(4).row();

        // ===== BsSplineChart 平滑曲线 =====
        c.add(new Label("② BsSplineChart 平滑曲线（Catmull-Rom 插值）:", skin)).padTop(14).left().row();
        BsSplineChart spline = new BsSplineChart();
        spline.setSize(640, 220);
        spline.setSkinFont(skin);
        spline.setLegendPlacement(BsChart.LegendPlacement.TOP);
        spline.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("用户增长", BsChart.pointsOfY(5, 12, 25, 38, 50, 65, 88, 110, 135, 160)),
                new BsChart.Series("预期目标", BsChart.pointsOfY(10, 18, 28, 38, 50, 62, 75, 90, 105, 120))
        ));
        c.add(wrapChart(spline, 640, 220)).padTop(4).row();

        // ===== BsScatterChart 散点图 =====
        c.add(new Label("③ BsScatterChart 散点图（身高/体重分布）:", skin)).padTop(14).left().row();
        BsScatterChart scatter = new BsScatterChart();
        scatter.setSize(640, 280);
        scatter.setSkinFont(skin);
        scatter.setPointRadius(5);
        scatter.setLegendPlacement(BsChart.LegendPlacement.TOP);
        scatter.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("男", BsChart.points(
                        160, 55, 165, 60, 170, 65, 175, 70, 180, 75, 178, 78, 172, 68, 182, 80,
                        168, 58, 185, 82, 176, 72)),
                new BsChart.Series("女", BsChart.points(
                        150, 45, 155, 50, 158, 52, 162, 55, 168, 60, 165, 58, 170, 62, 160, 52,
                        170, 65, 155, 48, 162, 56))
        ));
        c.add(wrapChart(scatter, 640, 280)).padTop(4).row();

        // ===== BsRadarChart 雷达图 =====
        c.add(new Label("④ BsRadarChart 雷达图（角色属性对比）:", skin)).padTop(14).left().row();
        Table radarRow = new Table();
        radarRow.defaults().pad(10);

        BsRadarChart radar1 = new BsRadarChart();
        radar1.setSize(320, 320);
        radar1.setSkinFont(skin);
        radar1.setMaxValue(100);
        radar1.setAxes("攻击", "防御", "速度", "智力", "运气");
        radar1.setLegendPlacement(BsChart.LegendPlacement.TOP);
        radar1.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("战士", BsChart.pointsOfY(85, 90, 40, 50, 60)),
                new BsChart.Series("法师", BsChart.pointsOfY(30, 40, 60, 95, 70))
        ));
        radarRow.add(wrapChart(radar1, 320, 320));

        BsRadarChart radar2 = new BsRadarChart();
        radar2.setSize(320, 320);
        radar2.setSkinFont(skin);
        radar2.setMaxValue(100);
        radar2.setAxes("语数", "英语", "物理", "化学", "生物", "历史");
        radar2.setLegendPlacement(BsChart.LegendPlacement.TOP);
        radar2.setMultiSeries(java.util.Arrays.asList(
                new BsChart.Series("学生 A", BsChart.pointsOfY(90, 85, 75, 80, 70, 95)),
                new BsChart.Series("学生 B", BsChart.pointsOfY(70, 60, 95, 85, 80, 65))
        ));
        radarRow.add(wrapChart(radar2, 320, 320));
        c.add(radarRow).padTop(4).row();

        // ===== BsDoughnutChart 环形图（中心显示总值）=====
        c.add(new Label("⑤ BsDoughnutChart 环形图（中心显示总值）:", skin)).padTop(14).left().row();
        BsDoughnutChart donut = new BsDoughnutChart();
        donut.setSize(560, 320);
        donut.setSkinFont(skin);
        donut.setLegendPlacement(BsChart.LegendPlacement.RIGHT);
        donut.setCenterLabel("总员工数", "2460");
        donut.setSlices(
                "研发", 1200,
                "销售", 600,
                "运营", 350,
                "市场", 180,
                "管理", 130
        );
        c.add(wrapChart(donut, 560, 320)).padTop(4).row();

        Label doughnutNote = new Label("(⑤ 中环形图中心绘制了「总员工数 2460」，便于一眼看总数)", skin);
        c.add(doughnutNote).padTop(8).row();
    }

    // ============================ Wave1-Basics: Switch/Avatar/Timeline/Statistic/Steps/Empty/Rating ============================
    private void fillWave1Basics(Table c) {
        c.add(sectionTitle("Wave1-Basics  —— Switch / Avatar / Timeline / Statistic / Steps / Empty / Rating")).row();

        // ===== BsSwitch =====
        c.add(new Label("① BsSwitch 开关（SM/MD/LG，禁用态）:", skin)).padTop(8).left().row();
        Table switchRow = new Table();
        switchRow.defaults().pad(10).left();
        BsSwitch sw1 = new BsSwitch(skin, BsSwitch.Size.SM);
        sw1.setLabel("通知");
        sw1.setChecked(true);
        sw1.setOnChange(v -> setStatus("Switch SM: " + v));
        switchRow.add(sw1);

        BsSwitch sw2 = new BsSwitch(skin, BsSwitch.Size.MD);
        sw2.setLabel("深色模式");
        sw2.setOnChange(v -> setStatus("Switch MD: " + v));
        switchRow.add(sw2);

        BsSwitch sw3 = new BsSwitch(skin, BsSwitch.Size.LG);
        sw3.setLabel("自动更新");
        sw3.setChecked(true);
        sw3.setOnChange(v -> setStatus("Switch LG: " + v));
        switchRow.add(sw3);

        BsSwitch sw4 = new BsSwitch(skin);
        sw4.setLabel("禁用");
        sw4.setChecked(true);
        sw4.setDisabled(true);
        switchRow.add(sw4);
        c.add(switchRow).left().row();

        // ===== BsAvatar =====
        c.add(new Label("② BsAvatar 头像（圆/方 shape + 在线状态）:", skin)).padTop(14).left().row();
        Drawable avatarImg = null;
        try {
            avatarImg = BsModal.drawableFromPath("bs/test/img/20251121200555.png");
        } catch (Throwable ignored) {}
        Table avatarRow = new Table();
        avatarRow.defaults().pad(10);
        // 圆 + 在线
        BsAvatar av1 = new BsAvatar(skin).image(avatarImg).size(56).shape(BsAvatar.Shape.CIRCLE).online(true);
        avatarRow.add(wrapAvatar(av1, "CIRCLE/online"));
        // 圆 + 离线
        BsAvatar av2 = new BsAvatar(skin).image(avatarImg).size(56).shape(BsAvatar.Shape.CIRCLE).online(false);
        avatarRow.add(wrapAvatar(av2, "CIRCLE/offline"));
        // 圆角方
        BsAvatar av3 = new BsAvatar(skin).image(avatarImg).size(56).shape(BsAvatar.Shape.ROUNDED);
        avatarRow.add(wrapAvatar(av3, "ROUNDED"));
        // 小头像
        BsAvatar av4 = new BsAvatar(skin).image(avatarImg).size(32).shape(BsAvatar.Shape.CIRCLE);
        avatarRow.add(wrapAvatar(av4, "small 32"));
        c.add(avatarRow).left().row();

        // ===== BsTimeline =====
        c.add(new Label("③ BsTimeline 时间轴（任务进度，6 色）:", skin)).padTop(14).left().row();
        BsTimeline tl = new BsTimeline(skin);
        tl.addItem("09:00", "创建了任务", BsTimeline.Color.PRIMARY);
        tl.addItem("10:30", "分配给张三", BsTimeline.Color.INFO);
        tl.addItem("14:00", "开始处理", BsTimeline.Color.WARNING);
        tl.addItem("15:30", "遇到问题需要 review", BsTimeline.Color.DANGER);
        tl.addItem("16:00", "问题已解决", BsTimeline.Color.SUCCESS);
        tl.setOnClick(item -> setStatus("Timeline: " + item.getTitle()));
        c.add(tl).width(500).growX().padTop(4).row();

        // ===== BsStatistic =====
        c.add(new Label("④ BsStatistic 数字统计卡（4 卡片，趋势 ↑↓）:", skin)).padTop(14).left().row();
        Table statRow = new Table();
        statRow.defaults().pad(8);
        statRow.add(new BsStatistic(skin).title("今日营收").value("¥12,345").trend(12.5f)).width(200);
        statRow.add(new BsStatistic(skin).title("活跃用户").value("8,920").trend(5.3f)).width(200);
        statRow.add(new BsStatistic(skin).title("订单量").value("432").trend(-2.8f)).width(200);
        statRow.add(new BsStatistic(skin).title("转化率").value("3.2%").trend(0f)).width(200);
        statRow.row();
        c.add(statRow).left().row();

        // ===== BsSteps =====
        c.add(new Label("⑤ BsSteps 步骤条（默认色：DONE 绿+线 / CURRENT 蓝带 ring / WAIT 空心灰）:",
                skin)).padTop(14).left().row();
        BsSteps steps = new BsSteps(skin);
        steps.addSteps("填写资料", "验证邮箱", "设置密码", "完成");
        steps.setCurrent(1);
        steps.setOnStepClick(idx -> setStatus("Steps: 切到第 " + (idx + 1) + " 步"));
        c.add(steps).growX().padTop(4).row();

        c.add(new Label("⑥ BsSteps 自定义颜色（DONE=紫 / CURRENT=橙 / WAIT=灰，线粗 4px）:",
                skin)).padTop(10).left().row();
        BsSteps customSteps = new BsSteps(skin);
        customSteps.addSteps("Step 1", "Step 2", "Step 3", "Step 4", "Step 5");
        customSteps.setCurrent(2);
        customSteps.setDoneColor(new Color(0x6F / 255f, 0x42 / 255f, 0xC1 / 255f, 1f));   // 紫
        customSteps.setCurrentColor(new Color(0xFD / 255f, 0x7E / 255f, 0x14 / 255f, 1f)); // 橙
        customSteps.setWaitColor(new Color(0x6C / 255f, 0x75 / 255f, 0x7D / 255f, 1f));    // 灰
        customSteps.setLineHeight(4);
        customSteps.setLineLength(50);
        customSteps.setOnStepClick(idx -> setStatus("CustomSteps: 第 " + (idx + 1) + " 步"));
        c.add(customSteps).growX().padTop(4).row();

        c.add(new Label("⑦ BsSteps 第 1 步（看不到已完成线）/ 最后一步（全部完成线）:",
                skin)).padTop(10).left().row();
        BsSteps firstStep = new BsSteps(skin);
        firstStep.addSteps("开始", "处理中", "完成");
        firstStep.setCurrent(0);
        firstStep.setOnStepClick(idx -> setStatus("firstStep: " + idx));
        c.add(firstStep).growX().padTop(4).row();

        BsSteps allDone = new BsSteps(skin);
        allDone.addSteps("开始", "处理中", "完成");
        allDone.setCurrent(2);   // 全部完成（最后一步没"完成"线，圆里是数字不是勾）
        allDone.setOnStepClick(idx -> setStatus("allDone: " + idx));
        c.add(allDone).growX().padTop(4).row();

        // ===== BsEmpty =====
        c.add(new Label("⑥ BsEmpty 空状态（无数据占位）:", skin)).padTop(14).left().row();
        BsEmpty empty = new BsEmpty(skin)
                .title("暂无消息")
                .description("您还没有收到任何消息，点击按钮刷新")
                .actionButton("刷新", () -> setStatus("点了刷新"));
        c.add(empty).growX().padTop(4).row();

        // ===== BsRating =====
        c.add(new Label("⑦ BsRating 星级评分（默认/半星/只读）:", skin)).padTop(14).left().row();
        Table ratingRow = new Table();
        ratingRow.defaults().pad(10).left();
        BsRating r1 = new BsRating(skin);
        r1.setValue(3);
        r1.setOnChange(v -> setStatus("Rating 1: " + v));
        ratingRow.add(r1);
        BsRating r2 = new BsRating(skin);
        r2.setHalfStars(true);
        r2.setValue(3.5f);
        r2.setOnChange(v -> setStatus("Rating 2 (半星): " + v));
        ratingRow.add(r2);
        BsRating r3 = new BsRating(skin);
        r3.setValue(4);
        r3.setReadOnly(true);
        ratingRow.add(r3);
        c.add(ratingRow).left().row();
    }

    /** Avatar 包装（添加下方说明文字）。 */
    private Table wrapAvatar(BsAvatar av, String label) {
        Table t = new Table();
        t.top();
        t.add(av).row();
        Label l = new Label(label, skin);
        l.setColor(Color.GRAY);
        l.setFontScale(0.85f);
        t.add(l).padTop(4);
        return t;
    }

    // ============================ Wave1-Inputs: AutoComplete/TagInput/DescriptionList ============================
    private void fillWave1Inputs(Table c) {
        c.add(sectionTitle("Wave1-Inputs  —— AutoComplete / TagInput / DescriptionList")).row();

        // ===== BsAutoComplete =====
        c.add(new Label("① BsAutoComplete 自动补全（输入 a/b/c 等查看建议）:",
                skin)).padTop(8).left().row();
        BsAutoComplete ac = new BsAutoComplete(skin);
        ac.setPopupWidth(280);
        ac.setCandidates(java.util.Arrays.asList(
                "Apple", "Banana", "Cherry", "Grape", "Orange",
                "Peach", "Pear", "Pineapple", "Strawberry", "Watermelon",
                "Avocado", "Blueberry", "Coconut", "Dragonfruit"
        ));
        ac.setOnSelect(text -> setStatus("AutoComplete 选了: " + text));
        c.add(ac).padTop(4).row();

        // ===== BsTagInput =====
        c.add(new Label("② BsTagInput 标签输入（回车变 chip，× 删除）:", skin)).padTop(14).left().row();
        BsTagInput tags = new BsTagInput(skin);
        tags.addTags(java.util.Arrays.asList("Java", "libgdx", "UI"));
        tags.setPlaceholder("输入标签后回车");
        tags.setOnChange(list -> setStatus("TagInput: " + list));
        c.add(tags).width(420).growX().padTop(4).row();

        // ===== BsDescriptionList =====
        c.add(new Label("③ BsDescriptionList 描述列表（用户详情，2 列）:", skin)).padTop(14).left().row();
        BsDescriptionList dl = new BsDescriptionList(skin);
        dl.setColumns(2);
        dl.setLabelWidth(80).setValueWidth(160);
        dl.addItem("姓名", "张三");
        dl.addItem("邮箱", "zhangsan@example.com");
        dl.addItem("手机", "13800138000");
        dl.addItem("城市", "北京");
        dl.addItem("部门", "研发部");
        dl.addItem("职级", "P7");
        dl.addItem("入职日期", "2023-05-08");
        dl.addItem("状态", "在职");
        c.add(dl).width(560).growX().padTop(4).row();

        Label inputsNote = new Label("(输入框点击外部时 AutoComplete popup 自动关闭)", skin);
        c.add(inputsNote).padTop(8).row();
    }

    // ============================ Wave1-Feedback: Result/LoadingOverlay ============================
    private void fillWave1Feedback(Table c) {
        c.add(sectionTitle("Wave1-Feedback  —— Result / LoadingOverlay")).row();

        // ===== BsResult 4 种类型 =====
        c.add(new Label("① BsResult 结果页（4 种类型，可加按钮）:", skin)).padTop(8).left().row();
        Table resultRow = new Table();
        resultRow.defaults().pad(8);

        BsResult r1 = new BsResult(skin, BsResult.Type.SUCCESS)
                .title("提交成功")
                .description("您的申请已成功提交")
                .primaryButton("返回", () -> setStatus("成功-返回"));
        resultRow.add(r1).size(280, 220);

        BsResult r2 = new BsResult(skin, BsResult.Type.WARNING)
                .title("请注意")
                .description("此操作可能会影响其他用户")
                .primaryButton("继续", () -> setStatus("警告-继续"))
                .secondaryButton("取消", () -> setStatus("警告-取消"));
        resultRow.add(r2).size(280, 220);
        c.add(resultRow).left().row();

        Table resultRow2 = new Table();
        resultRow2.defaults().pad(8);
        BsResult r3 = new BsResult(skin, BsResult.Type.ERROR)
                .title("提交失败")
                .description("网络异常，请稍后重试")
                .primaryButton("重试", () -> setStatus("失败-重试"));
        resultRow2.add(r3).size(280, 220);

        BsResult r4 = new BsResult(skin, BsResult.Type.INFO)
                .title("信息提示")
                .description("系统将于今晚 22:00 维护")
                .secondaryButton("知道了", () -> setStatus("信息-知道了"));
        resultRow2.add(r4).size(280, 220);
        c.add(resultRow2).left().padTop(8).row();

        // ===== BsLoadingOverlay =====
        c.add(new Label("② BsLoadingOverlay 全屏加载遮罩（3 种触发方式）:",
                skin)).padTop(14).left().row();
        Table loadingRow = new Table();
        loadingRow.defaults().pad(6);

        BsButton b1 = new BsButton("显示加载(2秒后自动关)", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.SM);
        b1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsLoadingOverlay.show(stage, skin, "加载中...", 2f, true);
            }
        });
        loadingRow.add(b1);

        BsButton b2 = new BsButton("带进度条的加载(手动关)", skin,
                BsButton.Variant.INFO, BsButton.Style.SOLID, BsButton.Size.SM);
        b2.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsLoadingOverlay overlay = BsLoadingOverlay.show(stage, skin, "上传中", 0.0f);
                // 后台线程模拟进度推进，通过 Gdx.app.postRunnable 切回主线程更新 UI
                Thread t = new Thread(() -> {
                    for (int i = 1; i <= 10; i++) {
                        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                        final float p = i / 10f;
                        Gdx.app.postRunnable(() -> overlay.setProgress(p));
                    }
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    Gdx.app.postRunnable(overlay::close);
                });
                t.setDaemon(true);
                t.start();
            }
        });
        loadingRow.add(b2);

        BsButton b3 = new BsButton("长文本加载(3秒)", skin,
                BsButton.Variant.WARNING, BsButton.Style.SOLID, BsButton.Size.SM);
        b3.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsLoadingOverlay.show(stage, skin, "正在处理大量数据，请稍候...", 3f, true);
            }
        });
        loadingRow.add(b3);

        c.add(loadingRow).left().row();
        Label feedbackNote = new Label("(LoadingOverlay 模态遮罩，期间拦截所有点击操作)", skin);
        c.add(feedbackNote).padTop(8).row();
    }

    // ============================ Wave2-Data: DataTable / PropertySheet ============================
    private BsDataTable dataTable;

    private void fillWave2Data(Table c) {
        c.add(sectionTitle("Wave2-Data  —— DataTable 增强表格 / PropertySheet 属性编辑器")).row();

        // ===== BsDataTable =====
        c.add(new Label("① BsDataTable 增强表格（分页 + 排序 + 单选 + 空状态）:",
                skin)).padTop(8).left().row();
        dataTable = new BsDataTable(skin);
        dataTable.setHeaders("ID", "姓名", "年龄", "部门", "状态");
        dataTable.setPageSize(8);
        dataTable.setSortable(true);
        dataTable.setMultiSelect(false);

        // 准备 25 行数据
        List<List<String>> rows = new java.util.ArrayList<>();
        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十",
                "郑十一", "王十二", "冯十三", "陈十四", "褚十五", "卫十六", "蒋十七"};
        String[] depts = {"研发", "销售", "运营", "市场", "财务"};
        String[] statuses = {"在职", "休假", "离职"};
        for (int i = 0; i < 25; i++) {
            rows.add(java.util.Arrays.asList(
                    String.valueOf(1001 + i),
                    names[i % names.length],
                    String.valueOf(22 + (i * 3) % 40),
                    depts[i % depts.length],
                    statuses[i % statuses.length]
            ));
        }
        dataTable.setData(rows);
        dataTable.setOnRowSelect(idx -> setStatus("DataTable 选中行: " + dataTable.getRow(idx)));
        dataTable.setOnSort((col, asc) -> setStatus("DataTable 排序: 列" + col + (asc ? " ↑" : " ↓")));
        c.add(dataTable).growX().padTop(4).row();

        // 控制按钮
        Table dtCtrl = new Table();
        dtCtrl.defaults().pad(4);
        BsButton bReload = new BsButton("重新加载 25 行", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bReload.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dataTable.setData(rows);
                setStatus("DataTable 重新加载");
            }
        });
        dtCtrl.add(bReload);

        BsButton bEmpty = new BsButton("切换为空", skin,
                BsButton.Variant.WARNING, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bEmpty.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dataTable.setData(java.util.Collections.emptyList());
                setStatus("DataTable 切换为空（演示 BsEmpty）");
            }
        });
        dtCtrl.add(bEmpty);

        BsButton bSelectFirst = new BsButton("选中第 1 行", skin,
                BsButton.Variant.INFO, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bSelectFirst.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dataTable.setSelected(0, true);
                setStatus("DataTable 选中第 0 行");
            }
        });
        dtCtrl.add(bSelectFirst);

        BsButton bShowSel = new BsButton("显示选中", skin,
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bShowSel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus("DataTable 选中: " + dataTable.getSelectedRows());
            }
        });
        dtCtrl.add(bShowSel);
        c.add(dtCtrl).left().padTop(4).row();
        c.add(new Label("(点表头排序，可数字/字符串自动识别；切换页保持选择)", skin)).padTop(4).row();

        // ===== BsPropertySheet =====
        c.add(new Label("② BsPropertySheet 属性编辑器（5 种类型 + 分组）:",
                skin)).padTop(14).left().row();
        BsPropertySheet sheet = new BsPropertySheet(skin);
        sheet.setLabelWidth(110);
        sheet.setValueWidth(200);
        sheet.setOnChange((key, value) -> setStatus("PropertySheet: " + key + " → " + value));

        sheet.addSection("基本信息");
        sheet.addProperty("name", "John Doe", BsPropertySheet.Type.TEXT);
        sheet.addProperty("age", 28, BsPropertySheet.Type.NUMBER);
        sheet.addProperty("role", "Admin", BsPropertySheet.Type.SELECT, "Admin", "Editor", "Viewer");
        sheet.addProperty("enabled", true, BsPropertySheet.Type.BOOLEAN);

        sheet.addSection("外观");
        sheet.addProperty("accent", Color.valueOf("#0D6EFD"),
                BsPropertySheet.Type.COLOR);
        sheet.addProperty("bg", Color.valueOf("#F8F9FA"),
                BsPropertySheet.Type.COLOR);
        sheet.addProperty("theme", "light", BsPropertySheet.Type.SELECT, "light", "dark", "auto");

        sheet.addSection("只读属性");
        sheet.addProperty("id", 10086, BsPropertySheet.Type.READONLY);
        sheet.addProperty("createdAt", "2026-06-27", BsPropertySheet.Type.READONLY);

        c.add(sheet).width(440).growX().padTop(4).row();
        c.add(new Label("(修改任意属性 → 状态栏显示 key→value 变更)", skin)).padTop(4).row();

        // ===== ② DataTable LABEL 模式 + 勾选列 + 单选 =====
        c.add(new Label("③ BsDataTable LABEL 模式（纯 Label + 左侧勾选列单选，行点击不再切选中）:",
                skin)).padTop(14).left().row();
        BsDataTable dt2 = new BsDataTable(skin);
        dt2.setHeaders("ID", "姓名", "部门", "状态");
        dt2.setPageSize(6);
        dt2.setLabelModeWithCheckColumn(false);   // 单选 + 勾选列
        List<List<String>> dt2Rows = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) {
            dt2Rows.add(java.util.Arrays.asList(
                    String.valueOf(2001 + i),
                    names[i % names.length],
                    depts[i % depts.length],
                    statuses[i % statuses.length]
            ));
        }
        dt2.setData(dt2Rows);
        dt2.setOnRowSelect(idx -> setStatus("LABEL 表格勾选: " + dt2.getRow(idx)));
        c.add(dt2).growX().padTop(4).row();

        // ===== ④ DataTable LABEL 模式 + 勾选列 + 多选 =====
        c.add(new Label("④ BsDataTable LABEL 模式（多选，可勾选多行批量操作）:",
                skin)).padTop(14).left().row();
        BsDataTable dt3 = new BsDataTable(skin);
        dt3.setHeaders("ID", "姓名", "部门", "状态");
        dt3.setPageSize(6);
        dt3.setLabelModeWithCheckColumn(true);    // 多选
        dt3.setData(dt2Rows);
        dt3.setOnRowSelect(idx -> setStatus("多选表格当前已选: " + dt3.getSelectedIndices()));
        c.add(dt3).growX().padTop(4).row();
        c.add(new Label("(③ 单选：勾选列点击切换 / 行点击触发回调；④ 多选：可勾多行)", skin))
                .padTop(4).row();
    }

    // ============================ Wave3-Editor: StatusBar ============================
    private void fillWave3Editor(Table c) {
        c.add(sectionTitle("Wave3-Editor  —— BsStatusBar 底部状态栏")).row();

        c.add(new Label("① 模拟编辑器底部状态栏（点 lang 段切换语言）:",
                skin)).padTop(8).left().row();
        BsStatusBar bar = new BsStatusBar(skin);
        bar.setLeftText("Ready");
        bar.setLeftDot(BsStatusBar.DotColor.SUCCESS);
        bar.addLeftSegment("dialogue3.dsl");
        bar.addLeftSegment("已保存");
        bar.setRight("zoom", "缩放: 100%");
        bar.setRight("coords", "x: 0, y: 0");
        bar.setRight("lang", "中文 ▾");
        bar.setRight("encoding", "UTF-8");
        bar.setRight("eol", "LF");
        final String[] langs = {"中文", "English", "日本語"};
        final int[] langIdx = {0};
        bar.setOnRightClick("lang", () -> {
            langIdx[0] = (langIdx[0] + 1) % langs.length;
            bar.setRight("lang", langs[langIdx[0]] + " ▾");
            setStatus("语言切换: " + langs[langIdx[0]]);
        });
        bar.setOnRightClick("zoom", () -> setStatus("点击 zoom（业务方打开缩放对话框）"));
        c.add(bar).growX().padTop(4).row();

        c.add(new Label("② 不同状态色（Idle / Info / Warning / Danger）:",
                skin)).padTop(14).left().row();
        BsStatusBar bar2 = new BsStatusBar(skin);
        bar2.setLeftText("Idle");
        bar2.setLeftDot(BsStatusBar.DotColor.IDLE);
        bar2.setRight("info", "状态可切换");
        c.add(bar2).growX().padTop(4).row();

        BsStatusBar bar3 = new BsStatusBar(skin);
        bar3.setLeftText("Compiling dialogue3.runtime...");
        bar3.setLeftDot(BsStatusBar.DotColor.INFO);
        bar3.setRight("progress", "75%");
        c.add(bar3).growX().padTop(4).row();

        BsStatusBar bar4 = new BsStatusBar(skin);
        bar4.setLeftText("Warning: 2 lint issues");
        bar4.setLeftDot(BsStatusBar.DotColor.WARNING);
        bar4.setRight("hint", "点击查看");
        c.add(bar4).growX().padTop(4).row();

        BsStatusBar bar5 = new BsStatusBar(skin);
        bar5.setLeftText("Error: 节点 ID 冲突");
        bar5.setLeftDot(BsStatusBar.DotColor.DANGER);
        bar5.setRight("hint", "点击修复");
        c.add(bar5).growX().padTop(4).row();

        // 控制按钮动态切换
        c.add(new Label("③ 动态切换状态:", skin)).padTop(14).left().row();
        Table ctrl = new Table();
        ctrl.defaults().pad(4);
        BsButton bReady = new BsButton("Ready(Success)", skin, BsButton.Variant.SUCCESS, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bReady.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                bar2.setLeftText("Ready");
                bar2.setLeftDot(BsStatusBar.DotColor.SUCCESS);
            }
        });
        ctrl.add(bReady);
        BsButton bBusy = new BsButton("Busy(Info)", skin, BsButton.Variant.INFO, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bBusy.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                bar2.setLeftText("Loading assets...");
                bar2.setLeftDot(BsStatusBar.DotColor.INFO);
            }
        });
        ctrl.add(bBusy);
        BsButton bWarn = new BsButton("Warn", skin, BsButton.Variant.WARNING, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bWarn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                bar2.setLeftText("有警告");
                bar2.setLeftDot(BsStatusBar.DotColor.WARNING);
            }
        });
        ctrl.add(bWarn);
        BsButton bErr = new BsButton("Error", skin, BsButton.Variant.DANGER, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bErr.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                bar2.setLeftText("编译失败");
                bar2.setLeftDot(BsStatusBar.DotColor.DANGER);
            }
        });
        ctrl.add(bErr);
        c.add(ctrl).left().padTop(4).row();

        Label sbNote = new Label("(右侧段支持点击回调：第①栏的 lang 段可点击切换语言)", skin);
        c.add(sbNote).padTop(8).row();
    }

    // ============================ Wave2-Business: SearchBar/Toolbar/FileItem/Transfer ============================
    private void fillWave2Business(Table c) {
        c.add(sectionTitle("Wave2-Business  —— SearchBar / Toolbar / FileItem / Transfer")).row();

        // ===== BsSearchBar =====
        c.add(new Label("① BsSearchBar 搜索栏（带过滤下拉 + 清除按钮）:",
                skin)).padTop(8).left().row();
        BsSearchBar sb1 = new BsSearchBar(skin);
        sb1.setPlaceholder("输入用户名或邮箱...");
        sb1.addFilter("全部", "姓名", "邮箱", "手机");
        sb1.setOnSearch(text -> setStatus("搜索: [" + sb1.getFilter() + "] " + text));
        sb1.setOnFilterChange(idx -> setStatus("过滤器: " + idx));
        c.add(sb1).padTop(4).row();

        c.add(new Label("② BsSearchBar 简版（无过滤器）:", skin)).padTop(10).left().row();
        BsSearchBar sb2 = new BsSearchBar(skin, false);
        sb2.setPlaceholder("直接搜索...");
        sb2.setOnSearch(text -> setStatus("简版搜索: " + text));
        c.add(sb2).padTop(4).row();

        // ===== BsToolbar =====
        c.add(new Label("③ BsToolbar 工具栏（文字按钮 + 分隔线 + 图标按钮 + 下拉菜单）:",
                skin)).padTop(14).left().row();
        BsToolbar tb = new BsToolbar(skin);
        tb.addButton("新建", () -> setStatus("新建"), BsButton.Variant.PRIMARY);
        tb.addButton("打开", () -> setStatus("打开"));
        tb.addButton("保存", () -> setStatus("保存"));
        tb.addSeparator();
        Drawable trash =
                com.git.bs.ui.BsIcon.get("trash");
        if (trash != null) {
            tb.addIconButton(trash, () -> setStatus("删除"), BsButton.Variant.DANGER);
        } else {
            tb.addButton("删除", () -> setStatus("删除"), BsButton.Variant.DANGER);
        }
        tb.addSeparator();
        tb.addButtonWithMenu("导出", menu -> {
            menu.addItem("PDF", () -> setStatus("导出 PDF"));
            menu.addItem("PNG", () -> setStatus("导出 PNG"));
            menu.addSeparator();
            menu.addItem("JSON", () -> setStatus("导出 JSON"));
        });
        tb.addSpring();   // 弹簧推右边
        tb.addButton("设置", () -> setStatus("设置"), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        c.add(tb).growX().padTop(4).row();

        // ===== BsFileItem =====
        c.add(new Label("④ BsFileItem 文件项（图标+名称+大小+操作）:",
                skin)).padTop(14).left().row();
        BsFileItem f1 = new BsFileItem(skin)
                .name("screenshot.png")
                .size(145_678)
                .actionButton("删除", () -> setStatus("删除 screenshot.png"), BsButton.Variant.DANGER);
        c.add(f1).growX().padTop(4).row();
        BsFileItem f2 = new BsFileItem(skin)
                .name("report.pdf")
                .size(2_412_350)
                .actionButton("下载", () -> setStatus("下载 report.pdf"), BsButton.Variant.PRIMARY)
                .actionButton("分享", () -> setStatus("分享"), BsButton.Variant.SECONDARY);
        c.add(f2).growX().padTop(2).row();
        BsFileItem f3 = new BsFileItem(skin)
                .name("data.db")
                .sizeText("1.4 GB")
                .actionButton("打开", () -> setStatus("打开 db"), BsButton.Variant.INFO);
        c.add(f3).growX().padTop(2).row();
        BsFileItem f4 = new BsFileItem(skin).name("empty.log").size(0);
        c.add(f4).growX().padTop(2).row();

        // ===== BsTransfer =====
        c.add(new Label("⑤ BsTransfer 穿梭框（左右双列权限分配）:",
                skin)).padTop(14).left().row();
        BsTransfer transfer = new BsTransfer(skin);
        transfer.setOptions("read", "write", "delete", "admin", "audit", "export", "import");
        transfer.setSelected(java.util.Arrays.asList("read", "write"));
        transfer.setOnChange(sel -> setStatus("Transfer 当前已选: " + sel));
        c.add(transfer).padTop(4).row();
        Label transferNote = new Label("(勾选左侧项 → 点 → 移到右侧；右侧点 ← 移回)", skin);
        c.add(transferNote).padTop(4).row();
    }

    // ============================ Wave3-EditorPro: Inspector / NodePalette / MiniMap ============================
    private void fillWave3EditorPro(Table c) {
        c.add(sectionTitle("Wave3-EditorPro  —— Inspector / NodePalette / MiniMap")).row();

        // ===== BsInspectorPanel =====
        c.add(new Label("① BsInspectorPanel 检视面板（带标题栏 + 类型徽章 + × 关闭）:",
                skin)).padTop(8).left().row();
        BsInspectorPanel insp = new BsInspectorPanel(skin);
        insp.setTarget("Player_01", "GameObject",
                com.git.bs.ui.BsIcon.get("person"));
        insp.sheet().setOnChange((key, value) -> setStatus("Inspector: " + key + " → " + value));
        insp.sheet().addSection("基本");
        insp.sheet().addProperty("name", "Hero", BsPropertySheet.Type.TEXT);
        insp.sheet().addProperty("id", 1001, BsPropertySheet.Type.READONLY);
        insp.sheet().addProperty("type", "Player", BsPropertySheet.Type.SELECT,
                "Player", "NPC", "Enemy", "Prop");
        insp.sheet().addSection("位置");
        insp.sheet().addProperty("x", 120.5f);
        insp.sheet().addProperty("y", 80.0f);
        insp.sheet().addProperty("visible", true, BsPropertySheet.Type.BOOLEAN);
        insp.sheet().addSection("外观");
        insp.sheet().addProperty("accent", Color.valueOf("#0D6EFD"),
                BsPropertySheet.Type.COLOR);
        insp.setOnClose(() -> setStatus("Inspector 关闭"));
        c.add(insp).width(440).growX().padTop(4).row();

        // 切换目标按钮
        Table inspCtrl = new Table();
        inspCtrl.defaults().pad(4);
        final BsInspectorPanel inspRef = insp;
        final String[] targets = {"Player_01", "NPC_Guard", "Enemy_Boss", "Prop_Chest"};
        final int[] tIdx = {0};
        BsButton nextTarget = new BsButton("切换检视目标", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        nextTarget.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                tIdx[0] = (tIdx[0] + 1) % targets.length;
                inspRef.setTarget(targets[tIdx[0]], "GameObject");
                inspRef.sheet().clearProperties();
                inspRef.sheet().addSection("基本");
                inspRef.sheet().addProperty("name", targets[tIdx[0]], BsPropertySheet.Type.TEXT);
                inspRef.sheet().addProperty("id", 2000 + tIdx[0], BsPropertySheet.Type.READONLY);
            }
        });
        inspCtrl.add(nextTarget);
        c.add(inspCtrl).left().padTop(4).row();

        // ===== BsNodePalette =====
        c.add(new Label("② BsNodePalette 节点选择面板（分类 + 搜索）:",
                skin)).padTop(14).left().row();
        BsNodePalette palette = new BsNodePalette(skin);
        palette.addCategory("流程控制", cat -> cat
                .node("Start")
                .node("Branch")
                .node("Loop")
                .node("Wait"));
        palette.addCategory("对话", cat -> cat
                .node("Say")
                .node("Choice")
                .node("Narration"));
        palette.addCategory("事件", cat -> cat
                .node("OnStart")
                .node("OnClick")
                .node("OnEnter")
                .node("OnExit"));
        palette.addCategory("变量", cat -> cat
                .node("Set")
                .node("Get")
                .node("Compare"));
        palette.setOnNodeClick((cat, name) -> setStatus("点击节点: " + cat + " → " + name));
        c.add(palette).width(360).height(280).padTop(4).row();

        // ===== BsMiniMap =====
        c.add(new Label("③ BsMiniMap 小地图（节点画布缩略图，点击跳转）:",
                skin)).padTop(14).left().row();
        BsMiniMap mm = new BsMiniMap();
        mm.setSkin(skin);
        mm.setSize(360, 240);
        mm.setCanvasBounds(0, 0, 2000, 1400);
        List<BsMiniMap.Node> nodes = new java.util.ArrayList<>();
        nodes.add(new BsMiniMap.Node(200, 200, new Color(0x0D / 255f, 0x6E / 255f, 0xFD / 255f, 1f)));
        nodes.add(new BsMiniMap.Node(500, 400, new Color(0xDC / 255f, 0x35 / 255f, 0x45 / 255f, 1f)));
        nodes.add(new BsMiniMap.Node(800, 300, new Color(0x19 / 255f, 0x87 / 255f, 0x54 / 255f, 1f)));
        nodes.add(new BsMiniMap.Node(1200, 600, new Color(0xFF / 255f, 0xC1 / 255f, 0x07 / 255f, 1f)));
        nodes.add(new BsMiniMap.Node(1500, 900, new Color(0x0D / 255f, 0xCA / 255f, 0xF0 / 255f, 1f)));
        mm.setNodes(nodes);
        List<float[]> conns = new java.util.ArrayList<>();
        conns.add(new float[]{200, 200, 500, 400});
        conns.add(new float[]{500, 400, 800, 300});
        conns.add(new float[]{800, 300, 1200, 600});
        conns.add(new float[]{1200, 600, 1500, 900});
        mm.setConnections(conns);
        mm.setViewport(400, 300, 600, 400);
        mm.setOnClick(canvasXY -> {
            mm.setViewport(canvasXY[0] - 300, canvasXY[1] - 200, 600, 400);
            setStatus(String.format("MiniMap 跳转到 (%.0f, %.0f)", canvasXY[0], canvasXY[1]));
        });
        c.add(new Container<BsMiniMap>() {{
            setActor(mm);
            fill();
        }}).size(360, 240).padTop(4).row();
        Label mmNote = new Label("(点击小地图任意位置 → 视口矩形移动到该处)", skin);
        c.add(mmNote).padTop(4).row();
    }

    // ============================ Wave3-Misc: Affix / Drawer ============================
    private void fillWave3Misc(Table c) {
        c.add(sectionTitle("Wave3-Misc  —— Affix / Drawer")).row();

        // ===== BsDrawer =====
        c.add(new Label("① BsDrawer 抽屉（标题栏 + 内容 + 底部按钮，从右滑入）:",
                skin)).padTop(8).left().row();
        Table drawerRow = new Table();
        drawerRow.defaults().pad(4);

        BsButton bEditUser = new BsButton("编辑用户", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.SM);
        bEditUser.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                showEditUserDrawer();
            }
        });
        drawerRow.add(bEditUser);

        BsButton bDetail = new BsButton("查看详情", skin,
                BsButton.Variant.INFO, BsButton.Style.SOLID, BsButton.Size.SM);
        bDetail.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsDrawer d = new BsDrawer(skin);
                d.setTitle("订单详情 #10086");
                d.setDrawerWidth(420);
                Table content = new Table(skin);
                content.left().top();
                content.defaults().left().pad(2);
                content.add(new Label("订单号: 10086", skin)).row();
                content.add(new Label("用户: 张三", skin)).row();
                content.add(new Label("金额: ¥328.00", skin)).row();
                content.add(new Label("状态: 已付款", skin)).row();
                content.add(new Label("创建时间: 2026-06-27 10:32", skin)).padTop(8).row();
                d.setContent(content);
                d.addButton("关闭", () -> d.close(), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
                d.setOnClose(() -> setStatus("Drawer 关闭"));
                d.show(stage);
            }
        });
        drawerRow.add(bDetail);

        BsButton bLeft = new BsButton("左侧抽屉", skin,
                BsButton.Variant.SECONDARY, BsButton.Style.SOLID, BsButton.Size.SM);
        bLeft.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsDrawer d = new BsDrawer(skin);
                d.setTitle("侧边菜单");
                d.setSide(BsDrawer.Side.LEFT);
                d.setDrawerWidth(280);
                Table content = new Table(skin);
                content.left().top();
                content.defaults().left().pad(4);
                for (String item : new String[]{"首页", "用户管理", "订单系统", "设置", "退出"}) {
                    content.add(new Label("• " + item, skin)).row();
                }
                d.setContent(content);
                d.setOnClose(() -> setStatus("左侧 Drawer 关闭"));
                d.show(stage);
            }
        });
        drawerRow.add(bLeft);
        c.add(drawerRow).left().padTop(4).row();

        // ===== BsAffix =====
        c.add(new Label("② BsAffix 固定钉（滚动时钉在视口顶部）:",
                skin)).padTop(14).left().row();
        // 演示：构造一个长滚动列表，标题用 Affix 包装
        Table innerContent = new Table();
        innerContent.left().top();
        innerContent.defaults().growX().left();
        // 第一个固定标题
        Table header1 = new Table(skin);
        header1.setBackground(skin.getDrawable("bs-menu-bar-bg"));
        header1.pad(8, 12, 8, 12);
        Label h1 = new Label("第一组：基础信息", skin);
        h1.setColor(new Color(0.1f, 0.1f, 0.12f, 1f));
        h1.setFontScale(1.1f);
        header1.add(h1).left();
        innerContent.add(new BsAffix(skin, header1, BsAffix.Placement.TOP)).growX().row();
        for (int i = 0; i < 12; i++) {
            innerContent.add(new Label("  - 数据项 " + (i + 1), skin)).pad(4).left().row();
        }
        // 第二个固定标题
        Table header2 = new Table(skin);
        header2.setBackground(skin.getDrawable("bs-menu-bar-bg"));
        header2.pad(8, 12, 8, 12);
        Label h2 = new Label("第二组：高级设置", skin);
        h2.setColor(new Color(0.1f, 0.1f, 0.12f, 1f));
        h2.setFontScale(1.1f);
        header2.add(h2).left();
        innerContent.add(new BsAffix(skin, header2, BsAffix.Placement.TOP)).growX().row();
        for (int i = 0; i < 15; i++) {
            innerContent.add(new Label("  - 高级项 " + (i + 1), skin)).pad(4).left().row();
        }
        BsScrollPane affixScroll = new BsScrollPane(innerContent, skin);
        affixScroll.setFadeScrollBars(false);
        c.add(affixScroll).growX().height(200).padTop(4).row();
        Label affixNote = new Label("(滚动内容时，分组标题会「钉住」在顶部，演示 Affix 行为)", skin);
        c.add(affixNote).padTop(4).row();
    }

    // ============================ Wave4: 新增组件 ============================

    /** Wave4-Pickers：Calendar / DateRangePicker / TimePicker / Cascader。 */
    private void fillWave4Pickers(Table c) {
        c.add(sectionTitle("Wave4-Pickers  —— Calendar / DateRange / Time / Cascader")).row();

        // ===== BsCalendar 单选 =====
        c.add(new Label("① BsCalendar 月历（单选，点击日期）：", skin)).padTop(8).left().row();
        BsCalendar singleCal = new BsCalendar(skin)
                .setOnSelect(d -> setStatus("Calendar 选中: " + d));
        c.add(singleCal).padTop(4).left().row();

        // ===== BsCalendar 区间 =====
        c.add(new Label("② BsCalendar 区间模式（先点起点，再点终点）：", skin)).padTop(14).left().row();
        BsCalendar rangeCal = new BsCalendar(skin, BsCalendar.Mode.RANGE)
                .setOnRange((s, e) -> setStatus(e == null
                        ? "区间起点: " + s
                        : "区间: " + s + " ~ " + e));
        c.add(rangeCal).padTop(4).left().row();

        // ===== BsDateRangePicker =====
        c.add(new Label("③ BsDateRangePicker（只读输入框，点击弹浮层选区间）：", skin)).padTop(14).left().row();
        BsDateRangePicker drp = new BsDateRangePicker(skin)
                .setOnChange((s, e) -> setStatus("DateRange: " + s + " ~ " + e));
        drp.setRange(java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now());
        c.add(drp).width(260).padTop(4).left().row();

        // ===== BsTimePicker =====
        c.add(new Label("④ BsTimePicker（只读输入框，点击弹时:分:秒面板）：", skin)).padTop(14).left().row();
        Table timeRow = new Table();
        timeRow.defaults().pad(4).left();
        BsTimePicker tpHms = new BsTimePicker(skin, true)
                .setOnChange(t -> setStatus("Time(HMS): " + t));
        tpHms.setValue(java.time.LocalTime.now());
        timeRow.add(new Label("时:分:秒", skin));
        timeRow.add(tpHms).width(140);

        BsTimePicker tpHm = new BsTimePicker(skin, false)
                .setOnChange(t -> setStatus("Time(HM): " + t));
        tpHm.setValue(java.time.LocalTime.now());
        timeRow.add(new Label("时:分", skin)).padLeft(16);
        timeRow.add(tpHm).width(100);
        c.add(timeRow).padTop(4).left().row();

        // ===== BsCascader =====
        c.add(new Label("⑤ BsCascader 级联选择（省 > 市 > 区，选到叶子回填）：", skin)).padTop(14).left().row();
        BsCascader.Option root1 = new BsCascader.Option().label("广东").value("gd")
                .child(new BsCascader.Option().label("深圳").value("sz")
                        .child(new BsCascader.Option().label("南山区").value("ns"))
                        .child(new BsCascader.Option().label("福田区").value("ft")))
                .child(new BsCascader.Option().label("广州").value("gz")
                        .child(new BsCascader.Option().label("天河区").value("th"))
                        .child(new BsCascader.Option().label("越秀区").value("yx")));
        BsCascader.Option root2 = new BsCascader.Option().label("浙江").value("zj")
                .child(new BsCascader.Option().label("杭州").value("hz")
                        .child(new BsCascader.Option().label("西湖区").value("xh")))
                .child(new BsCascader.Option().label("宁波").value("nb")
                        .child(new BsCascader.Option().label("海曙区").value("hs")));
        BsCascader cascader = new BsCascader(skin)
                .setOptions(java.util.Arrays.asList(root1, root2))
                .setOnChange(path -> {
                    StringBuilder sb = new StringBuilder("Cascader: ");
                    for (int i = 0; i < path.size(); i++) {
                        if (i > 0) sb.append(" / ");
                        sb.append(path.get(i).label);
                    }
                    setStatus(sb.toString());
                });
        c.add(cascader).width(280).padTop(4).left().row();
    }

    /** Wave4-Display：Anchor / Comment / CircularProgress / RangeSlider。 */
    private void fillWave4Display(Table c) {
        c.add(sectionTitle("Wave4-Display  —— Anchor / Comment / CircularProgress / RangeSlider")).row();

        // ===== BsAnchor 锚点导航 =====
        c.add(new Label("① BsAnchor 锚点导航（点击链接滚到目标，滚动时高亮当前节）：",
                skin)).padTop(8).left().row();
        Table anchorRow = new Table();
        // 内嵌滚动文档
        Table doc = new Table();
        doc.left().top();
        doc.defaults().growX().left();
        Label h0 = sectionTitle("概述");
        Label h1 = sectionTitle("安装");
        Label h2 = sectionTitle("用法");
        doc.add(h0).padTop(8).row();
        for (int i = 0; i < 8; i++) doc.add(new Label("  概述内容行 " + (i + 1), skin)).pad(2).row();
        doc.add(h1).padTop(8).row();
        for (int i = 0; i < 10; i++) doc.add(new Label("  安装内容行 " + (i + 1), skin)).pad(2).row();
        doc.add(h2).padTop(8).row();
        for (int i = 0; i < 12; i++) doc.add(new Label("  用法内容行 " + (i + 1), skin)).pad(2).row();
        BsScrollPane docScroll = new BsScrollPane(doc, skin);
        docScroll.setFadeScrollBars(false);
        BsAnchor anchor = new BsAnchor(skin, docScroll)
                .setOnAnchorChange(i -> setStatus("Anchor 当前节: " + i))
                .add("概述", h0)
                .add("安装", h1)
                .add("用法", h2);
        anchorRow.add(anchor).width(140).top().padRight(8);
        anchorRow.add(docScroll).grow().height(220);
        c.add(anchorRow).growX().padTop(4).row();

        // ===== BsComment 评论 / 聊天气泡 =====
        c.add(new Label("② BsComment 评论 / 聊天气泡（对方/自己/评论流）：",
                skin)).padTop(14).left().row();
        Drawable avatar = skin.newDrawable("white", BsPalette.PRIMARY.getMain());
        BsComment msgOther = new BsComment(skin)
                .avatar(avatar).name("张三").time("12:30").text("你好！今天天气不错。");
        BsComment msgSelf = new BsComment(skin)
                .self(true).avatar(avatar).name("我").text("收到，下午见 👍").maxWidth(260);
        BsComment comment = new BsComment(skin)
                .avatar(avatar).name("李四").time("昨天")
                .text("这条评论很有用，已点赞收藏。").bubble(false);
        c.add(msgOther).left().padTop(4).row();
        c.add(msgSelf).right().padTop(4).row();
        c.add(comment).left().padTop(4).row();

        // ===== BsCircularProgress 环形进度 =====
        c.add(new Label("③ BsCircularProgress 环形进度（百分比环 / 不确定加载环）：",
                skin)).padTop(14).left().row();
        Table ringRow = new Table();
        ringRow.defaults().pad(10);
        for (BsCircularProgress.Variant v : BsCircularProgress.Variant.values()) {
            BsCircularProgress ring = new BsCircularProgress(skin, v)
                    .setPercent(0.65f)
                    .setShowLabel(true);
            ring.setSize(72, 72);
            Container<BsCircularProgress> wrap = new Container<>(ring);
            wrap.size(72);
            ringRow.add(wrap);
        }
        // 不确定态加载环
        BsCircularProgress indet = new BsCircularProgress(skin, BsCircularProgress.Variant.PRIMARY)
                .setIndeterminate(true);
        indet.setSize(48, 48);
        Container<BsCircularProgress> indetWrap = new Container<>(indet);
        indetWrap.size(48);
        ringRow.add(indetWrap);
        c.add(ringRow).left().padTop(4).row();

        // ===== BsRangeSlider 双滑块区间 =====
        c.add(new Label("④ BsRangeSlider 双滑块区间（拖动两 knob 选 [low, high]）：",
                skin)).padTop(14).left().row();
        final Label[] rsLabel = { new Label("区间: 20 ~ 80", skin) };
        BsRangeSlider rs = new BsRangeSlider(0, 100, 1)
                .setRange(20, 80)
                .setMinGap(5)
                .setOnChange((lo, hi) -> rsLabel[0].setText("区间: " + (int) lo + " ~ " + (int) hi));
        rs.setSize(420, 24);
        Container<BsRangeSlider> rsWrap = new Container<>(rs);
        rsWrap.size(420, 24);
        c.add(rsWrap).padTop(4).left().row();
        c.add(rsLabel[0]).left().padTop(2).row();
    }

    /** Wave4-Form：BsFormValidator + BsRule 声明式表单校验。 */
    private void fillWave4Form(Table c) {
        c.add(sectionTitle("Wave4-Form  —— BsFormValidator + BsRule 声明式校验")).row();
        c.add(new Label("声明式规则（required/minLen/email/range/crossField）+ 异步校验，点「校验」查看结果：",
                skin)).padBottom(8).left().row();

        final BsTextField userF = new BsTextField("", skin);
        userF.setMessageText("3~16 字符");
        final BsTextField emailF = new BsTextField("", skin);
        emailF.setMessageText("邮箱");
        final BsTextField ageF = new BsTextField("", skin);
        ageF.setMessageText("18~60");
        final BsTextField pwdF = new BsTextField("", skin);
        pwdF.setPasswordMode(true);
        pwdF.setMessageText("密码");
        final BsTextField confirmF = new BsTextField("", skin);
        confirmF.setPasswordMode(true);
        confirmF.setMessageText("再输一次");

        Table form = new Table(skin);
        form.defaults().pad(4).left();
        form.add(new Label("用户名", skin)).width(70);
        form.add(userF).width(220).row();
        form.add(new Label("邮箱", skin)).width(70);
        form.add(emailF).width(220).row();
        form.add(new Label("年龄", skin)).width(70);
        form.add(ageF).width(220).row();
        form.add(new Label("密码", skin)).width(70);
        form.add(pwdF).width(220).row();
        form.add(new Label("确认", skin)).width(70);
        form.add(confirmF).width(220).row();
        c.add(form).left().row();

        // 错误展示区
        final Label errLabel = new Label("(待校验)", skin);
        errLabel.setColor(BsPalette.DANGER.getMain());
        errLabel.setWrap(true);
        c.add(errLabel).growX().padTop(6).row();

        BsFormValidator validator = new BsFormValidator()
                .addField("user", userF, BsRule.required("请输入用户名"), BsRule.minLen(3), BsRule.maxLen(16))
                .addField("email", emailF, BsRule.email())
                .addField("age", ageF, BsRule.range(18, 60))
                .addField("pwd", pwdF, BsRule.required("请输入密码"), BsRule.minLen(6))
                .addField("confirm", confirmF,
                        BsRule.crossField(ctx -> ctx.get("pwd").equals(ctx.self()) ? null : "两次密码不一致"));

        // 异步规则示例：模拟「用户名查重」——延迟 300ms 在 GL 线程返回
        validator.addAsyncRule("user", (val, onResult) -> {
            setStatus("异步查重中: " + val + " ...");
            new Thread(() -> {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                // 演示：用户名 "admin" 视为已存在
                boolean exists = "admin".equalsIgnoreCase(val == null ? "" : val.trim());
                Gdx.app.postRunnable(() -> onResult.accept(exists ? "用户名已存在" : null));
            }).start();
        });

        Table btnRow = new Table();
        btnRow.defaults().pad(4);
        BsButton bSync = new BsButton("同步校验", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.SM);
        bSync.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                java.util.Map<String, String> errs = validator.validateAll();
                errLabel.setText(errs.isEmpty() ? "✓ 同步校验通过" : "✗ " + errs.toString());
                setStatus(errs.isEmpty() ? "同步校验通过" : "同步校验失败");
            }
        });
        BsButton bAsync = new BsButton("异步校验（含查重）", skin,
                BsButton.Variant.INFO, BsButton.Style.SOLID, BsButton.Size.SM);
        bAsync.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                errLabel.setText("异步校验中...");
                validator.validateAsync(errs -> {
                    errLabel.setText(errs.isEmpty() ? "✓ 全部通过（含异步）" : "✗ " + errs.toString());
                    setStatus(errs.isEmpty() ? "异步校验通过" : "异步校验失败");
                });
            }
        });
        btnRow.add(bSync);
        btnRow.add(bAsync);
        c.add(btnRow).left().padTop(6).row();
    }

    /** Wave4-Data：DnD / VirtualList / DataGrid。 */
    private void fillWave4Data(Table c) {
        c.add(sectionTitle("Wave4-Data  —— DnD / VirtualList / DataGrid")).row();

        // ===== BsDnd 拖放 =====
        c.add(new Label("① BsDnd 拖放（把左边卡片拖到右边回收站）：", skin)).padTop(8).left().row();
        Table dndRow = new Table();
        dndRow.defaults().pad(6);
        // 卡片源：用 Table 包 Label 才能设背景
        final Table card = new Table();
        card.setBackground(skin.getDrawable("bs-window-bg"));
        Label cardLbl = new Label("卡片 #42", skin);
        cardLbl.setColor(BsPalette.PRIMARY.getMain());
        card.add(cardLbl).pad(6);
        // 回收站目标
        final Table bin = new Table();
        bin.setBackground(skin.getDrawable("bs-window-bg"));
        final Label binLbl = new Label("🗑 回收站", skin);
        binLbl.setColor(BsPalette.DANGER.getMain());
        bin.add(binLbl).pad(6);

        Container<Table> cardWrap = new Container<>(card);
        cardWrap.size(120, 50);
        Container<Table> binWrap = new Container<>(bin);
        binWrap.size(160, 60);

        BsDnd dnd = new BsDnd();
        dnd.source(card)
                .payload("卡片 #42")
                .onDropped((payload, overTarget) -> setStatus(overTarget != null
                        ? "已拖到回收站: " + payload
                        : "拖放取消（未落在目标上）"));
        dnd.target(bin)
                .setAccept(o -> true)
                .onDrop((payload, sourceActor) -> {
                    setStatus("回收站接收: " + payload);
                    binLbl.setText("🗑 已回收: " + payload);
                });
        dndRow.add(cardWrap);
        dndRow.add(new Label("  →  ", skin));
        dndRow.add(binWrap);
        c.add(dndRow).left().padTop(4).row();

        // ===== BsVirtualList 虚拟化长列表 =====
        c.add(new Label("② BsVirtualList 虚拟化长列表（1 万条数据，仅渲染可见 cell）：",
                skin)).padTop(14).left().row();
        java.util.List<String> huge = new java.util.ArrayList<>();
        for (int i = 0; i < 10000; i++) huge.add("数据项 #" + (i + 1));
        BsVirtualList<String> vlist = new BsVirtualList<>(skin, (existing, item, idx) -> {
            Table row;
            if (existing instanceof Table) {
                row = (Table) existing;
                row.clearChildren();
            } else {
                row = new Table();
            }
            // 斑马纹（bs-bg-hover 是 Color 资源，转 drawable）
            if (idx % 2 == 1) {
                row.setBackground(BsSkinFactory.drawableOf(skin.get("bs-bg-hover", Color.class)));
            } else {
                row.setBackground((Drawable) null);
            }
            Label l = new Label(item, skin);
            row.add(l).left().growX().padLeft(6);
            return row;
        }, 26f);
        vlist.setItems(huge)
                .setOnClick((idx, item) -> setStatus("VirtualList 点 " + idx + ": " + item));
        c.add(vlist).growX().height(220).padTop(4).row();

        // ===== BsDataGrid 虚拟化数据表格 =====
        c.add(new Label("③ BsDataGrid 虚拟化数据表格（固定表头 + 大数据量）：",
                skin)).padTop(14).left().row();
        java.util.List<BsDataGridDemoRow> rows = new java.util.ArrayList<>();
        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十"};
        for (int i = 0; i < 500; i++) {
            String n = names[i % names.length];
            rows.add(new BsDataGridDemoRow(i + 1, n, 18 + (i % 40),
                    n.toLowerCase() + (i + 1) + "@example.com"));
        }
        BsDataGrid<BsDataGridDemoRow> grid = new BsDataGrid<>(skin);
        grid.addColumn("ID", r -> String.valueOf(r.id), 60)
                .addColumn("姓名", r -> r.name, 100)
                .addColumn("年龄", r -> String.valueOf(r.age), 70)
                .addColumn("邮箱", r -> r.email, 240)
                .setItems(rows)
                .setOnRowClick((idx, r) -> setStatus("DataGrid 点行 " + idx + ": " + r.name));
        c.add(grid).growX().height(240).padTop(4).row();
    }

    /** BsDataGrid demo 行数据。 */
    private static final class BsDataGridDemoRow {
        final int id;
        final String name;
        final int age;
        final String email;
        BsDataGridDemoRow(int id, String name, int age, String email) {
            this.id = id; this.name = name; this.age = age; this.email = email;
        }
    }

    /** 演示：用 Drawer 装一个用户编辑表单。 */
    private void showEditUserDrawer() {
        BsDrawer d = new BsDrawer(skin);
        d.setTitle("编辑用户");
        d.setDrawerWidth(420);

        BsPropertySheet form = new BsPropertySheet(skin);
        form.setLabelWidth(80);
        form.addSection("基本信息");
        form.addProperty("name", "张三", BsPropertySheet.Type.TEXT);
        form.addProperty("age", 28, BsPropertySheet.Type.NUMBER);
        form.addProperty("role", "Admin", BsPropertySheet.Type.SELECT, "Admin", "Editor", "Viewer");
        form.addProperty("enabled", true, BsPropertySheet.Type.BOOLEAN);
        form.addSection("联系");
        form.addProperty("email", "zhangsan@example.com", BsPropertySheet.Type.TEXT);
        form.addProperty("phone", "13800138000", BsPropertySheet.Type.TEXT);

        d.setContent(form);
        d.addButton("取消", () -> {
            d.close();
            setStatus("编辑取消");
        }, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        d.addButton("保存", () -> {
            setStatus("保存用户: " + form.collectValues());
            d.close();
        }, BsButton.Variant.PRIMARY);
        d.setOnClose(() -> setStatus("Drawer 关闭"));
        d.show(stage);
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
