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
 * P2 明文 cipher：原样返回（不拷贝）。
 *
 * <p>用于在引入加密（P3 ChaCha20）之前，先把 pak 格式 / 打包器 / 读取器端到端跑通。
 * P3 把 {@link #INSTANCE} 换成 ChaCha20 实现即可。</p>
 *
 * @author authorZhao
 * @since 2026-07-20
 */
public final class IdentityCipher implements PakCipher {

    public static final IdentityCipher INSTANCE = new IdentityCipher();

    private IdentityCipher() {}

    @Override
    public byte[] apply(byte[] data, byte[] salt, int ordinal) {
        return data;
    }
}
