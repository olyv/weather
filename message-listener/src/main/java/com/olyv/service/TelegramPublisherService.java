package com.olyv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
public class TelegramPublisherService {

    private static final Logger log = LoggerFactory.getLogger(TelegramPublisherService.class);

    private final TelegramClient telegramClient;
    private final String defaultChatId;

    public TelegramPublisherService(TelegramClient telegramClient,
                                    @Value("${telegram.chat.id}") String defaultChatId) {
        this.telegramClient = telegramClient;
        this.defaultChatId = defaultChatId;
    }

    public void publishWeatherData(String textMessage) {
        SendMessage message = SendMessage.builder()
                .chatId(defaultChatId)
                .text(textMessage)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to publish message to Telegram: {}", e.getMessage(), e);
        }
    }
}