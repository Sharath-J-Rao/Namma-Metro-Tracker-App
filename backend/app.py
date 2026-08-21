"""Small backend for the Namma Metro Tracker mobile app."""

import csv  # Reads GTFS CSV files from the static feed.
import io  # Converts downloaded text bytes into file-like objects.
import os  # Reads configuration from environment variables.
import zipfile  # Reads the GTFS ZIP without extracting it permanently.
from datetime import datetime, time  # Handles scheduled metro service times.
from pathlib import Path  # Provides safe filesystem paths.

import httpx  # Calls an authorized IUDX resource from the backend.
from dotenv import load_dotenv  # Loads local .env settings during development.
from fastapi import FastAPI, HTTPException, Query  # Provides the HTTP API.

try:  # Allows the backend to run even before the optional GTFS-RT package is installed.
    from google.transit import gtfs_realtime_pb2  # Decodes GTFS-Realtime protobuf feeds.
except ImportError:  # Handles an environment without the optional dependency.
    gtfs_realtime_pb2 = None  # Disables GTFS-RT parsing until the dependency is installed.

load_dotenv()  # Loads .env values into the process environment.

APP_DIR = Path(__file__).resolve().parent  # Finds the backend folder.
GTFS_ZIP = Path(os.getenv("GTFS_ZIP", APP_DIR / "data" / "namma_metro_gtfs.zip"))  # Locates the static GTFS ZIP.
IUDX_LATEST_URL = os.getenv("IUDX_LATEST_URL", "").strip()  # Reads the optional IUDX latest-data URL.
IUDX_TOKEN = os.getenv("IUDX_TOKEN", "").strip()  # Reads the optional backend-only IUDX token.
IUDX_GTFS_RT_URL = os.getenv("IUDX_GTFS_RT_URL", "").strip()  # Reads the optional GTFS-RT feed URL.

app = FastAPI(title="Namma Metro Tracker API", version="6.0.0")  # Creates the API application.

FALLBACK_LINES = {  # Provides a small offline fallback until the official GTFS ZIP is installed.
    "purple": {"name": "Purple Line", "color": "#6A1B9A", "stations": [  # Defines the Purple Line.
        "Whitefield", "Hopefarm", "Kadugodi Tree Park", "Pattandur Agrahara", "Sri Sathya Sai Hospital",  # Lists eastern Purple stations.
        "Nallurhalli", "Kundalahalli", "Seetharampalya", "Hoodi", "Garudacharpalya", "Singayyanapalya",  # Continues the Purple stations.
        "K.R. Pura", "Benniganahalli", "Baiyappanahalli", "Swami Vivekananda Road", "Indiranagar",  # Continues toward the city.
        "Halasuru", "Trinity", "MG Road", "Cubbon Park", "Vidhana Soudha", "Central College",  # Continues through central Bengaluru.
        "Majestic", "City Railway Station", "Magadi Road", "Hosahalli", "Vijayanagara", "Attiguppe",  # Continues westward.
        "Deepanjali Nagar", "Mysuru Road", "Nayandahalli", "RR Nagar", "Jnanabharathi", "Pattanagere",  # Continues toward Kengeri.
        "Kengeri Bus Terminal", "Kengeri", "Challaghatta"  # Ends the Purple Line.
    ]},
    "green": {"name": "Green Line", "color": "#2E7D32", "stations": [  # Defines the Green Line.
        "Madavara", "Chikkabidarakallu", "Manjunathanagara", "Nagasandra", "Dasarahalli", "Jalahalli",  # Lists northern Green stations.
        "Peenya Industry", "Peenya", "Goraguntepalya", "Yeshwanthpur", "Sandal Soap Factory", "Mahalakshmi",  # Continues south-east.
        "Rajajinagar", "Kuvempu Road", "Srirampura", "Sampige Road", "Majestic", "Chickpete",  # Continues through central Bengaluru.
        "KR Market", "National College", "Lalbagh", "South End Circle", "Jayanagara", "RV Road",  # Continues south.
        "Banashankari", "JP Nagar", "Yelachenahalli", "Konanakunte Cross", "Doddakallasandra", "Vajarahalli",  # Continues south.
        "Thalaghattapura", "Silk Institute"  # Ends the Green Line.
    ]},
    "yellow": {"name": "Yellow Line", "color": "#C38A00", "stations": [  # Defines the Yellow Line.
        "RV Road", "Ragigudda", "Jayadeva Hospital", "BTM Layout", "Central Silk Board", "Bommanahalli",  # Lists the northern Yellow stations.
        "Hongasandra", "Kudlu Gate", "Singasandra", "Hosa Road", "Beratena Agrahara", "Electronic City",  # Continues south-east.
        "Infosys Agrahara", "Huskur Road", "Hebbagodi", "Bommasandra"  # Ends the Yellow Line.
    ]},
}


