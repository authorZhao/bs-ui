# 资源加密方案设计 — bs-ui

> 状态：草案，待 spike 验证后实施
> 范围：**资源**（skin / 烘焙字体 / icons / emoji / i18n）的打包加密 + 运行时解密
> 不含：`app.wasm` / `index.html` / 运行时 JS（本轮不管，后续可单独加加载器方案）

---

## 0. 摘要

把所有运行时资源打进**单个加密容器 `assets.pak`**，随构建产物分发（桌面进 jar、web 进 assets 目录）。
应用启动时一次性读入 pak、解密索引，之后 libGDX 所有 `Gdx.files.internal(path).read()` 命中 pak 的路径会**透明地解压解密**返回明文字节。

核心设计点：**解密逻辑全部在 `core` 模块、平台无关**，桌面（lwjgl3）和 web（teavm）零差异——因为两者都经 `Gdx.files.internal()` 读资源，我们就在这一层挂接。web 端反而更简单：从"逐文件 N 次 HTTP"变成"拉一个 pak、内存解密"。

加密算法用**纯 Java ChaCha20**（不依赖 `javax.crypto`，确保 teavm wasm-gc 也能编译），文本类资源额外过一层 **DEFLATE**。

---

## 1. 背景：当前加载链

```
① build   TeaVMBuilder.getAssetFileHandles() 逐个列出资源
          → TeaCompiler 原样拷到 build/dist/assets/**          ← 构建期注入点（打包器）
② deploy  build/dist → 服务器 / jar
③ 启动    gdx-teavm 预加载阶段（config.preloadListener 在此触发）  ← pak 读入点
④ 运行    Gdx.files.internal(path).read() 读缓存
          → libGDX 解析 Skin / TextureAtlas / BitmapFont        ← FileHandle 桥接点
```

**当前运行时资源（`TeaVMBuilder` 列出，ttf/chinese.txt 已烘焙、不在运行时包）：**

| 资源 | 体积 | 类型 | 可压缩 |
|------|------|------|--------|
| 烘焙字体 PNG 页（font-lg/md/sm/xl + default-font） | ~15 MB | 已压缩图 | DEFLATE 无效（不压） |
| emoji PNG | ~5 MB | 已压缩图 | 不压 |
| 字体 `.fnt` | ~3.5 MB | 文本 | ✅ ~75% |
| icons PNG | ~0.9 MB | 已压缩图 | 不压 |
| skin json/atlas/png | ~0.2 MB | 文本+图 | ✅ 文本部分 |
| i18n properties | 极小 | 文本 | ✅ |
| **合计** | **≈ 24 MB** | | |

---

## 2. 需求（梳理）

| 编号 | 需求 |
|------|------|
| **R1** | **跨平台**：lwjgl3（桌面）+ teavm（web）同一套方案，核心逻辑共享于 `core`。 |
| **R2** | **加密即产物**：构建产物中的资源文件本身就是密文（at-rest encrypted），不再有明文资源随产物分发。 |
| **R3** | **运行时解密**：启动时解密索引、按需解密解压，对 libGDX 上层（Skin / TextureAtlas / BitmapFont / Texture）完全透明。 |
| **R4** | **web 传输加密**：teavm 下经 HTTP 传输的必须是密文（pak 本体）。 |
| **R5** | **单 pak**：资源打成单个加密容器 `assets.pak`。 |
| **R6** | **目标是提高门槛**：非不可破解；接受密钥客户端化（混淆级强度）。 |
| **R7** | **开发体验**：提供 dev / 明文回退模式，日常迭代不受影响。 |
| **R8** | **不破坏现有**：Skin / TextureAtlas / BitmapFont 等调用点不改。 |

**非目标：**
- N1 不追求抗逆向（wasm 可调试、GPU 纹理可被抓取——本方案只让"DevTools 直接存文件"失效）。
- N2 不加密 `app.wasm` / `index.html` / 运行时 JS（本轮不管）。
- N3 不做服务端鉴权 / 授权。

---

## 3. 威胁模型与边界

**能挡住：**
- 直接从产物目录 / HTTP 下载拿到可用资源（json 配置、字体、贴图）。
- 网络抓包看到明文资源。

