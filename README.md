Namma Metro Tracker

A lightweight Android application for planning and tracking journeys on Bengaluru Namma Metro.

Current Version

v3.0.0

Supported operational lines
🟣 Purple Line
🟢 Green Line
🟡 Yellow Line

Upcoming metro corridors are not presented as operational services.

Features
Search Bengaluru Metro stations
Select origin and destination stations
Automatically determine the route
Detect interchange stations
Calculate number of stops
Estimate journey duration
Estimate fare
Swap origin and destination
Save a favourite starting station
View the Bengaluru Metro network
Track a journey station by station
Share journey details
Open station locations in Google Maps
Access BMRCL service information
Core route information works without requiring an account
Planned Features

Future releases may include:

Live train arrival information
Live service disruption/status information
GPS-based nearest station detection
More detailed station information
Station facilities and amenities
Proper interactive metro map
Saved favourite journeys
Trip history
Notifications for service disruptions
Offline-first route database
Improved fare and travel-time calculations

Live train positions and arrival predictions depend on the availability of a reliable public BMRCL data feed/API. The application does not fabricate live data when an official or reliable feed is unavailable.

Technology

The project is designed as an Android application using:

Kotlin
Android SDK
Gradle
Android Jetpack components
GitHub Actions for cloud builds
Building the APK

You do not need Android Studio if you use the included GitHub Actions workflow.

Option 1 — GitHub Actions
Create a GitHub repository.
Upload this project.
Keep the repository private during development.
Open the Actions tab.
Run the Android build workflow.
Wait for the workflow to complete.
Download the generated APK from the workflow artifacts.
Option 2 — Android Studio

If Android Studio is available later, open the project and run:

./gradlew assembleDebug

The generated debug APK will be placed under:

app/build/outputs/apk/debug/
Project Structure
NammaMetroTracker/
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           ├── res/
│           └── AndroidManifest.xml
├── .github/
│   └── workflows/
│       └── android-build.yml
├── gradle/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── README.md
Data

Metro station and route information is intended for journey-planning purposes.

Operational status, timings, fares, and other service information can change. Users should verify critical travel information through official BMRCL sources before travelling.

Official BMRCL website:

https://english.bmrc.co.in/

Disclaimer

Namma Metro Tracker is an independent application and is not affiliated with, operated by, or officially endorsed by Bangalore Metro Rail Corporation Limited (BMRCL) unless explicitly stated.

Metro timings, fares, routes, station information, and service status may change. The developer is not responsible for delays, missed trains, incorrect fare estimates, or other travel consequences resulting from the use of this application.

License

This project is released under the MIT License.

See LICENSE for the full license text.

Development

This project is currently under active development.

The priority is to provide a reliable, simple, and fast Namma Metro journey-planning experience while avoiding unsupported or fabricated live-data functionality.
