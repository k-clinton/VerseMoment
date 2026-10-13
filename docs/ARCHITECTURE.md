# Architecture

VerseMoment is intentionally simple and offline-first. The app is structured around a small domain model, local persistence layer, and a lightweight selection engine.

## Layers

- Domain: app-level data structures and selection enums
- Data: Room entities, DAOs, and preferences repository
- Selection: TimeMatch and Divine Discovery logic
- Wallpaper: local bitmap generation and lock-screen application
- Scheduling: native Android scheduling configuration with Android restrictions acknowledged

## Application flow

1. Start the app and load persisted settings.
2. Seed a minimal offline scripture dataset on first launch.
3. Query the current translation and selection preferences.
4. Resolve a verse using TimeMatch or Divine Discovery.
5. Render a wallpaper bitmap locally.
6. Apply it to the lock screen via `WallpaperManager`.
7. Record result and schedule the next update.

## Notes

This project is a strong foundation for an offline devotional wallpaper app. It is intentionally modular enough to later expand the scripture corpus, add more translations, and support more granular wallpaper presets without needing a backend.
