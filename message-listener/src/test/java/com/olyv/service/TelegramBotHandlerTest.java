package com.olyv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramBotHandlerTest {

    @Mock
    private WeatherAdvisorService advisorService;

    @Mock
    private TelegramClient telegramClient;

    private TelegramBotHandler botHandler;
    private final String botToken = "test-bot-token";

    @BeforeEach
    void setUp() {
        botHandler = new TelegramBotHandler(advisorService, telegramClient, botToken);
    }

    @Test
    @DisplayName("Should return configured bot token")
    void getBotToken_ReturnsConfiguredToken() {
        assertThat(botHandler.getBotToken()).isEqualTo(botToken);
    }

    @Test
    @DisplayName("Should send typing indicator, query advisor with conversationId, and send reply to user")
    void handleIncomingMessage_Success() throws TelegramApiException {
        Long chatId = 12345L;
        String userText = "Will it rain today?";
        String aiReply = "No rain expected.";

        when(advisorService.analyzeForUserQuery(eq(chatId.toString()), eq(userText))).thenReturn(aiReply);

        botHandler.handleIncomingMessage(chatId, userText);

        verify(advisorService).analyzeForUserQuery(chatId.toString(), userText);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, atLeastOnce()).execute(messageCaptor.capture());

        SendMessage lastSentMessage = messageCaptor.getValue();
        assertThat(lastSentMessage.getChatId()).isEqualTo(chatId.toString());
        assertThat(lastSentMessage.getText()).isEqualTo(aiReply);
    }

    @Test
    @DisplayName("Should send fallback message when advisor service throws exception")
    void handleIncomingMessage_AdvisorException_SendsFallbackMessage() throws TelegramApiException {
        Long chatId = 12345L;
        String userText = "Will it rain?";

        when(advisorService.analyzeForUserQuery(eq(chatId.toString()), eq(userText))).thenThrow(new RuntimeException("LLM unavailable"));

        botHandler.handleIncomingMessage(chatId, userText);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, atLeastOnce()).execute(messageCaptor.capture());

        SendMessage fallbackMessage = messageCaptor.getValue();
        assertThat(fallbackMessage.getChatId()).isEqualTo(chatId.toString());
        assertThat(fallbackMessage.getText()).contains("Sorry, I encountered an error");
    }

    @Test
    @DisplayName("Should consume updates and process valid message text")
    void getUpdatesConsumer_ProcessesMessageUpdate() throws TelegramApiException {
        Long chatId = 999L;
        String text = "Hello bot";
        when(advisorService.analyzeForUserQuery(eq(chatId.toString()), eq(text))).thenReturn("Hello user");

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(chatId);

        botHandler.getUpdatesConsumer().consume(List.of(update));

        verify(advisorService).analyzeForUserQuery(chatId.toString(), text);
    }
}
