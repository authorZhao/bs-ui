/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库。
 * Copyright (c) 2026 bs-ui contributors
 *
 * 基于 Apache License 2.0 开源，允许商用、修改和再分发。
 * 使用本库的产品须在“关于”界面标注本项目，详见 LICENSE。
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package com.git.bs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.git.bs.i18n.BsI18n;
import lombok.extern.slf4j.Slf4j;

/**
 * "关于" 弹窗（基于 {@link BsModal}）。
 *
 * <p>本类存在的核心理由是 <b>满足 bs-ui 的 License 附加条件</b>：任何把 bs-ui 直接使用
 * 或修改后使用、并分发给最终用户的产品，<b>必须在程序内的 About 页面清晰声明</b>
 * 库名、是否修改、版权 / 上游链接。本组件把这段声明预填好，使用方一行代码即可合规。</p>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * // 最简：仅展示 bs-ui 合规声明（modified=false 表示未改源码）
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
 * <p>本组件只负责把 <b>bs-ui 自身的声明</b> 渲染出来，方便使用方履行 License 义务。
 * 使用方追加的额外内容（自家产品信息、其他依赖）通过 {@link #appendLine} / {@link #appendSection} 添加。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsAboutDialog extends BsModal {

    /** bs-ui 版权行（License 附加条件要求展示的最低限度信息之一）。 */
    public static final String BS_UI_COPYRIGHT = "Copyright (c) 2026 bs-ui contributors";
    /** bs-ui 库名（License 附加条件要求展示的最低限度信息之一）。 */
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

    /** 声明是否修改过 bs-ui 源码（License 要求必须标注）。默认 false。 */
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
            Label h = new Label(heading, skin);
            h.setColor(BsTheme.tp());
            h.setFontScale(1.05f);
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

    /** 静态便捷入口：显示只含 bs-ui 合规声明的最小 About 弹窗。 */
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

        // 2. bs-ui 合规声明（License 强制要求，不可省）——用 BsText 取清晰烘焙字体，不缩放
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
