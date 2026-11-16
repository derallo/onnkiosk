# Privacy Policy — Onnkiosk

**Effective Date:** November 16, 2026  
**Application:** Onnkiosk (`com.nernal.frame.oauth`)  
**Repository:** [https://github.com/derallo/onnkiosk](https://github.com/derallo/onnkiosk)  
**Homepage:** [https://derallo.github.io/onnkiosk/](https://derallo.github.io/onnkiosk/)  
**Contact:** `derek.pangallo@gmail.com`

---

## 1. Google API Services User Data Policy & Limited Use Disclosure

> **Onnkiosk's use and transfer to any other app of information received from Google APIs will adhere to the [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy), including the Limited Use requirements.**

---

## 2. Architecture & Zero-Server Privacy Model

Onnkiosk is an open-source, standalone Android tablet dashboard. All Google API requests are made **directly from your local tablet to Google APIs over HTTPS**.

- **No Backend Servers:** Onnkiosk operates zero developer-hosted servers, databases, analytics trackers, or telemetry relays.
- **Local Storage Only:** OAuth 2.0 tokens (`access_token`, `refresh_token`) and offline display caches (`frame_offline_cache.json`) are stored exclusively on your device inside private Android `SharedPreferences` and app-private storage.
- **No Selling or Sharing:** Your Google data is never sold, rented, transferred to third parties, or used for advertising, profiling, or AI/ML model training.

---

## 3. Data Accessed & Purpose by Scope

When you sign in via Google OAuth 2.0 with PKCE (`S256`), Onnkiosk requests only the permissions needed to render your household dashboard:

1. **Profile & Identity (`openid`, `email`, `profile`)**
   - Used solely to show your signed-in account name and email inside the **Google Account & Sources** dialog.
2. **Google Calendar (`https://www.googleapis.com/auth/calendar`)**
   - Used to display your upcoming appointments in the **Next Event** strip and across **Itinerary**, **3-Day**, **Week**, and **Month** calendar views, as well as viewing event details and marking items when requested.
3. **Google Tasks (`https://www.googleapis.com/auth/tasks`)**
   - Used to display your selected Google Task lists in the **Tasks** and **Shopping** checklists, allowing you to toggle completion status and add new items directly from the tablet.
4. **Google Photos Picker (`https://www.googleapis.com/auth/photospicker.mediaitems.readonly`)**
   - Used solely when you explicitly launch the Google Photos Picker to select specific photos for the optional digital picture frame slideshow. When **Make Calendar Primary (Hide Photos)** is enabled, no photos are displayed.

---

## 4. Data Retention & Instant Deletion

- **In-App Sign Out:** Tapping **Sign Out** inside **Settings → Google Account** immediately revokes local session state and deletes all OAuth tokens, calendar caches, task caches, and photo album caches from the tablet.
- **Google Account Revocation:** You may also revoke Onnkiosk's access at any time from your [Google Account Permissions](https://myaccount.google.com/permissions) page.
