# Physical device testing checklist

The VerseMoment project was validated for compile-time correctness in this environment. It was not tested on a live Android device here.

Use this checklist for hardware verification:

1. Install the app on a physical device running Android 8+.
2. Confirm applying a verse keeps the current lock-screen image visible behind a translucent verse card; if Android blocks wallpaper reading, choose an image and verify that it is used as the card background.
3. In Wallpaper Studio, choose a verse or tap **Pick another**, then apply it; confirm Home **Refresh** applies a different verse and that verse card sits below the clock area.
4. Review permission prompts, exact alarm usage, and battery optimization guidance.
5. Check that the app respects quiet hours and does not retrigger in loops.
6. Verify TimeMatch generation for 08:20 and other edge cases.
7. Validate reboot recovery when automation was enabled.
8. Confirm successful updates show an applied message and failures show the platform error instead of appearing to succeed.
9. Watch behavior under device idle, screen-off, and battery-saving modes.
