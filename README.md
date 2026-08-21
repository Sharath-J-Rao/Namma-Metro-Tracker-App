# Namma Metro Tracker v3.0

Bengaluru Namma Metro Android app with offline route planning for the operational Purple, Green and Yellow lines.

## v3 additions
- Dependency-free schematic network map.
- Official BMRCL service/timing shortcut.
- Google Maps shortcut.
- Favourite-start shortcut.
- Station information dialog with line/interchange details.
- Route planning, transfer detection, fare estimate and station-by-station trip tracker.

## How to get the APK without Android Studio
You do **not** need Android Studio on your computer.

1. Create a GitHub repository in your browser.
2. Upload the contents of this folder to the repository (not the zip file inside the repo).
3. Open the repository's **Actions** tab.
4. Select **Build Namma Metro Tracker APK**.
5. Click **Run workflow**.
6. After it finishes, open the workflow run and download the artifact named `Namma-Metro-Tracker-v3-debug-apk`.
7. Extract the downloaded artifact and install `app-debug.apk` on your Android phone.

GitHub-hosted runners provide the build machine, and the included workflow installs the Android SDK packages and Gradle automatically.

## Important
This is an independent app, not an official BMRCL app. Live train-position data is not included because a documented public train-position API was not established. Official BMRCL service information can be opened from inside the app.
