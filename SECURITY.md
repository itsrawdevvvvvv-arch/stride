# Security Policy

## Supported versions

| Version | Supported |
| --- | --- |
| 1.0.x | ✅ |

## Reporting a vulnerability

Please **do not** open a public issue for a security problem.

Instead, use GitHub's private vulnerability reporting on this repository
(**Security → Report a vulnerability**), or contact the maintainers through the address on
the repository profile. Please include:

- a description of the issue and its impact
- steps to reproduce
- the affected version and Android version

We aim to acknowledge reports within a few days and will credit reporters in the release
notes unless you prefer otherwise.

## Scope

Stride stores all data locally and declares no network permission. The most relevant areas
are therefore:

- local data handling (Room database, DataStore, exported files)
- exported files written through the Storage Access Framework
- permission handling for location and activity recognition

## Design note

Because the app has no `INTERNET` permission, it is architecturally incapable of sending your
step or location data anywhere. Any change that adds network access would be a significant
change and must be discussed in an issue first.
