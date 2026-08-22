# Namma Metro Tracker v8.2

A customer-facing Bengaluru metro companion focused on journeys, the network map and moving train views.

## Customer experience
- Home, Journey, Metro Map and Live Trains pages.
- Line, From and To dropdowns.
- Journey result with fare, duration, stops, next-train estimate and service timing.
- Full-network schematic-style map with highlighted journey and moving train markers.
- Brand palette: #2FF2CA, #F2C072, #7884D9, #14A7CF.
- Offline-first configuration with remote admin support.

## Data and estimation
The repository also contains the weekly BMRCL GTFS refresh pipeline and the estimated train-motion engine. Customer UI avoids exposing backend terminology.

## Build without Android Studio
Use GitHub Actions and download `Namma-Metro-Tracker-v8.2-debug-apk`.
