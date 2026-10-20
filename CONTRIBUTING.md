# Contributing to Stride

Thanks for wanting to help. Stride is a small, focused app and contributions of any size are
welcome — bug reports, docs fixes, and features alike.

## Getting set up

1. Fork the repository and clone your fork.
2. Open the folder in **Android Studio** (Ladybug or newer) with **JDK 17**.
3. Let Gradle sync, then run the `app` configuration on a device or emulator.

```bash
./gradlew testDebugUnitTest   # run the unit tests
./gradlew lintDebug           # run Android Lint
./gradlew assembleDebug       # build a debug APK
```

> Step counting needs a real device with a hardware step counter. The emulator has none, so
> use the emulator for UI work and a phone for tracking work.

## Before you open a pull request

- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintDebug` reports no new errors
- [ ] New pure logic in `domain/` comes with unit tests
- [ ] Public functions have a short KDoc comment explaining *why*, not *what*
- [ ] The PR description says what changed and why

## Code style

- Kotlin official style (4-space indent, trailing commas).
- Compose screens stay dumb: they render state and emit events. Logic belongs in
  `StrideViewModel` or, better, in a pure function under `domain/`.
- Prefer adding to `domain/` over adding to a screen — pure code is testable code.
- No new dependencies without discussing it in an issue first.

## Commit messages

Conventional Commits are appreciated:

```
feat: pause and resume route recording
fix: re-baseline the step counter after a reboot
docs: explain the export formats
```

## Reporting bugs

Please use the bug report template and include:

- Android version and device model
- Whether the device has a hardware step counter
- Exact steps to reproduce
- What you expected and what happened

## Security

Please do not open a public issue for security problems — see [`SECURITY.md`](SECURITY.md).
