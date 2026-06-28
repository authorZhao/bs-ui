package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
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
 */
@Slf4j
public class BsAboutDialog extends BsModal {

    /** bs-ui 版权行（License 附加条件要求展示的最低限度信息之一）。 */
    public static final String BS_UI_COPYRIGHT = "Copyright (c) 2026 bs-ui contributors";
    /** bs-ui 库名（License 附加条件要求展示的最低限度信息之一）。 */
    public static final String BS_UI_NAME = "bs-ui";
    /** bs-ui 上游项目链接占位（如有正式 home URL，替换此常量即可全局生效）。 */
    public static final String BS_UI_HOME_URL = "https://github.com/bs-ui/bs-ui";

    /** 默认宽度。 */
    private static final float DEFAULT_WIDTH = 420;

    private final Skin skin;
    private final Table contentTable;
    private String productName;
    private String productVersion;
    private boolean modified = false;

    public BsAboutDialog(Skin skin) {
        super("关于", skin);
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
        addButton("确定", null, BsButton.Variant.PRIMARY, BsButton.Style.SOLID);
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

        // 1. 产品名 + 版本（可选）
        if (productName != null && !productName.isEmpty()) {
            String title = productVersion != null && !productVersion.isEmpty()
                    ? productName + "  v" + productVersion
                    : productName;
            Label p = new Label(title, skin);
            p.setColor(BsTheme.tp());
            p.setFontScale(1.3f);
            contentTable.add(p).growX().padBottom(8).row();
        }

        // 2. bs-ui 合规声明（License 强制要求，不可省）
        Label libLine = new Label("This product uses " + BS_UI_NAME
                + (modified ? " (modified)" : " (unmodified)"), skin);
        libLine.setColor(BsTheme.ts());
        libLine.setWrap(true);
        contentTable.add(libLine).growX().padTop(6).row();

        Label copyLine = new Label(BS_UI_COPYRIGHT, skin);
        copyLine.setColor(BsTheme.ts());
        contentTable.add(copyLine).growX().row();

        Label urlLine = new Label(BS_UI_HOME_URL, skin);
        Color linkColor;
        try {
            linkColor = skin.has("bs-link", Color.class)
                    ? skin.get("bs-link", Color.class)
                    : BsPalette.PRIMARY.getMain();
        } catch (Throwable t) {
            linkColor = BsPalette.PRIMARY.getMain();
        }
        urlLine.setColor(linkColor);
        contentTable.add(urlLine).growX().padBottom(4).row();

        // 3. License 提示（让最终用户知道协议类型）
        Label licLine = new Label("Licensed under the Apache License 2.0 "
                + "(with additional attribution condition)", skin);
        licLine.setColor(BsTheme.tm());
        licLine.setWrap(true);
        contentTable.add(licLine).growX().row();

        content(contentTable);
    }
}
