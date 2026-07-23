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
