#!/usr/bin/env python3
"""
Render faithful PNG mockups of the 3 new feature screens (Drive Score, Emergency SOS,
Evidence Timeline) plus the dashboard Pro-Tools launch row. These mirror the exact colors,
text, and layout of the Compose code in app/src/main/java/com/example/ui/.

This exists because the cloud QA box has no Android SDK/emulator (and Google's Maven repo is
blocked), so live device capture isn't possible here. Run Roborazzi locally for real captures:
    ./gradlew testDebugUnitTest   (see app/src/test/.../*ScreenshotTest.kt)
"""
import math, os, subprocess
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.join(os.path.dirname(__file__), "..", "qa_screenshots")
os.makedirs(OUT, exist_ok=True)

def fp(q):
    try:
        p = subprocess.run(["fc-match", "-f", "%{file}", q], capture_output=True, text=True).stdout.strip()
        return p if p and os.path.exists(p) else None
    except Exception:
        return None

_REG, _BOLD = fp("sans"), fp("sans:bold")
_MONO, _MONOB = fp("monospace"), fp("monospace:bold")
_cache = {}
def font(kind, size):
    path = {"reg": _REG, "bold": _BOLD, "mono": _MONO, "monob": _MONOB}.get(kind) or _REG
    key = (path, size)
    if key not in _cache:
        _cache[key] = ImageFont.truetype(path, size) if path else ImageFont.load_default()
    return _cache[key]

def hx(c):  # "#RRGGBB" or "#AARRGGBB" -> (r,g,b[,a])
    c = c.lstrip("#")
    if len(c) == 8:
        a, r, g, b = int(c[0:2],16), int(c[2:4],16), int(c[4:6],16), int(c[6:8],16)
        return (r, g, b, a)
    return (int(c[0:2],16), int(c[2:4],16), int(c[4:6],16))

W, H = 900, 1950
SB = 50  # status bar height

def new_screen(bg):
    img = Image.new("RGB", (W, H), hx(bg))
    d = ImageDraw.Draw(img, "RGBA")
    # status bar
    d.rectangle([0, 0, W, SB], fill=(0, 0, 0, 80))
    d.text((28, SB/2), "9:41", font=font("bold", 26), fill=(255,255,255), anchor="lm")
    d.text((W-28, SB/2), "5G  ▮▮▮▮  84%", font=font("reg", 22), fill=(255,255,255,210), anchor="rm")
    return img, d

def rrect(d, box, radius, fill=None, outline=None, width=2):
    d.rounded_rectangle(box, radius=radius, fill=hx(fill) if fill else None,
                        outline=hx(outline) if outline else None, width=width)

def T(d, xy, s, kind, size, fill, anchor="lm"):
    d.text(xy, s, font=font(kind, size), fill=hx(fill) if isinstance(fill,str) else fill, anchor=anchor)

def save(img, name):
    path = os.path.abspath(os.path.join(OUT, name))
    img.save(path)
    print("wrote", path)


