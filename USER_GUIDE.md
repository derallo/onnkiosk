# Onnkiosk (Android) — User Guide & Operational Reference

> [!IMPORTANT]
> **MANDATE FOR ALL AGENTS WORKING IN THIS REPOSITORY:**
> When adding, modifying, or removing features, UI widgets, network calls, OAuth logic, or settings in `android-oauth/` (Onnkiosk), you **MUST** update this document (`android-oauth/USER_GUIDE.md`) and keep `android-oauth/README.md` and `AGENTS.md` synchronized in the exact same commit/turn.

---

## 1. Overview & Architecture

**Onnkiosk** is a standalone, native Android (Jetpack Compose) ambient smart dashboard and digital picture frame optimized for kitchen countertops and wall-mounted tablets (specifically target-tuned for 1280×800 landscape screens such as the Walmart Onn 8" Tablet / `ceres-b3`).

### Key Principles
1. **Direct API Integration**: Communicates directly with Google Cloud APIs (Calendar v3, Tasks v1, Photos Picker) and RainViewer / Open-Meteo. No intermediate proxy servers.
2. **Offline-First Resilience**: All active calendars, tasks, and recent photo metadata are cached locally to disk (`frame_offline_cache.json`). The dashboard paints instantly on boot even if Wi-Fi is down.
3. **Ambient Touch Ergonomics**: Oversized hit targets (min 48dp, 56–64dp primary), high-contrast warm palette (`#101113` background, `#f5f2eb` text, `#d4af37` gold accents), zero outer-screen scrolling, and high-visibility status chips readable from 6–10 feet.

---

## 2. Setup & Installation

### A. Tablet Hardware Preparation (Developer Mode & Stay Awake)
1. Open Android **Settings** → **About tablet**.
2. Scroll to **Build number** and tap it **7 times** until you see *"You are now a developer!"*.
3. Go back to **Settings** → **System** → **Developer options**.
4. Enable:
   - **USB debugging**: `ON` (tap "Always allow from this computer" when plugged in).
   - **Stay awake**: `ON` (ensures the screen never turns off while plugged into charging power).

### B. Installing the Application
- **Via ADB**:
  ```bash
  adb install -r app-release.apk
  ```
- **Via APK Direct Download**: Download the latest release `.apk` in Chrome on the tablet. Open Android Settings → Apps → Special app access → **Install unknown apps** → Grant permission for **Onnkiosk** (also required for in-app self-updates).

### C. Boot Receiver & Kiosk Auto-Start
Onnkiosk includes a `BootReceiver` that automatically launches the dashboard when the tablet boots or recovers from power loss.

---

## 3. Google OAuth & Account Connection

Onnkiosk supports connecting your Google account directly using your own OAuth Client ID.

### Connection Methods:
1. **In-App Embedded Browser / Direct Web Flow**:
   - Tap **Settings** (gear icon) in the top-right bar.
   - Tap **Google Account** → enter your **Client ID** and **Client Secret** (or leave Secret empty if using a Public Client).
   - Tap **Start Sign-In**. An embedded custom tab/browser opens Google's standard consent screen.
2. **Local Area Network (LAN) Companion Pairing**:
   - When you tap **Start Sign-In**, Onnkiosk starts a temporary local HTTP loopback server (e.g. port `3000`, `8080`, or `8089`).
   - A companion URL is displayed: `http://<tablet-ip>:3000/start`.
   - You can open that URL from any phone or computer on the same Wi-Fi network, sign in with Google, and the authorization code redirects straight back to the tablet automatically.
3. **Manual Code / URL Paste Fallback**:
   - If local redirects are intercepted or blocked by browser sandboxes, paste either the full redirect URL (`http://localhost:3000/auth/callback?code=...`) or the raw `code=...` parameter into the input box and tap **Connect Onnkiosk**.

---

## 4. Dashboard Features & Daily Operation

### Left Wing: Informational Telemetry
- **Clock & Date**: High-contrast ambient clock with clean Day-of-Week, Month, Day, and Year display.
- **Weather & Rain Alert**: Real-time temperature, condition badge, high/low, and automatic rain detection pill.
- **Doppler Radar Modal**: Tap the **Radar** button in the weather card to open the fullscreen interactive RainViewer radar modal with zoom, pan, play/pause controls, and frame scrubber.
- **Next Event Card**: Prominently highlights the very next upcoming appointment, its start time, remaining countdown, and location.
- **Agenda & Calendar View**:
  - Tab selector for **Agenda**, **Day**, **3-Day**, **Week**, and **Month** views.
  - All-day events stay pinned cleanly to the top.
  - Tap any event to inspect location, organizer, attendees, and description.
- **Google Tasks Checklist**:
  - Tap the circular checkbox to toggle tasks completed (applies strikethrough styling and syncs with Google Tasks).
  - Tap the **+** button to type and add new tasks directly from the counter.
  - Delete completed tasks or clear lists.

### Right Wing: Lifestyle, Photos & Media
- **Google Photos Ambient Slideshow**:
  - Automatically rotates photos at configurable intervals (10s, 30s, 1m, 5m).
  - Tap photo directly to skip immediately to the next image.
  - **Year Filter**: Dropdown menu to filter photos from specific years (2017–2026) or show All Years.
  - **Starred Favorites (`★`)**: Tap the star icon on any photo to add it to your local favorites. Tap **Favorites** in the bottom bar to cycle exclusively through starred photos.
  - **Fullscreen Slideshow Mode**: Tap the fullscreen expansion icon for an edge-to-edge frame view. Remains in fullscreen mode until touched or dismissed.
  - **Clean Date Captions**: Cleans up missing or bogus epoch date stamps (e.g. Dec 31, 1969) gracefully.

---

## 5. In-App Updates & Maintenance

- **Automatic GitHub Release Checks**: Onnkiosk checks the GitHub repository (`derallo/onnkiosk`) for new releases periodically in the background.
- **Update Modal**: When a newer release is published, an update chip appears. Tap **Update** to review changelog release notes, download the APK, and trigger the Android package installer directly.
- **Log Inspection**: Tap **Settings** → **Diagnostics** to view token refresh status, memory usage, and background sync health.

---

## 6. Instructions for Future Agents
1. **Keep Documentation In Sync**: Whenever modifying Compose UI layouts, ViewModel states, API calls in `NernalRepository.kt`, or OAuth handling in `GoogleAuthManager.kt`, you must update this file, `README.md`, and the root `AGENTS.md`.
2. **Never Break Offline Fallback**: Ensure any change to models or preferences respects `frame_offline_cache.json` and graceful network error handling.
3. **Verify Tablet Constraints**: Avoid adding dependencies that introduce large native blobs or increase APK size unnecessarily. Keep target rendering smooth at 60fps on low-tier quad-core ARM chips.
