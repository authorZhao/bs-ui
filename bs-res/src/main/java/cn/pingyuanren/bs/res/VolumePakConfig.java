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

/**
 * {@link VolumePakBootstrap} 的启动配置（分卷 pak 的全部可变点集中于此）。
 *
 * <pre>{@code
 * VolumePakBootstrap.init(VolumePakConfig.builder()
 *         .indexPath("paks.txt")                            // 分卷索引文件名
 *         .cipher(new ChaCha20Cipher(myPrivateKey))         // 密码器（key 游戏私有）
 *         .modeResolver(new SysPropsResourceModeResolver(   // 三态开关策略
 *                 "mygame.assets", "MYGAME_ASSETS"))
 *         .logTag("mygame")
 *         .build());
 * }</pre>
 *
 * @author authorZhao
 * @since 0.3.7
 */
public final class VolumePakConfig {

    private final String indexPath;
    private final PakCipher cipher;
    private final ResourceModeResolver modeResolver;
    private final String logTag;

    private VolumePakConfig(Builder b) {
        this.indexPath = b.indexPath;
        this.cipher = b.cipher;
        this.modeResolver = b.modeResolver;
        this.logTag = b.logTag;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 分卷索引文件名（索引内每行一个分卷文件名，# 注释/空行忽略）。 */
    public String indexPath() {
        return indexPath;
    }

    /** 指定 cipher；{@code null} = 默认 {@code ChaCha20Cipher(PakKeys.KEY)}。 */
    public PakCipher cipher() {
        return cipher;
    }

    /** 资源模式策略；{@code null} 视为恒 AUTO。 */
    public ResourceModeResolver modeResolver() {
        return modeResolver;
    }

    /** 日志前缀（区分使用方）。 */
    public String logTag() {
        return logTag;
    }

    /** builder。未设置的项用默认值：indexPath=paks.txt / cipher=null / modeResolver=null / logTag=pak。 */
    public static final class Builder {

        private String indexPath = "paks.txt";
        private PakCipher cipher;
        private ResourceModeResolver modeResolver;
        private String logTag = "pak";

        /** 分卷索引文件名（默认 {@code paks.txt}）。 */
        public Builder indexPath(String indexPath) {
            this.indexPath = indexPath;
            return this;
        }

        /** 条目 cipher（默认 {@code new ChaCha20Cipher(PakKeys.KEY)}）。 */
        public Builder cipher(PakCipher cipher) {
            this.cipher = cipher;
            return this;
        }

        /** ChaCha20 快捷口（等价 {@code cipher(new ChaCha20Cipher(key))}）。 */
        public Builder key(byte[] key) {
            this.cipher = new ChaCha20Cipher(key);
            return this;
        }

        /** 资源模式策略（默认恒 AUTO）。 */
        public Builder modeResolver(ResourceModeResolver modeResolver) {
            this.modeResolver = modeResolver;
            return this;
        }

        /** 日志前缀（默认 {@code pak}）。 */
        public Builder logTag(String logTag) {
            this.logTag = logTag;
            return this;
        }

        public VolumePakConfig build() {
            return new VolumePakConfig(this);
        }
    }
}
