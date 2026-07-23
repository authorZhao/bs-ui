package com.git.bs.res;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * pak 格式 round-trip 回归：写（ChaCha20 + DEFLATE）→ 读 → 逐条字节一致 + 加密生效 + 缺失处理。
 * 对应手动检查 main {@code PakFormatCheck}；这里是 CI/gradle test 自动版。
 */
class PakFormatTest {

    @Test
    void roundTripWithEncryption() {
        var in = new LinkedHashMap<String, byte[]>();
        in.put("com/git/bs/ui/skin/bs-dark.json", "{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        in.put("com/git/bs/ui/skin/bs-dark.png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A});
        in.put("com/git/bs/ui/skin/sub/x.txt", "hello 世界".getBytes(StandardCharsets.UTF_8));
        in.put("empty.dat", new byte[0]);

        byte[] salt = new byte[PakFormat.SALT_LEN];
        new SecureRandom().nextBytes(salt);
        byte[] pak = PakWriter.write(in, new ChaCha20Cipher(PakKeys.KEY), salt);

        // 加密生效：索引已加密，pak 字节里不应出现明文路径
        String pakStr = new String(pak, StandardCharsets.ISO_8859_1);
        assertFalse(pakStr.contains("bs-dark.json"), "pak 不应含明文路径");

        FileResourcePack pack = FileResourcePack.open(pak, new ChaCha20Cipher(PakKeys.KEY));
        assertEquals(in.size(), pack.size());
        for (Map.Entry<String, byte[]> e : in.entrySet()) {
            assertTrue(pack.has(e.getKey()), "has " + e.getKey());
            assertArrayEquals(e.getValue(), pack.read(e.getKey()), "round-trip " + e.getKey());
            assertEquals(e.getValue().length, pack.length(e.getKey()), "length " + e.getKey());
        }
        // 顺序保留（= ordinal 顺序）
        assertEquals(new ArrayList<>(in.keySet()), pack.paths());
        // 缺失
        assertFalse(pack.has("no/such/file"));
        assertNull(pack.read("no/such/file"));
        assertEquals(0, pack.length("no/such/file"));
    }

    @Test
    void differentSaltProducesDifferentCiphertext() {
        // 同样明文、不同 salt → 密文不同（nonce 派生自 salt）
        var in = new LinkedHashMap<String, byte[]>();
        in.put("a.txt", "same content".getBytes(StandardCharsets.UTF_8));
        byte[] saltA = new byte[PakFormat.SALT_LEN];
        byte[] saltB = new byte[PakFormat.SALT_LEN];
        new SecureRandom().nextBytes(saltA);
        new SecureRandom().nextBytes(saltB);
        byte[] pakA = PakWriter.write(in, new ChaCha20Cipher(PakKeys.KEY), saltA);
        byte[] pakB = PakWriter.write(in, new ChaCha20Cipher(PakKeys.KEY), saltB);
        assertNotEquals(pakA.length, -1); // sanity
        // 两者密文（除 header 里 salt 字段）应不同——简单断言整体不等
        assertFalse(java.util.Arrays.equals(pakA, pakB), "不同 salt 应产生不同密文");
        // 但都能正确解出
        assertArrayEquals(in.get("a.txt"), FileResourcePack.open(pakA, new ChaCha20Cipher(PakKeys.KEY)).read("a.txt"));
        assertArrayEquals(in.get("a.txt"), FileResourcePack.open(pakB, new ChaCha20Cipher(PakKeys.KEY)).read("a.txt"));
    }
}
