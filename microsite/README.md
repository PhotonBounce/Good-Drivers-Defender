# Good Drivers Defender — presentation microsite

A self-contained, static "coming soon to Google Play" / portfolio site featuring an
interactive in-browser **emulator** of the app (live HTML/CSS/JS screens), parallax
eye-candy, feature/intelligence sections, screenshot gallery, pricing with the 7-day
VIP trial, FAQ and a notify CTA.

No build step. No backend. Pure static files.

## Files

```
index.html          Main page
styles.css          All styles
app.js              Emulator screen-switching, parallax, scroll progress, reveals
site.webmanifest    PWA manifest
robots.txt          SEO
sitemap.xml         SEO (canonical: https://photon-bounce.com/gooddriversdefender/)
404.html            Branded not-found page
.htaccess           Apache: DirectoryIndex + gzip/cache/security headers (safe; ignored elsewhere)
assets/
  icon.png          App icon (also used as favicon / apple-touch-icon)
  feature.png       OG / social share image
  shots/*.png       10 screenshot renders used in the gallery
GoodDriversDefender.apk   NOT included — see "Enable the APK download" below
```

## Deploy (for whoever uploads it)

1. Upload the **contents of this `microsite/` folder** (not the folder itself) into the
   web-served directory for the target URL — **preserving the `assets/` subfolder**.
   For `https://photon-bounce.com/gooddriversdefender/`, that is usually:
   `public_html/gooddriversdefender/`  (or `www/gooddriversdefender/`).
   - IMPORTANT: it must go under the site's **web docroot** (e.g. `public_html/`), not the
     FTP login home. Uploading to the FTP home alone will return 403/404 in the browser.
   - Include the dotfile **`.htaccess`** (some FTP clients hide dotfiles — enable "show
     hidden files"). It sets `DirectoryIndex index.html`, which prevents the common
     "403 Forbidden" you get when directory listing is disabled and no index is found.
2. Verify: open `https://photon-bounce.com/gooddriversdefender/` — the page title should
   be "Good Drivers Defender — AI Dashcam & Driving Coach" and the hero reads "Drive smarter."

## Enable the APK download

The hero has a **"Download beta APK"** button wired to `GoodDriversDefender.apk` (a plain
relative link, `index.html` search for `apkDownload`) — but the APK file itself is **not**
in this folder. It has to come from GitHub Actions, which this environment could not reach
directly (its network egress is HTTPS-only; GitHub's artifact storage and the Android build
toolchain are both on hosts outside that allowlist).

To finish it, whoever has normal internet + GitHub access should:
1. Open the latest green build:
   https://github.com/PhotonBounce/Good-Drivers-Defender/actions
   (pick the newest run of "Android CI" with a green check)
2. Scroll to **Artifacts** → download **`debug-apk`** → unzip it → you get `app-debug.apk`.
3. Rename it to **`GoodDriversDefender.apk`** and upload it into this same
   `gooddriversdefender/` folder on the server, next to `index.html`.
4. Reload the page — the button downloads it immediately, no other changes needed.

If the button is clicked before the file is uploaded, the browser will show a 404 for that
one file — the rest of the site is unaffected.

Note: `debug-apk` is a **debug build** (not signed for Play, "Unknown sources" warning on
install — expected for a pre-launch beta). For a production-signed APK/AAB instead, see
`docs/PLAY_SUBMISSION_STEPS.md` in the main repo (the signing-secrets step) — once that's
set up, CI's `release-aab` artifact is the Play-ready bundle.

## Notes / things to personalize

- The "Notify me" button links to `mailto:hello@photon-bounce.com` — change it in
  `index.html` (search for `mailto:`) to the real contact address.
- All external use is Google Fonts (loaded from a CDN with a system-font fallback) and the
  Google Play link placeholder. Replace the Play Store URL once the listing is live
  (search `play.google.com` is not present here; the CTA pills point to `#pricing`).
- Everything renders offline except the web fonts; if the host blocks the font CDN it
  gracefully falls back to system fonts.

## Privacy policy (for Google Play)

`privacy.html` (+ `style.css`) is bundled here so a single upload serves both the site and the
policy. After upload it is reachable at `<your-microsite-path>/privacy.html` — paste that exact,
working URL into Play Console → App content → Privacy policy. **Verify it loads (HTTP 200, not 403)
in a browser before submitting** — an unreachable URL is the most common cause of Google's
"Invalid Privacy policy" rejection.
