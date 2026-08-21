# Namma Metro Tracker v7

An offline-first Android app for planning Bengaluru Namma Metro journeys, backed by an optional private configuration server.

## Android app

- Purple, Green and Yellow line dropdown
- From/To station dropdowns restricted to the selected line
- Approximate fare
- Approximate journey duration and stop count
- First/last service times
- Approximate next-train headway
- Countdown to last scheduled service
- Service notices
- Remote configuration sync
- Offline use with the last successful configuration

## Remote configuration backend

The `backend/` folder contains a small FastAPI + SQLite admin server.

You can edit fares, first/last train times, service headways and service notices from the `/admin` page. The server increments a configuration version each time you save. Android clients download `/api/config` and cache the result locally.

### Windows quick start

1. Run `backend/setup_windows.bat` once.
2. Set `ADMIN_PASSWORD` in `backend/.env`.
3. Run `backend/start_windows.bat`.
4. Open `http://YOUR-PC-IP:8000/admin` in a browser.
5. In the Android app choose **BACKEND** and enter `http://YOUR-PC-IP:8000`.

The phone and PC must be reachable on the same LAN when using an `http://` local URL.

### Cloud deployment

`backend/Dockerfile` and `render.yaml` are included for a hosted backend. Use HTTPS in production and set `ADMIN_USER` / `ADMIN_PASSWORD` as deployment secrets.

## IUDX

IUDX is deliberately optional in v7. The app does not require an IUDX account, API key or live feed. Live train positions can be added later without redesigning the Android client.

## Build the APK without Android Studio

Use GitHub Actions:

1. Open **Actions**.
2. Select **Build Namma Metro Tracker APK**.
3. Run workflow on `v7-admin-config`.
4. Download `Namma-Metro-Tracker-v7-debug-apk`.
5. Extract the ZIP and install `app-debug.apk`.

## Data disclaimer

Fares and timings are configurable estimates/schedules and can change. The app does not label scheduled headway estimates as live train positions.

## License

MIT License. See `LICENSE`.
