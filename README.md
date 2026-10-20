# Stride

An open-source, **local-only step tracker for Android**. No GPS, no accounts, no network —
it reads your device's hardware step counter, keeps your history on the phone, and turns
that raw data into **patterns**: daily and weekly bars, an hour-of-day heatmap, a monthly
calendar heatmap, streaks, averages and a 30-day trend.

The whole UI follows a hand-drawn "ink on paper" design language — rounded ink-outlined
cards, chunky dot-matrix numerals, handwritten labels, a single slate-blue accent and
doodle line-art.

---

## What it does

- **Counts steps** using the hardware `TYPE_STEP_COUNTER` sensor (no GPS, no location).
- **Runs in the background** via a low-priority foreground service, so steps keep being
  recorded while the app is closed. A quiet ongoing notification shows today's count.
- **Stores everything on-device** in a Room (SQLite) database. Nothing leaves the phone.
- **Builds patterns** from your data:
  - Last-7-days bar chart with a dashed goal line (goal-hit days highlighted)
  - Hour-of-day heatmap for today
  - Monthly calendar heatmap
  - 30-day trend sparkline
  - 7-day / 30-day averages, best day, current streak, goal-hit rate
- **Daily goal** you can set with a slider.
- **Demo data** so the charts look meaningful on first launch (toggle it off any time in
  Settings — it is clearly marked and removable).

## Screens

| Tab | What's there |
| --- | --- |
| **Today** | Week strip, big date, today's steps hero card, month/year countdown bars, quick stats, doodle scene |
| **Patterns** | Weekly bars, hourly heatmap, monthly heatmap, 30-day trend, stat tiles |
| **History** | Scrolling list of the last 30 days with per-day progress bars |
| **Settings** | Daily goal, sensor status, demo-data controls, about |

---

## Building

Requires **Android Studio** (Ladybug or newer) with **JDK 17**, and Android SDK **34**.

```bash
git clone https://github.com/itsrawdevvvvvv-arch/stride.git
cd stride
./gradlew assembleDebug      # or just open the folder in Android Studio and press Run
```

The debug APK lands in `app/build/outputs/apk/debug/`.

- **minSdk 26** (Android 8.0) · **targetSdk 34**
- Kotlin 2.0 · Jetpack Compose (Material 3) · Room · DataStore · Coroutines/Flow

### Permissions

- `ACTIVITY_RECOGNITION` — required on Android 10+ to read the step sensor. Requested on
  first launch and from Settings.
- `POST_NOTIFICATIONS` — so the background notification can show (Android 13+).
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_HEALTH` / `RECEIVE_BOOT_COMPLETED`.

The app works fine if you deny the notification permission; only the ongoing status
notification is hidden.

---

## Architecture

```
com.epicmems.stride
├── data
│   ├── local        Room entities, DAO, database
│   ├── SettingsRepository   DataStore prefs + raw sensor baseline
│   └── StepRepository       single source of truth for step data
├── domain           DayStat / StrideUiState models + pure pattern maths
├── step             sensor engine, foreground service, boot receiver
├── ui
│   ├── theme        colours, typography (dot / hand / marker fonts), shapes
│   ├── components   InkCard, DotText, PillBar, WeekStrip, charts, doodles
│   ├── screens      Today, Patterns, History, Settings
│   ├── StrideViewModel.kt
│   └── StrideRoot.kt
└── util             date helpers, demo-data generator
```

Data flow: `StepCounterService` → `StepSensorEngine` → `StepRepository` → Room.
The UI observes Room through `StrideViewModel` and never talks to the sensor directly.

### How step counting works

`TYPE_STEP_COUNTER` reports the *total* steps since the device last booted. Stride persists
the last raw reading in DataStore and stores the **difference**. If a reading is lower than
the stored one, the device rebooted — the counter is simply re-baselined instead of
recording a negative delta. Because attribution uses the current date at write time, days
roll over correctly on their own.

---

## Fonts & licences

Bundled fonts are all under the **SIL Open Font License 1.1**:

- **DotGothic16** — dot-matrix numerals
- **Caveat** — handwritten accents
- **Patrick Hand** — marker labels

## Licence

MIT — see [LICENSE](LICENSE).
