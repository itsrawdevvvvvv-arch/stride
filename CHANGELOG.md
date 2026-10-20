# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] — 2026-10-20

### Added

- Hardware step counting via `TYPE_STEP_COUNTER` in a foreground service, so steps keep
  counting with the app closed and the phone in a pocket.
- GPS route recording with live drawing of the route as you walk.
- Home-screen widget showing today's steps and goal progress.
- Calendar screen: completed days are filled, and tapping any date crosses it off by hand.
- Export: daily steps as CSV, everything as JSON, individual routes as GPX.
- Ongoing notification with a stop action for both step tracking and route recording.
- Daily goal, unit selection (km/mi) and notification toggle in Settings.
- Optional sample data, loaded on demand, so the charts have something to show.
- Unit tests for the pattern, geometry and export logic.
