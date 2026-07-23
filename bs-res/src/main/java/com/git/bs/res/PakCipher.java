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
