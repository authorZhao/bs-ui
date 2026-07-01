package com.git.bs.demo.modules;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.git.bs.ui.BsAboutDialog;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsCheckBox;
import com.git.bs.ui.BsContextMenu;
import com.git.bs.ui.BsList;
import com.git.bs.ui.BsMenuBar;
import com.git.bs.ui.BsRadioButton;
import com.git.bs.ui.BsRadioButtonGroup;
import com.git.bs.ui.BsScrollPane;
import com.git.bs.ui.BsSelectBox;
import com.git.bs.ui.BsSlider;
import com.git.bs.ui.BsStatusLabel;
import com.git.bs.ui.BsTextArea;
import com.git.bs.ui.BsTextField;
import com.git.bs.ui.BsTooltip;
import com.git.bs.ui.BsWindow;

import java.util.Arrays;
import java.util.function.Consumer;

import static com.git.bs.demo.modules.ModuleSupport.*;

/**
 * 基础控件模块组：Labels / Buttons / ImageButton / Inputs / Selects / RadioCheck / Slider / Misc / MenuBar / ContextMenu。
 *
 * <p>持有 skin / stage / setStatus，不持有可变跨模块状态。</p>
 */
public class BsBasicModules {

    private final Skin skin;
    private final Stage stage;
    private final Consumer<String> setStatus;

    public BsBasicModules(Skin skin, Stage stage, Consumer<String> setStatus) {
        this.skin = skin;
        this.stage = stage;
        this.setStatus = setStatus;
    }

    // ============================ Labels ============================
    public void fillLabels(Table c) {
        c.add(sectionTitle(skin, "Labels  —— 标签（多种 Variant 与字号）")).row();

        c.add(new Label("普通文本 Label —— Bootstrap 风格深灰字", skin)).row();

        // 彩色 Label
        c.add(new Label("彩色 Label（BsStatusLabel 6 variant）：", skin)).left().row();
        Table colorRow = new Table();
        colorRow.defaults().pad(4);
        for (BsStatusLabel.Variant v : BsStatusLabel.Variant.values()) {
            colorRow.add(new BsStatusLabel(v.name(), skin, v));
        }
        c.add(colorRow).left().row();

        // 多字号 Label
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
    public void fillButtons(Table c) {
        c.add(sectionTitle(skin, "Buttons  —— 文字按钮（6 色 × Solid/Outline × 3 尺寸）")).row();
        for (BsButton.Style st : BsButton.Style.values()) {
            c.add(new Label(st.name() + ":", skin)).padRight(6);
            for (BsButton.Variant v : BsButton.Variant.values()) {
                BsButton b = new BsButton(v.name(), skin, v, st, BsButton.Size.SM);
                b.addListener(logClick(setStatus, "按钮", st + "/" + v));
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
    public void fillImageButtons(Table c) {
        c.add(sectionTitle(skin, "ImageButton  —— 图标按钮")).row();
        c.add(new Label("纯图标 Button（用 skin drawable 当背景）；图片资源预留 —— "
                + "若 assets 里没有图标，会用 Bs 样式色块兜底。", skin)).width(700).row();

        Table row = new Table();
        row.defaults().pad(4);
        for (BsButton.Variant v : BsButton.Variant.values()) {
            BsButton b = new BsButton("★", skin, v, BsButton.Style.SOLID, BsButton.Size.MD);
            b.addListener(logClick(setStatus, "图标按钮", v.name()));
            row.add(b);
        }
        c.add(row).row();

        c.add(new Label("Image（静态图片，预留）：", skin)).padTop(10).row();
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
    public void fillInputs(Table c) {
        c.add(sectionTitle(skin, "Inputs  —— 文本输入框 / 多行 TextArea")).row();

        c.add(new Label("单行 TextField:", skin)).padRight(6);
        BsTextField tf = new BsTextField("", skin);
        tf.setMessageText("请输入用户名…");
        tf.setTextFieldListener((f, ch) -> setStatus.accept("输入: " + f.getText()));
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
    public void fillSelects(Table c) {
        c.add(sectionTitle(skin, "Selects  —— 下拉选择（SelectBox / List）")).row();

        c.add(new Label("SelectBox（点击展开）:", skin)).padRight(6);
        BsSelectBox<String> sb = new BsSelectBox<>(skin);
        sb.setItems(items(Arrays.asList("选项 1", "选项 2", "选项 3", "长一点的选项 text")));
        sb.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                setStatus.accept("SelectBox 选中: " + sb.getSelected());
            }
        });
        c.add(sb).width(220).row();

        c.add(new Label("List（用 ScrollPane 包裹，所有项可滚动选中）:", skin)).padTop(10).row();
        BsList<String> list = new BsList<>(skin);
        list.setItems(items(Arrays.asList(
                "苹果", "香蕉", "橙子", "葡萄", "西瓜", "芒果", "荔枝", "榴莲")));
        list.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus.accept("List 选中: " + list.getSelected());
            }
        });
        BsScrollPane listScroll = new BsScrollPane(list, skin);
        listScroll.setFadeScrollBars(false);
        listScroll.setScrollingDisabled(true, false);
        c.add(listScroll).width(220).height(140).row();
    }

