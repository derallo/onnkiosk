# Onnkiosk (Android) — User Guide & Operational Reference

> [!IMPORTANT]
> **MANDATE FOR ALL AGENTS WORKING IN THIS REPOSITORY:**
> When adding, modifying, or removing features, UI widgets, network calls, OAuth logic, or settings in `android-oauth/` (Onnkiosk), you **MUST** update this document (`android-oauth/USER_GUIDE.md`) and keep `android-oauth/README.md` and `AGENTS.md` synchronized in the exact same commit/turn.

---

## 1. Overview & Architecture

**Onnkiosk** is a standalone, native Android (Jetpack Compose) ambient smart dashboard and digital picture frame optimized for kitchen countertops and wall-mounted tablets (specifically target-tuned for 1280×800 landscape screens such as the Walmart Onn 8" Tablet / `ceres-b3`).

### Key Principles
1. **Direct API Integration**: Communicates directly with Google Cloud APIs (Calendar v3, Tasks v1, Photos Picker), optional Gemini API (`gemini-2.5-flash`), and RainViewer / Open-Meteo. No intermediate proxy servers.
2. **Hardware-Adaptive Capabilities**: Automatically detects camera, microphone, speakers, and on-device speech recognition at launch (`DeviceCapabilitiesDetector`) — enabling Voice Assistant, Audible Event Alerts, and Camera Motion Wake/Auto-Sleep on tablets with those sensors (such as Fire Tablets on LineageOS) while omitting them on sensor-less frames (such as `ceres-b3`).
3. **Offline-First Resilience**: All active calendars, tasks, local Memory Book entries, and recent photo metadata are cached locally to disk (`oauth_album_library_cache.json`, `memory_book_local.json`). The dashboard paints instantly on boot even if Wi-Fi is down.
4. **Ambient Touch Ergonomics**: Oversized hit targets (min 48dp, 56–64dp primary), high-contrast warm palette (`#000000` background, `#faf8f5` text, `#f5f2eb` gold/cream accents), zero outer-screen scrolling, and high-visibility status chips readable from 6–10 feet.

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
- **Clock, Weather & Next Event Header**: Top-aligned high-contrast ambient clock paired with an equal-size Day & Date (`EEE, MMM d`) and flush-left location underneath (no divider bar), real-time temperature, today's high/low/precip, and 4 larger upcoming forecast cards (day abbreviation + weather icon on the same line, no duplicate "Today" card). Tap the chevron icon to collapse weather into a compact `[icon temp° ▾]` badge or expand it again.
- **Dismissible NWS Weather Alerts**: Active alerts appear as a compact warning icon by the date; tap it to inspect full alert details and dismiss individual alerts.
- **Doppler Radar Modal**: Tap the Radar icon right beside the current temperature to open the fullscreen interactive RainViewer/NEXRAD radar modal with zoom, pan, play/pause controls, and frame scrubber.
- **Home Hub (Calendar & Google Tasks)**:
  - Tab selector for **All**, **Events (`Itinerary / 3-Day / Week / Month`) ▾**, and **Tasks** views.
  - Swipe left or right on **3-Day**, **Week**, or **Month** calendars to step backward/forward in time.
  - Multi-day all-day events consolidate cleanly into a single entry with a `Through YYYY-MM-DD` note, and all-day events stay pinned cleanly to the top of day views.
  - **Month View Day Pop-out**: Tap any day in Month view to pop out a modal list of that day's events and tasks; tap **+ Add Event** to schedule an event directly on that date, or tap any event in the pop-out to open full Event Details.
  - Tap any event to inspect location, organizer, attendees, and description.
  - Tap circular checkboxes to mark Google Tasks done (with strikethrough styling and automatic Google sync), filter individual task lists via the filter dropdown, and use the 5-second **Undo** bar after deleting an item.
  - Tap the **+** button in the top-right to open the **New Entry** dialog and add a **Task** or **Event** directly to your Google account.
  - **Calendar Primary Mode**: Tap the collapse icon (`>|`) at the far right of the bottom photo bar (or toggle **Make Calendar Primary** in Settings) to expand Calendar & Tasks full-screen while preserving full date/time, weather, and forecast cards (with the same weather collapse toggle); tap **Photos** (`[icon] Photos`) in the top bar to bring the photo carousel back.

### Right Wing: Lifestyle, Photos & Adaptive Smart Controls
- **Streamlined Top Utility Bar**: Wider icon-only outlined **Speak** (`🎤`) button on the left of the right column (alongside **Memories** when active, plus **Account** and **Wi-Fi** during initial setup only); on the right edge from right to left: **Settings**, **Night**, and **Eye** (Camera Motion Wake, when a camera is detected). User Guide (`?`), Account, Wi-Fi, and manual Photo Refresh live cleanly inside **Settings** after initial setup.
- **Voice Assistant (`🎤` button — visible when microphone & speakers are detected)**:
  - **On-Device Speech (Zero API Key Needed)**: Tap `🎤` to ask for the time, date, weather/forecast, morning briefing, schedule, or active tasks; say *"Remind me to..."* or *"Add event..."* to add items; or say *"Open radar"*, *"Next photo"*, *"Star this photo"*, *"Fullscreen"*, *"Show calendar"*, or *"Dim screen"*.
  - **"Super Onnkiosk" Gemini AI (Optional Gemini API Key in Settings)**: Add a Gemini API key in Settings — by typing/pasting, tapping **Send from Phone/PC** (scan the QR code or open the local Wi-Fi URL), or tapping **Pick from Google Tasks** to explicitly select a key you added to Tasks (which removes the task after import) — to unlock full conversational Q&A, multi-turn follow-up listening, live front-camera vision (*"What am I holding?"*, *"Read this recipe"*), and voice-managed **Memory Book** saving/searching.
  - **Top Subtitles & Quick Answer Card**: Spoken interactions display live transcripts in a top overlay and detailed responses in a dismissible Quick Answer Card with a **Read Aloud / Stop** button.
- **Morning Alarm, Night Mode, Camera Motion Wake & Idle Auto-Sleep**:
  - **Morning Alarm & Daily Brief (in Settings)**: Set a wake time, days of week, and Daily Brief timing (**When Silenced**, **At Alarm Time**, or **Chime Only**). When the alarm fires, it automatically ends Night Mode, wakes the screen, chimes, and offers **Snooze 5m** or **Silence & Start Day** (speaking your weather & schedule brief).
  - Tap the **Moon** icon for manual **Night Mode** — stays in Night Mode (ignoring camera motion and event wake) until you tap the screen to turn it off or your Morning Alarm fires (Night Mode displays an `⏰` badge when an alarm is armed).
  - Tap the **Eye** icon in the top bar (or toggle in Settings) to enable/disable hands-free wake when movement is detected in front of the tablet after **Auto-Sleep After Inactivity** (`Off`, `5m`, `10m`, `15m`, `30m`).
- **Audible Event Alerts & Read Aloud (enabled when speakers are detected)**:
  - Upcoming events (1–5 minutes before start) play a gentle 4-note chime and speak the event aloud (toggleable in Settings, with a **Test Chime & Voice** button).
  - Event details (`EventDetailsDialog`) and Memory Book entries also provide a **Read Aloud** button.
- **Memory Book (`Memories` book button)**: Browse saved local recipes, family notes, and synced Nernal Pi notebooks, with category filtering and Read Aloud.
- **Optional Nernal Raspberry Pi Sync**: Enable in **Settings → Nernal Raspberry Pi Sync** to sync reminders, Memory Book entries, and `★` photo favorites with a Nernal Raspberry Pi hub on your local Wi-Fi.
- **Google Photos Ambient Slideshow**:
  - Automatically rotates photos at configurable intervals (`5s`, `10s`, `15s`, `20s`, `30s`, `60s`), with swipe left/right navigation and tap-to-inspect fullscreen for 10 seconds. Starred photos show a subtle top-right `★` badge.
  - **Multi-Year & Album Filters**: Filter photos across specific years or all years in Settings, or filter by album.
  - **Bottom Control Bar (left to right)**: **Prev (`<`)**, **Pause/Play (`⏸/▶`)**, **Next (`>`)**, center **Album · Year**, **Star filter (`★ Only / ☆ Off`)**, **Favorite (`★`)**, **Fullscreen (`⛶`)**, and **Hide (`>|`)**.
  - **Fullscreen Slideshow Mode**: Tap the fullscreen expansion icon in the bottom photo bar for an edge-to-edge frame view. Remains in fullscreen mode for 30 minutes or until touched/dismissed.
  - **Auto-Detect Album Links**: Add a Google Task containing a `photos.app.goo.gl` link and Onnkiosk imports it into the slideshow automatically and marks the task complete.

---

## 5. Private Feedback, In-App Updates & Maintenance

- **Send Private Feedback (`?` → Send Feedback)**:
  - Tap the **?** button in the top bar (or open the User Guide) and tap **Send Feedback**.
  - Enter a bug report or feature request, choose whether to attach scrubbed diagnostic logs (enabled by default), and tap **Send Feedback**.
  - Submissions include your signed-in Google email (or `"Not signed in"`) and go through a write-only Cloud Run relay (`onnkiosk-feedback-relay`) into the **private** `derallo/onnkiosk-feedback` GitHub repository (`feedback/YYYY-MM-DD/*.md` and Issues). No GitHub token is embedded in the APK, and other users can never read your email or logs.
  - **Checking Submissions (Maintainers)**: Run `gh issue list --repo derallo/onnkiosk-feedback` or `gh api /repos/derallo/onnkiosk-feedback/contents/feedback`.
- **Automatic GitHub Release Checks**: Onnkiosk checks the GitHub repository (`derallo/onnkiosk`) for new releases periodically in the background.
- **Update Modal**: When a newer release is published, an update chip appears. Tap **Update** to review changelog release notes, download the APK, and trigger the Android package installer directly.
- **Log Inspection**: Tap **Settings** → **Diagnostics** to view token refresh status, memory usage, and background sync health.

---

## 6. Instructions for Future Agents
1. **Keep Documentation In Sync**: Whenever modifying Compose UI layouts, ViewModel states, API calls in `NernalRepository.kt`, or OAuth handling in `GoogleAuthManager.kt`, you must update this file, `README.md`, and the root `AGENTS.md`.
2. **Never Break Offline Fallback**: Ensure any change to models or preferences respects `frame_offline_cache.json` and graceful network error handling.
3. **Verify Tablet Constraints**: Avoid adding dependencies that introduce large native blobs or increase APK size unnecessarily. Keep target rendering smooth at 60fps on low-tier quad-core ARM chips.
