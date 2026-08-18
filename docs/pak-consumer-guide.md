# bs-ui pak 资源加密使用指南（给使用者）

> 状态：P3/P4 已完成——ChaCha20 加密 + DEFLATE 压缩实装，桌面（lwjgl3）与 Web（TeaVM）两端可用。
> 设计细节见 [resource-encryption-design.md](./resource-encryption-design.md)（格式 BPK1、威胁模型、阶段记录）。

---

## 一、它解决什么问题

把 skin / 字体 / icons / emoji / i18n 等运行时资源打进**单个加密容器 `assets.pak`**：

- **产物里没有明文资源**（桌面 jar 内、Web 的 HTTP 传输中都只见密文 pak）；
- **对上层完全透明**：`PakBootstrap.init()` 之后，libGDX 一切 `Gdx.files.internal(...)`、`Gdx.files.getFileHandle(..., Internal)` 命中 pak 路径的自动解密解压，Skin / TextureAtlas / BitmapFont 加载代码**一行不改**；
- **加密为纯 Java ChaCha20**（不依赖 `javax.crypto`），JVM 与 TeaVM wasm-gc 通用。

> 定位是**提高门槛**（混淆级）：密钥编译在客户端，能挡"直接从产物目录/抓包拿资源"，挡不住逆向调试与 GPU 抓帧。

## 二、依赖选型

| 坐标 | 内容 | 适用 |
|------|------|------|
| `cn.pingyuanren:bs-ui-core:0.3.2` | 组件库本体 + core i18n（**不含** skin/icons/emoji 素材） | 所有使用者 |
| `cn.pingyuanren:bs-ui-res:0.3.2` | **pak 工具箱**：`PakBootstrap` / `PakPacker` / ChaCha20 等 | 要用 pak 加密的使用者 |
| `cn.pingyuanren:bs-ui-core-all:0.3.2` | 聚合：core + 三个素材包（jar 内**明文**素材） | 开箱即用，**不能**再 pak 加密 |

用 pak 的组合就是前两个：

```groovy
repositories { mavenCentral() }

dependencies {
    implementation 'cn.pingyuanren:bs-ui-core:0.3.2'
    implementation 'cn.pingyuanren:bs-ui-res:0.3.2'
}
```

素材（skin/icons/emoji/i18n）由你自己提供——classpath 路径要对应，如 skin 放 `<dir>/cn/pingyuanren/bs/ui/skin/**`。可以自己制作，也可以从 `bs-assets-*` jar 解出来改。

## 三、接入三步

### 1. 打包：`PakPacker`（构建期）

`PakPacker` 的用法（`bs-ui-res` 内置的 main）：

```
PakPacker <outputFile> <dir1> <prefix1> [<dir2> <prefix2> ...]
```

- `<dirN> <prefixN>` 成对出现：把 `dirN` 目录下的文件（**扁平扫描、不递归**，跳过 `.ttf`）按 `prefixN/文件名` 收进 pak；
- 多组可一次打完；文本类（`.json/.atlas/.fnt/.properties/.txt` 等）自动 **DEFLATE 压缩后再加密**，PNG 等已压缩文件原样加密；
- 每次构建随机 salt，密文随构建变化。

在你 app 的 build.gradle 里加（**任务必须放消费模块**，不能放库模块——pak 要进 classpath 被 `processResources` 消费，放库模块会循环依赖，见设计文档 P2 坑）：

```groovy
def pakFile = file("${buildDir}/pak/assets.pak")
tasks.register('packResources', JavaExec) {
    dependsOn ':bs-res:classes'                     // 或你工程里 bs-ui-res 的模块名
    classpath = configurations.runtimeClasspath
    mainClass = 'cn.pingyuanren.bs.res.PakPacker'
    args = [pakFile.absolutePath,
            file('src/main/resources/cn/pingyuanren/bs/ui/skin').absolutePath,
            'cn/pingyuanren/bs/ui/skin']
    inputs.dir file('src/main/resources/cn/pingyuanren/bs/ui/skin')
    outputs.file pakFile
}
sourceSets.main.resources.srcDir(file("${buildDir}/pak"))   // pak 进 jar classpath
processResources.dependsOn('packResources')
```

### 2. 启动：`PakBootstrap.init()`（运行期，一行）

在你 App 的 `create()` **最早期**（早于 `BsUI.init()` / `BsI18n.init()` 等任何资源加载）调一次：

