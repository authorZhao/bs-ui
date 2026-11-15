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
package cn.pingyuanren.bs.res;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link ResourceModeResolver} 的内置通用实现：JVM 属性 &gt; 环境变量 &gt; {@link ResourceMode#AUTO}。
 *
 * <p>取值 {@code loose} / {@code pak} / {@code auto}（大小写不敏感、trim），未知值 warn 后按
 * AUTO 处理。属性名/环境变量名由使用方传入，游戏不必复用 bs-ui 的键名：</p>
 * <pre>{@code
 * new SysPropsResourceModeResolver("cwwg9.assets", "CWWG9_ASSETS")
 * }</pre>
 *
 * @author authorZhao
 * @since 0.3.7
 */
@Slf4j
public final class SysPropsResourceModeResolver implements ResourceModeResolver {

    private final String propKey;
    private final String envKey;

    public SysPropsResourceModeResolver(String propKey, String envKey) {
        this.propKey = propKey;
        this.envKey = envKey;
    }

    @Override
    public ResourceMode resolve() {
        String mode = System.getProperty(propKey);
        if (mode == null || mode.isBlank()) {
            mode = envKey == null ? null : System.getenv(envKey);
        }
        if (mode == null || mode.isBlank()) {
            return ResourceMode.AUTO;
        }
        return parse(mode.trim());
    }

    private ResourceMode parse(String mode) {
        switch (mode.toLowerCase()) {
            case "loose": return ResourceMode.LOOSE;
            case "pak":   return ResourceMode.PAK;
            case "auto":  return ResourceMode.AUTO;
            default:
                log.warn("SysPropsResourceModeResolver: 未知资源模式 \"{}\"（应为 loose/pak/auto），按 AUTO 处理", mode);
                return ResourceMode.AUTO;
        }
    }
}
