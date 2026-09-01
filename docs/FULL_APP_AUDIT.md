# Good Drivers Defender — Full Application Audit

**Date:** 2026-07-22 · **Scope:** entire repo (app source, service, UI, data, build, CI, tests, microsite, docs) · **Method:** six parallel specialist audits (state/monetization, recording pipeline, UI×2, security/privacy, build/CI) with independent cross-verification; every finding below was verified against source with file:line evidence. Findings found independently by 2+ auditors are marked ⛛.

**Headline:** the analytics/read-only surface of the app is genuinely solid (9 of 12 feature screens fully real, entitlement patterns correct, no Compose foot-guns, no network code, no secrets in git, coherent build matrix). The serious problems cluster in four areas: **(A) evidence-credibility theater, (B) action screens that don't act, (C) a recording pipeline that lives in the wrong layer, (D) billing edge-cases that punish paying users.**

---

## CRITICAL — launch blockers

### C1. Parking Sentry is 100% cosmetic
`ui/ParkingSentryScreen.kt:42-132` — "ARMED" is a local `remember` flag wired to nothing (no sensor, no service; defaults to armed). Its "detected impacts" list filters `isAutoCaptured`, which **no code ever sets true** (`IncidentRecord.kt:10` default false; both incident-creation paths omit it) → the list is structurally empty and the screen permanently claims "Your vehicle is clear." False safety assurance + Play deceptive-functionality exposure.
**Fix:** make it real (hoist armed state to VM, monitor g-force spikes, insert `isAutoCaptured=true` incidents) or remove the screen. → *Fixed in this pass: minimal real implementation.*

### C2. Collision "auto-alert" cannot auto-send ⛛
`ui/CollisionDetectScreen.kt:74-79` — the countdown for an *incapacitated* driver ends in an `ACTION_SEND` chooser that requires a conscious tap. No SMS permission/path exists. Detection runs only while that screen is composed, sampled at ~5 Hz (misses sub-100 ms spikes). Also: rotation resets the countdown state, silently cancelling an active alert (`:50-52`).
**Fix:** honest copy ("prepares an alert to share") now; true auto-SMS is a feature decision (SEND_SMS + emergency contact). → *Copy honesty + rotation-safe state in this pass.*

### C3. Swipe-away leaves a lying "Recording active" notification ⛛
`RecorderViewModel.kt:1439-1451` + `DefenderService.kt` — all capture (mic/GPS/camera) lives in the UI-scoped ViewModel; the FGS is an empty shell (notification + wake lock). Task removal kills capture via `onCleared()` but the service keeps running its "Recording active — GPS, camera & microphone in use" notification indefinitely. User believes they're protected; nothing records.
**Fix now:** `onTaskRemoved()` stops the service; `onCleared()` stops the service; null-intent sticky restart stops itself. **Architectural follow-up (open):** move capture ownership into the service for true background recording.

### C4. First-run GPS is dead ⛛
`RecorderViewModel.kt:474-477, 749-753` — location updates are requested once in `init` (before the permission dialog), the `SecurityException` is swallowed and never retried. Fresh install → grant permissions → first session records speed 0, no track, incidents at (0,0); motion auto-capture disabled too.
**Fix:** re-register the tracker when permissions become granted. → *Fixed in this pass.*

