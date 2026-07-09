package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

/**
 * 应用页（按 Win11 真实结构）。
 *
 * <p>设置组：应用管理 / 默认应用 / 可选功能 / 高级应用设置 / 应用执行别名。</p>
 */
@Slf4j
public class AppsPage extends CategoryPage {

    public AppsPage(Skin skin) {
        super("应用", skin);

        group("已安装的应用",
                SettingItem.value("已安装应用数", "", "237 个应用"),
                SettingItem.page("⊞", "已安装的应用", "搜索、排序、修改、卸载应用", "apps/installed"),
                SettingItem.select("排序方式", "", new String[]{"名称","大小","安装日期"}, "名称"),
                SettingItem.select("筛选器", "按驱动器筛选", new String[]{"所有驱动器","C:","D:"}, "所有驱动器")
        );

        group("默认应用",
                SettingItem.button("按应用设置默认值", "选择应用后设其默认打开的链接/文件", "设置"),
                SettingItem.button("按文件类型设置默认值", "如 .pdf/.mp4 由谁打开", "设置"),
                SettingItem.button("按链接类型设置默认值", "web/mail/map 等协议", "设置"),
                SettingItem.button("重置所有默认应用", "恢复 Microsoft 推荐默认", "重置")
        );

        group("可选功能",
                SettingItem.value("已安装功能", "", "记事本、写字板、画图、DirectPlay、Quick Assist"),
                SettingItem.button("添加可选功能", "添加字体、媒体编解码器、OpenSSH 等", "添加功能"),
                SettingItem.button("更多 Windows 功能", "查看完整可选功能列表", "查看")
        );

        group("高级应用设置",
                SettingItem.select("应用安装来源", "允许从任意位置安装", new String[]{"任何来源,但警告","任何来源","仅 Microsoft Store","仅 Microsoft Store (推荐)"}, "仅 Microsoft Store (推荐)"),
                SettingItem.toggle("存档应用程序", "节省空间,运行时下载", false),
                SettingItem.toggle("卸载不使用的应用", "自动卸载长期未用", false)
        );

        group("应用执行别名",
                SettingItem.toggle("应用执行别名", "允许应用执行覆盖系统命令", true),
                SettingItem.link("管理执行别名", "如 python.exe、winget.exe 由谁执行", "管理")
        );

        group("启动应用",
                SettingItem.value("开机自启应用", "", "12 个应用"),
                SettingItem.button("管理启动应用", "查看/禁用开机启动项", "管理"),
                SettingItem.toggle("启动状态排序", "按开机影响排序", false)
        );
    }
}
