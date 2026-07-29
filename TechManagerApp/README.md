# Tech Manager (فنيين) — Android App

Native Android (Java) port of the original single-page web app (`تقرقر القسم.html`).
Same features, same data, now a real Android Studio project instead of a browser page.

## What's included

- **Login** — department grid (كهرباء / نجارين / ألمنيوم / خياطين) + Admin, password-gated, EN/AR toggle.
- **Home** — live stats, technician roster grouped by department, per-technician status
  (Factory/On Site + temporary Vacation/Sick/Perm. Site/Absent with optional end date), WhatsApp
  quick-link, add-technician form (dept users add to their own department).
- **Report** — daily end-of-shift report: date, supervisor, section, contract no., an editable
  workers table (status, check-in/out, auto-calculated hours minus the configured break), an
  "available workers today" picker, free-text fields, and export as **PDF**, **Word (.doc)**, or
  **plain text**, each shareable straight to WhatsApp or any other app. Previous reports list with
  a WhatsApp-ready preview.
- **Evaluation** — monthly 1–10 scoring per technician with notes, running average, history log,
  and the same PDF/text export/share flow.
- **Settings (Admin only)** — per-department passwords, default shift times, break minutes, admin
  password change, add a technician to any department, and full report/evaluation management
  (view/delete) across the whole company.

Data is stored locally on the device (SharedPreferences/JSON), mirroring how the original used the
browser's localStorage — no backend/server required.

## Opening the project

1. Install **Android Studio** (Giraffe/Koala or newer).
2. `File → Open`, select the `TechManagerApp` folder.
3. Let Gradle sync (it will download the Android Gradle Plugin and dependencies the first time —
   an internet connection is required for this one-time sync).
4. Run on an emulator or a device running **Android 7.0 (API 24)** or newer.

No manual configuration is needed — `applicationId` is `com.factory.techmanager`, `minSdk` 24,
`targetSdk`/`compileSdk` 34.

## Login passwords (defaults, same as the original)

| Department | Password |
|---|---|
| Electricians (كهرباء) | 2 |
| Carpenters (نجارين) | 1 |
| Aluminum (ألمنيوم) | 3 |
| Tailors (خياطين) | 4 |
| Admin | 123 |

All of these can be changed from **Settings** once logged in as Admin.

## Notes on the port

- The web app's html2canvas+jsPDF PDF pipeline was replaced with native
  `android.graphics.pdf.PdfDocument` rendering — no extra libraries required.
- The "Word" export keeps the original's approach: an HTML file saved with a `.doc` extension,
  which Microsoft Word opens natively.
- Sharing uses Android's share sheet / WhatsApp intents via a `FileProvider`, instead of the
  browser's `navigator.share`.
- The dense HTML table for daily workers was adapted into stacked mobile-friendly cards (same
  fields, same data) for usability on phone screens.
