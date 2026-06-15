# Good Drivers Defender — QA Report

_Generated during cloud QA pass. Date: 2026-06-15. App version: `1.2` (versionCode 3),
`applicationId = com.aistudio.driverrecorder.gpxrt`, targetSdk 35, minSdk 24._

This report covers a **static** QA pass (the cloud container has no Android SDK, so a full
Gradle build / on-device run was not possible here — see `docs/ON_DEVICE_TESTING.md` for the
device test you run on your phone). Findings are grouped by severity.

---

## ✅ What's already good
- **Billing** (`BillingManager.kt`) is well implemented: connects, queries products, acknowledges
  purchases, retries on disconnect, and re-validates on resume. Pro entitlement drives ad-gating.
- **Ad gating** (`AdManager.kt`) correctly skips interstitials for premium users and reloads after dismiss.
- **Foreground service** declares correct Android-14 FGS types (`location|microphone`) and passes them to
  `startForeground(...)`.
- **targetSdk 35** meets the Google Play requirement (effective Aug 2025).
- Modern stack: Jetpack Compose, Material 3, Room, CameraX, coroutines.

---

## 🔴 CRITICAL — blocks Google Play release (you must act)

| # | Issue | Why it blocks | Owner |
|---|-------|---------------|-------|
| C1 | **AdMob uses Google's SAMPLE/TEST IDs** (`ca-app-pub-3940256099942544/...` in `AdManager.kt` + manifest `~3347511713`). | Shipping test ad units to production violates AdMob policy and earns **$0**. App can be suspended. | You (create real AdMob app + ad units) |
| C2 | **No release signing.** `keystore.rsp` has placeholder `changeMe123`; no `.jks` exists; `key.properties` points to the debug keystore. | Play requires a **signed release AAB**. You cannot upload an unsigned/debug build. | You (create upload keystore) |
| C3 | **Deceptive permission disclosure.** Camera/mic/location are requested under euphemisms ("telemetries", "drive sensors", "diagnostic permissions") in `MainActivity.kt`. | Violates Play **User Data / Permissions** policy — high rejection risk. | Fix copy (see P1) |
| C4 | **No privacy policy.** App records camera, audio, GPS and shows ads (uses ad ID). | Play **requires** a privacy policy URL for these. | Draft provided: `PRIVACY_POLICY.md` — you host it |
| C5 | **Play needs an AAB, not the committed `app-debug.apk`.** | Upload format + signing. | `./gradlew bundleRelease` |

---

## 🟠 HIGH — correctness / will cause runtime issues

### H1 — Foreground service can crash on Android 14+
`DefenderService.onStartCommand` calls `startForeground(..., LOCATION | MICROPHONE)`. On Android 14+,
if `RECORD_AUDIO` **or** location permission is not granted at that moment, the framework throws
`SecurityException` / `MissingForegroundServiceTypeException` and the app crashes. `RecorderViewModel`
starts the service (line ~525) — guard it.

**Recommended patch (`DefenderService.kt`, in the `"START"` branch):**
```kotlin
if (action == "START") {
    acquireWakeLock()
    val notification = buildNotification()
    try {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    } catch (e: Exception) {
        // Permission revoked / FGS restriction — fail safe instead of crashing
        android.util.Log.e("DefenderService", "startForeground failed: ${e.message}")
        releaseWakeLock()
        stopSelf()
        return START_NOT_STICKY
    }
}
```
Also ensure recording is only started **after** camera/mic/location runtime permissions are granted.

### H2 — `POST_NOTIFICATIONS` never requested at runtime
Declared in the manifest but **not** in the Accompanist permission request in `MainActivity.kt`. On
Android 13+ the foreground-service notification won't appear unless granted — and that notification is
the user's only signal that recording is active (also an FGS-policy expectation).

**Recommended patch (`MainActivity.kt`, `AppPermissionAndOnboardingWrapper`):**
```kotlin
val basePerms = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA
)
val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    basePerms + Manifest.permission.POST_NOTIFICATIONS else basePerms
val diagnosticPermissionsState = rememberMultiplePermissionsState(permissions = perms)
```

### H3 — WakeLock held with no timeout
`DefenderService.acquireWakeLock()` calls `acquire()` with no timeout → indefinite hold = battery drain
and a Lint **error** ("Wakelock without timeout"). Use a bounded timeout and renew if needed:
```kotlin
).apply { acquire(60 * 60 * 1000L /* 1h safety cap */) }
```

---

## 🟡 MEDIUM — polish / policy hygiene

- **M1 — Branding was inconsistent.** `app_name` was "DriverSHIELD", metadata said "Good Drivers' Defender",
  Gradle project was "My Application". ✅ Unified to **"Good Drivers Defender"** in this branch
  (`strings.xml`, `settings.gradle.kts`). Change `strings.xml` if you prefer a different store name —
  but the Play listing name should match `app_name`.
- **M2 — `stopForeground(true)` is deprecated.** Use `stopForeground(Service.STOP_FOREGROUND_REMOVE)`.
- **M3 — Release build has `isMinifyEnabled = false`.** Enabling R8 shrinking/obfuscation reduces size and
  hardens the app. Test thoroughly with `proguard-rules.pro` if you turn it on.
- **M4 — Client-side-only Pro check.** Entitlement is trusted from the local Billing cache (no server
  verification). Acceptable for v1; consider server-side validation later to resist tampering.
- **M5 — `metadata.json` claims `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`** and `.env.example` references
  `GEMINI_API_KEY`, but there is **no** Gemini/network code in the app. Harmless (no key leak), but the
  AI-Studio metadata is stale — safe to ignore or clean up.
- **M6 — Broad storage perms.** `READ_EXTERNAL_STORAGE` (no `maxSdkVersion`) is effectively unused on
  Android 13+ (granular media perms). Confirm it's needed; otherwise scope or drop it.

---

## 🧹 Repo hygiene (fixed in this branch)
- Removed **~510 MB** of junk from git tracking: bundled `jdk11/`, `jdk17/`, `.build_tools/` (full Gradle
  distro), `app-debug.apk`, JVM crash dumps, UI-Automator dumps, and the runtime `driver_recorder_db*` files.
- Untracked **placeholder secrets**: `key.properties`, `keystore.rsp`, `app/google-play-service-account.json`
  (all contained only placeholder/debug values — **no live credentials were leaked**, but they don't belong in git).
- Rewrote `.gitignore` to keep them out going forward. Tracked footprint: **1,876 files → 84 files**.
- Removed the hardcoded `org.gradle.java.home=D:\...` from `gradle.properties` (build portability).

> ⚠️ After you merge this branch, the bundled `jdk17/` is no longer in the repo. Build with a **system
> JDK 17** (set `JAVA_HOME`) — you already have Java installed. Your local copy of the folder is untouched.

---

## Suggested fix order
1. Apply H1, H2, H3 code patches, then `./gradlew assembleDebug` to confirm it still builds.
2. Run `docs/ON_DEVICE_TESTING.md` on your phone.
3. Do the Google Play prerequisites in `docs/GOOGLE_PLAY_READINESS.md` (AdMob IDs, signing, privacy policy, listing).
4. `./gradlew bundleRelease` → upload the AAB.
