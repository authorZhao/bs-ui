package com.git.bs.demo.modules;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import com.git.bs.ui.BsAlert;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsButtonGroup;
import com.git.bs.ui.BsIcon;
import com.git.bs.ui.BsIconLabel;
import com.git.bs.ui.BsLink;
import com.git.bs.ui.BsMenuBar;
import com.git.bs.ui.BsProgress;
import com.git.bs.ui.BsScrollPane;
import com.git.bs.ui.BsTextField;
import com.git.bs.ui.BsToast;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.git.bs.demo.modules.ModuleSupport.*;

/**
 * 反馈类控件模块组：Icons / ProgressToast / ButtonGroupAlert。
 *
 * <p>{@code iconGrid / iconFilterField} 持有 Icons 过滤状态；
 * {@code demoProgress / progressValue} 持有 Progress 控制状态。</p>
 */
public class BsFeedbackModules {

    private final Skin skin;
    private final Stage stage;
    private final Consumer<String> setStatus;

    // Icons 模块状态
    private BsScrollPane iconGridScroll;
    private Table iconGrid;
    private BsTextField iconFilterField;

    // Progress 模块状态
    private BsProgress demoProgress;
    private float progressValue = 0f;

    public BsFeedbackModules(Skin skin, Stage stage, Consumer<String> setStatus) {
        this.skin = skin;
        this.stage = stage;
        this.setStatus = setStatus;
    }

    // ============================ Icons ============================
    public void fillIcons(Table c) {
        c.add(sectionTitle(skin, "Icons  —— Bootstrap Icons 图标库")).row();

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
                if (BsIcon.load()) {
                    refreshIconGrid();
                    setStatus.accept("重新加载 atlas 成功");
                } else {
                    setStatus.accept("atlas 未生成");
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

        if (BsIcon.load()) {
            refreshIconGrid();
        } else {
            iconGrid.add(new Label("(尚未生成 atlas)", skin)).colspan(8).row();
        }

        // Icon 应用演示
        c.add(new Label("② Icon 应用演示（按钮 / 链接 / IconLabel / 菜单）：",
                skin)).padTop(14).left().row();
        Table demoRow = new Table();
        demoRow.defaults().pad(8).left();

        BsButton settingBtn = new BsButton("设置", skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        Drawable gearIcon = BsIcon.get("gear");
        if (gearIcon != null) settingBtn.setIcon(gearIcon);
        settingBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus.accept("点了 [设置] 按钮");
            }
        });
        demoRow.add(settingBtn);

