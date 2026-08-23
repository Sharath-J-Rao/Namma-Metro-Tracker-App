import json
import os
import re
import sqlite3
from datetime import datetime, timezone
from pathlib import Path

DATABASE_URL = os.getenv('DATABASE_URL', '').strip()
DATA_DIR = Path(os.getenv('METRO_DATA_DIR', str(Path(__file__).with_name('data')))
DATA_DIR.mkdir(parents=True, exist_ok=True)
SQLITE_PATH = DATA_DIR / 'metro_admin.db'
TIME_RE = re.compile(r'^([01]\d|2[0-3]):[0-5]\d$')

DEFAULT_CONFIG = {
    'version': 1,
    'updated_at': datetime.now(timezone.utc).isoformat(),
    'notice': '',
    'fares': [
        {'max_km': 2, 'fare': 11}, {'max_km': 4, 'fare': 21}, {'max_km': 6, 'fare': 32},
        {'max_km': 8, 'fare': 42}, {'max_km': 10, 'fare': 53}, {'max_km': 15, 'fare': 63},
        {'max_km': 20, 'fare': 74}, {'max_km': 25, 'fare': 84}, {'max_km': 30, 'fare': 90}, {'max_km': 999, 'fare': 95}
    ],
    'lines': {
        'purple': {'name': 'Purple Line', 'first_train': '05:00', 'last_train': '23:05', 'headway_min': 8, 'stations': ['Whitefield','Hopefarm','Kadugodi Tree Park','Pattandur Agrahara','Sri Sathya Sai Hospital','Nallurhalli','Kundalahalli','Seetharampalya','Hoodi','Garudacharpalya','Singayyanapalya','K.R. Pura','Benniganahalli','Baiyappanahalli','Swami Vivekananda Road','Indiranagar','Halasuru','Trinity','MG Road','Cubbon Park','Vidhana Soudha','Central College','Majestic','City Railway Station','Magadi Road','Hosahalli','Vijayanagara','Attiguppe','Deepanjali Nagar','Mysuru Road','Nayandahalli','RR Nagar','Jnanabharathi','Pattanagere','Kengeri Bus Terminal','Kengeri','Challaghatta']},
        'green': {'name': 'Green Line', 'first_train': '05:00', 'last_train': '23:05', 'headway_min': 8, 'stations': ['Madavara','Chikkabidarakallu','Manjunathanagara','Nagasandra','Dasarahalli','Jalahalli','Peenya Industry','Peenya','Goraguntepalya','Yeshwanthpur','Sandal Soap Factory','Mahalakshmi','Rajajinagar','Kuvempu Road','Srirampura','Sampige Road','Majestic','Chickpete','KR Market','National College','Lalbagh','South End Circle','Jayanagara','RV Road','Banashankari','JP Nagar','Yelachenahalli','Konanakunte Cross','Doddakallasandra','Vajarahalli','Thalaghattapura','Silk Institute']},
        'yellow': {'name': 'Yellow Line', 'first_train': '05:00', 'last_train': '23:00', 'headway_min': 7, 'stations': ['RV Road','Ragigudda','Jayadeva Hospital','BTM Layout','Central Silk Board','Bommanahalli','Hongasandra','Kudlu Gate','Singasandra','Hosa Road','Beratena Agrahara','Electronic City','Infosys Agrahara','Huskur Road','Hebbagodi','Bommasandra']}
    }
}


def _is_postgres():
    return DATABASE_URL.startswith(('postgres://', 'postgresql://'))


def _validate_time(value, field):
    if not isinstance(value, str) or not TIME_RE.fullmatch(value):
        raise ValueError(f'{field} must use HH:MM in 24-hour format')
    return value


def validate_config(config):
    if not isinstance(config, dict) or not isinstance(config.get('lines'), dict) or not isinstance(config.get('fares'), list):
        raise ValueError('Invalid configuration structure')
    if not config['lines']:
        raise ValueError('At least one metro line is required')
    if not config['fares']:
        raise ValueError('At least one fare slab is required')
    previous_km = 0.0
    previous_fare = -1
    for slab in config['fares']:
        try:
            max_km = float(slab['max_km'])
            fare = int(slab['fare'])
        except (KeyError, TypeError, ValueError) as exc:
            raise ValueError('Each fare slab needs numeric max_km and fare') from exc
        if max_km <= previous_km or max_km <= 0:
            raise ValueError('Fare distance limits must be strictly increasing and positive')
        if fare < 0 or fare < previous_fare:
            raise ValueError('Fare values must be non-negative and non-decreasing')
        previous_km, previous_fare = max_km, fare
    for key, line in config['lines'].items():
        if not isinstance(line, dict) or not line.get('name'):
            raise ValueError(f'Line {key} needs a name')
        stations = line.get('stations')
        if not isinstance(stations, list) or not stations:
            raise ValueError(f'{line.get("name", key)} needs stations')
        cleaned = [str(s).strip() for s in stations]
        if any(not s for s in cleaned) or len(cleaned) != len(set(s.casefold() for s in cleaned)):
            raise ValueError(f'{line["name"]} contains blank or duplicate stations')
        first = _validate_time(line.get('first_train'), f'{line["name"]} first_train')
        last = _validate_time(line.get('last_train'), f'{line["name"]} last_train')
        if int(first[:2]) * 60 + int(first[3:]) >= int(last[:2]) * 60 + int(last[3:]):
            raise ValueError(f'{line["name"]} last train must be later than first train')
        try:
            headway = int(line.get('headway_min'))
        except (TypeError, ValueError) as exc:
            raise ValueError(f'{line["name"]} headway must be a whole number') from exc
        if not 1 <= headway <= 120:
            raise ValueError(f'{line["name"]} headway must be between 1 and 120 minutes')
    return config


def _sqlite_connection():
    return sqlite3.connect(SQLITE_PATH, timeout=10)


def init_db():
    if _is_postgres():
        import psycopg
        with psycopg.connect(DATABASE_URL) as db:
            with db.cursor() as cur:
                cur.execute('CREATE SCHEMA IF NOT EXISTS private')
                cur.execute('CREATE TABLE IF NOT EXISTS private.metro_config (id SMALLINT PRIMARY KEY CHECK(id=1), version BIGINT NOT NULL, updated_at TIMESTAMPTZ NOT NULL, payload JSONB NOT NULL)')
                cur.execute('CREATE TABLE IF NOT EXISTS private.metro_config_history (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, version BIGINT NOT NULL, updated_at TIMESTAMPTZ NOT NULL, payload JSONB NOT NULL, changed_by TEXT NOT NULL DEFAULT \'system\')')
                cur.execute('SELECT 1 FROM private.metro_config WHERE id=1')
                if cur.fetchone() is None:
                    cur.execute('INSERT INTO private.metro_config (id,version,updated_at,payload) VALUES (1,%s,%s,%s)', (DEFAULT_CONFIG['version'], DEFAULT_CONFIG['updated_at'], json.dumps(DEFAULT_CONFIG)))
            db.commit()
        return
    with _sqlite_connection() as db:
        db.execute('CREATE TABLE IF NOT EXISTS metro_config (id INTEGER PRIMARY KEY CHECK(id=1), version INTEGER NOT NULL, updated_at TEXT NOT NULL, payload TEXT NOT NULL)')
        db.execute('CREATE TABLE IF NOT EXISTS metro_config_history (id INTEGER PRIMARY KEY AUTOINCREMENT, version INTEGER NOT NULL, updated_at TEXT NOT NULL, payload TEXT NOT NULL, changed_by TEXT NOT NULL DEFAULT \'system\')')
        if db.execute('SELECT 1 FROM metro_config WHERE id=1').fetchone() is None:
            db.execute('INSERT INTO metro_config VALUES (1, ?, ?, ?)', (DEFAULT_CONFIG['version'], DEFAULT_CONFIG['updated_at'], json.dumps(DEFAULT_CONFIG, separators=(',', ':'))))
        db.commit()


def load_config():
    init_db()
    if _is_postgres():
        import psycopg
        with psycopg.connect(DATABASE_URL) as db:
            with db.cursor() as cur:
                cur.execute('SELECT payload FROM private.metro_config WHERE id=1')
                row = cur.fetchone()
        return row[0]
    with _sqlite_connection() as db:
        row = db.execute('SELECT payload FROM metro_config WHERE id=1').fetchone()
    return json.loads(row[0])


def save_config(config, changed_by='admin'):
    validate_config(config)
    if not isinstance(changed_by, str) or len(changed_by) > 100:
        changed_by = 'admin'
    if _is_postgres():
        import psycopg
        with psycopg.connect(DATABASE_URL) as db:
            with db.cursor() as cur:
                cur.execute('SELECT version FROM private.metro_config WHERE id=1 FOR UPDATE')
                row = cur.fetchone()
                if row is None:
                    raise RuntimeError('Configuration store is not initialized')
                config = dict(config)
                config['version'] = int(row[0]) + 1
                config['updated_at'] = datetime.now(timezone.utc).isoformat()
                payload = json.dumps(config, separators=(',', ':'))
                cur.execute('INSERT INTO private.metro_config_history(version,updated_at,payload,changed_by) VALUES (%s,%s,%s,%s)', (config['version'], config['updated_at'], payload, changed_by))
                cur.execute('UPDATE private.metro_config SET version=%s,updated_at=%s,payload=%s WHERE id=1', (config['version'], config['updated_at'], payload))
            db.commit()
        return config
    current = load_config()
    config = dict(config)
    config['version'] = int(current.get('version', 0)) + 1
    config['updated_at'] = datetime.now(timezone.utc).isoformat()
    payload = json.dumps(config, separators=(',', ':'))
    with _sqlite_connection() as db:
        db.execute('INSERT INTO metro_config_history(version,updated_at,payload,changed_by) VALUES (?,?,?,?)', (config['version'], config['updated_at'], payload, changed_by))
        db.execute('UPDATE metro_config SET version=?,updated_at=?,payload=? WHERE id=1', (config['version'], config['updated_at'], payload))
        db.commit()
    return config


def history(limit=20):
    limit = max(1, min(int(limit), 100))
    init_db()
    if _is_postgres():
        import psycopg
        with psycopg.connect(DATABASE_URL) as db:
            with db.cursor() as cur:
                cur.execute('SELECT version,updated_at,changed_by,payload FROM private.metro_config_history ORDER BY version DESC LIMIT %s', (limit,))
                return [{'version': r[0], 'updated_at': r[1].isoformat(), 'changed_by': r[2], 'payload': r[3]} for r in cur.fetchall()]
    with _sqlite_connection() as db:
        rows = db.execute('SELECT version,updated_at,changed_by,payload FROM metro_config_history ORDER BY version DESC LIMIT ?', (limit,)).fetchall()
    return [{'version': r[0], 'updated_at': r[1], 'changed_by': r[2], 'payload': json.loads(r[3])} for r in rows]
