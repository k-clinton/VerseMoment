# Scheduling and Android limitations

The default target is a five-minute wallpaper refresh, but Android does not guarantee exact execution timing. Background work may be deferred or skipped when the device is idle, battery optimizations are active, or manufacturer restrictions are in place.

## Native approach

When automation is enabled, VerseMoment schedules a one-shot `AlarmManager` update on app launch and after each update. The receiver chooses a verse other than the most recently applied one, applies it, records success or failure, then schedules the next update. Scheduling is restored after reboot, app replacement, time changes, and exact-alarm permission changes. Disabling automation cancels the pending alarm.

If exact-alarm access is granted, VerseMoment requests `setExactAndAllowWhileIdle`. Otherwise it uses an inexact alarm. On Android 12 and later, exact-alarm access is controlled by the system's **Alarms & reminders** special app access; the Settings screen links to that control when access is missing.

## Important caveats

- A five-minute interval is a target, not a guarantee. Android can defer inexact alarms and rate-limit exact alarms while the device is idle; manufacturer battery management may delay them further.
- Battery optimization can delay or suppress scheduled updates.
- Reboots restore the next alarm only if automation is enabled.
- The app should separate `last attempted` and `last successful` timestamps.
- The UI must communicate delay risk instead of presenting a fake countdown.

## Recommended on-device validation

- Verify the app can schedule background work.
- Test permission denial and battery restrictions.
- Confirm the lock screen updates while the device is idle.
- Validate reboot recovery and quiet-hours behavior.
