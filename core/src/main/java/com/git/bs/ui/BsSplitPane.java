package com.git.bs.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane;

/** 双 ScrollPane + 分隔条（简化版：直接用 SplitPane 组合两个 Actor）。 */
public class BsSplitPane extends SplitPane {
    public BsSplitPane(Actor first, Actor second, boolean vertical, Skin skin) {
        super(first, second, vertical, skin);
    }
}
