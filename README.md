# ZPL Label Viewer

[![Build](https://github.com/Milimarty/zpl-label-viewer/actions/workflows/build.yml/badge.svg)](https://github.com/Milimarty/zpl-label-viewer/actions/workflows/build.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

An **IntelliJ Platform plugin** that lets you preview and debug ZPL II shipping labels directly inside your IDE — no Zebra printer, no browser, no external tools needed.

> **Rendering engine:** Powered by [zpl-renderer](https://github.com/Milimarty/zpl-renderer) — a standalone open-source ZPL II library you can also use independently in any JVM project.

---

## Features

- **Auto-detect** — paste raw ZPL or Base-64 encoded ZPL; the plugin detects the format automatically
- **Local renderer** — renders offline using the embedded [zpl-renderer](https://github.com/Milimarty/zpl-renderer) library
  - Text fields, fonts, word-wrap
  - Code 128, QR Code, EAN-8, EAN-13, UPC-A, Data Matrix barcodes
  - Graphic boxes, ellipses, diagonal lines
  - `^GFA` compressed bitmaps
- **Labelary API** — optional fallback to [labelary.com](https://labelary.com) for unsupported commands
- **Render source selector** — Local only / Local → API fallback / API only
- **Fit-to-window preview** — image scales to fit the panel; shows zoom level in the status bar
- **200 / 300 DPI** — selectable output resolution
- **Save as PNG** — export rendered label to disk
- **View ZPL** — decode and inspect the ZPL source from a Base-64 input
- **Clear cache** — flush in-memory image caches

---

## Installation

### From JetBrains Marketplace

*(Coming soon — the plugin will be published to the marketplace.)*

### From disk (manual)

1. Download the latest `.zip` from [Releases](https://github.com/Milimarty/zpl-label-viewer/releases).
2. In your IDE: **Settings → Plugins → ⚙ → Install Plugin from Disk…**
3. Select the downloaded `.zip` and restart.

---

## Usage

1. Open the **ZPL Label Viewer** tool window (right side panel).
2. Paste your ZPL or Base-64 string into the input area.
3. Select **Input** (Auto-detect / ZPL / Base-64) and **Source** (Local / Local→API / API).
4. Click **Render**.
5. The label appears in the preview panel scaled to fit.
6. Optionally click **Save PNG** to export.

### Keyboard tips

- After pasting input, press **Render** immediately — auto-detect runs as you type.
- Use **View ZPL** to inspect the decoded ZPL when working with Base-64 encoded labels.

---

## Architecture

```
ZplToolWindowFactory      ← IntelliJ tool window UI
        │
        ├─ ZplRenderer    ← zpl-renderer library (local rendering)
        │     └─ ZplEngine → ControlHandler, TextHandler,
        │                    RectangleHandler, BitmapHandler, BarcodeHandler
        │
        └─ Labelary API   ← OkHttp3 fallback (labelary.com)
```

The plugin intentionally keeps a clean boundary between UI concerns (`ZplToolWindowFactory`) and rendering logic (`ZplRenderer` from the library). This makes it straightforward to update the renderer independently.

---

## Building from Source

### Prerequisites

- JDK 17+
- PhpStorm / IntelliJ IDEA (for testing the plugin in a sandbox)
- Gradle 9.x (wrapper included)

### Build the plugin ZIP

```bash
./gradlew buildPlugin -x buildSearchableOptions
```

The artifact is placed in `build/distributions/zpl-label-viewer-1.1.0.zip`.

> **Note:** `buildSearchableOptions` requires launching a headless IDE instance.
> Skip it during development with `-x buildSearchableOptions` if PhpStorm is already running.

### Run in sandbox IDE

```bash
./gradlew runIde
```

### Verify plugin compatibility

```bash
./gradlew verifyPlugin
```

---

## Signing (for release)

Place your signing credentials in the project root (these are gitignored):

```
chain.crt
private.pem
```

Set the password:

```bash
export SIGN_PASSWORD=your_password
./gradlew signPlugin
```

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
