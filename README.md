# Namma Metro Tracker 8.5.0

A Bengaluru metro companion focused on journey planning, network mapping and train movement estimates.

## Customer experience
- Home, Journey, Metro Map and Live Trains pages.
- Line, From and To station selection.
- Journey summary with fare, duration, stops and next-train estimate.
- Network map with highlighted routes and moving train markers.
- Offline-first operation with remotely refreshable configuration.

## Data
Schedule and network data can be refreshed without rebuilding the application. Train movement shown in the current release is an estimate based on available schedule/network data; it is not represented as BMRCL GPS telemetry.

## Google Play release
This release targets Android 16 (API level 36) and builds an Android App Bundle (`.aab`) for Google Play submission.

Google Play App Signing should be enabled in Play Console. The CI release bundle is intentionally unsigned so the upload key remains under the publisher's control.

## Build
GitHub Actions produces both:
- `Namma-Metro-Tracker-v8.5-debug-apk` for device testing.
- `Namma-Metro-Tracker-v8.5-release-aab` for Play Console submission.
