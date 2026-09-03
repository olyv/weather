package com.olyv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olyv.model.WeatherEntity;
import com.olyv.repository.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageHandlerServiceTest {

    @Mock
    private TelegramPublisherService telegramPublisher;

    @Mock
    private WeatherRepository weatherRepository;

    private MessageHandlerService messageHandlerService;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        messageHandlerService = new MessageHandlerService(telegramPublisher, weatherRepository, mapper);
    }

    @Test
    @DisplayName("Should parse valid MQTT payload, persist WeatherEntity, and publish Telegram message")
    void process_ValidJsonPayload_SavesEntityAndPublishesMessage() {
        String payload = """
                {
                    "temp": 22,
                    "humidity": 60,
                    "pressure": 1013
                }
                """;
        GenericMessage<String> message = new GenericMessage<>(payload);

        messageHandlerService.process(message);

        ArgumentCaptor<WeatherEntity> entityCaptor = ArgumentCaptor.forClass(WeatherEntity.class);
        verify(weatherRepository).save(entityCaptor.capture());
        WeatherEntity savedEntity = entityCaptor.getValue();

        assertThat(savedEntity.temperature()).isEqualTo(22);
        assertThat(savedEntity.humidity()).isEqualTo(60);
        assertThat(savedEntity.pressure()).isEqualTo(1013);
        assertThat(savedEntity.createdAt()).isNotNull();

        verify(telegramPublisher).publishWeatherData(anyString());
    }

    @Test
    @DisplayName("Should handle invalid JSON payload gracefully without persisting or publishing")
    void process_InvalidJsonPayload_IgnoresMessage() {
        GenericMessage<String> message = new GenericMessage<>("invalid json");

        messageHandlerService.process(message);

        verify(weatherRepository, never()).save(any());
        verify(telegramPublisher, never()).publishWeatherData(anyString());
    }
}