        BsButton userBtn = new BsButton("用户", skin, BsButton.Variant.SUCCESS, BsButton.Style.SOLID, BsButton.Size.MD);
        Drawable personIcon = BsIcon.get("person");
        if (personIcon != null) userBtn.setIcon(personIcon);
        userBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus.accept("点了 [用户] 按钮");
            }
        });
        demoRow.add(userBtn);

        BsButton delBtn = new BsButton("删除", skin, BsButton.Variant.DANGER, BsButton.Style.OUTLINE, BsButton.Size.MD);
        Drawable trashIcon = BsIcon.get("trash");
        if (trashIcon != null) delBtn.setIcon(trashIcon);
        delBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus.accept("点了 [删除] 按钮");
            }
        });
        demoRow.add(delBtn);

        c.add(demoRow).left().row();

        Table demoRow2 = new Table();
        demoRow2.defaults().pad(8).left();

        BsLink inboxLink = new BsLink("收件箱", skin);
        Drawable envelopeIcon = BsIcon.get("envelope");
        if (envelopeIcon != null) inboxLink.setIcon(envelopeIcon);
        inboxLink.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus.accept("点了 [收件箱] 链接");
            }
        });
        demoRow2.add(inboxLink);

        BsLink homeLink = new BsLink("首页", skin);
        Drawable homeIcon = BsIcon.get("house");
        if (homeIcon != null) homeLink.setIcon(homeIcon);
        demoRow2.add(homeLink);

        Drawable heartIcon = BsIcon.get("heart");
        if (heartIcon != null) {
            BsIconLabel il1 = new BsIconLabel("点赞", skin)
                    .icon(heartIcon)
                    .iconColor(Color.valueOf("#DC3545"));
            demoRow2.add(il1);
        }
        Drawable starIcon = BsIcon.get("star-fill");
        if (starIcon != null) {
            BsIconLabel il2 = new BsIconLabel("收藏", skin)
                    .icon(starIcon)
                    .iconColor(Color.valueOf("#FFC107"));
            demoRow2.add(il2);
        }

        c.add(demoRow2).left().padTop(4).row();

        // 带图标的菜单栏
        c.add(new Label("菜单栏 + 图标：", skin)).padTop(10).left().row();
        BsMenuBar iconBar = new BsMenuBar(skin);
        BsMenuBar.BsMenu fileMenu = iconBar.addMenu("文件", BsIcon.get("folder"));
        if (gearIcon != null) {
            fileMenu.addItem("新建", () -> setStatus.accept("文件 → 新建"));
            fileMenu.addItem("打开", () -> setStatus.accept("文件 → 打开"));
            fileMenu.addSeparator();
            fileMenu.addItem("退出", () -> setStatus.accept("文件 → 退出"));
        }
        BsMenuBar.BsMenu editMenu = iconBar.addMenu("编辑", BsIcon.get("pencil"));
        if (gearIcon != null) {
            editMenu.addItem("撤销", () -> setStatus.accept("编辑 → 撤销"));
            editMenu.addItem("重做", () -> setStatus.accept("编辑 → 重做"));
        }
        c.add(iconBar).growX().padTop(4).row();

        c.add(new Label("(图标来自转换后的 atlas；按钮/链接用 setIcon，菜单用 addMenu(title, icon)，"
                + "Label 用 BsIconLabel)", skin)).padTop(8).row();
    }

    private void refreshIconGrid() {
        if (iconGrid == null) return;
        iconGrid.clearChildren();
        if (!BsIcon.isLoaded()) {
            iconGrid.add(new Label("(atlas 未加载)", skin)).colspan(8).row();
            return;
        }
        String filter = iconFilterField.getText().trim().toLowerCase();
        List<String> names = new ArrayList<>(BsIcon.getAllNames());
        names.sort(String::compareTo);
        List<String> filtered = new ArrayList<>();
        for (String n : names) {
            if (filter.isEmpty() || n.toLowerCase().startsWith(filter) || n.toLowerCase().contains(filter)) {
                filtered.add(n);
            }
        }
        int maxShow = 300;
        boolean truncated = filtered.size() > maxShow;
        if (truncated) filtered = filtered.subList(0, maxShow);

        int cols = 8;
        int col = 0;
        Drawable cellBg = skin.newDrawable("white", new Color(
                0x2C / 255f, 0x3E / 255f, 0x50 / 255f, 1f));
        for (String name : filtered) {
            Drawable d = BsIcon.get(name);
            if (d == null) continue;
            Image img = new Image(d);
            img.setScaling(Scaling.fit);
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
                    setStatus.accept("点击图标: " + name);
                }
            });
            iconGrid.add(cell).pad(4);
            col++;
            if (col >= cols) { iconGrid.row(); col = 0; }
        }
        if (truncated) {
            iconGrid.row();
            Label more = new Label("(只显示前 " + maxShow + " 个，更精确过滤查看更多)", skin);
            more.setColor(Color.GRAY);
            iconGrid.add(more).colspan(cols).padTop(8).row();
        }
        if (filtered.isEmpty()) {
            iconGrid.add(new Label("(没有匹配 \"" + filter + "\" 的图标)", skin)).colspan(cols).padTop(20).row();
        }
    }

    // ============================ Progress & Toast ============================
    public void fillProgressToast(Table c) {
        c.add(sectionTitle(skin, "Progress & Toast  —— 进度条 / 轻提示(吐司)")).row();

        // Progress: 6 色
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

        // 可控进度条
        c.add(new Label("可控进度条(点击 +10% / -10% / 重置 / 条纹 / 动画):", skin)).padTop(12).left().row();
        demoProgress = new BsProgress(skin);
        demoProgress.setVariant(BsProgress.Variant.PRIMARY);
        demoProgress.setProgress(0f);
        demoProgress.setShowLabel(true);
        c.add(demoProgress).width(480).height(22).left().row();

        Table ctrlRow = new Table();
        ctrlRow.defaults().pad(4);
        ctrlRow.add(progBtn(skin, "-10%", () -> {
            progressValue = Math.max(0, progressValue - 0.1f);
            demoProgress.setProgress(progressValue);
            setStatus.accept("进度: " + Math.round(progressValue * 100) + "%");
        }));
        ctrlRow.add(progBtn(skin, "+10%", () -> {
            progressValue = Math.min(1, progressValue + 0.1f);
            demoProgress.setProgress(progressValue);
            setStatus.accept("进度: " + Math.round(progressValue * 100) + "%");
        }));
        ctrlRow.add(progBtn(skin, "重置", () -> {
            progressValue = 0;
            demoProgress.setProgress(0);
            setStatus.accept("进度重置");
        }));
        ctrlRow.add(progBtn(skin, "切换条纹", () -> demoProgress.setStriped(true)));
        ctrlRow.add(progBtn(skin, "切换动画", () -> demoProgress.setAnimated(true)));
        c.add(ctrlRow).left().row();

        // Toast
        c.add(new Label("Toast 轻提示(右上角堆叠,自动消失):", skin)).padTop(14).left().row();
        Table toastRow = new Table();
        toastRow.defaults().pad(4);
        for (final BsToast.Variant v : BsToast.Variant.values()) {
            BsButton b = new BsButton(v.name(), skin, BsButton.Variant.PRIMARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    BsToast.show(stage, skin, v.name() + " 提示", "这是一条 " + v.name() + " 级别的轻提示",
                            v, 3f, BsToast.Placement.TOP_RIGHT);
                    setStatus.accept("Toast: " + v);
                }
            });
            toastRow.add(b);
        }
        c.add(toastRow).left().row();

        c.add(new Label("Toast 不同位置(placement):", skin)).padTop(8).left().row();
        Table placeRow = new Table();
        placeRow.defaults().pad(4);
        placeRow.add(toastPlaceBtn(skin, stage, setStatus, "右上", BsToast.Placement.TOP_RIGHT));
        placeRow.add(toastPlaceBtn(skin, stage, setStatus, "左上", BsToast.Placement.TOP_LEFT));
        placeRow.add(toastPlaceBtn(skin, stage, setStatus, "顶部居中", BsToast.Placement.TOP_CENTER));
        placeRow.add(toastPlaceBtn(skin, stage, setStatus, "右下", BsToast.Placement.BOTTOM_RIGHT));
        placeRow.add(toastPlaceBtn(skin, stage, setStatus, "左下", BsToast.Placement.BOTTOM_LEFT));
        c.add(placeRow).left().row();

        c.add(new Label("(Toast 不阻断操作，3 秒后自动消失；右上角堆叠，新提示会推到下方)",
                skin)).padTop(8).row();
    }

    // ============================ ButtonGroup & Alert ============================
    public void fillButtonGroupAlert(Table c) {
        c.add(sectionTitle(skin, "ButtonGroup & Alert  —— 按钮组 / 警告横条")).row();

        // ButtonGroup 单选
        c.add(new Label("ButtonGroup 单选(分段选择器,active 互斥):", skin)).padTop(8).left().row();
        BsButtonGroup single = new BsButtonGroup(skin, BsButtonGroup.Mode.SINGLE);
        single.addToggle("日");
        single.addToggle("周");
        single.addToggle("月");
        single.addToggle("年");
        single.select(1);
        single.setOnChange(idx -> setStatus.accept("ButtonGroup 单选: " + idx));
        c.add(single).left().row();

        // 多选
        c.add(new Label("ButtonGroup 多选(可同时选中多个):", skin)).padTop(10).left().row();
        BsButtonGroup multi = new BsButtonGroup(skin, BsButtonGroup.Mode.MULTI);
        multi.addToggle("粗体");
        multi.addToggle("斜体");
        multi.addToggle("下划线");
        multi.addToggle("删除线");
        multi.setOnChange(idx -> setStatus.accept("ButtonGroup 多选当前选中: " + multi.getSelectedIndices()));
        c.add(multi).left().row();

        // 工具栏风格
        c.add(new Label("ButtonGroup 工具栏风格(左对齐/居中/右对齐,不同颜色):", skin)).padTop(10).left().row();
        BsButtonGroup toolbar = new BsButtonGroup(skin, BsButtonGroup.Mode.SINGLE);
        toolbar.addToggle("左对齐", BsButton.Variant.SECONDARY);
        toolbar.addToggle("居中", BsButton.Variant.SECONDARY);
        toolbar.addToggle("右对齐", BsButton.Variant.SECONDARY);
        toolbar.select(0);
        toolbar.setOnChange(idx -> setStatus.accept("工具栏: " + idx));
        c.add(toolbar).left().row();

        // Alert 6 色
        c.add(new Label("Alert 警告横条(6 色,可关闭):", skin)).padTop(14).left().row();
        for (BsAlert.Variant v : BsAlert.Variant.values()) {
            BsAlert alert = new BsAlert(skin, alertMessage(v), v);
            alert.setDismissible(true);
            alert.setOnClose(() -> setStatus.accept("关闭 Alert: " + v));
            c.add(alert).width(520).growX().padTop(4).row();
        }

        // Alert 带标题
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

        c.add(new Label("(Alert 是页面内静态横条，不阻断操作；区别于对话框模态遮罩)",
                skin)).padTop(8).row();
    }
}
