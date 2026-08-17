# bs-ui 依赖方式与 pak 资源加密用法（给使用者）

> 状态：P2 阶段（pak 为明文 identity、无压缩；加密/压缩在 P3）。本文给出依赖选型与 pak 接入流程。

## 一、两个 core 变体

| 坐标 | 内容 | 适用 |
|------|------|------|
| `cn.pingyuanren:bs-ui-core` | 核心 Java 代码 + i18n 资源；**不含** skin/icon/emoji 素材 | 想自己提供素材、或想用 pak 加密打包的使用者 |
| `cn.pingyuanren:bs-ui-core-all` | 聚合：`bs-ui-core` + `bs-assets-skin` + `bs-assets-emoji` + `bs-assets-icons`（传递依赖） | 想**开箱即用**、直接拿默认全套素材的使用者 |

单独的素材模块（`bs-ui-core-all` 已自动带上，也可单独引用）：
- `cn.pingyuanren:bs-assets-skin` —— 烘焙皮肤（atlas/png/json + 位图字体）
- `cn.pingyuanren:bs-assets-emoji` —— emoji + 头像图集
- `cn.pingyuanren:bs-assets-icons` —— bootstrap-icons 图标集

素材的 classpath 路径固定为 `cn/pingyuanren/bs/ui/{skin,emoji,icons}/**`，无论来自 `bs-assets-*` 还是使用者自带，加载代码（`Gdx.files.internal("cn/pingyuanren/bs/ui/skin/...")`）都一样。

## 二、`bs-ui-core-all`：开箱即用

```groovy
implementation 'cn.pingyuanren:bs-ui-core-all:0.3.0'
```

素材在 jar 里，`Gdx.files.internal(...)` 直接能读，无需额外配置。**不能**用 pak 加密（素材是 jar 里的明文）。

## 三、`bs-ui-core` + pak：自带素材 + 加密打包

适合：想用自己的素材、或想把素材加密成单个 `assets.pak` 再分发。

### 流程

1. **准备素材目录**（classpath 路径要对应，如 skin 放在 `<dir>/cn/pingyuanren/bs/ui/skin/**`）。素材可以是自己做的，也可以从 `bs-assets-skin` 等 jar 里解出来改。

2. **打包**：跑 `cn.pingyuanren.bs.res.PakPacker`（在 `bs-ui-core` 里），参数 `<资源目录> <classpath前缀> <输出pak>`：
   ```groovy
   // 你 app 的 build.gradle 里（任务必须放消费模块，不能放 core——见 resource-encryption-design.md P2 坑）
   def pakFile = file("${buildDir}/pak/assets.pak")
   tasks.register('packResources', JavaExec) {
     dependsOn ':bs-ui-core:classes'   // 或你引用 core 的工程名
     classpath = configurations.runtimeClasspath
     mainClass = 'cn.pingyuanren.bs.res.PakPacker'
     args = [file('src/main/resources/cn/pingyuanren/bs/ui/skin').absolutePath,
             'cn/pingyuanren/bs/ui/skin',
             pakFile.absolutePath]
     inputs.dir file('src/main/resources/cn/pingyuanren/bs/ui/skin')
     outputs.file pakFile
   }
   sourceSets.main.resources.srcDir(file("${buildDir}/pak"))
   processResources.dependsOn('packResources')
   ```
   产出 `assets.pak`（P2 明文；P3 加密+压缩后同一接口）。

3. **启动时加载 pak**：在你 app 的 `create()` 最早期（早于 `BsUI.init()` 等任何 `Gdx.files.internal`）调一次：
   ```java
   cn.pingyuanren.bs.res.PakBootstrap.init();
   ```
   它会从 classpath 读 `assets.pak`，用 `FileResourcePack` 解析，包装 `Gdx.files`。之后所有命中 pak 的 `internal(...)` 透明走 pak。

### 加密

- **cipher = ChaCha20**（P3 已接入）：pak 索引和条目经 ChaCha20 加密（密钥 `PakKeys.KEY`，4-long XOR 混淆存 core），运行时 `FileResourcePack` 透明解密。**纯 Java、零依赖**，JVM 和 TeaVM wasm-gdc 通用，**无需按平台抽象**（javax.crypto 在 wasm-gc 不可用，故自写 ChaCha20）。
- **`PakBootstrap.init()` 正式行为**：classpath 有 `assets.pak` 就加载包装，没有就跳过。**无需任何 -D 开关**。
- **没做压缩**（保持简单；BPK1 格式 eflags 已支持，后续可开 DEFLATE）。

**模式**：开发（`buildRelease` / lwjgl3 `gradle run`）= 散列明文资源、不打包；发布（桌面 `distWinSettings` / web `releasePak`）= 打**加密** pak。

**换 key**：改 `cn.pingyuanren.bs.res.PakKeys` 的 PARTS/MASK 常量（建议随机值），打包器和读取器都引用 `PakKeys.KEY`，自动一致。

### 可执行 jar 打包示例（lwjgl3 桌面端）

参考 `lwjgl3/build.gradle` 的 `distWinSettings` 任务：fat jar 把代码 + core i18n + `assets.pak` 全打进一个可执行 jar，**排除明文** skin/emoji/icons/demo-i18n（它们只在 pak 里）。关键点：
- `from sourceSets.main.output` + `from { configurations.runtimeClasspath.collect { zipTree(it) } }`；
- `dependsOn classes, configurations.runtimeClasspath`（**必须**，否则依赖 jar 没构建好/陈旧）；
- `exclude('cn/pingyuanren/bs/ui/{skin,emoji,icons}/**', 'cn/pingyuanren/bs/demo/i18n/**')`；
- `manifest { attributes 'Main-Class': '...' }`。
- `assets.pak` 由 `packResources`（多根：skin/emoji/icons/demo-i18n）产出，经 `sourceSets.main.resources.srcDir` 进 jar。

运行：`java -jar app.jar`（自包含，无需外部文件；pak 在 jar 内）。