# ─────────────────────────── 1. DRIVE SCORE ───────────────────────────
def render_drive_score():
    img, d = new_screen("#0F172A")
    y = SB + 30
    T(d, (40, y+20), "‹", "bold", 44, "#FFFFFF", "lm")
    T(d, (78, y+20), "DRIVE SCORE", "bold", 34, "#FFFFFF", "lm")
    T(d, (80, y+58), "Your safety rating for logged trips", "reg", 22, "#94A3B8", "lm")

    score, grade, accent = 82, "A", "#22C55E"
    cx, cy, R = W//2, y+330, 170
    # gauge track (270° from 135°, clockwise -> gap at bottom)
    box = [cx-R, cy-R, cx+R, cy+R]
    d.arc(box, 135, 45, fill=hx("#1E293B"), width=40)
    end = 135 + 270*(score/100)
    d.arc(box, 135, end, fill=hx(accent), width=40)
    # round caps
    for ang in (135, end):
        rad = math.radians(ang)
        ex, ey = cx + R*math.cos(rad), cy + R*math.sin(rad)
        d.ellipse([ex-20, ey-20, ex+20, ey+20], fill=hx(accent))
    T(d, (cx, cy-26), str(score), "monob", 120, accent, "mm")
    T(d, (cx, cy+58), "/ 100", "reg", 26, "#64748B", "mm")
    rrect(d, [cx-78, cy+86, cx+78, cy+128], 12, fill="#14321F", outline=accent, width=2)
    T(d, (cx, cy+107), f"GRADE {grade}", "bold", 24, accent, "mm")

    # Adaptive Q-score: trend chip + EMA score (mirrors TripScoreScreen)
    ty = cy + R + 36
    rrect(d, [cx-135, ty-24, cx+135, ty+24], 11, fill="#143020", outline="#22C55E", width=2)
    T(d, (cx, ty), "▲ IMPROVING", "bold", 23, "#22C55E", "mm")
    T(d, (cx, ty+50), "Adaptive: 85.4", "mono", 22, "#64748B", "mm")

    # stat cards
    cards = [
        ("TOP SPEED", "71", "mph", "#38BDF8"),
        ("MAX FORCE", "2.13", "G", "#F97316"),
        ("HARD BRAKES", "1", "events", "#EF4444"),
        ("AUTO-SAVED", "3", "clips", "#22C55E"),
    ]
    gx0, gw, gh, gap = 40, (W-80-24)//2, 175, 24
    sy = cy + R + 150
    for i, (lab, val, unit, tint) in enumerate(cards):
        cxx = gx0 + (i % 2) * (gw + gap)
        cyy = sy + (i // 2) * (gh + gap)
        rrect(d, [cxx, cyy, cxx+gw, cyy+gh], 22, fill="#1E293B")
        d.ellipse([cxx+24, cyy+24, cxx+54, cyy+54], outline=hx(tint), width=4)
        T(d, (cxx+30, cyy+95), val, "monob", 52, "#FFFFFF", "lm")
        wv = d.textlength(val, font=font("monob", 52))
        T(d, (cxx+40+wv, cyy+102), unit, "reg", 22, "#64748B", "lm")
        T(d, (cxx+30, cyy+140), lab, "bold", 20, "#94A3B8", "lm")

    T(d, (40, sy + 2*gh + gap + 30),
      "Based on 4 logged incidents. Fewer hard-braking events raise your score.",
      "reg", 20, "#94A3B8", "lm")
    save(img, "01_drive_score.png")
# NOTE: stat-card grid pushed down to make room for the adaptive trend row above.


# ─────────────────────────── 2. EMERGENCY SOS ───────────────────────────
def render_sos(armed=True):
    img, d = new_screen("#1A0606")
    y = SB + 30
    T(d, (40, y+20), "‹", "bold", 44, "#FFFFFF", "lm")
    T(d, (78, y+20), "EMERGENCY SOS", "bold", 34, "#FFFFFF", "lm")
    sub = "SENDING IN 2…  tap to CANCEL" if armed else \
          "Hold to arm. Shares live GPS + last incident."
    T(d, (W//2, y+78), sub, "reg", 22, "#FCA5A5" if armed else "#94A3B8", "mm")

    cx, cy = W//2, y+440
    # pulsing halo
    d.ellipse([cx-250, cy-250, cx+250, cy+250], fill=hx("#26EF4444"))
    d.ellipse([cx-205, cy-205, cx+205, cy+205], fill=hx("#33EF4444"))
    # main button
    d.ellipse([cx-180, cy-180, cx+180, cy+180], fill=hx("#B91C1C" if armed else "#DC2626"),
              outline=hx("#FCA5A5"), width=8)
    if armed:
        T(d, (cx, cy-20), "2", "monob", 150, "#FFFFFF", "mm")
        T(d, (cx, cy+90), "TAP TO CANCEL", "bold", 22, "#FFFFFF", "mm")
    else:
        T(d, (cx, cy-30), "✚", "bold", 120, "#FFFFFF", "mm")
        T(d, (cx, cy+70), "ACTIVATE", "bold", 28, "#FFFFFF", "mm")

    # live location card
    bx0, bx1 = 40, W-40
    by0 = cy + 290
    rrect(d, [bx0, by0, bx1, by0+150], 18, fill="#000000", outline="#66EF4444", width=2)
    T(d, (bx0+28, by0+34), "LIVE LOCATION BROADCAST", "bold", 20, "#EF4444", "lm")
    T(d, (bx0+28, by0+82), "LAT 47.620500   LON -122.349300", "mono", 26, "#FFFFFF", "lm")
    T(d, (bx0+28, by0+122), "3 incident(s) on file will be referenced", "reg", 20, "#94A3B8", "lm")
    save(img, "02_emergency_sos.png")


# ─────────────────────────── 3. EVIDENCE TIMELINE ───────────────────────────
def render_timeline():
    img, d = new_screen("#0F172A")
    y = SB + 24
    T(d, (40, y+16), "‹", "bold", 44, "#FFFFFF", "lm")
    T(d, (78, y+16), "EVIDENCE TIMELINE", "bold", 32, "#FFFFFF", "lm")
    T(d, (80, y+54), "4 incidents — newest first, color = peak force", "reg", 21, "#94A3B8", "lm")

    incidents = [
        ("Jun 15 • 8:42 AM", "Interstate 5 N • King County", "68", "2.41", "SEVERE", "#EF4444",
         "Reckless Cut-off / Lane Change", True),
        ("Jun 14 • 6:10 PM", "Highway 99 • Pierce County", "54", "1.72", "HARD", "#F97316",
         "Tailgating / Dangerous Proximity", True),
        ("Jun 14 • 7:55 AM", "Aurora Ave N • King County", "31", "1.12", "MINOR", "#22C55E",
         "", False),
        ("Jun 12 • 5:03 PM", "Rainier Ave S • King County", "44", "0.95", "MINOR", "#22C55E",
         "Failure to Yield", False),
    ]
    rail = 70
    cardx0, cardx1 = rail+30, W-40
    yy = y + 110
    for i, (t, loc, spd, g, sev, col, behav, auto) in enumerate(incidents):
        ch = 230 if behav else 195
        # rail line
        d.line([(rail, yy+18), (rail, yy+ch+34)], fill=hx("#1E293B"), width=4)
        # node
        d.ellipse([rail-16, yy+2, rail+16, yy+34], fill=hx(col), outline=hx(col+"40") if len(col)==7 else hx(col), width=6)
        # card
        rrect(d, [cardx0, yy, cardx1, yy+ch], 18, fill="#1E293B", outline=col+"66" if len(col)==7 else col, width=2)
        T(d, (cardx0+24, yy+34), t, "bold", 24, "#FFFFFF", "lm")
        # severity pill
        pw = 120
        rrect(d, [cardx1-pw-20, yy+16, cardx1-20, yy+52], 8, fill="#22"+col.lstrip("#"), outline=col, width=2)
        T(d, (cardx1-20-pw/2, yy+34), sev, "bold", 19, col, "mm")
        T(d, (cardx0+24, yy+78), loc, "reg", 21, "#CBD5E1", "lm")
        # metrics
        mx = cardx0+24
        for lab, val in [("SPEED", f"{spd} mph"), ("FORCE", f"{g} G"), ("SOURCE", "AUTO" if auto else "MANUAL")]:
            T(d, (mx, yy+120), lab, "bold", 16, "#64748B", "lm")
            T(d, (mx, yy+148), val, "monob", 24, "#FFFFFF", "lm")
            mx += 200
        if behav:
            T(d, (cardx0+24, yy+192), f"“{behav}”", "reg", 20, "#FBBF24", "lm")
        yy += ch + 26
    save(img, "03_evidence_timeline.png")


# ─────────────────────────── 4. DASHBOARD PRO-TOOLS ROW ───────────────────────────
def render_dashboard_protools():
    img, d = new_screen("#020617")
    y = SB + 24
    T(d, (40, y), "DASHBOARD  ·  bottom console (Risk Intelligence highlighted)", "bold", 24, "#94A3B8", "lm")
    yy = y + 50
    # speed card + limit/G column (mirrors existing dashboard row)
    rrect(d, [40, yy, 470, yy+200], 18, fill="#1E293B")
    T(d, (255, yy+90), "63", "monob", 90, "#22C55E", "mm")
    T(d, (255, yy+160), "MPH", "bold", 22, "#D1D5DB", "mm")
    rrect(d, [490, yy, W-40, yy+92], 16, fill="#1E293B")
    T(d, (W//2+190, yy+30), "LIMIT", "reg", 18, "#9CA3AF", "mm")
    T(d, (W//2+190, yy+62), "65 MPH", "bold", 30, "#FFFFFF", "mm")
    rrect(d, [490, yy+108, W-40, yy+200], 16, fill="#7C2D12")
    T(d, (W//2+190, yy+140), "2.13 G", "bold", 30, "#EF4444", "mm")
    T(d, (W//2+190, yy+176), "HEAVY FORCE SHOCK", "reg", 17, "#E5E7EB", "mm")

    # NEW: Risk Intelligence bar (smoothed real-time Q-risk readout, highlighted)
    riy = yy + 256
    d.rounded_rectangle([28, riy-14, W-28, riy+78], radius=18, outline=hx("#FBBF24"), width=3)
    T(d, (44, riy-40), "▼ NEW: RISK INTELLIGENCE", "bold", 20, "#FBBF24", "lm")
    rrect(d, [40, riy, W-40, riy+64], 14, fill="#0F172A")
    d.ellipse([62, riy+24, 80, riy+42], fill=hx("#FBBF24"))
    T(d, (98, riy+33), "RISK INTELLIGENCE", "bold", 18, "#94A3B8", "lm")
    T(d, (W//2+40, riy+33), "MODERATE RISK", "bold", 20, "#FBBF24", "mm")
    mx0, mx1 = W-250, W-60
    rrect(d, [mx0, riy+26, mx1, riy+40], 6, fill="#1E293B")
    rrect(d, [mx0, riy+26, mx0+int((mx1-mx0)*0.55), riy+40], 6, fill="#FBBF24")

    # pro tools row
    ry = yy + 396
    T(d, (44, ry-40), "PRO TOOLS", "bold", 20, "#94A3B8", "lm")
    tools = [("SCORE", "#22C55E", "★"), ("SOS", "#EF4444", "✚"), ("TIMELINE", "#38BDF8", "≡")]
    bw = (W-80-2*16)//3
    for i,(lab,col,gly) in enumerate(tools):
        bx = 40 + i*(bw+16)
        rrect(d, [bx, ry, bx+bw, ry+112], 14, fill="#1E293B", outline=col+"99", width=2)
        d.ellipse([bx+bw/2-26, ry+22, bx+bw/2+26, ry+74], outline=hx(col), width=4)
        T(d, (bx+bw/2, ry+48), gly, "bold", 30, col, "mm")
        T(d, (bx+bw/2, ry+92), lab, "bold", 19, "#FFFFFF", "mm")

    # action buttons row beneath
    ay = ry + 160
    rrect(d, [40, ay, 430, ay+96], 14, fill="#2563EB")
    T(d, (235, ay+38), "START TRIP", "bold", 26, "#FFFFFF", "mm")
    T(d, (235, ay+70), "Auto-motion ready", "reg", 18, "#DBEAFE", "mm")
    rrect(d, [446, ay, 660, ay+96], 14, fill="#334155")
    T(d, (553, ay+48), "ROAD", "bold", 22, "#FFFFFF", "mm")
    rrect(d, [676, ay, W-40, ay+96], 14, fill="#059669")
    T(d, (778, ay+48), "AUTO", "bold", 22, "#FFFFFF", "mm")
    save(img, "04_dashboard_protools.png")


def poly(d, pts, color, width):
    d.line(pts, fill=hx(color), width=width, joint="curve")


# ─────────────────────────── 5. PARKING SENTRY ───────────────────────────
def render_parking_sentry():
    img, d = new_screen("#050B16")
    y = SB + 30
    T(d, (40, y+18), "‹", "bold", 44, "#FFFFFF", "lm")
    T(d, (78, y+18), "PARKING SENTRY", "bold", 34, "#FFFFFF", "lm")

    green = "#22C55E"
    cx, cy, R = W//2, y + 260, 200
    for i in (1, 2, 3):
        r = R*i/3
        d.ellipse([cx-r, cy-r, cx+r, cy+r], outline=hx("#4022C55E"), width=2)
    d.line([(cx-R, cy), (cx+R, cy)], fill=hx("#2622C55E"), width=2)
    d.line([(cx, cy-R), (cx, cy+R)], fill=hx("#2622C55E"), width=2)
    d.pieslice([cx-R, cy-R, cx+R, cy+R], 300, 355, fill=hx("#4D22C55E"))
    # shield glyph (polygon)
    sh = [(cx-30, cy-58), (cx+30, cy-58), (cx+30, cy-20), (cx, cy+4), (cx-30, cy-20)]
    d.polygon(sh, fill=hx(green))
    T(d, (cx, cy-34), "✓", "bold", 30, "#050B16", "mm")
    T(d, (cx, cy+34), "SENTRY ARMED", "bold", 30, green, "mm")
    T(d, (cx, cy+66), "Monitoring for impacts", "reg", 20, "#94A3B8", "mm")

    sy = cy + R + 36
    T(d, (40, sy), "IMPACT SENSITIVITY", "bold", 20, "#94A3B8", "lm")
    cy2 = sy + 30
    cw = (W-80-2*12)//3
    for i, lev in enumerate(["LOW", "MEDIUM", "HIGH"]):
        bx = 40 + i*(cw+12)
        sel = lev == "MEDIUM"
        rrect(d, [bx, cy2, bx+cw, cy2+70], 12, fill="#1D4ED8" if sel else "#1E293B",
              outline="#60A5FA" if sel else None, width=2)
        T(d, (bx+cw/2, cy2+35), lev, "bold", 22, "#FFFFFF" if sel else "#94A3B8", "mm")

    by = cy2 + 100
    rrect(d, [40, by, W-40, by+86], 14, fill="#991B1B")
    T(d, (W//2, by+43), "DISARM SENTRY", "bold", 26, "#FFFFFF", "mm")

    ly = by + 120
    T(d, (40, ly), "DETECTED IMPACTS (2)", "bold", 20, "#94A3B8", "lm")
    events = [("Jun 15 • 2:14 PM", "1.84"), ("Jun 13 • 9:42 AM", "1.51")]
    ey = ly + 30
    for t, g in events:
        rrect(d, [40, ey, W-40, ey+72], 12, fill="#1E293B")
        T(d, (64, ey+36), t, "reg", 24, "#FFFFFF", "lm")
        T(d, (W-64, ey+36), f"{g} G", "monob", 26, "#F97316", "rm")
        ey += 84
    save(img, "05_parking_sentry.png")


# ─────────────────────────── 6. LIVE TELEMETRY GRAPH ───────────────────────────
def render_telemetry_graph():
    img, d = new_screen("#0F172A")
    y = SB + 30
    T(d, (40, y+18), "‹", "bold", 44, "#FFFFFF", "lm")
    T(d, (78, y+18), "LIVE TELEMETRY", "bold", 34, "#FFFFFF", "lm")
    T(d, (80, y+56), "Real-time speed & G-force trace", "reg", 22, "#94A3B8", "lm")

    cyan, orange = "#38BDF8", "#F97316"
    ly = y + 96
    d.ellipse([40, ly, 60, ly+20], fill=hx(cyan)); T(d, (72, ly+10), "SPEED (mph)", "bold", 21, "#CBD5E1", "lm")
    d.ellipse([320, ly, 340, ly+20], fill=hx(orange)); T(d, (352, ly+10), "G-FORCE", "bold", 21, "#CBD5E1", "lm")

    # chart
    gx0, gy0, gx1, gy1 = 40, ly+50, W-40, ly+50+440
    rrect(d, [gx0, gy0, gx1, gy1], 16, fill="#020617")
    px0, py0, px1, py1 = gx0+24, gy0+24, gx1-24, gy1-24
    for i in range(5):
        gyy = py0 + (py1-py0)*i/4
        d.line([(px0, gyy), (px1, gyy)], fill=hx("#1E293B"), width=2)
    n = 34
    spd = [38 + 26*math.sin(i/3.0) + (8 if i > 22 else 0) for i in range(n)]
    gf = [1.1 + 1.2*abs(math.sin(i/2.3 + 1)) for i in range(n)]
    smax, gmax = 80.0, 3.0
    sp_pts = [(px0 + (px1-px0)*i/(n-1), py1 - (spd[i]/smax)*(py1-py0)) for i in range(n)]
    gf_pts = [(px0 + (px1-px0)*i/(n-1), py1 - (gf[i]/gmax)*(py1-py0)) for i in range(n)]
    poly(d, gf_pts, orange, 6)
    poly(d, sp_pts, cyan, 7)

    sy = gy1 + 28
    stats = [("NOW", "58", "mph", cyan), ("MAX", "71", "mph", "#EF4444"),
             ("AVG", "44", "mph", "#22C55E"), ("PEAK G", "2.3", "G", orange)]
    sw = (W-80-3*12)//4
    for i, (lab, val, unit, tint) in enumerate(stats):
        bx = 40 + i*(sw+12)
        rrect(d, [bx, sy, bx+sw, sy+150], 12, fill="#1E293B")
        T(d, (bx+16, sy+28), lab, "bold", 17, "#64748B", "lm")
        T(d, (bx+16, sy+78), val, "monob", 40, tint, "lm")
        T(d, (bx+16, sy+120), unit, "reg", 18, "#94A3B8", "lm")
    save(img, "06_telemetry_graph.png")


# ─────────────────────────── 7. TRIP HISTORY ───────────────────────────
def render_trip_history():
    img, d = new_screen("#0F172A")
    y = SB + 24
    T(d, (40, y+16), "‹", "bold", 44, "#FFFFFF", "lm")
    T(d, (78, y+16), "TRIP HISTORY", "bold", 32, "#FFFFFF", "lm")
    T(d, (80, y+54), "3 drive session(s) logged", "reg", 21, "#94A3B8", "lm")

    indigo = "#818CF8"
    sessions = [
        ("Sun, Jun 15 • 8:42 AM", "3", "71 mph", "2.41", [28,40,55,52,68,71,49,38]),
        ("Sat, Jun 14 • 6:10 PM", "2", "54 mph", "1.72", [22,35,48,54,44,30]),
        ("Thu, Jun 12 • 5:03 PM", "1", "44 mph", "0.95", [18,30,44,40,26,33,20]),
    ]
    yy = y + 100
    cardx0, cardx1 = 40, W-40
    for date, ev, top, pg, speeds in sessions:
        ch = 290
        rrect(d, [cardx0, yy, cardx1, yy+ch], 16, fill="#1E293B")
        T(d, (cardx0+24, yy+34), date, "bold", 24, "#FFFFFF", "lm")
        # sparkline
        spx0, spy0, spx1, spy1 = cardx0+24, yy+70, cardx1-24, yy+170
        mv = max(speeds)
        pts = [(spx0 + (spx1-spx0)*i/(len(speeds)-1), spy1 - speeds[i]/mv*(spy1-spy0)) for i in range(len(speeds))]
        poly(d, pts, indigo, 5)
        # stats
        labels = [("EVENTS", ev), ("TOP SPEED", top), ("PEAK G", pg)]
        third = (cardx1-cardx0)/3
        for i, (lab, val) in enumerate(labels):
            cxx = cardx0 + third*i + third/2
            T(d, (cxx, yy+218), val, "monob", 28, "#FFFFFF", "mm")
            T(d, (cxx, yy+252), lab, "bold", 16, "#64748B", "mm")
        yy += ch + 22
    save(img, "07_trip_history.png")


def arc_gauge(d, cx, cy, R, frac, accent, track="#1E293B", stroke=40):
    box = [cx-R, cy-R, cx+R, cy+R]
    d.arc(box, 135, 45, fill=hx(track), width=stroke)
    end = 135 + 270*frac
    d.arc(box, 135, end, fill=hx(accent), width=stroke)
    for ang in (135, end):
        rad = math.radians(ang)
        ex, ey = cx + R*math.cos(rad), cy + R*math.sin(rad)
        r = stroke/2
        d.ellipse([ex-r, ey-r, ex+r, ey+r], fill=hx(accent))


# ─────────────────────────── 8. COLLISION DETECTION (alert) ───────────────────────────
def render_collision():
    img, d = new_screen("#1A0606")
    y = SB + 30
    rrect(d, [40, y, W-40, y+90], 14, fill="#DC2626")
    T(d, (W//2, y+45), "⚠  COLLISION DETECTED", "bold", 36, "#FFFFFF", "mm")

    cx, cy, R = W//2, y+330, 165
    arc_gauge(d, cx, cy, R, 0.70, "#EF4444", stroke=38)
    T(d, (cx, cy-16), "4.2", "monob", 110, "#EF4444", "mm")
    T(d, (cx, cy+60), "G-FORCE", "bold", 22, "#94A3B8", "mm")

    sy = cy + R + 60
    T(d, (W//2, sy), "Auto-alerting in", "reg", 26, "#FCA5A5", "mm")
    T(d, (W//2, sy+80), "9", "monob", 130, "#FFFFFF", "mm")
    T(d, (W//2, sy+165), "seconds", "reg", 24, "#FCA5A5", "mm")

    by = sy + 230
    rrect(d, [40, by, W-40, by+110], 16, fill="#16A34A")
    T(d, (W//2, by+55), "✓  I'M OK — CANCEL", "bold", 32, "#FFFFFF", "mm")
    rrect(d, [40, by+128, W-40, by+238], 16, fill="#B91C1C")
    T(d, (W//2, by+183), "✚  SEND HELP NOW", "bold", 32, "#FFFFFF", "mm")
    save(img, "08_collision_detect.png")


# ─────────────────────────── 9. AR HUD ───────────────────────────
def render_hud():
    img = Image.new("RGB", (W, H), (0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")
    # sky / ground
    d.rectangle([0, 0, W, H//2], fill=hx("#0A1733"))
    d.rectangle([0, H//2, W, H], fill=hx("#071A12"))
    cyan = "#22D3EE"
    cyc = H//2
    off = int((W/2) * math.tan(math.radians(7)))
    d.line([(0, cyc-off), (W, cyc+off)], fill=hx(cyan), width=5)
    for p in (-2, -1, 1, 2):
        yy = cyc + p*90
        d.line([(int(W*0.34), yy), (int(W*0.66), yy)], fill=hx("#6622D3EE"), width=3)
    # reticle
    d.line([(W//2-110, cyc), (W//2-40, cyc)], fill=hx("#4ADE80"), width=6)
    d.line([(W//2+40, cyc), (W//2+110, cyc)], fill=hx("#4ADE80"), width=6)
    d.ellipse([W//2-9, cyc-9, W//2+9, cyc+9], fill=hx("#4ADE80"))
    # status bar
    d.rectangle([0, 0, W, SB], fill=(0, 0, 0, 120))
    T(d, (28, SB/2), "9:41", "bold", 26, "#FFFFFF", "lm")
    T(d, (W-28, SB/2), "5G  ▮▮▮▮  84%", "reg", 22, (255,255,255,210), "rm")
    # header
    T(d, (40, SB+44), "‹  AR HUD", "bold", 30, "#FFFFFF", "lm")
    rrect(d, [W-230, SB+24, W-40, SB+78], 8, fill="#1E293B")
    T(d, (W-135, SB+51), "⟷ MIRROR", "bold", 22, cyan, "mm")
    # speed
    T(d, (W//2, cyc-40), "64", "monob", 210, cyan, "mm")
    T(d, (W//2, cyc+120), "MPH", "bold", 40, cyan, "mm")
    # bottom strip
    by = H - 150
    for i, (lab, val, col) in enumerate([("TIME", "08:42:13", "#FFFFFF"),
                                          ("G-FORCE", "1.34", cyan),
                                          ("POSITION", "47.6205, -122.3493", "#94A3B8")]):
        bx = 40 + i*((W-80)//3)
        T(d, (bx, by), lab, "bold", 18, "#64748B", "lm")
        T(d, (bx, by+34), val, "monob", 24, col, "lm")
    save(img, "09_ar_hud.png")


# ─────────────────────────── 10. ACHIEVEMENTS ───────────────────────────
def render_achievements():
    img, d = new_screen("#0F172A")
    y = SB + 24
    T(d, (40, y+16), "‹", "bold", 44, "#FFFFFF", "lm")
    T(d, (78, y+16), "ACHIEVEMENTS", "bold", 32, "#FFFFFF", "lm")

    # level card
    ly = y + 70
    rrect(d, [40, ly, W-40, ly+170], 18, fill="#1E293B", outline="#66FBBF24", width=2)
    d.ellipse([72, ly+28, 168, ly+124], fill=hx("#422006"))
    T(d, (120, ly+76), "3", "monob", 52, "#FBBF24", "mm")
    T(d, (190, ly+58), "DRIVER LEVEL 3", "bold", 28, "#FFFFFF", "lm")
    T(d, (190, ly+96), "4 / 6 badges • 850 XP", "reg", 22, "#94A3B8", "lm")
    # xp bar
    bx0, bx1 = 72, W-72
    rrect(d, [bx0, ly+138, bx1, ly+156], 9, fill="#0F172A")
    rrect(d, [bx0, ly+138, bx0 + int((bx1-bx0)*0.7), ly+156], 9, fill="#FBBF24")

    T(d, (40, ly+200), "BADGES", "bold", 20, "#94A3B8", "lm")

    badges = [
        ("First Evidence", "Log your first incident", "#22C55E", True, 1.0, "✓"),
        ("Collector", "Log 10 incidents", "#FBBF24", False, 0.4, "★"),
        ("Smooth Operator", "0 hard brakes", "#38BDF8", True, 1.0, "⛨"),
        ("Sentinel", "Auto-capture 5 events", "#F97316", False, 0.6, "⚡"),
        ("Night Guardian", "Night-time incident", "#818CF8", True, 1.0, "☾"),
        ("Speed Aware", "Record 60+ mph", "#EF4444", False, 0.73, "◎"),
    ]
    gx0 = 40
    gw = (W-80-12)//2
    gh = 250
    gy0 = ly + 230
    for i, (name, desc, col, on, prog, gly) in enumerate(badges):
        cxx = gx0 + (i % 2)*(gw+12)
        cyy = gy0 + (i//2)*(gh+12)
        rrect(d, [cxx, cyy, cxx+gw, cyy+gh], 16, fill="#1E293B",
              outline=col+"99" if on else "#22FFFFFF", width=2)
        # icon circle
        icx, icy = cxx+gw//2, cyy+58
        ic_bg = col+"2E" if on else "#33000000"
        d.ellipse([icx-38, icy-38, icx+38, icy+38], fill=hx(ic_bg))
        T(d, (icx, icy), gly if on else "🔒", "bold", 38, col if on else "#475569", "mm")
        T(d, (icx, cyy+128), name, "bold", 21, "#FFFFFF" if on else "#94A3B8", "mm")
        T(d, (icx, cyy+158), desc, "reg", 16, "#64748B", "mm")
        if on:
            T(d, (icx, cyy+200), "✓ UNLOCKED", "bold", 19, col, "mm")
        else:
            pbx0, pbx1 = cxx+24, cxx+gw-24
            rrect(d, [pbx0, cyy+196, pbx1, cyy+210], 7, fill="#0F172A")
            rrect(d, [pbx0, cyy+196, pbx0+int((pbx1-pbx0)*prog), cyy+210], 7, fill=col)
    save(img, "10_achievements.png")


if __name__ == "__main__":
    render_drive_score()
    render_sos(armed=True)
    render_timeline()
    render_dashboard_protools()
    render_parking_sentry()
    render_telemetry_graph()
    render_trip_history()
    render_collision()
    render_hud()
    render_achievements()
    print("done ->", os.path.abspath(OUT))

    # ── Timestamped screenshot archive ──────────────────────────────────────
    import shutil
    from datetime import datetime

    run_label = datetime.now().strftime("%Y-%m-%d_%H-%M")
    root_dir = os.path.join(os.path.dirname(__file__), "..", "screenshots")
    archive_dir = os.path.join(root_dir, run_label)
    os.makedirs(archive_dir, exist_ok=True)

    pngs = sorted(f for f in os.listdir(OUT) if f.endswith(".png"))
    for name in pngs:
        shutil.copy(os.path.join(OUT, name), os.path.join(archive_dir, name))

    # Update (or recreate) the "latest" symlink
    latest_link = os.path.join(root_dir, "latest")
    if os.path.islink(latest_link):
        os.remove(latest_link)
    os.symlink(os.path.abspath(archive_dir), latest_link)

    # Generate index.html gallery
    rows = "\n".join(
        f'  <figure style="display:inline-block;margin:8px;text-align:center">'
        f'<img src="{name}" style="height:320px;border:1px solid #334"/>'
        f'<figcaption style="color:#94a3b8;font-size:11px;margin-top:4px">{name}</figcaption>'
        f'</figure>'
        for name in pngs
    )
    html = f"""<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><title>QA Screenshots — {run_label}</title>
<style>body{{background:#0f172a;font-family:sans-serif;padding:16px}}
h1{{color:#e2e8f0;font-size:18px}}p{{color:#64748b;font-size:12px}}</style>
</head>
<body>
<h1>Good Drivers Defender — QA Screenshots</h1>
<p>Run: {run_label} &nbsp;|&nbsp; {len(pngs)} screens</p>
{rows}
</body></html>"""
    with open(os.path.join(archive_dir, "index.html"), "w") as fh:
        fh.write(html)

    print(f"Screenshots archived → {os.path.abspath(archive_dir)}")
    print(f"Gallery             → {os.path.abspath(os.path.join(archive_dir, 'index.html'))}")
