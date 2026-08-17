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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 构建期：把 {@code app.wasm} 用 ChaCha20 加密（原地覆盖）。
 *
 * <p>配合 {@code deploy/loader.js}：loader.js 在浏览器 monkeypatch {@code WebAssembly.instantiate}，
 * runtime fetch 到的 app.wasm 是密文（Response=密文，存了没用），loader 在 patched instantiate 里
 * 用<b>同样的 key/salt/ordinal</b> 内存解密再调真 instantiate——明文只在 JS 局部变量、不经 Response，
 * DevTools 存不到明文 wasm。</p>
 *
 * <p>用法：{@code WasmEncrypter <wasmFile>}（原地加密覆盖）。salt/ordinal 与 loader.js 必须一致。
 * 由 releasePak 在 {@code -PwasmCrypt=true} 时调用。</p>
 *
 * @author authorZhao
 * @since 2026-07-23
 */
public final class WasmEncrypter {

    /** 固定 salt（16 字节）+ ordinal，派生 wasm 单流的 nonce。loader.js 用同样的值。 */
    static final byte[] WASM_SALT = "bs-ui-wasm-crypt".getBytes(StandardCharsets.US_ASCII); // 16 字节
    static final int WASM_ORDINAL = 0;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("用法: WasmEncrypter <wasmFile>");
            System.exit(2);
        }
        File f = new File(args[0]);
        byte[] raw = Files.readAllBytes(f.toPath());
        byte[] enc = encrypt(raw);
        Files.write(f.toPath(), enc); // 原地覆盖
        System.out.println("WasmEncrypter: " + raw.length + " → " + enc.length + " 字节（原地加密）: " + f);
    }

    /** ChaCha20(PakKeys.KEY, deriveNonce(WASM_SALT, WASM_ORDINAL), wasm)。对称——同样参数再 apply 一次即解密。 */
    public static byte[] encrypt(byte[] wasm) {
        return new ChaCha20Cipher(PakKeys.KEY).apply(wasm, WASM_SALT, WASM_ORDINAL);
    }

    private WasmEncrypter() {}
}