### C5. Evidence-fabrication cluster ⛛⛛ (3 auditors independently)
The app's core pitch — credible evidence — is undermined by its own export/report code:
- "secure, **encrypted** ZIP" (`LockerScreen.kt:197`) — plain unencrypted `ZipOutputStream` (`RecorderViewModel.kt:864`).
- `INTEGRITY LOCK: SECURED [MD5_CSUM: ${hashCode()}]` — a Java `hashCode()` of a folder name posing as an MD5 (`:906`; same trick as a "signature" in `ReportGeneratorScreen.kt:199`).
- Fabricated ASCII-art "DUAL-CAM frames" and a 44-byte silent WAV labeled `audio_witness.wav` are inserted into evidence bundles when no real media exists (`:896-924, 973-1005`).
- The complaint draft asserts "**calibrated**… **tamper-proof** recorder", auto-drafts "**certifies under penalty of perjury**… authentic as direct, unedited logs", claims logs are "**prima facie proof**", contains jurisdiction gibberish (`STATE / JURISDICTION: DECLARED OF OUTBOARD EVIDENCE`), and demands a **hardcoded $7,995 total that contradicts its own line items ($12,495)** (`ReportGeneratorScreen.kt:146-246`).
- Quick-capture incidents leak internal placeholders into the legal text: defendant "Owner of Vehicle Plate [**MANUAL ACTIVE SNAPSHOT**]" driving a "**SECURED VIDEO LOG**" (`RecorderViewModel.kt:566-567` + report filter gap).
- Chain-of-custody stamp shows **local time labeled UTC** (`DashboardScreen.kt:108-120`); report prints hemispheres hardcoded ("-118.24° West").
- The claims-softening pass recorded as complete in `docs/GOOGLE_PLAY_READINESS.md:156` was **not complete**: "court-ready" ×2 in `web/index.html`, "tamper-evident" in `microsite/index.html:64`, "Certified Suit Writer" TTS in `LockerScreen.kt:753`.
**Fix:** remove all fabricated artifacts, real SHA-256 or nothing, honest copy, computed totals, sentinel filtering, UTC/hemisphere correctness, finish the softening across web+microsite. → *Fixed in this pass.*

---

## HIGH

