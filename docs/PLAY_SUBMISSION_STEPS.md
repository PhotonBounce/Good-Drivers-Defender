# Google Play Submission — Step-by-Step (the human tasks)

Everything in the **code/CI is done** (subscription-only, no-ads, 7-day trial, R8 validated,
manifest/SDK/version audit passed, CI builds & validates the release bundle every run).
What remains needs your Google/GitHub accounts. Follow this in order.

Repo: **PhotonBounce/Good-Drivers-Defender** · App ID: `com.aistudio.driverrecorder.gpxrt`

---

## 0. Prerequisites (once)

- **Google Play Developer account** — $25 one-time:
  https://play.google.com/console/signup
- **JDK** (for `keytool`, used to make the signing key):
  https://adoptium.net/temurin/releases/
- ⚠️ **New-account testing rule** — if your developer account is a *personal* account created
  after ~Nov 2023, Google requires a **closed test with ≥12 testers opted-in for 14 continuous
  days** before you can publish to *production*. Internal testing (Step 4) does **not** have this
  rule, so start there. Details:
  https://support.google.com/googleplay/android-developer/answer/14151465

---

## 1. Signing secrets → uploadable **signed** AAB

This is the one thing blocking an uploadable build. CI is already wired; it just needs four
**Repository** secrets with the exact names below.

### 1a. Create the upload keystore (skip if you already have one)

In a terminal (any folder), with the JDK installed:

