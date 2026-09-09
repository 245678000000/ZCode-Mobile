<h1 align="center">ZCode Mobile</h1>

<p align="center">专为 ZCode Desktop 打造的非官方 Android 移动端远程控制客户端。</p>

<p align="center">
  <a href="./README.en.md">English</a> | <a href="./README.md">简体中文</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-4B5563?style=flat-square" alt="支持平台：Android 8.0+">
  <img src="https://img.shields.io/badge/Target%20SDK-API%2036-4B5563?style=flat-square" alt="目标 SDK：API 36">
  <img src="https://img.shields.io/badge/Kotlin-2.3.21-3776AB?style=flat-square" alt="编程语言：Kotlin 2.3.21">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-3776AB?style=flat-square" alt="UI 框架：Jetpack Compose">
</p>

ZCode Mobile 允许开发者通过 Android 移动设备实时监控并远程操控电脑上的 ZCode 编程会话。手机本身定位为**移动控制面板**，而代码编译、终端执行、Git 操作、MCP 工具调用以及 Agent 深度推理等重型工作，全部保留在电脑端的 ZCode 桌面运行环境中。

## 核心亮点

| 亮点 | 对使用者的价值 |
|---|---|
| 秒级扫码配对 | 支持扫描屏幕二维码或粘贴远程地址，即刻与桌面端建立加密连接。 |
| 硬件级凭据保护 | 连接凭据通过 Android Keystore 进行 AES-GCM 高强度加密，私钥绝不明文落盘。 |
| 语音任务录入 | 内置语音转文本功能，无需手机敲键盘即可快速向桌面 Agent 派发编程指令。 |
| 系统级文本分享 | 支持通过系统分享菜单，一键将其他应用中的报错信息、文本片段发送至工作区。 |
| 多格式产物预览 | 支持在移动端查阅 Markdown 文档、HTML、代码文件、JSON 数据、图片及 PDF 产物。 |

## 架构设计

```text
┌───────────────────────────────┐                 ┌───────────────────────────────┐
│        Android Client         │                 │         ZCode Desktop         │
│  (Control Surface / API 26+)  │                 │    (Local Execution Engine)   │
├───────────────────────────────┤                 ├───────────────────────────────┤
│  Compose UI & Navigation      │                 │  Agent Core & Tool Runner     │
│  CameraX & Barcode Scanner    │──[Remote URL]──▶│  Integrated Terminal & Git   │
│  SpeechRecognizer Task Input  │                 │  MCP Servers & Skills Engine  │
│  Encrypted Keystore Storage   │◀─[Status/Data]──│  Local Workspace & Browser    │
│  Embedded WebView Controller  │                 │  Desktop Remote Web Server    │
└───────────────────────────────┘                 └───────────────────────────────┘
```

本客户端通过内嵌 WebView 直接加载官方 ZCode Remote Control 界面，不针对桌面应用进行逆向破解或伪造私有协议，确保稳定与安全。

## 快速安装

最低系统要求：Android 8.0（API 级别 26）或更高版本。

1. 获取或编译 Debug 版本 APK：
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
2. 在 Android 设备上开启“允许安装未知来源应用”权限。
3. 将 APK 文件传输至手机并完成安装。

## 快速开始

1. 在电脑上打开 ZCode Desktop，进入设置开启 **Remote Control**。
2. 在手机上启动 ZCode Mobile。
3. 点击 **扫码连接** 扫描桌面端二维码，或直接粘贴 `http://` / `https://` 开头的远程地址。
4. 点击 **连接** 并进入会话控制台。
5. 即可在手机上查看执行进度，或向桌面端 Agent 发送新指令。

## 常见操作工作流

### 语音派发任务

1. 进入活跃的会话控制页面。
2. 点击工具栏的 **语音任务** 图标。
3. 对着手机麦克风清晰表述任务需求。
4. 核对识别转换后的文字，点击 **发送到 ZCode**。

### 产物与文件预览

移动端内建各类产物渲染与预览能力：
- 格式化 Markdown 报告与设计文档
- 语法高亮的代码文件与 JSON 数据
- 运行截屏、图片资源与 PDF 规范文件

## 安全与隐私

- 凭据安全：所有 Remote URL 与访问令牌均使用 AES-GCM 算法加密保存在 Android Keystore 中，不存留明文。
- 日志脱敏：界面对敏感路径令牌进行掩码遮蔽，运行日志严格过滤 Session ID、Cookie 及认证请求头。
- 网络防护：HTTPS 连接强制拦截 HTTP 混合明文资源；明文 HTTP 传输严格限制于私有局域网环境。

## 源码编译

编译依赖要求：
- JDK 17
- Android SDK（包含 Platform 36 及 Build Tools 36）
- 在项目根目录的 `local.properties` 中指定 SDK 路径：
  ```properties
  sdk.dir=/path/to/Android/sdk
  ```

编译命令：

```bash
# 编译 Debug APK
./gradlew assembleDebug

# 编译 Release APK（使用本地调试密钥签名）
./gradlew assembleRelease
```

构建输出路径：
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## 开发路线

- 若 ZCode 官方发布正式开放 API 或 SDK，演进为原生通信客户端
- 支持双向实时的任务状态流式订阅
- 接入细粒度工具执行授权与人工审批流
- 多设备连接档案快速切换
- 增加生物识别（指纹 / 面容）解锁已保存的连接

## 免责声明

本项目为 ZCode 的非官方社区客户端。ZCode 及其相关商标归其合法持有人所有。本项目与 ZCode 官方团队无商业隶属或官方背书关系。

## Star 历史

[![Star History Chart](https://api.star-history.com/svg?repos=245678000000%2FZCode-Mobile&type=Date)](https://star-history.com/#245678000000/ZCode-Mobile&Date)
