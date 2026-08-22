# Namma Metro Tracker v8

An offline-first Bengaluru Namma Metro commuter app with a Namma Metro-inspired information architecture, custom palette, remote configuration and an **Estimated Live** train model.

## v8 Android experience

- Namma Metro-style illustrated/utility-card information architecture
- Brand palette: `#2FF2CA`, `#F2C072`, `#7884D9`, `#14A7CF`
- Custom Namma Metro Tracker train logo
- Clear Home / Estimated Live / Map / Info pages
- Purple, Green and Yellow line selection
- From/To station dropdowns constrained to the selected line
- Fare, journey duration, stops, headway and last-train countdown
- Service notice card
- Offline cache with remote configuration sync
- GPS permission groundwork for rider-location features
- Mathematical train-motion visualization

## Estimated Live model

The app deliberately labels modelled positions as **Estimated Live**. Train positions are not BMRCL GPS telemetry. The model is designed to consume refreshed GTFS schedule/geometry data and interpolate a continuous position between scheduled stops. A rider's own Android GPS can be used separately for location-aware commuter features once GTFS station geometry is available.

The current community BMRCL GTFS source is `Vonter/bmrcl-gtfs`. Its README documents that schedules originate from BMRCL timetable information and spatial data from OpenStreetMap, and that intermediate stop timings are synthesized/approximate. The source database is ODbL-1.0; keep attribution and database-license requirements with any redistributed derived dataset.

## Automatic GTFS refresh

`.github/workflows/gtfs-weekly-refresh.yml` runs weekly and can also be started manually. It downloads the current community BMRCL GTFS feed, normalizes it into `backend/data/gtfs_model.json`, and commits the refreshed model. The backend exposes `/api/network` and `/api/estimated-trains` for the normalized model.

## Remote configuration backend

The `backend/` folder contains the FastAPI admin service backed by Supabase/PostgreSQL in production (SQLite is retained for local development). Fares, timings, headways and service notices can be changed from `/admin` without rebuilding the APK.

## Build the APK without Android Studio

1. Open **Actions**.
2. Select **Build Namma Metro Tracker APK**.
3. Run workflow on `v8-ui-estimated-live`.
4. Download `Namma-Metro-Tracker-v8-debug-apk`.
5. Extract the ZIP and install `app-debug.apk`.

## Disclaimer

This independent app is not affiliated with or endorsed by BMRCL. Estimated train positions are model outputs, not live telemetry. Verify critical travel information with official BMRCL service information.

## License

MIT License for the application code. Third-party data remains subject to its own licenses and attribution requirements.
