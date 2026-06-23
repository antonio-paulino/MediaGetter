# MediaGetter

<img src="icons/MediaGetter.ico" width="72" align="left" alt="MediaGetter">

A **Windows desktop app** for downloading video and audio from YouTube (and the
1000+ other sites supported by yt-dlp). Search by keyword or paste a URL, pick
your quality and format, and download — with real-time progress (percentage,
speed and ETA).

<br clear="left">

Built with **Kotlin + Compose Desktop (Material 3)**. Under the hood it drives
[`yt-dlp`](https://github.com/yt-dlp/yt-dlp) and [`ffmpeg`](https://ffmpeg.org/),
both of which are downloaded automatically on first launch — you don't need to
install anything yourself.

---

## Features

### Download
- 🔎 **Search** YouTube or **download directly from a URL**
- 📋 **Playlists & channels** — detected automatically; pick which entries to
  download and add them all to the queue at once
- 🗂️ **Download queue** — every download runs through a queue with per-item
  progress, **cancel / retry / remove**, and **parallel downloads** (1–4 at once)
- 🎚️ Pick **video quality** (with FPS and file size) or **audio only**
- 🎵 **Audio conversion** — MP3 / M4A / Opus / FLAC / WAV, or keep the original
  (cover art is embedded for MP3/M4A/FLAC)
- ✂️ **Clip a section** — download only a `start–end` range
- 🛡️ **SponsorBlock** — automatically remove sponsors, intros, outros, etc.
- 💬 **Subtitles** — embed subtitles (including auto-generated), by language
- 📑 **Chapters** — embed chapters into the file
- ⚡ **One-click presets** — Best, 1080p, Smallest, MP3
- 📊 **Real-time progress** — percentage, speed, ETA and phase
  (downloading / merging / finalizing)
- 📁 Choose the **output folder** (per download or as a default)
- 🗂️ Completion notification with an **Open folder** button

### App
- 🌙 **Light / dark theme** (remembered between sessions)
- ⚙️ **Settings** persisted to `~/.mediagetter/settings.json`:
  default folder, default audio format, speed limit, parallel downloads,
  default options
- 🔧 **Reliability** — shows the yt-dlp version and lets you **update it in-app**;
  optional speed limit; optional JavaScript runtime (deno) for maximum
  extraction compatibility
- 📦 Metadata and thumbnail embedded into the final file

Downloads are saved to your **Downloads** folder by default.

---

## Using the app (end user)

You don't need Java or anything else installed — the app bundles everything.

1. Get the app (see [Packaging](#packaging) or your build artifacts).
2. **Portable version:** open the `MediaGetter` folder and **double-click**
   `MediaGetter.exe`.
3. **Installer:** run the `.msi` and follow the wizard (it creates a Start menu
   shortcut).

> On first launch the app downloads `yt-dlp` and `ffmpeg` into
> `%USERPROFILE%\.mediagetter`. This can take a few seconds.

---

## Development

Requirements: **JDK 17+** (tested with JDK 21). Gradle is included via the wrapper.

```powershell
# Run from source
.\gradlew run
```

---

## Packaging

### Portable version (double-click, no install)

```powershell
.\gradlew createDistributable
```

Output: `build\compose\binaries\main\app\MediaGetter\`, containing
`MediaGetter.exe` plus a bundled Java runtime. To distribute it, copy/zip the
**whole folder** (the `.exe` needs the sibling folders next to it).

### `.msi` installer

```powershell
.\gradlew packageMsi
```

Output: `build\compose\binaries\main\msi\`. Compose Desktop downloads the WiX
Toolset automatically — you don't need to install it.

---

## Icon

The icon lives at `icons/MediaGetter.ico` (multi-resolution: 16→256 px) and is
wired into the build in `build.gradle.kts` (`windows { iconFile.set(...) }`).
The window/taskbar icon is loaded from `src/main/resources/icon.png`.

To regenerate it (e.g. after changing the design):

```powershell
powershell -ExecutionPolicy Bypass -File build-tools\make-icon.ps1
```

---

## Project structure

```
src/main/kotlin/pt/paulinoo/mediagetter/
├─ Main.kt                  # app entry point + window
├─ model/                   # data models (VideoInfo, formats, options, settings, presets)
├─ service/
│  ├─ ProcessRunner.kt      # runs external processes with streamed output
│  ├─ YtDlpService.kt       # info, search and download via yt-dlp
│  ├─ YtDlpManager.kt       # installs/updates yt-dlp, reports its version
│  ├─ FfmpegManager.kt      # installs ffmpeg
│  ├─ DenoManager.kt        # optional JS runtime, downloaded on demand
│  ├─ YtDlpConfig.kt        # process-wide yt-dlp knobs (JS runtime, rate limit)
│  └─ SettingsManager.kt    # loads/saves settings as JSON
├─ viewmodel/               # UI state + download queue (MVVM)
├─ ui/                      # composables (App, settings, queue, playlist, cards, theme, logo)
└─ util/Format.kt           # size and duration formatting
```

---

## License

Personal use. Respect YouTube's Terms of Service and the copyright of the
content you download.
