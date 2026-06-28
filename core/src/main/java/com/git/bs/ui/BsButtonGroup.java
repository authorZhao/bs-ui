package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Bootstrap 5 风格按钮组（Button group）—— 多个按钮横向排成一组，
 * 支持 active 互斥切换（单选/多选）。用于工具栏、分段选择器（segmented）。
 *
 * <p>用法：</p>
 * <pre>{@code
 * // 单选模式（默认）：active 互斥
 * BsButtonGroup group = new BsButtonGroup(skin, BsButtonGroup.Mode.SINGLE);
 * group.addToggle("日");
 * group.addToggle("周");
 * group.addToggle("月");
 * group.select(1);            // 默认选"周"
 * group.setOnChange(idx -> setStatus("选: " + idx));
 * stage.addActor(group);
 *
 * // 多选模式
 * BsButtonGroup multi = new BsButtonGroup(skin, BsButtonGroup.Mode.MULTI);
 * }</pre>
 *
 * <p>实现：内部维护 List&lt;BsButton&gt; + BitSet 记录 active 状态。
 * active 视觉通过 setStyle 切换 {@code bs-btn-X}（SOLID）/ {@code bs-btn-outline-X}（OUTLINE）。
 * 每个 toggle 按钮构造时把它的 Variant 存进 userObject，便于 active 时查回。
 * </p>
 */
@Slf4j
public class BsButtonGroup extends Table {

    public enum Mode { SINGLE, MULTI }

    private final Mode mode;
    private final List<BsButton> buttons = new ArrayList<>();
    private final BitSet active = new BitSet();
    private IntConsumer onChange;

    public BsButtonGroup(Skin skin) {
        this(skin, Mode.SINGLE);
    }

    public BsButtonGroup(Skin skin, Mode mode) {
        this.mode = mode;
        left();
        defaults().pad(0);
    }

    /** 添加一个切换按钮（active 时变 PRIMARY 实心）。 */
    public BsButtonGroup addToggle(String label) {
        return addToggle(label, BsButton.Variant.PRIMARY);
    }

    /**
     * 添加一个切换按钮。
     * @param label 文本
     * @param activeVariant active 时的颜色
     */
    public BsButtonGroup addToggle(String label, BsButton.Variant activeVariant) {
        BsButton btn = new BsButton(label, BsUI.getSkin(), activeVariant, BsButton.Style.OUTLINE, BsButton.Size.SM);
        btn.setUserObject(activeVariant);   // active 时切换 SOLID 需要拿到 variant
        int index = buttons.size();
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggle(index);
            }
        });
        buttons.add(btn);
        add(btn).padRight(2);
        applyVisual(index);
        return this;
    }

    /**
     * 应用 active 状态到按钮 style。
     * <p>active = SOLID 实色（bs-btn-X）+ 白字；
     * inactive = 浅灰填充（bs-btn-group-inactive）+ 深灰字。</p>
     * <p>用 SOLID ↔ 浅灰填充的强对比让"互斥选中"一目了然，
     * 比 OUTLINE ↔ SOLID 更容易区分（OUTLINE 在选中前后都带蓝边）。</p>
     */
    private void applyVisual(int index) {
        if (index < 0 || index >= buttons.size()) return;
        BsButton btn = buttons.get(index);
        BsButton.Variant v = (BsButton.Variant) btn.getUserObject();
        if (v == null) v = BsButton.Variant.PRIMARY;
        Skin skin = BsUI.getSkin();
        try {
            if (active.get(index)) {
                // 选中：SOLID primary 色 + 白字
                btn.setStyle(skin.get("bs-btn-" + v.name().toLowerCase(),
                        com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle.class));
            } else {
                // 未选：浅灰填充 + 深灰字（独立 style，避免被 SOLID 的色块覆盖）
                com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle inactive =
                        new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle(
                                skin.getDrawable("bs-btn-group-inactive"),
                                skin.getDrawable("bs-menu-title-hover"),
                                skin.getDrawable("bs-menu-title-hover"),
                                skin.getFont("default"));
                inactive.fontColor = new com.badlogic.gdx.graphics.Color(0.35f, 0.35f, 0.4f, 1f);
                btn.setStyle(inactive);
            }
        } catch (Throwable ignored) {}
    }

    /** 切换指定索引按钮的 active 状态。 */
    public BsButtonGroup toggle(int index) {
        if (index < 0 || index >= buttons.size()) return this;
        boolean newState = !active.get(index);
        if (mode == Mode.SINGLE) {
            active.clear();
            active.set(index, newState);
            for (int i = 0; i < buttons.size(); i++) applyVisual(i);
            if (onChange != null) {
                int sel = newState ? index : -1;
                try { onChange.accept(sel); } catch (Throwable t) { log.warn("onChange", t); }
            }
        } else {
            active.set(index, newState);
            applyVisual(index);
            if (onChange != null) {
                try { onChange.accept(newState ? index : -1); } catch (Throwable t) { log.warn("onChange", t); }
            }
        }
        return this;
    }

    /** 单选模式下选中指定索引（active）。 */
    public BsButtonGroup select(int index) {
        if (mode != Mode.SINGLE) return this;
        active.clear();
        if (index >= 0 && index < buttons.size()) active.set(index);
        for (int i = 0; i < buttons.size(); i++) applyVisual(i);
        if (onChange != null) {
            try { onChange.accept(index); } catch (Throwable t) { log.warn("onChange", t); }
        }
        return this;
    }

    /** 获取当前 active 索引（SINGLE 模式；无 active 返回 -1）。 */
    public int getSelectedIndex() {
        if (mode != Mode.SINGLE) return -1;
        for (int i = active.nextSetBit(0); i >= 0; i = active.nextSetBit(i + 1)) {
            return i;
        }
        return -1;
    }

    /** MULTI 模式下获取所有 active 索引。 */
    public List<Integer> getSelectedIndices() {
        List<Integer> r = new ArrayList<>();
        for (int i = active.nextSetBit(0); i >= 0; i = active.nextSetBit(i + 1)) {
            r.add(i);
        }
        return r;
    }

    public BsButtonGroup setOnChange(IntConsumer cb) {
        this.onChange = cb;
        return this;
    }

    // ========================= active 视觉（应用在 addToggle 内） =========================

    public int size() { return buttons.size(); }

    public BsButton getButton(int index) {
        if (index < 0 || index >= buttons.size()) return null;
        return buttons.get(index);
    }
}
