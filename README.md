# Onnkiosk (Android)

An ambient smart display and dashboard for Android tablets, featuring Google Calendar integration, Google Tasks checklist, Google Photos slideshow, weather, and animated live Doppler radar.

Optimized for **landscape 1280×800** displays (such as the Walmart Onn 8" Tablet / `ceres-b3`).

---

## Features

- **Google Calendar**: Day, 3-Day, Week, and Month views with all-day events pinned cleanly at the top.
- **Google Tasks**: Interactive task checklist with completion toggles, due dates, and quick addition.
- **Google Photos**: Auto-cycling photo slideshow with year filtering, album selection, full-screen mode, and favorites (`★`).
- **Weather & Doppler Radar**: Current temperature, forecasts, and live interactive RainViewer radar tile overlays with timeline scrubbing.
- **Self-Updating**: In-app update notifications via GitHub Releases with direct download and install.
- **Privacy & Security**: Connects directly to Google APIs using your own OAuth Client credentials or user login. No intermediary servers, no third-party tracking.

---

## Hardware Optimization

- **Target Device**: Walmart Onn 8" Tablet (`ceres-b3`) or any Android 13+ (API 33+) tablet running in landscape mode (`1280×800` or `1920×1200`).
- **Orientation**: Locked in landscape orientation.
- **Display**: High-contrast dark theme designed for ambient wall-mount or countertop kitchen/living room kiosks.

---

## Setup & Installation

### Option 1: One-Command Automated Tablet Installer (Recommended)
Connect your tablet via USB and run our setup helper:
```bash
./install-tablet.sh
```
This automatically:
- Detects the connected tablet via ADB.
- Fetches and installs the latest release APK from GitHub.
- Configures **Stay Awake while plugged in** (`stay_on_while_plugged_in = 7`).
- Ensures auto-rotate / landscape orientation.
- Grants **Install Unknown Apps** (`REQUEST_INSTALL_PACKAGES`) so in-app self-updates work seamlessly.
- Launches **Onnkiosk** immediately.

To see all available installer options and first-time tablet preparation steps:
```bash
./install-tablet.sh --help
```

---

### Option 2: Manual Direct Download on the Tablet (No Computer)
1. Open Chrome on the tablet and visit: [github.com/derallo/onnkiosk/releases/latest](https://github.com/derallo/onnkiosk/releases/latest).
2. Tap `app-release.apk` to download and install.
3. When prompted, allow Chrome to install unknown apps.
4. In tablet Settings → Apps → Special app access → Install unknown apps → Allow for **Onnkiosk** (enables seamless in-app self-updates).
5. In Developer Options, enable **Stay awake** so the display remains on while docked on power.

---

### Option 3: Manual USB / ADB Install
1. Enable **Developer options** on the tablet (Settings → About tablet → tap **Build number** 7 times).
2. Enable **USB debugging** and connect via USB.
3. Run:
```bash
adb install -r app-release.apk
```

### 4. Connect Your Google Account
1. Launch **Onnkiosk**.
2. Tap the **Settings** (gear) icon in the top right.
3. In the Google Account section, enter your Google OAuth Client ID and Secret (or sign in).
4. Select the Google Calendars and Photo Albums you want displayed on your dashboard.

---

## Building from Source

### Prerequisites
- JDK 17 or higher
- Android SDK (API 36/37)

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run unit tests
./gradlew testDebugUnitTest
```
The resulting APKs will be located in `app/build/outputs/apk/`.
