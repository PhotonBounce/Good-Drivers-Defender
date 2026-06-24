# Good Drivers Defender

An Android **dashcam & trip recorder**: records camera video + audio, logs GPS route/speed, stores
incidents in an on-device evidence locker, and exports summary reports. Built with Jetpack Compose,
Material 3, Room, and CameraX. Monetized with AdMob + a Play Billing "Pro" subscription (removes ads,
unlocks the report builder).

- **applicationId:** `com.aistudio.driverrecorder.gpxrt`
- **Version:** 2.0 (versionCode 4) · **minSdk:** 24 · **targetSdk/compileSdk:** 35

## Driver Intelligence

On top of raw recording, the app derives **adaptive, on-device driving insights** — deterministic
math in `data/`, no network or ML service:

- **Adaptive Q-score** — an EMA-smoothed safety score (α=0.3) weighting recent trips more, with an
  IMPROVING / STABLE / DECLINING trend (`AdaptiveScoreEngine`).
- **Real-time Risk Intelligence** — a 0–1 risk level blended from speed, g-force, and hard-brake
  rate, EMA-smoothed so the dashboard halo glides instead of flickering on sensor spikes.
- **Coaching tips** — the single most impactful suggestion for the next drive (`Coaching.kt`).
- **Gamified achievements** — driver XP / level and unlockable badges (`Gamification.kt`).

The scoring / gamification / coaching logic is pure and unit-tested (`app/src/test/`).

**Privacy:** sensitive evidence (the incident database + `evidence_media/`) is **excluded from cloud
backup** so it stays on-device — see `res/xml/data_extraction_rules.xml`.

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
    ui/                    Compose screens (Dashboard, Drive Score, Locker, Paywall, Report, …)
    data/                  Room DB, repo, billing + scoring engine (AdaptiveScoreEngine,
                           Gamification, Coaching)
    ads/                   AdManager (AdMob)
    viewmodel/             RecorderViewModel
  src/test/                Unit tests (scoring, gamification, coaching) + Roborazzi screenshots
docs/                      QA report, Play readiness, on-device testing
tools/                     render_qa_screenshots.py → screenshots/<timestamp>/ (+ index.html gallery)
PRIVACY_POLICY.md          Privacy policy template (host it, link in Play Console)
```

## Status & next steps

See **[`docs/QA_REPORT.md`](docs/QA_REPORT.md)** for the full QA pass and
**[`docs/GOOGLE_PLAY_READINESS.md`](docs/GOOGLE_PLAY_READINESS.md)** for the Play submission checklist.
Before release you must: set **real AdMob IDs**, configure **release signing**, **host the privacy policy**,
fix the permission-disclosure copy, and upload a **signed AAB**.
