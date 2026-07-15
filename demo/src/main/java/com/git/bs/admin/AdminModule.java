package com.git.bs.admin;

import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Admin 模板的功能模块接口。新增模块只需：
 * <ol>
 *   <li>实现本接口</li>
 *   <li>在 {@link BsAdminShell} 构造时 {@code shell.register(new XxxModule())}</li>
 * </ol>
 * 模块会自动出现在侧边栏菜单树、点击后内容区渲染 {@link #buildView(BsAdminShell)} 的返回值，
 * 面包屑自动按 path 拆段更新。
 * @author authorZhao
 * @since 2026-07-16
 */
public interface AdminModule {

    /**
     * 菜单路径，用 "/" 分隔，支持多级。例如：
     * <ul>
     *   <li>"首页" —— 一级叶子</li>
     *   <li>"用户管理/用户" —— 二级菜单（一级"用户管理"为分组）</li>
     * </ul>
     * 路径段会原样作为侧边栏菜单项文字与面包屑文字。
     */
    String getPath();

    /**
     * 渲染模块内容。返回的 Actor 会被 BsAdminShell 自动包装进 BsScrollPane（适配溢出），
     * 然后塞进主内容区。推荐返回一个 top().left() 的 Table。
     *
     * @param shell 当前壳，可取 stage / skin / 触发跳转等
     */
    Actor buildView(BsAdminShell shell);

    /**
     * 模块标题（用于面包屑最后一段）。默认取 path 末段。
     */
    default String getTitle() {
        String p = getPath();
        int idx = p.lastIndexOf('/');
        return idx >= 0 ? p.substring(idx + 1) : p;
    }
}
