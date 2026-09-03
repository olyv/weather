package com.olyv.service;

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
        try {
            // 1. Show "typing..." indicator while LLM generates response
            SendChatAction chatAction = SendChatAction.builder()
                    .chatId(chatId)
                    .action(ActionType.TYPING.toString())
                    .build();
            telegramClient.execute(chatAction);

            // 2. Query LLM + SQLite
//            String aiResponse = advisorService.analyzeForUserQuery(userMessageText);
            String aiResponse = "dummy response to " + userMessageText;

            // 3. Send final answer back to user
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(aiResponse)
                    .build();
            telegramClient.execute(message);

        } catch (TelegramApiException e) {
            e.printStackTrace();
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

                    // SET BREAKPOINT HERE
                    handleIncomingMessage(chatId, userText);
                }
            }
        };
    }
}
