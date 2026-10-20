# Privacy

**Stride collects nothing and sends nothing.**

## The short version

The app does not declare the `INTERNET` permission. It is therefore architecturally
incapable of transmitting your data anywhere — there is no code path that could, even in
principle, without a code change that would be obvious in review.

## What is stored, and where

| Data | Where it lives | Leaves the device? |
| --- | --- | --- |
| Daily step counts | Room database (`stride.db`) | Only if you export it |
| Hourly step buckets | Room database | Only if you export it |
| GPS routes and points | Room database | Only if you export it |
| Preferences (goal, units) | DataStore | No |
| Sensor baseline | DataStore | No |

All of it lives in the app's private storage. Nothing is backed up to a server by Stride.

## Permissions and what they're for

- **`ACTIVITY_RECOGNITION`** — required by Android 10+ to read the hardware step counter.
  Without it the app cannot count steps.
- **`ACCESS_FINE_LOCATION`** — used only while you are recording a route, and only to append
  points to that route. Location is never read in the background outside a route recording,
  and never for step counting.
- **`POST_NOTIFICATIONS`** — so the ongoing tracking notification can be shown.
- **`FOREGROUND_SERVICE` / `RECEIVE_BOOT_COMPLETED`** — so counting survives the app being
  closed and the phone restarting.

## Exports

When you export, you choose the destination through Android's Storage Access Framework
(Google Drive, Files, a USB stick, wherever). Stride writes the file you asked for and
nothing else. Exports are plain text (CSV, JSON, GPX) — inspect them yourself if you like.

## Removing your data

**Settings → Clear all data** wipes the step history and every route. Uninstalling the app
removes everything with it.

## Third parties

None. No analytics, no crash reporting, no ads, no SDK that phones home.

## Changes

If a future version ever needs network access, it will be called out prominently in the
release notes and in this document.
