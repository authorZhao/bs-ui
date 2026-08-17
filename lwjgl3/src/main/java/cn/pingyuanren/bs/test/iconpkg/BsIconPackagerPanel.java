/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库
 * Copyright (c) 2026 bs-ui contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */

package cn.pingyuanren.bs.test.iconpkg;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import cn.pingyuanren.bs.ui.BsButton;
import cn.pingyuanren.bs.ui.BsForm;
import cn.pingyuanren.bs.ui.BsTextField;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Bootstrap Icons 转换工具的配置面板（基于 BsForm + 后台线程）。
 *
 * <p>用户在面板输入参数，点击"开始转换"，后台线程跑 {@link BootstrapIconPackager#pack}，
 * UI 线程定时刷新进度。完成后用回调通知业务方（让用户可以加载 atlas 显示图标）。</p>
 */
@Slf4j
public class BsIconPackagerPanel extends Table {

    private final Skin skin;
    private final BsTextField inputDirField;
    private final BsTextField outputDirField;
    private final BsTextField iconSizeField;
    private final BsTextField atlasSizeField;
    private final BsTextField paddingField;
    private final BsTextField includeField;   // 逗号分隔
    private final BsTextField excludeField;   // 逗号分隔
    private final BsTextField fillColorField;
    private final Label statusLabel;
    private final Label progressLabel;
    private final TextButton startBtn;

    /** 转换完成回调（在 UI 线程触发）。 */
    private Runnable onComplete;
    /** 后台转换线程；null=未在跑。 */
    private Thread worker;
    /** 跨线程进度共享（worker 写、UI 读）。 */
    private final AtomicReference<BootstrapIconPackager.Progress> progressRef = new AtomicReference<>();
    /** 转换结束标志。 */
    private volatile boolean done;
    private volatile boolean failed;
    private volatile String failMsg;

    public BsIconPackagerPanel(Skin skin) {
        this.skin = skin;
        left().top();
        defaults().pad(4).left();

        add(new Label("Bootstrap Icons → libgdx atlas 转换工具", skin)).padBottom(8).row();

        // 表单（参数输入）
        BsForm form = new BsForm(skin, 110, 320, 0);
        form.defaults().pad(3).left();

        inputDirField = new BsTextField("E:/idea/workspace2/test/icons", skin);
        form.addField("SVG 输入目录", inputDirField);

        outputDirField = new BsTextField("bs/icons", skin);
        form.addField("输出目录", outputDirField,
                v -> (v == null || v.isEmpty()) ? "必填" : null);

        iconSizeField = new BsTextField("32", skin);
        iconSizeField.setTextFieldFilter((f, c) -> Character.isDigit(c));
        form.addField("图标尺寸(px)", iconSizeField,
                v -> {
                    try { int n = Integer.parseInt(v); return n >= 8 && n <= 512 ? null : "8~512"; }
                    catch (Exception e) { return "必须是整数"; }
                });

        atlasSizeField = new BsTextField("1024", skin);
        atlasSizeField.setTextFieldFilter((f, c) -> Character.isDigit(c));
        form.addField("atlas 大小(px)", atlasSizeField,
                v -> {
                    try { int n = Integer.parseInt(v); return (n == 256 || n == 512 || n == 1024 || n == 2048 || n == 4096) ? null : "256/512/1024/2048/4096"; }
                    catch (Exception e) { return "必须 2 的幂"; }
                });

        paddingField = new BsTextField("2", skin);
        paddingField.setTextFieldFilter((f, c) -> Character.isDigit(c));
        form.addField("padding(px)", paddingField);

        fillColorField = new BsTextField("#FFFFFF", skin);
        form.addField("染色(hex)", fillColorField,
                v -> (v == null || v.isEmpty() || v.startsWith("#") && v.length() == 7) ? null : "#RRGGBB 或空");

        includeField = new BsTextField("", skin);
        form.addField("只包含(逗号分隔)", includeField);

        excludeField = new BsTextField("", skin);
        form.addField("排除(逗号分隔)", excludeField);

        add(form).growX().row();

        // 开始按钮 + 进度
        Table actionRow = new Table();
        actionRow.defaults().pad(4);
        actionRow.left();

        startBtn = new BsButton("开始转换", skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.MD);
        startBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { onStartClick(); }
        });
        actionRow.add(startBtn).width(120).height(34);

        progressLabel = new Label("", skin);
        progressLabel.setColor(new Color(0.2f, 0.5f, 0.9f, 1f));
        actionRow.add(progressLabel).padLeft(12).left().growX();
        add(actionRow).growX().padTop(8).row();

        // 状态行
        statusLabel = new Label("(待开始)", skin);
        statusLabel.setColor(new Color(0.4f, 0.4f, 0.45f, 1f));
        statusLabel.setWrap(true);
        add(statusLabel).growX().padTop(6).row();

        add(new Label("转换可能需要几分钟（2000+ SVG），转换期间界面会显示进度，请耐心等待。",
                skin)).padTop(8).row();
    }

    /** 注册转换完成回调（在 UI 线程触发）。 */
    public void setOnComplete(Runnable r) { this.onComplete = r; }

    /** act 中检查后台进度并刷新 UI（业务方要把这个面板加到 stage 上，stage 会自动调 act）。 */
    @Override
    public void act(float delta) {
        super.act(delta);
        // 刷新进度
        BootstrapIconPackager.Progress p = progressRef.get();
        if (p != null && !done) {
            progressLabel.setText(String.format("[%d/%d] %s", p.done, p.total, p.current));
        }
        // 完成检查
        if (done) {
            done = false;  // 重置标志
            startBtn.setDisabled(false);
            startBtn.setText("开始转换");
            if (failed) {
                statusLabel.setColor(new Color(0xDC / 255f, 0x35 / 255f, 0x45 / 255f, 1f));
                statusLabel.setText("✗ 转换失败: " + failMsg);
                progressLabel.setText("");
            } else {
                statusLabel.setColor(new Color(0x19 / 255f, 0x87 / 255f, 0x54 / 255f, 1f));
                statusLabel.setText("✓ 转换完成。可以用 BsIcon.load(\"" + outputDirField.getText()
                        + "/bootstrap-icons.atlas\") 加载图标了。");
                progressLabel.setText("");
                if (onComplete != null) {
                    try { onComplete.run(); } catch (Throwable t) { log.warn("onComplete", t); }
                }
            }
        }
    }

    /** 点击开始转换。 */
    private void onStartClick() {
        if (worker != null && worker.isAlive()) {
            statusLabel.setText("正在转换中，请等待...");
            return;
        }

        // 简单校验
        try {
            Integer.parseInt(iconSizeField.getText().trim());
            Integer.parseInt(atlasSizeField.getText().trim());
        } catch (Exception e) {
            statusLabel.setColor(new Color(0xDC / 255f, 0x35 / 255f, 0x45 / 255f, 1f));
            statusLabel.setText("✗ 图标尺寸/atlas 大小必须是数字");
            return;
        }

        // 构造配置
        BootstrapIconPackager.PackConfig cfg = BootstrapIconPackager.PackConfig.builder()
                .inputDir(inputDirField.getText().trim())
                .outputDir(outputDirField.getText().trim())
                .iconSize(Integer.parseInt(iconSizeField.getText().trim()))
                .atlasSize(Integer.parseInt(atlasSizeField.getText().trim()))
                .padding(Integer.parseInt(paddingField.getText().trim()))
                .fillColor(fillColorField.getText().trim().isEmpty() ? null : fillColorField.getText().trim())
                .includeFilter(parseList(includeField.getText()))
                .excludeFilter(parseList(excludeField.getText()))
                .build();

        // 启动后台线程
        done = false;
        failed = false;
        failMsg = null;
        progressRef.set(new BootstrapIconPackager.Progress(0, 1, "准备中..."));
        startBtn.setDisabled(true);
        startBtn.setText("转换中...");
        statusLabel.setColor(new Color(0.4f, 0.4f, 0.45f, 1f));
        statusLabel.setText("开始转换...");

        worker = new Thread(() -> {
            try {
                BootstrapIconPackager.pack(cfg, p -> progressRef.set(p));
                Gdx.app.postRunnable(() -> { done = true; failed = false; });
            } catch (Throwable t) {
                log.error("BootstrapIconPackager 后台转换异常", t);
                failMsg = t.getMessage();
                Gdx.app.postRunnable(() -> { done = true; failed = true; });
            }
        }, "BsIconPackager-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    private static java.util.List<String> parseList(String text) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) return list;
        for (String s : text.split("[,，\\s]+")) {
            String t = s.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }
}
