package com.git.bs.admin.modules;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.git.bs.admin.AdminModule;
import com.git.bs.admin.BsAdminShell;
import com.git.bs.demo.BsControlsSkinScreen;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsUI;
import lombok.extern.slf4j.Slf4j;

/**
 * UI 模块：把 BsControlsSkinScreen 的 48 个控件演示作为二级菜单挂到 admin 侧边栏「UI 模块」下。
 *
 * <p>每个二级菜单项（叶子）点击后在内容区显示对应模块的控件预览，
 * 复用 {@link BsControlsSkinScreen#fillModuleContent(int, Table)}，不重复实现 48 个 fill 方法。</p>
 *
 * <p>48 个二级项由 {@link BsAdminShell} 构造时调用 {@link #registerAll(BsAdminShell)} 批量注册，
 * 路径形如 {@code "UI 模块/标签"}（扁平，不再分级成 通用UI/业务UI/图形UI）。</p>
 *
 * <p>stage 注入：admin shell 构造后调 {@link #bindStage(Stage)}，
 * 让 Pickers/DateTime/Overlay/Modal/Dialogs 等依赖真实 stage 的弹窗正常工作。</p>
 */
@Slf4j
public class UiDemoModule {

    public static final String PATH = "UI 模块";

    /** BsControlsSkinScreen 内容工厂（复用 48 个 fill 方法）。 */
    private static BsControlsSkinScreen contentFactory;
    /** admin shell 注入的真实 stage（懒加载工厂创建时应用，让弹窗 attach 到可见 stage）。 */
    private static Stage boundStage;

    /** 取内容工厂单例（懒加载，skin 用当前主题 skin，stage 用注入的 boundStage）。 */
    private static BsControlsSkinScreen factory() {
        if (contentFactory == null) {
            contentFactory = new BsControlsSkinScreen(BsUI.getSkin());
            // 懒加载创建后立即应用已注入的 stage（bindStage 可能在工厂创建前就调过）
            if (boundStage != null) contentFactory.setStage(boundStage);
        }
        return contentFactory;
    }

    /**
     * 记录 admin shell 的真实 stage，供内容工厂使用。
     * <p>注意：工厂是懒加载的，{@code registerAll}/{@code bindStage} 调用时工厂可能还没创建，
     * 所以这里只记录 stage 引用，真正注入发生在 {@link #factory()} 首次创建工厂时；
     * 若工厂已存在则立即注入。</p>
     */
    public static void bindStage(Stage s) {
        boundStage = s;
        if (contentFactory != null) contentFactory.setStage(s);
    }

    /**
     * 清空内容工厂缓存（强制下次按新 skin 重建）。
     * <p>注意：不清 boundStage——主题切换时新 shell 已 bindStage 设了新 stage，
     * 而旧 shell 的 dispose 在新 shell 构造之后才执行，清 boundStage 会误清新值。</p>
     */
    public static void resetFactory() {
        contentFactory = null;
    }

    /**
     * 把 BsControlsSkinScreen 的 48 个控件演示（即其左侧导航 MODULES 列表）
     * 全部作为二级菜单注册到「UI 模块」下，菜单名原封不动使用 MODULES 里的全名
     * （如 "Labels  标签"，与 BsControlsSkinScreen 左侧导航完全一致）。
     *
     * <p>注意：MODULES 名字里含 {@code /}（如 "Overlay  Tooltip/Spinner/Popover/Link"），
     * 而 path 用 {@code /} 作层级分隔符，直接拼会错误拆出多级。
     * 这里把名字里的 {@code /} 替换成 {@code ·}（中文间隔点），既不冲突也保留可读性。</p>
     *
     * <p>由 BsAdminShell 构造时调用。</p>
     */
    public static void registerAll(BsAdminShell shell) {
        for (int idx = 0; idx < BsControlsSkinScreen.MODULES.size(); idx++) {
            final int moduleIdx = idx;
            String name = safeName(BsControlsSkinScreen.MODULES.get(idx));
            final String path = PATH + "/" + name;
            shell.register(new AdminModule() {
                @Override public String getPath() { return path; }
                @Override public Actor buildView(BsAdminShell s) {
                    return buildModuleView(moduleIdx);
                }
            });
        }
    }

    /** 把模块名里的 {@code /} 替换成 {@code ·}，避免与 path 分隔符冲突拆出多级。 */
    private static String safeName(String name) {
        return name == null ? "" : name.replace("/", "·");
    }

    // ============ 具体控件演示页内容 ============

    /**
     * 构造一个具体控件演示页的内容（供注册的真实模块调用）。
     */
    public static Actor buildModuleView(int moduleIdx) {
        Skin skin = BsUI.getSkin();
        Table root = new Table();
        root.top().left();
        root.pad(12);
        root.defaults().top().left();

        // 模块标题（与菜单名一致，/ 替换为 ·）
        String name = safeName(BsControlsSkinScreen.MODULES.get(moduleIdx));
        Label title = new Label(name, skin);
        title.setFontScale(1.3f);
        title.setColor(BsTheme.tp());
        root.add(title).left().padBottom(8).row();

        // 内容（复用 BsControlsSkinScreen 的 fill 方法）
        Table content = new Table(skin);
        content.defaults().pad(6).left();
        try {
            factory().fillModuleContent(moduleIdx, content);
        } catch (Throwable t) {
            log.error("fillModuleContent 失败 idx={}", moduleIdx, t);
            content.add(new Label("加载失败: " + t.getMessage(), skin));
        }
        root.add(content).growX().top().row();
        return root;
    }
}
