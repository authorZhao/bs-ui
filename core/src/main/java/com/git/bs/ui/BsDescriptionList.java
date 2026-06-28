package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * Bootstrap 风格描述列表（Description List / dl-dt-dd）——
 * 左标签右值，详情页标配（用户详情、产品参数、订单信息）。
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsDescriptionList dl = new BsDescriptionList(skin);
 * dl.setColumns(2);   // 一行 2 组（dt | dd | dt | dd）
 * dl.addItem("姓名", "张三");
 * dl.addItem("邮箱", "zhangsan@example.com");
 * dl.addItem("手机", "13800138000");
 * dl.addItem("地址", "北京市朝阳区");
 * stage.addActor(dl);
 * }</pre>
 *
 * <p>实现：水平 Table，每 N 个 item 一行；每项 = [dt 标签 | dd 值]。
 * dt 用灰色小字，dd 用深色正文。</p>
 */
public class BsDescriptionList extends Table {

    private int columns = 1;
    private float labelWidth = 80;
    private float valueWidth = 160;
    private int currentItem = 0;
    /** 用户自定义颜色覆盖（null 表示走主题 token）。 */
    private Color labelColorOverride = null;
    private Color valueColorOverride = null;
    private Color labelColor() { return labelColorOverride != null ? labelColorOverride : BsTheme.ts(); }
    private Color valueColor() { return valueColorOverride != null ? valueColorOverride : BsTheme.tp(); }

    public BsDescriptionList(Skin skin) {
        left().top();
        defaults().top().left();
    }

    /** 设置列数（一行多少组标签-值）。 */
    public BsDescriptionList setColumns(int n) {
        this.columns = Math.max(1, n);
        return this;
    }

    public BsDescriptionList setLabelWidth(float w) { this.labelWidth = w; return this; }
    public BsDescriptionList setValueWidth(float w) { this.valueWidth = w; return this; }
    public BsDescriptionList setLabelColor(Color c) { this.labelColorOverride = c; return this; }
    public BsDescriptionList setValueColor(Color c) { this.valueColorOverride = c; return this; }

    /** 添加一组键值。 */
    public BsDescriptionList addItem(String label, String value) {
        Skin skin = BsUI.getSkin();
        // dt
        Label dt = new Label(label, skin);
        dt.setColor(labelColor());
        dt.setFontScale(0.9f);
        add(dt).width(labelWidth).left().padTop(6).padRight(8);
        // dd
        Label dd = new Label(value == null ? "-" : value, skin);
        dd.setColor(valueColor());
        dd.setWrap(true);
        add(dd).width(valueWidth).left().padTop(6).growX();
        currentItem++;
        if (currentItem % columns == 0) {
            row();
        }
        return this;
    }
}
