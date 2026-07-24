package com.git.bs.demo.modules;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.git.bs.ui.BsAccordion;
import com.git.bs.ui.BsAlert;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsChoiceDialog;
import com.git.bs.ui.BsCollapse;
import com.git.bs.ui.BsConfirmDialog;
import com.git.bs.ui.BsForm;
import com.git.bs.ui.BsInputNumber;
import com.git.bs.ui.BsInputGroup;
import com.git.bs.ui.BsLink;
import com.git.bs.ui.BsModal;
import com.git.bs.ui.BsPopover;
import com.git.bs.ui.BsPromptDialog;
import com.git.bs.ui.BsSpinner;
import com.git.bs.ui.BsColorPicker;
import com.git.bs.ui.BsDatePicker;
import com.git.bs.ui.BsIcon;
import com.git.bs.ui.BsTextField;
import com.git.bs.ui.BsTooltip;

import java.util.Arrays;
import java.util.function.Consumer;

import static com.git.bs.demo.modules.ModuleSupport.*;

/**
 * 表单与浮层模块组：Pickers / Form / DateTime / Overlay / Modal / Dialogs / CollapseAccordion / InputNumberGroup。
 * @author authorZhao
 * @since 2026-07-16
 */
public class BsFormModules {

    private final Skin skin;
    private final Stage stage;
    private final Consumer<String> setStatus;

    public BsFormModules(Skin skin, Stage stage, Consumer<String> setStatus) {
        this.skin = skin;
        this.stage = stage;
        this.setStatus = setStatus;
    }

    // ============================ Pickers ============================
    public void fillPickers(Table c) {
        c.add(sectionTitle(skin, "Pickers  —— 日期选择 / 颜色选择")).row();

        c.add(new Label("日期选择器（点击文本框弹出日历）:", skin)).padTop(6).row();
        BsDatePicker datePicker = new BsDatePicker(skin);
        datePicker.setValue(java.time.LocalDate.now());
        datePicker.setOnChange(d -> setStatus.accept("日期 = " + d));
        c.add(datePicker).width(220).left().row();

        c.add(new Label("再来一个日期选择器（不同初始值）:", skin)).padTop(10).row();
        BsDatePicker datePicker2 = new BsDatePicker(skin);
        datePicker2.setValue(java.time.LocalDate.of(2026, 1, 1));
        datePicker2.setOnChange(d -> setStatus.accept("日期2 = " + d));
        c.add(datePicker2).width(220).left().row();

        c.add(new Label("颜色选择器（点击色块弹出调色板）:", skin)).padTop(14).row();
        Table colorRow = new Table();
        colorRow.defaults().pad(4);
        BsColorPicker colorPicker = new BsColorPicker(skin);
        colorPicker.setSelectedColor(Color.valueOf("#0D6EFD"));
        colorPicker.setOnChange(col -> setStatus.accept(String.format("色: R=%d G=%d B=%d",
                (int) (col.r * 255), (int) (col.g * 255), (int) (col.b * 255))));
        colorRow.add(colorPicker).size(60, 28);

        BsColorPicker colorPicker2 = new BsColorPicker(skin);
        colorPicker2.setSelectedColor(Color.valueOf("#DC3545"));
        colorPicker2.setOnChange(col -> setStatus.accept(String.format("色2: R=%d G=%d B=%d",
                (int) (col.r * 255), (int) (col.g * 255), (int) (col.b * 255))));
        colorRow.add(colorPicker2).size(60, 28);

        c.add(colorRow).left().row();
        c.add(new Label("(选色后状态栏会显示 RGB 值)", skin)).padTop(4).row();
    }

    // ============================ Form ============================
    public void fillForm(Table c) {
        c.add(sectionTitle(skin, "Form  —— 通用表单（带校验）")).row();

        c.add(new Label("填写表单（输入时实时校验，校验通过才能提交）:", skin)).padBottom(8).row();

        BsForm form = new BsForm(skin, 90, 220, 180);

        form.addField("用户名", new BsTextField("", skin),
                v -> (v == null || v.isEmpty()) ? "用户名必填" : null);

        form.addField("邮箱", new BsTextField("", skin),
                v -> {
                    if (v == null || v.isEmpty()) return "邮箱必填";
                    if (!v.contains("@")) return "邮箱必须包含 @";
                    return null;
                });

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

        form.addField("备注", new BsTextField("", skin));

        BsDatePicker birth = new BsDatePicker(skin);
        birth.setValue(java.time.LocalDate.of(2000, 1, 1));
        form.addField("生日", birth);

        form.addSubmitBar(
                "保存", () -> setStatus.accept("提交: " + String.join(" / ", form.collectValues())),
                "取消", () -> setStatus.accept("已取消")
        );

        c.add(form).growX().left().row();
        c.add(new Label("(输入用户名/邮箱/年龄后会即时显示校验错误)", skin)).padTop(8).row();
    }

