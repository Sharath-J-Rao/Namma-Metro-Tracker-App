# Namma Metro Tracker v3.0

A lightweight Android app for planning and tracking journeys on Bengaluru Namma Metro.

## Features

- Purple, Green and Yellow operational lines
- Station search
- Origin/destination route planning
- Interchange detection
- Stop count and journey-time estimate
- Fare estimate
- From/To swap
- Favourite starting station
- Station information
- Schematic metro network map
- Station-by-station trip tracker
- Share journey
- Google Maps shortcut
- Official BMRCL service-information shortcut
- Core route planning without an account

## Build without Android Studio

This repository includes a GitHub Actions workflow that builds the debug APK on a GitHub-hosted runner. No Android Studio, Android SDK, Gradle, or Java installation is required on your computer.

1. Open the **Actions** tab.
2. Select **Build Namma Metro Tracker APK**.
3. Choose **Run workflow**.
4. Wait for the workflow to finish.
5. Open the completed run.
6. Download the `Namma-Metro-Tracker-v3-debug-apk` artifact.
7. Extract it and install `app-debug.apk` on your Android device.

## Live data

This release does not fabricate live train positions or arrival predictions. A documented public BMRCL train-position API has not been established. The app provides official BMRCL information shortcuts instead.

## Disclaimer

This is an independent application and is not affiliated with or endorsed by Bangalore Metro Rail Corporation Limited (BMRCL). Timings, fares, routes and service conditions may change. Verify critical travel information with official BMRCL sources before travelling.

## License

MIT License. See `LICENSE`.
