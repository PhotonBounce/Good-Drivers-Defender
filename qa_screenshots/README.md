# QA Screenshots — Round 2 upgrades

Visual review of the three new features added this round. These PNGs were **rendered from the exact
colors/layout/text of the Compose code** (`app/src/main/java/com/example/ui/`) because this cloud QA
environment has no Android SDK/emulator and Google's Maven repo is network-blocked, so live device
capture wasn't possible here.

| File | Screen | What's new |
|------|--------|-----------|
| `01_drive_score.png` | **Drive Score** (`TripScoreScreen.kt`) | Gamified 0–100 safety score with circular gauge, letter grade, and stat cards, computed from logged incident severity. |
| `02_emergency_sos.png` | **Emergency SOS** (`EmergencySosScreen.kt`) | Tap-to-arm SOS with a 3-2-1 countdown that shares live GPS + last incident via the system share sheet. |
| `03_evidence_timeline.png` | **Evidence Timeline** (`EvidenceTimelineScreen.kt`) | Severity-color-coded vertical timeline of all incidents (green/amber/red by peak G-force). |
| `04_dashboard_protools.png` | **Dashboard** integration | The new "PRO TOOLS" launch row (SCORE / SOS / TIMELINE) added to the main console. |

## Get REAL device captures on your machine
These are faithful design renders, not live captures. To generate real screenshots:

```bash
# Real on-device capture:
adb exec-out screencap -p > shot.png

# Or headless Compose screenshot tests (project already uses Roborazzi):
./gradlew testDebugUnitTest        # writes PNGs under app/src/test/screenshots/ + build/outputs/roborazzi/
```

To re-render these mockups: `python3 tools/render_qa_screenshots.py`
