package com.olyv.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olyv.model.WeatherEntity;
import com.olyv.model.WeatherMqttEvent;
import com.olyv.repository.WeatherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.*;

@Service
public class MessageHandlerService {

    private static final Logger log = LoggerFactory.getLogger(MessageHandlerService.class);
    private final TelegramPublisherService telegramPublisher;
    private final WeatherRepository weatherRepository;
    private final ObjectMapper mapper;

    public MessageHandlerService(TelegramPublisherService telegramPublisher, WeatherRepository weatherRepository, ObjectMapper mapper) {
        this.telegramPublisher = telegramPublisher;
        this.weatherRepository = weatherRepository;
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

    private Optional<WeatherMqttEvent> parseMessage(Message<?> message) {
        String payload = (String) message.getPayload();
        try {
            var json = mapper.readTree(payload);
            return Optional.of(
                    new WeatherMqttEvent(
                        json.get("temp").asInt(),
                        json.get("humidity").asInt(),
                        json.get("pressure").asInt()
                    )
            );
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to parse message: {} {}", payload, e.getMessage());
        }
        return Optional.empty();
    }

    private void persistMessage(WeatherMqttEvent event) {
        var weatherEntity = new WeatherEntity(null,
                LocalDateTime.now().truncatedTo(SECONDS),
                event.temperature(),
                event.humidity(),
                event.pressure()
        );
        weatherRepository.save(weatherEntity);
        log.info("Persisted weather event: {}", weatherEntity);
    }

    private void send(String textMessage) {
        telegramPublisher.publishWeatherData(textMessage);
    }
}
