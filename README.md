# Maintenance Reports (Android)

Native Android (Java) rewrite of the original single-file HTML maintenance-report app.
Same features: 6 sections, technician lists, GPS, camera/gallery photos, dual signature pads,
branded PDF export, WhatsApp send, JSON backup, AR/EN toggle, technician admin panel.

## Open & run
1. Install **Android Studio** (Hedgehog/Koala or newer).
2. `File > Open` and pick this `MaintenanceReports` folder.
3. Android Studio will offer to generate the Gradle wrapper automatically the
   first time — accept it (or `File > Sync Project with Gradle Files`).
4. Run on a device/emulator with **API 26+**.

No extra setup is needed — everything (Gson, AppCompat, Material, ExifInterface) is pulled
from Maven Central/Google's Maven via the dependencies already listed in `app/build.gradle`.

## What changed vs. the web version
- Photos and signatures are stored as JPEG/PNG files in the app's private storage
  instead of base64 strings, then embedded when the PDF is generated.
- Language switching uses Android's per-app language API (`AppCompatDelegate`), which
  also drives automatic RTL/LTR mirroring — no manual `dir="rtl"` toggling needed.
- "Download PDF/JSON" opens Android's share sheet (save to Files/Drive/etc.) instead of
  a browser download, since Android has no direct equivalent to a `<a download>` click.
- "Send PDF via WhatsApp" targets the WhatsApp app directly via an Android share Intent;
  if WhatsApp isn't installed it falls back to opening `wa.me` with the text summary,
  same as the web app's own fallback.
- An in-progress report draft lives in memory only (matches the original, which never
  persisted a half-filled form either) — only the recent-reports list, technician lists,
  office phone number and language choice survive an app restart.

## Project layout
- `model/` — Report, Fault, AcUnit, Technician, Section, etc.
- `storage/AppStorage.java` — SharedPreferences + Gson persistence (technicians, report
  serial counters, recent reports, office phone).
- `util/ImageUtil.java` — photo capture/compression pipeline (matches the original's
  max-width-900 / quality-60 compression).
- `util/PdfGenerator.java` — draws the branded, paginated PDF report.
- `ui/` — HomeActivity, ReportFormActivity, SuccessActivity, AdminActivity, SignatureView.

## Note
This was generated in an environment without the Android SDK, so it hasn't been
build-verified with Gradle/AGP — only statically checked (well-formed XML, balanced
braces, every `R.id` / `R.string` / `R.layout` / `R.drawable` / `R.color` / `R.style`
reference resolved against the actual resources). Please build once in Android Studio
and report back if it flags anything; it should build cleanly, but this is the one
step that couldn't be verified from here.

## Get a ready-made APK without installing Android Studio (GitHub Actions)

This project includes `.github/workflows/build-apk.yml`, which builds a debug APK
automatically on GitHub's servers whenever you push the code. Steps:

1. Create a free account at github.com if you don't have one.
2. Create a new **empty** repository (any name, Public or Private — Actions minutes
   are free for public repos and included free for private repos too).
3. Upload this whole project into that repository:
   - Easiest: on the repo page, click **Add file > Upload files**, then drag the
     entire extracted `MaintenanceReports` folder in. GitHub keeps the folder
     structure. Commit the upload.
   - Or, if you use git: `git init && git add . && git commit -m "init" && git branch -M main && git remote add origin <your-repo-url> && git push -u origin main`.
4. Go to the **Actions** tab of your repository. A workflow run named
   "Build Debug APK" should start automatically (if not, click **Run workflow**).
5. Wait for it to finish (first run: ~5-8 minutes while it downloads the Android
   SDK; later runs are faster).
6. Once it shows a green checkmark, open that run, scroll to **Artifacts**, and
   download **app-debug-apk** — it's a zip containing `app-debug.apk`.
7. Send that APK to your phone (WhatsApp to yourself, email, cable, etc.), open
   it, and allow "install from unknown sources" if asked.

No Android Studio needed for this path — GitHub's servers do the building.
