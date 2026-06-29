package com.git.bs.admin;

import lombok.Getter;
import lombok.Setter;

/**
 * Admin 模板的登录态/当前用户上下文。
 *
 * <p><b>内存态</b>：不写 Preferences，进程退出即失效。
 * 演示账号 admin / 123456。</p>
 */
public class AdminContext {

    public static final String DEMO_USER = "admin";
    public static final String DEMO_PWD = "123456";

    @Getter
    @Setter
    private String currentUser;

    private static final AdminContext INSTANCE = new AdminContext();

    public static AdminContext get() {
        return INSTANCE;
    }

    private AdminContext() {
    }

    /** 登录校验（与演示账号比对）。 */
    public boolean check(String user, String pwd) {
        return DEMO_USER.equals(user) && DEMO_PWD.equals(pwd);
    }

    /** 登录成功后调用。 */
    public void login(String userName) {
        this.currentUser = userName;
    }

    /** 退出登录。 */
    public void logout() {
        this.currentUser = null;
    }

    public boolean isLogged() {
        return currentUser != null;
    }
}
