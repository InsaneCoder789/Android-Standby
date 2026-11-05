# Android Standby

A native Android bedside display with configurable clock, music, photo and information frames. Supports Android 9+ (min SDK 28); currently compiles with SDK 36 and targets SDK 35.

Choose up to three of 23 frames, swipe horizontally between frames and vertically between their variants. Physical tilt detection is independent of Android rotation lock. A screensaver host and an optional foreground Instant mode share display control. Media, calls, weather and automatic activation depend on granted permissions and platform behavior.

## Build

Use Gradle 8.13 with the Android SDK configured through `ANDROID_HOME` or an ignored `local.properties`. Tasks: `testDebugUnitTest`, `assembleDebug`, `lintDebug`.

## Evidence and limitations

The previous local layout pass passed 146 JVM tests and built a debug APK. Lint exposed API-29 calls in the debug benchmark despite min SDK 28. Recent changes require fresh build/device validation. This is a custom display, not a replacement for Samsung's low-power AOD. Preview tools may reset app preferences and must run only on disposable test installs.

## Repository history

This repository was initialized from an existing uncommitted workspace on 7 November 2026. The initial import is split into logical commits with explicitly reconstructed author/committer dates spanning 5–7 November 2026, at the owner's request. Those dates do not establish when individual features were implemented or tested. Subsequent iteration commits describe actual new changes.

Device screenshots and generated raster designs remain local. Credentials, SDK configuration and signing keys are excluded.