**挡不住（且不追求）：**
- 调试 wasm、hook `WebAssembly` 内存拿到解密后的字节。
- 从 WebGL 抓取已上传的纹理。

**为什么密钥放在 wasm 里而不是 Service Worker：**
Service Worker 解密会把**明文喂回页面**，DevTools Network 能看到页面的请求拿到的是明文响应——等于没加密。
解密放在 `Gdx.files` 这层（桌面跑在 JVM、web 编译进 wasm），网络上只有密文，攻击者至少要动手调试 wasm 才能拿到明文——门槛从"5 秒存文件"提到"会逆向 + 花时间"。

---

## 4. 方案总览

```
┌─────────────── 构建期（packer 工具，gradle 任务）───────────────┐
│  资源源(core classpath + assets/)                              │
│    → 文本类: DEFLATE 压缩 ; PNG/已压缩: 原样                    │
│    → 每项 ChaCha20 加密（key=KEY_PAK, nonce=按条目派生）        │
│    → 索引加密                                                  │
│    → 写出 assets.pak                                           │
│        桌面: 进 jar classpath                                  │
│        web:   进 build/dist/assets/                            │
└────────────────────────────────────────────────────────────────┘
┌─────────────── 运行期（core 共享，平台无关）────────────────────┐
│  PakBootstrap.init()  〔app 启动最早期，在 skin 加载之前〕      │
│    bytes = Gdx.files.internal("assets.pak").readBytes()        │
│    pack  = ResourcePack.open(bytes, KEY_PAK)   // 解密索引      │
│    PakFiles.install(pack)                       // 包装 Gdx.files│
│                                                                │
│  之后: Gdx.files.internal("com/.../bs-dark.json")              │
│        → PakFiles 命中 pak → PakFileHandle                      │
│        → read(): 定位条目 → ChaCha20 解密 → (inflate) → 明文字节│
│        → libGDX Skin/TextureAtlas/BitmapFont 照常解析           │
└────────────────────────────────────────────────────────────────┘
```

---

## 5. pak 二进制格式（`BPK1`）

全部整数 **小端序**。

```
偏移  长度  字段
─────────────────────────────────────────────────────────────
 0    4     magic      "BPK1" (0x42 0x50 0x4B 0x31)
 4    1     version    = 1
 5    1     flags      bit0: 索引已压缩
 6    2     reserved   = 0
 8    16    salt       构建期随机；参与条目 nonce 派生（也作 build-id）
24    4     indexOff   索引区起始偏移（u32 LE）
28    4     indexLen   索引区密文长度（u32 LE）
─────────────────────────────────────────────────────────────
索引区（位于 indexOff，密文长 indexLen）：
   ChaCha20 解密 →（flags bit0 时再 inflate）→ 明文索引：
     u32   entryCount
     重复 entryCount 次：
       u16   pathLen ; pathLen 字节 UTF-8 逻辑路径
              （例：com/git/bs/ui/skin/bs-dark.json）
       u8    eflags   bit0: 条目已压缩（DEFLATE）
       u32   blobOff  相对 blobArea 起始的偏移
       u32   cipherLen 该条目密文长度
       u32   rawLen   明文长度
─────────────────────────────────────────────────────────────
数据区 blobArea = align16(indexOff + indexLen)：
   每个条目：cipherLen 字节
     = ChaCha20(KEY_PAK, nonceEntry,  (eflags bit0 ? DEFLATE(raw) : raw))
```

**条目 nonce 派生**（12 字节，确定性，每条目唯一）：
```
nonceEntry = salt[0..8]  ++  u32LE(entryOrdinal)
```
- 同一次构建内 ordinal 唯一 → nonce 唯一 → 流密码安全（key+nonce 不复用）。
- salt 随构建变 → 跨构建 nonce 不同。
- 不必为每条目存 nonce（省空间、隐藏条目边界）。

---

## 6. 加密 / 压缩选型

