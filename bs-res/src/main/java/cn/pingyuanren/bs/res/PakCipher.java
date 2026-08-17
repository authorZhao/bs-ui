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
 * 对称处理索引/条目字节：加密与解密同一操作。
 *
 * <p>P2 用 {@link IdentityCipher}（明文，原样返回）；P3 换成 ChaCha20（与 keystream 异或，
 * 同一方法既加密也解密）。{@code salt + ordinal} 用于派生 nonce（索引用 ordinal = -1）。</p>
 *
 * <p><b>契约</b>：实现必须保持长度不变（流密码特性），这样写索引时能先确定索引长度
 * 再算 blob 偏移。返回值可以是入参数组本体（如 identity）或新数组（如 chacha），调用方约定不改写返回数组。</p>
 *
 * @author authorZhao
 * @since 2026-07-20
 */
public interface PakCipher {

    byte[] apply(byte[] data, byte[] salt, int ordinal);
}
