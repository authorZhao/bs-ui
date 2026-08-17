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
package cn.pingyuanren.bs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import cn.pingyuanren.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * "关于" 弹窗（基于 {@link BsModal}）。
 *
 * <p>bs-ui 以 <b>Mozilla Public License 2.0</b> 开源：允许商用、修改、分发以及与
 * 私有代码组合；唯一核心义务是 <b>修改 bs-ui 源文件并对外分发时，须以 MPL 2.0
 * 提供这些文件的源码</b>。在程序内展示 About / 致谢页面<b>不是 MPL 2.0 的强制要求</b>，
 * 但展示库名与上游链接是对开源作者的友好致谢方式。本组件把这段信息预填好，
 * 使用方一行代码即可完成致谢。</p>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * // 最简：仅展示 bs-ui 致谢信息（modified=false 表示未改源码）
 * BsAboutDialog.show(stage, skin, "My App", false);
 *
 * // 完整：自定义产品名 + 版本 + 是否修改 + 追加其他依赖致谢
 * new BsAboutDialog(skin)
 *     .product("My App", "1.0.0")
 *     .modified(true)
 *     .appendLine("感谢 libGDX、VISUI、Bootstrap 等开源项目")
 *     .appendSection("开源依赖", "libGDX (Apache-2.0)", "VISUI (Apache-2.0)")
 *     .showModal(stage);
 * }</pre>
 *
 * <p>本组件只负责把 <b>bs-ui 自身的致谢信息</b> 渲染出来。
 * 使用方追加的额外内容（自家产品信息、其他依赖）通过 {@link #appendLine} / {@link #appendSection} 添加。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsAboutDialog extends BsModal {

    /** bs-ui 版权行（About 致谢信息）。 */
    public static final String BS_UI_COPYRIGHT = "Copyright (c) 2026 bs-ui contributors";
    /** bs-ui 库名（About 致谢信息）。 */
    public static final String BS_UI_NAME = "bs-ui";
    /** bs-ui 作者。 */
    public static final String BS_UI_AUTHOR = "authorZhao";
    /** bs-ui 上游项目链接（真实仓库地址，替换此常量即可全局生效）。 */
    public static final String BS_UI_HOME_URL = "https://github.com/authorZhao/bs-ui";

    /** 默认宽度。 */
    private static final float DEFAULT_WIDTH = 420;

    private final Skin skin;
    private final Table contentTable;
    private String productName;
    private String productVersion;
    private boolean modified = false;

    public BsAboutDialog(Skin skin) {
        super(BsI18n.get("core.about", "关于"), skin);
        this.skin = skin;
        contentTable = new Table(skin);
        contentTable.defaults().left().padTop(2).padBottom(2);
        contentWidth(DEFAULT_WIDTH);
        closeOnBackdrop(true);
    }

    /** 设置产品名 + 版本（顶部展示，可选）。 */
    public BsAboutDialog product(String name, String version) {
        this.productName = name;
        this.productVersion = version;
        return this;
    }

    /** 声明是否修改过 bs-ui 源码（分发修改版时按 MPL 2.0 须提供该文件源码，此处如实标注）。默认 false。 */
    public BsAboutDialog modified(boolean b) {
        this.modified = b;
        return this;
    }

    /** 追加一行普通文本（用于自家版权、其他依赖致谢等）。 */
    public BsAboutDialog appendLine(String text) {
        Label l = new Label(text, skin);
        l.setColor(BsTheme.ts());
        l.setWrap(true);
        l.setAlignment(Align.left);
        contentTable.add(l).growX().row();
        return this;
    }

    /** 追加一个带小标题的分节（用于"致谢"列表等）。 */
    public BsAboutDialog appendSection(String heading, String... lines) {
        if (heading != null && !heading.isEmpty()) {
            Label.LabelStyle hStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
            hStyle.font = skin.getFont("font-lg");
            Label h = new Label(heading, hStyle);
            h.setColor(BsTheme.tp());
            contentTable.add(h).growX().padTop(10).row();
        }
        for (String line : lines) {
            appendLine(line);
        }
        return this;
    }

    @Override
    public void showModal(Stage stage) {
        rebuildContent();
        addButton(BsI18n.get("btn.ok", "确定"), null, BsButton.Variant.PRIMARY, BsButton.Style.SOLID);
        super.showModal(stage);
    }

    /** 静态便捷入口：显示只含 bs-ui 致谢信息的最小 About 弹窗。 */
    public static BsAboutDialog show(Stage stage, Skin skin, String productName, boolean modified) {
        BsAboutDialog d = new BsAboutDialog(skin)
                .product(productName, null)
                .modified(modified);
        d.showModal(stage);
        return d;
    }

    // =================== 内部：组装内容 ===================

    private void rebuildContent() {
        contentTable.clearChildren();

        // 1. 产品名 + 版本（可选）——直接用最大号烘焙字体（XXL），不缩放，避免发虚
        if (productName != null && !productName.isEmpty()) {
            String title = productVersion != null && !productVersion.isEmpty()
                    ? productName + "  v" + productVersion
                    : productName;
            BsText p = new BsText(title, BsText.Size.XXL).bold();
            contentTable.add(p).growX().padBottom(8).row();
        }

        // 2. bs-ui 致谢信息（MPL 2.0 下为可选项）——用 BsText 取清晰烘焙字体，不缩放
        BsText libLine = new BsText(BsI18n.get("about.uses_lib", BS_UI_NAME)
                + " " + BsI18n.get(modified ? "about.modified" : "about.unmodified"),
                BsText.Size.MD, BsText.Variant.DEFAULT);
        libLine.setWrap(true);
        contentTable.add(libLine).growX().padTop(6).row();

        BsText copyLine = new BsText(BS_UI_COPYRIGHT, BsText.Size.SM, BsText.Variant.SECONDARY);
        contentTable.add(copyLine).growX().row();

        BsText authorLine = new BsText(BsI18n.get("about.author") + ": " + BS_UI_AUTHOR,
                BsText.Size.SM, BsText.Variant.SECONDARY);
        contentTable.add(authorLine).growX().row();

        // GitHub 链接：点击用系统浏览器打开（桌面端与 TeaVM/Web 端均可用）
        BsLink urlLink = new BsLink(BS_UI_HOME_URL, skin);
        urlLink.left();
        urlLink.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    Gdx.net.openURI(BS_UI_HOME_URL);
                } catch (Throwable t) {
                    log.warn("Failed to open URI: {}", BS_UI_HOME_URL, t);
                }
            }
        });
        contentTable.add(urlLink).growX().padBottom(4).row();

        // 3. License 提示（让最终用户知道协议类型）
        BsText licLine = new BsText(BsI18n.get("about.license_note"), BsText.Size.SM, BsText.Variant.MUTED);
        licLine.setWrap(true);
        contentTable.add(licLine).growX().row();

        content(contentTable);
    }
}