| 维度 | 选型 | 理由 |
|------|------|------|
| **加密** | **ChaCha20（纯 Java，~120 行）** | 不依赖 `javax.crypto`，JVM 和 teavm wasm-gc 都能编译运行；速度足够（~20MB 解密估计数十 ms）；对"提高门槛"强度足够。 |
| **nonce** | 12 字节，按条目派生 | 见上，避免 key+nonce 复用。 |
| **完整性** | 本轮**不加** Poly1305 | 目标是混淆级，非防篡改；省实现量。索引解析失败即视为 pak 损坏，拒绝加载。 |
| **压缩** | **DEFLATE（`java.util.zip.Inflater/Deflater`，nowrap）**，仅文本类 | `.fnt/.json/.atlas/.properties/.txt` 压缩 ~70%；PNG/JPG/wasm 已压缩，标记为"不压"原样加密。 |
| **压缩判定** | 打包器按扩展名判定 | `.png/.jpg/.wasm/.gz/.br/.ttf?` 不压；其余压。 |

**teavm 可用性是 spike 项**：需确认 `java.util.zip.Inflater` 在 wasm-gc 下可用；若不可用，web 端退化为"只加密不压缩"或塞一个纯 JavaInflater。ChaCha20 纯 Java 无此问题。

---

## 7. 运行时设计（`core` 模块）

四个类，全部平台无关：

```java
/** 打开 pak，解密索引，按需解密条目。 */
public final class ResourcePack {
    public static ResourcePack open(byte[] pakBytes, byte[] key);  // 解密+解压索引
    public boolean has(String logicalPath);
    public byte[] read(String logicalPath);        // 解密条目 + (inflate)
    public InputStream readStream(String logicalPath);
}

/** 包装某条目的 FileHandle，read()/readBytes() 从 pak 取明文。
 *  覆盖 libGDX 加载器实际用到的方法：read/readBytes/length/exists/
 *  name/path/extension/child/sibling/isDirectory/list（空）。 */
public final class PakFileHandle extends FileHandle { ... }

/** 包装 Gdx.files：命中 pak 路径返回 PakFileHandle，其余委派原实现。 */
public final class PakFiles implements Files {
    public static void install(ResourcePack pack);   // Gdx.files = new PakFiles(Gdx.files, pack)
    @Override public FileHandle internal(String path) {
        return pack.has(path) ? new PakFileHandle(path, pack) : delegate.internal(path);
    }
    // external/local/classpath/absolute... 全部委派 delegate
}

/** app 启动最早期调用一次。 */
public final class PakBootstrap {
    public static void init() {
        byte[] pak = Gdx.files.internal("assets.pak").readBytes();
        ResourcePack pack = ResourcePack.open(pak, PakKeys.KEY);
        PakFiles.install(pack);
    }
}
```

**接入点**：`PakBootstrap.init()` 放在 app 启动流程的最前面（`create()` 第一行，早于 `BsSkinLoader.loadAllThemes()` 等）。此时 `Gdx.files` 还是平台原生实现，`internal("assets.pak")` 在桌面读 jar classpath、在 web 读 gdx-teavm 预加载缓存——**两端统一**。之后 `PakFiles.install()` 包装 `Gdx.files`，所有后续 `internal()` 命中 pak 的走解密路径。

> 关键：libGDX 的 `Gdx.files` 是 `public static Files`，可在启动期重新赋值；库内基本是调用时读取（非静态持有），所以早赋值能拦住后续全部 `Gdx.files.internal(...)`。

---

## 8. 构建打包器

**新 gradle 子模块 `:bs-packer`**（Java main），或并入 `bs-skin-export`。

- 输入：资源根目录列表（`core/src/main/resources/com/git/bs/ui/**`、`.../i18n/**`、外部 `assets/`）+ include/exclude glob + 密钥。
- 输出：`assets.pak`。
- **gradle 任务 `packResources`**：处理完资源后产出 `assets.pak` 到约定目录。

**各平台消费：**

| 平台 | pak 落点 | 同时要做 |
|------|----------|----------|
| lwjgl3 | app resources → 进 jar classpath | 从打包源中**移除**被 pak 收录的明文资源，避免重复/泄露 |
| teavm | `build/dist/assets/assets.pak` | `TeaVMBuilder.getAssetFileHandles()` **从长清单改为只列 `assets.pak` + `freetype.js`** |

---

## 9. 密钥管理