    // ============================ DateTime ============================
    public void fillDateTime(Table c) {
        c.add(sectionTitle(skin, "DateTime  —— 日期时间选择器（精确到秒）")).row();

        c.add(new Label("含时间模式（点击弹出日历+时分秒）:", skin)).padTop(6).row();
        BsDatePicker dt1 = new BsDatePicker(skin, true);
        dt1.setValue(java.time.LocalDateTime.now());
        dt1.setOnChange(dt -> setStatus.accept("选了: " + dt));
        c.add(dt1).width(240).left().row();

        c.add(new Label("另一个含时间选择器:", skin)).padTop(10).row();
        BsDatePicker dt2 = new BsDatePicker(skin, true);
        dt2.setValue(java.time.LocalDateTime.of(2026, 1, 1, 9, 30, 0));
        dt2.setOnChange(dt -> setStatus.accept("dt2 = " + dt));
        c.add(dt2).width(240).left().row();

        c.add(new Label("纯日期模式（对比）:", skin)).padTop(10).row();
        BsDatePicker dt3 = new BsDatePicker(skin);
        dt3.setValue(java.time.LocalDate.now());
        dt3.setOnChange(dt -> setStatus.accept("纯日期: " + dt.toLocalDate()));
        c.add(dt3).width(240).left().row();

        c.add(new Label("(含时间模式选完点'确定'才提交，避免时间被吞)", skin)).padTop(8).row();
    }

