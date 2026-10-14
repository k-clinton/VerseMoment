# Physical device testing checklist

The VerseMoment project was validated for compile-time correctness in this environment. It was not tested on a live Android device here.

Use this checklist for hardware verification:

1. Install the app on a physical device running Android 8+.
2. Use **Apply to lock screen** in Wallpaper Studio and **Refresh** on Home to confirm both actions set the lock-screen wallpaper using `WallpaperManager.FLAG_LOCK`.
3. Review permission prompts, exact alarm usage, and battery optimization guidance.
4. Check that the app respects quiet hours and does not retrigger in loops.
5. Verify TimeMatch generation for 08:20 and other edge cases.
6. Validate reboot recovery when automation was enabled.
7. Confirm successful updates show an applied message and failures show the platform error instead of appearing to succeed.
8. Watch behavior under device idle, screen-off, and battery-saving modes.
