#!/usr/bin/env python3
"""
Generate Google Play Store marketing assets for Good Drivers Defender.
Outputs to play_assets/:
  - feature_graphic.png      (1024×500 — required)
  - screenshot_1080_*.png    (1080×1920 phone screenshots)
  - icon_512.png             (512×512 high-res icon)
"""
import math, os, subprocess
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.join(os.path.dirname(__file__), "..", "play_assets")
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

def hx(c):
    c = c.lstrip("#")
    if len(c) == 8:
        a,r,g,b = int(c[0:2],16),int(c[2:4],16),int(c[4:6],16),int(c[6:8],16)
        return (r,g,b,a)
    return (int(c[0:2],16),int(c[2:4],16),int(c[4:6],16))

def T(d, xy, s, kind, size, fill, anchor="lm"):
    d.text(xy, s, font=font(kind, size), fill=hx(fill) if isinstance(fill,str) else fill, anchor=anchor)

def rrect(d, box, r, fill=None, outline=None, width=2):
    d.rounded_rectangle(box, radius=r,
                         fill=hx(fill) if fill else None,
                         outline=hx(outline) if outline else None,
                         width=width)

# ─────────────────────────────────────────────────────────────────
# 1. FEATURE GRAPHIC  1024 × 500
# ─────────────────────────────────────────────────────────────────
def make_feature_graphic():
    W, H = 1024, 500
    img = Image.new("RGB", (W, H), hx("0F172A"))
    d = ImageDraw.Draw(img, "RGBA")

    # Dark radial glow centered left
    for r in range(350, 0, -10):
        alpha = int(60 * (1 - r/350))
        d.ellipse([120-r, 250-r, 120+r, 250+r], fill=(239, 68, 68, alpha))

    # App name
    T(d, (64, 160), "Good Drivers", "bold", 72, "FFFFFF", "lm")
    T(d, (64, 248), "Defender", "bold", 90, "EF4444", "lm")
    T(d, (64, 316), "Dashcam · Evidence · SOS · AR HUD", "reg", 26, "94A3B8", "lm")

    # Tagline badge
    rrect(d, [64, 358, 380, 400], 20, fill="EF4444")
    T(d, (222, 379), "FREE + PRO  |  v2.0", "bold", 22, "FFFFFF", "mm")

    # Right panel — mini phone mockup
    px, py, pw, ph = 660, 40, 260, 420
    rrect(d, [px, py, px+pw, py+ph], 24, fill="111827", outline="334155", width=3)
    # Screen content
    T(d, (px+pw//2, py+60),  "DASHBOARD", "bold", 16, "94A3B8", "mm")
    T(d, (px+pw//2, py+130), "72", "bold", 80, "FFFFFF", "mm")
    T(d, (px+pw//2, py+195), "MPH", "bold", 20, "94A3B8", "mm")

    # G-force badge
    rrect(d, [px+30, py+230, px+pw-30, py+278], 12, fill="1E293B", outline="EF4444", width=2)
    T(d, (px+pw//2, py+254), "● REC  1.2 G", "bold", 20, "EF4444", "mm")

    # Score
    rrect(d, [px+30, py+300, px+pw-30, py+360], 12, fill="1E293B", outline="22C55E", width=2)
    T(d, (px+pw//2, py+316), "DRIVE SCORE", "bold", 14, "22C55E", "mm")
    T(d, (px+pw//2, py+342), "94  A+", "bold", 26, "FFFFFF", "mm")

    img.save(os.path.join(OUT, "feature_graphic.png"))
    print("  ✓ feature_graphic.png  (1024×500)")


# ─────────────────────────────────────────────────────────────────
# 2. 512×512 HIGH-RES ICON
# ─────────────────────────────────────────────────────────────────
def make_icon():
    S = 512
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")

    # Background circle
    d.ellipse([0, 0, S, S], fill=hx("0F172A"))
    # Red outer ring
    d.ellipse([8, 8, S-8, S-8], outline=hx("EF4444"), width=12)
    # Inner shield body
    d.ellipse([60, 60, S-60, S-60], fill=hx("1E293B"))
    # "D" letter
    T(d, (S//2, S//2+8), "D", "bold", 240, "EF4444", "mm")
    # Small shield dot
    d.ellipse([S//2-16, S//2+88, S//2+16, S//2+120], fill=hx("FFFFFF"))

    img.save(os.path.join(OUT, "icon_512.png"))
    print("  ✓ icon_512.png  (512×512)")


# ─────────────────────────────────────────────────────────────────
# SHARED helpers for phone screenshots  1080×1920
# ─────────────────────────────────────────────────────────────────
PW, PH = 1080, 1920
PSB = 60  # status bar

def new_phone(bg="0F172A"):
    img = Image.new("RGB", (PW, PH), hx(bg))
    d = ImageDraw.Draw(img, "RGBA")
    d.rectangle([0, 0, PW, PSB], fill=(0, 0, 0, 100))
    T(d, (34, PSB//2), "9:41", "bold", 32, "FFFFFF", "lm")
    T(d, (PW-34, PSB//2), "5G  ▮▮▮▮  84%", "reg", 26, (255,255,255,200), "rm")
    return img, d

def phone_title(d, y, text, sub=None):
    T(d, (PW//2, y), text, "bold", 52, "FFFFFF", "mm")
    if sub:
        T(d, (PW//2, y+64), sub, "reg", 28, "94A3B8", "mm")

def pcard(d, box, fill="1E293B", border="334155", r=24):
    rrect(d, box, r, fill=fill, outline=border, width=2)

def save(img, name):
    path = os.path.join(OUT, name)
    img.save(path)
    print(f"  ✓ {name}")


# ─────────────────────────────────────────────────────────────────
# SCREENSHOT 1 — Dashboard
# ─────────────────────────────────────────────────────────────────
def ss_dashboard():
    img, d = new_phone()
    phone_title(d, PSB+90, "DASHBOARD", "Good Drivers Defender")

    # Speed circle
    cx, cy, r = PW//2, 700, 220
    d.ellipse([cx-r, cy-r, cx+r, cy+r], outline=hx("EF4444"), width=12)
    T(d, (cx, cy-30), "72", "bold", 160, "FFFFFF", "mm")
    T(d, (cx, cy+80), "MPH", "bold", 40, "94A3B8", "mm")
    T(d, (cx, cy+140), "ZONE  55 MPH", "reg", 28, "EF4444", "mm")

    # Stat row
    stats = [("1.2G", "G-Force"), ("● REC", "Active"), ("GPS", "Lock")]
    for i,(val,lbl) in enumerate(stats):
        x = 90 + i*320
        pcard(d, [x, 970, x+280, 1090])
        T(d, (x+140, 1014), val, "bold", 32, "FFFFFF", "mm")
        T(d, (x+140, 1058), lbl, "reg", 22, "94A3B8", "mm")

    # PRO TOOLS rows
    rows = [
        [("SCORE","22C55E"), ("SOS","EF4444"), ("TIMELINE","38BDF8")],
        [("SENTRY","818CF8"), ("GRAPH","06B6D4"), ("TRIPS","A855F7")],
        [("IMPACT","EF4444"), ("AR HUD","06B6D4"), ("BADGES","FBBF24")],
    ]
    for ri, row in enumerate(rows):
        y = 1150 + ri * 200
        for ci, (label, color) in enumerate(row):
            x = 40 + ci * 340
            pcard(d, [x, y, x+310, y+160], border=color)
            T(d, (x+155, y+80), label, "bold", 34, color, "mm")

    save(img, "screenshot_1080_01_dashboard.png")

# ─────────────────────────────────────────────────────────────────
# SCREENSHOT 2 — Drive Score
# ─────────────────────────────────────────────────────────────────
def ss_drive_score():
    img, d = new_phone()
    phone_title(d, PSB+90, "DRIVE SCORE")

    # Arc gauge
    cx, cy = PW//2, 800
    for thick in range(22, 0, -2):
        d.arc([cx-260, cy-260, cx+260, cy+260], 150, 30, fill=hx("1E293B"), width=thick)
    d.arc([cx-260, cy-260, cx+260, cy+260], 150, 150+int(0.94*240), fill=hx("22C55E"), width=20)

    T(d, (cx, cy-30), "94", "bold", 180, "22C55E", "mm")
    T(d, (cx, cy+90), "A+", "bold", 80, "FFFFFF", "mm")
    T(d, (cx, cy+178), "Excellent Driver", "reg", 34, "94A3B8", "mm")

    # Stat cards
    cards = [
        ("12", "Trips", "38BDF8"), ("0", "Hard Brakes", "22C55E"),
        ("47", "MPH avg", "FBBF24"), ("3", "XP Events", "A855F7"),
    ]
    for i, (val, lbl, col) in enumerate(cards):
        x = 40 + (i%2)*530
        y = 1120 + (i//2)*200
        pcard(d, [x, y, x+490, y+160], border=col)
        T(d, (x+245, y+60), val, "bold", 52, col, "mm")
        T(d, (x+245, y+118), lbl, "reg", 26, "94A3B8", "mm")

    save(img, "screenshot_1080_02_drive_score.png")

# ─────────────────────────────────────────────────────────────────
# SCREENSHOT 3 — Emergency SOS
# ─────────────────────────────────────────────────────────────────
def ss_sos():
    img, d = new_phone("1A0000")
    phone_title(d, PSB+90, "EMERGENCY SOS", "Tap to arm · 3-2-1 countdown")

    cx, cy = PW//2, 800
    # Pulsing rings
    for r in [320, 280, 240]:
        a = int(60 * (320-r) / 320)
        d.ellipse([cx-r, cy-r, cx+r, cy+r], outline=(239,68,68,max(40,a)), width=4)
    d.ellipse([cx-200, cy-200, cx+200, cy+200], fill=hx("7F1D1D"))
    T(d, (cx, cy-16), "SOS", "bold", 100, "EF4444", "mm")
    T(d, (cx, cy+80), "ARMED", "bold", 36, "FFFFFF", "mm")

    # Countdown
    T(d, (cx, cy+220), "Sending alert in  3", "bold", 48, "FBBF24", "mm")

    # Location card
    pcard(d, [60, 1080, PW-60, 1220], border="EF4444")
    T(d, (PW//2, 1130), "LAST KNOWN LOCATION", "bold", 26, "EF4444", "mm")
    T(d, (PW//2, 1170), "I-95 N, Fairfield County, CT", "reg", 30, "FFFFFF", "mm")
    T(d, (PW//2, 1205), "41.1408°N  73.2613°W  ·  62 MPH", "reg", 24, "94A3B8", "mm")

    # Buttons
    rrect(d, [60, 1280, 490, 1400], 20, fill="22C55E")
    T(d, (275, 1340), "I'M OK", "bold", 40, "FFFFFF", "mm")
    rrect(d, [590, 1280, PW-60, 1400], 20, fill="EF4444")
    T(d, (PW-275, 1340), "SEND HELP", "bold", 40, "FFFFFF", "mm")

    save(img, "screenshot_1080_03_sos.png")

# ─────────────────────────────────────────────────────────────────
# SCREENSHOT 4 — Parking Sentry
# ─────────────────────────────────────────────────────────────────
def ss_sentry():
    img, d = new_phone()
    phone_title(d, PSB+90, "PARKING SENTRY", "Guard mode active")

    cx, cy = PW//2, 760
    # Radar rings
    for r in [240, 180, 120, 70]:
        d.ellipse([cx-r, cy-r, cx+r, cy+r], outline=(99,102,241,100), width=2)
    # Sweep arc
    d.pieslice([cx-240, cy-240, cx+240, cy+240], -90, -30, fill=(99,102,241,70))
    # Center dot
    d.ellipse([cx-12, cy-12, cx+12, cy+12], fill=hx("818CF8"))
    T(d, (cx, cy+290), "ARMED", "bold", 40, "22C55E", "mm")

    # Impact log
    pcard(d, [60, 1120, PW-60, 1340], border="EF4444")
    T(d, (PW//2, 1160), "DETECTED IMPACT", "bold", 30, "EF4444", "mm")
    T(d, (120, 1210), "09:14:33  ·  2.4 G  — HIGH", "bold", 28, "FFFFFF")
    T(d, (120, 1255), "09:02:11  ·  1.6 G  — MED", "reg",  26, "94A3B8")
    T(d, (120, 1296), "08:47:58  ·  1.1 G  — LOW", "reg",  26, "94A3B8")

    # Sensitivity chips
    for i,(lbl,col) in enumerate([("LOW","22C55E"),("MED","FBBF24"),("HIGH","EF4444")]):
        x = 80 + i*310
        rrect(d, [x, 1400, x+280, 1470], 20, outline=col)
        T(d, (x+140, 1435), lbl, "bold", 28, col, "mm")

    save(img, "screenshot_1080_04_sentry.png")

# ─────────────────────────────────────────────────────────────────
# SCREENSHOT 5 — Achievements
# ─────────────────────────────────────────────────────────────────
def ss_achievements():
    img, d = new_phone()
    phone_title(d, PSB+90, "ACHIEVEMENTS")

    # Level card
    pcard(d, [40, 200, PW-40, 420], border="FBBF24")
    d.ellipse([72, 240, 176, 344], fill=hx("422006"))
    T(d, (124, 292), "7", "bold", 70, "FBBF24", "mm")
    T(d, (210, 268), "DRIVER LEVEL 7", "bold", 38, "FFFFFF")
    T(d, (210, 320), "4 / 6 badges  ·  3475 XP", "reg", 26, "94A3B8")
    # XP bar
    rrect(d, [72, 362, PW-72, 396], 10, fill="0F172A")
    rrect(d, [72, 362, 72+int(0.7*(PW-144)), 396], 10, fill="FBBF24")

    # Badge grid
    badges = [
        ("First Evidence", "22C55E", True),
        ("Collector",      "FBBF24", True),
        ("Smooth Op.",     "38BDF8", True),
        ("Sentinel",       "F97316", True),
        ("Night Guard",    "818CF8", False),
        ("Speed Aware",    "EF4444", False),
    ]
    for i, (name, col, unlocked) in enumerate(badges):
        cx2 = 40 + (i%2)*540 + 250
        cy2 = 500 + (i//2)*270 + 120
        bx, by = cx2-240, cy2-120
        border = col if unlocked else "334155"
        pcard(d, [bx, by, bx+480, by+240], border=border)
        oc = col if unlocked else "475569"
        d.ellipse([cx2-44, cy2-64, cx2+44, cy2+24], fill=(hx(col)[0],hx(col)[1],hx(col)[2],60 if unlocked else 30))
        T(d, (cx2, cy2-20), "★" if unlocked else "🔒", "bold", 40, oc, "mm")
        T(d, (cx2, cy2+36), name, "bold", 26, "FFFFFF" if unlocked else "94A3B8", "mm")
        if unlocked:
            T(d, (cx2, cy2+70), "✓ UNLOCKED", "bold", 22, col, "mm")

    save(img, "screenshot_1080_05_achievements.png")


# ─────────────────────────────────────────────────────────────────
# RUN ALL
# ─────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    print("Generating Google Play assets…")
    make_feature_graphic()
    make_icon()
    print("Generating phone screenshots (1080×1920)…")
    ss_dashboard()
    ss_drive_score()
    ss_sos()
    ss_sentry()
    ss_achievements()
    print(f"\nAll assets saved to: {os.path.abspath(OUT)}/")
    print("""
Upload checklist:
  play_assets/feature_graphic.png       → Play Console › Store listing › Feature graphic
  play_assets/icon_512.png              → Play Console › Store listing › App icon (512×512)
  play_assets/screenshot_1080_01_*.png  → Play Console › Store listing › Phone screenshots
  … (5 screenshots total)
""")
