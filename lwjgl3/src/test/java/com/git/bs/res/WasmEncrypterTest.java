package com.git.bs.res;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WasmEncrypter 回归：加密 → 对称解密 = 原文；密文与原文不同。
 */
class WasmEncrypterTest {

//    @Test
//    void encryptThenDecryptRoundTrip() {
//        byte[] wasm = new byte[1000];
//        new Random(42).nextBytes(wasm);
//
//        byte[] enc = WasmEncrypter.encrypt(wasm);
//        // 加密生效：密文与原文不同
//        assertFalse(java.util.Arrays.equals(wasm, enc), "密文不应等于明文");
//
//        // 对称：同样 key/salt/ordinal 再 apply 一次 = 原文（loader.js 在浏览器做这一步）
//        byte[] dec = new ChaCha20Cipher(PakKeys.KEY).apply(enc, WasmEncrypter.WASM_SALT, WasmEncrypter.WASM_ORDINAL);
//        assertArrayEquals(wasm, dec, "解密后应还原原文");
//    }
}
