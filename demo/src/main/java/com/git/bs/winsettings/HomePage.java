package com.git.bs.winsettings;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsDarkTheme;
import com.git.bs.ui.BsLightTheme;
import com.git.bs.ui.BsLink;
import com.git.bs.ui.BsSelectBox;
import com.git.bs.ui.BsText;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

/**
 * Win11 设置主页（推荐流，参考真实 Win11 23H2 主页结构）。
 *
 * <p>从上到下：</p>
 * <ol>
 *   <li>顶部用户/设备卡：锁屏图 + 设备名 + 重命名 + 以太网 / Windows 更新状态</li>
 *   <li>Microsoft 账户卡：标题 + 简介（点击跳账户页）</li>
 *   <li><b>推荐设置卡</b>：标题行「推荐设置 · 最近使用的和常用的设置」+ 一行一行设置项（{@link WinRow}），
 *       hover 微亮、点击跳转</li>
 *   <li>蓝牙和其他设备卡（点击跳蓝牙页）</li>
 *   <li>个性化卡：背景缩略图 + 色彩模式 + 浏览更多背景/颜色和主题</li>
 *   <li>底部：获取帮助 / 提供反馈</li>
 * </ol>
 */
@Slf4j
public class HomePage extends SettingsPage {

    public HomePage(Skin skin) {
        super(skin);
    }

    @Override
    public Actor buildView(Router router) {
        Table col = new Table();
        col.top().left();
        col.defaults().growX().left().top().padBottom(12);
        col.add(buildUserCard()).row();
        col.add(buildMsAccountCard(router)).row();
        col.add(buildRecommendedCard(router)).row();
        col.add(buildBluetoothCard(router)).row();
        col.add(buildPersonalizationCard(router)).row();
        col.add(buildFooterLinks()).padTop(4).row();
        return col;
    }

    // ---------- 通用工具 ----------

    private Table card() {
        Table t = new Table();
        t.setBackground(skin.getDrawable("bs-window-bg"));
        t.pad(16).left().top();
        t.defaults().left().top();
        return t;
    }

