/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库
 * Copyright (c) 2026 bs-ui contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */

package cn.pingyuanren.bs.admin.modules;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import cn.pingyuanren.bs.admin.AdminModule;
import cn.pingyuanren.bs.admin.BsAdminShell;
import cn.pingyuanren.bs.ui.BsButton;
import cn.pingyuanren.bs.ui.BsConfirmDialog;
import cn.pingyuanren.bs.ui.BsDataTable;
import cn.pingyuanren.bs.ui.BsForm;
import cn.pingyuanren.bs.ui.BsModal;
import cn.pingyuanren.bs.ui.BsSelectBox;
import cn.pingyuanren.bs.ui.BsTextField;
import cn.pingyuanren.bs.ui.BsTheme;
import cn.pingyuanren.bs.ui.BsToast;
import cn.pingyuanren.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 用户管理 - 用户列表。
 * 路径："用户管理/用户"（一级"用户管理"下另有 角色/权限 占位项，由 BsAdminShell 注册）。
 *
 * 内容：搜索框 + 新增按钮 + BsDataTable（6 个用户），行操作 编辑/删除。
 * @author authorZhao
 * @since 2026-07-16
 */
@Slf4j
public class UserListModule implements AdminModule {

    public static final String PATH = "用户管理/用户";

    private static final String[] HEADERS = {"ID", "用户名", "姓名", "角色", "状态", "创建时间"};

    /** 初始用户数据（硬编码演示）。 */
    private static final String[][] SEED_USERS = {
            {"1", "admin",    "管理员",  "超级管理员", "启用", "2024-01-01 09:00"},
            {"2", "zhangsan", "张三",    "编辑",       "启用", "2024-02-12 14:23"},
            {"3", "lisi",     "李四",    "查看者",     "禁用", "2024-03-04 10:11"},
            {"4", "wangwu",   "王五",    "编辑",       "启用", "2024-04-19 16:30"},
            {"5", "zhaoliu",  "赵六",    "查看者",     "启用", "2024-05-08 08:45"},
            {"6", "qianqi",   "钱七",    "编辑",       "禁用", "2024-06-21 19:12"}
    };

    /** 当前可变用户数据（编辑/删除/新增会改它）。 */
    private final List<List<String>> users = new ArrayList<>();
    private BsDataTable table;
    private BsTextField searchField;
    private BsAdminShell shellRef;

    public UserListModule() {
        for (String[] row : SEED_USERS) {
            users.add(new ArrayList<>(Arrays.asList(row)));
        }
    }

    @Override
    public String getPath() {
        return PATH;
    }

