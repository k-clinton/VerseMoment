# Scheduling and Android limitations

The default target is a five-minute wallpaper refresh, but Android does not guarantee exact execution timing. Background work may be deferred or skipped when the device is idle, battery optimizations are active, or manufacturer restrictions are in place.

## Native approach

The implementation uses Android's native scheduler as the default strategy and handles exact-alarm availability through `AlarmManager` checks. Where exact alarms are not available, the app falls back to an inexact scheduling window to remain compliant.

## Important caveats

- Exact timing is not guaranteed on most Android devices.
- Battery optimization can delay or suppress scheduled updates.
- Reboots require a safe re-registration flow if automation is enabled.
- The app should separate `last attempted` and `last successful` timestamps.
- The UI must communicate delay risk instead of presenting a fake countdown.

## Recommended on-device validation

- Verify the app can schedule background work.
- Test permission denial and battery restrictions.
- Confirm the lock screen updates while the device is idle.
- Validate reboot recovery and quiet-hours behavior.
