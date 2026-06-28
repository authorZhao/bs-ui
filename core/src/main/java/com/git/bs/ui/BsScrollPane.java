package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;

public class BsScrollPane extends ScrollPane {
    public BsScrollPane(Actor widget, Skin skin) { super(widget, skin); }
    public BsScrollPane(Actor widget, Skin skin, String styleName) { super(widget, skin, styleName); }
}
