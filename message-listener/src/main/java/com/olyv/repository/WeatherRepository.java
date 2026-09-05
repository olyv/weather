package com.olyv.repository;

import com.olyv.model.WeatherEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WeatherRepository extends ListCrudRepository<WeatherEntity, Long> {

    @Query("""
        SELECT id, created_at, temperature, humidity, pressure
        FROM weather_readings
        WHERE created_at >= datetime('now', '-' || :hours || ' hours')
        ORDER BY created_at ASC
    """)
    List<WeatherEntity> getMetricsForLastHours(@Param("hours") int hours);

    @Modifying
    @Query("DELETE FROM weather_readings WHERE created_at < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}