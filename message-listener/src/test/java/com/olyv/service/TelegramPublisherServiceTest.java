package com.olyv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelegramPublisherServiceTest {

    @Mock
    private TelegramClient telegramClient;

    private TelegramPublisherService telegramPublisherService;
    private final String defaultChatId = "123456789";

    @BeforeEach
    void setUp() {
        telegramPublisherService = new TelegramPublisherService(telegramClient, defaultChatId);
    }

    @Test
    @DisplayName("Should build SendMessage with text and defaultChatId and execute client")
    void publishWeatherData_ExecutesTelegramClient() throws TelegramApiException {
        String testMessage = "Test Weather Report";

        telegramPublisherService.publishWeatherData(testMessage);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(messageCaptor.capture());

        SendMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getChatId()).isEqualTo(defaultChatId);
        assertThat(capturedMessage.getText()).isEqualTo(testMessage);
    }
}