| # | Finding | Evidence | Status |
|---|---|---|---|
| H1 ⛛ | Mid-trip upgrade still cut off at 30-min free cap (job never re-checks `isPro`, not cancelled on purchase) | `RecorderViewModel.kt:593-601` | fixed |
| H2 | Transient Play error silently downgrades subscribers — `queryPurchasesAsync` response code unchecked, empty list → `isPro=false` on every resume | `BillingManager.kt:83-90` | fixed |
| H3 | `acknowledgePurchase` fire-and-forget (result discarded, Activity-scoped) → silent failure → Google auto-refund after 3 days | `BillingManager.kt:100-107` | fixed |
| H4 | "3 incidents/day" is actually 3-per-process-launch (in-memory counters) + check/increment race allows burst bypass | `RecorderViewModel.kt:352-368, 541-576` | fixed |
| H5 | FGS started without runtime-permission checks → SecurityException/RemoteServiceException window on Android 14+; session continues without FGS protection | `RecorderViewModel.kt:588-592`, `DefenderService.kt:52-69` | fixed |
| H6 ⛛ | Back gesture quits the app from every one of 15 routes (no back handling exists; predictive back previews app dismissal) | `MainActivity.kt:262-322` | fixed |
| H7 ⛛ | Subscribe button silent no-op when billing unavailable/details unloaded; `billingAvailable` consumed by no UI; Restore also silent | `PaywallScreen.kt:278-284`, `BillingManager.kt:163-169` | fixed |
| H8 | Report draft + selected incident lost on rotation (`activeSuitIncident` in plain `remember`; route survives, state doesn't) | `MainActivity.kt:161` | fixed |
| H9 | Main-thread MediaStore stream copies in single-file save (video/audio/snapshot) → jank/ANR | `RecorderViewModel.kt:1309-1388` | fixed |
| H10 | Video gallery shows empty after every app restart (`refreshSavedVideos()` only called on new-clip finalize; prune doesn't refresh) | `RecorderViewModel.kt:102,156` | fixed |
| H11 ⛛ | Exports silently fail on Android 7–9: `WRITE_EXTERNAL_STORAGE` declared ≤28 but never requested at runtime | manifest:15, `MainActivity.kt:126-134` | fixed |
| H12 | Privacy-claim gaps: 2 Hz high-accuracy GPS from launch (policy says "only while foreground service runs") + reverse-geocoding sends coordinates off-device via Play services; incident videos auto-copied to public Downloads (policy says "when you export") | `RecorderViewModel.kt:476,702-705,745,151-154` | fixed (geocode throttle + no auto-copy + session-scoped high-rate GPS; policy wording updated) |
| H13 ⛛ | Dead networking stack (Retrofit/OkHttp/Moshi/Firebase-BoM/Guava/secrets-plugin) force-kept by R8 rules; broad `com.example.data.**` keep exposes entitlement logic un-obfuscated | `build.gradle.kts:104-138`, `proguard-rules.pro:20-39` | fixed |
| H14 | Sticky-restart zombie: null-intent `onStartCommand` does nothing (no startForeground, no resume) | `DefenderService.kt:47-81` | fixed |

## MEDIUM (fixed in this pass unless noted)

- Billing collector leaked + Pro flashes to free on every rotation (`setBillingManager` never cancels the old collect; fresh `isLoading` state mirrored) — fixed.
- `ITEM_ALREADY_OWNED` not handled → owner stays locked out — fixed (re-sync).
- Trial revivable by setting device clock back (pure wall-clock check) — fixed (monotonic high-water mark + regression test).
- Trial expiry mid-session splits entitlement (flow stale vs live getter) — fixed (periodic re-eval).
- Manual STOP while moving instantly auto-restarts recording incl. mic (privacy) — fixed (suppression until vehicle stops).
- `MediaRecorder.stop()` throw skips `release()` (native mic leak) — fixed (release in finally).
- `withAudioEnabled()` without RECORD_AUDIO check → no incident video at all when mic denied — fixed (guarded; records video-only).
- Wake lock 1 h timeout never renewed (comment claims otherwise) → CPU dozes on long Pro trips — fixed (renewal).
- SOS/collision Google-Maps link broken in comma-decimal locales (safety-critical) — fixed (`Locale.US`).
- SOS "Hold to arm" is a plain tap — fixed (copy).
- Rotation wipes witness-form draft, timeframe selections, plan choice etc. (only splash was saveable) — fixed (rememberSaveable pass).
- Gallery tap → `ActivityNotFoundException` crash on devices without a video handler — fixed (guarded).
- "UTC" stamp shows local time; report hemisphere labels hardcoded N/W — fixed.
- Sentinel badge + "AUTO-SAVED" stat permanently dead (`isAutoCaptured` never written) — fixed via C1/auto-capture wiring.
- Show-over-lock-screen exposes evidence locker without device auth (`setShowWhenLocked`) — **open: product decision** (dashcam-mount UX vs evidence privacy).
- composeBom 2024.09.00 (~2 y old) under SDK 36 — **open: recommend bump + visual regression run**.
- CI: tests run after the expensive release build; installable artifacts upload from failing runs — fixed (tests first).
- CI `key.properties` heredoc unquoted (password with `$` corrupts) + empty-alias edge — fixed.
- Docs contradictions (targetSdk 35 line, banner-ads line, Pages instructions) — fixed.

## LOW (fixed: log-stripping proguard rule, `kotlinOptions`→`compilerOptions`, jetifier/suppressUnsupportedCompileSdk removal, `.gitignore` zipApk output, READ_EXTERNAL_STORAGE removal, FileProvider path narrowing + dead `external-path` removal, finalize-error handling + no-op media scan, HUD mirrored controls, the "resource/folder" share hack deleted, onboarding auto-dismiss on grant; noted-only: microsite SW is one-visit-behind SWR (acceptable for a marketing site), Pillow "QA screenshots" can drift from real UI, telemetry graph buffers reset on rotation)

## Verified clean ✅
Entitlement read patterns (getter vs flow) correct at every gate · no side-effects in composition · stable LazyColumn keys · FileProvider flags on all shares · no INTERNET permission and zero network code (no-ads/no-analytics claims are code-true) · exported components locked down · no secrets in git · TTS never speaks sensitive data · SOS null-island guard intact · notification channel ordering · billing tokens never logged · KSP/Kotlin/AGP/Gradle version matrix coherent · CI has no injection surface · existing unit tests are real tests · microsite anchors/ids/emulator wiring/manifest icons all consistent.

## Top remaining risks (need product/owner decisions)
1. **Move capture into the service** (C3 root cause) — until then, "background recording" lasts only as long as the Activity's ViewModel.
2. **Lock-screen exposure** — choose: drop `setShowWhenLocked`, or gate locker/gallery behind an unlock.
3. **Compose BoM bump** + Roborazzi coverage for real screens.
4. **Missing tests** (top 5): daily-cap limiter · BillingManager state mapping · price-phase selection · trip-end scoring integration · Room DAO round-trip.
5. **Signing secrets still unset** → release AAB remains unsigned; **microsite still not uploaded**; **new logo still pending re-attachment**.
