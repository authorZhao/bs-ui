package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class BsList<T> extends List<T> {
    public BsList(Skin skin) { super(skin); }
    public BsList(Skin skin, String styleName) { super(skin, styleName); }
}
