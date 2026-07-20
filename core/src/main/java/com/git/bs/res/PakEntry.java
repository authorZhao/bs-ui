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
 * BPK1 索引条目（运行时只读）。
 *
 * @author authorZhao
 * @since 2026-07-20
 */
final class PakEntry {

    /** 逻辑路径（classpath 风格，正斜杠）。 */
    final String path;
    /** 条目序号；P3 用于派生 nonce。 */
    final int ordinal;
    /** eflags bit0：条目已压缩。 */
    final boolean compressed;
    /** 相对「blob 区起点」的偏移。 */
    final int blobOff;
    /** cipher 处理（+ 可能压缩）后的字节长度。 */
    final int storedLen;
    /** 明文长度。 */
    final int rawLen;

    PakEntry(String path, int ordinal, boolean compressed, int blobOff, int storedLen, int rawLen) {
        this.path = path;
        this.ordinal = ordinal;
        this.compressed = compressed;
        this.blobOff = blobOff;
        this.storedLen = storedLen;
        this.rawLen = rawLen;
    }
}
