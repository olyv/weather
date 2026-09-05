package com.olyv.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("weather_readings")
public record WeatherEntity(
        @Id Long id,
        @Column("created_at") LocalDateTime createdAt,
        int temperature,
        int humidity,
        int pressure
) {}