def _read_gtfs_file(name: str) -> list[dict[str, str]]:  # Reads one CSV file from the configured GTFS ZIP.
    if not GTFS_ZIP.exists():  # Checks whether an official GTFS ZIP is installed.
        return []  # Returns an empty list when no feed is configured.
    with zipfile.ZipFile(GTFS_ZIP, "r") as archive:  # Opens the GTFS ZIP safely.
        if name not in archive.namelist():  # Checks whether the requested GTFS file exists.
            return []  # Returns an empty list when the file is missing.
        raw = archive.read(name)  # Reads the GTFS CSV bytes.
    return list(csv.DictReader(io.TextIOWrapper(io.BytesIO(raw), encoding="utf-8-sig")))  # Parses UTF-8 CSV rows.


def _static_network() -> dict:  # Builds the API's station model from official GTFS or fallback data.
    routes = _read_gtfs_file("routes.txt")  # Loads GTFS routes.
    stops = _read_gtfs_file("stops.txt")  # Loads GTFS stops.
    trips = _read_gtfs_file("trips.txt")  # Loads GTFS trips.
    stop_times = _read_gtfs_file("stop_times.txt")  # Loads GTFS stop sequences and times.
    if not routes or not stops:  # Falls back when the official GTFS feed is not installed yet.
        return {"source": "fallback", "lines": FALLBACK_LINES}  # Returns the embedded prototype network.
    route_name = {row.get("route_id", ""): row.get("route_short_name") or row.get("route_long_name") or row.get("route_id", "") for row in routes}  # Maps route IDs to display names.
    stop_name = {row.get("stop_id", ""): row.get("stop_name", "") for row in stops}  # Maps stop IDs to names.
    route_stations: dict[str, list[str]] = {}  # Creates an ordered station list per route.
    trip_route = {row.get("trip_id", ""): row.get("route_id", "") for row in trips}  # Maps trip IDs to route IDs.
    for row in sorted(stop_times, key=lambda item: (item.get("trip_id", ""), int(item.get("stop_sequence", "0") or 0))):  # Walks each trip's ordered stops.
        rid = trip_route.get(row.get("trip_id", ""), "")  # Finds the route behind this trip.
        if not rid:  # Skips orphaned stop-time records.
            continue  # Moves to the next row.
        station = stop_name.get(row.get("stop_id", ""), "")  # Resolves the human-readable station name.
        if not station:  # Skips rows without station names.
            continue  # Moves to the next row.
        route_stations.setdefault(rid, [])  # Creates the route list on first use.
        if station not in route_stations[rid]:  # Avoids repeated station names within one route.
            route_stations[rid].append(station)  # Adds the station in sequence order.
    lines = {}  # Creates the normalized API line dictionary.
    for rid, stations in route_stations.items():  # Converts each GTFS route to the app model.
        display = route_name.get(rid, rid)  # Gets the route display name.
        key = display.lower().replace(" line", "").strip()  # Produces a stable client key.
        color = {"purple": "#6A1B9A", "green": "#2E7D32", "yellow": "#C38A00"}.get(key, "#1565C0")  # Uses familiar metro line colors.
        lines[key] = {"name": display if display.lower().endswith("line") else f"{display} Line", "color": color, "stations": stations}  # Stores the normalized route.
    return {"source": "gtfs", "lines": lines}  # Returns the official-feed network.


