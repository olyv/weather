package com.olyv.model;

import java.time.LocalDateTime;

public record WeatherEntity(
        Long id,
        LocalDateTime createdAt,
        int temperature,
        int humidity,
        int pressure
) {
    public WeatherEntity(LocalDateTime createdAt,
                         int temperature,
                         int humidity,
                         int pressure) {
        this(null,
                createdAt,
                temperature,
                humidity,
                pressure
        );
    }
}
