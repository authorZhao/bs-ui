# 归属与许可证（给使用者）

## 1. bs-ui 自身

bs-ui 基于 **Mozilla Public License 2.0（MPL 2.0）**，可商用、修改、再分发，
并可与私有 / 闭源代码组合（MPL 是文件级弱 copyleft，义务只覆盖 bs-ui 自己的源文件）。

**唯一核心义务**：如果你**修改了 bs-ui 的源文件**并对外分发（源码或可执行形式），
这些被修改的文件必须以 MPL 2.0 提供源码；未修改的文件无开源义务。

### 怎么合规

- 只**原样使用**（未改源码）：无任何开源义务，宽松程度等同 MIT/Apache 场景。
- **改过 bs-ui 源文件**并分发：把你改动的那些文件以 MPL 2.0 公开源码即可（其余代码仍可闭源）。

### 致谢（可选，不强制）

MPL 2.0 不要求在程序内展示致谢。如果你愿意致谢，在 README 或 app 的「关于」界面加一行即可（示例）：

> Powered by **bs-ui** (MPL-2.0) — https://github.com/authorZhao/bs-ui

如果改过 bs-ui 源码，额外注明「(modified)」。库自带 `BsAboutDialog`，一行代码即可弹出预填好的致谢弹窗。

## 2. 第三方依赖

### 2.1 运行时依赖（你的 app 分发时会带上，需遵守许可）

| 依赖 | 许可证 | 用途 |
|------|--------|------|
| [libGDX](https://libgdx.com)（gdx / -freetype / -backend-lwjgl3 / -platform …） | Apache 2.0 | 底层游戏/UI 框架 |
| [gdx-teavm](https://github.com/xpenatan/gdx-teavm)（backend-web / -freetype-teavm） | Apache 2.0 | TeaVM/web 后端 |
| [SLF4J](http://www.slf4j.org) | MIT | 日志门面 |

> 这些是 Apache 2.0 / MIT（宽松许可，可商用）。**Apache 2.0 要求分发时附 LICENSE + NOTICE**——你的 app 分发包里带上对应 LICENSE 即可（通常打在 jar 的 `META-INF/` 或文档里）。

### 2.2 仅构建/工具期依赖（**用户 app 运行时不需要**，不影响下游分发）

这些只在 bs-ui 的**开发/构建工具**里用到，**不是 `bs-ui-core` 的运行时依赖**——使用者不需要引入它们，也不用为它们担心许可合规：

| 依赖 | 许可证 | 用途（仅 bs-ui 工具内部） |
|------|--------|--------------------------|
| [Apache XMLGraphics Batik](https://xmlgraphics.apache.org/batik/) | Apache 2.0 | icon 打包工具：Bootstrap Icons SVG → PNG+atlas 转换（`iconpkg` 开发工具用，运行时不涉及） |
| [Alibaba fastjson2](https://github.com/alibaba/fastjson2) | Apache 2.0 | 桌面端皮肤导出的 JSON 处理（仅桌面 lwjgl3 导出功能用，web/teavm 不涉及） |
| [Project Lombok](https://projectlombok.org) | MIT | 编译期注解处理器（`@Slf4j` 等），`compileOnly` 不进运行时 jar |

> 如果你**只用** `bs-ui-core`（不含 `bs-skin-export` 工具），这三个不会出现在你的 classpath 里。

## 3. LICENSE / NOTICE 文件

- `LICENSE`：bs-ui 的 MPL 2.0 全文。
- `NOTICE`：bs-ui 的归属声明 + 主要第三方一览。
- 分发你自己的 app 时：保留 bs-ui 的 LICENSE/NOTICE；若修改过 bs-ui 源文件并分发，按 MPL 2.0 公开这些文件的源码，并按需附上所依赖库的许可。

## 4. 反过来：贡献回 bs-ui

提 PR 即视为你同意以 MPL 2.0 许可贡献代码（与项目一致）。重大改动请在 PR 里说明。
