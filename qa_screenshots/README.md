# QA Screenshots — new feature screens

Visual review of the features added in rounds 2 and 3. These PNGs were **rendered from the exact
colors/layout/text of the Compose code** (`app/src/main/java/com/example/ui/`) because this cloud QA
environment has no Android SDK/emulator and Google's Maven repo is network-blocked, so live device
capture wasn't possible here.

## Round 2
| File | Screen | What's new |
|------|--------|-----------|
| `01_drive_score.png` | **Drive Score** (`TripScoreScreen.kt`) | Gamified 0–100 safety score gauge, letter grade, and stat cards. |
| `02_emergency_sos.png` | **Emergency SOS** (`EmergencySosScreen.kt`) | Tap-to-arm SOS with 3-2-1 countdown that shares live GPS + last incident. |
| `03_evidence_timeline.png` | **Evidence Timeline** (`EvidenceTimelineScreen.kt`) | Severity-color-coded vertical timeline of all incidents. |
| `04_dashboard_protools.png` | **Dashboard** integration | The "PRO TOOLS" launch row added to the main console. |

## Round 3
| File | Screen | What's new |
|------|--------|-----------|
| `05_parking_sentry.png` | **Parking Sentry** (`ParkingSentryScreen.kt`) | Armed guard mode with rotating radar sweep, impact-sensitivity selector, and a detected-impact log. |
| `06_telemetry_graph.png` | **Live Telemetry** (`TelemetryGraphScreen.kt`) | Real-time dual-line chart of speed + G-force with NOW/MAX/AVG/PEAK stats. |
| `07_trip_history.png` | **Trip History** (`TripHistoryScreen.kt`) | Past drive sessions grouped from incidents, each with a speed sparkline + stats. |

All 7 are reachable from two "PRO TOOLS" rows on the dashboard (SCORE / SOS / TIMELINE and SENTRY / GRAPH / TRIPS).

## Get REAL device captures on your machine
These are faithful design renders, not live captures. To generate real screenshots:

```bash
adb exec-out screencap -p > shot.png          # live on-device capture
./gradlew testDebugUnitTest                    # headless Roborazzi Compose screenshots
python3 tools/render_qa_screenshots.py         # re-render these mockups
```
