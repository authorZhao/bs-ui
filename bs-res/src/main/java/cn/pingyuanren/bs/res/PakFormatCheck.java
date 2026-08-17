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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2：BPK1 格式 round-trip 确定性检查（不依赖 GL / Gdx.files）。
 *
 * <p>构造若干假条目（文本/二进制/嵌套路径/空文件）→ {@link PakWriter} 写 pak →
 * {@link FileResourcePack} 读回 → 逐条校验字节一致、顺序保留、缺失返回 null。
 * 运行：{@code ./gradlew :core:pakFormatCheck}。</p>
 *
 * @author authorZhao
 * @since 2026-07-20
 */
public final class PakFormatCheck {

    private static int pass = 0;

    public static void main(String[] args) {
        LinkedHashMap<String, byte[]> in = new LinkedHashMap<>();
        in.put("cn/pingyuanren/bs/ui/skin/bs-dark.json", "{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        in.put("cn/pingyuanren/bs/ui/skin/bs-dark.png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A});
        in.put("cn/pingyuanren/bs/ui/skin/sub/x.txt", "hello 世界".getBytes(StandardCharsets.UTF_8)); // 多字节 + 嵌套
        in.put("empty.dat", new byte[0]); // 空文件

        byte[] salt = new byte[PakFormat.SALT_LEN];
        new java.security.SecureRandom().nextBytes(salt);
        byte[] pak = PakWriter.write(in, new ChaCha20Cipher(PakKeys.KEY), salt);

        // header
        check(pak.length > PakFormat.HEADER_SIZE, "pak 长度 " + pak.length + " > header");
        check("BPK1".equals(new String(pak, 0, 4, StandardCharsets.US_ASCII)), "magic = BPK1");
        check((pak[PakFormat.OFF_VERSION] & 0xff) == PakFormat.VERSION, "version = " + PakFormat.VERSION);

        // 加密生效：索引已加密，pak 字节里不应出现明文路径
        String pakStr = new String(pak, StandardCharsets.ISO_8859_1);
        check(!pakStr.contains("bs-dark.json"), "加密生效：pak 不含明文路径 'bs-dark.json'");

        FileResourcePack pack = FileResourcePack.open(pak, new ChaCha20Cipher(PakKeys.KEY));
        check(pack.size() == in.size(), "条目数 = " + pack.size() + "（期望 " + in.size() + "）");

        // 逐条 round-trip
        for (Map.Entry<String, byte[]> e : in.entrySet()) {
            check(pack.has(e.getKey()), "has " + e.getKey());
            byte[] got = pack.read(e.getKey());
            check(Arrays.equals(got, e.getValue()), "round-trip 字节一致: " + e.getKey()
                    + "（期望 " + e.getValue().length + " 字节）");
            check(pack.length(e.getKey()) == e.getValue().length, "length " + e.getKey());
        }

        // 顺序保留（= ordinal 顺序）
        List<String> expectedOrder = new ArrayList<>(in.keySet());
        check(expectedOrder.equals(pack.paths()), "顺序保留: " + pack.paths());

        // 缺失
        check(!pack.has("no/such/file"), "缺失 has=false");
        check(pack.read("no/such/file") == null, "缺失 read=null");
        check(pack.length("no/such/file") == 0, "缺失 length=0");

        System.out.println();
        System.out.println("PakFormatCheck: 全部通过（" + pass + " 项）");
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError("FAIL: " + msg);
        pass++;
        System.out.println("  ok  " + msg);
    }

    private PakFormatCheck() {}
}