def _estimate_fare(stops_count: int) -> int:  # Produces a clearly labelled fallback fare estimate when distance data is unavailable.
    kilometres = max(0, stops_count) * 1.4  # Uses a conservative placeholder average station spacing.
    slabs = [(2, 11), (4, 21), (6, 32), (8, 42), (10, 53), (15, 63), (20, 74), (25, 84), (30, 90), (999, 95)]  # Represents the 2026 fare zones.
    for limit, fare in slabs:  # Finds the matching zone.
        if kilometres <= limit:  # Checks whether the estimated trip length fits this zone.
            return fare  # Returns the estimated token/QR fare.
    return 95  # Caps the fallback estimate at the published maximum.


def _minutes_to_service_close() -> int | None:  # Calculates minutes until the currently configured last-service time.
    now = datetime.now()  # Reads current local backend time.
    close = {"purple": time(23, 5), "green": time(23, 5), "yellow": time(23, 55)}  # Uses current scheduled closing guidance.
    return None  # The selected line is supplied by the endpoint below rather than assumed globally.


def _live_iudx_payload() -> dict:  # Retrieves optional latest IUDX data through the backend only.
    if not IUDX_LATEST_URL or not IUDX_TOKEN:  # Checks whether secure IUDX configuration exists.
        return {"enabled": False, "data": None}  # Reports that live mode is not configured.
    headers = {"Authorization": f"Bearer {IUDX_TOKEN}"}  # Builds the authenticated request header.
    try:  # Prevents a live-data outage from breaking the rest of the app.
        response = httpx.get(IUDX_LATEST_URL, headers=headers, timeout=8.0)  # Requests the latest authorized resource snapshot.
        response.raise_for_status()  # Converts HTTP failures into Python exceptions.
        return {"enabled": True, "data": response.json()}  # Returns the JSON payload to the normalization layer.
    except Exception as exc:  # Handles network, authentication and parsing failures.
        return {"enabled": False, "data": None, "error": str(exc)}  # Keeps the API available with a live-data warning.


def _live_gtfs_rt() -> dict:  # Retrieves and decodes an optional GTFS-Realtime feed.
    if not IUDX_GTFS_RT_URL or not IUDX_TOKEN or gtfs_realtime_pb2 is None:  # Checks whether GTFS-RT is fully configured.
        return {"enabled": False, "vehicles": [], "trip_updates": []}  # Returns an empty live state when unavailable.
    try:  # Keeps live feed failures isolated from static data.
        headers = {"Authorization": f"Bearer {IUDX_TOKEN}"}  # Builds the authenticated feed request.
        response = httpx.get(IUDX_GTFS_RT_URL, headers=headers, timeout=8.0)  # Downloads the protobuf feed.
        response.raise_for_status()  # Raises an error for HTTP failure responses.
        feed = gtfs_realtime_pb2.FeedMessage()  # Creates the protobuf feed object.
        feed.ParseFromString(response.content)  # Decodes the GTFS-Realtime bytes.
        vehicles = []  # Collects normalized vehicle positions.
        trip_updates = []  # Collects normalized trip updates.
        for entity in feed.entity:  # Walks through every GTFS-RT entity.
            if entity.HasField("vehicle"):  # Detects vehicle-position records.
                vehicle = entity.vehicle  # Gets the vehicle descriptor.
                position = vehicle.position  # Gets the GPS position object.
                vehicles.append({"id": vehicle.vehicle.id, "trip_id": vehicle.trip.trip_id, "lat": position.latitude, "lon": position.longitude, "timestamp": vehicle.timestamp})  # Stores a compact train position.
            if entity.HasField("trip_update"):  # Detects station arrival/departure updates.
                update = entity.trip_update  # Gets the trip-update object.
                trip_updates.append({"trip_id": update.trip.trip_id, "stop_updates": [{"stop_id": item.stop_id, "arrival": item.arrival.time if item.HasField("arrival") else None, "departure": item.departure.time if item.HasField("departure") else None} for item in update.stop_time_update]})  # Stores compact trip timings.
        return {"enabled": True, "vehicles": vehicles, "trip_updates": trip_updates}  # Returns normalized live data.
    except Exception as exc:  # Handles feed errors without breaking static service.
        return {"enabled": False, "vehicles": [], "trip_updates": [], "error": str(exc)}  # Returns a live-data error state.


