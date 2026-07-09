package com.git.bs.winsettings;

/** 页面路由接口：主页卡片 / 导航项点击后跳转到指定分类页。 */
public interface Router {
    /** 跳转到 key 对应的页面（home/system/bluetooth/...）。 */
    void navigate(String key);
}
