<h1 align="center">ZCode Mobile</h1>

<p align="center">Unofficial Android remote client for controlling ZCode Desktop sessions.</p>

<p align="center">
  <a href="./README.md">English</a> | <a href="./README.zh-CN.md">简体中文</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-4B5563?style=flat-square" alt="Platform: Android 8.0+">
  <img src="https://img.shields.io/badge/Target%20SDK-API%2036-4B5563?style=flat-square" alt="Target SDK: API 36">
  <img src="https://img.shields.io/badge/Kotlin-2.3.21-3776AB?style=flat-square" alt="Kotlin: 2.3.21">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-3776AB?style=flat-square" alt="Jetpack Compose">
</p>

ZCode Mobile allows developers to monitor and control their ZCode Desktop coding sessions from an Android device. The phone functions strictly as a mobile control surface, while the desktop workstation executes code, runs terminal workflows, manages Git repositories, and orchestrates Agent tasks.

## Highlights

| Highlight | Why it matters |
|---|---|
| Zero-setup pairing | Scan a QR code or paste a Remote URL to establish an encrypted session instantly. |
| Hardware-backed security | Connection tokens are encrypted using AES-GCM via Android Keystore and never written in plaintext. |
| Hands-free task input | Integrated speech-to-text enables natural voice prompts to be dispatched directly to your agent. |
| System share integration | Forward text snippets, logs, and instructions directly from other mobile apps to your workspace. |
| Comprehensive previews | Built-in rendering support for Markdown, HTML, images, PDF documents, code, and JSON artifacts. |

## Architecture

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

The application communicates directly with the official ZCode Remote Control endpoint via an embedded WebView. It does not reverse-engineer private protocols or alter desktop application binaries.

## Quick Install

Minimum requirement: Android 8.0 (API level 26) or higher.

1. Build or download the debug APK:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
2. Enable installation from unknown sources on your Android device.
3. Transfer the APK to your device and run the installer.

## Quick Start

1. Open ZCode Desktop on your computer and enable **Remote Control**.
2. Launch ZCode Mobile on your Android device.
3. Tap **Scan QR** to capture the desktop screen code, or paste the `http://` / `https://` Remote URL.
4. Tap **Connect** to load the official session workspace.
5. Review task execution and send instructions directly to your desktop agent.

## Workflow & Operations

### Voice Task Dispatch

1. Open an active connection session.
2. Tap **Voice Task** from the toolbar.
3. Speak your prompt or coding instruction into the device microphone.
4. Review the transcribed text and tap **Send to ZCode**.

### Artifact Review

The client inspects rendered outputs across multiple formats:
- Markdown and rich text reports
- Source code snippets and JSON structures
- Static image assets and PDF documents

## Security & Privacy

- Sensitive credentials: All Remote URLs and tokens are encrypted with AES-GCM inside Android Keystore.
- Output sanitization: UI views automatically redact path tokens, and debug logs strip authentication headers, cookies, and session IDs.
- Network policy: HTTPS sessions strictly reject mixed HTTP content. Cleartext HTTP traffic is limited to local area networks (LAN) per Android network security configuration.

## Build from Source

Prerequisites:
- JDK 17
- Android SDK with Platform 36 and Build Tools 36
- Configured `local.properties` pointing to your Android SDK directory:
  ```properties
  sdk.dir=/path/to/Android/sdk
  ```

Build commands:

```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK (locally signed with debug keystore)
./gradlew assembleRelease
```

Build outputs:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## Roadmap

- Native communication protocol adapter if ZCode releases an official API or SDK
- Live bi-directional task status streaming
- Interactive permission and tool approval workflows
- Expanded multi-device session profiles
- Biometric authentication (fingerprint / face unlock) for saved connections

## Disclaimer

This is an unofficial, community-driven client for ZCode. ZCode and associated trademarks are the property of their respective owners. This project is not affiliated with or endorsed by the creators of ZCode.

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=245678000000%2FZCode-Mobile&type=Date)](https://star-history.com/#245678000000/ZCode-Mobile&Date)