@app.get("/api/health")  # Defines a simple health endpoint.
async def health() -> dict:  # Handles health checks from the Android app or monitoring tools.
    return {"ok": True, "gtfs_configured": GTFS_ZIP.exists(), "iudx_configured": bool(IUDX_TOKEN), "gtfs_rt_configured": bool(IUDX_GTFS_RT_URL)}  # Reports backend capabilities without exposing secrets.


@app.get("/api/lines")  # Defines the line-list endpoint.
async def lines() -> dict:  # Returns the current line and station model.
    return _static_network()  # Loads GTFS data when available and otherwise uses the offline fallback.


@app.get("/api/status")  # Defines the station/travel information endpoint.
async def status(line: str = Query(...), source: str = Query(...), destination: str = Query(...)) -> dict:  # Accepts a selected line and two stations.
    network = _static_network()  # Loads the current static network.
    selected = network["lines"].get(line.lower().replace(" line", "").strip())  # Finds the requested line.
    if not selected:  # Validates the selected line.
        raise HTTPException(status_code=404, detail="Metro line not found")  # Returns a clear client error.
    stations = selected["stations"]  # Gets the ordered stations for the selected line.
    if source not in stations or destination not in stations:  # Validates both requested stations.
        raise HTTPException(status_code=400, detail="Source or destination is not on the selected line")  # Rejects invalid combinations.
    start = stations.index(source)  # Finds the start station index.
    end = stations.index(destination)  # Finds the destination station index.
    stops_count = abs(end - start)  # Calculates the number of station-to-station hops.
    journey_minutes = max(2, round(stops_count * 2.4))  # Estimates trip duration when precise GTFS travel times are unavailable.
    fare = _estimate_fare(stops_count)  # Calculates the fallback distance-based fare estimate.
    close = {"purple": time(23, 5), "green": time(23, 5), "yellow": time(23, 55)}[line.lower().replace(" line", "").strip()]  # Gets the current line's scheduled end time.
    now = datetime.now()  # Reads current local backend time.
    today_close = now.replace(hour=close.hour, minute=close.minute, second=0, microsecond=0)  # Builds today's last-service timestamp.
    if today_close <= now:  # Handles the period after the last scheduled train.
        minutes_to_close = 0  # Shows that today's last service has already passed.
    else:  # Handles normal operating periods.
        minutes_to_close = round((today_close - now).total_seconds() / 60)  # Calculates the countdown in whole minutes.
    live = _live_gtfs_rt()  # Retrieves live vehicle/arrival data when configured.
    return {"source": network["source"], "live": live, "line": selected["name"], "line_color": selected["color"], "from": source, "to": destination, "stops": stops_count, "estimated_minutes": journey_minutes, "estimated_fare": fare, "fare_basis": "Distance-based 2026 slab estimate until GTFS fare/shapes data is loaded", "last_service": close.strftime("%H:%M"), "minutes_to_last_service": minutes_to_close}  # Returns the complete mobile status card.
