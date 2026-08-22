"""Download and normalize the community BMRCL GTFS feed for the admin backend."""
import csv
import io
import json
import os
import zipfile
from datetime import datetime, timezone
from pathlib import Path

import httpx

SOURCE_URL = os.getenv(
    "BMRCL_GTFS_URL",
    "https://raw.githubusercontent.com/Vonter/bmrcl-gtfs/main/gtfs/bmrcl.zip",
)
OUT_DIR = Path(__file__).with_name("data")
OUT_FILE = OUT_DIR / "gtfs_model.json"


def read_table(zf: zipfile.ZipFile, name: str):
    with zf.open(name) as raw:
        text = io.TextIOWrapper(raw, encoding="utf-8-sig", newline="")
        return list(csv.DictReader(text))


def norm_name(value: str) -> str:
    return " ".join((value or "").replace("Metro Station", "").split()).strip().lower()


def line_name(route: dict) -> str:
    text = f"{route.get('route_short_name','')} {route.get('route_long_name','')}".lower()
    if "purple" in text:
        return "Purple Line"
    if "green" in text:
        return "Green Line"
    if "yellow" in text:
        return "Yellow Line"
    return route.get("route_long_name") or route.get("route_short_name") or route.get("route_id", "Unknown")


def build_model(feed_bytes: bytes) -> dict:
    with zipfile.ZipFile(io.BytesIO(feed_bytes)) as zf:
        routes = read_table(zf, "routes.txt")
        stops = read_table(zf, "stops.txt")
        trips = read_table(zf, "trips.txt")
        stop_times = read_table(zf, "stop_times.txt")
        shapes = read_table(zf, "shapes.txt") if "shapes.txt" in zf.namelist() else []

    stop_map = {row.get("stop_id", ""): row for row in stops}
    route_line = {row.get("route_id", ""): line_name(row) for row in routes}
    trip_routes = {row.get("trip_id", ""): route_line.get(row.get("route_id", ""), "Unknown") for row in trips}

    ordered_stops = {}
    for row in stop_times:
        trip_id = row.get("trip_id", "")
        line = trip_routes.get(trip_id, "Unknown")
        if line not in {"Purple Line", "Green Line", "Yellow Line"}:
            continue
        try:
            seq = int(row.get("stop_sequence", "0"))
        except ValueError:
            continue
        ordered_stops.setdefault(line, {})[row.get("stop_id", "")] = min(seq, ordered_stops.setdefault(line, {}).get(row.get("stop_id", ""), 10**9))

    lines = {}
    for line, stop_orders in ordered_stops.items():
        ids = sorted(stop_orders, key=stop_orders.get)
        station_rows = []
        seen = set()
        for sid in ids:
            row = stop_map.get(sid)
            if not row:
                continue
            name = (row.get("stop_name") or "").strip()
            key = norm_name(name)
            if not name or key in seen:
                continue
            seen.add(key)
            try:
                lat = float(row.get("stop_lat", ""))
                lon = float(row.get("stop_lon", ""))
            except ValueError:
                continue
            station_rows.append({"id": sid, "name": name, "lat": lat, "lon": lon})
        lines[line] = {"stations": station_rows}

    shape_map = {}
    for row in shapes:
        sid = row.get("shape_id", "")
        try:
            seq = int(row.get("shape_pt_sequence", "0"))
            lat = float(row.get("shape_pt_lat", ""))
            lon = float(row.get("shape_pt_lon", ""))
        except ValueError:
            continue
        shape_map.setdefault(sid, []).append((seq, lat, lon))
    for sid, pts in shape_map.items():
        pts.sort(key=lambda item: item[0])

    trips_model = []
    for trip in trips:
        line = trip_routes.get(trip.get("trip_id", ""))
        if line not in lines:
            continue
        times = [r for r in stop_times if r.get("trip_id") == trip.get("trip_id")]
        times.sort(key=lambda r: int(r.get("stop_sequence", "0") or 0))
        compact = []
        for r in times:
            if r.get("arrival_time") and r.get("departure_time") and r.get("stop_id") in stop_map:
                compact.append({"stop_id": r.get("stop_id"), "arrival": r.get("arrival_time"), "departure": r.get("departure_time")})
        if len(compact) >= 2:
            trips_model.append({"trip_id": trip.get("trip_id"), "line": line, "direction": trip.get("trip_headsign", ""), "stops": compact})

    return {
        "source": SOURCE_URL,
        "updated_at": datetime.now(timezone.utc).isoformat(),
        "lines": lines,
        "trips": trips_model,
    }


def sync() -> dict:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    response = httpx.get(SOURCE_URL, timeout=60, follow_redirects=True)
    response.raise_for_status()
    model = build_model(response.content)
    OUT_FILE.write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8")
    return model


if __name__ == "__main__":
    model = sync()
    print(f"GTFS model updated: {len(model['trips'])} trips")
