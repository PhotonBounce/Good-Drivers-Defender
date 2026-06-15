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

    # stat cards
    cards = [
        ("TOP SPEED", "71", "mph", "#38BDF8"),
        ("MAX FORCE", "2.13", "G", "#F97316"),
        ("HARD BRAKES", "1", "events", "#EF4444"),
        ("AUTO-SAVED", "3", "clips", "#22C55E"),
    ]
    gx0, gw, gh, gap = 40, (W-80-24)//2, 175, 24
    sy = cy + R + 70
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
    T(d, (40, y), "DASHBOARD  ·  bottom console (new row highlighted)", "bold", 24, "#94A3B8", "lm")
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

    # NEW pro tools row (highlighted)
    ry = yy + 230
    d.rounded_rectangle([28, ry-16, W-28, ry+128], radius=20, outline=hx("#FBBF24"), width=3)
    T(d, (44, ry-40), "▼ NEW: PRO TOOLS", "bold", 20, "#FBBF24", "lm")
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


if __name__ == "__main__":
    render_drive_score()
    render_sos(armed=True)
    render_timeline()
    render_dashboard_protools()
    print("done ->", os.path.abspath(OUT))
