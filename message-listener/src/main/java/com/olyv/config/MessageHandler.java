package com.olyv.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    private static final String INSERT_SQL = """
            INSERT
            INTO weather_readings (temperature, humidity, pressure)
            VALUES (?, ?, ?)
            """;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper;

    public MessageHandler(JdbcTemplate jdbcTemplate, ObjectMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void process(Message<?> message) {
        String payload = (String) message.getPayload();

        try {
            JsonNode json = mapper.readTree(payload);

            var temperature = json.get("temp").asInt();
            var humidity = json.get("humidity").asInt();
            var pressure = json.get("pressure").asInt();

            jdbcTemplate.update(INSERT_SQL, temperature, humidity, pressure);

            log.info("💾 Row inserted into SQLite | Temp: {}°C | Humidity: {}% | Pressure: {} hPa",
                    temperature, humidity, pressure);

        } catch (Exception e) {
            log.error("❌ SQL Insert Failed: {}", e.getMessage());
        }
    }
}
