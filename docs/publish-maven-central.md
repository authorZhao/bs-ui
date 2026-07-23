# 发布到 Maven Central（操作手册）

> 目标：让别人 `implementation 'com.git.bs:bs-ui-core:0.1.x'` 就能用（其实是 group 改后，见下）。
> 状态：gradle 配置已就绪、**默认关**（`-PcentralRelease=true` 才启用）。下面是你要准备的东西 + 流程。

## 0. 当前最大 blocker：namespace 不可验证

Maven Central 要求发布坐标的 group（namespace）**必须能验证归属**。当前各模块 `group = 'com.git.bs'`，反向域名是 `bs.git.com`——**这个域名不属于本项目**，Central 验不了。

两个可行改法（二选一，**改完所有模块的 `group` 同步**）：

| 方案 | group 改成 | 验证方式 | 前提 |
|------|-----------|---------|------|
| GitHub 流 | `com.github.authorZhao` | Central Portal GitHub 授权 / 建一个约定 repo | 你有 `github.com/authorZhao`（LICENSE 里就是这个） |
| 自有域名 | `cn.pingyuanren` | DNS TXT 记录 | 你拥有 `pingyuanren.cn`（部署文档里那个域名） |

> 推荐 `io.github.authorZhao` 或 `com.github.authorZhao`（GitHub 流，无需域名/备案）。改 group 后，`publish.gradle` 里 POM 的 `scm.url` 也同步（目前写的是 `github.com/authorZhao/bs-ui`，group 改成 `com.github.authorZhao` 正好对上）。

## 1. 你要准备的东西（checklist，我没法替你做）

| # | 项目 | 怎么弄 |
|---|------|--------|
| 1 | **Central Portal 账号** | 注册 `central.sonatype.com`（2024 后 OSSRH 已废，统一用 Portal） |
| 2 | **User Token** | Portal → Account → Generate User Token，得到 username + password（不是登录密码） |
| 3 | **namespace 验证** | 选定 group（见 §0）后，在 Portal 里 Add Namespace 并完成验证（GitHub repo 或 DNS TXT） |
| 4 | **GPG 签名密钥** | `gpg --gen-key`；`gpg --list-secret-keys --keyid-format LONG` 拿 keyId；`gpg --export-secret-keys --armor <keyId>` 导出私钥；把公钥上传到 keyserver（`gpg --keyserver keyserver.ubuntu.com --send-keys <keyId>`） |
| 5 | **gradle.properties**（本地 `~/.gradle/gradle.properties`，**不要提交到 git**） | 填 4 项（见下） |

`~/.gradle/gradle.properties` 要填的 4 项：
```properties
centralUser=<Portal User Token username>
centralToken=<Portal User Token password>
signingKey=<gpg --export-secret-keys --armor 的完整输出，多行用 \n>
signingPassword=<gpg 密钥的 passphrase>
```

## 2. gradle 配置（已就绪、默认关）

在 `gradle/publish.gradle` 末尾有一段 `if (project.hasProperty('centralRelease')) { ... }`：
- `apply plugin: 'signing'` + `useInMemoryPgpKeys(signingKey, signingPassword)` 签名。
- 追加一个 `centralPortal` 仓库（Central Portal 端点）。

**不传 `-PcentralRelease` 时整段不生效**——日常 `publishToMavenLocal` 不受影响。

## 3. 发布步骤

```bash
# 1. group 改成可验证 namespace（§0），同步 POM scm/url
# 2. 本地 gradle.properties 配好 4 项（§1）
# 3. 构建 + 签名 + 上传（每个要发布的模块都跑；或写个聚合任务）
./gradlew clean build publishToMavenLocal                       # 先本地验证产物 OK
./gradlew :core:publishAllPublicationsToCentralPortal -PcentralRelease=true
./gradlew :common:publishAllPublicationsToCentralPortal -PcentralRelease=true
# ... 对 assets-skin / assets-emoji / assets-icons / core-all 同理
```

上传后在 `central.sonatype.com` 看 Deployment 状态：**新流程自动校验 + 发布**（无需手动 release）。校验失败会在 Portal 报错（最常见：签名缺失、POM 元数据不全、javadoc/sources jar 缺失——这些 publish.gradle 都已配，group 改对就行）。

> 发布端点 URL 偶有变动，以 Central Portal 官方「Publishing via Gradle」文档为准（publish.gradle 里写的是常用值，需时核对）。

## 4. POM 元数据（已配，改 group 后检查）

`publish.gradle` 的 POM 已含 `name/description/url/licenses/developers/scm`（Apache 2.0 + 附加归属条款）。改 group 时确认 `scm.url`、`url` 还指向正确的 GitHub 仓库即可。sources jar + javadoc jar 也已配（Central 强制）。

## 5. 还差什么（一句话）

**就差你的 5 样**：Central 账号、User Token、namespace 选定并验证、GPG 密钥、本地 gradle.properties。这些是账号/密钥类，我配不了；gradle 这边的签名+仓库+元数据都已配好、默认关。

## 6. 备注

- **第一次发布**前，务必先在 Portal 把 namespace 验证跑通（没验证，上传会被拒）。
- **版本号**：Central release 一旦发布不可覆盖；要改就升版本号。SNAPSHOT 可重复覆盖。
- **签名**：用 `useInMemoryPgpKeys`（CI 友好，不依赖本地 keyring 文件）。公钥必须传到 keyserver，否则 Central 校验过不了。
