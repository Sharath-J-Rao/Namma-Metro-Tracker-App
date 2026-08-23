import json
import os
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from pathlib import Path

from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from admin import router as admin_router, require_admin
from store import init_db, load_config, save_config, history

MODEL_FILE = Path(__file__).with_name("data") / "gtfs_model.json"

@asynccontextmanager
async def lifespan(_app):
    init_db()
    yield

app = FastAPI(title="Namma Metro Admin API", version="8.0.0", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=[o.strip() for o in os.getenv("CORS_ORIGINS", "").split(",") if o.strip()] or ["https://localhost"], allow_credentials=False, allow_methods=["GET", "PUT"], allow_headers=["Authorization", "Content-Type"])
app.include_router(admin_router)

class ConfigPayload(BaseModel):
    version: int = 1
    updated_at: str = ""
    notice: str = ""
    fares: list[dict]
    lines: dict

@app.get("/")
async def root():
    return {"service": "Namma Metro Admin API", "version": "8.0.0", "admin": "/admin"}

@app.get("/api/health")
async def health():
    cfg = load_config()
    return {"ok": True, "config_version": cfg["version"], "gtfs_model": MODEL_FILE.exists()}

@app.get("/api/config")
async def public_config():
    response = JSONResponse(load_config())
    response.headers["Cache-Control"] = "no-store, max-age=0"
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Referrer-Policy"] = "no-referrer"
    return response

@app.get("/api/network")
async def public_network():
    if not MODEL_FILE.exists():
        return {"available": False, "message": "GTFS model has not been synced yet."}
    return json.loads(MODEL_FILE.read_text(encoding="utf-8"))

@app.get("/api/estimated-trains")
async def estimated_trains():
    now = datetime.now(timezone.utc)
    if not MODEL_FILE.exists():
        return {"source": "fallback-model", "updated_at": now.isoformat(), "trains": []}
    model = json.loads(MODEL_FILE.read_text(encoding="utf-8"))
    trains = []
    minute_of_day = now.hour * 60 + now.minute + now.second / 60.0
    for trip in model.get("trips", []):
        stops = trip.get("stops", [])
        if len(stops) < 2:
            continue
        def minutes(text):
            h, m, s = (text or "00:00:00").split(":")
            return int(h) * 60 + int(m) + int(s) / 60.0
        start = minutes(stops[0].get("departure"))
        end = minutes(stops[-1].get("arrival"))
        if start <= minute_of_day <= end and end > start:
            progress = (minute_of_day - start) / (end - start)
            index = min(len(stops) - 2, int(progress * (len(stops) - 1)))
            frac = progress * (len(stops) - 1) - index
            trains.append({"trip_id": trip.get("trip_id"), "line": trip.get("line"), "direction": trip.get("direction"), "progress": round(progress, 4), "from_stop": stops[index].get("stop_id"), "to_stop": stops[index + 1].get("stop_id"), "segment_progress": round(frac, 4)})
            if len(trains) >= 60:
                break
    return {"source": "gtfs-schedule-model", "updated_at": model.get("updated_at"), "trains": trains}

@app.get("/api/admin/history")
async def admin_history(_: bool = Depends(require_admin)):
    return {"items": history()}

@app.put("/api/admin/config")
async def update_config(payload: ConfigPayload, _: bool = Depends(require_admin)):
    return save_config(payload.model_dump(), changed_by="admin")
