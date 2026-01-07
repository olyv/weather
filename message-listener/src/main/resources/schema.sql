CREATE TABLE IF NOT EXISTS weather_readings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    temperature INTEGER,
    humidity INTEGER,
    pressure INTEGER,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);