- **`KEY_PAK`（32 字节）**：放在 `core` 的 `PakKeys` 类，**拆分 + 运行期拼装**（不要一整块常量）。编译进 jar（桌面）和 wasm（web）。
- 打包器和运行时用**同一把** KEY_PAK。nonce 按条目派生保证不复用。
- 强度定位：混淆级。攻击者反编译/调试可拿到，但需先逆向代码、理解格式——满足"提高门槛"。
- 可选增强（后续）：每构建换 salt；salt 写进 pak 头（明文），key 不变也能让条目密文流变。

---

## 10. 跨平台适配清单

| 项 | lwjgl3（桌面） | teavm（web） |
|----|----------------|--------------|
| pak 读取 | classpath / jar | gdx-teavm 预加载缓存（HTTP 单次拉 pak） |
| `Gdx.files` 包装 | 同一套 `PakFiles` | 同一套 `PakFiles` |
| 加密实现 | 纯 Java ChaCha20 | 同上，编译进 wasm-gc |
| 解压 | `java.util.zip.Inflater` | 同上（**spike：确认可用**） |
| 产物 | jar 内 `assets.pak` | `assets/assets.pak`（HTTP 密文传输） |
| 预加载清单 | 无关 | `TeaVMBuilder` 改为只列 pak + freetype.js |

**桌面端唯一需注意**：jar 里同时存在 `assets.pak` 与（可能残留的）明文资源时，确保打包器把明文资源从 jar 排除，否则 R2（加密即产物）不成立。

---

## 11. 开发模式（dev / 明文回退）

- gradle 属性 `-PpakDev=true`（或系统属性）：
  - 打包器**跳过加密/压缩**，产出"明文 pak"（或干脆不打包）。
  - 运行时 `PakBootstrap` 检测到明文 pak 直接透传，不做解密。
- 日常迭代资源改了即生效，不被加密/打包拖慢。
- 发布构建（`buildRelease`）才启用完整加密。

---

## 12. 待验证 spike（实施前必须跑）

| # | 验证内容 | 方法 | 失败的对策 |
|---|----------|------|-----------|
| **S1** | `Gdx.files` 能否在两端启动期重新赋值、且不破坏 gdx-teavm 内部 | 写一个**纯委派、不带 pak** 的 `PakFiles`，两端 app 照常启动 | 改用更深的 hook 或 fork gdx-teavm |
| **S2** | `PakFileHandle` 覆盖的方法够不够 Skin/TextureAtlas/BitmapFont/Texture/Pixmap 用 | 用 `PakFileHandle`（背后是明文内存表）替换 `bs-dark.json` 单文件加载 | 补缺失方法 / 调整 FileHandle 构造 |
| **S3** | teavm wasm-gc 下 `java.util.zip.Inflater` 可用？ | 在 demo 里 inflate 一小段 | web 端不压缩，或塞纯 JavaInflater |
| **S4** | teavm 预加载清单只剩 `assets.pak + freetype.js` 时能否启动、pak 单次 HTTP 拉取 | 实际构建跑一遍 | 保留必要的最小清单 |
| **S5** | 性能：wasm-gc 下解密 ~20MB 条目 + 索引的耗时 | 计时 | 大 PNG 条目改为惰性解密 / LRU 缓存 |

S1、S2 先在 **lwjgl3**（完整 JVM，最快验证）跑通，再移植 teavm。

### P1 验证结果（2026-07-17，lwjgl3）

**S1 通过**：`Gdx.files` 在启动期重赋值为 `PakFiles` 包装，app 正常启动并渲染（WinSettingsApp 加载 3 套烘焙皮肤 + emoji + 进入 home 页），baseline（不开 pak）同样跑通。

**S2 通过**：32 个 skin 资源（3 套 json/atlas/png + 4 个 .fnt + 22 个字体页 PNG）全部经 `PakFileHandle` 从内存包加载，app 渲染正常。sibling 链（json→atlas→png、json→fnt）正常。

**关键发现（必读，否则 P3 字体会断）**：libGDX `BitmapFont` 加载字体页贴图走的是 `Gdx.files.getFileHandle(path, Internal)`（`fontFile != null` 分支），**不是** `internal()`。`PakFiles` 必须**同时拦截 `getFileHandle(Internal)`**，否则字体 PNG 页绕过 pak 从磁盘加载——P3 加密后磁盘无明文，字体页 404、字体全断。已在 `PakFiles.getFileHandle` 修复，`PakSurfaceCheck` 覆盖。

