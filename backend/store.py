import json
import sqlite3
from datetime import datetime, timezone
from pathlib import Path

DB_PATH = Path(__file__).with_name('metro_admin.db')
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


def init_db():
    with sqlite3.connect(DB_PATH) as db:
        db.execute('CREATE TABLE IF NOT EXISTS config (id INTEGER PRIMARY KEY CHECK(id=1), version INTEGER NOT NULL, updated_at TEXT NOT NULL, payload TEXT NOT NULL)')
        if db.execute('SELECT 1 FROM config WHERE id=1').fetchone() is None:
            db.execute('INSERT INTO config VALUES (1, ?, ?, ?)', (DEFAULT_CONFIG['version'], DEFAULT_CONFIG['updated_at'], json.dumps(DEFAULT_CONFIG, separators=(',', ':'))))
        db.commit()


def load_config():
    init_db()
    with sqlite3.connect(DB_PATH) as db:
        row = db.execute('SELECT payload FROM config WHERE id=1').fetchone()
    return json.loads(row[0])


def save_config(config):
    current = load_config()
    config = dict(config)
    config['version'] = int(current.get('version', 0)) + 1
    config['updated_at'] = datetime.now(timezone.utc).isoformat()
    with sqlite3.connect(DB_PATH) as db:
        db.execute('UPDATE config SET version=?, updated_at=?, payload=? WHERE id=1', (config['version'], config['updated_at'], json.dumps(config, separators=(',', ':'))))
        db.commit()
    return config
