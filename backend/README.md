# Namma Metro Tracker v7 backend

This is the optional remote configuration server for the Android app. It does **not** require IUDX.

## What it does

- Stores fares, line timings, headways, stations and service notices in SQLite.
- Serves `/api/config` for the Android app.
- Provides a protected `/admin` web panel.
- Increments a configuration version every time you save changes.
- The Android app caches the last successful configuration and works offline.

## Run on Windows

```powershell
cd backend
py -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
uvicorn app:app --host 0.0.0.0 --port 8000
```

Open the admin panel:

```text
http://YOUR-PC-IP:8000/admin
```

The browser will ask for the username/password defined in `.env`.

## Environment

Set at least:

```text
ADMIN_USER=admin
ADMIN_PASSWORD=change-this-to-a-strong-password
```

Do not commit `.env` or real passwords to GitHub.

## Android

In the app, tap **BACKEND** and enter the server URL, for example:

```text
http://192.168.1.50:8000
```

The phone and server must be reachable on the same LAN. The APK will sync the latest configuration and keep the last successful configuration locally if the server is unavailable.

## Cloud deployment

Deploy the `backend` directory to any Python/FastAPI host. Set `ADMIN_USER` and `ADMIN_PASSWORD` as platform secrets/environment variables. Give the Android app the HTTPS URL of that service.

## Security

The Android app only reads `/api/config`. Administrative writes require HTTP Basic authentication. Use HTTPS in production. Do not expose the backend management endpoint directly to the public internet without a strong password and TLS.
