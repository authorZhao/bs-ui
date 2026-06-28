package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

public class BsTextField extends TextField {
    public BsTextField(String text, Skin skin) {
        super(text, skin);
    }

    public BsTextField(String text, Skin skin, String styleName) {
        super(text, skin, styleName);
    }
}
