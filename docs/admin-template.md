# bs-ui Admin 管理后台模板

一套 vue-admin 风格的管理后台模板，作为 bs-ui 库能力的"应用级"展示。

- **入口启动类**：`com.git.bs.test.AdminLauncher`（窗口标题 "bs-ui 管理后台模板"）
- **核心代码位置**：`demo/src/main/java/com/git/bs/admin/`（teavm/lwjgl3 只负责启动类）
- **复用**：完全复用 core 的 bs-ui 组件（BsLayoutAdmin / BsBreadcrumb / BsForm / BsDataTable / BsModal / BsCard …），不修改 core。
- **登录态**：内存态（`AdminContext`），演示账号 `admin / 123456`，进程退出失效。

## 运行

```bash
# 编译
./gradlew :demo:compileJava :lwjgl3:compileJava

# 运行
./gradlew :lwjgl3:run -PmainClass=com.git.bs.test.AdminLauncher
# 或 IDEA 直接 Run AdminLauncher
```

## 模块清单（内置 4 个 + 2 占位）

| 路径 | 类型 | 内容 |
|------|------|------|
| 首页 | DashboardModule | 4 个统计卡 + 最近活动列表 |
| 用户管理/用户 | UserListModule | BsDataTable 6 用户 + 搜索 + 新增 + 编辑(BsModal+BsForm) + 删除(BsConfirmDialog) |
| 用户管理/角色 | 占位 | Toast "角色管理为示例占位，未实现" |
| 用户管理/权限 | 占位 | Toast "权限管理为示例占位，未实现" |
| UI 模块 | UiDemoModule | 6 个控件卡（Buttons/Inputs/Form/Switch/Modal/Card 精简版） |
| 业务模块 | BusinessDemoModule | 订单 CRUD 示例（BsDataTable + BsSwitch + 详情 BsModal） |

## 架构

### 包结构

```
demo/src/main/java/com/git/bs/
├── game/AdminApp.java              # 入口 App（镜像 BsSkinApp，进入 AdminLoginScreen）
└── admin/
    ├── AdminModule.java            # 模块接口（getPath / buildView / getTitle）
    ├── BsAdminShell.java           # 主屏：BsLayoutAdmin + 面包屑 + 内容区(ScrollPane) + 注册表
    ├── AdminLoginScreen.java       # 登录页（BsCard + BsForm）
    ├── AdminContext.java           # 内存态登录上下文
    └── modules/                    # 4 个内置模块实现
```

### 核心接口：`AdminModule`

```java
public interface AdminModule {
    String getPath();                                      // "用户管理/用户"
    Actor buildView(BsAdminShell shell);                   // 渲染内容（自动包进 ScrollPane）
    default String getTitle() { return path 末段; }
}
```

### 注册表机制

`BsAdminShell` 构造时注册：

```java
register(new DashboardModule());
register(new UserListModule());
registerPlaceholder("用户管理/角色", "角色管理为示例占位，未实现");
registerPlaceholder("用户管理/权限", "权限管理为示例占位，未实现");
register(new UiDemoModule());
register(new BusinessDemoModule());
```

`register(module)` 内部：
1. 按 `path` 的 `/` 拆段，挂到 `BsLayoutAdmin` 侧边栏菜单树（一级为叶子，多级则一级是分组 SidebarItem，二级是叶子）。
2. 点击叶子 → `navigate(path)` → 调 `module.buildView(this)`，返回 Actor 用 `BsScrollPane` 包装后塞进内容区，并更新面包屑（首页 › seg1 › seg2）。

### 布局适配（侧边栏/顶部栏折叠）

- 模块 `buildView` 返回的 Actor **推荐**返回一个 `Table`（top/left）。
- `BsAdminShell` 会把它包进 `BsScrollPane`，内容超出自动滚动，不挤压。
- 侧边栏折叠/展开由 `BsLayoutAdmin` 内部处理宽度动画，内容区 cell 自动 growX 占位，模块无需关心。

### 面包屑 + 右上角设置

- **面包屑**：复用 `BsLayoutAdmin.getTopBar()` 的 `topMenuRow` 单元格（topBar 第 3 个子元素）放 `BsBreadcrumb`，不修改 core。
- **用户区**：`setUserInfo(currentUserName, null)` + `addUserMenuItem`：
  - 个人中心（Toast 占位）
  - 设置（`BsModal` 含 3 个 `BsSwitch` 演示）
  - 主题切换（`🌙 Dark` / `☀ Light`，调 `BsUI.setTheme` → 触发 `AdminApp.applyTheme` 重建 screen）
  - 退出登录（`AdminContext.logout()` → 回 `AdminLoginScreen`）

### 主题切换链路

`AdminApp` 完全镜像 `BsSkinApp`：
- 启动加载 light skin + 字体池（default 同步，sm/md/lg/xl 分帧）
- 监听 `BsUI.addOnThemeChangeListener` → `applyTheme(theme)`
- `applyTheme` 根据登录态重建 screen（已登录 → `BsAdminShell`，未登录 → `AdminLoginScreen`），释放旧 screen。
- skin 资源路径：`com/git/bs/ui/skin/{light|dark}.json`（与 BsSkinApp 共用）

## 新增模块指南

**两步**：

1. 实现 `AdminModule`：

```java
package com.git.bs.admin.modules;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.git.bs.admin.AdminModule;
import com.git.bs.admin.BsAdminShell;
import com.git.bs.ui.BsTheme;
import com.git.bs.ui.BsUI;

public class MyModule implements AdminModule {
    public static final String PATH = "我的模块/子页";

    @Override public String getPath() { return PATH; }

    @Override
    public Actor buildView(BsAdminShell shell) {
        Skin skin = BsUI.getSkin();
        Table root = new Table();
        root.top().left();
        root.pad(16);
        Label title = new Label("我的模块", skin);
        title.setColor(BsTheme.tp());
        root.add(title).left().row();
        // ... 组合任意 bs-ui 组件
        return root;
    }
}
```

2. 在 `BsAdminShell` 构造函数里注册一行：

```java
register(new MyModule());
```

模块会自动出现在侧边栏（按 path 自动建多级菜单）、点击自动渲染、面包屑自动更新。

## 点测清单

- [ ] 登录页：`admin/123456` 进主页；错误密码弹 Alert；空字段弹警告
- [ ] 主页（首页）：4 个统计卡（用户数/订单数/收入/活跃）+ 最近活动列表
- [ ] 侧边栏：点 ☰ 折叠/展开有动画；内容区不溢出
- [ ] 面包屑：首页 › 用户管理 › 用户，点"首页"回 Dashboard
- [ ] 用户管理/用户：表格显示 6 个账号；搜索框过滤；行点击弹"编辑/删除"操作；编辑弹窗改字段后表格刷新；删除有 Confirm 确认
- [ ] 用户管理/角色、权限：点击弹 Toast 占位
- [ ] UI 模块：6 个控件卡正常（Buttons/Inputs/Form/Switch/Modal/Card）
- [ ] 业务模块：表格显示 6 订单；行点击弹详情；详情中开关切状态；底部批量开关 + 重置按钮
- [ ] 右上角用户名：点开下拉
  - 个人中心 → Toast
  - 设置 → BsModal 含 3 个 Switch
  - 🌙 Dark → 切深色主题，shell 重建；☀ Light → 切回
  - 退出登录 → 回登录页
- [ ] 主题切换后侧边栏/内容区布局正常
