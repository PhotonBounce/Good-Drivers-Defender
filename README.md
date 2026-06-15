# Good Drivers Defender

An Android **dashcam & trip recorder**: records camera video + audio, logs GPS route/speed, stores
incidents in an on-device evidence locker, and exports summary reports. Built with Jetpack Compose,
Material 3, Room, and CameraX. Monetized with AdMob + a Play Billing "Pro" subscription (removes ads,
unlocks the report builder).

- **applicationId:** `com.aistudio.driverrecorder.gpxrt`
- **Version:** 1.2 (versionCode 3) · **minSdk:** 24 · **targetSdk/compileSdk:** 35

## Build

Requires a **system JDK 17** (set `JAVA_HOME`) and the Android SDK (platform 35). Do **not** hardcode a
JDK path in `gradle.properties`.

```bash
./gradlew clean assembleDebug          # debug APK
./gradlew lint testDebugUnitTest       # static analysis + unit/screenshot tests
./gradlew bundleRelease                # signed release AAB (needs key.properties — see below)
```

## Signing (release)

Create an upload keystore (kept **out** of git) and a `key.properties` file — see
[`docs/GOOGLE_PLAY_READINESS.md`](docs/GOOGLE_PLAY_READINESS.md). `key.properties`, `*.jks`, and
`.env` are git-ignored on purpose.

## Project layout

```
app/                       Android module
  src/main/java/com/example/
    MainActivity.kt        Compose host + permission/onboarding flow
    DefenderService.kt     Foreground service (location|microphone)
    ui/                    Compose screens (Dashboard, Locker, Paywall, Report, Video gallery)
    data/                  Room DB, EvidenceRepository, BillingManager
    ads/                   AdManager (AdMob)
    viewmodel/             RecorderViewModel
docs/                      QA report, Play readiness, on-device testing
PRIVACY_POLICY.md          Privacy policy template (host it, link in Play Console)
```

## Status & next steps

See **[`docs/QA_REPORT.md`](docs/QA_REPORT.md)** for the full QA pass and
**[`docs/GOOGLE_PLAY_READINESS.md`](docs/GOOGLE_PLAY_READINESS.md)** for the Play submission checklist.
Before release you must: set **real AdMob IDs**, configure **release signing**, **host the privacy policy**,
fix the permission-disclosure copy, and upload a **signed AAB**.
