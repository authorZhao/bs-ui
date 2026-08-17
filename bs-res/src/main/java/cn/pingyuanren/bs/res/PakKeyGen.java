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

import java.security.SecureRandom;

/**
 * 生成新的 pak 密钥：随机产 32 字节 key + 随机 mask，算出 PARTS = key^mask，
 * 打印成可直接贴进 {@link PakKeys} 的 Java 代码。
 *
 * <p>用法：{@code ./gradlew :core:pakKeyGen}，把输出的 PARTS / MASK 两段覆盖 PakKeys.java
 * 里同名数组，然后重新 {@code distWinSettings} / {@code releasePak} 打包。<b>密钥一换，旧 pak 立刻失效</b>
 *（读取器用新 key 解不开旧 pak）——发布新版本时换 key 即可让历史 pak 作废。</p>
 *
 * @author authorZhao
 * @since 2026-07-22
 */
public final class PakKeyGen {

    public static void main(String[] args) {
        SecureRandom rng = new SecureRandom();
        byte[] key = new byte[32];
        rng.nextBytes(key);

        long[] keyLongs = new long[4];
        for (int i = 0; i < 4; i++) {
            int o = i * 8;
            long v = 0;
            for (int j = 7; j >= 0; j--) v = (v << 8) | (key[o + j] & 0xff); // little-endian → long
            keyLongs[i] = v;
        }
        long[] mask = new long[4];
        for (int i = 0; i < 4; i++) mask[i] = rng.nextLong();
        long[] parts = new long[4];
        for (int i = 0; i < 4; i++) parts[i] = keyLongs[i] ^ mask[i];

        System.out.println();
        System.out.println("// === 把 PakKeys.java 的 PARTS / MASK 覆盖成下面两段（密钥已随机生成）===");
        printArr("PARTS", parts);
        printArr("MASK", mask);
        System.out.println("// 改完重新 distWinSettings / releasePak 打包。旧 pak 会失效（密钥变了）。");
        System.out.println();
    }

    private static void printArr(String name, long[] a) {
        StringBuilder sb = new StringBuilder();
        sb.append("    private static final long[] ").append(name).append(" = {\n");
        for (int i = 0; i < a.length; i++) {
            sb.append("            0x").append(String.format("%016X", a[i])).append("L");
            sb.append(i < a.length - 1 ? ",\n" : "\n");
        }
        sb.append("    };");
        System.out.println(sb);
    }

    private PakKeyGen() {}
}
