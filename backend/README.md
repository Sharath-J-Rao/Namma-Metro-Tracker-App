# Namma Metro Tracker v6 backend

This backend is the secure bridge between the Android app and official/static transit data.

## Data flow

```text
Official GTFS ZIP ───────┐
                         ├──> FastAPI ───> Android app
Authorized IUDX / GTFS-RT ┘       │
                                  ├── static stations/routes
                                  ├── fare/journey calculations
                                  └── live vehicles/trip updates
```

IUDX requires consumer registration/authentication for secure resource access, so the IUDX token is kept on this server and is never shipped inside the APK. citeturn807592search3turn514859search5

## Run locally

```bash
python -m venv .venv
.venv\\Scripts\\activate
pip install -r requirements.txt
copy .env.example .env
uvicorn app:app --host 0.0.0.0 --port 8000
```

Linux/macOS activation:

```bash
source .venv/bin/activate
```

## Static GTFS

Place the authorized Namma Metro GTFS ZIP at:

```text
backend/data/namma_metro_gtfs.zip
```

The service reads `routes.txt`, `stops.txt`, `trips.txt`, and `stop_times.txt`. The embedded line data is retained only as a fallback so the APK can still be demonstrated without the GTFS feed.

IUDX documentation describes file access and API access after a resource has been discovered and an appropriate access token obtained. citeturn514859search3turn514859search5

## Live GTFS-Realtime

Set these backend-only variables in `.env` after the authorized IUDX resource and feed endpoint are known:

```text
IUDX_GTFS_RT_URL=...
IUDX_TOKEN=...
```

The backend decodes GTFS-Realtime vehicle positions and trip updates using `gtfs-realtime-bindings`.

## Android connection

The APK can run in offline fallback mode. To use the backend, set the API URL in the app's **Data source** control, for example:

```text
http://192.168.1.50:8000
```

Use the LAN IP of the computer/server running this backend when the phone and server are on the same network.
