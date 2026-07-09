package com.git.bs.winsettings;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.extern.slf4j.Slf4j;

/**
 * 账户页（按 Win11 真实结构）。
 *
 * <p>设置组：你的信息 / 登录选项 / 家庭和其他用户 / 备份与凭据 / 设备密码恢复。</p>
 */
@Slf4j
public class AccountsPage extends CategoryPage {

    public AccountsPage(Skin skin) {
        super("账户", skin);

        group("你的信息",
                SettingItem.value("账户", "", "authorZhao (Microsoft 账户)"),
                SettingItem.value("账户类型", "", "管理员"),
                SettingItem.value("已登录设备", "", "同步到 OneDrive"),
                SettingItem.button("管理账户", "管理 Microsoft 账户", "管理"),
                SettingItem.button("改用本地账户", "切换到本地账户登录", "改用本地账户")
        );

        group("登录选项",
                SettingItem.value("Windows Hello (人脸)", "Windows Hello 人脸识别", "已设置"),
                SettingItem.toggle("Windows Hello (指纹)", "指纹登录", true),
                SettingItem.value("PIN", "Windows Hello PIN", "已设置 (数字)"),
                SettingItem.button("添加/更改 PIN", "", "更改"),
                SettingItem.toggle("动态锁", "手机离开时自动锁定 PC", false),
                SettingItem.select("无密码登录", "仅允许 Windows Hello / PIN", new String[]{"禁用","启用"}, "启用"),
                SettingItem.select("屏幕超时锁定", "无操作多久锁屏", new String[]{"从不","1 分钟","3 分钟","5 分钟","15 分钟"}, "5 分钟")
        );

        group("电子邮件和账户",
                SettingItem.value("主账户", "", "author@outlook.com"),
                SettingItem.button("添加账户", "添加工作或学校账户", "添加账户"),
                SettingItem.value("已添加账户", "", "1 个 Microsoft, 2 个工作账户")
        );

        group("家庭和其他用户",
                SettingItem.value("其他用户", "", "2 个标准用户"),
                SettingItem.button("添加账户", "添加家庭成员或其他用户", "添加账户",
                        () -> log.info("添加用户账户")),
                SettingItem.button("设置工作或学校账户", "连接组织账户", "连接"),
                SettingItem.toggle("允许家人/其他用户设置锁屏轮播", "", false)
        );

        group("Windows 备份",
                SettingItem.value("备份状态", "", "已同步到 OneDrive"),
                SettingItem.button("备份设置", "备份应用/凭据/设置到 OneDrive", "备份", () -> log.info("Windows 备份")),
                SettingItem.button("记住我的应用", "新设备恢复应用", "管理")
        );

        group("凭据与恢复",
                SettingItem.button("凭据管理器", "查看/管理保存的设备凭据", "管理"),
                SettingItem.value("账户恢复", "配置备用恢复方式", "已配置"),
                SettingItem.link("查看账户活动", "Microsoft 账户最近登录", "查看")
        );
    }
}
