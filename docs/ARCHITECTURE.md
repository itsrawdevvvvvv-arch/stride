# Architecture

Stride is a small single-module Android app. The point of the structure is that everything
interesting is a pure function, and everything Android-specific is a thin shell around it.

## Layers

```
ui/          Compose screens, theme, components, ViewModel
   │  observes StateFlow, emits events
   ▼
domain/      Pure Kotlin: Patterns, Geo, Export, models   ← unit tested
   ▲
   │  maps entities to models
data/        Repositories + Room entities/DAOs + DataStore
   ▲
   │  writes
track/       StepSensorEngine, foreground services, notifications
widget/      RemoteViews widget reading the same repositories
```

### `data/`

- **Room** holds four entities: `step_days`, `step_hours`, `routes`, `route_points`.
  `step_hours` exists so the hour-of-day heatmap is a cheap query rather than a recompute.
- **Repositories** are the only thing that touches a DAO. `StepRepository.addSteps` is the
  single write path for steps, and it is idempotent with respect to the day: it upserts the
  day row and the matching hour bucket.
- **DataStore** holds preferences and — importantly — `last_raw_counter`, the persisted
  baseline for the hardware step sensor.

### `domain/`

No Android imports. This is what the tests cover.

- `Patterns` turns a list of `StepDay` into windows, averages, streaks and goal-hit rates.
- `Geo` does haversine distance, path length, bounding boxes and the normalisation that
  lets a route be drawn inside any canvas while keeping its shape.
- `Export` builds CSV, JSON and GPX strings.

### `track/`

- `StepSensorEngine` owns the `TYPE_STEP_COUNTER` listener. The sensor reports *total steps
  since boot*, so the engine stores the difference against the persisted baseline. If a
  reading comes back lower than the baseline, the device rebooted — re-baseline rather than
  record a negative delta.
- `StepTrackingService` is a foreground service (`health` type) that keeps the engine alive
  and mirrors today's count into an ongoing notification. It is started on app launch and
  after a reboot.
- `RouteTrackingService` is a foreground service (`location` type) that records GPS fixes.
  Points are buffered in memory so the UI can draw the route live, and flushed to Room in
  batches of eight so a route survives the process being killed. The route row is created
  before any points so points always have a parent.
- `TrackingState` is a tiny in-memory holder of the live recording state, exposed as
  `MutableStateFlow`s the ViewModel combines.

### `ui/`

- `StrideViewModel` combines six flows (steps, hours, settings, routes, live recording,
  selected month) into one `StrideUiState`. Screens are stateless functions of that state.
- The theme is a dark surface ladder plus four pastel accents; `PastelCard`, `PillRow`,
  `PrimaryButton`, `StepRing`, `WeekBars`, `HourHeat` and `RouteCanvas` are the reusable
  pieces. `RouteCanvas` is what turns a list of coordinates into a drawing.

### `widget/`

`StepWidgetProvider` uses classic `RemoteViews` (no extra dependency, works on every
launcher). It reads today's row straight from Room. `WidgetUpdater` is called by the tracking
service about once a minute so the widget stays reasonably fresh.

## Data flow for one step

1. Hardware sensor fires → `StepSensorEngine.onRawReading(raw)`.
2. Engine reads `last_raw_counter`, computes the delta, persists the new baseline.
3. `StepRepository.addSteps(delta)` upserts the day row and the hour bucket.
4. Room emits to `observeAll()` / `observeHours()`.
5. `StrideViewModel` recomputes `StrideUiState`; Compose recomposes.
6. The tracking service also updates the notification and (periodically) the widget.

## Testing

`app/src/test` covers `Patterns`, `Geo` and `Export` — the parts where a subtle bug would be
invisible in the UI. `./gradlew testDebugUnitTest` runs them on the JVM with no device.
