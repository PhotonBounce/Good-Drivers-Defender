# Google Play Readiness Checklist — Good Drivers Defender

Everything you need to take this app from code to a live Play Store listing. Items marked
**[YOU]** require your accounts/decisions; **[DONE]** were handled in this branch.

---

## 1. Pre-upload blockers

- [ ] **[YOU] Real AdMob IDs.** In [AdMob](https://apps.admob.com): create the app, then a Banner and an
  Interstitial ad unit. Replace the three sample IDs:
  - `AndroidManifest.xml` → `com.google.android.gms.ads.APPLICATION_ID` (currently `ca-app-pub-3940256099942544~3347511713`)
  - `AdManager.kt` → `BANNER_AD_UNIT_ID`, `INTERSTITIAL_AD_UNIT_ID`
  - Best practice: keep the **test** IDs for debug builds and inject **real** IDs only for release.
- [ ] **[YOU] Upload keystore + signing.** Create it once and keep it safe (losing it = you can never update the app):
  ```bash
  keytool -genkeypair -v -keystore my-upload-key.jks -alias upload \
    -keyalg RSA -keysize 2048 -validity 10000
  ```
  Then create `key.properties` (NOT committed — now git-ignored):
  ```properties
  storeFile=../my-upload-key.jks
  storePassword=<your-real-password>
  keyAlias=upload
  keyPassword=<your-real-password>
  ```
  Strongly recommended: enroll in **Play App Signing** (Google manages the app signing key; you keep the upload key).
- [ ] **[YOU] Host the privacy policy.** Publish `PRIVACY_POLICY.md` at a public URL (GitHub Pages, your
  site, Notion, etc.) and fill in your contact email + entity name. You'll paste the URL into Play Console.
- [ ] **[YOU] Build the release bundle:** `./gradlew bundleRelease` → upload
  `app/build/outputs/bundle/release/app-release.aab` (AAB, **not** APK).

---

## 2. Play Console — Data Safety form (answers for THIS app)

Based on the manifest + code, declare:

| Data type | Collected? | Shared? | Purpose | Notes |
|-----------|-----------|---------|---------|-------|
| **Precise location** (GPS) | Yes | No | App functionality (trip/incident logging) | Stored on-device |
| **Microphone / audio** | Yes (in recordings) | No | App functionality (dashcam audio) | On-device |
| **Photos/Videos** (camera) | Yes | No | App functionality (dashcam video evidence) | On-device |
| **Device or other IDs** (advertising ID) | Yes | Yes | Advertising (AdMob) | Via Google Mobile Ads SDK |
| **App activity / crash** | Only if you add Firebase/analytics | — | Analytics | Currently none in code |

- **Encryption in transit:** Yes (ads SDK uses HTTPS).
- **Users can request deletion:** describe how (data is local; uninstall removes it).
- ⚠️ Your **Data Safety answers must match runtime behavior** — that's why the deceptive "telemetry" copy
  (QA item C3) must be fixed before you submit.

---

## 3. Permissions declarations (Play Console)

- **Foreground service types** (`location`, `microphone`): Play requires a short justification + screen
  recording showing the in-use notification. Have a demo clip ready.
- **`ACCESS_BACKGROUND_LOCATION`**: ✅ removed in this branch — avoids the heavy background-location review.
  If you re-add it, you must complete the in-app declaration and submit a demo video.
- **Camera / Microphone**: prominent in-app disclosure before the first request (see C3 fix).

---

## 4. Content rating & category
- Run the **content rating questionnaire** (IARC). This is a utility/tools app with user-generated video;
  answer honestly about UGC and data collection.
- Suggested **category:** Auto & Vehicles (or Tools). **Tags:** dashcam, GPS, driving, safety.
- **Target audience:** adults/drivers — do **not** target children (camera/mic/location + ads).

## 5. Store listing assets you need to produce **[YOU]**
- App icon 512×512 (you have `appicon.png` — verify resolution).
- Feature graphic 1024×500.
- Phone screenshots (2–8). You already have `real_screenshots/` and `real_app_screenshot.png` — verify they
  show the current UI and contain no placeholder/test data.
- Short description (≤80 chars) + full description (≤4000). Draft below.

### Draft store listing copy (honest — aligned with Data Safety)
> **Short:** Dashcam that records video, audio & GPS to document your drives as evidence.
>
> **Full:** Good Drivers Defender turns your phone into a dashcam and trip recorder. It records video and
> audio from your camera and logs your GPS route and speed so you have a clear, timestamped record of your
> drive. Save incidents to your on-device evidence locker and export a summary report. Go Pro to remove ads
> and unlock the report builder.
>
> Good Drivers Defender records **only when you start it**, and shows an ongoing notification while active.
> Your recordings and location history stay **on your device** unless you choose to share them. Camera,
> microphone, and location permissions are required for the dashcam to function. Ads are provided by Google
> AdMob. See our privacy policy: <YOUR_PRIVACY_POLICY_URL>

---

## 6. Legal/positioning note (read this)
The current UI frames the app heavily around "civil suit / recovery claims / evidence packets". Recording
your **own** drive is legal and legitimate, but: (a) keep marketing claims accurate (don't promise legal
outcomes), (b) audio recording laws vary by state/country — add a line advising users to comply with local
consent laws, and (c) avoid implying the app provides legal advice. Honest, functional framing also reduces
Play review friction.

---

## 7. Final pre-submit checklist
- [ ] Real AdMob IDs in release build
- [ ] Signed AAB via upload keystore + Play App Signing
- [ ] Privacy policy URL live and linked in Play Console
- [ ] Data Safety form completed (matches behavior)
- [ ] FGS + permissions justifications submitted
- [ ] Deceptive permission copy fixed (QA C3)
- [ ] Content rating completed
- [ ] Screenshots/feature graphic uploaded
- [ ] Internal testing track tested on a real device before production rollout
