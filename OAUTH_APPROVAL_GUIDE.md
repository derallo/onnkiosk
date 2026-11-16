# Google OAuth Verification & Approval Package: Onnkiosk

This document provides everything required to submit Onnkiosk for Google OAuth Verification so users do not see the **"Google hasn't verified this app"** warning screen during authentication.

---

## 1. Why Google Shows the "Unverified App" Warning

Google OAuth enforces security verification for any application requesting sensitive or restricted scopes.

Onnkiosk requests:
- `openid`, `email`, `profile` (Non-sensitive)
- `https://www.googleapis.com/auth/tasks` (Sensitive)
- `https://www.googleapis.com/auth/calendar` (Sensitive)
- `https://www.googleapis.com/auth/photospicker.mediaitems.readonly` (Sensitive / Restricted depending on Cloud Project tier)

When an app is in **"Testing"** mode or has not completed Google's **OAuth App Verification**, users attempting to authenticate are met with:
> *"Google hasn't verified this app. The app is requesting access to sensitive info in your Google Account."*

---

## 2. Step-by-Step Google Cloud Verification Checklist

To eliminate the warning, complete these steps in the [Google Cloud Console (APIs & Services > OAuth consent screen)](https://console.cloud.google.com/apis/credentials/consent):

### Step A: Configure Consent Screen Metadata
1. **App Name**: `Onnkiosk`
2. **User Support Email**: Your contact/admin email (e.g. `derek.pangallo@gmail.com`).
3. **App Logo**: Upload a 120×120px app icon (the high-contrast Onnkiosk clock/display icon).
4. **Application Home Page**: `https://github.com/derallo/onnkiosk`
5. **Application Privacy Policy Link**: A public URL detailing how data is processed (e.g. `https://github.com/derallo/onnkiosk/blob/main/PRIVACY.md` or a hosted page).
   > **Key Privacy Guarantee to State**: *"Onnkiosk connects directly to Google APIs over HTTPS to fetch calendars, tasks, and photos solely for local ambient display on your countertop tablet. No personal data, tokens, or credentials are sent to third parties or stored on intermediate servers."*
6. **Application Terms of Service Link**: (Optional/Recommended) `https://github.com/derallo/onnkiosk/blob/main/TERMS.md`.
7. **Authorized Domains**: Add your domain (e.g. `github.com` or custom personal domain).
8. **Developer Contact Information**: Your contact email.

### Step B: Declare Scopes & Justifications
In the Scopes step, declare:
1. `.../auth/calendar`:
   - **Justification**: *"Onnkiosk is an ambient kitchen and countertop display. It requests read/write calendar access to display today's upcoming family schedule, multi-day agenda, and pinned all-day events on the tablet dashboard, allowing family members to stay coordinated."*
2. `.../auth/tasks`:
   - **Justification**: *"To render an interactive daily household chore and shopping checklist on the tablet. Users can check off tasks or add household reminders directly on the countertop screen."*
3. `.../auth/photospicker.mediaitems.readonly`:
   - **Justification**: *"To display a rotating family digital photo frame slideshow in ambient standby mode, allowing users to browse their curated photo albums and favorite photos."*

### Step C: Publishing Status
Switch User Type from **External - Testing** to **In Production** and submit for verification.

---

## 3. Demo Video Requirements (Google Verification Standard)

Google requires a publicly accessible YouTube link (Unlisted) demonstrating:
1. The OAuth consent flow initiated from the app.
2. The browser address bar showing the **OAuth Client ID** matching the Cloud Console.
3. The user granting permissions.
4. How each requested scope (`calendar`, `tasks`, `photos`) is actively used inside the user interface.

---

## 4. YouTube Video Recording Script (For Video Agent / Creator)

**Target Duration**: 1:45 to 2:15 minutes  
**Format**: Screen capture of tablet (via `scrcpy` or clean screen recorder) showing browser address bar clearly, with clear spoken voiceover.

### Scene 1: Introduction & App Overview (0:00 – 0:20)
- **Visual**: Onnkiosk home screen running on tablet in dark landscape layout (`1280×800`).
- **Voiceover**:
  > *"Hello. This is Onnkiosk, an open-source ambient countertop dashboard and digital picture frame designed for family coordination on home tablets. In this video, we demonstrate our Google OAuth authentication flow and explain how our application uses each requested Google API scope directly on the device."*

### Scene 2: Initiating Sign-In & Client ID Verification (0:20 – 0:50)
- **Visual**:
  1. Tap the **Settings (gear)** icon in top-right bar.
  2. Tap **Google Account** → Show the Client Config screen.
  3. Tap **Start Sign-In**. The Google authentication screen opens in the browser.
  4. **CRITICAL REQUIREMENT**: Zoom in / highlight the browser address bar for 3 seconds showing:
     `accounts.google.com/o/oauth2/v2/auth?client_id=<YOUR_CLIENT_ID>&redirect_uri=...`
- **Voiceover**:
  > *"To connect calendar and tasks, the user opens Settings and initiates Google Sign-In. As seen in the URL bar, the request is directed to Google's standard OAuth 2.0 authorization endpoint using our verified Client ID with PKCE."*

### Scene 3: Consent Screen & Permission Approval (0:50 – 1:10)
- **Visual**: Show the Google permissions screen listing:
  - *See, edit, share, and permanently delete all the calendars you can access using Google Calendar*
  - *Create, edit, organize, and delete all your tasks*
  - *See photos and videos you select*
  Tap **Allow / Continue**. Browser redirects to loopback callback: *"Google Account Connected"*.
- **Voiceover**:
  > *"The user approves access to their Google Calendar, Google Tasks, and Google Photos Picker. All authorization codes and tokens are exchanged directly with Google's servers over HTTPS and stored securely in Android SharedPreferences on the local device. No third-party servers are used."*

### Scene 4: Demonstrating Calendar & Task Usage (1:10 – 1:40)
- **Visual**: Return to Onnkiosk main dashboard:
  1. Show the **Next Event** card at the top updating with the user's real event.
  2. Switch between **Day**, **3-Day**, **Week**, and **Month** tabs showing synchronized Google Calendar events.
  3. Tap an event to open event details (location, description).
  4. Point to the **Tasks Checklist**: tap a circle to mark a Google Task complete (show strikethrough).
  5. Tap **+** to add a task: *"Buy milk"*, showing it appear instantly in the list.
- **Voiceover**:
  > *"On the Left Wing of the dashboard, Google Calendar events are rendered in real-time across Agenda, Day, Week, and Month views, with all-day events pinned at the top so the family never misses an appointment. In the Omni Hub below, Google Tasks are displayed as a touch-friendly checklist where family members can toggle tasks complete or add reminders directly from the counter."*

### Scene 5: Demonstrating Photos Slideshow Usage (1:40 – 2:00)
- **Visual**: Point to Right Wing:
  1. Show the rotating photo slideshow.
  2. Tap the **Year dropdown** (filter by year).
  3. Tap the **Star (`★`)** button on a photo to favorite it.
  4. Tap the **Fullscreen** icon to expand edge-to-edge.
- **Voiceover**:
  > *"On the Right Wing, the Google Photos integration powers an ambient digital photo frame. Users can filter family memories by year, favorite pictures with one touch, and expand into an edge-to-edge ambient slideshow."*

### Scene 6: Sign-Out / Data Revocation & Conclusion (2:00 – 2:15)
- **Visual**: Open Settings → Google Account → Tap **Disconnect Account** (show data cleared and return to unauthenticated state).
- **Voiceover**:
  > *"Users have total control and can disconnect their account at any time, which purges all local tokens and caches immediately. Thank you for reviewing Onnkiosk."*
