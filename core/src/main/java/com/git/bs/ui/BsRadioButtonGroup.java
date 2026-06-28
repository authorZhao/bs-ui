package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;

/**
 * RadioButton 互斥组：封装 {@link ButtonGroup}，避免使用 static 单例导致
 * 多屏幕共享同一组、切屏后旧按钮残留的污染问题。
 * <p>用法：</p>
 * <pre>{@code
 * BsRadioButtonGroup group = new BsRadioButtonGroup();
 * BsRadioButton r1 = group.add(new BsRadioButton("A", skin));
 * BsRadioButton r2 = group.add(new BsRadioButton("B", skin));
 * }</pre>
 * <p>默认 minCheckCount=0（允许全不选）、maxCheckCount=1（单选互斥）。</p>
 */
public class BsRadioButtonGroup {

    private final ButtonGroup<CheckBox> delegate = new ButtonGroup<>();

    public BsRadioButtonGroup() {
        delegate.setMinCheckCount(0);
        delegate.setMaxCheckCount(1);
    }

    /** 把 rb 加入本组并返回 rb 自身（链式）。 */
    public BsRadioButton add(BsRadioButton rb) {
        delegate.add(rb);
        return rb;
    }

    /** 从本组移除 rb（屏幕销毁时调用，避免残留引用）。 */
    public void remove(BsRadioButton rb) {
        delegate.remove(rb);
    }

    /** 清空本组所有按钮（屏幕 dispose 时调用）。 */
    public void clear() {
        delegate.clear();
    }

    /** 当前被选中的按钮，无选中返回 null。 */
    public BsRadioButton getChecked() {
        CheckBox c = delegate.getChecked();
        return c instanceof BsRadioButton ? (BsRadioButton) c : null;
    }

    public int getCheckedIndex() {
        return delegate.getCheckedIndex();
    }
}
