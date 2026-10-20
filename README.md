<div align="center">

# Stride

**An open-source step & route tracker for Android.**
Counts steps with the hardware sensor, draws your walks with GPS, and keeps everything on your device.

[![Android CI](https://github.com/itsrawdevvvvvv-arch/stride/actions/workflows/android.yml/badge.svg)](https://github.com/itsrawdevvvvvv-arch/stride/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-D4F36B.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-141414.svg)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg)](https://kotlinlang.org)

</div>

---

## What it is

Stride is a local-first fitness tracker. It has no account, no cloud and no ads — your step
history and GPS routes never leave the phone unless *you* export them.

- **Real step counting.** Uses Android's hardware `TYPE_STEP_COUNTER` sensor inside a
  foreground service, so counting continues with the phone in your pocket and the app
  closed. No GPS is required for counting, which keeps battery use low.
- **GPS route recording.** Records a route as a polyline and draws it — a "Strava-style"
  drawing of where you walked. Routes can be exported as GPX.
- **Home-screen widget.** Today's steps and goal progress, on your home screen.
- **Calendar with manual marks.** Every day you complete the goal is filled in; tap any
  date to cross it off by hand.
- **Export anything.** Daily steps as CSV, everything as JSON, individual routes as GPX.

## Screens

| Today | Routes | Calendar | Settings |
| --- | --- | --- | --- |
| Today's steps, goal ring, weekly bars | Record & browse GPS routes | Month grid with completed days | Goal, units, export, data |

> Screenshots live in [`docs/screenshots`](docs/screenshots). Add your own with
> `adb exec-out screencap -p > docs/screenshots/today.png`.

## Requirements

- Android **8.0 (API 26)** or newer
- A device with a hardware step-counter sensor for step counting
  (the app still records GPS routes without one)
- Android Studio Ladybug+ and **JDK 17** to build

## Build

```bash
git clone https://github.com/itsrawdevvvvvv-arch/stride.git
cd stride
./gradlew assembleDebug          # or open the folder in Android Studio and press Run
./gradlew testDebugUnitTest      # unit tests
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Permissions

| Permission | Why | When it's asked |
| --- | --- | --- |
| `ACTIVITY_RECOGNITION` | Read the step-counter sensor (required on Android 10+) | On first launch |
| `ACCESS_FINE_LOCATION` | Record GPS routes | When you start a route |
| `POST_NOTIFICATIONS` | Show the ongoing tracking notification | On first launch (Android 13+) |
| `FOREGROUND_SERVICE` (+`_HEALTH`, `_LOCATION`) | Keep counting while the app is closed | Declared only |
| `RECEIVE_BOOT_COMPLETED` | Resume counting after a restart | Declared only |

Denying location still leaves step counting fully working.

## How it works

```
StepTrackingService ──▶ StepSensorEngine ──▶ StepRepository ──▶ Room
RouteTrackingService ─▶ (GPS fixes)      ──▶ RouteRepository ─▶ Room
                                                     │
                                       StrideViewModel │ (Flow)
                                                     ▼
                                        Compose UI  +  Widget
```

- The step sensor reports *total steps since boot*. Stride persists the last raw reading and
  stores the difference; a reading lower than the stored one means a reboot, so the counter
  is re-baselined instead of recording a negative delta.
- Route points are buffered in memory for live drawing and flushed to the database in small
  batches, so a route survives the process being killed.
- Everything the UI shows is derived from Room through `StrideViewModel` — the screens never
  touch the sensors directly.

More detail in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Privacy

Stride has no network permission. It cannot upload anything, because it has no way to.
See [`docs/PRIVACY.md`](docs/PRIVACY.md).

## Export formats

| Format | Contents |
| --- | --- |
| CSV | `date, steps, goal, distance_m, marked` — one row per day |
| JSON | Every day, every route and every route point |
| GPX | A single route as a standard GPX 1.1 track |

## Project layout

```
app/src/main/java/com/epicmems/stride/
├── data/            Room entities, DAOs, repositories, DataStore settings
├── domain/          Pure logic: patterns, geo maths, export builders
├── track/           Step sensor engine, foreground services, notifications
├── widget/          Home-screen widget provider
├── ui/              Compose theme, components, screens, ViewModel
└── util/            Date & number helpers, optional sample data
```

## Contributing

Issues and pull requests are welcome — please read [`CONTRIBUTING.md`](CONTRIBUTING.md)
and the [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) first.

## Roadmap

- [ ] Pause/resume for route recording
- [ ] Live share of a route while walking
- [ ] Health Connect import/export
- [ ] Wear OS companion
- [ ] Route list filtering by date range

## Credits

- **Poppins** typeface — SIL Open Font License 1.1
- Built with Kotlin, Jetpack Compose, Room and Coroutines

## License

[MIT](LICENSE) © 2026 Epic Mems