    // ============================ Overlay ============================
    public void fillOverlay(Table c) {
        c.add(sectionTitle(skin, "Overlay  —— Tooltip / Spinner / Popover / Link")).row();

        // Tooltip（4 方向）
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

        // Spinner
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

        // Popover
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
                .onConfirm(() -> setStatus.accept("Popover: 已确认删除"));
        pop2.attach(stage);
        popRow.add(popBtn2);

        c.add(popRow).left().row();

        // Link
        c.add(new Label("Link 链接按钮（hover 字色加深）:", skin)).padTop(12).left().row();
        Table linkRow = new Table();
        linkRow.defaults().pad(8);
        BsLink link1 = new BsLink("忘记密码？", skin);
        link1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus.accept("点击: 忘记密码");
            }
        });
        BsLink link2 = new BsLink("注册新账号", skin);
        link2.setColor(Color.valueOf("#198754"));
        link2.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus.accept("点击: 注册新账号");
            }
        });
        BsLink link3 = new BsLink("联系我们", skin);
        link3.setColor(Color.valueOf("#DC3545"));
        link3.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus.accept("点击: 联系我们");
            }
        });
        linkRow.add(link1);
        linkRow.add(link2);
        linkRow.add(link3);
        c.add(linkRow).left().row();
    }

    // ============================ Modal ============================
    public void fillModal(Table c) {
        c.add(sectionTitle(skin, "Modal  —— 通用三行模态框")).row();

        c.add(new Label("点击下方按钮，弹出不同样式的模态框:", skin)).padTop(6).left().row();

        Table btnRow = new Table();
        btnRow.defaults().pad(6);

        // 1. 基础模态框（无图标）
        BsButton basic = new BsButton("基础模态框(无图标)", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        basic.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Label content = new Label("这是一个基础模态框（无图标）。三行结构：标题 / 内容 / 按钮。\n点击取消或确认关闭。", skin);
                content.setWrap(true);
                new BsModal("提示", skin)
                        .content(content)
                        .contentWidth(380)
                        .separator(true)
                        .addButton("取消", () -> setStatus.accept("基础模态: 取消"), BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE)
                        .addButton("确认", () -> setStatus.accept("基础模态: 确认"), BsButton.Variant.PRIMARY, BsButton.Style.SOLID)
                        .showModal(stage);
            }
        });
        btnRow.add(basic);

        // 2. 带标题图标（有图标）
        BsButton withIcon = new BsButton("带标题图标(有图标)", skin,
                BsButton.Variant.INFO, BsButton.Style.SOLID, BsButton.Size.MD);
        withIcon.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Label content = new Label("标题前面有一个图标（蓝色色块占位）。可换成任意 drawable。", skin);
                content.setWrap(true);
                new BsModal("操作确认", skin)
                        .setTitleIcon(com.git.bs.ui.BsIcon.get("info-circle-fill", Color.valueOf("#0D6EFD")))
                        .content(content)
                        .contentWidth(360)
                        .separator(true)
                        .addButton("关闭", () -> {}, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE)
                        .showModal(stage);
            }
        });
        btnRow.add(withIcon);

        // 3. 标题 banner
        BsButton withBanner = new BsButton("标题 Banner 背景", skin,
                BsButton.Variant.WARNING, BsButton.Style.SOLID, BsButton.Size.MD);
        withBanner.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
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

        // 4. 表单内容
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
                                setStatus.accept("表单提交: " + String.join(" / ", form.collectValues()));
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
    public void fillDialogs(Table c) {
        c.add(sectionTitle(skin, "Dialogs  —— 对话框库（带动画）")).row();

        c.add(new Label("基于 BsModal 实现的 4 种对话框，每种带不同入场/出场动画:",
                skin)).padTop(6).left().row();

        Table row1 = new Table();
        row1.defaults().pad(6);

        // Alert 4 种级别（复用 ModuleSupport.alertBtn，避免重复按钮构造样板）
        row1.add(new Label("Alert 弹窗:", skin)).right().padRight(8);
        BsButton bNotice = alertBtn(skin, stage, setStatus, "通知 (淡入)",
                com.git.bs.ui.BsAlertDialog.Level.NOTICE, "新消息", "您收到 1 条新消息，请注意查收。");
        BsButton bWarn = alertBtn(skin, stage, setStatus, "警告 (顶部滑入)",
                com.git.bs.ui.BsAlertDialog.Level.WARNING, "操作不可逆", "此操作将永久删除数据，是否继续？");
        BsButton bError = alertBtn(skin, stage, setStatus, "错误 (缩放)",
                com.git.bs.ui.BsAlertDialog.Level.ERROR, "提交失败", "网络异常，请稍后重试。错误码 500。");
        BsButton bSuccess = alertBtn(skin, stage, setStatus, "成功 (淡入)",
                com.git.bs.ui.BsAlertDialog.Level.SUCCESS, "保存成功", "您的修改已成功保存到服务器。");

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

        // 无图标 Alert（对比：setTitleIcon(null) 显式去掉图标）
        Table row0 = new Table();
        row0.defaults().pad(6);
        row0.add(new Label("无图标对比:", skin)).right().padRight(8);
        BsButton bNoticeNoIcon = new BsButton("通知(无图标)", skin,
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bNoticeNoIcon.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsAlertDialog d = new com.git.bs.ui.BsAlertDialog(
                        "通知(无图标)", "这个对话框没有标题图标——标题文字直接顶到左边。",
                        com.git.bs.ui.BsAlertDialog.Level.NOTICE, skin);
                d.setTitleIcon(null);
                d.showModal(stage);
            }
        });
        BsButton bConfirmNoIcon = new BsButton("确认(无图标)", skin,
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        bConfirmNoIcon.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                com.git.bs.ui.BsConfirmDialog d = new com.git.bs.ui.BsConfirmDialog(
                        "确认(无图标)", "没有图标的确认对话框。", skin,
                        ok -> setStatus.accept(ok ? "确认(无图标): 是" : "确认(无图标): 否"));
                d.setTitleIcon(null);
                d.showModal(stage);
            }
        });
        row0.add(bNoticeNoIcon);
        row0.add(bConfirmNoIcon);
        c.add(row0).left().padTop(4).row();

        Table row2 = new Table();
        row2.defaults().pad(6);
        row2.add(bWarn);
        row2.add(bError);
        row2.add(bSuccess);

        c.add(row1).left().row();
        c.add(row2).left().padTop(4).row();

        // Confirm / Prompt / Choice
        c.add(new Label("交互对话框:", skin)).padTop(14).left().row();
        Table row3 = new Table();
        row3.defaults().pad(6);

        BsButton bConfirm = new BsButton("确认对话框 Confirm", skin,
                BsButton.Variant.INFO, BsButton.Style.OUTLINE, BsButton.Size.MD);
        bConfirm.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsConfirmDialog.show(stage, skin,
                        "确认删除？", "此操作不可撤销，确定要删除这条记录吗？",
                        ok -> setStatus.accept(ok ? "用户点了【是】" : "用户点了【否】"));
            }
        });
        row3.add(bConfirm);

        BsButton bPrompt = new BsButton("文本输入 Prompt", skin,
                BsButton.Variant.SUCCESS, BsButton.Style.OUTLINE, BsButton.Size.MD);
        bPrompt.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsPromptDialog.show(stage, skin,
                        "新建项目", "请输入项目名称：", "my-project",
                        text -> setStatus.accept(text == null ? "用户取消输入" : "输入: " + text));
            }
        });
        row3.add(bPrompt);

        BsButton bChoice = new BsButton("多选一 Choice", skin,
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.MD);
        bChoice.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                BsChoiceDialog.show(stage, skin,
                        "选择难度", "请选择游戏难度：",
                        Arrays.asList("简单 / Easy", "普通 / Normal", "困难 / Hard", "地狱 / Inferno"),
                        idx -> setStatus.accept(idx < 0 ? "用户取消" : "选了第 " + idx + " 项"));
            }
        });
        row3.add(bChoice);

        c.add(row3).left().row();
        c.add(new Label("(NOTICE 淡入 / WARNING 上滑入 / ERROR 缩放 / SUCCESS 淡入；交互框带淡出)",
                skin)).padTop(8).row();
    }

    // ============================ Collapse & Accordion ============================
    public void fillCollapseAccordion(Table c) {
        c.add(sectionTitle(skin, "Collapse & Accordion  —— 折叠 / 手风琴")).row();

        // 单个 Collapse
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
        collapse.setOnToggle((src, expanded) -> setStatus.accept("Collapse: " + (expanded ? "展开" : "收起")));
        c.add(collapse).width(500).growX().row();

        // Accordion
        c.add(new Label("Accordion 手风琴(单选模式,一次只展开一节):", skin)).padTop(16).left().row();
        BsAccordion acc = new BsAccordion(skin);
        acc.setSingleOpen(true);
        acc.addSection("基本信息", makeAccordionContent(skin, "基本信息内容:用户 ID / 注册时间 / 状态"));
        acc.addSection("联系方式", makeAccordionContent(skin, "邮箱 / 手机 / 地址"));
        acc.addSection("安全设置", makeAccordionContent(skin, "密码强度 / 两步验证 / 登录历史"));
        acc.addSection("高级", makeAccordionContent(skin, "API token / Webhook / 订阅偏好"));
        acc.expand(0);
        c.add(acc).width(500).growX().row();

        c.add(new Label("(点击节标题切换，单选模式会自动收起其他节)", skin)).padTop(8).row();
    }

    // ============================ InputNumber & InputGroup ============================
    public void fillInputNumberGroup(Table c) {
        c.add(sectionTitle(skin, "InputNumber & InputGroup  —— 数字步进器 / 输入组")).row();

        // BsInputNumber
        c.add(new Label("InputNumber 数字步进器（长按 +/- 按钮可连续增减）:", skin)).padTop(8).left().row();
        Table row1 = new Table();
        row1.defaults().pad(6);
        BsInputNumber num1 = new BsInputNumber(skin);
        num1.setRange(0, 100).setStep(1).setValue(20);
        num1.setOnChange(v -> setStatus.accept("步进器 1: " + (int) (double) v));
        row1.add(num1);

        BsInputNumber num2 = new BsInputNumber(skin);
        num2.setRange(-50, 50).setStep(5).setValue(0);
        num2.setOnChange(v -> setStatus.accept("步进器 2: " + (int) (double) v));
        row1.add(num2);

        BsInputNumber num3 = new BsInputNumber(skin);
        num3.setRange(0, 10).setStep(0.5).setDecimals(1).setValue(2.5);
        num3.setOnChange(v -> setStatus.accept("步进器 3 (小数): " + v));
        row1.add(num3);

        c.add(row1).left().row();

        // BsInputGroup
        c.add(new Label("InputGroup 输入组（前缀/后缀 文字、图标、按钮）:", skin)).padTop(14).left().row();

        c.add(new Label("前缀文字（@ 用户名）:", skin)).padTop(6).left().row();
        BsInputGroup g1 = new BsInputGroup(skin)
                .prependText("@")
                .field(new BsTextField("", skin));
        c.add(g1).left().padTop(4).row();

        c.add(new Label("后缀文字（金额单位）:", skin)).padTop(6).left().row();
        BsInputGroup g2 = new BsInputGroup(skin)
                .prependText("¥")
                .field(new BsTextField("", skin))
                .appendText(".00");
        c.add(g2).left().padTop(4).row();

        c.add(new Label("后缀按钮（搜索框）:", skin)).padTop(6).left().row();
        BsTextField searchField = new BsTextField("", skin);
        searchField.setMessageText("输入关键词...");
        BsInputGroup g3 = new BsInputGroup(skin)
                .field(searchField)
                .appendButton("搜索", () -> setStatus.accept("搜索: " + searchField.getText()),
                        BsButton.Variant.PRIMARY);
        c.add(g3).left().padTop(4).row();

        c.add(new Label("前缀图标 + 后缀文字（邮箱）:", skin)).padTop(6).left().row();
        Drawable envelope = BsIcon.get("envelope");
        BsTextField emailField = new BsTextField("", skin);
        emailField.setMessageText("user@example.com");
        BsInputGroup g4 = new BsInputGroup(skin);
        if (envelope != null) g4.prependIcon(envelope);
        g4.field(emailField).appendText(".com");
        c.add(g4).left().padTop(4).row();

        c.add(new Label("(InputNumber 别叫 Spinner，避免与加载转圈 BsSpinner 重名)", skin)).padTop(8).row();
    }
}