```bash
keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

It will ask for:
- a **keystore password** → remember it (this is `STORE_PASSWORD`)
- name/organization → anything reasonable
- a **key password** → press Enter to reuse the keystore password (this is `KEY_PASSWORD`)

The alias `upload` is your `KEY_ALIAS`.

> 🔐 **Back up `upload-keystore.jks` and both passwords somewhere safe.** If you lose them you can
> never update the app again. (When you first publish, enroll in **Play App Signing** — Google holds
> the real app-signing key; this keystore is just your *upload* key:
> https://support.google.com/googleplay/android-developer/answer/9842756 )

### 1b. Base64-encode the keystore (so it can live in a secret)

- **Linux:**   `base64 -w0 upload-keystore.jks > keystore_base64.txt`
- **macOS:**   `base64 < upload-keystore.jks | tr -d '\n' > keystore_base64.txt`
- **Windows (PowerShell):**
  `[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Out-File -NoNewline keystore_base64.txt`

Open `keystore_base64.txt` — it's one long line with **no line breaks**. That whole string is
`KEYSTORE_BASE64`.

### 1c. Add the four **Repository** secrets

Go to (must be signed in as the repo owner):
**https://github.com/PhotonBounce/Good-Drivers-Defender/settings/secrets/actions**

Click **“New repository secret”** and add each (names are **case-sensitive**; use the
**Repository secrets** section, **NOT** *Environment* secrets):

| Secret name        | Value                                                        |
|--------------------|-------------------------------------------------------------|
| `KEYSTORE_BASE64`  | the entire one-line string from `keystore_base64.txt`        |
| `STORE_PASSWORD`   | your keystore password                                      |
| `KEY_PASSWORD`     | your key password (same as keystore password if you reused) |
| `KEY_ALIAS`        | `upload`                                                     |

> Why it failed before: the secrets were resolving **empty** in CI (a blank value, not `***`).
> That means a name/case mismatch or they were added as *Environment* secrets. Re-add them exactly
> as above. GitHub secrets reference: https://docs.github.com/actions/security-guides/using-secrets-in-github-actions

### 1d. Trigger CI and grab the signed AAB

Any new push triggers a build, or just re-run the latest one. Watch it here:
**https://github.com/PhotonBounce/Good-Drivers-Defender/actions**

When green, open the run → scroll to **Artifacts** → download **`release-aab`** → inside is
`app-release.aab` (now **signed**). That's your Play upload.
*(Tell me once the secrets are in and I'll re-run CI and confirm a signed AAB drops out.)*

---

## 2. Create the two subscriptions in Play Console

Path: **Play Console → (your app) → Monetize → Products → Subscriptions → Create subscription**
Help: https://support.google.com/googleplay/android-developer/answer/140504

Create **two** subscriptions. The **Product IDs must match the app exactly** and **can’t be changed
after creation**:

1. **`defender_pro_monthly`**
   - Add a **base plan** (e.g. ID `monthly`), **auto-renewing**, billing period **Monthly (P1M)**, price e.g. **$4.99** → **Activate**.
2. **`defender_pro_annual`**
   - Add a **base plan** (e.g. ID `annual`), **auto-renewing**, billing period **Yearly (P1Y)**, price e.g. **$34.99** → **Activate**.

Notes:
- The app reads the **live** Play prices, so whatever you set is what users see (the `$4.99/$34.99`
  in code are just fallbacks).
- The **7-day VIP trial is implemented in-app** (`TrialManager`), so you do **not** need to add a
  Play free-trial offer. (You *may* add a Play intro offer too, but it would stack with the in-app trial.)
- Subscriptions become purchasable in testing once you’ve uploaded a build with the Billing library
  to a track (Step 4) and added license testers (Step 4, first bullet).
- Overview / concepts: https://support.google.com/googleplay/android-developer/answer/9900533

---

## 3. Store listing, privacy policy, Data Safety, content rating

### 3a. Host the privacy policy (need a public URL)
The policy file is in the repo at **`web/privacy.html`**. Two easy options:
- **Your host:** upload `web/privacy.html` to your site so it’s at
  `https://photon-bounce.com/privacy.html` (the store copy already references this URL).
- **GitHub Pages (free):** ask me and I’ll add a Pages workflow that publishes it to a `github.io` URL.

### 3b. Create the app
**Play Console → All apps → Create app** → name **“Good Drivers Defender”**, default language,
**App**, **Free**, accept declarations.

### 3c. Main store listing (text + graphics)
Path: **Play Console → Grow → Store presence → Main store listing**
Asset specs: https://support.google.com/googleplay/android-developer/answer/9866151
- **Short (80 chars) + Full description:** copy from **`docs/STORE_LISTING.md`** in the repo.
- **App icon (512×512):** `play_assets/icon_512.png`
- **Feature graphic (1024×500):** `play_assets/feature_graphic.png`
- **Phone screenshots (min 2):** `play_assets/screenshot_1080_*.png` (5 provided)

### 3d. App content declarations
Path: **Play Console → Policy and programs → App content**
- **Privacy policy:** paste the URL from 3a.
- **Data safety:** fill from **`docs/STORE_LISTING.md`** → Location (approx+precise), Photos/Videos,
  Audio = *collected, not shared, app functionality, stored on device*; **no advertising ID**;
  encryption in transit = yes; deletion = uninstall removes. Help:
  https://support.google.com/googleplay/android-developer/answer/10787469
- **Ads:** select **No** (app contains no ads).
- **Content rating (IARC):** start the questionnaire (category: Utility/Productivity), answer
  honestly, submit. Help: https://support.google.com/googleplay/android-developer/answer/9859655
- Also complete **Target audience**, **News app = No**, **Government app = No**, and any
  **permissions** declaration prompts (foreground-service location/mic, all-files = N/A).

---

## 4. Smoke-test the **signed release (R8)** build on a device

Do this on the **Internal testing** track (fast, no 12-tester rule).

1. **Make test purchases free** — **Play Console → Setup → License testing** → add your Google
   account email(s). https://support.google.com/googleplay/android-developer/answer/6062777
2. **Create the release** — **Play Console → Test and release → Testing → Internal testing →
   Create new release** → upload the **signed `app-release.aab`** (from Step 1d) → add release notes →
   **Review release → Roll out**. https://support.google.com/googleplay/android-developer/answer/9845334
3. **Add testers** — Internal testing → **Testers** tab → create an email list → add your phone’s
   Google account → **Save**. Copy the **opt-in URL**, open it on your phone, accept, then install
   from Google Play.
4. **Verify on the real device** (this is the R8 smoke-test):
   - App launches with **no crash** (R8/obfuscation sanity).
   - Recording, Drive Score, SOS, Sentry, evidence locker all work.
   - Paywall shows your **real** monthly/annual prices.
   - Purchase a subscription (free as a license tester) → VIP unlocks.
   - The **7-day trial** state behaves on a fresh install.

When internal testing looks good, promote to **Closed** (the 12-tester/14-day requirement, if it
applies to your account) and then **Production**.

---

## Quick reference

**Secret names (exact):** `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS`
**Product IDs (exact):** `defender_pro_monthly`, `defender_pro_annual`
**GitHub secrets:** https://github.com/PhotonBounce/Good-Drivers-Defender/settings/secrets/actions
**CI / artifacts:** https://github.com/PhotonBounce/Good-Drivers-Defender/actions
**Play Console:** https://play.google.com/console
**Repo copy for listings/data-safety:** `docs/STORE_LISTING.md` · assets in `play_assets/`