```java
import cn.pingyuanren.bs.res.PakBootstrap;

@Override
public void create() {
    PakBootstrap.init();      // classpath 有 assets.pak 就加载并包装 Gdx.files；没有则跳过（明文回退）
    BsUI.init();
    BsI18n.init();
    setScreen(new MainScreen());
}
```

行为细节：

- pak 不存在时打 warn 日志直接跳过，资源照常从磁盘明文读——**开发期天然回退，无需任何开关**；
- 内部：读 `assets.pak` 字节 → `FileResourcePack.open(bytes)` 解密索引 → `Gdx.files = new PakFiles(原实现, pack)`；
- 拦截 `internal()` / `getFileHandle(Internal)` / `classpath()` 三条路径（BitmapFont 加载字体页走的是 `getFileHandle`，必须拦——设计文档 P1 关键发现），其余委派平台原生实现；
- 无参数、无系统属性、幂等回退。

### 3. 分发：产物里只剩密文

桌面端打可执行 jar 时**排除明文素材**（否则加密白做）：

```groovy
tasks.register('distApp', Jar) {
    dependsOn classes, configurations.runtimeClasspath
    from sourceSets.main.output
    from { configurations.runtimeClasspath.collect { zipTree(it) } }
    exclude('cn/pingyuanren/bs/ui/{skin,emoji,icons}/**', 'cn/pingyuanren/bs/demo/i18n/**')  // 明文素材出局
    manifest { attributes 'Main-Class': 'com.example.Main' }
}
```

运行 `java -jar app.jar`：pak 在 jar 内，自包含。

本项目可跑的参考任务：

| 平台 | 命令 | 说明 |
|------|------|------|
| 桌面 | `./gradlew :lwjgl3:distWinSettings` | fat jar：代码 + core i18n + assets.pak，明文素材 exclude |
| 桌面 | `./gradlew :lwjgl3:distBsSkin` | 同上，换 Main-Class |
| Web | `./gradlew :teavm:releasePak` | `getAssetFileHandles` 只列 `assets.pak`，HTTP 只传一个密文 pak |
| Web | `./gradlew :teavm:releasePak -PwasmCrypt=true` | 追加加密 `app.wasm` + 注入 `loader.js`（**浏览器端运行时解密需实测**，谨慎启用） |
| 开发 | `./gradlew :lwjgl3:run` / `:teavm:buildRelease` | 不打包或明文散列资源，迭代不受影响 |

## 四、换密钥（建议发布前做）

密钥 `PakKeys.KEY`（32 字节）以 4×long PARTS XOR MASK 的拆分形式存于 `bs-ui-res` 源码，打包器与运行时引用同一常量，改一处两端自动一致。

1. 生成新 key：`./gradlew :bs-res:pakKeyGen`（随机生成 PARTS/MASK 并打印）；
2. 把打印的常量粘贴进 `cn.pingyuanren.bs.res.PakKeys`；
3. **重新打包所有 pak**——换 key 后旧 pak 全部失效（解不开索引即拒载）。

> 注意：`bs-ui-res` 是发布到 Maven Central 的公共构件，**默认 key 是公开的**。真要保护自己的素材，必须 fork/复制 `bs-res` 相关类到你的工程里换私有 key，而不是依赖公共构件的默认 key。

## 五、验证工具（bs-res 自带）

| 任务 | 内容 |
|------|------|
| `./gradlew :bs-res:pakFormatCheck` | BPK1 round-trip：写 pak → 读回 → 逐条字节一致 |
| `./gradlew :bs-res:pakSurfaceCheck` | `PakFileHandle` 表面检查（不依赖 GL） |
| `./gradlew :bs-res:pakKeyGen` | 生成新密钥常量 |

## 六、限制与注意

- **`PakPacker` 扁平扫描不递归**、跳过 `.ttf`（ttf 应在构建期烘焙成 `.fnt` + PNG 页再进 pak）；
- pak 内资源**只读**（`PakFileHandle` 是内存/文件只读视图）；
- 加密强度为**混淆级**：密钥在客户端代码里，防"5 秒扒素材"，不防逆向；
- `wasmCrypt`（app.wasm 加密）为实验特性，浏览器端解密未充分实测；
- Web 端启动会一次性拉全量 pak（本项目 ~19MB），首屏带宽要留意。

---

**相关文档**：[getting-started.md](./getting-started.md) 第九节（自带素材替换默认资源） | [resource-encryption-design.md](./resource-encryption-design.md)（设计与格式） | **English**: 本文档暂只有中文版
