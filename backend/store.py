import json
import os
import sqlite3
from datetime import datetime, timezone
from pathlib import Path

DATABASE_URL = os.getenv('DATABASE_URL', '').strip()
DATA_DIR = Path(os.getenv('METRO_DATA_DIR', str(Path(__file__).with_name('data')))
DATA_DIR.mkdir(parents=True, exist_ok=True)
SQLITE_PATH = DATA_DIR / 'metro_admin.db'

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


def validate_config(config):
    if not isinstance(config, dict) or not isinstance(config.get('lines'), dict) or not isinstance(config.get('fares'), list):
        raise ValueError('Invalid configuration structure')
    previous = 0.0
    for slab in config['fares']:
        max_km = float(slab['max_km'])
        fare = int(slab['fare'])
        if max_km <= previous or fare < 0:
            raise ValueError('Fare slabs must have increasing distance limits and non-negative fares')
        previous = max_km
    for line in config['lines'].values():
        if not line.get('name') or not line.get('stations') or not line.get('first_train') or not line.get('last_train'):
            raise ValueError('Every line needs a name, stations, first train and last train')
        if int(line.get('headway_min', 0)) <= 0:
            raise ValueError('Headway must be positive')
    return config


def _sqlite_connection():
    return sqlite3.connect(SQLITE_PATH)


def init_db():
    if _is_postgres():
        import psycopg
        with psycopg.connect(DATABASE_URL) as db:
            with db.cursor() as cur:
                cur.execute('CREATE TABLE IF NOT EXISTS metro_config (id INTEGER PRIMARY KEY, version INTEGER NOT NULL, updated_at TEXT NOT NULL, payload JSONB NOT NULL)')
                cur.execute('CREATE TABLE IF NOT EXISTS metro_config_history (id BIGSERIAL PRIMARY KEY, version INTEGER NOT NULL, updated_at TEXT NOT NULL, payload JSONB NOT NULL, changed_by TEXT NOT NULL DEFAULT \'system\')')
                cur.execute('SELECT 1 FROM metro_config WHERE id=1')
                if cur.fetchone() is None:
                    cur.execute('INSERT INTO metro_config (id,version,updated_at,payload) VALUES (1,%s,%s,%s)', (DEFAULT_CONFIG['version'], DEFAULT_CONFIG['updated_at'], json.dumps(DEFAULT_CONFIG)))
            db.commit()
        return
    with _sqlite_connection() as db:
        db.execute('CREATE TABLE IF NOT EXISTS metro_config (id INTEGER PRIMARY KEY CHECK(id=1), version INTEGER NOT NULL, updated_at TEXT NOT NULL, payload TEXT NOT NULL)')
        db.execute('CREATE TABLE IF NOT EXISTS metro_config_history (id INTEGER PRIMARY KEY AUTOINCREMENT, version INTEGER NOT NULL, updated_at TEXT NOT NULL, payload TEXT NOT NULL, changed_by TEXT NOT NULL DEFAULT \'system\')')
        db.execute('CREATE TABLE IF NOT EXISTS config (id INTEGER PRIMARY KEY CHECK(id=1), version INTEGER NOT NULL, updated_at TEXT NOT NULL, payload TEXT NOT NULL)')
        row = db.execute('SELECT 1 FROM metro_config WHERE id=1').fetchone()
        if row is None:
            db.execute('INSERT INTO metro_config VALUES (1, ?, ?, ?)', (DEFAULT_CONFIG['version'], DEFAULT_CONFIG['updated_at'], json.dumps(DEFAULT_CONFIG, separators=(',', ':'))))
        db.commit()


def load_config():
    init_db()
    if _is_postgres():
        import psycopg
        with psycopg.connect(DATABASE_URL) as db:
            with db.cursor() as cur:
                cur.execute('SELECT payload FROM metro_config WHERE id=1')
                row = cur.fetchone()
        return row[0]
    with _sqlite_connection() as db:
        row = db.execute('SELECT payload FROM metro_config WHERE id=1').fetchone()
    return json.loads(row[0])


def save_config(config, changed_by='admin'):
    validate_config(config)
    current = load_config()
    config = dict(config)
    config['version'] = int(current.get('version', 0)) + 1
    config['updated_at'] = datetime.now(timezone.utc).isoformat()
    payload = json.dumps(config, separators=(',', ':'))
    if _is_postgres():
        import psycopg
        with psycopg.connect(DATABASE_URL) as db:
            with db.cursor() as cur:
                cur.execute('INSERT INTO metro_config_history(version,updated_at,payload,changed_by) VALUES (%s,%s,%s,%s)', (config['version'], config['updated_at'], payload, changed_by))
                cur.execute('UPDATE metro_config SET version=%s,updated_at=%s,payload=%s WHERE id=1', (config['version'], config['updated_at'], payload))
            db.commit()
        return config
    with _sqlite_connection() as db:
        db.execute('INSERT INTO metro_config_history(version,updated_at,payload,changed_by) VALUES (?,?,?,?)', (config['version'], config['updated_at'], payload, changed_by))
        db.execute('UPDATE metro_config SET version=?,updated_at=?,payload=? WHERE id=1', (config['version'], config['updated_at'], payload))
        db.commit()
    return config


def history(limit=20):
    init_db()
    if _is_postgres():
        import psycopg
        with psycopg.connect(DATABASE_URL) as db:
            with db.cursor() as cur:
                cur.execute('SELECT version,updated_at,changed_by,payload FROM metro_config_history ORDER BY version DESC LIMIT %s', (limit,))
                return [{'version': r[0], 'updated_at': r[1], 'changed_by': r[2], 'payload': r[3]} for r in cur.fetchall()]
    with _sqlite_connection() as db:
        rows = db.execute('SELECT version,updated_at,changed_by,payload FROM metro_config_history ORDER BY version DESC LIMIT ?', (limit,)).fetchall()
    return [{'version': r[0], 'updated_at': r[1], 'changed_by': r[2], 'payload': json.loads(r[3])} for r in rows]
