package com.olyv.repository;

import com.olyv.model.WeatherEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class WeatherRepository {

    private static final Logger log = LoggerFactory.getLogger(WeatherRepository.class);

    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_TEMPERATURE = "temperature";
    private static final String COLUMN_HUMIDITY = "humidity";
    private static final String COLUMN_PRESSURE = "pressure";

    public static final String SELECT_FOR_LAST_HOURS = """
        SELECT %s, %s, %s, %s 
        FROM weather_data 
        WHERE timestamp >= datetime('now', '-' || ? || ' hours')
        ORDER BY timestamp ASC
    """.formatted(COLUMN_CREATED_AT, COLUMN_TEMPERATURE, COLUMN_HUMIDITY, COLUMN_PRESSURE);

    private static final String INSERT_SQL = """
            INSERT
            INTO weather_readings (%s, %s, %s, %s)
            VALUES (?, ?, ?, ?)
            """.formatted(COLUMN_CREATED_AT, COLUMN_TEMPERATURE, COLUMN_HUMIDITY, COLUMN_PRESSURE);

    private static final String DELETE_SQL = "DELETE FROM weather_readings WHERE " + COLUMN_CREATED_AT + " < ?";

    private final JdbcTemplate jdbcTemplate;

    public WeatherRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(WeatherEntity weather) {
        try {
            jdbcTemplate.update(
                    INSERT_SQL,
                    weather.createdAt(),
                    weather.temperature(),
                    weather.humidity(),
                    weather.pressure()
            );
            log.info("💾 Row inserted into SQLite | Temp: {}°C | Humidity: {}% | Pressure: {} hPa",
                    weather.temperature(), weather.humidity(), weather.pressure());
        } catch (DataAccessException e) {
            log.error("❌ SQL Insert Failed: {}", e.getMessage());
        }
    }

    public int deleteOlderThan(LocalDateTime cutoff) {
        try {
            int deletedRows = jdbcTemplate.update(DELETE_SQL, cutoff);
            log.info("✅ Cleanup complete. Removed {} records using WeatherRepository.", deletedRows);
            return deletedRows;
        } catch (Exception e) {
            log.error("❌ SQL Cleanup failed: {}", e.getMessage());
            return 0;
        }
    }

    public List<WeatherEntity> getMetricsForLastHours(int hours) {
        return jdbcTemplate.query(SELECT_FOR_LAST_HOURS, (rs, rowNum) -> new WeatherEntity(
                rs.getTimestamp(COLUMN_CREATED_AT).toLocalDateTime(),
                rs.getInt(COLUMN_TEMPERATURE),
                rs.getInt(COLUMN_HUMIDITY),
                rs.getInt(COLUMN_PRESSURE)
        ), hours);
    }
}
