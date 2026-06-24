# On-Device QA — Good Drivers Defender

Run this on **your machine** with your phone connected (USB debugging on). The cloud
environment can't reach your phone, so these are the steps to execute locally. Copy/paste
the commands; I'll interpret any output you paste back.

## 0. Prerequisites
- Phone: **Settings → About phone → tap Build number 7×** to enable Developer options, then enable
  **USB debugging**. Plug in via USB and accept the "Allow USB debugging?" prompt.
- A system **JDK 17** on PATH (`java -version`). Build with the Gradle wrapper.

## 1. Confirm the device is visible
```bash
adb devices
```
Expect one device listed as `device` (not `unauthorized`).

## 2. Build and install a debug build
```bash
# from the project root
gradlew.bat clean assembleDebug          # Windows
# ./gradlew clean assembleDebug          # macOS/Linux
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 3. Launch + watch logs
```bash
adb logcat -c
adb shell am start -n com.aistudio.driverrecorder.gpxrt/com.example.MainActivity
adb logcat *:E AdManager:D BillingManager:D DefenderService:D
```

## 4. Manual test script (tap through on the phone)
| # | Step | Expected | Watch for |
|---|------|----------|-----------|
| 1 | First launch → splash | Splash, then onboarding card | No crash |
| 2 | Permission prompts | Camera, Mic, Location asked | **Notifications** prompt appears on Android 13+ (QA item H2 — currently missing) |
| 3 | Grant all → Dashboard | Camera preview + HUD/speedometer | Preview renders, no ANR |
| 4 | Start recording | Ongoing notification "…Shield Engaged" | Notification visible = FGS OK (QA H1/H2) |
| 5 | Lock screen while recording | App shows over lockscreen / keeps recording | No crash on lock/power |
| 6 | Stop recording → Videos tab | Saved clip appears, plays back | File saved via MediaStore |
| 7 | Evidence (Locker) tab | Incident list | Data persists after relaunch |
| 8 | Suit Writer (Report) | Paywalled for free users | Gating works |
| 9 | Go Pro tab | Paywall shows prices | Use a **license-tester** account (see below) |
| 10 | Ads | Test banner/interstitial show (sample IDs) | Replace with real IDs before release (QA C1) |

## 5. Billing testing (without real charges)
- In **Play Console → Setup → License testing**, add your Google account as a tester.
- Upload at least one build to an **internal testing** track and create the two subscription products
  (`defender_pro_monthly`, `defender_pro_annual`) — billing returns no products until they exist in Console.
- Testers see "(Test)" purchases; no real money is charged.

## 6. Quick automated checks (optional but recommended)
```bash
gradlew.bat lint                 # static analysis → app/build/reports/lint-results-*.html
gradlew.bat testDebugUnitTest    # unit + Robolectric/Roborazzi screenshot tests
```

## 7. What to send me back
Paste: `adb devices` output, any red lines from logcat during steps 2–6, and the path/result of the
lint report. I'll triage and push fixes.
