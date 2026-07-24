package com.git.bs.demo.modules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.git.bs.ui.BsAffix;
import com.git.bs.ui.BsAnchor;
import com.git.bs.ui.BsAutoComplete;
import com.git.bs.ui.BsAvatar;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsCalendar;
import com.git.bs.ui.BsCascader;
import com.git.bs.ui.BsCircularProgress;
import com.git.bs.ui.BsComment;
import com.git.bs.ui.BsDateRangePicker;
import com.git.bs.ui.BsDescriptionList;
import com.git.bs.ui.BsDrawer;
import com.git.bs.ui.BsEmpty;
import com.git.bs.ui.BsFigure;
import com.git.bs.ui.BsFileItem;
import com.git.bs.ui.BsFloatingLabel;
import com.git.bs.ui.BsIcon;
import com.git.bs.ui.BsInspectorPanel;
import com.git.bs.ui.BsListGroup;
import com.git.bs.ui.BsLoadingOverlay;
import com.git.bs.ui.BsMiniMap;
import com.git.bs.ui.BsModal;
import com.git.bs.ui.BsNodePalette;
import com.git.bs.ui.BsPalette;
import com.git.bs.ui.BsPlaceholder;
import com.git.bs.ui.BsPropertySheet;
import com.git.bs.ui.BsRangeSlider;
import com.git.bs.ui.BsRating;
import com.git.bs.ui.BsResult;
import com.git.bs.ui.BsSearchBar;
import com.git.bs.ui.BsSkinFactory;
import com.git.bs.ui.BsStatistic;
import com.git.bs.ui.BsStatusBar;
import com.git.bs.ui.BsSteps;
import com.git.bs.ui.BsSwitch;
import com.git.bs.ui.BsTagInput;
import com.git.bs.ui.BsTimePicker;
import com.git.bs.ui.BsTimeline;
import com.git.bs.ui.BsToolbar;
import com.git.bs.ui.BsTransfer;
import com.git.bs.ui.BsCarousel;
import com.git.bs.ui.BsScrollPane;
import com.git.bs.ui.BsTextField;
import com.git.bs.ui.ext.BsDataGrid;
import com.git.bs.ui.ext.BsDnd;
import com.git.bs.ui.ext.BsFormValidator;
import com.git.bs.ui.ext.BsRule;
import com.git.bs.ui.ext.BsVirtualList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.git.bs.demo.modules.ModuleSupport.*;

