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

### A. One-Command Automated Tablet Installer (`install-tablet.sh`)
For fastest setup on any Android tablet, connect via USB-C and run:
```bash
./install-tablet.sh
```
The helper script automatically detects the device, downloads the latest release APK, configures kiosk hardware flags (`stay_on_while_plugged_in`), grants self-update permissions, installs the app, and launches Onnkiosk.

Run `./install-tablet.sh --help` to view all flags (`--latest`, `--local`, `--build`, `--setup-only`, `--device`).

---

### B. Manual Tablet Hardware Preparation (Developer Mode & Stay Awake)
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
- **Clock, Weather & Next Event Header**: High-contrast ambient clock with clean Day-of-Week, Month, Day, and Year display, real-time temperature, condition badge, 4-day forecast, and automatic NWS weather alert pills. The Next Event banner highlights your upcoming appointment with its time/date and an inline description snippet.
- **Doppler Radar Modal**: Tap the **Radar** button in the weather card to open the fullscreen interactive RainViewer/NEXRAD radar modal with zoom, pan, play/pause controls, and frame scrubber.
- **Home Hub (Calendar & Google Tasks)**:
  - Tab selector for **All**, **Events (`Itinerary / 3-Day / Week / Month`) ▾**, and **Tasks** views.
  - Multi-day all-day events consolidate cleanly into a single entry with a `Through YYYY-MM-DD` note, and all-day events stay pinned cleanly to the top of day views.
  - Tap any event to inspect location, organizer, attendees, and description.
  - Tap circular checkboxes to mark Google Tasks done (with strikethrough styling and automatic Google sync), filter individual task lists via the filter dropdown, and use the 5-second **Undo** bar after deleting an item.
  - Tap the **+** button in the top-right to open the **New Entry** dialog and add a **Task** or **Event** directly to your Google account.
  - **Calendar Primary Mode**: Tap **Hide** in the photo bar (or toggle **Make Calendar Primary** in Settings) to expand Calendar & Tasks full-screen; tap **Show Photos** in the top bar to bring the photo carousel back.

### Right Wing: Lifestyle, Photos & Media
- **Google Photos Ambient Slideshow**:
  - Automatically rotates photos at configurable intervals (`5s`, `10s`, `15s`, `20s`, `30s`, `60s`), with swipe left/right navigation and tap-to-inspect fullscreen for 10 seconds.
  - **Multi-Year & Album Filters**: Filter photos across specific years or all years in Settings, or filter by album.
  - **Starred Favorites (`★`)**: Tap the star icon on any photo to add it to your local favorites. Tap **Star on / Star off** in the bottom bar to cycle exclusively through starred photos.
  - **Fullscreen Slideshow Mode**: Tap the fullscreen expansion icon for an edge-to-edge frame view. Remains in fullscreen mode for 30 minutes or until touched/dismissed.
  - **Auto-Detect Album Links**: Add a Google Task containing a `photos.app.goo.gl` link and Onnkiosk imports it into the slideshow automatically and marks the task complete.

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