    private ClickListener click(Runnable r) {
        return new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { r.run(); }
        };
    }

    private BsButton outlineBtn(String text, Runnable r) {
        BsButton b = new BsButton(text, skin, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        b.addListener(click(r));
        return b;
    }

    // ---------- 各卡片 ----------

    /** 顶部用户/设备卡：锁屏图 + 设备名 + 重命名 + 以太网 / Windows 更新状态。 */
    private Actor buildUserCard() {
        Table card = card();
        Table lock = new Table();
        lock.setBackground(skin.newDrawable("white", BsTheme.tm()));
        lock.defaults().center();
        lock.add(new BsText("锁屏图片", BsText.Size.SM, BsText.Variant.MUTED));
        card.add(lock).width(160).height(90).padRight(16).top();

        Table right = new Table();
        right.left().top();
        right.defaults().left();
        Table nameRow = new Table();
        nameRow.left();
        nameRow.defaults().left();
        nameRow.add(new BsText("DESKTOP-BSUI", BsText.Size.LG).bold()).padRight(10);
        nameRow.add(outlineBtn("重命名", () -> log.info("[主页] 重命名设备")));
        right.add(nameRow).row();

        Table status = new Table();
        status.left();
        status.defaults().left().padRight(24);
        status.add(new BsText("🌐 以太网  已连接", BsText.Size.SM, BsText.Variant.SECONDARY));
        status.add(new BsText("⟳ Windows 更新  最新", BsText.Size.SM, BsText.Variant.SUCCESS));
        right.add(status).padTop(10).row();
        card.add(right).growX().top();
        return card;
    }

    /** Microsoft 账户卡：纯介绍（点击跳账户页）。 */
    private Actor buildMsAccountCard(Router router) {
        Table card = card();
        Table col = new Table();
        col.left().top();
        col.defaults().growX().left().top();
        col.add(new BsText("所有功能尽在 Microsoft 账户", BsText.Size.DEFAULT).bold()).row();
        col.add(new BsText("使用 Microsoft 账户登录以同步设置、文件和首选项", BsText.Size.SM, BsText.Variant.MUTED)).padTop(3).row();
        card.add(col).growX();
        card.setTouchable(Touchable.enabled);
        card.addListener(click(() -> router.navigate("accounts")));
        return card;
    }

    /** 推荐设置卡：标题行「推荐设置 · 最近使用的和常用的设置」+ 一行一行（WinRow，hover 微亮）。 */
    private Actor buildRecommendedCard(Router router) {
        Table card = card();
        Table col = new Table();
        col.left().top();
        col.defaults().growX().left().top();

        // 标题行
        Table head = new Table();
        head.left();
        head.defaults().left();
        head.add(new BsText("推荐设置", BsText.Size.DEFAULT).bold()).padRight(12);
        head.add(new BsText("最近使用的和常用的设置", BsText.Size.SM, BsText.Variant.MUTED));
        col.add(head).padBottom(4).row();

        // 一行一行：{图标, 标题, 描述, 跳转 key}，用 WinRow.nav（hover 微亮 + 箭头 + 点击跳转）
        String[][] items = {
                {"⚡", "电源和电池",    "管理电源模式、屏幕和睡眠",       "system"},
                {"🔊", "声音",         "输出/输入设备、音量、空间音频",  "system"},
                {"🔒", "锁屏界面",      "锁屏背景、屏幕保护、状态",       "personalization"},
                {"🖥", "显示",         "分辨率、缩放、夜间模式",         "system"},
                {"🎨", "个性化",       "背景、颜色、主题、字体",         "personalization"},
                {"ⓑ", "蓝牙和其他设备", "管理已连接设备、鼠标、键盘",     "bluetooth"},
                {"⊞", "已安装的应用",   "添加或删除应用、默认应用",       "apps"},
                {"⟳", "Windows 更新",  "检查更新、更新历史、暂停",       "update"},
        };
        for (String[] it : items) {
            final String key = it[3];
            col.add(WinRow.nav(skin, it[0], it[1], it[2], () -> router.navigate(key))).growX().row();
        }
        card.add(col).growX();
        return card;
    }

    /** 蓝牙和其他设备卡：摘要 + 点击跳蓝牙页。 */
    private Actor buildBluetoothCard(Router router) {
        Table card = card();
        Table col = new Table();
        col.left().top();
        col.defaults().growX().left().top();
        col.add(new BsText("蓝牙和其他设备", BsText.Size.DEFAULT).bold()).row();
        col.add(new BsText("管理已连接设备、鼠标、键盘、笔、自动播放", BsText.Size.SM, BsText.Variant.MUTED))
                .padTop(3).padBottom(8).row();
        col.add(new BsText("● 蓝牙耳机   ● 鼠标   ● 键盘   已连接", BsText.Size.SM, BsText.Variant.SECONDARY)).row();
        card.add(col).growX();
        card.setTouchable(Touchable.enabled);
        card.addListener(click(() -> router.navigate("bluetooth")));
        return card;
    }

    /** 个性化卡：背景缩略图行 + 色彩模式 + 链接。 */
    private Actor buildPersonalizationCard(Router router) {
        Table card = card();
        Table col = new Table();
        col.left().top();
        col.defaults().growX().left().top();
        col.add(new BsText("个性化", BsText.Size.DEFAULT).bold()).padBottom(10).row();

        // 背景缩略图
        Table thumbs = new Table();
        thumbs.left();
        thumbs.defaults().padRight(6);
        Color[] cs = {
                new Color(0.30f, 0.50f, 0.85f, 1f),
                new Color(0.85f, 0.40f, 0.35f, 1f),
                new Color(0.30f, 0.70f, 0.45f, 1f),
                new Color(0.75f, 0.62f, 0.22f, 1f),
                new Color(0.55f, 0.35f, 0.75f, 1f),
        };
        for (int i = 0; i < cs.length; i++) {
            final int idx = i;
            Table thumb = new Table();
            thumb.setBackground(skin.newDrawable("white", cs[i]));
            thumb.setTouchable(Touchable.enabled);
            thumb.addListener(click(() -> log.info("[个性化] 选择背景 {}", idx + 1)));
            thumbs.add(thumb).size(84, 52);
        }
        col.add(thumbs).padBottom(12).row();

        // 色彩模式
        Table modeRow = new Table();
        modeRow.left();
        modeRow.defaults().left();
        modeRow.add(new BsText("色彩模式", BsText.Size.DEFAULT)).padRight(10);
        BsSelectBox<String> mode = new BsSelectBox<>(skin);
        mode.setItems("亮", "暗", "自定义");
        mode.setSelected(BsUI.currentTheme().isDark() ? "暗" : "亮");   // 初始跟随当前主题
        mode.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                String m = mode.getSelected();
                log.info("[个性化] 色彩模式 = {}", m);
                // 真实换肤：选亮/暗 → BsUI.setTheme → App 监听器重建 screen
                if ("暗".equals(m)) BsUI.setTheme(BsDarkTheme.INSTANCE);
                else if ("亮".equals(m)) BsUI.setTheme(BsLightTheme.INSTANCE);
            }
        });
        modeRow.add(mode);
        col.add(modeRow).padBottom(10).row();

        // 链接
        Table links = new Table();
        links.left();
        links.defaults().left().padRight(24);
        BsLink browseBg = new BsLink("浏览更多背景", skin);
        browseBg.addListener(click(() -> router.navigate("personalization")));
        BsLink colorTheme = new BsLink("颜色和主题", skin);
        colorTheme.addListener(click(() -> router.navigate("personalization")));
        links.add(browseBg);
        links.add(colorTheme);
        col.add(links).row();

        card.add(col).growX();
        return card;
    }

    /** 底部：获取帮助 / 提供反馈。 */
    private Actor buildFooterLinks() {
        Table row = new Table();
        row.left();
        row.defaults().left().padRight(24);
        BsLink help = new BsLink("获取帮助", skin);
        help.addListener(click(() -> log.info("[主页] 获取帮助")));
        BsLink feedback = new BsLink("提供反馈", skin);
        feedback.addListener(click(() -> log.info("[主页] 提供反馈")));
        row.add(help);
        row.add(feedback);
        return row;
    }
}