/**
 * Wave 系列扩展组件模块组：P2Content / P2Carousel / Wave1-4 各项。
 *
 * <p>{@code dataTable} 持有 Wave2-Data 的表格状态。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsWaveModules {

    private final Skin skin;
    private final Stage stage;
    private final Consumer<String> setStatus;

    private BsDataTableRef dataTable;  // Wave2-Data 模块状态

    public BsWaveModules(Skin skin, Stage stage, Consumer<String> setStatus) {
        this.skin = skin;
        this.stage = stage;
        this.setStatus = setStatus;
    }

    // ============================ P2-Content ============================
    public void fillP2Content(Table c) {
        c.add(sectionTitle(skin, "P2-Content  —— 骨架屏/图文/列表组/浮动标签")).row();

        // Placeholder
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

        // Figure
        c.add(new Label("③ BsFigure 图文（图片 + 图注）:", skin)).padTop(14).left().row();
        try {
            Drawable img1 = BsModal.drawableFromPath("bs/test/img/20251110013443.png");
            BsFigure fig = new BsFigure(skin)
                    .image(img1)
                    .imageSize(320, 180)
                    .caption("图 1：这是产品发布会的现场照片，演示 Bs UI 框架的应用场景");
            c.add(fig).width(340).growX().padTop(4).row();
        } catch (Throwable t) {
            c.add(new Label("(图片加载失败: " + t.getMessage() + ")", skin)).row();
        }

        // ListGroup
        c.add(new Label("④ BsListGroup 列表组（图标/副标题/badge/禁用/选中态）:",
                skin)).padTop(14).left().row();
        BsListGroup group = new BsListGroup(skin);
        group.setItemHeight(48);
        Drawable envelope = BsIcon.get("envelope");
        Drawable gear = BsIcon.get("gear");
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
        group.setOnSelect(idx -> setStatus.accept("ListGroup 选中: " + idx));
        c.add(group).width(420).growX().padTop(4).row();

        // FloatingLabel
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

    // ============================ P2-Carousel ============================
    public void fillP2Carousel(Table c) {
        c.add(sectionTitle(skin, "P2-Carousel  —— 轮播图（自动播放 + 左右箭头 + 指示点）")).row();

        c.add(new Label("① 自动播放 3 秒切换（点击左右箭头或底部圆点手动切换）:",
                skin)).padTop(8).left().row();
        BsCarousel carousel = new BsCarousel(skin);
        carousel.setSize(640, 280);
        carousel.setAutoPlay(true);
        carousel.setInterval(3f);
        carousel.addSlide(makeColorSlide(skin, "第一张 Banner", "新品上线",
                new Color(0x0D / 255f, 0x6E / 255f, 0xFD / 255f, 1f)));
        carousel.addSlide(makeColorSlide(skin, "第二张 Banner", "限时折扣",
                new Color(0xDC / 255f, 0x35 / 255f, 0x45 / 255f, 1f)));
        carousel.addSlide(makeColorSlide(skin, "第三张 Banner", "会员专享",
                new Color(0x19 / 255f, 0x87 / 255f, 0x54 / 255f, 1f)));
        carousel.addSlide(makeColorSlide(skin, "第四张 Banner", "马上抢购",
                new Color(0xFF / 255f, 0xC1 / 255f, 0x07 / 255f, 1f)));
        c.add(wrapFill(carousel, 640, 280)).padTop(4).row();

        c.add(new Label("② 5 秒切换（慢速 + 实际图片）:", skin)).padTop(14).left().row();
        BsCarousel imgCarousel = new BsCarousel(skin);
        imgCarousel.setSize(640, 280);
        imgCarousel.setAutoPlay(true);
        imgCarousel.setInterval(5f);
        try {
            Drawable d1 = BsModal.drawableFromPath("bs/test/img/20251110013443.png");
            Drawable d2 = BsModal.drawableFromPath("bs/test/img/20251109230728.png");
            imgCarousel.addSlide(makeImageSlide(skin, d1, "图片轮播 1"));
            imgCarousel.addSlide(makeImageSlide(skin, d2, "图片轮播 2"));
        } catch (Throwable t) {
            imgCarousel.addSlide(makeColorSlide(skin, "图片加载失败", "占位",
                    Color.GRAY));
        }
        c.add(wrapFill(imgCarousel, 640, 280)).padTop(4).row();

        c.add(new Label("③ 不自动播放（手动控制）:", skin)).padTop(14).left().row();
        BsCarousel manual = new BsCarousel(skin);
        manual.setSize(640, 200);
        manual.setAutoPlay(false);
        manual.addSlide(makeColorSlide(skin, "A", "纯手动控制", new Color(0x6C / 255f, 0x75 / 255f, 0x7D / 255f, 1f)));
        manual.addSlide(makeColorSlide(skin, "B", "需要点箭头切换", new Color(0x0D / 255f, 0xCA / 255f, 0xF0 / 255f, 1f)));
        c.add(wrapFill(manual, 640, 200)).padTop(4).row();

        c.add(new Label("(底部圆点 = 指示器，可点击跳转；左右 ‹ › = 切换箭头)", skin)).padTop(8).row();
    }

    // ============================ Wave1-Basics ============================
    public void fillWave1Basics(Table c) {
        c.add(sectionTitle(skin, "Wave1-Basics  —— Switch / Avatar / Timeline / Statistic / Steps / Empty / Rating")).row();

        // Switch
        c.add(new Label("① BsSwitch 开关（SM/MD/LG，禁用态）:", skin)).padTop(8).left().row();
        Table switchRow = new Table();
        switchRow.defaults().pad(10).left();
        BsSwitch sw1 = new BsSwitch(skin, BsSwitch.Size.SM);
        sw1.setLabel("通知");
        sw1.setChecked(true);
        sw1.setOnChange(v -> setStatus.accept("Switch SM: " + v));
        switchRow.add(sw1);

        BsSwitch sw2 = new BsSwitch(skin, BsSwitch.Size.MD);
        sw2.setLabel("深色模式");
        sw2.setOnChange(v -> setStatus.accept("Switch MD: " + v));
        switchRow.add(sw2);

        BsSwitch sw3 = new BsSwitch(skin, BsSwitch.Size.LG);
        sw3.setLabel("自动更新");
        sw3.setChecked(true);
        sw3.setOnChange(v -> setStatus.accept("Switch LG: " + v));
        switchRow.add(sw3);

        BsSwitch sw4 = new BsSwitch(skin);
        sw4.setLabel("禁用");
        sw4.setChecked(true);
        sw4.setDisabled(true);
        switchRow.add(sw4);
        c.add(switchRow).left().row();

        // Avatar
        c.add(new Label("② BsAvatar 头像（圆/方 shape + 在线状态）:", skin)).padTop(14).left().row();
        Drawable avatarImg = null;
        try {
            avatarImg = BsModal.drawableFromPath("bs/test/img/20251121200555.png");
        } catch (Throwable ignored) {}
        Table avatarRow = new Table();
        avatarRow.defaults().pad(10);
        BsAvatar av1 = new BsAvatar(skin).image(avatarImg).size(56).shape(BsAvatar.Shape.CIRCLE).online(true);
        avatarRow.add(wrapAvatar(skin, av1, "CIRCLE/online"));
        BsAvatar av2 = new BsAvatar(skin).image(avatarImg).size(56).shape(BsAvatar.Shape.CIRCLE).online(false);
        avatarRow.add(wrapAvatar(skin, av2, "CIRCLE/offline"));
        BsAvatar av3 = new BsAvatar(skin).image(avatarImg).size(56).shape(BsAvatar.Shape.ROUNDED);
        avatarRow.add(wrapAvatar(skin, av3, "ROUNDED"));
        BsAvatar av4 = new BsAvatar(skin).image(avatarImg).size(32).shape(BsAvatar.Shape.CIRCLE);
        avatarRow.add(wrapAvatar(skin, av4, "small 32"));
        c.add(avatarRow).left().row();

        // Timeline
        c.add(new Label("③ BsTimeline 时间轴（任务进度，6 色）:", skin)).padTop(14).left().row();
        BsTimeline tl = new BsTimeline(skin);
        tl.addItem("09:00", "创建了任务", BsTimeline.Color.PRIMARY);
        tl.addItem("10:30", "分配给张三", BsTimeline.Color.INFO);
        tl.addItem("14:00", "开始处理", BsTimeline.Color.WARNING);
        tl.addItem("15:30", "遇到问题需要 review", BsTimeline.Color.DANGER);
        tl.addItem("16:00", "问题已解决", BsTimeline.Color.SUCCESS);
        tl.setOnClick(item -> setStatus.accept("Timeline: " + item.getTitle()));
        c.add(tl).width(500).growX().padTop(4).row();

        // Statistic
        c.add(new Label("④ BsStatistic 数字统计卡（4 卡片，趋势 ↑↓）:", skin)).padTop(14).left().row();
        Table statRow = new Table();
        statRow.defaults().pad(8);
        statRow.add(new BsStatistic(skin).title("今日营收").value("¥12,345").trend(12.5f)).width(200);
        statRow.add(new BsStatistic(skin).title("活跃用户").value("8,920").trend(5.3f)).width(200);
        statRow.add(new BsStatistic(skin).title("订单量").value("432").trend(-2.8f)).width(200);
        statRow.add(new BsStatistic(skin).title("转化率").value("3.2%").trend(0f)).width(200);
        statRow.row();
        c.add(statRow).left().row();

        // Steps
        c.add(new Label("⑤ BsSteps 步骤条（默认色：DONE 绿+线 / CURRENT 蓝带 ring / WAIT 空心灰）:",
                skin)).padTop(14).left().row();
        BsSteps steps = new BsSteps(skin);
        steps.addSteps("填写资料", "验证邮箱", "设置密码", "完成");
        steps.setCurrent(1);
        steps.setOnStepClick(idx -> setStatus.accept("Steps: 切到第 " + (idx + 1) + " 步"));
        c.add(steps).growX().padTop(4).row();

        c.add(new Label("⑥ BsSteps 自定义颜色（DONE=紫 / CURRENT=橙 / WAIT=灰，线粗 4px）:",
                skin)).padTop(10).left().row();
        BsSteps customSteps = new BsSteps(skin);
        customSteps.addSteps("Step 1", "Step 2", "Step 3", "Step 4", "Step 5");
        customSteps.setCurrent(2);
        customSteps.setDoneColor(new Color(0x6F / 255f, 0x42 / 255f, 0xC1 / 255f, 1f));
        customSteps.setCurrentColor(new Color(0xFD / 255f, 0x7E / 255f, 0x14 / 255f, 1f));
        customSteps.setWaitColor(new Color(0x6C / 255f, 0x75 / 255f, 0x7D / 255f, 1f));
        customSteps.setLineHeight(4);
        customSteps.setLineLength(50);
        customSteps.setOnStepClick(idx -> setStatus.accept("CustomSteps: 第 " + (idx + 1) + " 步"));
        c.add(customSteps).growX().padTop(4).row();

        c.add(new Label("⑦ BsSteps 第 1 步（看不到已完成线）/ 最后一步（全部完成线）:",
                skin)).padTop(10).left().row();
        BsSteps firstStep = new BsSteps(skin);
        firstStep.addSteps("开始", "处理中", "完成");
        firstStep.setCurrent(0);
        firstStep.setOnStepClick(idx -> setStatus.accept("firstStep: " + idx));
        c.add(firstStep).growX().padTop(4).row();

        BsSteps allDone = new BsSteps(skin);
        allDone.addSteps("开始", "处理中", "完成");
        allDone.setCurrent(2);
        allDone.setOnStepClick(idx -> setStatus.accept("allDone: " + idx));
        c.add(allDone).growX().padTop(4).row();

        // Empty
        c.add(new Label("⑥ BsEmpty 空状态（无数据占位）:", skin)).padTop(14).left().row();
        BsEmpty empty = new BsEmpty(skin)
                .title("暂无消息")
                .description("您还没有收到任何消息，点击按钮刷新")
                .actionButton("刷新", () -> setStatus.accept("点了刷新"));
        c.add(empty).growX().padTop(4).row();

        // Rating
        c.add(new Label("⑦ BsRating 星级评分（默认/半星/只读）:", skin)).padTop(14).left().row();
        Table ratingRow = new Table();
        ratingRow.defaults().pad(10).left();
        BsRating r1 = new BsRating(skin);
        r1.setValue(3);
        r1.setOnChange(v -> setStatus.accept("Rating 1: " + v));
        ratingRow.add(r1);
        BsRating r2 = new BsRating(skin);
        r2.setHalfStars(true);
        r2.setValue(3.5f);
        r2.setOnChange(v -> setStatus.accept("Rating 2 (半星): " + v));
        ratingRow.add(r2);
        BsRating r3 = new BsRating(skin);
        r3.setValue(4);
        r3.setReadOnly(true);
        ratingRow.add(r3);
        c.add(ratingRow).left().row();
    }

    // ============================ Wave1-Inputs ============================
    public void fillWave1Inputs(Table c) {
        c.add(sectionTitle(skin, "Wave1-Inputs  —— AutoComplete / TagInput / DescriptionList")).row();

        c.add(new Label("① BsAutoComplete 自动补全（输入 a/b/c 等查看建议）:",
                skin)).padTop(8).left().row();
        BsAutoComplete ac = new BsAutoComplete(skin);
        ac.setPopupWidth(280);
        ac.setCandidates(Arrays.asList(
                "Apple", "Banana", "Cherry", "Grape", "Orange",
                "Peach", "Pear", "Pineapple", "Strawberry", "Watermelon",
                "Avocado", "Blueberry", "Coconut", "Dragonfruit"
        ));
        ac.setOnSelect(text -> setStatus.accept("AutoComplete 选了: " + text));
        c.add(ac).padTop(4).row();

        c.add(new Label("② BsTagInput 标签输入（回车变 chip，× 删除）:", skin)).padTop(14).left().row();
        BsTagInput tags = new BsTagInput(skin);
        tags.addTags(Arrays.asList("Java", "libgdx", "UI"));
        tags.setPlaceholder("输入标签后回车");
        tags.setOnChange(list -> setStatus.accept("TagInput: " + list));
        c.add(tags).width(420).growX().padTop(4).row();

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

        c.add(new Label("(输入框点击外部时 AutoComplete popup 自动关闭)", skin)).padTop(8).row();
    }

    // ============================ Wave1-Feedback ============================
    public void fillWave1Feedback(Table c) {
        c.add(sectionTitle(skin, "Wave1-Feedback  —— Result / LoadingOverlay")).row();

        c.add(new Label("① BsResult 结果页（4 种类型，可加按钮）:", skin)).padTop(8).left().row();
        Table resultRow = new Table();
        resultRow.defaults().pad(8);

        BsResult r1 = new BsResult(skin, BsResult.Type.SUCCESS)
                .title("提交成功")
                .description("您的申请已成功提交")
                .primaryButton("返回", () -> setStatus.accept("成功-返回"));
        resultRow.add(r1).size(280, 220);

        BsResult r2 = new BsResult(skin, BsResult.Type.WARNING)
                .title("请注意")
                .description("此操作可能会影响其他用户")
                .primaryButton("继续", () -> setStatus.accept("警告-继续"))
                .secondaryButton("取消", () -> setStatus.accept("警告-取消"));
        resultRow.add(r2).size(280, 220);
        c.add(resultRow).left().row();

        Table resultRow2 = new Table();
        resultRow2.defaults().pad(8);
        BsResult r3 = new BsResult(skin, BsResult.Type.ERROR)
                .title("提交失败")
                .description("网络异常，请稍后重试")
                .primaryButton("重试", () -> setStatus.accept("失败-重试"));
        resultRow2.add(r3).size(280, 220);

        BsResult r4 = new BsResult(skin, BsResult.Type.INFO)
                .title("信息提示")
                .description("系统将于今晚 22:00 维护")
                .secondaryButton("知道了", () -> setStatus.accept("信息-知道了"));
        resultRow2.add(r4).size(280, 220);
        c.add(resultRow2).left().padTop(8).row();

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
        c.add(new Label("(LoadingOverlay 模态遮罩，期间拦截所有点击操作)", skin)).padTop(8).row();
    }

    // ============================ Wave2-Data ============================
    public void fillWave2Data(Table c) {
        c.add(sectionTitle(skin, "Wave2-Data  —— DataTable 增强表格 / PropertySheet 属性编辑器")).row();

        c.add(new Label("① BsDataTable 增强表格（分页 + 排序 + 单选 + 空状态）:",
                skin)).padTop(8).left().row();
        dataTable = new BsDataTableRef();
        dataTable.table = new com.git.bs.ui.BsDataTable(skin);
        dataTable.table.setHeaders("ID", "姓名", "年龄", "部门", "状态");
        dataTable.table.setPageSize(8);
        dataTable.table.setSortable(true);
        dataTable.table.setMultiSelect(false);

        List<List<String>> rows = new ArrayList<>();
        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十",
                "郑十一", "王十二", "冯十三", "陈十四", "褚十五", "卫十六", "蒋十七"};
        String[] depts = {"研发", "销售", "运营", "市场", "财务"};
        String[] statuses = {"在职", "休假", "离职"};
        for (int i = 0; i < 25; i++) {
            rows.add(Arrays.asList(
                    String.valueOf(1001 + i),
                    names[i % names.length],
                    String.valueOf(22 + (i * 3) % 40),
                    depts[i % depts.length],
                    statuses[i % statuses.length]
            ));
        }
        dataTable.rows = rows;
        dataTable.table.setData(rows);
        dataTable.table.setOnRowSelect(idx -> setStatus.accept("DataTable 选中行: " + dataTable.table.getRow(idx)));
        dataTable.table.setOnSort((col, asc) -> setStatus.accept("DataTable 排序: 列" + col + (asc ? " ↑" : " ↓")));
        c.add(dataTable.table).growX().padTop(4).row();

        Table dtCtrl = new Table();
        dtCtrl.defaults().pad(4);
        BsButton bReload = new BsButton("重新加载 25 行", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bReload.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dataTable.table.setData(dataTable.rows);
                setStatus.accept("DataTable 重新加载");
            }
        });
        dtCtrl.add(bReload);

        BsButton bEmpty = new BsButton("切换为空", skin,
                BsButton.Variant.WARNING, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bEmpty.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dataTable.table.setData(java.util.Collections.emptyList());
                setStatus.accept("DataTable 切换为空（演示 BsEmpty）");
            }
        });
        dtCtrl.add(bEmpty);

        BsButton bSelectFirst = new BsButton("选中第 1 行", skin,
                BsButton.Variant.INFO, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bSelectFirst.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                dataTable.table.setSelected(0, true);
                setStatus.accept("DataTable 选中第 0 行");
            }
        });
        dtCtrl.add(bSelectFirst);

        BsButton bShowSel = new BsButton("显示选中", skin,
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bShowSel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus.accept("DataTable 选中: " + dataTable.table.getSelectedRows());
            }
        });
        dtCtrl.add(bShowSel);
        c.add(dtCtrl).left().padTop(4).row();
        c.add(new Label("(点表头排序，可数字/字符串自动识别；切换页保持选择)", skin)).padTop(4).row();

        // PropertySheet
        c.add(new Label("② BsPropertySheet 属性编辑器（5 种类型 + 分组）:",
                skin)).padTop(14).left().row();
        BsPropertySheet sheet = new BsPropertySheet(skin);
        sheet.setLabelWidth(110);
        sheet.setValueWidth(200);
        sheet.setOnChange((key, value) -> setStatus.accept("PropertySheet: " + key + " → " + value));

        sheet.addSection("基本信息");
        sheet.addProperty("name", "John Doe", BsPropertySheet.Type.TEXT);
        sheet.addProperty("age", 28, BsPropertySheet.Type.NUMBER);
        sheet.addProperty("role", "Admin", BsPropertySheet.Type.SELECT, "Admin", "Editor", "Viewer");
        sheet.addProperty("enabled", true, BsPropertySheet.Type.BOOLEAN);

        sheet.addSection("外观");
        sheet.addProperty("accent", Color.valueOf("#0D6EFD"), BsPropertySheet.Type.COLOR);
        sheet.addProperty("bg", Color.valueOf("#F8F9FA"), BsPropertySheet.Type.COLOR);
        sheet.addProperty("theme", "light", BsPropertySheet.Type.SELECT, "light", "dark", "auto");

        sheet.addSection("只读属性");
        sheet.addProperty("id", 10086, BsPropertySheet.Type.READONLY);
        sheet.addProperty("createdAt", "2026-06-27", BsPropertySheet.Type.READONLY);

        c.add(sheet).width(440).growX().padTop(4).row();
        c.add(new Label("(修改任意属性 → 状态栏显示 key→value 变更)", skin)).padTop(4).row();

        // DataTable LABEL 模式 单选
        c.add(new Label("③ BsDataTable LABEL 模式（纯 Label + 左侧勾选列单选，行点击不再切选中）:",
                skin)).padTop(14).left().row();
        com.git.bs.ui.BsDataTable dt2 = new com.git.bs.ui.BsDataTable(skin);
        dt2.setHeaders("ID", "姓名", "部门", "状态");
        dt2.setPageSize(6);
        dt2.setLabelModeWithCheckColumn(false);
        List<List<String>> dt2Rows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            dt2Rows.add(Arrays.asList(
                    String.valueOf(2001 + i),
                    names[i % names.length],
                    depts[i % depts.length],
                    statuses[i % statuses.length]
            ));
        }
        dt2.setData(dt2Rows);
        dt2.setOnRowSelect(idx -> setStatus.accept("LABEL 表格勾选: " + dt2.getRow(idx)));
        c.add(dt2).growX().padTop(4).row();

        // DataTable LABEL 模式 多选
        c.add(new Label("④ BsDataTable LABEL 模式（多选，可勾选多行批量操作）:",
                skin)).padTop(14).left().row();
        com.git.bs.ui.BsDataTable dt3 = new com.git.bs.ui.BsDataTable(skin);
        dt3.setHeaders("ID", "姓名", "部门", "状态");
        dt3.setPageSize(6);
        dt3.setLabelModeWithCheckColumn(true);
        dt3.setData(dt2Rows);
        dt3.setOnRowSelect(idx -> setStatus.accept("多选表格当前已选: " + dt3.getSelectedIndices()));
        c.add(dt3).growX().padTop(4).row();
        c.add(new Label("(③ 单选：勾选列点击切换 / 行点击触发回调；④ 多选：可勾多行)",
                skin)).padTop(4).row();
    }

    /** 内部持有 DataTable 与原始数据，供控制按钮重新加载。 */
    private static final class BsDataTableRef {
        com.git.bs.ui.BsDataTable table;
        List<List<String>> rows;
    }

    // ============================ Wave3-Editor ============================
    public void fillWave3Editor(Table c) {
        c.add(sectionTitle(skin, "Wave3-Editor  —— BsStatusBar 底部状态栏")).row();

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
            setStatus.accept("语言切换: " + langs[langIdx[0]]);
        });
        bar.setOnRightClick("zoom", () -> setStatus.accept("点击 zoom（业务方打开缩放对话框）"));
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

        c.add(new Label("(右侧段支持点击回调：第①栏的 lang 段可点击切换语言)", skin)).padTop(8).row();
    }

    // ============================ Wave2-Business ============================
    public void fillWave2Business(Table c) {
        c.add(sectionTitle(skin, "Wave2-Business  —— SearchBar / Toolbar / FileItem / Transfer")).row();

        // SearchBar
        c.add(new Label("① BsSearchBar 搜索栏（带过滤下拉 + 清除按钮）:",
                skin)).padTop(8).left().row();
        BsSearchBar sb1 = new BsSearchBar(skin);
        sb1.setPlaceholder("输入用户名或邮箱...");
        sb1.addFilter("全部", "姓名", "邮箱", "手机");
        sb1.setOnSearch(text -> setStatus.accept("搜索: [" + sb1.getFilter() + "] " + text));
        sb1.setOnFilterChange(idx -> setStatus.accept("过滤器: " + idx));
        c.add(sb1).padTop(4).row();

        c.add(new Label("② BsSearchBar 简版（无过滤器）:", skin)).padTop(10).left().row();
        BsSearchBar sb2 = new BsSearchBar(skin, false);
        sb2.setPlaceholder("直接搜索...");
        sb2.setOnSearch(text -> setStatus.accept("简版搜索: " + text));
        c.add(sb2).padTop(4).row();

        // Toolbar
        c.add(new Label("③ BsToolbar 工具栏（文字按钮 + 分隔线 + 图标按钮 + 下拉菜单）:",
                skin)).padTop(14).left().row();
        BsToolbar tb = new BsToolbar(skin);
        tb.addButton("新建", () -> setStatus.accept("新建"), BsButton.Variant.PRIMARY);
        tb.addButton("打开", () -> setStatus.accept("打开"));
        tb.addButton("保存", () -> setStatus.accept("保存"));
        tb.addSeparator();
        Drawable trash = BsIcon.get("trash");
        if (trash != null) {
            tb.addIconButton(trash, () -> setStatus.accept("删除"), BsButton.Variant.DANGER);
        } else {
            tb.addButton("删除", () -> setStatus.accept("删除"), BsButton.Variant.DANGER);
        }
        tb.addSeparator();
        tb.addButtonWithMenu("导出", menu -> {
            menu.addItem("PDF", () -> setStatus.accept("导出 PDF"));
            menu.addItem("PNG", () -> setStatus.accept("导出 PNG"));
            menu.addSeparator();
            menu.addItem("JSON", () -> setStatus.accept("导出 JSON"));
        });
        tb.addSpring();
        tb.addButton("设置", () -> setStatus.accept("设置"), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        c.add(tb).growX().padTop(4).row();

        // FileItem
        c.add(new Label("④ BsFileItem 文件项（图标+名称+大小+操作）:",
                skin)).padTop(14).left().row();
        BsFileItem f1 = new BsFileItem(skin)
                .name("screenshot.png")
                .size(145_678)
                .actionButton("删除", () -> setStatus.accept("删除 screenshot.png"), BsButton.Variant.DANGER);
        c.add(f1).growX().padTop(4).row();
        BsFileItem f2 = new BsFileItem(skin)
                .name("report.pdf")
                .size(2_412_350)
                .actionButton("下载", () -> setStatus.accept("下载 report.pdf"), BsButton.Variant.PRIMARY)
                .actionButton("分享", () -> setStatus.accept("分享"), BsButton.Variant.SECONDARY);
        c.add(f2).growX().padTop(2).row();
        BsFileItem f3 = new BsFileItem(skin)
                .name("data.db")
                .sizeText("1.4 GB")
                .actionButton("打开", () -> setStatus.accept("打开 db"), BsButton.Variant.INFO);
        c.add(f3).growX().padTop(2).row();
        BsFileItem f4 = new BsFileItem(skin).name("empty.log").size(0);
        c.add(f4).growX().padTop(2).row();

        // Transfer
        c.add(new Label("⑤ BsTransfer 穿梭框（左右双列权限分配）:",
                skin)).padTop(14).left().row();
        BsTransfer transfer = new BsTransfer(skin);
        transfer.setOptions("read", "write", "delete", "admin", "audit", "export", "import");
        transfer.setSelected(Arrays.asList("read", "write"));
        transfer.setOnChange(sel -> setStatus.accept("Transfer 当前已选: " + sel));
        c.add(transfer).padTop(4).row();
        c.add(new Label("(勾选左侧项 → 点 → 移到右侧；右侧点 ← 移回)", skin)).padTop(4).row();
    }

    // ============================ Wave3-EditorPro ============================
    public void fillWave3EditorPro(Table c) {
        c.add(sectionTitle(skin, "Wave3-EditorPro  —— Inspector / NodePalette / MiniMap")).row();

        // Inspector
        c.add(new Label("① BsInspectorPanel 检视面板（带标题栏 + 类型徽章 + × 关闭）:",
                skin)).padTop(8).left().row();
        BsInspectorPanel insp = new BsInspectorPanel(skin);
        insp.setTarget("Player_01", "GameObject", BsIcon.get("person"));
        insp.sheet().setOnChange((key, value) -> setStatus.accept("Inspector: " + key + " → " + value));
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
        insp.sheet().addProperty("accent", Color.valueOf("#0D6EFD"), BsPropertySheet.Type.COLOR);
        insp.setOnClose(() -> setStatus.accept("Inspector 关闭"));
        c.add(insp).width(440).growX().padTop(4).row();

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

        // NodePalette
        c.add(new Label("② BsNodePalette 节点选择面板（分类 + 搜索）:",
                skin)).padTop(14).left().row();
        BsNodePalette palette = new BsNodePalette(skin);
        palette.addCategory("流程控制", cat -> cat
                .node("Start").node("Branch").node("Loop").node("Wait"));
        palette.addCategory("对话", cat -> cat
                .node("Say").node("Choice").node("Narration"));
        palette.addCategory("事件", cat -> cat
                .node("OnStart").node("OnClick").node("OnEnter").node("OnExit"));
        palette.addCategory("变量", cat -> cat
                .node("Set").node("Get").node("Compare"));
        palette.setOnNodeClick((cat, name) -> setStatus.accept("点击节点: " + cat + " → " + name));
        c.add(palette).width(360).height(280).padTop(4).row();

        // MiniMap
        c.add(new Label("③ BsMiniMap 小地图（节点画布缩略图，点击跳转）:",
                skin)).padTop(14).left().row();
        BsMiniMap mm = new BsMiniMap();
        mm.setSkin(skin);
        mm.setSize(360, 240);
        mm.setCanvasBounds(0, 0, 2000, 1400);
        List<BsMiniMap.Node> nodes = new ArrayList<>();
        nodes.add(new BsMiniMap.Node(200, 200, new Color(0x0D / 255f, 0x6E / 255f, 0xFD / 255f, 1f)));
        nodes.add(new BsMiniMap.Node(500, 400, new Color(0xDC / 255f, 0x35 / 255f, 0x45 / 255f, 1f)));
        nodes.add(new BsMiniMap.Node(800, 300, new Color(0x19 / 255f, 0x87 / 255f, 0x54 / 255f, 1f)));
        nodes.add(new BsMiniMap.Node(1200, 600, new Color(0xFF / 255f, 0xC1 / 255f, 0x07 / 255f, 1f)));
        nodes.add(new BsMiniMap.Node(1500, 900, new Color(0x0D / 255f, 0xCA / 255f, 0xF0 / 255f, 1f)));
        mm.setNodes(nodes);
        List<float[]> conns = new ArrayList<>();
        conns.add(new float[]{200, 200, 500, 400});
        conns.add(new float[]{500, 400, 800, 300});
        conns.add(new float[]{800, 300, 1200, 600});
        conns.add(new float[]{1200, 600, 1500, 900});
        mm.setConnections(conns);
        mm.setViewport(400, 300, 600, 400);
        mm.setOnClick(canvasXY -> {
            mm.setViewport(canvasXY[0] - 300, canvasXY[1] - 200, 600, 400);
            setStatus.accept(String.format("MiniMap 跳转到 (%.0f, %.0f)", canvasXY[0], canvasXY[1]));
        });
        c.add(wrapFill(mm, 360, 240)).padTop(4).row();
        c.add(new Label("(点击小地图任意位置 → 视口矩形移动到该处)", skin)).padTop(4).row();
    }

    // ============================ Wave3-Misc ============================
    public void fillWave3Misc(Table c) {
        c.add(sectionTitle(skin, "Wave3-Misc  —— Affix / Drawer")).row();

        // Drawer
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
                d.setOnClose(() -> setStatus.accept("Drawer 关闭"));
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
                d.setOnClose(() -> setStatus.accept("左侧 Drawer 关闭"));
                d.show(stage);
            }
        });
        drawerRow.add(bLeft);
        c.add(drawerRow).left().padTop(4).row();

        // Affix
        c.add(new Label("② BsAffix 固定钉（滚动时钉在视口顶部）:",
                skin)).padTop(14).left().row();
        Table innerContent = new Table();
        innerContent.left().top();
        innerContent.defaults().growX().left();
        Table header1 = new Table(skin);
        header1.setBackground(skin.getDrawable("bs-menu-bar-bg"));
        header1.pad(8, 12, 8, 12);
        Label.LabelStyle lg = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        lg.font = skin.getFont("font-lg");
        Label h1 = new Label("第一组：基础信息", lg);
        h1.setColor(new Color(0.1f, 0.1f, 0.12f, 1f));
        header1.add(h1).left();
        innerContent.add(new BsAffix(skin, header1, BsAffix.Placement.TOP)).growX().row();
        for (int i = 0; i < 12; i++) {
            innerContent.add(new Label("  - 数据项 " + (i + 1), skin)).pad(4).left().row();
        }
        Table header2 = new Table(skin);
        header2.setBackground(skin.getDrawable("bs-menu-bar-bg"));
        header2.pad(8, 12, 8, 12);
        Label h2 = new Label("第二组：高级设置", lg);
        h2.setColor(new Color(0.1f, 0.1f, 0.12f, 1f));
        header2.add(h2).left();
        innerContent.add(new BsAffix(skin, header2, BsAffix.Placement.TOP)).growX().row();
        for (int i = 0; i < 15; i++) {
            innerContent.add(new Label("  - 高级项 " + (i + 1), skin)).pad(4).left().row();
        }
        BsScrollPane affixScroll = new BsScrollPane(innerContent, skin);
        affixScroll.setFadeScrollBars(false);
        c.add(affixScroll).growX().height(200).padTop(4).row();
        c.add(new Label("(滚动内容时，分组标题会「钉住」在顶部，演示 Affix 行为)", skin)).padTop(4).row();
    }

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
            setStatus.accept("编辑取消");
        }, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        d.addButton("保存", () -> {
            setStatus.accept("保存用户: " + form.collectValues());
            d.close();
        }, BsButton.Variant.PRIMARY);
        d.setOnClose(() -> setStatus.accept("Drawer 关闭"));
        d.show(stage);
    }

    // ============================ Wave4-Pickers ============================
    public void fillWave4Pickers(Table c) {
        c.add(sectionTitle(skin, "Wave4-Pickers  —— Calendar / DateRange / Time / Cascader")).row();

        c.add(new Label("① BsCalendar 月历（单选，点击日期）：", skin)).padTop(8).left().row();
        BsCalendar singleCal = new BsCalendar(skin)
                .setOnSelect(d -> setStatus.accept("Calendar 选中: " + d));
        c.add(singleCal).padTop(4).left().row();

        c.add(new Label("② BsCalendar 区间模式（先点起点，再点终点）：", skin)).padTop(14).left().row();
        BsCalendar rangeCal = new BsCalendar(skin, BsCalendar.Mode.RANGE)
                .setOnRange((s, e) -> setStatus.accept(e == null
                        ? "区间起点: " + s
                        : "区间: " + s + " ~ " + e));
        c.add(rangeCal).padTop(4).left().row();

        c.add(new Label("③ BsDateRangePicker（只读输入框，点击弹浮层选区间）：", skin)).padTop(14).left().row();
        BsDateRangePicker drp = new BsDateRangePicker(skin)
                .setOnChange((s, e) -> setStatus.accept("DateRange: " + s + " ~ " + e));
        drp.setRange(java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now());
        c.add(drp).width(260).padTop(4).left().row();

        c.add(new Label("④ BsTimePicker（只读输入框，点击弹时:分:秒面板）：", skin)).padTop(14).left().row();
        Table timeRow = new Table();
        timeRow.defaults().pad(4).left();
        BsTimePicker tpHms = new BsTimePicker(skin, true)
                .setOnChange(t -> setStatus.accept("Time(HMS): " + t));
        tpHms.setValue(java.time.LocalTime.now());
        timeRow.add(new Label("时:分:秒", skin));
        timeRow.add(tpHms).width(140);

        BsTimePicker tpHm = new BsTimePicker(skin, false)
                .setOnChange(t -> setStatus.accept("Time(HM): " + t));
        tpHm.setValue(java.time.LocalTime.now());
        timeRow.add(new Label("时:分", skin)).padLeft(16);
        timeRow.add(tpHm).width(100);
        c.add(timeRow).padTop(4).left().row();

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
                .setOptions(Arrays.asList(root1, root2))
                .setOnChange(path -> {
                    StringBuilder sb = new StringBuilder("Cascader: ");
                    for (int i = 0; i < path.size(); i++) {
                        if (i > 0) sb.append(" / ");
                        sb.append(path.get(i).label);
                    }
                    setStatus.accept(sb.toString());
                });
        c.add(cascader).width(280).padTop(4).left().row();
    }

    // ============================ Wave4-Display ============================
    public void fillWave4Display(Table c) {
        c.add(sectionTitle(skin, "Wave4-Display  —— Anchor / Comment / CircularProgress / RangeSlider")).row();

        // Anchor
        c.add(new Label("① BsAnchor 锚点导航（点击链接滚到目标，滚动时高亮当前节）：",
                skin)).padTop(8).left().row();
        Table anchorRow = new Table();
        Table doc = new Table();
        doc.left().top();
        doc.defaults().growX().left();
        Label h0 = sectionTitle(skin, "概述");
        Label h1 = sectionTitle(skin, "安装");
        Label h2 = sectionTitle(skin, "用法");
        doc.add(h0).padTop(8).row();
        for (int i = 0; i < 8; i++) doc.add(new Label("  概述内容行 " + (i + 1), skin)).pad(2).row();
        doc.add(h1).padTop(8).row();
        for (int i = 0; i < 10; i++) doc.add(new Label("  安装内容行 " + (i + 1), skin)).pad(2).row();
        doc.add(h2).padTop(8).row();
        for (int i = 0; i < 12; i++) doc.add(new Label("  用法内容行 " + (i + 1), skin)).pad(2).row();
        BsScrollPane docScroll = new BsScrollPane(doc, skin);
        docScroll.setFadeScrollBars(false);
        BsAnchor anchor = new BsAnchor(skin, docScroll)
                .setOnAnchorChange(i -> setStatus.accept("Anchor 当前节: " + i))
                .add("概述", h0)
                .add("安装", h1)
                .add("用法", h2);
        anchorRow.add(anchor).width(140).top().padRight(8);
        anchorRow.add(docScroll).grow().height(220);
        c.add(anchorRow).growX().padTop(4).row();

        // Comment
        c.add(new Label("② BsComment 评论 / 聊天气泡（对方/自己/评论流）：",
                skin)).padTop(14).left().row();
        // 头像用首字母占位：avatar(null) + name() 自动生成"主题色圆 + 首字"头像
        //（微信/钉钉同款 fallback，无需图片资源）
        BsComment msgOther = new BsComment(skin)
                .name("张三").avatar(null).time("12:30").text("你好！今天天气不错。");
        BsComment msgSelf = new BsComment(skin)
                .self(true).name("我").avatar(null).text("收到，下午见 👍").maxWidth(260);
        BsComment comment = new BsComment(skin)
                .name("李四").avatar(null).time("昨天")
                .text("这条评论很有用，已点赞收藏。").bubble(false);
        c.add(msgOther).left().padTop(4).row();
        c.add(msgSelf).right().padTop(4).row();
        c.add(comment).left().padTop(4).row();

        // CircularProgress
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
        BsCircularProgress indet = new BsCircularProgress(skin, BsCircularProgress.Variant.PRIMARY)
                .setIndeterminate(true);
        indet.setSize(48, 48);
        Container<BsCircularProgress> indetWrap = new Container<>(indet);
        indetWrap.size(48);
        ringRow.add(indetWrap);
        c.add(ringRow).left().padTop(4).row();

        // RangeSlider
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

    // ============================ Wave4-Form ============================
    public void fillWave4Form(Table c) {
        c.add(sectionTitle(skin, "Wave4-Form  —— BsFormValidator + BsRule 声明式校验")).row();
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

        validator.addAsyncRule("user", (val, onResult) -> {
            setStatus.accept("异步查重中: " + val + " ...");
            Thread t = new Thread(() -> {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                boolean exists = "admin".equalsIgnoreCase(val == null ? "" : val.trim());
                Gdx.app.postRunnable(() -> onResult.accept(exists ? "用户名已存在" : null));
            });
            t.setDaemon(true);
            t.start();
        });

        Table btnRow = new Table();
        btnRow.defaults().pad(4);
        BsButton bSync = new BsButton("同步校验", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.SM);
        bSync.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Map<String, String> errs = validator.validateAll();
                errLabel.setText(errs.isEmpty() ? "✓ 同步校验通过" : "✗ " + errs.toString());
                setStatus.accept(errs.isEmpty() ? "同步校验通过" : "同步校验失败");
            }
        });
        BsButton bAsync = new BsButton("异步校验（含查重）", skin,
                BsButton.Variant.INFO, BsButton.Style.SOLID, BsButton.Size.SM);
        bAsync.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                errLabel.setText("异步校验中...");
                validator.validateAsync(errs -> {
                    errLabel.setText(errs.isEmpty() ? "✓ 全部通过（含异步）" : "✗ " + errs.toString());
                    setStatus.accept(errs.isEmpty() ? "异步校验通过" : "异步校验失败");
                });
            }
        });
        btnRow.add(bSync);
        btnRow.add(bAsync);
        c.add(btnRow).left().padTop(6).row();
    }

    // ============================ Wave4-Data ============================
    public void fillWave4Data(Table c) {
        c.add(sectionTitle(skin, "Wave4-Data  —— DnD / VirtualList / DataGrid")).row();

        // DnD
        c.add(new Label("① BsDnd 拖放（把左边卡片拖到右边回收站）：", skin)).padTop(8).left().row();
        Table dndRow = new Table();
        dndRow.defaults().pad(6);
        final Table card = new Table();
        card.setBackground(skin.getDrawable("bs-window-bg"));
        Label cardLbl = new Label("卡片 #42", skin);
        cardLbl.setColor(BsPalette.PRIMARY.getMain());
        card.add(cardLbl).pad(6);
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
                .onDropped((payload, overTarget) -> setStatus.accept(overTarget != null
                        ? "已拖到回收站: " + payload
                        : "拖放取消（未落在目标上）"));
        dnd.target(bin)
                .setAccept(o -> true)
                .onDrop((payload, sourceActor) -> {
                    setStatus.accept("回收站接收: " + payload);
                    binLbl.setText("🗑 已回收: " + payload);
                });
        dndRow.add(cardWrap);
        dndRow.add(new Label("  →  ", skin));
        dndRow.add(binWrap);
        c.add(dndRow).left().padTop(4).row();

        // VirtualList
        c.add(new Label("② BsVirtualList 虚拟化长列表（1 万条数据，仅渲染可见 cell）：",
                skin)).padTop(14).left().row();
        List<String> huge = new ArrayList<>();
        for (int i = 0; i < 10000; i++) huge.add("数据项 #" + (i + 1));
        BsVirtualList<String> vlist = new BsVirtualList<>(skin, (existing, item, idx) -> {
            Table row;
            if (existing instanceof Table) {
                row = (Table) existing;
                row.clearChildren();
            } else {
                row = new Table();
            }
            if (idx % 2 == 1) {
                row.setBackground(BsSkinFactory.drawableOf(skin.get("bs-bg-hover", Color.class)));
            } else {
                row.setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);
            }
            Label l = new Label(item, skin);
            row.add(l).left().growX().padLeft(6);
            return row;
        }, 26f);
        vlist.setItems(huge)
                .setOnClick((idx, item) -> setStatus.accept("VirtualList 点 " + idx + ": " + item));
        c.add(vlist).growX().height(220).padTop(4).row();

        // DataGrid
        c.add(new Label("③ BsDataGrid 虚拟化数据表格（固定表头 + 大数据量）：",
                skin)).padTop(14).left().row();
        List<BsDataGridDemoRow> rows = new ArrayList<>();
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
                .setOnRowClick((idx, r) -> setStatus.accept("DataGrid 点行 " + idx + ": " + r.name));
        c.add(grid).growX().height(240).padTop(4).row();
    }

    /** BsDataGrid demo 行数据。 */
    public static final class BsDataGridDemoRow {
        public final int id;
        public final String name;
        public final int age;
        public final String email;

        public BsDataGridDemoRow(int id, String name, int age, String email) {
            this.id = id; this.name = name; this.age = age; this.email = email;
        }
    }
}
