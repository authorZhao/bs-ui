package com.git.bs.admin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.git.bs.game.AdminApp;
import com.git.bs.ui.BsAlertDialog;
import com.git.bs.ui.BsButton;
import com.git.bs.ui.BsTextField;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsToast;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin 模板登录页：
 * 居中 BsCard 内放标题 + 用户名/密码输入框 + 登录/重置按钮（全部居中），
 * 校验 admin/123456 成功后进入 {@link BsAdminShell}，失败弹 {@link BsAlertDialog}。
 * 登录态仅内存（{@link AdminContext}），不持久化。
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class AdminLoginScreen extends ScreenAdapter {

    public static final int WIN_W = 1920;
    public static final int WIN_H = 1080;

    private final AdminApp app;
    private final Skin skin;
    private final Stage stage;

    private BsTextField userField;
    private BsTextField pwdField;
    /** 登录页背景图纹理（dispose 时释放）。 */
    private Texture bgTexture;

    public AdminLoginScreen(AdminApp app) {
        this.app = app;
        this.skin = app.getSkin();
        this.stage = new Stage(new ScreenViewport());
        buildLayout();
    }

    private void buildLayout() {
        Table root = new Table();
        root.setFillParent(true);
        // 加载背景图作为 root 背景（Table.setBackground 会自动拉伸铺满）
        //bgTexture = new Texture(Gdx.files.internal("img/hei_wu_kong.jpg"));
        //root.setBackground(new TextureRegionDrawable(bgTexture));
        root.top().center();

        // 顶部品牌区（大屏下不显空旷）
        Table brand = new Table();
        brand.center();
        brand.defaults().pad(4).center();
        Label.LabelStyle xlStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        xlStyle.font = skin.getFont("font-xl");
        Label.LabelStyle lgStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        lgStyle.font = skin.getFont("font-lg");
        Label brandTitle = new Label("bs-ui Admin", xlStyle);
        brandTitle.setColor(BsTheme.tp());
        Label brandSub = new Label("管理后台模板 · Admin Template", lgStyle);
        brandSub.setColor(BsTheme.ts());
        brand.add(brandTitle).row();
        brand.add(brandSub).padTop(8).row();

        // 登录卡片：直接用 Table 控制布局，柔和浅色背景（不那么刺眼的白）
        Table card = new Table();
        card.setBackground(skin.newDrawable("white",
                new com.badlogic.gdx.graphics.Color(0.97f, 0.975f, 0.98f, 1f)));
        card.pad(28);
        card.center();

        // 居中容器：标题 + 副标题 + 字段 + 按钮
        Table center = new Table();
        center.center();
        center.defaults().center();

        Label cardTitle = new Label("欢迎登录", xlStyle);
        cardTitle.setColor(BsTheme.tp());
        Label cardSub = new Label("请输入账号密码", skin);
        cardSub.setColor(BsTheme.tm());
        center.add(cardTitle).row();
        center.add(cardSub).padTop(4).padBottom(20).row();

        // 字段：用户名 / 密码（label 居右、输入框居左，整行居中）
        float fieldW = 320f;
        float labelW = 70f;

        userField = new BsTextField("", skin);
        userField.setMessageText("请输入用户名");
        center.add(fieldRow(skin, "用户名", userField, labelW, fieldW)).padBottom(10).row();

        pwdField = new BsTextField("", skin);
        pwdField.setMessageText("请输入密码");
        pwdField.setPasswordMode(true);
        pwdField.setPasswordCharacter('*');
        center.add(fieldRow(skin, "密码", pwdField, labelW, fieldW)).padBottom(18).row();

        // 按钮栏（登录/重置 居中）
        BsButton loginBtn = new BsButton("登录", skin,
                BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        loginBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y) {
                doLogin();
            }
        });
        BsButton resetBtn = new BsButton("重置", skin,
                BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE, BsButton.Size.MD);
        resetBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y) {
                userField.setText("");
                pwdField.setText("");
            }
        });
        Table btnBar = new Table();
        btnBar.defaults().pad(0, 10, 0, 10).center();
        btnBar.add(loginBtn);
        btnBar.add(resetBtn);
        center.add(btnBar).padTop(4).row();

        card.add(center).grow();

        // 底部提示
        Label hint = new Label("演示账号：admin / 123456", lgStyle);
        hint.setColor(BsTheme.tm());

        // 组装：品牌区 + 卡片 + 提示，整体居中、留白合理
        Table cardWrap = new Table();
        cardWrap.defaults().pad(8).center();
        cardWrap.add(card).width(620).height(340).row();
        cardWrap.add(hint).padTop(18).center();

        root.add(brand).padTop(80).padBottom(28).row();
        root.add(cardWrap);
        stage.addActor(root);
    }

    /** 一行字段：label（居右） + 输入框（居左），整行用 Table 居中容纳。 */
    private Table fieldRow(Skin skin, String labelText, BsTextField field, float labelW, float fieldW) {
        Table row = new Table();
        row.center();
        row.defaults().center().pad(0, 6, 0, 6);
        Label l = new Label(labelText, skin);
        l.setColor(BsTheme.tp());
        l.setAlignment(com.badlogic.gdx.utils.Align.right);
        row.add(l).width(labelW).right();
        row.add(field).width(fieldW).left();
        return row;
    }

    private void doLogin() {
        String u = userField.getText();
        String p = pwdField.getText();
        if (u.isEmpty() || p.isEmpty()) {
            BsAlertDialog.show(stage, skin, BsAlertDialog.Level.WARNING, "提示", "请填写用户名和密码");
            return;
        }
        if (AdminContext.get().check(u, p)) {
            AdminContext.get().login(u);
            BsToast.show(stage, skin, "登录成功", BsToast.Variant.SUCCESS, 1.2f);
            // 下一帧切屏，避免在事件回调里直接切
            Gdx.app.postRunnable(() -> {
                try {
                    app.setScreen(new BsAdminShell(app));
                } catch (Throwable t) {
                    log.error("登录跳转失败", t);
                }
            });
        } else {
            BsAlertDialog.show(stage, skin, BsAlertDialog.Level.ERROR, "登录失败", "账号或密码错误");
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(BsTheme.bb());
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
        if (bgTexture != null) bgTexture.dispose();
    }
}
