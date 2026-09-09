# ZCode Mobile

Unofficial Android Remote Client for ZCode.

ZCode Mobile allows users to remotely control their ZCode Desktop sessions from Android devices.

This app is **not** a coding agent running on the phone. The phone is a control surface. ZCode Desktop on your computer remains the execution environment for code, terminal, Git, files, browser, MCP, Skills, and Agent work.

```
Android App
  → ZCode Remote Control / Remote Web
    → ZCode Desktop on your computer
      → local code, terminal, Git, files, browser, MCP, Skill, Agent
```

## Features

- Scan a ZCode Remote Control QR code
- Paste a Remote URL
- Save the connection with encrypted storage
- Auto-open the last session on launch
- Load the official ZCode Remote page in a WebView
- Back / forward, refresh, reconnect
- Network and Remote error handling
- Speech-to-text task input
- Share-to-app entry
- Structured task list from visible Remote page state (v0.2)
- Task / approval / artifact event detection via DOM observer (v0.2)
- Real task-completed and approval notifications (v0.2)
- Artifact preview (Markdown, HTML, image, PDF, code, JSON)
- Settings for voice, notifications, downloads, and WebView data

## v0.2

- Task event detection from the Remote page (MutationObserver, no private API)
- Approval detection with a two-signal rule (dialog/buttons/waiting/command)
- Artifact detection from visible file links
- Structured Home / Task Detail UI
- Notification integration for TaskCompleted, TaskFailed, ApprovalRequired
- Developer debug panel (debug builds)
- Unit tests for parse, status, dedupe, approval, artifacts, URL, storage codec

Foreground only: Android may pause WebView JavaScript when the app is backgrounded. v0.2 does **not** claim realtime background monitoring, and does not use a persistent foreground service or wake lock.

If the observer cannot read the page, the WebView still works. Native task UI simply stays empty instead of showing fake data.

## Architecture

```
Android phone = controller
ZCode Desktop = executor
```

MVP stack:

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Android WebView (official Remote Control page)
- CameraX + ML Kit barcode scanning
- Android SpeechRecognizer
- Android Keystore AES-GCM
- DOM observer + JavaScript bridge (visible page state only)
- DataStore
- WorkManager
- Notification API

The first version does **not** reverse-engineer ZCode, forge private APIs, or patch the desktop app. If ZCode later publishes an official API, SDK, WebSocket protocol, or deep link, this client can grow into a native remote client.

## Installation

1. Enable unknown sources / install from this computer on your Android device.
2. Copy `app/build/outputs/apk/debug/app-debug.apk` to the phone.
3. Install the APK.

Minimum Android version: **8.0 (API 26)**.

## Build

Requirements:

- JDK 17
- Android SDK with `platforms;android-36` and Build Tools 36
- Network access to Google Maven

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
./gradlew assembleDebug
```

Debug APK:

```
app/build/outputs/apk/debug/app-debug.apk
```

Release APK (locally signed; replace the keystore before publishing):

```bash
./gradlew assembleRelease
```

Output:

```
app/build/outputs/apk/release/app-release.apk
```

`local.properties` must contain your SDK path:

```
sdk.dir=/path/to/Android/sdk
```

## Usage

1. On the computer, open ZCode Desktop and enable **Remote Control**.
2. On the phone, open ZCode Mobile.
3. Scan the QR code, or paste the Remote URL (`http://` or `https://`).
4. Tap **连接**, then **打开 ZCode**.
5. Use the official Remote page to talk to the desktop agent.
6. Optional: tap **语音任务**, speak, then **发送到 ZCode**.

If a Remote URL is already saved, launch goes straight to the Remote page.

## Security

- Remote URLs are encrypted with AES-GCM. The key is stored in Android Keystore and never written to disk in plaintext.
- UI redacts path tokens. Logs never print tokens, cookies, Authorization headers, or session IDs.
- Release builds keep sensitive logging off.
- HTTPS Remote pages block mixed HTTP content.
- `file://` and content access stay off unless a future setting explicitly needs them.
- LAN Remote Control often uses HTTP on a private network; cleartext is allowed for that case only at the OS network-security layer.

This client talks only to the Remote URL you provide. It does not include a hidden C2, account dump, or unofficial ZCode protocol.

## Roadmap

- Native client if ZCode publishes an official remote protocol
- Real task-status stream instead of demo notifications
- Real permission-approval protocol
- Richer artifact sync from the desktop session
- Multi-device profiles
- Biometric unlock for saved connections

## Disclaimer

This is an unofficial community client for ZCode.

ZCode and related trademarks belong to their respective owners.

This project does not impersonate an official ZCode product.
