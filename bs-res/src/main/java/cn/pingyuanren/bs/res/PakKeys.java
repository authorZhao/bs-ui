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

import java.util.Arrays;

/**
 * 资源 pak 密钥（32 字节，ChaCha20 用）。混淆存储提高静态提取门槛。
 *
 * <p>真实 key = {@code PARTS[i] ^ MASK[i]}（4 个 long，拼成 32 字节 little-endian）。
 * 不是明文字面量、也不在常量池里以可读形式出现，提升逆向门槛；但 key 仍在客户端代码内，
 * 强度定位为<b>混淆级</b>（提高门槛，非真正保密）——与本方案"增加破解难度"的目标一致。</p>
 *
 * <p><b>换 key</b>：改下面 8 个常量（建议随机值）即可。打包器（{@link PakPacker}）和读取器
 * （{@link FileResourcePack}）都引用 {@link #KEY}，自动一致。</p>
 *
 * @author authorZhao
 * @since 2026-07-22
 */
public final class PakKeys {

    private static final long[] PARTS = {
            0x9E3779B97F4A7C15L, 0xC2B2AE3D5B6A8F11L, 0x1B8A5D3F7E2C9064L, 0x4D6A2E8F0C1B7359L
    };

    private static final long[] MASK = {
            0x5A3E1C7D9F0B2468L, 0x7E4D2A8B9C1F3057L, 0x2D9F4B6E8A0C3175L, 0xB59E3F2A1D7C4806L
    };

    /** 32 字节 ChaCha20 密钥。 */
    public static final byte[] KEY = build();

    private static byte[] build() {
        byte[] k = new byte[32];
        for (int i = 0; i < 4; i++) {
            long v = PARTS[i] ^ MASK[i];
            int o = i * 8;
            k[o]     = (byte) v;
            k[o + 1] = (byte) (v >>> 8);
            k[o + 2] = (byte) (v >>> 16);
            k[o + 3] = (byte) (v >>> 24);
            k[o + 4] = (byte) (v >>> 32);
            k[o + 5] = (byte) (v >>> 40);
            k[o + 6] = (byte) (v >>> 48);
            k[o + 7] = (byte) (v >>> 56);
        }
        return k;
    }

    /** 清零一份 key 副本（如临时复制后用完）。不碰 {@link #KEY} 本体。 */
    public static void wipe(byte[] keyCopy) {
        if (keyCopy != null) Arrays.fill(keyCopy, (byte) 0);
    }

    private PakKeys() {}
}
