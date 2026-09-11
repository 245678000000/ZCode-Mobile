<h1 align="center">ZCode Mobile</h1>

<p align="center">Unofficial Android remote-control client for ZCode Desktop.</p>

<p align="center">
  <a href="./README.en.md">English</a> | <a href="./README.md">简体中文</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-4B5563?style=flat-square" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Target%20SDK-API%2036-4B5563?style=flat-square" alt="API 36">
  <img src="https://img.shields.io/badge/Kotlin-2.3.21-3776AB?style=flat-square" alt="Kotlin 2.3.21">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-3776AB?style=flat-square" alt="Jetpack Compose">
</p>

The phone is a control surface. Code, terminal, Git, MCP, Skills and the Agent keep running in ZCode Desktop on your computer; the phone uses ZCode's own "Mobile Remote Control" page to watch progress, send tasks and receive notifications.

```text
Android phone                        Computer
┌──────────────────────┐            ┌──────────────────────┐
│ ZCode Mobile         │  Remote URL │ ZCode Desktop        │
│  · scan / paste link │───────────▶│  · Remote Control page│
│  · official Remote   │◀───────────│  · Agent / terminal   │
│    page in a WebView │  page state │  · Git / MCP / Skills │
│  · task list, alerts │            │                       │
└──────────────────────┘            └──────────────────────┘
```

No reverse engineering, no forged private protocol: the WebView loads the official Remote page, and tasks / approval prompts are read from what is **visible** on that page. When nothing can be read the page still works; the native list simply stays empty.

## Screens

<p align="center">
  <img src="docs/screenshots/overview.png" alt="Home, Connect, Approval, Task, Settings" width="100%">
</p>

The design follows ZCode Desktop: white ground, system sans, plain hairline lists instead of cards, one composer, a black round send button, orange reserved for "needs your confirmation". Dark mode follows the system.

> These are design renders of the Compose implementation, not device screenshots.

## Features

| | |
|---|---|
| **Connect** | Scan the QR code on the desktop, or paste the link from "Copy link". The link is stored encrypted with an AES-GCM key held in Android Keystore. |
| **Composer on Home** | Type like on the desktop; the text is filled into the Remote page's input. Voice input is available too. |
| **Task list** | Tasks read from the Remote page, grouped into waiting / active / recent. |
| **Notifications** | Task completed, task failed and approval-required notifications while the app is in the foreground. |
| **Approval prompt** | Shows the command the Agent wants to run and a heuristic risk level. The app **never** clicks allow/deny for you; it takes you to the Remote page. |
| **Artifact preview** | Markdown, HTML, images, PDF, code, JSON. |
| **Share sheet** | Share error text from any app straight to the Agent. |

### Known limitations

- **Foreground only.** Android pauses WebView JavaScript in the background. No foreground service, no wake lock, no background monitoring claims.
- **Heuristic selectors.** The DOM selectors in `app/src/main/assets/zcode-selectors.json` were written from public docs and have not been validated against a live Remote page; the fixtures under `app/src/test/resources/fixtures` are hand-written stand-ins.
- **Approvals are read-only.** Allow/deny must be tapped on the Remote page.

## Install

Android 8.0 (API 26) or newer.

1. Download `zcode-mobile-*-release.apk` from [Releases](https://github.com/245678000000/ZCode-Mobile/releases), or build locally.
2. Allow installs from unknown sources and install it.

## Use

1. On the computer, open ZCode Desktop and click **Mobile Remote Control** at the bottom of the sidebar.
2. On the phone, open ZCode Mobile and **scan the QR code**, or click **Copy link** on the desktop and **paste the link** on the phone.
   - `https://` is accepted for any host; `http://` only for LAN, loopback, `.local`, link-local and CGNAT (Tailscale) hosts, because the link path carries the session secret.
3. Back on Home, type a task and send it, or tap **Open Remote page** for the full UI.
4. On Android 13+, the notification permission is requested the first time Home is shown, and again when a notification toggle is turned on in Settings.

## Build

JDK 17, Android SDK with Platform 36 and Build Tools 36, and `sdk.dir` set in `local.properties`.

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"   # macOS Homebrew example
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
./gradlew assembleRelease
```

Release signing is resolved in this order:

1. `RELEASE_STORE_FILE` / `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD` environment variables (CI)
2. `keystore.properties` at the repo root (git-ignored):
   ```properties
   storeFile=app/keystore/your-release.jks
   storePassword=…
   keyAlias=…
   keyPassword=…
   ```
3. The debug keystore, with a warning — installable, but a build signed on another machine cannot upgrade it.

### Publishing from GitHub Actions

`.github/workflows/release.yml` builds and attaches APKs to a GitHub Release on every `v*` tag. It refuses to run without a real keystore so every Release is signed with the same key and users can upgrade in place. Add these repository secrets once:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `base64 -i app/keystore/your-release.jks` |
| `RELEASE_STORE_PASSWORD` | keystore password |
| `RELEASE_KEY_ALIAS` | key alias |
| `RELEASE_KEY_PASSWORD` | key password |

## Security

- The Remote link is AES-GCM encrypted with a key that never leaves Android Keystore.
- Path tokens are masked in the UI; logs never print tokens, cookies, Authorization headers or session ids. Release builds log sanitized errors only.
- HTTPS pages block mixed content; third-party cookies are off; `file://` and content access are off.
- Public `http://` links are rejected.

## Layout

```text
app/src/main/java/app/zcode/mobile/
├── remote/        WebView, JS bridge, DOM observer, event parsing and dedupe
├── model/         Task / Approval / Artifact / event models
├── notification/  notifications
├── security/      Keystore-backed encrypted storage
├── ui/            Compose UI (theme / components / screens)
└── util/          URL validation, log sanitizing
app/src/main/assets/
├── zcode-observer.js       MutationObserver injected into the Remote page
└── zcode-selectors.json    DOM selector config
```

## Roadmap

- Validate and tighten selectors against a live Remote page DOM
- Native transport if ZCode publishes an official API / SDK
- Multiple device profiles
- Biometric unlock for the saved connection

## Disclaimer

Unofficial community project. ZCode and related trademarks belong to their respective owners; this project is not affiliated with or endorsed by ZCode.

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=245678000000%2FZCode-Mobile&type=Date)](https://star-history.com/#245678000000/ZCode-Mobile&Date)