    // ============================ Radio & Check ============================
    public void fillRadioCheck(Table c) {
        c.add(sectionTitle(skin, "Radio & Check  —— 单选 / 多选")).row();

        c.add(new Label("CheckBox（多选，各自独立）:", skin)).padTop(6).row();
        Table cbRow = new Table();
        cbRow.defaults().pad(4).left();
        BsCheckBox cb1 = new BsCheckBox("启用音效", skin);
        BsCheckBox cb2 = new BsCheckBox("启用背景音乐", skin);
        BsCheckBox cb3 = new BsCheckBox("全屏显示", skin);
        cb2.setChecked(true);
        for (BsCheckBox cb : Arrays.asList(cb1, cb2, cb3)) {
            cb.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    setStatus.accept("CheckBox [" + cb.getText() + "] = " + cb.isChecked());
                }
            });
            cbRow.add(cb).padRight(20);
        }
        c.add(cbRow).row();

        c.add(new Label("RadioButton（单选，同组互斥）:", skin)).padTop(12).row();
        Table rbRow = new Table();
        rbRow.defaults().pad(4).left();
        BsRadioButtonGroup rbGroup = new BsRadioButtonGroup();
        BsRadioButton r1 = rbGroup.add(new BsRadioButton("简单难度", skin));
        BsRadioButton r2 = rbGroup.add(new BsRadioButton("普通难度", skin));
        BsRadioButton r3 = rbGroup.add(new BsRadioButton("困难难度", skin));
        r2.setChecked(true);
        for (BsRadioButton rb : Arrays.asList(r1, r2, r3)) {
            rb.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    if (rb.isChecked()) setStatus.accept("RadioButton 选中: " + rb.getText());
                }
            });
            rbRow.add(rb).padRight(20);
        }
        c.add(rbRow).row();
    }

    // ============================ Sliders ============================
    public void fillSliders(Table c) {
        c.add(sectionTitle(skin, "Slider  —— 滑块")).row();

        c.add(new Label("水平 Slider 0~100:", skin)).padRight(6);
        BsSlider hSlider = new BsSlider(0, 100, 1, false, skin);
        hSlider.setValue(40);
        hSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                setStatus.accept("水平 Slider = " + hSlider.getValue());
            }
        });
        c.add(hSlider).width(300).row();

        c.add(new Label("垂直 Slider 0~10:", skin)).padTop(10).row();
        BsSlider vSlider = new BsSlider(0, 10, 0.5f, true, skin);
        vSlider.setValue(5);
        vSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                setStatus.accept("垂直 Slider = " + vSlider.getValue());
            }
        });
        c.add(vSlider).size(40, 200).row();
    }

    // ============================ Misc ============================
    public void fillMisc(Table c) {
        c.add(sectionTitle(skin, "Misc  —— Tooltip / 禁用按钮 / 提示信息")).row();

        c.add(new Label("鼠标悬浮在下方按钮上 →", skin)).padRight(4);
        BsButton tipBtn = new BsButton("Hover me!", skin, BsButton.Variant.INFO, BsButton.Style.SOLID, BsButton.Size.MD);
        c.add(tipBtn).row();
        BsTooltip tooltip = new BsTooltip(tipBtn, "这是一个 Tooltip 提示气泡", skin);
        tooltip.attach(stage);

        c.add(new Label("模态窗口:", skin)).padTop(10).row();
        BsButton openModal = new BsButton("打开模态窗口", skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        openModal.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                showDemoModal();
            }
        });
        c.add(openModal).row();

        c.add(new Label("状态标签示例:", skin)).padTop(10).row();
        Table tags = new Table();
        tags.defaults().pad(4);
        tags.add(new BsStatusLabel("Primary", skin, BsStatusLabel.Variant.PRIMARY));
        tags.add(new BsStatusLabel("Success", skin, BsStatusLabel.Variant.SUCCESS));
        tags.add(new BsStatusLabel("Warning", skin, BsStatusLabel.Variant.WARNING));
        tags.add(new BsStatusLabel("Danger", skin, BsStatusLabel.Variant.DANGER));
        tags.add(new BsStatusLabel("Info", skin, BsStatusLabel.Variant.INFO));
        c.add(tags).row();

        c.add(new Label("Alert 弹窗（4 种级别 + 入场动画）:", skin)).padTop(12).row();
        Table alertRow = new Table();
        alertRow.defaults().pad(4);
        alertRow.add(alertBtn(skin, stage, setStatus, "通知 Notice", com.git.bs.ui.BsAlertDialog.Level.NOTICE,
                "这是 NOTICE 级别的提示，淡入动画。"));
        alertRow.add(alertBtn(skin, stage, setStatus, "警告 Warning", com.git.bs.ui.BsAlertDialog.Level.WARNING,
                "这是 WARNING 级别的提示，从顶部滑入。"));
        alertRow.add(alertBtn(skin, stage, setStatus, "错误 Error", com.git.bs.ui.BsAlertDialog.Level.ERROR,
                "这是 ERROR 级别的提示，缩放进入。"));
        alertRow.add(alertBtn(skin, stage, setStatus, "成功 Success", com.git.bs.ui.BsAlertDialog.Level.SUCCESS,
                "这是 SUCCESS 级别的提示，淡入动画。"));
        c.add(alertRow).row();
    }

    private void showDemoModal() {
        BsWindow win = new BsWindow("模态窗口示例", skin, true);
        win.setMovable(true);
        Table content = new Table(skin);
        content.defaults().pad(8);
        content.add(new Label("这是一个模态窗口。", skin)).row();
        content.add(new Label("点击「关闭」按钮 / 点窗口外区域 都可关闭。", skin)).padTop(6).row();
        BsButton closeBtn = new BsButton("关闭", skin, BsButton.Variant.SECONDARY, BsButton.Style.SOLID, BsButton.Size.SM);
        closeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                win.close();
            }
        });
        content.add(closeBtn).padTop(10);
        win.add(content);
        win.showModal(stage);
        setStatus.accept("打开模态窗口");
    }

    // ============================ MenuBar ============================
    public void fillMenuBar(Table c) {
        c.add(sectionTitle(skin, "MenuBar  —— 菜单栏（点击按钮弹出下拉项）")).row();

        c.add(new Label("点击下方菜单按钮，弹出下拉项；点 item 触发回调，点外部或 Esc 关闭。",
                skin)).padBottom(8).row();

        BsMenuBar bar = new BsMenuBar(skin);
        BsMenuBar.BsMenu file = bar.addMenu("File");
        file.addItem("New", () -> setStatus.accept("File → New"));
        file.addItem("Open...", () -> setStatus.accept("File → Open"));
        file.addSeparator();
        file.addItem("Exit", () -> setStatus.accept("File → Exit"));

        BsMenuBar.BsMenu edit = bar.addMenu("Edit");
        edit.addItem("Undo", () -> setStatus.accept("Edit → Undo"));
        edit.addItem("Redo", () -> setStatus.accept("Edit → Redo"));
        edit.addSeparator();
        edit.addItem("Cut", () -> setStatus.accept("Edit → Cut"));
        edit.addItem("Copy", () -> setStatus.accept("Edit → Copy"));
        edit.addItem("Paste", () -> setStatus.accept("Edit → Paste"));

        BsMenuBar.BsMenu help = bar.addMenu("Help");
        help.addItem("About...", () -> BsAboutDialog.show(stage, skin, "Bs UI 控件测试台", false));
        help.addItem("Docs...", () -> setStatus.accept("Help → Docs"));

        c.add(bar).growX().row();

        c.add(new Label("(状态栏会显示你点过的菜单项)", skin)).padTop(10).row();
    }

    // ============================ ContextMenu（新模块）============================
    public void fillContextMenu(Table c) {
        c.add(sectionTitle(skin, "ContextMenu  —— 右键上下文菜单（Windows 风格）")).row();
        c.add(new Label("① 右键点击下方文本框/表格/标签 → 在点击位置弹出菜单（桌面）；触屏长按 0.5s。",
                skin)).padTop(6).left().width(700).row();

        // 1. 挂到 TextField
        BsTextField tf = new BsTextField("右键点我", skin);
        new BsContextMenu(skin)
                .add("复制", () -> setStatus.accept("ContextMenu: 复制"))
                .add("粘贴", () -> setStatus.accept("ContextMenu: 粘贴"))
                .addDisabled("重命名（只读）")
                .addSeparator()
                .add("清空", () -> tf.setText(""))
                .attach(tf);
        c.add(new Label("TextField:", skin)).padTop(8).row();
        c.add(tf).width(360).row();

        // 2. 挂到 Table 行
        Table tableRow = new Table();
        tableRow.setBackground(skin.newDrawable("white", Color.valueOf("#F8F9FA")));
        tableRow.add(new Label("📊 张三的档案 —— 右键点这一行", skin)).pad(10).left();
        new BsContextMenu(skin)
                .add("查看详情", () -> setStatus.accept("行 → 查看详情"))
                .add("编辑", () -> setStatus.accept("行 → 编辑"))
                .addSeparator()
                .add("删除", () -> setStatus.accept("行 → 删除"))
                .attach(tableRow);
        c.add(tableRow).width(360).padTop(8).row();

        // 3. 手动弹出按钮
        BsButton btn = new BsButton("点我弹出菜单（不靠右键）", skin,
                BsButton.Variant.INFO, BsButton.Style.SOLID, BsButton.Size.SM);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Vector2 p = btn.localToStageCoordinates(new Vector2(btn.getWidth(), 0));
                new BsContextMenu(skin)
                        .add("新建文件", () -> setStatus.accept("按钮菜单 → 新建"))
                        .add("打开文件", () -> setStatus.accept("按钮菜单 → 打开"))
                        .addSeparator()
                        .add("退出", () -> setStatus.accept("按钮菜单 → 退出"))
                        .show(stage, p.x, p.y);
            }
        });
        c.add(btn).padTop(10).row();

        c.add(new Label("(点击菜单项 / 点外部空白 / 按 Esc 都可关闭)", skin)).padTop(8).row();
    }
}
