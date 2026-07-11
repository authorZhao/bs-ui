package com.git.bs.winsettings;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.git.bs.i18n.BsI18n;
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

    /** 当前语言对应的下拉显示标签（"中文"/"English"）。 */
    private static String currentLangLabel() {
        return "zh_cn".equals(BsI18n.currentLocale()) ? "中文" : "English";
    }

    /** 弹出重命名对话框：输入新设备名，确认后回显到设备名标签。 */
    private void showRenameDialog() {
        com.badlogic.gdx.scenes.scene2d.Stage stage =
                (com.badlogic.gdx.scenes.scene2d.Stage) com.badlogic.gdx.Gdx.input.getInputProcessor();
        if (stage == null) {
            log.warn("[主页] 无法获取 stage，重命名对话框取消");
            return;
        }
        final com.git.bs.ui.BsTextField nameField = new com.git.bs.ui.BsTextField(deviceName, skin);
        com.badlogic.gdx.scenes.scene2d.ui.Table form = new com.badlogic.gdx.scenes.scene2d.ui.Table(skin);
        form.pad(10);
        form.defaults().pad(6).left().growX();
        form.add(new BsText(BsI18n.get("home.rename_prompt"), BsText.Size.SM, BsText.Variant.MUTED)).row();
        form.add(nameField).growX().row();

        final com.git.bs.ui.BsModal modal = new com.git.bs.ui.BsModal(BsI18n.get("home.rename"), skin);
        modal.content(form).contentWidth(360).separator(true);
        modal.addButton(BsI18n.get("home.cancel"), modal::close,
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        modal.addButton(BsI18n.get("home.confirm"), () -> {
                    String newName = nameField.getText().trim();
                    if (!newName.isEmpty()) {
                        deviceName = newName;
                        deviceNameText.setText(newName);
                        log.info("[主页] 设备重命名为 {}", newName);
                    }
                    modal.close();
                },
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID);
        modal.showModal(stage);
    }

    private BsButton outlineBtn(String text, Runnable r) {
        BsButton b = new BsButton(text, skin, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.SM);
        b.addListener(click(r));
        return b;
    }

    // ---------- 各卡片 ----------

    /** 当前设备名（点击重命名弹框改写）。 */
    private String deviceName = BsI18n.get("home.device_name");
    /** 设备名显示控件引用（重命名后 setText 刷新）。 */
    private BsText deviceNameText;

    /** 顶部用户/设备卡：锁屏图 + 设备名 + 重命名 + 以太网 / Windows 更新状态。 */
    private Actor buildUserCard() {
        Table card = card();
        Table lock = new Table();
        lock.setBackground(com.git.bs.ui.BsSkinFactory.drawableOf(BsTheme.tm()));
        lock.defaults().center();
        lock.add(new BsText(BsI18n.get("home.lock_screen"), BsText.Size.SM, BsText.Variant.MUTED));
        card.add(lock).width(160).height(90).padRight(16).top();

        Table right = new Table();
        right.left().top();
        right.defaults().left();
        Table nameRow = new Table();
        nameRow.left();
        nameRow.defaults().left();
        deviceNameText = new BsText(deviceName, BsText.Size.LG).bold();
        nameRow.add(deviceNameText).padRight(10);
        nameRow.add(outlineBtn(BsI18n.get("home.rename"), this::showRenameDialog));
        right.add(nameRow).row();

        Table status = new Table();
        status.left();
        status.defaults().left().padRight(24);
        status.add(new BsText(BsI18n.get("home.network_connected"), BsText.Size.SM, BsText.Variant.SECONDARY));
        status.add(new BsText(BsI18n.get("home.update_latest"), BsText.Size.SM, BsText.Variant.SUCCESS));
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
        col.add(new BsText(BsI18n.get("home.ms_account_title"), BsText.Size.DEFAULT).bold()).row();
        col.add(new BsText(BsI18n.get("home.ms_account_desc"), BsText.Size.SM, BsText.Variant.MUTED)).padTop(3).row();
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
        head.add(new BsText(BsI18n.get("home.recommended_title"), BsText.Size.DEFAULT).bold()).padRight(12);
        head.add(new BsText(BsI18n.get("home.recommended_desc"), BsText.Size.SM, BsText.Variant.MUTED));
        col.add(head).padBottom(4).row();

        // 一行一行：{图标, 标题key, 描述key, 跳转 key}，用 WinRow.nav（hover 微亮 + 箭头 + 点击跳转）
        String[][] items = {
                {"⚡", "home.power_title",       "home.power_desc",       "system"},
                {"🔊", "home.sound_title",      "home.sound_desc",      "system"},
                {"🔒", "home.lockscreen_title", "home.lockscreen_desc", "personalization"},
                {"🖥", "home.display_title",    "home.display_desc",    "system"},
                {"🎨", "home.personalization_title", "home.personalization_desc", "personalization"},
                {"📡", "home.bluetooth_title",  "home.bluetooth_desc",  "bluetooth"},
                {"📱", "home.apps_title",       "home.apps_desc",       "apps"},
                {"🔄", "home.update_title",     "home.update_desc",     "update"},
        };
        for (String[] it : items) {
            final String navKey = it[3];
            col.add(WinRow.nav(skin, it[0], BsI18n.get(it[1]), BsI18n.get(it[2]),
                    () -> router.navigate(navKey))).growX().row();
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
        col.add(new BsText(BsI18n.get("home.bluetooth_card_title"), BsText.Size.DEFAULT).bold()).row();
        col.add(new BsText(BsI18n.get("home.bluetooth_card_desc"), BsText.Size.SM, BsText.Variant.MUTED))
                .padTop(3).padBottom(8).row();
        col.add(new BsText(BsI18n.get("home.bluetooth_devices"), BsText.Size.SM, BsText.Variant.SECONDARY)).row();
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
        col.add(new BsText(BsI18n.get("home.personalization_card_title"), BsText.Size.DEFAULT).bold()).padBottom(10).row();

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
            thumb.setBackground(com.git.bs.ui.BsSkinFactory.drawableOf(cs[i]));
            thumb.setTouchable(Touchable.enabled);
            thumb.addListener(click(() -> log.info("[个性化] 选择背景 {}", idx + 1)));
            thumbs.add(thumb).size(84, 52);
        }
        col.add(thumbs).padBottom(12).row();

        // 色彩模式
        Table modeRow = new Table();
        modeRow.left();
        modeRow.defaults().left();
        modeRow.add(new BsText(BsI18n.get("home.color_mode"), BsText.Size.DEFAULT)).padRight(10);
        BsSelectBox<String> mode = new BsSelectBox<>(skin);
        mode.setItems(BsI18n.get("home.color_mode_light"), BsI18n.get("home.color_mode_dark"), BsI18n.get("home.color_mode_custom"));
        mode.setSelected(BsUI.currentTheme().isDark()
                ? BsI18n.get("home.color_mode_dark") : BsI18n.get("home.color_mode_light"));   // 初始跟随当前主题
        mode.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                String m = mode.getSelected();
                log.info("[个性化] 色彩模式 = {}", m);
                // 真实换肤：选亮/暗 → BsUI.setTheme → App 监听器重建 screen
                if (BsI18n.get("home.color_mode_dark").equals(m)) BsUI.setTheme(BsDarkTheme.INSTANCE);
                else if (BsI18n.get("home.color_mode_light").equals(m)) BsUI.setTheme(BsLightTheme.INSTANCE);
            }
        });
        modeRow.add(mode);
        col.add(modeRow).padBottom(10).row();

        // 语言切换（验证 i18n：选语言 → BsI18n.setLocale → App 监听器重建 screen）
        Table langRow = new Table();
        langRow.left();
        langRow.defaults().left();
        langRow.add(new BsText(BsI18n.get("home.language"), BsText.Size.DEFAULT)).padRight(10);
        BsSelectBox<String> lang = new BsSelectBox<>(skin);
        lang.setItems("中文", "English");
        lang.setSelected("中文".equals(currentLangLabel()) ? "中文" : "English");
        lang.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                String sel = lang.getSelected();
                String locale = "中文".equals(sel) ? "zh_cn" : "en_us";
                log.info("[个性化] 语言 = {}", locale);
                BsI18n.setLocale(locale);
            }
        });
        langRow.add(lang);
        col.add(langRow).padBottom(10).row();

        // 链接
        Table links = new Table();
        links.left();
        links.defaults().left().padRight(24);
        BsLink browseBg = new BsLink(BsI18n.get("home.browse_backgrounds"), skin);
        browseBg.addListener(click(() -> router.navigate("personalization")));
        BsLink colorTheme = new BsLink(BsI18n.get("home.colors_and_theme"), skin);
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
        BsLink help = new BsLink(BsI18n.get("home.help"), skin);
        help.addListener(click(() -> log.info("[主页] 获取帮助")));
        BsLink feedback = new BsLink(BsI18n.get("home.feedback"), skin);
        feedback.addListener(click(() -> log.info("[主页] 提供反馈")));
        row.add(help);
        row.add(feedback);
        return row;
    }
}
