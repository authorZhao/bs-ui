// loader.js — app.wasm 内存解密加载器（与 core 的 WasmEncrypter / ChaCha20Cipher 配对）
//
// 在所有其它脚本之前加载。monkeypatch WebAssembly 的全部四个编译入口
//（compile / compileStreaming / instantiate / instantiateStreaming）：
// TeaVM runtime 拿到的 app.wasm 是【密文】，这里检测到非 wasm magic 就内存解密，
// 再调真 compile/instantiate——明文只在 JS 局部变量、不经 streaming Response。
//
// ⚠️ 需浏览器实测。JS ChaCha20 是 Java 版同算法移植（BigInt 算 64 位 key 避免精度丢失）。
(function () {
  // ---- KEY = PakKeys.PARTS[i] ^ MASK[i]（与 core/PakKeys.java 完全一致）----
  const PARTS = [0x9E3779B97F4A7C15n, 0xC2B2AE3D5B6A8F11n, 0x1B8A5D3F7E2C9064n, 0x4D6A2E8F0C1B7359n];
  const MASK  = [0x5A3E1C7D9F0B2468n, 0x7E4D2A8B9C1F3057n, 0x2D9F4B6E8A0C3175n, 0xB59E3F2A1D7C4806n];
  const KEY = new Uint8Array(32);
  for (let i = 0; i < 4; i++) {
    let v = PARTS[i] ^ MASK[i];
    for (let j = 0; j < 8; j++) { KEY[i * 8 + j] = Number(v & 0xFFn); v >>= 8n; }
  }
  const WASM_SALT = new TextEncoder().encode("bs-ui-wasm-crypt");
  const WASM_ORDINAL = 0;

  // ---- ChaCha20 (RFC 7539)，与 core/ChaCha20Cipher.java 同算法 ----
  const SIGMA = [0x61707865, 0x3320646e, 0x79622d32, 0x6b206574];
  function rotl(v, n) { return ((v << n) | (v >>> (32 - n))); }
  function qr(s, a, b, c, d) {
    s[a] = (s[a] + s[b]) | 0; s[d] = rotl(s[d] ^ s[a], 16);
    s[c] = (s[c] + s[d]) | 0; s[b] = rotl(s[b] ^ s[c], 12);
    s[a] = (s[a] + s[b]) | 0; s[d] = rotl(s[d] ^ s[a], 8);
    s[c] = (s[c] + s[d]) | 0; s[b] = rotl(s[b] ^ s[c], 7);
  }
  function leInt(b, o) { return (b[o] | (b[o + 1] << 8) | (b[o + 2] << 16) | (b[o + 3] << 24)); }
  function block(state, out) {
    const x = state.slice();
    for (let i = 0; i < 10; i++) {
      qr(x, 0, 4, 8, 12); qr(x, 1, 5, 9, 13); qr(x, 2, 6, 10, 14); qr(x, 3, 7, 11, 15);
      qr(x, 0, 5, 10, 15); qr(x, 1, 6, 11, 12); qr(x, 2, 7, 8, 13); qr(x, 3, 4, 9, 14);
    }
    for (let i = 0; i < 16; i++) {
      const v = (x[i] + state[i]) | 0;
      out[i * 4] = v & 0xff; out[i * 4 + 1] = (v >>> 8) & 0xff;
      out[i * 4 + 2] = (v >>> 16) & 0xff; out[i * 4 + 3] = (v >>> 24) & 0xff;
    }
  }
  function chacha20(key, nonce, data) {
    const state = new Int32Array(16);
    state[0] = SIGMA[0]; state[1] = SIGMA[1]; state[2] = SIGMA[2]; state[3] = SIGMA[3];
    for (let i = 0; i < 8; i++) state[4 + i] = leInt(key, i * 4);
    state[13] = leInt(nonce, 0); state[14] = leInt(nonce, 4); state[15] = leInt(nonce, 8);
    const out = new Uint8Array(data.length);
    const ks = new Uint8Array(64);
    let pos = 0, counter = 0;
    while (pos < data.length) {
      state[12] = counter++;
      block(state, ks);
      const len = Math.min(64, data.length - pos);
      for (let i = 0; i < len; i++) out[pos + i] = data[pos + i] ^ ks[i];
      pos += len;
    }
    return out;
  }
  function deriveNonce(salt, ordinal) {
    const nonce = new Uint8Array(12);
    for (let i = 0; i < 8; i++) nonce[i] = salt[i];
    nonce[8] = ordinal & 0xff; nonce[9] = (ordinal >>> 8) & 0xff;
    nonce[10] = (ordinal >>> 16) & 0xff; nonce[11] = (ordinal >>> 24) & 0xff;
    return nonce;
  }

  // ---- 解密辅助：检测 wasm magic（00 61 73 6d）——明文放行，非明文才解密 ----
  function isPlaintextWasm(b) {
    return b.length >= 4 && b[0] === 0x00 && b[1] === 0x61 && b[2] === 0x73 && b[3] === 0x6d;
  }
  function toBytes(source) {
    if (source instanceof Uint8Array) return source;
    if (source instanceof ArrayBuffer) return new Uint8Array(source);
    if (ArrayBuffer.isView(source)) return new Uint8Array(source.buffer, source.byteOffset, source.byteLength);
    return null; // 不是 bytes（如 Module）→ 不处理
  }
  function decryptIfEncrypted(bytes) {
    return isPlaintextWasm(bytes) ? bytes : chacha20(KEY, deriveNonce(WASM_SALT, WASM_ORDINAL), bytes);
  }

  // ---- monkeypatch WebAssembly 全部四个编译入口 ----
  const realCompile = WebAssembly.compile.bind(WebAssembly);
  const realInstantiate = WebAssembly.instantiate.bind(WebAssembly);

  // 1. compile(bytes) → 解密 → 真 compile
  WebAssembly.compile = async function (source) {
    const bytes = toBytes(source);
    if (!bytes) return realCompile(source);
    return realCompile(decryptIfEncrypted(bytes));
  };

  // 2. compileStreaming(Response | Promise<Response>) → 解密 → 真 compile（从字节，不走 streaming Response）
  if (WebAssembly.compileStreaming) {
    WebAssembly.compileStreaming = async function (source) {
      const response = await source;
      const buf = new Uint8Array(await response.arrayBuffer());
      return realCompile(decryptIfEncrypted(buf));
    };
  }

  // 3. instantiate(bytes | Module, imports) → 解密 → 真 instantiate
  WebAssembly.instantiate = async function (source, imports) {
    const bytes = toBytes(source);
    if (!bytes) return realInstantiate(source, imports);
    return realInstantiate(decryptIfEncrypted(bytes), imports);
  };

  // 4. instantiateStreaming(Response | Promise<Response>, imports) → 解密 → 真 instantiate
  if (WebAssembly.instantiateStreaming) {
    WebAssembly.instantiateStreaming = async function (source, imports) {
      const response = await source;
      const buf = new Uint8Array(await response.arrayBuffer());
      return realInstantiate(decryptIfEncrypted(buf), imports);
    };
  }
})();