    @Override
    public Actor buildView(BsAdminShell shell) {
        this.shellRef = shell;
        Skin skin = BsUI.getSkin();

        Table root = new Table();
        root.top().left();
        root.pad(16);
        root.defaults().top().left();

        // 标题
        Label.LabelStyle xl = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        xl.font = skin.getFont("font-xl");
        Label title = new Label("用户列表", xl);
        title.setColor(BsTheme.tp());
        root.add(title).left().padBottom(8).row();

        // 工具栏：搜索框 + 新增按钮
        Table toolbar = new Table();
        toolbar.defaults().pad(4).left();
        Label searchLabel = new Label("搜索：", skin);
        searchLabel.setColor(BsTheme.ts());
        toolbar.add(searchLabel);
        searchField = new BsTextField("", skin);
        searchField.setMessageText("用户名/姓名");
        searchField.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyFilter();
            }
        });
        toolbar.add(searchField).width(220).padRight(16);

        BsButton addBtn = new BsButton("新增用户", skin, BsButton.Variant.PRIMARY, BsButton.Style.SOLID, BsButton.Size.SM);
        addBtn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
            @Override
            public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                showEditDialog(shell, -1);
            }
        });
        toolbar.add(addBtn);
        toolbar.add().growX();
        root.add(toolbar).growX().padBottom(8).row();

        // 表格
        table = new BsDataTable(skin);
        table.setHeaders(HEADERS);
        refreshTableData(users);
        // 行操作占位：用 setOnRowSelect 拿到行索引后弹操作菜单（编辑/删除）
        table.setOnRowSelect(idx -> showRowActions(shell, idx));

        root.add(table).growX().padTop(4).row();

        // 操作提示
        Label hint = new Label("提示：点击表格任意行可触发 编辑/删除 操作", skin);
        hint.setColor(BsTheme.tm());
        root.add(hint).left().padTop(8).row();

        return root;
    }

    /** 应用搜索过滤（按用户名/姓名子串匹配）。 */
    private void applyFilter() {
        String kw = searchField.getText() == null ? "" : searchField.getText().trim();
        if (kw.isEmpty()) {
            refreshTableData(users);
            return;
        }
        List<List<String>> filtered = new ArrayList<>();
        for (List<String> row : users) {
            // row: id, username, name, role, status, time
            String username = row.get(1);
            String name = row.get(2);
            if (username.contains(kw) || name.contains(kw)) {
                filtered.add(row);
            }
        }
        refreshTableData(filtered);
    }

    private void refreshTableData(List<List<String>> data) {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> r : data) copy.add(new ArrayList<>(r));
        table.setData(copy);
    }

    /** 弹出"行操作"小弹窗：编辑 / 删除。idx 是当前过滤后的索引，但我们用 ID 定位原始数据。 */
    private void showRowActions(BsAdminShell shell, int idx) {
        List<String> row = table.getRow(idx);
        if (row == null || row.isEmpty()) return;
        String id = row.get(0);

        BsModal modal = new BsModal("行操作 - " + row.get(1), BsUI.getSkin());
        Table body = new Table(BsUI.getSkin());
        body.pad(10);
        body.defaults().pad(6).left();
        Label info = new Label("ID: " + id + "  用户名: " + row.get(1) + "  姓名: " + row.get(2), BsUI.getSkin());
        info.setColor(BsTheme.tp());
        body.add(info).row();
        modal.content(body).contentWidth(360);
        modal.addButton("编辑", () -> {
            modal.close();
            int realIdx = findIndexById(id);
            if (realIdx >= 0) showEditDialog(shell, realIdx);
        }, BsButton.Variant.PRIMARY, BsButton.Style.SOLID);
        modal.addButton("删除", () -> {
            modal.close();
            confirmDelete(shell, id, row.get(1));
        }, BsButton.Variant.DANGER, BsButton.Style.SOLID);
        modal.addButton("取消", modal::close, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        modal.showModal(shell.getStage());
    }

    private void confirmDelete(BsAdminShell shell, String id, String name) {
        BsConfirmDialog.show(shell.getStage(), BsUI.getSkin(), "确认删除",
                "确定要删除用户 [" + name + "] (ID=" + id + ") 吗？", yes -> {
                    if (yes) {
                        int idx = findIndexById(id);
                        if (idx >= 0) {
                            users.remove(idx);
                            applyFilter();
                            BsToast.show(shell.getStage(), BsUI.getSkin(),
                                    "已删除用户 " + name, BsToast.Variant.SUCCESS, 1.2f);
                        }
                    }
                });
    }

    /** 编辑/新增弹窗。idx=-1 表示新增。 */
    private void showEditDialog(BsAdminShell shell, int idx) {
        Skin skin = BsUI.getSkin();
        boolean isNew = idx < 0;
        BsModal modal = new BsModal(isNew ? "新增用户" : "编辑用户", skin);

        BsForm form = new BsForm(skin, 70, 240, 140);
        BsTextField usernameF = new BsTextField("", skin);
        BsTextField nameF = new BsTextField("", skin);
        // 角色用 SelectBox（若不可用则降级 TextField）
        Actor roleEditor = makeRoleEditor(skin);
        // 状态用 SelectBox
        Actor statusEditor = makeStatusEditor(skin);

        if (!isNew) {
            List<String> row = users.get(idx);
            usernameF.setText(row.get(1));
            nameF.setText(row.get(2));
            setSelectValue(roleEditor, row.get(3));
            setSelectValue(statusEditor, row.get(4));
        }

        form.addField("用户名", usernameF, v -> (v == null || v.isEmpty()) ? "必填" : null);
        form.addField("姓名", nameF, v -> (v == null || v.isEmpty()) ? "必填" : null);
        form.addField("角色", roleEditor);
        form.addField("状态", statusEditor);

        modal.content(form).contentWidth(460);
        modal.addButton("保存", () -> {
            if (!form.validateAll()) return;
            String username = usernameF.getText();
            String name = nameF.getText();
            String role = getSelectValue(roleEditor);
            String status = getSelectValue(statusEditor);
            if (isNew) {
                int newId = nextId();
                List<String> r = new ArrayList<>(Arrays.asList(
                        String.valueOf(newId), username, name, role, status, now()));
                users.add(r);
                BsToast.show(shell.getStage(), skin, "已新增用户 " + username, BsToast.Variant.SUCCESS, 1.2f);
            } else {
                List<String> r = users.get(idx);
                r.set(1, username);
                r.set(2, name);
                r.set(3, role);
                r.set(4, status);
                BsToast.show(shell.getStage(), skin, "已更新用户 " + username, BsToast.Variant.SUCCESS, 1.2f);
            }
            applyFilter();
            modal.close();
        }, BsButton.Variant.PRIMARY, BsButton.Style.SOLID);
        modal.addButton("取消", modal::close, BsButton.Variant.SECONDARY, BsButton.Style.OUTLINE);
        modal.showModal(shell.getStage());
    }

    private Actor makeRoleEditor(Skin skin) {
        BsSelectBox<String> box = new BsSelectBox<>(skin);
        box.setItems("超级管理员", "编辑", "查看者");
        return box;
    }

    private Actor makeStatusEditor(Skin skin) {
        BsSelectBox<String> box = new BsSelectBox<>(skin);
        box.setItems("启用", "禁用");
        return box;
    }

    @SuppressWarnings("unchecked")
    private void setSelectValue(Actor editor, String value) {
        if (editor instanceof com.badlogic.gdx.scenes.scene2d.ui.SelectBox) {
            try {
                ((com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String>) editor).setSelected(value);
            } catch (Throwable ignored) {
                if (editor instanceof TextField) ((TextField) editor).setText(value);
            }
        } else if (editor instanceof TextField) {
            ((TextField) editor).setText(value);
        }
    }

    @SuppressWarnings("unchecked")
    private String getSelectValue(Actor editor) {
        if (editor instanceof com.badlogic.gdx.scenes.scene2d.ui.SelectBox) {
            try {
                return ((com.badlogic.gdx.scenes.scene2d.ui.SelectBox<String>) editor).getSelected();
            } catch (Throwable t) {
                return "";
            }
        } else if (editor instanceof TextField) {
            return ((TextField) editor).getText();
        }
        return "";
    }

    private int nextId() {
        int max = 0;
        for (List<String> r : users) {
            try { max = Math.max(max, Integer.parseInt(r.get(0))); } catch (Throwable ignored) {}
        }
        return max + 1;
    }

    private int findIndexById(String id) {
        for (int i = 0; i < users.size(); i++) {
            if (id.equals(users.get(i).get(0))) return i;
        }
        return -1;
    }

    private String now() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
