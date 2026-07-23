/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库。
 * Copyright (c) 2026 bs-ui contributors
 *
 * 基于 Apache License 2.0 开源，允许商用、修改和再分发。
 * 使用本库的产品须在“关于”界面标注本项目，详见 LICENSE。
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package com.git.bs.res;

/**
 * 纯 Java ChaCha20 流密码（RFC 7539：256-bit key / 96-bit nonce / 32-bit counter）。
 * 实现 {@link PakCipher}，加解密同一操作（XOR keystream），长度不变。
 *
 * <p><b>为什么用纯 Java 而非 javax.crypto / WebCrypto</b>：javax.crypto 在 TeaVM wasm-gc
 * 下不可用；纯 int/byte 实现编译到 JVM 和 wasm-gc 完全一致，<b>无需任何依赖、无需按平台抽象</b>。
 * 对"提高破解门槛"这个目标强度足够。</p>
 *
 * <p><b>nonce 派生</b>：{@code salt[0..8]}（8 字节）+ {@code ordinal}（4 字节 little-endian）= 12 字节。
 * 同一次构建 salt 随机、ordinal 唯一 → 每条目/索引 nonce 各不相同 → 流密码安全（key+nonce 不复用）。
 * 索引用 ordinal = -1。</p>
 *
 * <p>counter 从 0 起逐块递增。加密/解密对称（同一 {@code apply}），打包器与读取器用同一 key 即可往返。</p>
 *
 * @author authorZhao
 * @since 2026-07-22
 */
public final class ChaCha20Cipher implements PakCipher {

    private static final int SIGMA_0 = 0x61707865; // "expa"
    private static final int SIGMA_1 = 0x3320646e; // "nd 3"
    private static final int SIGMA_2 = 0x79622d32; // "2-by"
    private static final int SIGMA_3 = 0x6b206574; // "te k"

    private final byte[] key; // 32 字节

    public ChaCha20Cipher(byte[] key32) {
        if (key32 == null || key32.length != 32) {
            throw new IllegalArgumentException("ChaCha20 key 必须是 32 字节，实际 " + (key32 == null ? 0 : key32.length));
        }
        this.key = key32;
    }

    @Override
    public byte[] apply(byte[] data, byte[] salt, int ordinal) {
        if (data.length == 0) return data; // 空数据不处理（流密码对空输入直接返回）
        byte[] nonce = deriveNonce(salt, ordinal); // 12 字节
        return chacha20(key, nonce, data);
    }

    /** nonce = salt[0..8]（8 字节）+ ordinal（4 字节 LE）。 */
    private static byte[] deriveNonce(byte[] salt, int ordinal) {
        byte[] nonce = new byte[12];
        int n = salt == null ? 0 : Math.min(8, salt.length);
        for (int i = 0; i < n; i++) nonce[i] = salt[i];
        nonce[8]  = (byte) ordinal;
        nonce[9]  = (byte) (ordinal >>> 8);
        nonce[10] = (byte) (ordinal >>> 16);
        nonce[11] = (byte) (ordinal >>> 24);
        return nonce;
    }

    private static byte[] chacha20(byte[] key, byte[] nonce, byte[] data) {
        int[] state = new int[16];
        state[0] = SIGMA_0;
        state[1] = SIGMA_1;
        state[2] = SIGMA_2;
        state[3] = SIGMA_3;
        for (int i = 0; i < 8; i++) state[4 + i] = leInt(key, i * 4);
        // state[12] = counter（每块自增）；state[13..15] = 96-bit nonce
        state[13] = leInt(nonce, 0);
        state[14] = leInt(nonce, 4);
        state[15] = leInt(nonce, 8);

        byte[] out = new byte[data.length];
        byte[] ks = new byte[64]; // keystream block（64 字节）
        int pos = 0;
        int counter = 0;
        while (pos < data.length) {
            state[12] = counter++;
            keystreamBlock(state, ks);
            int len = Math.min(64, data.length - pos);
            for (int i = 0; i < len; i++) {
                out[pos + i] = (byte) (data[pos + i] ^ ks[i]);
            }
            pos += len;
        }
        return out;
    }

    /** 生成一个 64 字节 keystream 块：state 复制 → 20 轮 → 加回原 state → 序列化。 */
    private static void keystreamBlock(int[] state, byte[] out) {
        int[] x = state.clone();
        for (int i = 0; i < 10; i++) { // 10 个 double-round = 20 轮
            qr(x, 0, 4, 8, 12);
            qr(x, 1, 5, 9, 13);
            qr(x, 2, 6, 10, 14);
            qr(x, 3, 7, 11, 15);
            qr(x, 0, 5, 10, 15);
            qr(x, 1, 6, 11, 12);
            qr(x, 2, 7, 8, 13);
            qr(x, 3, 4, 9, 14);
        }
        for (int i = 0; i < 16; i++) {
            int v = x[i] + state[i];
            int o = i * 4;
            out[o]     = (byte) v;
            out[o + 1] = (byte) (v >>> 8);
            out[o + 2] = (byte) (v >>> 16);
            out[o + 3] = (byte) (v >>> 24);
        }
    }

    /** ChaCha20 quarter round。 */
    private static void qr(int[] x, int a, int b, int c, int d) {
        x[a] += x[b]; x[d] = rotl(x[d] ^ x[a], 16);
        x[c] += x[d]; x[b] = rotl(x[b] ^ x[c], 12);
        x[a] += x[b]; x[d] = rotl(x[d] ^ x[a], 8);
        x[c] += x[d]; x[b] = rotl(x[b] ^ x[c], 7);
    }

    private static int rotl(int v, int n) {
        return (v << n) | (v >>> (32 - n));
    }

    private static int leInt(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8)
                | ((b[off + 2] & 0xff) << 16) | ((b[off + 3] & 0xff) << 24);
    }
}
