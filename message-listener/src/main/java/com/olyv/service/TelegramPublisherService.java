package com.olyv.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TelegramPublisherService {

    private static final Logger log = LoggerFactory.getLogger(TelegramPublisherService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    public void publishWeatherData(String textMessage) {
        sendToTelegram(textMessage);
    }

    private void sendToTelegram(String text) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        var body = Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "Markdown"
        );
        try {
            restTemplate.postForObject(url, body, String.class);
            log.info("Successfully published message to Telegram.");
        } catch (Exception e) {
            log.error("Failed to publish to Telegram. Error: {}", e.getMessage());
        }
    }
}
