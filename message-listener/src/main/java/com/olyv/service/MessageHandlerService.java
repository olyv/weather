package com.olyv.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olyv.model.WeatherDataReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MessageHandlerService {

    private static final Logger log = LoggerFactory.getLogger(MessageHandlerService.class);
    private static final String INSERT_SQL = """
            INSERT
            INTO weather_readings (temperature, humidity, pressure)
            VALUES (?, ?, ?)
            """;
    private final TelegramPublisherService telegramPublisher;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper;

    // Init with "dummy" values
    private int lastTemperature = -999;
    private int lastHumidity = -999;
    private int lastPressure = -999;

    public MessageHandlerService(TelegramPublisherService telegramPublisher, JdbcTemplate jdbcTemplate, ObjectMapper mapper) {
        this.telegramPublisher = telegramPublisher;
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void process(Message<?> message) {
        var weatherDataReport = parseMessage(message);
        weatherDataReport.ifPresent(it -> {
            persistMessage(it);
            send(it.toString());
        });
    }

    private Optional<WeatherDataReport> parseMessage(Message<?> message) {
        String payload = (String) message.getPayload();
        try {
            var json = mapper.readTree(payload);
            return Optional.of(
                    new WeatherDataReport(
                        json.get("temp").asInt(),
                        json.get("humidity").asInt(),
                        json.get("pressure").asInt()
                    )
            );
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to parse message: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private void persistMessage(WeatherDataReport weather) {
        try {
            jdbcTemplate.update(INSERT_SQL, weather.temperature(), weather.humidity(), weather.pressure());
            log.info("💾 Row inserted into SQLite | Temp: {}°C | Humidity: {}% | Pressure: {} hPa",
                    weather.temperature(), weather.humidity(), weather.pressure());
        } catch (DataAccessException e) {
            log.error("❌ SQL Insert Failed: {}", e.getMessage());
        }
    }

    private void send(String textMessage) {
        telegramPublisher.publishWeatherData(textMessage);
    }
}
