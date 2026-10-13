# VerseMoment

VerseMoment is a native Android app built with Kotlin, Jetpack Compose, Room, DataStore, and Hilt. The app is designed to refresh a user's lock-screen wallpaper with inspirational Bible verses, including TimeMatch selection (using the current clock time to suggest a reference) and Divine Discovery (theme-based verse selection).

## Features

- Lock-screen-only wallpaper updates
- Default 5-minute refresh interval
- TimeMatch and Divine Discovery modes
- Local offline scripture dataset and bundled translation metadata
- Favorites and wallpaper history
- Theme and preferences management
- Compose-based screens for home, discover, saved, history, settings, and studio
- Native lock-screen application flow via `WallpaperManager`

## Architecture

The project uses a simple MVVM-style structure:

- `app` module contains the Android app, Compose screens, DI setup, and application entry point
- `domain` models represent immutable scripture and mode information
- `data` layer provides Room persistence and DataStore-backed preferences
- `selection` contains deterministic time-based and curated selection logic
- `wallpaper` handles local bitmap generation and lock-screen application
- `scheduling` contains the scheduling strategy scaffold for alarm-based automation

## Requirements

- JDK 17
- Android SDK 34
- Gradle 8.7+
- Android Studio or command-line build tools

## Setup

1. Install or point Gradle to an Android SDK at `/home/kappy/.local/android-sdk` or your own local SDK directory.
2. Set `local.properties` with:

   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

3. Run:

   ```bash
   ./gradlew assembleDebug
   ```

## Running tests

```bash
./gradlew testDebugUnitTest
```

## Scripture and licensing

This MVP includes a legal public-domain sample translation (KJV) and a small offline dataset for selection, discovery, and packaging. The data is stored locally and never depends on a network API. Translation attribution is included in the app and in the model metadata so it can be surfaced in the UI and on the wallpaper when required.

## Android scheduling limitations

Android does not guarantee exact five-minute updates under all conditions. The app uses a policy-aware native scheduling approach with `AlarmManager` and respects Android's background execution limits. In real-world use, delays, battery optimization rules, and OEM restrictions may defer or skip updates. The app therefore communicates restrictions honestly and stores last-attempt and last-success timestamps separately.

## Physical device testing

This environment does not include a connected Android device or emulator, so wallpaper application, scheduling, and live lock-screen tests were not executed here. The project includes the necessary native hooks and UI flow for those tests on a physical device.

## Current implementation status

- Project scaffolding complete
- Android app compiles successfully in this environment
- Core domain, database, navigation, sample UI, and selection logic implemented
- Native wallpaper and scheduling logic scaffolded
- Full physical-device validation remains for a real Android handset
