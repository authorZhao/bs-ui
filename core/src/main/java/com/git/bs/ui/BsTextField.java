package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter;

public class BsTextField extends TextField {
    public BsTextField(String text, Skin skin) {
        super(text, skin);
    }

    public BsTextField(String text, Skin skin, String styleName) {
        super(text, skin, styleName);
    }

    /// 程序化设值：绕过 TextFieldFilter。
    ///
    /// libGDX 1.14.x 的 `setText` 内部走 `paste`，会逐字符过 `filter.acceptChar`，
    /// 若 filter 拒绝字符（如只读选择器的 `(f, c) -> false`），setText 设的值会被全部丢弃，
    /// 表现为「输入框永远空白」。这里在设值前临时摘掉 filter，设完恢复。
    public void setTextProgrammatic(String str) {
        TextFieldFilter old = getTextFieldFilter();
        setTextFieldFilter(null);
        try {
            setText(str);
        } finally {
            setTextFieldFilter(old);
        }
    }
}