**预存 bug（与本方案无关）**：`WinSettingsApp.dispose()` 遍历 skin 字体 map 调 `remove(key)` 时遇到 null key 抛 `name cannot be null`。baseline（不开 pak）也复现，是 app 原有清理逻辑的问题，非 pak 引起。

**接入开关**：spike 默认关，`-Dbs.pak.spike=true` 启用、`=exit` 跑完自动退出；`PakSurfaceCheck`（`./gradlew :core:pakSurfaceCheck`）做无 GL 的确定性表面检查（21 项）。

### P2 验证结果（2026-07-20，lwjgl3）

**完成**：BPK1 二进制格式（`PakFormat`/`PakEntry`）+ 加密抽象（`PakCipher` + `IdentityCipher`，P3 换 ChaCha20）+ `PakWriter`（打包）/`FileResourcePack`（读取，implements ResourcePack）+ 构建期打包器 `PakPacker`（gradle `packResources` 任务）。

**确定性**：`./gradlew :core:pakFormatCheck` 20/20（round-trip：写 pak → 读回 → 逐条字节一致、顺序保留、空文件、缺失返回 null）。

**端到端**：`packResources` 把 33 个 skin 资源打成 `assets.pak`（18.9MB，明文、无压缩，pak ≈ 输入 + 索引/头开销）；`PakBootstrap` 从 classpath 读 `assets.pak` → `FileResourcePack` → 包装 `Gdx.files`；运行时 32 个 skin 资源（含**全部字体页 PNG**）经 `PakFileHandle` 从 pak **文件**加载（非 P1 的内存表），app 正常启动渲染。仍是 identity cipher、无压缩——P3 换 ChaCha20 + 打开 DEFLATE 即可，**格式/打包器/读取器零改动**（blobOff 用相对偏移、cipher 长度不变契约已为此设计）。

**gradle 循环依赖坑（必读）**：`packResources` 不能放 core——pak 要进 classpath 被 `processResources` 消费，而打包器又要 core 的 classes，会形成 `processResources↔packResources↔classes` 成环。解法：任务放**消费模块**（lwjgl3/teavm），pak 进该模块 classpath，打包器依赖 `:core:classes`（core:classes 不反向依赖消费模块，无环）。

---

## 13. 落地计划（每步独立可测、可回退）

| 阶段 | 内容 | 验证 |
|------|------|------|
| **P0** | 本设计文档 | 评审确认 |
| **P1** | spike S1+S2（lwjgl3）：`PakFiles` 委派包装 + `PakFileHandle` 替换单文件（**明文内存表，无加密**） | app 启动、单资源正常加载 |
| **P2** | pak 格式 + 打包器（**条目明文**）+ `ResourcePack`（lwjgl3 端到端，仍无加密） | 全部 skin/字体/emoji/icons/i18n 走 pak |
| **P3** | 加 ChaCha20 + DEFLATE + `KEY_PAK`（lwjgl3 完整：资源 at-rest 加密） | jar 内只见密文 pak，运行正常 |
| **P4** | teavm 移植（spike S3、S4）：web 端资源 HTTP 密文传输 | 浏览器跑通，Network 只见 assets.pak |
| **P5** | 两端移除明文资源；dev 模式；补文档 | 产物中无明文资源 |

**回退策略**：P5 之前，明文资源与 pak 并存，任何阶段出问题都可关掉 `PakBootstrap` 回到现状。

---

## 14. 风险与缓解

| 风险 | 缓解 |
|------|------|
| gdx-teavm 对 `Gdx.files` 替换不兼容（内部 `instanceof`/静态持有） | spike S1 先验证；不行则只在资源路径走自定义解析、不动 `Gdx.files` |
| `PakFileHandle` 方法覆盖不全导致某加载器 NPE | spike S2 逐个加载器验证；保留明文回退 |
| wasm-gc 下 Inflater 不可用 | 退化 web 端只加密不压缩；或引入纯 JavaInflater |
| ~20MB 全量解密拖慢 web 启动 | 索引必解；条目惰性解密 + 解密结果 LRU；按需再决定是否缩范围 |
| 桌面 jar 残留明文资源（R2 破洞） | 打包器显式 exclude；CI 检查 jar 内无明文资源后缀 |
