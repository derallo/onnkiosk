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

### 1. Enable Developer Options & USB Debugging on the Tablet
1. Open Android **Settings** → **About tablet**.
2. Tap **Build number** 7 times until you see the message *"You are now a developer!"*.
3. Go back to **Settings** → **System** → **Developer options**.
4. Enable:
   - **USB debugging**: Toggle ON (connect via USB and tap "Always allow from this computer" when prompted).
   - **Stay awake**: Toggle ON (screen will stay awake while plugged into power).

### 2. Install the APK
Download the latest `app-release.apk` from the [Releases](https://github.com/derallo/onnkiosk/releases) section.

You can install it directly via `adb`:
```bash
adb install -r app-release.apk
```
Or download the APK directly on the tablet's browser and tap to install (grant "Install unknown apps" when prompted by your browser/file manager).

### 3. Grant Permissions
- **Install unknown apps**: In tablet Settings → Apps → Special app access → Install unknown apps → Allow for **Onnkiosk** (enables seamless in-app self-updates).
- **Notifications**: Allow notifications for calendar/event alerts if prompted.

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
