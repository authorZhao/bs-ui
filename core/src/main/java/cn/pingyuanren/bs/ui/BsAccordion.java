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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap 5 风格手风琴（Accordion）—— 多个 {@link BsCollapse} 纵向堆叠，
 * 通常一次只展开一个（默认行为，可配置允许同时展开多个）。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsAccordion acc = new BsAccordion(skin);
 * acc.setSingleOpen(true);   // 一次只展开一个（默认）
 * acc.addSection("基本信息", new Label("内容1"));
 * acc.addSection("联系方式", new Label("内容2"));
 * acc.addSection("安全设置", new Label("内容3"));
 * stage.addActor(acc);
 * }</pre>
 *
 * <p>实现：内部维护 List&lt;BsCollapse&gt;，singleOpen=true 时展开任意一个会自动收起其他。
 * 视觉上每个 section 之间留 4px 间隙（Bootstrap accordion 通常无缝相连，但留间隙更易识别）。</p>
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class BsAccordion extends Table {

    private final List<BsCollapse> sections = new ArrayList<>();
    private boolean singleOpen = true;

    public BsAccordion(Skin skin) {
        top().left();
        defaults().growX();
    }

    /** 一次只允许展开一个 section（默认 true）。false = 可同时展开多个。 */
    public BsAccordion setSingleOpen(boolean single) {
        this.singleOpen = single;
        return this;
    }

    public boolean isSingleOpen() { return singleOpen; }

    /** 添加一节。 */
    public BsAccordion addSection(String title, Actor content) {
        BsCollapse c = new BsCollapse(BsUI.getSkin());
        c.setTitle(title);
        c.setContent(content);
        c.setOnToggle((src, expanded) -> {
            if (expanded && singleOpen) {
                // 收起其他
                for (BsCollapse other : sections) {
                    if (other != src && other.isExpanded()) {
                        other.setExpanded(false);
                    }
                }
            }
        });
        sections.add(c);
        add(c).growX().padBottom(4).row();
        return this;
    }

    /** 程序化展开指定 section（index 从 0 开始）。 */
    public BsAccordion expand(int index) {
        if (index < 0 || index >= sections.size()) return this;
        sections.get(index).setExpanded(true);
        return this;
    }

    /** 收起所有。 */
    public BsAccordion collapseAll() {
        for (BsCollapse c : sections) {
            c.setExpanded(false);
        }
        return this;
    }

    public int getSectionCount() { return sections.size(); }

    public BsCollapse getSection(int index) {
        if (index < 0 || index >= sections.size()) return null;
        return sections.get(index);
    }
}
