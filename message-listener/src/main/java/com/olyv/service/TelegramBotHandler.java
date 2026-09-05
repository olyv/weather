package com.olyv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class TelegramBotHandler implements SpringLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotHandler.class);

    private final WeatherAdvisorService advisorService;
    private final TelegramClient telegramClient;
    private final String botToken;

    public TelegramBotHandler(WeatherAdvisorService advisorService,
                              TelegramClient telegramClient,
                              @Value("${telegram.bot.token}") String botToken) {
        this.advisorService = advisorService;
        this.telegramClient = telegramClient;
        this.botToken = botToken;
    }

    public void handleIncomingMessage(Long chatId, String userMessageText) {
        log.info("Received message from chatId {}: '{}'", chatId, userMessageText);

        try {
            // 1. Send "typing..." action (non-critical, logged as warning if fails)
            sendTypingIndicator(chatId);

            // 2. Query LLM + SQLite
            String aiResponse = advisorService.analyzeForUserQuery(userMessageText);

            // 3. Send final answer back to Telegram
            sendMessage(chatId, aiResponse);
            log.info("Successfully replied to chatId {}", chatId);

        } catch (Exception e) {
            log.error("❌ Error generating response for chatId {}: {}", chatId, e.getMessage(), e);
            sendFallbackMessage(chatId);
        }
    }

    private void sendTypingIndicator(Long chatId) {
        try {
            SendChatAction chatAction = SendChatAction.builder()
                    .chatId(chatId)
                    .action(ActionType.TYPING.toString())
                    .build();
            telegramClient.execute(chatAction);
        } catch (TelegramApiException e) {
            log.warn("⚠️ Could not send typing indicator to chatId {}: {}", chatId, e.getMessage());
        }
    }

    private void sendMessage(Long chatId, String textMessage) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(textMessage)
                .build();
        telegramClient.execute(message);
    }

    private void sendFallbackMessage(Long chatId) {
        try {
            SendMessage fallback = SendMessage.builder()
                    .chatId(chatId)
                    .text("Sorry, I encountered an error while analyzing the weather data. Please try again in a moment!")
                    .build();
            telegramClient.execute(fallback);
        } catch (TelegramApiException e) {
            log.error("❌ Failed to send fallback message to chatId {}: {}", chatId, e.getMessage());
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updates -> {
            for (Update update : updates) {
                if (update.hasMessage() && update.getMessage().hasText()) {
                    Long chatId = update.getMessage().getChatId();
                    String userText = update.getMessage().getText();

                    // Isolated per-message execution loop
                    try {
                        handleIncomingMessage(chatId, userText);
                    } catch (Exception e) {
                        log.error("❌ Critical error in update consumer loop for chatId {}: {}", chatId, e.getMessage(), e);
                    }
                }
            }
        };
    }
}