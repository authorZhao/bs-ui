package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * 单选按钮：基于 {@link CheckBox} + {@link ButtonGroup} 强制互斥。
 * <p>使用名为 {@code "radio"} 的 CheckBoxStyle —— BsSkinFactory 已生成对应的圆形图标
 * （未选=灰圆环，选中=灰圆环+蓝实心圆点），与方形 checkbox 视觉区分。</p>
 *
 * <p><b>互斥组的建立</b>：本类不持有 static 单例（旧版本 static GROUP 会导致多屏共享、
 * 切屏后旧按钮残留）。请用 {@link BsRadioButtonGroup} 显式管理互斥：</p>
 * <pre>{@code
 * BsRadioButtonGroup group = new BsRadioButtonGroup();
 * BsRadioButton r1 = group.add(new BsRadioButton("A", skin));
 * BsRadioButton r2 = group.add(new BsRadioButton("B", skin));
 * }</pre>
 * <p>未加入任何 group 的 BsRadioButton 不参与互斥（独立可勾选，行为等同 CheckBox）。</p>
 */
public class BsRadioButton extends CheckBox {

    public BsRadioButton(String text, Skin skin) {
        super(text, skin, "radio");
    }

    public BsRadioButton(String text, Skin skin, String styleName) {
        super(text, skin, styleName);
    }
}
