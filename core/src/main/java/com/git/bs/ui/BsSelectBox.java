package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class BsSelectBox<T> extends SelectBox<T> {
    public BsSelectBox(Skin skin) { super(skin); }
    public BsSelectBox(Skin skin, String styleName) { super(skin, styleName); }
}
